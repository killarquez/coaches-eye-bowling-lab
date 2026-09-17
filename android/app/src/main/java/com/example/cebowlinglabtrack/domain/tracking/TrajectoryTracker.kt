package com.example.cebowlinglabtrack.domain.tracking

import com.example.cebowlinglabtrack.domain.calibration.HomographyMatrix
import com.example.cebowlinglabtrack.domain.model.BallMetrics
import com.example.cebowlinglabtrack.domain.model.LaneConstants
import com.example.cebowlinglabtrack.domain.model.Point2D
import com.example.cebowlinglabtrack.domain.model.TrajectoryPoint

/**
 * State machine for bowling ball optical tracking.
 */
enum class TrackingState {
    IDLE,
    APPROACH_DETECTED,
    BALL_RELEASED,
    BALL_IN_FLIGHT,
    PIN_DECK_ENTRY,
    SHOT_COMPLETED
}

/**
 * Coordinates ball centroid observations, homography projection,
 * Extended Kalman Filtering, and telemetry extraction.
 */
class TrajectoryTracker(
    private var homography: HomographyMatrix
) {
    var state: TrackingState = TrackingState.IDLE
        private set

    private var ekf: ExtendedKalmanFilter? = null
    private val rawPoints = mutableListOf<TrajectoryPoint>()
    private val filteredPoints = mutableListOf<TrajectoryPoint>()

    private var shotStartTimeMs: Long = 0L
    private var lastFrameTimeMs: Long = 0L
    private var lastBallMetrics: BallMetrics? = null

    val trajectory: List<TrajectoryPoint> get() = filteredPoints.toList()
    val metrics: BallMetrics? get() = lastBallMetrics

    fun updateHomography(newHomography: HomographyMatrix) {
        this.homography = newHomography
    }

    /**
     * Resets tracker for a new shot.
     */
    fun reset() {
        state = TrackingState.IDLE
        ekf = null
        rawPoints.clear()
        filteredPoints.clear()
        shotStartTimeMs = 0L
        lastFrameTimeMs = 0L
        lastBallMetrics = null
    }

    /**
     * Ingests an optical ball centroid detection (u, v) at frame timestamp timeMs.
     *
     * @param centroidScreen Detected ball centroid in camera image pixel coordinates.
     * @param timeMs Frame timestamp in milliseconds.
     * @return Current filtered trajectory point or null if not yet locked.
     */
    fun onBallCentroidDetected(centroidScreen: Point2D?, timeMs: Long, ballRadiusPx: Double = 0.0): TrajectoryPoint? {
        if (state == TrackingState.SHOT_COMPLETED) return null

        // 1. Invert contact patch pixel coordinate to lane coordinate (Board X, Distance Y)
        // Using bottom tangent of the circle (y + r) removes 3D elevation parallax and locks to true wood contact
        val contactScreen = if (centroidScreen != null && ballRadiusPx > 0.0) {
            Point2D(centroidScreen.x, centroidScreen.y + ballRadiusPx)
        } else {
            centroidScreen
        }
        val laneCoord = contactScreen?.let { homography.inverse(it) }

        val dtSec = if (lastFrameTimeMs > 0) {
            ((timeMs - lastFrameTimeMs) / 1000.0).coerceIn(0.005, 0.1)
        } else {
            0.016 // ~60 FPS initial
        }
        lastFrameTimeMs = timeMs

        if (laneCoord == null) {
            // Ball occluded or not detected this frame
            if (state == TrackingState.BALL_IN_FLIGHT || state == TrackingState.BALL_RELEASED || state == TrackingState.PIN_DECK_ENTRY) {
                ekf?.let { filter ->
                    filter.coast(dtSec)
                    if (filter.consecutiveCoastFrames > 10 || filter.yFt >= LaneConstants.FOUL_LINE_TO_HEADPIN_FT) {
                        if (filter.yFt >= 42.0 && filteredPoints.size >= 4) {
                            completeShot()
                        } else if (filter.consecutiveCoastFrames > 18) {
                            reset()
                        }
                    }
                }
            }
            return null
        }

        val zX = laneCoord.board
        val zY = laneCoord.distanceFt

        // Reject impossible detections far outside lane bounds
        if (zX < -3.0 || zX > 43.0 || zY < -8.0 || zY > 66.0) {
            return null
        }

        // Handle state machine transitions
        when (state) {
            TrackingState.IDLE, TrackingState.APPROACH_DETECTED -> {
                // Ball detection from approach (-4 ft) all the way past arrows (+35 ft) initiates tracking
                if (zY in -4.0..35.0) {
                    val estVy = 25.0 // ~17 mph default
                    val offsetSec = (zY.coerceAtLeast(0.0) / estVy)
                    shotStartTimeMs = timeMs - (offsetSec * 1000).toLong()

                    state = if (zY >= 15.0) TrackingState.BALL_IN_FLIGHT else TrackingState.BALL_RELEASED
                    ekf = ExtendedKalmanFilter(
                        initialX = zX,
                        initialY = zY,
                        initialVx = 0.0,
                        initialVy = estVy,
                        initialAx = 0.0
                    )
                    val pt = TrajectoryPoint(
                        xBoard = zX,
                        yFt = zY,
                        timeMs = (offsetSec * 1000).toLong(),
                        vx = 0.0,
                        vy = estVy,
                        isFiltered = true
                    )
                    filteredPoints.add(pt)
                    return pt
                }
            }

            TrackingState.BALL_RELEASED, TrackingState.BALL_IN_FLIGHT -> {
                val filter = ekf ?: return null
                filter.predict(dtSec)
                val accepted = filter.update(zX, zY)

                val elapsedMs = (timeMs - shotStartTimeMs).coerceAtLeast(0L)
                val filteredPoint = TrajectoryPoint(
                    xBoard = filter.xBoard,
                    yFt = filter.yFt,
                    timeMs = elapsedMs,
                    vx = filter.vx,
                    vy = filter.vy,
                    isFiltered = true
                )
                filteredPoints.add(filteredPoint)

                if (filter.yFt >= 15.0) {
                    state = TrackingState.BALL_IN_FLIGHT
                }
                if (filter.yFt >= 55.0) {
                    state = TrackingState.PIN_DECK_ENTRY
                }
                if (filter.yFt >= 59.5) {
                    completeShot()
                }

                return filteredPoint
            }

            TrackingState.PIN_DECK_ENTRY -> {
                val filter = ekf ?: return null
                filter.predict(dtSec)
                filter.update(zX, zY)
                val elapsedMs = (timeMs - shotStartTimeMs).coerceAtLeast(0L)
                val pt = TrajectoryPoint(filter.xBoard, filter.yFt, elapsedMs, filter.vx, filter.vy, true)
                filteredPoints.add(pt)

                if (filter.yFt >= 60.5) {
                    completeShot()
                }
                return pt
            }

            TrackingState.SHOT_COMPLETED -> {
                return null
            }
        }

        return null
    }

    /**
     * Marks the shot completed and extracts full telemetry metrics.
     */
    fun completeShot(): BallMetrics {
        state = TrackingState.SHOT_COMPLETED
        val computedMetrics = TelemetryExtractor.extractMetrics(filteredPoints)
        lastBallMetrics = computedMetrics
        return computedMetrics
    }
}

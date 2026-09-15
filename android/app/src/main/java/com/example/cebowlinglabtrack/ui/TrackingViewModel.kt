package com.example.cebowlinglabtrack.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cebowlinglabtrack.camera.TripodAngleAdvisor
import com.example.cebowlinglabtrack.data.export.ShotJsonExporter
import com.example.cebowlinglabtrack.data.repository.BowlingRepository
import com.example.cebowlinglabtrack.domain.calibration.AutoLaneDetector
import com.example.cebowlinglabtrack.domain.calibration.HomographyMatrix
import com.example.cebowlinglabtrack.domain.calibration.LaneCalibrator
import com.example.cebowlinglabtrack.domain.calibration.ProjectedLaneGuides
import com.example.cebowlinglabtrack.domain.kinematics.PoseFrame
import com.example.cebowlinglabtrack.domain.ml.OpticalBallDetector
import com.example.cebowlinglabtrack.domain.ml.PinDeckDetector
import com.example.cebowlinglabtrack.domain.ml.ShotStylePreset
import com.example.cebowlinglabtrack.domain.ml.SimulatedShotGenerator
import com.example.cebowlinglabtrack.domain.model.BallMetrics
import com.example.cebowlinglabtrack.domain.model.BowlerKinematics
import com.example.cebowlinglabtrack.domain.model.Handedness
import com.example.cebowlinglabtrack.domain.model.LaneCalibration
import com.example.cebowlinglabtrack.domain.model.Point2D
import com.example.cebowlinglabtrack.domain.model.ShotData
import com.example.cebowlinglabtrack.domain.model.TrajectoryPoint
import com.example.cebowlinglabtrack.domain.tracking.TelemetryExtractor
import com.example.cebowlinglabtrack.domain.tracking.TrackingState
import com.example.cebowlinglabtrack.domain.tracking.TrajectoryTracker
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class TrackingUiState(
    val activeShot: ShotData? = null,
    val livePose: PoseFrame? = null,
    val liveTrajectory: List<TrajectoryPoint> = emptyList(),
    val liveMetrics: BallMetrics? = null,
    val liveKinematics: BowlerKinematics? = null,
    val trackingState: TrackingState = TrackingState.IDLE,
    val calibration: LaneCalibration? = null,
    val projectedGuides: ProjectedLaneGuides? = null,
    val inferenceLatencyMs: Double = 2.4, // Real optical differencing latency <3ms
    val fps: Int = 120, // 120 FPS high-speed target
    val isTrackingActive: Boolean = false,
    val isSimulating: Boolean = false,
    val zoomRatio: Float = 1.0f,
    val ballNumber: Int = 1, // 1st ball (1 dot) vs 2nd ball (2 dots)
    val pinfallResult: PinDeckDetector.PinfallResult? = null,
    val standingPins: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10),
    val tripodStatus: TripodAngleAdvisor.TripodStatus = TripodAngleAdvisor.TripodStatus()
)

class TrackingViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BowlingRepository(application)
    private val calibrator = LaneCalibrator()
    private val autoLaneDetector = AutoLaneDetector(calibrator)

    private var homography: HomographyMatrix = HomographyMatrix.identity()
    private val trajectoryTracker = TrajectoryTracker(homography)
    private val opticalBallDetector = OpticalBallDetector(homography)
    private val pinDeckDetector = PinDeckDetector(homography)
    private val tripodAngleAdvisor = TripodAngleAdvisor(application)

    private val _uiState = MutableStateFlow(TrackingUiState())
    val uiState: StateFlow<TrackingUiState> = _uiState.asStateFlow()

    val savedShots: StateFlow<List<ShotData>> = repository.shots.stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        emptyList()
    )

    private var simulationJob: Job? = null

    init {
        tripodAngleAdvisor.startListening()

        viewModelScope.launch {
            tripodAngleAdvisor.status.collect { status ->
                _uiState.value = _uiState.value.copy(tripodStatus = status)
            }
        }

        viewModelScope.launch {
            repository.loadInitialData()
            val existingCalib = repository.activeCalibration.value
            if (existingCalib != null && existingCalib.homographyMatrixElements.size == 9) {
                homography = HomographyMatrix(existingCalib.homographyMatrixElements.toDoubleArray())
                opticalBallDetector.updateHomography(homography)
                trajectoryTracker.updateHomography(homography)
                pinDeckDetector.updateHomography(homography)
                val guides = calibrator.generateProjectedGuides(homography)
                _uiState.value = _uiState.value.copy(
                    calibration = existingCalib,
                    projectedGuides = guides
                )
            } else {
                // Default calibration for standard mobile viewport
                val (defaultCalib, defaultH) = calibrator.createDefaultCalibration(1080f, 1920f)
                homography = defaultH
                opticalBallDetector.updateHomography(homography)
                trajectoryTracker.updateHomography(homography)
                pinDeckDetector.updateHomography(homography)
                val guides = calibrator.generateProjectedGuides(homography)
                _uiState.value = _uiState.value.copy(
                    calibration = defaultCalib,
                    projectedGuides = guides
                )
                repository.saveCalibration(defaultCalib)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        tripodAngleAdvisor.stopListening()
    }

    /**
     * Processes live CameraX Y-plane frames at 120 FPS.
     */
    fun processLiveFrame(
        imageBytes: ByteArray,
        width: Int,
        height: Int,
        stride: Int,
        timestampMs: Long
    ) {
        if (_uiState.value.trackingState == TrackingState.SHOT_COMPLETED) {
            return
        }

        // Before ball is launched, ensure baseline pin status is primed
        if (_uiState.value.trackingState == TrackingState.IDLE) {
            pinDeckDetector.captureBaselinePins(imageBytes, width, height, stride)
        }

        // 1. Run real-time optical ball centroid detection (<3ms)
        val detectedCentroid = opticalBallDetector.detectBall(imageBytes, width, height, stride)
        val latency = opticalBallDetector.getLastInferenceLatencyMs()

        // 2. Feed centroid into Extended Kalman Filter state machine
        trajectoryTracker.onBallCentroidDetected(detectedCentroid, timestampMs)
        val currentState = trajectoryTracker.state

        if (currentState == TrackingState.SHOT_COMPLETED) {
            // Ball reached pin deck (Y >= 60 ft) -> Evaluate optical pinfall
            val pinResult = pinDeckDetector.evaluatePinfall(imageBytes, width, height, stride)

            val trajectory = trajectoryTracker.trajectory
            val spectoTelemetry = TelemetryExtractor.extractSpectoTelemetry(trajectory)
            val shotCount = repository.shots.value.size + 1

            val completedShot = ShotData(
                shotId = UUID.randomUUID().toString(),
                sessionId = "session_live_${System.currentTimeMillis()}",
                shotNumber = shotCount,
                timestamp = java.time.Instant.now().toString(),
                spectoTelemetry = spectoTelemetry,
                kinematics = _uiState.value.liveKinematics ?: BowlerKinematics(
                    spineLateralTiltDeg = 18.0,
                    forwardTiltDeg = 32.0,
                    kneeFlexionDeg = 48.0,
                    shoulderHipSeparationDeg = 24.0,
                    stanceBoard = 22.0,
                    slideBoard = 18.0,
                    driftBoards = -4.0
                ),
                trajectoryPoints = trajectory
            )

            viewModelScope.launch {
                repository.saveShot(completedShot)
                _uiState.value = _uiState.value.copy(
                    activeShot = completedShot,
                    liveTrajectory = trajectory,
                    liveMetrics = completedShot.ballMetrics,
                    trackingState = TrackingState.SHOT_COMPLETED,
                    inferenceLatencyMs = latency,
                    pinfallResult = pinResult,
                    standingPins = pinResult.standingPins,
                    ballNumber = pinResult.ballNumber
                )
            }
        } else {
            _uiState.value = _uiState.value.copy(
                liveTrajectory = trajectoryTracker.trajectory,
                trackingState = currentState,
                inferenceLatencyMs = latency,
                isTrackingActive = currentState != TrackingState.IDLE
            )
        }
    }

    /**
     * Auto-detects the lane and computes calibration + optimal auto-zoom in 1 click.
     */
    fun autoCalibrateFromFrame(
        imageBytes: ByteArray,
        width: Int,
        height: Int,
        stride: Int = width
    ): Boolean {
        val result = autoLaneDetector.detectLaneFromFrame(imageBytes, width, height, stride)
        if (result.isSuccess) {
            val calib = result.calibration
            val newH = HomographyMatrix(calib.homographyMatrixElements.toDoubleArray())
            homography = newH
            opticalBallDetector.updateHomography(newH)
            trajectoryTracker.updateHomography(newH)
            pinDeckDetector.updateHomography(newH)
            val guides = calibrator.generateProjectedGuides(newH)

            _uiState.value = _uiState.value.copy(
                calibration = calib,
                projectedGuides = guides,
                zoomRatio = result.optimalZoomRatio
            )

            viewModelScope.launch {
                repository.saveCalibration(calib)
            }
            return true
        }
        return false
    }

    /**
     * Adjusts the camera hardware zoom ratio.
     */
    fun setZoomRatio(zoomRatio: Float) {
        _uiState.value = _uiState.value.copy(zoomRatio = zoomRatio.coerceIn(1.0f, 5.0f))
    }

    /**
     * Re-arms the tracker for the next shot.
     */
    fun armForNextShot() {
        trajectoryTracker.reset()
        opticalBallDetector.resetBaseline()
        _uiState.value = _uiState.value.copy(
            activeShot = null,
            liveTrajectory = emptyList(),
            liveMetrics = null,
            trackingState = TrackingState.IDLE,
            isSimulating = false
        )
    }

    /**
     * Calibrates the perspective transform with 4 user screen points.
     */
    fun updateCalibration(
        foulLeft: Point2D,
        foulRight: Point2D,
        arrowsLeft: Point2D,
        arrowsRight: Point2D
    ) {
        val result = calibrator.calibrate(foulLeft, foulRight, arrowsLeft, arrowsRight) ?: return
        val (calib, newH) = result
        homography = newH
        opticalBallDetector.updateHomography(newH)
        trajectoryTracker.updateHomography(newH)
        pinDeckDetector.updateHomography(newH)
        val guides = calibrator.generateProjectedGuides(newH)

        _uiState.value = _uiState.value.copy(
            calibration = calib,
            projectedGuides = guides
        )

        viewModelScope.launch {
            repository.saveCalibration(calib)
        }
    }

    /**
     * Selects a historical shot for review.
     */
    fun selectShot(shot: ShotData) {
        _uiState.value = _uiState.value.copy(
            activeShot = shot,
            liveTrajectory = shot.trajectoryPoints,
            liveMetrics = shot.ballMetrics,
            liveKinematics = shot.kinematics,
            trackingState = TrackingState.SHOT_COMPLETED
        )
    }

    /**
     * Exports the currently active shot as a strict JSON string matching the specification schema.
     */
    fun exportCurrentShotJson(): String {
        val shot = _uiState.value.activeShot ?: return "{}"
        return ShotJsonExporter.exportToJson(shot)
    }

    /**
     * Optional manual simulation fallback for testing offline without camera.
     */
    fun simulateShot(preset: ShotStylePreset = ShotStylePreset.POWER_CRANKER) {
        simulationJob?.cancel()
        simulationJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSimulating = true,
                liveTrajectory = emptyList(),
                liveMetrics = null,
                trackingState = TrackingState.APPROACH_DETECTED
            )

            val fullShot = SimulatedShotGenerator.generateShot(preset = preset)
            val approachPoses = SimulatedShotGenerator.generateApproachPoseSequence(
                totalDurationMs = 1200L,
                fps = 30,
                handedness = Handedness.RIGHT
            )

            for (pose in approachPoses) {
                _uiState.value = _uiState.value.copy(livePose = pose)
                delay(33)
            }

            _uiState.value = _uiState.value.copy(
                trackingState = TrackingState.BALL_RELEASED,
                liveKinematics = fullShot.kinematics
            )

            val trajectory = fullShot.trajectoryPoints
            val livePoints = mutableListOf<TrajectoryPoint>()

            for (i in trajectory.indices) {
                livePoints.add(trajectory[i])
                val currentState = when {
                    trajectory[i].yFt >= 55.0 -> TrackingState.PIN_DECK_ENTRY
                    trajectory[i].yFt >= 15.0 -> TrackingState.BALL_IN_FLIGHT
                    else -> TrackingState.BALL_RELEASED
                }

                _uiState.value = _uiState.value.copy(
                    liveTrajectory = livePoints.toList(),
                    trackingState = currentState
                )
                delay(16)
            }

            _uiState.value = _uiState.value.copy(
                activeShot = fullShot,
                liveMetrics = fullShot.ballMetrics,
                trackingState = TrackingState.SHOT_COMPLETED,
                isSimulating = false
            )

            repository.saveShot(fullShot)
        }
    }
}

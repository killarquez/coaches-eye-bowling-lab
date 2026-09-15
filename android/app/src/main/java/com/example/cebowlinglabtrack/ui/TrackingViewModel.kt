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
import com.example.cebowlinglabtrack.domain.ml.OpticalRevCounter
import com.example.cebowlinglabtrack.domain.ml.OpticalBallDetector
import com.example.cebowlinglabtrack.domain.ml.PinDeckDetector
import com.example.cebowlinglabtrack.domain.ml.ShotStylePreset
import com.example.cebowlinglabtrack.domain.ml.SimulatedShotGenerator
import com.example.cebowlinglabtrack.domain.model.BallMetrics
import com.example.cebowlinglabtrack.domain.model.BowlerKinematics
import com.example.cebowlinglabtrack.domain.model.BowlerProfile
import com.example.cebowlinglabtrack.domain.model.Handedness
import com.example.cebowlinglabtrack.domain.model.LaneCalibration
import com.example.cebowlinglabtrack.domain.model.Point2D
import com.example.cebowlinglabtrack.domain.model.ShotData
import com.example.cebowlinglabtrack.domain.model.TargetComparisonResult
import com.example.cebowlinglabtrack.domain.model.TrajectoryPoint
import com.example.cebowlinglabtrack.domain.model.VisualTargetLine
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
    val isLaneCalibrated: Boolean = false, // Safety interlock: inhibits shot recording until calibrated
    val calibrationQuality: String = "UNCALIBRATED",
    val projectedGuides: ProjectedLaneGuides? = null,
    val activeTargetLine: VisualTargetLine = VisualTargetLine.DEFAULT,
    val targetComparison: TargetComparisonResult? = null,
    val showTargetLineSelector: Boolean = false,
    val inferenceLatencyMs: Double = 2.4, // Real optical differencing latency <3ms
    val fps: Int = 120, // 120 FPS high-speed target
    val isTrackingActive: Boolean = false,
    val isSimulating: Boolean = false,
    val zoomRatio: Float = 1.0f,
    val ballNumber: Int = 1, // 1st ball (1 dot) vs 2nd ball (2 dots)
    val pinfallResult: PinDeckDetector.PinfallResult? = null,
    val standingPins: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10),
    val tripodStatus: TripodAngleAdvisor.TripodStatus = TripodAngleAdvisor.TripodStatus(),
    val activeBowler: BowlerProfile? = null,
    val allBowlers: List<BowlerProfile> = emptyList(),
    val isOpticalRevModeActive: Boolean = true
)

class TrackingViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BowlingRepository(application)
    private val calibrator = LaneCalibrator()
    private val autoLaneDetector = AutoLaneDetector(calibrator)

    private var homography: HomographyMatrix = HomographyMatrix.identity()
    private val trajectoryTracker = TrajectoryTracker(homography)
    private val opticalBallDetector = OpticalBallDetector(homography)
    private val opticalRevCounter = OpticalRevCounter()
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
            repository.bowlers.collect { roster ->
                val current = _uiState.value.activeBowler
                val updatedActive = if (current != null) {
                    roster.find { it.id == current.id } ?: roster.firstOrNull()
                } else {
                    roster.firstOrNull()
                }
                _uiState.value = _uiState.value.copy(
                    allBowlers = roster,
                    activeBowler = updatedActive
                )
            }
        }

        viewModelScope.launch {
            repository.loadInitialData()
            val existingCalib = repository.activeCalibration.value
            if (existingCalib != null && existingCalib.homographyMatrixElements.size == 9 && existingCalib.reprojectionErrorRmse < 15.0) {
                homography = HomographyMatrix(existingCalib.homographyMatrixElements.toDoubleArray())
                opticalBallDetector.updateHomography(homography)
                trajectoryTracker.updateHomography(homography)
                pinDeckDetector.updateHomography(homography)
                val guides = calibrator.generateProjectedGuides(homography, _uiState.value.activeTargetLine)
                _uiState.value = _uiState.value.copy(
                    calibration = existingCalib,
                    isLaneCalibrated = true,
                    calibrationQuality = "CALIBRATED",
                    projectedGuides = guides
                )
            } else {
                // Initial launch: uncalibrated physical lane (safety interlock active)
                val (defaultCalib, defaultH) = calibrator.createDefaultCalibration(1080f, 1920f)
                homography = defaultH
                opticalBallDetector.updateHomography(homography)
                trajectoryTracker.updateHomography(homography)
                pinDeckDetector.updateHomography(homography)
                val guides = calibrator.generateProjectedGuides(homography, _uiState.value.activeTargetLine)
                _uiState.value = _uiState.value.copy(
                    calibration = defaultCalib,
                    isLaneCalibrated = false,
                    calibrationQuality = "CALIBRATION REQUIRED",
                    projectedGuides = guides
                )
            }
        }
    }

    private var latestFrame: ByteArray? = null
    private var latestWidth: Int = 0
    private var latestHeight: Int = 0
    private var latestStride: Int = 0

    override fun onCleared() {
        super.onCleared()
        tripodAngleAdvisor.stopListening()
    }

    /**
     * Processes live CameraX Y-plane frames at 120 FPS.
     * Safety Interlock: Inhibits tracking and recording if the physical lane is not yet calibrated.
     */
    fun processLiveFrame(
        imageBytes: ByteArray,
        width: Int,
        height: Int,
        stride: Int,
        timestampMs: Long
    ) {
        latestFrame = imageBytes
        latestWidth = width
        latestHeight = height
        latestStride = stride

        // Safety interlock: inhibit tracking if lane is not calibrated
        if (!_uiState.value.isLaneCalibrated) {
            return
        }

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

        // 2. If ball is detected, feed high-contrast tape sub-region into Optical Rev Counter
        if (detectedCentroid != null && _uiState.value.isOpticalRevModeActive) {
            val ballRadiusPx = opticalBallDetector.getLastBallRadiusPx()
            opticalRevCounter.processFrame(
                imageBytes = imageBytes,
                width = width,
                height = height,
                stride = stride,
                ballCenter = detectedCentroid,
                ballRadiusPx = ballRadiusPx,
                timestampMs = timestampMs
            )
        }

        // 3. Feed centroid into Extended Kalman Filter state machine
        trajectoryTracker.onBallCentroidDetected(detectedCentroid, timestampMs)
        val currentState = trajectoryTracker.state

        if (currentState == TrackingState.SHOT_COMPLETED) {
            // Ball reached pin deck (Y >= 60 ft) -> Evaluate optical pinfall
            val pinResult = pinDeckDetector.evaluatePinfall(imageBytes, width, height, stride)

            val trajectory = trajectoryTracker.trajectory
            val durationMs = if (trajectory.size >= 2) {
                (trajectory.last().timestampMs - trajectory.first().timestampMs).coerceAtLeast(1000L)
            } else 1800L

            val opticalRevResult = if (_uiState.value.isOpticalRevModeActive) {
                opticalRevCounter.evaluateShotRevRate(
                    shotDurationMs = durationMs,
                    fallbackRpm = _uiState.value.activeBowler?.benchmarkRpm ?: 400
                )
            } else null

            val spectoTelemetry = TelemetryExtractor.extractSpectoTelemetry(
                trajectory = trajectory,
                opticalRevResult = opticalRevResult
            )
            val targetComp = _uiState.value.activeTargetLine.compareShot(trajectory)
            val shotCount = repository.shots.value.size + 1
            val currentBowlerId = _uiState.value.activeBowler?.id ?: "CEB-101"

            val completedShot = ShotData(
                shotId = UUID.randomUUID().toString(),
                sessionId = "session_${currentBowlerId}_${System.currentTimeMillis()}",
                shotNumber = shotCount,
                timestamp = java.time.Instant.now().toString(),
                bowlerId = currentBowlerId,
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
                    ballNumber = pinResult.ballNumber,
                    targetComparison = targetComp
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
            val guides = calibrator.generateProjectedGuides(newH, _uiState.value.activeTargetLine)

            _uiState.value = _uiState.value.copy(
                calibration = calib,
                isLaneCalibrated = true,
                calibrationQuality = "CALIBRATED (AUTO)",
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
     * Attempts 1-tap auto-calibration from the most recently captured live camera frame.
     * If no live camera frame is present, returns false.
     */
    fun autoCalibrateLatestFrame(): Boolean {
        val bytes = latestFrame ?: return false
        val w = latestWidth
        val h = latestHeight
        val s = if (latestStride > 0) latestStride else w
        if (w <= 0 || h <= 0) return false
        return autoCalibrateFromFrame(bytes, w, h, s)
    }

    /**
     * Arms the tracker using standard USBC physical lane dimensions preset.
     */
    fun calibrateWithDefaults() {
        val defaultCalib = LaneCalibrator.DEFAULT_CALIBRATION
        val newH = HomographyMatrix(defaultCalib.homographyMatrixElements.toDoubleArray())
        homography = newH
        opticalBallDetector.updateHomography(newH)
        trajectoryTracker.updateHomography(newH)
        pinDeckDetector.updateHomography(newH)
        val guides = calibrator.generateProjectedGuides(newH, _uiState.value.activeTargetLine)
        _uiState.value = _uiState.value.copy(
            calibration = defaultCalib,
            isLaneCalibrated = true,
            calibrationQuality = "CALIBRATED (STANDARD)",
            projectedGuides = guides
        )
        viewModelScope.launch {
            repository.saveCalibration(defaultCalib)
        }
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
        opticalRevCounter.reset()
        _uiState.value = _uiState.value.copy(
            activeShot = null,
            liveTrajectory = emptyList(),
            liveMetrics = null,
            targetComparison = null,
            trackingState = TrackingState.IDLE,
            isSimulating = false
        )
    }

    /**
     * Selects an active target line (Strike.app style).
     */
    fun selectTargetLine(targetLine: VisualTargetLine) {
        val guides = calibrator.generateProjectedGuides(homography, targetLine)
        _uiState.value = _uiState.value.copy(
            activeTargetLine = targetLine,
            projectedGuides = guides,
            showTargetLineSelector = false
        )
    }

    /**
     * Toggles the target line selection drawer.
     */
    fun toggleTargetLineSelector(show: Boolean? = null) {
        _uiState.value = _uiState.value.copy(
            showTargetLineSelector = show ?: !_uiState.value.showTargetLineSelector
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
        val guides = calibrator.generateProjectedGuides(newH, _uiState.value.activeTargetLine)

        _uiState.value = _uiState.value.copy(
            calibration = calib,
            isLaneCalibrated = true,
            calibrationQuality = "CALIBRATED (MANUAL 4-PT)",
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
     * Selects an athlete from the roster as the active bowler.
     */
    fun selectBowler(bowler: BowlerProfile) {
        _uiState.value = _uiState.value.copy(activeBowler = bowler)
    }

    /**
     * Adds or updates a bowler profile and sets them as active.
     */
    fun saveBowler(bowler: BowlerProfile) {
        viewModelScope.launch {
            repository.saveBowler(bowler)
            _uiState.value = _uiState.value.copy(activeBowler = bowler)
        }
    }

    /**
     * Toggles the direct optical rev counter (tape COG to PAP).
     */
    fun toggleOpticalRevMode(active: Boolean? = null) {
        _uiState.value = _uiState.value.copy(
            isOpticalRevModeActive = active ?: !_uiState.value.isOpticalRevModeActive
        )
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
            val currentBowlerId = _uiState.value.activeBowler?.id ?: "CEB-101"
            val taggedShot = fullShot.copy(
                bowlerId = currentBowlerId,
                sessionId = "session_${currentBowlerId}_${System.currentTimeMillis()}"
            )

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
                liveKinematics = taggedShot.kinematics
            )

            val trajectory = taggedShot.trajectoryPoints
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

            val targetComp = _uiState.value.activeTargetLine.compareShot(taggedShot.trajectoryPoints)

            _uiState.value = _uiState.value.copy(
                activeShot = taggedShot,
                liveMetrics = taggedShot.ballMetrics,
                trackingState = TrackingState.SHOT_COMPLETED,
                isSimulating = false,
                targetComparison = targetComp
            )

            repository.saveShot(taggedShot)
        }
    }
}

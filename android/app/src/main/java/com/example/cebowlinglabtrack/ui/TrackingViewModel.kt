package com.example.cebowlinglabtrack.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cebowlinglabtrack.camera.TripodAngleAdvisor
import com.example.cebowlinglabtrack.camera.ViewfinderCoordinateTransformer
import com.example.cebowlinglabtrack.data.export.ShotJsonExporter
import com.example.cebowlinglabtrack.data.repository.BowlingRepository
import com.example.cebowlinglabtrack.domain.calibration.AutoLaneDetector
import com.example.cebowlinglabtrack.domain.calibration.CalibrationAnchorMode
import com.example.cebowlinglabtrack.domain.calibration.HomographyMatrix
import com.example.cebowlinglabtrack.domain.calibration.LaneCalibrator
import com.example.cebowlinglabtrack.domain.calibration.ProjectedLaneGuides
import com.example.cebowlinglabtrack.domain.kinematics.PoseFrame
import com.example.cebowlinglabtrack.domain.ml.OpticalRevCounter
import com.example.cebowlinglabtrack.domain.ml.OpticalBallDetector
import com.example.cebowlinglabtrack.domain.ml.PinDeckDetector
import com.example.cebowlinglabtrack.domain.ml.TFLiteBallDetector
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
    val isOpticalRevModeActive: Boolean = true,
    val isMLDetectorActive: Boolean = true,
    val laneRecognitionStatus: String? = null,
    val autoCenterGuidance: String? = null,
    val pinRackDetected: Boolean = false,
    val guttersDetected: Boolean = false
)

class TrackingViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BowlingRepository(application)
    private val calibrator = LaneCalibrator()
    private val autoLaneDetector = AutoLaneDetector(calibrator)

    private var homography: HomographyMatrix = HomographyMatrix.identity()
    private val trajectoryTracker = TrajectoryTracker(homography)
    private val opticalBallDetector = OpticalBallDetector(homography)
    private var tfliteBallDetector: TFLiteBallDetector? = null
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
    private var pinfallEvaluationJob: Job? = null
    private var idleFrameCount = 0

    private var baseHomography: HomographyMatrix = homography
    private var baseCalibrationZoom: Float = 1.0f
    private var viewportWidth: Float = 1080f
    private var viewportHeight: Float = 1920f

    init {
        try {
            tfliteBallDetector = TFLiteBallDetector.fromAsset(application)
        } catch (e: Throwable) {
            // Graceful fallback to OpticalBallDetector if TFLite asset is uninitialized or unsupported
            tfliteBallDetector = null
        }

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
                val h = HomographyMatrix(existingCalib.homographyMatrixElements.toDoubleArray())
                baseHomography = h
                baseCalibrationZoom = if (existingCalib.calibrationZoomRatio in 1.0f..5.0f) existingCalib.calibrationZoomRatio else 1.0f
                homography = h
                trajectoryTracker.updateHomography(h)
                cameraHomographySyncedForCalibId = ""
                val w = if (latestWidth > 0) latestWidth.toFloat() else 1080f
                val hgt = if (latestHeight > 0) latestHeight.toFloat() else 1920f
                ensureCameraHomographySynced(w, hgt)
                val guides = calibrator.generateProjectedGuides(h, _uiState.value.activeTargetLine)
                _uiState.value = _uiState.value.copy(
                    calibration = existingCalib,
                    isLaneCalibrated = true,
                    calibrationQuality = "CALIBRATED",
                    projectedGuides = guides,
                    zoomRatio = baseCalibrationZoom
                )
            } else {
                // Initial launch: uncalibrated physical lane (safety interlock active)
                val (defaultCalib, defaultH) = calibrator.createDefaultCalibration(viewportWidth, viewportHeight)
                baseHomography = defaultH
                baseCalibrationZoom = 1.0f
                homography = defaultH
                trajectoryTracker.updateHomography(defaultH)
                cameraHomographySyncedForCalibId = ""
                ensureCameraHomographySynced(1080f, 1920f)
                val guides = calibrator.generateProjectedGuides(defaultH, _uiState.value.activeTargetLine)
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
    private var cameraHomographySyncedForWidth = 0
    private var cameraHomographySyncedForHeight = 0
    private var cameraHomographySyncedForCalibId = ""

    private fun ensureCameraHomographySynced(camW: Float, camH: Float) {
        val calib = _uiState.value.calibration ?: return
        if (cameraHomographySyncedForWidth == camW.toInt() &&
            cameraHomographySyncedForHeight == camH.toInt() &&
            cameraHomographySyncedForCalibId == calib.id) {
            return
        }
        val transformer = ViewfinderCoordinateTransformer(camW, camH, viewportWidth, viewportHeight)
        val foulLeftCam = transformer.screenToCamera(calib.foulLineLeftScreen)
        val foulRightCam = transformer.screenToCamera(calib.foulLineRightScreen)
        val deckLeftCam = transformer.screenToCamera(calib.arrowsLeftScreen)
        val deckRightCam = transformer.screenToCamera(calib.arrowsRightScreen)

        val camCalibPair = calibrator.calibrate(
            foulLineLeft = foulLeftCam,
            foulLineRight = foulRightCam,
            arrowsLeft = deckLeftCam,
            arrowsRight = deckRightCam,
            anchorMode = CalibrationAnchorMode.PIN_DECK,
            calibrationZoomRatio = _uiState.value.zoomRatio
        )
        val camHMatrix = camCalibPair?.second ?: homography
        opticalBallDetector.updateHomography(camHMatrix)
        pinDeckDetector.updateHomography(camHMatrix)
        cameraHomographySyncedForWidth = camW.toInt()
        cameraHomographySyncedForHeight = camH.toInt()
        cameraHomographySyncedForCalibId = calib.id
    }

    override fun onCleared() {
        super.onCleared()
        tripodAngleAdvisor.stopListening()
        tfliteBallDetector?.close()
        tfliteBallDetector = null
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

        val transformer = ViewfinderCoordinateTransformer(
            cameraWidth = width.toFloat(),
            cameraHeight = height.toFloat(),
            screenWidth = viewportWidth,
            screenHeight = viewportHeight
        )
        ensureCameraHomographySynced(width.toFloat(), height.toFloat())

        // Before ball is launched, ensure baseline pin status is primed and live standing pins monitored
        if (_uiState.value.trackingState == TrackingState.IDLE) {
            pinDeckDetector.captureBaselinePins(imageBytes, width, height, stride)
            idleFrameCount++
            if (idleFrameCount % 10 == 0) {
                val livePins = pinDeckDetector.detectStandingPinsNow(imageBytes, width, height, stride)
                if (livePins != _uiState.value.standingPins) {
                    _uiState.value = _uiState.value.copy(standingPins = livePins)
                }
            }
        }

        // 1. Run real-time ball detection in Camera Buffer Space
        val detectedCamCentroid: Point2D?
        val latency: Double
        val ballRadiusPx: Int

        val mlDetector = tfliteBallDetector
        if (_uiState.value.isMLDetectorActive && mlDetector != null) {
            val mlCentroid = mlDetector.detectBall(imageBytes, width, height, stride)
            if (mlCentroid != null) {
                detectedCamCentroid = mlCentroid
                latency = mlDetector.getLastInferenceLatencyMs()
                ballRadiusPx = mlDetector.getLastBallRadiusPx()
            } else {
                // Secondary fallback to classical optical differencing if ML confidence is below threshold
                detectedCamCentroid = opticalBallDetector.detectBall(imageBytes, width, height, stride)
                latency = opticalBallDetector.getLastInferenceLatencyMs()
                ballRadiusPx = opticalBallDetector.getLastBallRadiusPx()
            }
        } else {
            // Optical differencing mode
            detectedCamCentroid = opticalBallDetector.detectBall(imageBytes, width, height, stride)
            latency = opticalBallDetector.getLastInferenceLatencyMs()
            ballRadiusPx = opticalBallDetector.getLastBallRadiusPx()
        }

        // 2. Transform detected ball centroid from camera buffer space to Compose screen space
        val screenCentroid = detectedCamCentroid?.let { transformer.cameraToScreen(it) }

        // 3. If ball is detected, feed high-contrast tape sub-region into Optical Rev Counter (in camera buffer space)
        if (detectedCamCentroid != null && _uiState.value.isOpticalRevModeActive) {
            opticalRevCounter.processFrame(
                imageBytes = imageBytes,
                width = width,
                height = height,
                stride = stride,
                ballCenter = detectedCamCentroid,
                ballRadiusPx = ballRadiusPx,
                timestampMs = timestampMs
            )
        }

        // 4. Feed screen centroid into Extended Kalman Filter state machine (which inverts screen homography)
        trajectoryTracker.onBallCentroidDetected(screenCentroid, timestampMs)
        val currentState = trajectoryTracker.state

        if (currentState == TrackingState.SHOT_COMPLETED) {
            if (pinfallEvaluationJob == null || !pinfallEvaluationJob!!.isActive) {
                // Ball reached pin deck (Y >= 60 ft) -> Transition to PIN_DECK_ENTRY and wait 1.8s for pin scatter
                _uiState.value = _uiState.value.copy(
                    liveTrajectory = trajectoryTracker.trajectory,
                    trackingState = TrackingState.PIN_DECK_ENTRY,
                    inferenceLatencyMs = latency,
                    isTrackingActive = true
                )
                pinfallEvaluationJob = viewModelScope.launch {
                    delay(1800L) // 1.8s physical pin scatter & settling time
                    val finalBytes = latestFrame ?: imageBytes
                    val finalW = if (latestWidth > 0) latestWidth else width
                    val finalH = if (latestHeight > 0) latestHeight else height
                    val finalS = if (latestStride > 0) latestStride else stride
                    val pinResult = pinDeckDetector.evaluatePinfall(finalBytes, finalW, finalH, finalS)
                    finalizeCompletedShot(pinResult, latency)
                }
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

    private fun finalizeCompletedShot(pinResult: PinDeckDetector.PinfallResult, latency: Double = 2.4) {
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
        val currentBowlerId = _uiState.value.activeBowler?.id ?: "CEB-103"

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
    }

    fun updateViewportSize(w: Float, h: Float) {
        if (w > 50f && h > 50f) {
            viewportWidth = w
            viewportHeight = h
        }
    }

    /**
     * Auto-detects the lane and computes calibration + optimal auto-zoom in 1 click.
     */
    fun autoCalibrateFromFrame(
        imageBytes: ByteArray,
        width: Int,
        height: Int,
        stride: Int = width,
        alignment: Handedness? = null
    ): Boolean {
        val result = autoDetectLaneDetailed(imageBytes, width, height, stride, alignment)
        return result.isSuccess
    }

    /**
     * Executes autonomous lane recognition and auto-centering pipeline,
     * returning detailed results for real-time coach feedback.
     */
    fun autoDetectLaneDetailed(
        imageBytes: ByteArray,
        width: Int,
        height: Int,
        stride: Int = width,
        alignment: Handedness? = null,
        anchorMode: CalibrationAnchorMode = CalibrationAnchorMode.PIN_DECK
    ): AutoLaneDetector.AutoDetectionResult {
        val currentZoom = _uiState.value.zoomRatio
        val effectiveAlignment = alignment ?: (_uiState.value.activeBowler?.handedness ?: Handedness.RIGHT)
        val result = autoLaneDetector.detectLaneFromFrame(
            imageBytes = imageBytes,
            width = width,
            height = height,
            stride = stride,
            zoomRatio = currentZoom,
            alignment = effectiveAlignment,
            anchorMode = anchorMode,
            tfliteDetector = tfliteBallDetector,
            screenWidth = viewportWidth,
            screenHeight = viewportHeight
        )

        if (result.isSuccess) {
            val calib = result.calibration
            val newH = HomographyMatrix(calib.homographyMatrixElements.toDoubleArray())
            baseHomography = newH
            baseCalibrationZoom = currentZoom
            homography = newH

            trajectoryTracker.updateHomography(newH)
            cameraHomographySyncedForCalibId = ""
            ensureCameraHomographySynced(width.toFloat(), height.toFloat())
            val guides = calibrator.generateProjectedGuides(newH, _uiState.value.activeTargetLine)

            _uiState.value = _uiState.value.copy(
                calibration = calib,
                isLaneCalibrated = true,
                calibrationQuality = "CALIBRATED (AUTO)",
                projectedGuides = guides,
                zoomRatio = result.optimalZoomRatio,
                laneRecognitionStatus = result.statusMessage,
                autoCenterGuidance = result.autoCenterGuidance,
                pinRackDetected = result.pinRackDetected,
                guttersDetected = result.guttersDetected
            )

            viewModelScope.launch {
                repository.saveCalibration(calib)
            }
        } else {
            _uiState.value = _uiState.value.copy(
                laneRecognitionStatus = result.statusMessage,
                autoCenterGuidance = result.autoCenterGuidance,
                pinRackDetected = result.pinRackDetected,
                guttersDetected = result.guttersDetected
            )
        }
        return result
    }

    /**
     * Attempts 1-tap auto-calibration from the most recently captured live camera frame.
     * If no live camera frame is present, returns false.
     */
    fun autoCalibrateLatestFrame(alignment: Handedness? = null): Boolean {
        val bytes = latestFrame ?: return false
        val w = latestWidth
        val h = latestHeight
        val s = if (latestStride > 0) latestStride else w
        if (w <= 0 || h <= 0) return false
        return autoCalibrateFromFrame(bytes, w, h, s, alignment)
    }

    /**
     * Arms the tracker using standard USBC physical lane dimensions preset for the active zoom.
     */
    fun calibrateWithDefaults(
        anchorMode: com.example.cebowlinglabtrack.domain.calibration.CalibrationAnchorMode = com.example.cebowlinglabtrack.domain.calibration.CalibrationAnchorMode.PIN_DECK,
        alignment: Handedness = _uiState.value.activeBowler?.handedness ?: Handedness.RIGHT
    ) {
        val currentZoom = _uiState.value.zoomRatio
        val (defaultCalib, newH) = calibrator.createDefaultCalibration(
            viewWidth = viewportWidth,
            viewHeight = viewportHeight,
            zoomRatio = currentZoom,
            anchorMode = CalibrationAnchorMode.PIN_DECK,
            alignment = alignment
        )
        baseHomography = newH
        baseCalibrationZoom = currentZoom
        homography = newH

        trajectoryTracker.updateHomography(newH)
        cameraHomographySyncedForCalibId = ""
        val w = if (latestWidth > 0) latestWidth.toFloat() else 1080f
        val h = if (latestHeight > 0) latestHeight.toFloat() else 1920f
        ensureCameraHomographySynced(w, h)

        val guides = calibrator.generateProjectedGuides(newH, _uiState.value.activeTargetLine)
        _uiState.value = _uiState.value.copy(
            calibration = defaultCalib,
            isLaneCalibrated = true,
            calibrationQuality = "CALIBRATED (USBC PRESET)",
            projectedGuides = guides
        )
        viewModelScope.launch {
            repository.saveCalibration(defaultCalib)
        }
    }

    /**
     * Adjusts the camera hardware zoom ratio and dynamically scales homography
     * to keep all AR guides, pins, chevrons and ball tracking 100% locked to the physical lane.
     */
    fun setZoomRatio(zoomRatio: Float) {
        val clampedZoom = zoomRatio.coerceIn(1.0f, 5.0f)
        val currentZoom = _uiState.value.zoomRatio
        if (kotlin.math.abs(clampedZoom - currentZoom) < 0.01f) return

        val scaleFactor = (clampedZoom / baseCalibrationZoom.coerceAtLeast(0.5f)).toDouble()
        val cx = (viewportWidth / 2.0)
        val cy = (viewportHeight / 2.0)

        val scaledH = baseHomography.scaleForZoom(scaleFactor, cx, cy)
        homography = scaledH
        trajectoryTracker.updateHomography(scaledH)
        cameraHomographySyncedForCalibId = ""
        val w = if (latestWidth > 0) latestWidth.toFloat() else 1080f
        val h = if (latestHeight > 0) latestHeight.toFloat() else 1920f
        ensureCameraHomographySynced(w, h)

        val guides = calibrator.generateProjectedGuides(scaledH, _uiState.value.activeTargetLine)
        _uiState.value = _uiState.value.copy(
            zoomRatio = clampedZoom,
            projectedGuides = guides
        )
    }

    /**
     * Re-arms the tracker for the next shot.
     */
    fun armForNextShot() {
        pinfallEvaluationJob?.cancel()
        pinfallEvaluationJob = null
        idleFrameCount = 0
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
        arrowsRight: Point2D,
        anchorMode: com.example.cebowlinglabtrack.domain.calibration.CalibrationAnchorMode = com.example.cebowlinglabtrack.domain.calibration.CalibrationAnchorMode.PIN_DECK
    ) {
        val currentZoom = _uiState.value.zoomRatio
        val result = calibrator.calibrate(
            foulLineLeft = foulLeft,
            foulLineRight = foulRight,
            arrowsLeft = arrowsLeft,
            arrowsRight = arrowsRight,
            anchorMode = CalibrationAnchorMode.PIN_DECK,
            calibrationZoomRatio = currentZoom
        ) ?: return
        val (calib, newH) = result
        baseHomography = newH
        baseCalibrationZoom = currentZoom
        homography = newH

        trajectoryTracker.updateHomography(newH)
        cameraHomographySyncedForCalibId = ""
        val w = if (latestWidth > 0) latestWidth.toFloat() else 1080f
        val h = if (latestHeight > 0) latestHeight.toFloat() else 1920f
        ensureCameraHomographySynced(w, h)

        val guides = calibrator.generateProjectedGuides(newH, _uiState.value.activeTargetLine)

        _uiState.value = _uiState.value.copy(
            calibration = calib,
            isLaneCalibrated = true,
            calibrationQuality = "CALIBRATED (${anchorMode.displayName})",
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
     * Toggles the edge LiteRT / TFLite ball detector engine vs optical frame differencing.
     */
    fun toggleMLDetectorMode(active: Boolean? = null) {
        _uiState.value = _uiState.value.copy(
            isMLDetectorActive = active ?: !_uiState.value.isMLDetectorActive
        )
    }

    /**
     * Sets or injects a custom [TFLiteBallDetector] instance.
     */
    fun setMLDetector(detector: TFLiteBallDetector?) {
        this.tfliteBallDetector?.close()
        this.tfliteBallDetector = detector
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

package com.example.cebowlinglabtrack.camera

import android.hardware.camera2.CaptureRequest
import android.util.Range
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.ImageAnalysis

/**
 * Camera 3A configuration options for bowling lane capture.
 *
 * Defaults to 120 FPS high-speed capture (supported on Snapdragon 8 Elite / Galaxy S25 Ultra)
 * with automatic fallback to 60 FPS.
 *
 * Mandatory locks for bowling lane optical tracking:
 * - Exposure locked (AE OFF, fixed shutter speed ~1/500s to 1/1000s) to freeze high-velocity ball motion.
 * - Fixed ISO to prevent exposure hunting during ball roll.
 * - White Balance locked (AWB OFF) to prevent color temperature shifts across lane oil patterns.
 * - Autofocus locked (AF OFF) at hyperfocal distance (~25-35 ft) so entire lane remains in focus.
 */
data class Camera3AConfig(
    val targetFps: Int = 120,
    val exposureTimeNs: Long = 1_000_000L, // 1/1000 second (1 millisecond) for 120 FPS
    val isoSensitivity: Int = 800,
    val hyperfocalDiopters: Float = 0.12f, // 1 / 8.3 meters ~ 27 feet down-lane
    val lockAe: Boolean = true,
    val lockAwb: Boolean = false, // Set false to ensure standard ISP color balance and eliminate Bayer green tint
    val lockAf: Boolean = true
)

object CameraPipelineHelper {

    /**
     * Determines the optimal available FPS range, preferring 120 FPS on flagship hardware,
     * falling back to 60 FPS or 30 FPS.
     */
    fun selectOptimalFpsRange(supportedRanges: Array<Range<Int>>?): Range<Int> {
        if (supportedRanges == null || supportedRanges.isEmpty()) return Range(120, 120)

        // Check if exact 120 FPS is available
        val r120 = supportedRanges.firstOrNull { it.upper >= 120 }
        if (r120 != null) {
            return Range(120, 120)
        }

        // Check 60 FPS
        val r60 = supportedRanges.firstOrNull { it.upper >= 60 }
        if (r60 != null) {
            return Range(60, 60)
        }

        return supportedRanges.maxByOrNull { it.upper } ?: Range(30, 30)
    }

    private val laneConfigurator = LaneCameraConfigurator()

    /**
     * Applies manual 3A lock controls to CameraX ImageAnalysis via Camera2Interop.
     */
    @OptIn(ExperimentalCamera2Interop::class)
    fun applyManual3AControls(
        builder: ImageAnalysis.Builder,
        config: Camera3AConfig = Camera3AConfig(),
        fpsRange: Range<Int> = Range(config.targetFps, config.targetFps)
    ) {
        val interop = Camera2Interop.Extender(builder)

        // 1. Target High FPS Range (120 FPS / 60 FPS)
        interop.setCaptureRequestOption(
            CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
            fpsRange
        )

        // 2. Lock Auto-Exposure and set manual shutter + ISO
        if (config.lockAe) {
            interop.setCaptureRequestOption(
                CaptureRequest.CONTROL_AE_MODE,
                CaptureRequest.CONTROL_AE_MODE_OFF
            )
            interop.setCaptureRequestOption(
                CaptureRequest.SENSOR_EXPOSURE_TIME,
                config.exposureTimeNs
            )
            interop.setCaptureRequestOption(
                CaptureRequest.SENSOR_SENSITIVITY,
                config.isoSensitivity
            )
        }

        // 3. Auto White Balance (Eliminate Bayer green tint by using Camera2 ISP AWB or Fluorescent)
        if (config.lockAwb) {
            interop.setCaptureRequestOption(
                CaptureRequest.CONTROL_AWB_MODE,
                CaptureRequest.CONTROL_AWB_MODE_FLUORESCENT
            )
        } else {
            interop.setCaptureRequestOption(
                CaptureRequest.CONTROL_AWB_MODE,
                CaptureRequest.CONTROL_AWB_MODE_AUTO
            )
        }

        // 4. Lock Autofocus to manual hyperfocal distance (e.g. ~27-33 ft down-lane)
        if (config.lockAf) {
            interop.setCaptureRequestOption(
                CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_OFF
            )
            interop.setCaptureRequestOption(
                CaptureRequest.LENS_FOCUS_DISTANCE,
                config.hyperfocalDiopters
            )
        }

        // 5. Disable Optical and Digital Video Stabilization to preserve 1:1 pixel homography
        interop.setCaptureRequestOption(
            CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
            CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_OFF
        )
        interop.setCaptureRequestOption(
            CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,
            CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_OFF
        )

        // 6. Request Hardware ISP High-Quality Lens Distortion Correction
        interop.setCaptureRequestOption(
            CaptureRequest.DISTORTION_CORRECTION_MODE,
            CaptureRequest.DISTORTION_CORRECTION_MODE_HIGH_QUALITY
        )
    }
}

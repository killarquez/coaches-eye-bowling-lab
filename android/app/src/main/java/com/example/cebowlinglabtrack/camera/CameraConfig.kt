package com.example.cebowlinglabtrack.camera

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.util.Range
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.ImageAnalysis

/**
 * Camera 3A configuration options for bowling lane capture.
 *
 * Mandatory locks for bowling lane optical tracking:
 * - Exposure locked (AE OFF, fixed shutter speed ~1/500s to 1/1000s) to freeze high-velocity ball motion.
 * - Fixed ISO to prevent exposure hunting during ball roll.
 * - White Balance locked (AWB OFF) to prevent color temperature shifts across lane oil patterns.
 * - Autofocus locked (AF OFF) at hyperfocal distance (~25-35 ft) so entire lane remains in focus.
 */
data class Camera3AConfig(
    val targetFps: Int = 60,
    val exposureTimeNs: Long = 2_000_000L, // 1/500 second (2 milliseconds)
    val isoSensitivity: Int = 800,
    val hyperfocalDiopters: Float = 0.12f, // 1 / 8.3 meters ~ 27 feet down-lane
    val lockAe: Boolean = true,
    val lockAwb: Boolean = true,
    val lockAf: Boolean = true
)

object CameraPipelineHelper {

    /**
     * Applies manual 3A lock controls to CameraX ImageAnalysis via Camera2Interop.
     */
    @OptIn(ExperimentalCamera2Interop::class)
    fun applyManual3AControls(
        builder: ImageAnalysis.Builder,
        config: Camera3AConfig = Camera3AConfig()
    ) {
        val interop = Camera2Interop.Extender(builder)

        // 1. Target High FPS Range (e.g. 60 FPS)
        interop.setCaptureRequestOption(
            CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
            Range(config.targetFps, config.targetFps)
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

        // 3. Lock Auto White Balance
        if (config.lockAwb) {
            interop.setCaptureRequestOption(
                CaptureRequest.CONTROL_AWB_MODE,
                CaptureRequest.CONTROL_AWB_MODE_OFF
            )
        }

        // 4. Lock Autofocus to manual hyperfocal distance
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
    }
}

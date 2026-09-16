package com.example.cebowlinglabtrack.camera

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.util.Range
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.ImageAnalysis

/**
 * Camera2 High-Speed Manual Exposure and Hardware Control Pipeline.
 *
 * Locks exposure time (<= 1/666s or 1/1000s) to eliminate high-velocity ball motion blur,
 * clamps ISO to indoor synthetic lane levels, locks hyperfocal AF, disables optical/video
 * stabilization to prevent homography drift, and enables hardware ISP lens distortion correction.
 */
class LaneCameraConfigurator {

    /**
     * Applies locked manual exposure settings optimized for high-speed bowling ball tracking
     * on standard Camera2 CaptureRequest.Builder instances.
     *
     * @param targetFps 120 or 240 depending on device hardware capabilities
     * @param exposureTimeNanos Target shutter speed (e.g. 1_000_000L = 1/1000s, 1_500_000L = 1/666s, 2_000_000L = 1/500s)
     * @param iso Target sensor sensitivity (e.g. 800..1600 for indoor synthetic lanes)
     */
    fun configureHighSpeedCaptureRequest(
        builder: CaptureRequest.Builder,
        characteristics: CameraCharacteristics? = null,
        targetFps: Int = 120,
        exposureTimeNanos: Long = 1_500_000L, // 1/666 second shutter
        iso: Int = 1000
    ) {
        // 1. Lock Auto Exposure to OFF for manual control
        builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)

        // Clamp exposure time within hardware limits
        val exposureRange = characteristics?.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
        val clampedExposure = exposureRange?.let {
            exposureTimeNanos.coerceIn(it.lower, it.upper)
        } ?: exposureTimeNanos
        builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, clampedExposure)

        // Clamp ISO sensitivity within hardware limits
        val isoRange = characteristics?.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
        val clampedIso = isoRange?.let {
            iso.coerceIn(it.lower, it.upper)
        } ?: iso
        builder.set(CaptureRequest.SENSOR_SENSITIVITY, clampedIso)

        // 2. Lock High Frame Rate Target Range
        builder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, Range(targetFps, targetFps))

        // 3. Lock Focus to Hyperfocal / Manual (Infinite lane depth)
        builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
        // Focus distance in diopters (1 / distance_in_meters). 0.0f = infinity, 0.1f = 10 meters downlane
        builder.set(CaptureRequest.LENS_FOCUS_DISTANCE, 0.1f)

        // 4. Disable Optical & Digital Video Stabilization to preserve 1:1 pixel homography
        builder.set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_OFF)
        builder.set(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE, CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_OFF)

        // 5. Enable hardware ISP distortion correction
        builder.set(CaptureRequest.DISTORTION_CORRECTION_MODE, CaptureRequest.DISTORTION_CORRECTION_MODE_HIGH_QUALITY)

        // 6. Lock Auto White Balance to standard indoor fluorescent/LED
        builder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_FLUORESCENT)
    }

    /**
     * CameraX ImageAnalysis Extender overload for binding manual 3A controls to CameraX pipelines.
     */
    @OptIn(ExperimentalCamera2Interop::class)
    fun configureImageAnalysisBuilder(
        builder: ImageAnalysis.Builder,
        characteristics: CameraCharacteristics? = null,
        targetFps: Int = 120,
        exposureTimeNanos: Long = 1_500_000L,
        iso: Int = 1000
    ) {
        val interop = Camera2Interop.Extender(builder)

        // 1. Lock AE
        interop.setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)

        val exposureRange = characteristics?.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
        val clampedExposure = exposureRange?.let { exposureTimeNanos.coerceIn(it.lower, it.upper) } ?: exposureTimeNanos
        interop.setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, clampedExposure)

        val isoRange = characteristics?.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
        val clampedIso = isoRange?.let { iso.coerceIn(it.lower, it.upper) } ?: iso
        interop.setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, clampedIso)

        // 2. High FPS Range
        interop.setCaptureRequestOption(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, Range(targetFps, targetFps))

        // 3. Hyperfocal Focus
        interop.setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
        interop.setCaptureRequestOption(CaptureRequest.LENS_FOCUS_DISTANCE, 0.1f)

        // 4. Disable Stabilization to preserve homography
        interop.setCaptureRequestOption(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_OFF)
        interop.setCaptureRequestOption(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE, CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_OFF)

        // 5. ISP Lens Distortion Correction
        interop.setCaptureRequestOption(CaptureRequest.DISTORTION_CORRECTION_MODE, CaptureRequest.DISTORTION_CORRECTION_MODE_HIGH_QUALITY)

        // 6. White Balance
        interop.setCaptureRequestOption(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_FLUORESCENT)
    }
}

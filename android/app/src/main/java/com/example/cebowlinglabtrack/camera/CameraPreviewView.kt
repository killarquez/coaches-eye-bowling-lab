package com.example.cebowlinglabtrack.camera

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.util.Range
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.cebowlinglabtrack.theme.DarkBackground
import com.example.cebowlinglabtrack.theme.DarkCardBorder
import com.example.cebowlinglabtrack.theme.DarkSurface
import com.example.cebowlinglabtrack.theme.NeonCyan
import com.example.cebowlinglabtrack.theme.NeonStrikeGreen
import com.example.cebowlinglabtrack.theme.TextMuted
import com.example.cebowlinglabtrack.theme.TextPrimary
import com.example.cebowlinglabtrack.theme.TextSecondary
import java.util.concurrent.Executors

/**
 * Live CameraX Preview Composable with 120 FPS high-speed support,
 * hardware zoom control, and frame dispatch to optical CV analyzers.
 */
@Composable
fun CameraPreviewView(
    onFrameAvailable: ((imageBytes: ByteArray, width: Int, height: Int, stride: Int, timestampMs: Long) -> Unit)? = null,
    zoomRatio: Float = 1.0f,
    targetFps: Int = 120,
    modifier: Modifier = Modifier,
    overlayContent: @Composable BoxScope.() -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var cameraInstance by remember { mutableStateOf<Camera?>(null) }

    // Respond to zoom ratio changes
    LaunchedEffect(zoomRatio, cameraInstance) {
        try {
            val cam = cameraInstance ?: return@LaunchedEffect
            val zoomState = cam.cameraInfo.zoomState.value
            val minZ = zoomState?.minZoomRatio ?: 1.0f
            val maxZ = zoomState?.maxZoomRatio ?: 3.5f
            cam.cameraControl.setZoomRatio(zoomRatio.coerceIn(minZ, maxZ))
        } catch (e: Throwable) {
            Log.e("CameraPreviewView", "setZoomRatio error: ${e.message}")
        }
    }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val rotationBufferHolder = remember { RotationBufferHolder() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }

                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()

                            val preview = Preview.Builder()
                                .build()
                                .also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }

                            val analysisBuilder = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)

                            // Apply fixed 3A locks (shutter ~1/1000s, 120 FPS / 60 FPS range)
                            val fpsRange = Range(targetFps, targetFps)
                            CameraPipelineHelper.applyManual3AControls(
                                builder = analysisBuilder,
                                config = Camera3AConfig(targetFps = targetFps),
                                fpsRange = fpsRange
                            )

                            val imageAnalysis = analysisBuilder.build()

                            if (onFrameAvailable != null) {
                                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                    try {
                                        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                                        val srcW = imageProxy.width
                                        val srcH = imageProxy.height
                                        val yPlane = imageProxy.planes[0]
                                        val yBuffer = yPlane.buffer
                                        val srcStride = yPlane.rowStride
                                        val timestampMs = imageProxy.imageInfo.timestamp / 1_000_000L

                                        when (rotationDegrees) {
                                            90 -> {
                                                // 90° Clockwise Rotation: (W_src, H_src) -> (H_src, W_src)
                                                val outW = srcH
                                                val outH = srcW
                                                val totalPixels = outW * outH
                                                if (rotationBufferHolder.rotatedBytes == null || rotationBufferHolder.rotatedBytes!!.size != totalPixels) {
                                                    rotationBufferHolder.rotatedBytes = ByteArray(totalPixels)
                                                }
                                                val out = rotationBufferHolder.rotatedBytes!!
                                                for (ySrc in 0 until srcH) {
                                                    val srcRowOffset = ySrc * srcStride
                                                    val xOut = (srcH - 1) - ySrc
                                                    for (xSrc in 0 until srcW) {
                                                        val pixel = yBuffer.get(srcRowOffset + xSrc)
                                                        val outIdx = xSrc * outW + xOut
                                                        out[outIdx] = pixel
                                                    }
                                                }
                                                onFrameAvailable(out, outW, outH, outW, timestampMs)
                                            }
                                            270 -> {
                                                // 270° Clockwise Rotation: (W_src, H_src) -> (H_src, W_src)
                                                val outW = srcH
                                                val outH = srcW
                                                val totalPixels = outW * outH
                                                if (rotationBufferHolder.rotatedBytes == null || rotationBufferHolder.rotatedBytes!!.size != totalPixels) {
                                                    rotationBufferHolder.rotatedBytes = ByteArray(totalPixels)
                                                }
                                                val out = rotationBufferHolder.rotatedBytes!!
                                                for (ySrc in 0 until srcH) {
                                                    val srcRowOffset = ySrc * srcStride
                                                    val xOut = ySrc
                                                    for (xSrc in 0 until srcW) {
                                                        val pixel = yBuffer.get(srcRowOffset + xSrc)
                                                        val yOut = (srcW - 1) - xSrc
                                                        val outIdx = yOut * outW + xOut
                                                        out[outIdx] = pixel
                                                    }
                                                }
                                                onFrameAvailable(out, outW, outH, outW, timestampMs)
                                            }
                                            180 -> {
                                                val totalPixels = srcW * srcH
                                                if (rotationBufferHolder.rawBytes == null || rotationBufferHolder.rawBytes!!.size != totalPixels) {
                                                    rotationBufferHolder.rawBytes = ByteArray(totalPixels)
                                                }
                                                val out = rotationBufferHolder.rawBytes!!
                                                for (ySrc in 0 until srcH) {
                                                    val srcRowOffset = ySrc * srcStride
                                                    val yOut = (srcH - 1) - ySrc
                                                    val outRowOffset = yOut * srcW
                                                    for (xSrc in 0 until srcW) {
                                                        val xOut = (srcW - 1) - xSrc
                                                        out[outRowOffset + xOut] = yBuffer.get(srcRowOffset + xSrc)
                                                    }
                                                }
                                                onFrameAvailable(out, srcW, srcH, srcW, timestampMs)
                                            }
                                            else -> {
                                                // 0°: Direct contiguous copy (packed stride)
                                                val totalPixels = srcW * srcH
                                                if (rotationBufferHolder.rawBytes == null || rotationBufferHolder.rawBytes!!.size != totalPixels) {
                                                    rotationBufferHolder.rawBytes = ByteArray(totalPixels)
                                                }
                                                val out = rotationBufferHolder.rawBytes!!
                                                if (srcStride == srcW) {
                                                    yBuffer.rewind()
                                                    yBuffer.get(out, 0, totalPixels)
                                                } else {
                                                    for (ySrc in 0 until srcH) {
                                                        val srcRowOffset = ySrc * srcStride
                                                        val outRowOffset = ySrc * srcW
                                                        for (xSrc in 0 until srcW) {
                                                            out[outRowOffset + xSrc] = yBuffer.get(srcRowOffset + xSrc)
                                                        }
                                                    }
                                                }
                                                onFrameAvailable(out, srcW, srcH, srcW, timestampMs)
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Log.e("CameraPreviewView", "Error in frame analysis", e)
                                    } finally {
                                        imageProxy.close()
                                    }
                                }
                            }

                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                            cameraProvider.unbindAll()
                            val boundCamera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageAnalysis
                            )
                            cameraInstance = boundCamera
                            boundCamera.cameraControl.setZoomRatio(zoomRatio.coerceIn(1.0f, 5.0f))
                        } catch (e: Exception) {
                            Log.e("CameraPreviewView", "Failed to bind camera use cases", e)
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Permission request prompt UI
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkBackground)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(NeonCyan.copy(alpha = 0.15f))
                        .border(1.dp, NeonCyan, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = "Camera",
                        tint = NeonCyan,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "CAMERA PERMISSION REQUIRED",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "CE Bowling Lab needs camera access to perform live 120 FPS optical tracking, auto-lane calibration, and shot video replay on the approach.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(28.dp))

                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonStrikeGreen),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(0.8f).height(50.dp)
                ) {
                    Text(
                        text = "GRANT CAMERA ACCESS",
                        color = Color.Black,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Overlay composable content (AR guides, HUD, handles)
        overlayContent()
    }
}

private class RotationBufferHolder {
    var rotatedBytes: ByteArray? = null
    var rawBytes: ByteArray? = null
}

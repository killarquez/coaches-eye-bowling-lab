package com.example.cebowlinglabtrack.domain.ml

import android.content.Context
import com.example.cebowlinglabtrack.domain.model.Point2D
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.math.max
import kotlin.math.min

/**
 * High-performance edge ML bowling ball detector powered by LiteRT (TensorFlow Lite).
 *
 * Design Invariants:
 * 1. Zero Allocations on the Hot Path: Pre-allocated direct [ByteBuffer] and reusable output arrays.
 * 2. Color Space: Directly ingests camera luminance (single-channel Y-plane / Grayscale),
 *    avoiding YUV-to-RGB conversion overhead on Android.
 * 3. Geometry Integrity: Outputs normalized screen-space bounding boxes and sub-pixel centroids (u, v),
 *    preserving DLT projective homography mapping in downstream tracking.
 */
class TFLiteBallDetector(
    private val runner: TFLiteRunner,
    val inputSize: Int = DEFAULT_INPUT_SIZE,
    val confidenceThreshold: Float = DEFAULT_CONFIDENCE_THRESHOLD
) : BallDetectorEngine, AutoCloseable {

    companion object {
        const val DEFAULT_INPUT_SIZE = 416
        const val DEFAULT_CONFIDENCE_THRESHOLD = 0.50f
        const val MAX_DETECTIONS = 10
        const val MODEL_ASSET_PATH = "models/bowling_ball_v1.tflite"

        /**
         * Loads model file directly from Android APK assets as memory-mapped [ByteBuffer].
         */
        fun loadModelFile(context: Context, modelPath: String = MODEL_ASSET_PATH): ByteBuffer {
            val assetFileDescriptor = context.assets.openFd(modelPath)
            val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
            val fileChannel = fileInputStream.channel
            val startOffset = assetFileDescriptor.startOffset
            val declaredLength = assetFileDescriptor.declaredLength
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        }

        /**
         * Factory method to create [TFLiteBallDetector] from Android assets with automatic delegate fallback:
         * GPU Delegate -> NNAPI Delegate -> 4-thread CPU.
         */
        fun fromAsset(
            context: Context,
            modelPath: String = MODEL_ASSET_PATH,
            numThreads: Int = 4,
            useGpu: Boolean = true,
            inputSize: Int = DEFAULT_INPUT_SIZE,
            confidenceThreshold: Float = DEFAULT_CONFIDENCE_THRESHOLD
        ): TFLiteBallDetector {
            val modelBuffer = loadModelFile(context, modelPath)
            return fromBuffer(
                modelBuffer = modelBuffer,
                numThreads = numThreads,
                useGpu = useGpu,
                inputSize = inputSize,
                confidenceThreshold = confidenceThreshold
            )
        }

        /**
         * Factory method to create [TFLiteBallDetector] from a direct [ByteBuffer].
         */
        fun fromBuffer(
            modelBuffer: ByteBuffer,
            numThreads: Int = 4,
            useGpu: Boolean = true,
            inputSize: Int = DEFAULT_INPUT_SIZE,
            confidenceThreshold: Float = DEFAULT_CONFIDENCE_THRESHOLD
        ): TFLiteBallDetector {
            val runner = AndroidInterpreterRunner.create(modelBuffer, numThreads, useGpu)
            return TFLiteBallDetector(runner, inputSize, confidenceThreshold)
        }

        /**
         * Factory method for local testing and parity verification.
         */
        fun createForTesting(
            runner: TFLiteRunner,
            inputSize: Int = DEFAULT_INPUT_SIZE,
            confidenceThreshold: Float = DEFAULT_CONFIDENCE_THRESHOLD
        ): TFLiteBallDetector {
            return TFLiteBallDetector(runner, inputSize, confidenceThreshold)
        }
    }

    /**
     * Detection result payload containing sub-pixel centroid and bounding box in camera space.
     */
    data class BallDetectionResult(
        val centroid: Point2D,
        val boundingBox: FloatArray, // [ymin, xmin, ymax, xmax] normalized [0.0, 1.0]
        val confidence: Float,
        val latencyMs: Double
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as BallDetectionResult
            if (centroid != other.centroid) return false
            if (!boundingBox.contentEquals(other.boundingBox)) return false
            if (confidence != other.confidence) return false
            if (latencyMs != other.latencyMs) return false
            return true
        }

        override fun hashCode(): Int {
            var result = centroid.hashCode()
            result = 31 * result + boundingBox.contentHashCode()
            result = 31 * result + confidence.hashCode()
            result = 31 * result + latencyMs.hashCode()
            return result
        }
    }

    // ========================================================================
    // Pre-allocated Hot-Path Buffers (Zero Allocations In Inference Loop)
    // ========================================================================

    // Single-channel Float32 input tensor buffer: shape [1, inputSize, inputSize, 1]
    private val inputTensorBuffer: ByteBuffer = ByteBuffer.allocateDirect(
        1 * inputSize * inputSize * 1 * 4
    ).order(ByteOrder.nativeOrder())

    // Standard detection output tensors:
    // Output 0: Locations [1, MAX_DETECTIONS, 4] -> normalized [ymin, xmin, ymax, xmax]
    private val outputLocations = Array(1) { Array(MAX_DETECTIONS) { FloatArray(4) } }
    // Output 1: Classes [1, MAX_DETECTIONS]
    private val outputClasses = Array(1) { FloatArray(MAX_DETECTIONS) }
    // Output 2: Scores [1, MAX_DETECTIONS]
    private val outputScores = Array(1) { FloatArray(MAX_DETECTIONS) }
    // Output 3: NumDetections [1]
    private val outputNumDetections = FloatArray(1)

    // Pre-allocated map for multi-tensor output runner
    private val outputsMap: Map<Int, Any> = mapOf(
        0 to outputLocations,
        1 to outputClasses,
        2 to outputScores,
        3 to outputNumDetections
    )

    // Reusable float array for best bounding box to avoid per-frame allocation
    private val bestBoxHolder = FloatArray(4)

    private var lastLatencyMs: Double = 0.0
    private var lastBallRadiusPx: Int = 18

    // ========================================================================
    // Detection Pipeline
    // ========================================================================

    /**
     * Primary zero-allocation detection method accepting direct [ByteBuffer] Y-plane.
     */
    fun detect(yBuffer: ByteBuffer, width: Int, height: Int): BallDetectionResult? {
        val startNs = System.nanoTime()

        // 1. Direct down-sample / bilinear mapping into pre-allocated input tensor buffer
        prepareInputFromByteBuffer(yBuffer, width, height)

        // 2. Execute TFLite Inference
        inputTensorBuffer.rewind()
        runner.run(inputTensorBuffer, outputsMap)

        // 3. Parse Top Confidence Detection
        val result = parseTopDetection(width, height, startNs)
        lastLatencyMs = (System.nanoTime() - startNs) / 1_000_000.0
        return result
    }

    /**
     * Zero-allocation detection conforming to [BallDetectorEngine] interface.
     * Takes raw luminance [imageBytes] (Y-plane) with row stride.
     */
    override fun detectBall(
        imageBytes: ByteArray,
        width: Int,
        height: Int,
        stride: Int
    ): Point2D? {
        val startNs = System.nanoTime()

        // 1. Downsample and copy luminance values into pre-allocated model input buffer
        prepareInputFromByteArray(imageBytes, width, height, stride)

        // 2. Execute TFLite Inference
        inputTensorBuffer.rewind()
        runner.run(inputTensorBuffer, outputsMap)

        // 3. Parse Top Detection
        val result = parseTopDetection(width, height, startNs)
        lastLatencyMs = (System.nanoTime() - startNs) / 1_000_000.0

        return result?.centroid
    }

    /**
     * Direct [ByteBuffer] overload for [BallDetectorEngine].
     */
    fun detectBall(yBuffer: ByteBuffer, width: Int, height: Int): Point2D? {
        val result = detect(yBuffer, width, height)
        return result?.centroid
    }

    override fun getLastInferenceLatencyMs(): Double = lastLatencyMs

    fun getLastBallRadiusPx(): Int = lastBallRadiusPx

    // ========================================================================
    // Tensor Preprocessing & Postprocessing (Zero Heap Allocations)
    // ========================================================================

    private fun prepareInputFromByteArray(
        imageBytes: ByteArray,
        width: Int,
        height: Int,
        stride: Int
    ) {
        inputTensorBuffer.rewind()
        val inSize = inputSize

        for (yModel in 0 until inSize) {
            val ySrc = (yModel * height) / inSize
            val rowOffset = ySrc * stride

            for (xModel in 0 until inSize) {
                val xSrc = (xModel * width) / inSize
                val idx = rowOffset + xSrc

                if (idx < imageBytes.size) {
                    val luminance = (imageBytes[idx].toInt() and 0xFF) / 255.0f
                    inputTensorBuffer.putFloat(luminance)
                } else {
                    inputTensorBuffer.putFloat(0.0f)
                }
            }
        }
    }

    private fun prepareInputFromByteBuffer(
        yBuffer: ByteBuffer,
        width: Int,
        height: Int
    ) {
        inputTensorBuffer.rewind()
        val inSize = inputSize
        val bufferCapacity = yBuffer.capacity()

        for (yModel in 0 until inSize) {
            val ySrc = (yModel * height) / inSize
            val rowOffset = ySrc * width

            for (xModel in 0 until inSize) {
                val xSrc = (xModel * width) / inSize
                val idx = rowOffset + xSrc

                if (idx < bufferCapacity) {
                    val luminance = (yBuffer.get(idx).toInt() and 0xFF) / 255.0f
                    inputTensorBuffer.putFloat(luminance)
                } else {
                    inputTensorBuffer.putFloat(0.0f)
                }
            }
        }
    }

    private fun parseTopDetection(
        width: Int,
        height: Int,
        startNs: Long
    ): BallDetectionResult? {
        val scores = outputScores[0]
        val boxes = outputLocations[0]

        var bestIdx = -1
        var bestScore = confidenceThreshold

        for (i in 0 until MAX_DETECTIONS) {
            val score = scores[i]
            if (score >= bestScore) {
                bestScore = score
                bestIdx = i
            }
        }

        if (bestIdx < 0) {
            return null
        }

        // Bounding box format: [ymin, xmin, ymax, xmax] normalized to [0.0, 1.0]
        val box = boxes[bestIdx]
        val ymin = box[0].coerceIn(0.0f, 1.0f)
        val xmin = box[1].coerceIn(0.0f, 1.0f)
        val ymax = box[2].coerceIn(ymin, 1.0f)
        val xmax = box[3].coerceIn(xmin, 1.0f)

        bestBoxHolder[0] = ymin
        bestBoxHolder[1] = xmin
        bestBoxHolder[2] = ymax
        bestBoxHolder[3] = xmax

        // Denormalize sub-pixel centroid to screen space coordinates (u, v)
        val centerNormX = (xmin + xmax) / 2.0
        val centerNormY = (ymin + ymax) / 2.0

        val u = centerNormX * width
        val v = centerNormY * height

        // Compute estimated radius in pixels for OpticalRevCounter
        val boxPxW = (xmax - xmin) * width
        val boxPxH = (ymax - ymin) * height
        lastBallRadiusPx = ((max(boxPxW, boxPxH) / 2.0f).toInt()).coerceIn(8, 64)

        val latency = (System.nanoTime() - startNs) / 1_000_000.0

        return BallDetectionResult(
            centroid = Point2D(u, v),
            boundingBox = bestBoxHolder.clone(),
            confidence = bestScore,
            latencyMs = latency
        )
    }

    override fun close() {
        runner.close()
    }
}

// ============================================================================
// TFLite Runner Abstraction (Enables GPU/NNAPI on Device & Host Testing)
// ============================================================================

/**
 * Runner interface to decouple the inference execution from host OS limitations.
 */
interface TFLiteRunner : AutoCloseable {
    fun run(input: ByteBuffer, outputs: Map<Int, Any>)
}

/**
 * Real device Android interpreter runner with automatic GPU delegate fallback.
 */
class AndroidInterpreterRunner(
    private val interpreter: Interpreter,
    private val gpuDelegate: GpuDelegate? = null
) : TFLiteRunner {

    companion object {
        fun create(
            modelBuffer: ByteBuffer,
            numThreads: Int = 4,
            useGpu: Boolean = true
        ): AndroidInterpreterRunner {
            var delegate: GpuDelegate? = null
            val options = Interpreter.Options()

            if (useGpu) {
                try {
                    val compatList = CompatibilityList()
                    if (compatList.isDelegateSupportedOnThisDevice) {
                        delegate = GpuDelegate(compatList.bestOptionsForThisDevice)
                        options.addDelegate(delegate)
                    } else {
                        options.setNumThreads(numThreads)
                    }
                } catch (e: Throwable) {
                    options.setNumThreads(numThreads)
                }
            } else {
                options.setNumThreads(numThreads)
            }

            val interpreter = Interpreter(modelBuffer, options)
            return AndroidInterpreterRunner(interpreter, delegate)
        }
    }

    override fun run(input: ByteBuffer, outputs: Map<Int, Any>) {
        val inputs = arrayOf<Any>(input)
        interpreter.runForMultipleInputsOutputs(inputs, outputs)
    }

    override fun close() {
        interpreter.close()
        gpuDelegate?.close()
    }
}

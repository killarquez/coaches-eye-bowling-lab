package com.example.cebowlinglabtrack

import com.example.cebowlinglabtrack.domain.calibration.LaneCalibrator
import com.example.cebowlinglabtrack.domain.ml.BallDetectorEngine
import com.example.cebowlinglabtrack.domain.ml.OpticalBallDetector
import com.example.cebowlinglabtrack.domain.ml.TFLiteBallDetector
import com.example.cebowlinglabtrack.domain.ml.TFLiteRunner
import com.example.cebowlinglabtrack.domain.model.Point2D
import com.example.cebowlinglabtrack.domain.tracking.TrajectoryTracker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class TFLiteBallDetectorTest {

    /**
     * Controllable synthetic TFLite runner for deterministic unit testing on host JVM.
     */
    private class MockTFLiteRunner(
        private val mockBoxes: Array<FloatArray> = arrayOf(floatArrayOf(0.70f, 0.48f, 0.74f, 0.52f)),
        private val mockScores: FloatArray = floatArrayOf(0.92f),
        private val mockClasses: FloatArray = floatArrayOf(0.0f)
    ) : TFLiteRunner {
        var isClosed = false
        var runInvocationCount = 0
        var lastInputCapacity = 0

        override fun run(input: ByteBuffer, outputs: Map<Int, Any>) {
            runInvocationCount++
            lastInputCapacity = input.capacity()

            @Suppress("UNCHECKED_CAST")
            val outputLocations = outputs[0] as? Array<Array<FloatArray>>
            @Suppress("UNCHECKED_CAST")
            val outputClasses = outputs[1] as? Array<FloatArray>
            @Suppress("UNCHECKED_CAST")
            val outputScores = outputs[2] as? Array<FloatArray>
            val outputNum = outputs[3] as? FloatArray

            if (outputLocations != null && outputScores != null) {
                // Populate mock detections
                val numDet = mockBoxes.size.coerceAtMost(TFLiteBallDetector.MAX_DETECTIONS)
                for (i in 0 until numDet) {
                    outputLocations[0][i][0] = mockBoxes[i][0]
                    outputLocations[0][i][1] = mockBoxes[i][1]
                    outputLocations[0][i][2] = mockBoxes[i][2]
                    outputLocations[0][i][3] = mockBoxes[i][3]
                    outputScores[0][i] = mockScores.getOrElse(i) { 0.0f }
                    outputClasses?.get(0)?.set(i, mockClasses.getOrElse(i) { 0.0f })
                }
                outputNum?.set(0, numDet.toFloat())
            }
        }

        override fun close() {
            isClosed = true
        }
    }

    /**
     * Synthetic runner simulating YOLOv8 tensor output shape [1, 5, anchors].
     */
    private class MockYoloRunner(
        private val mockCx: Float = 208f,
        private val mockCy: Float = 312f,
        private val mockW: Float = 20.8f,
        private val mockH: Float = 20.8f,
        private val mockConf: Float = 0.88f,
        private val anchorCount: Int = 3549
    ) : TFLiteRunner {
        var isClosed = false
        var runCount = 0

        override fun isYoloFormat(): Boolean = true
        override fun getYoloAnchorCount(): Int = anchorCount
        override fun getInputChannels(): Int = 3

        override fun run(input: ByteBuffer, outputs: Map<Int, Any>) {
            runCount++
            @Suppress("UNCHECKED_CAST")
            val yoloOut = outputs[0] as? Array<Array<FloatArray>>
            if (yoloOut != null) {
                // Set anchor 50 to mock detection values
                yoloOut[0][0][50] = mockCx
                yoloOut[0][1][50] = mockCy
                yoloOut[0][2][50] = mockW
                yoloOut[0][3][50] = mockH
                yoloOut[0][4][50] = mockConf
            }
        }

        override fun close() {
            isClosed = true
        }
    }

    @Test
    fun testInitializationAndCleanClosure() {
        val mockRunner = MockTFLiteRunner()
        val detector = TFLiteBallDetector.createForTesting(mockRunner)

        assertFalse("Runner should not be closed on start", mockRunner.isClosed)
        detector.close()
        assertTrue("Runner must be closed cleanly to release GPU/native handles", mockRunner.isClosed)
    }

    @Test
    fun testZeroAllocationInputMappingFromByteArray() {
        val mockRunner = MockTFLiteRunner()
        val detector = TFLiteBallDetector.createForTesting(mockRunner, inputSize = 416)

        val width = 1080
        val height = 1920
        val imageBytes = ByteArray(width * height) { (it % 255).toByte() }

        // Execute detection
        val centroid = detector.detectBall(imageBytes, width, height, stride = width)
        assertNotNull("Centroid should be detected from mock detections", centroid)
        assertEquals(1, mockRunner.runInvocationCount)
        assertEquals(416 * 416 * 4, mockRunner.lastInputCapacity)
    }

    @Test
    fun testDirectByteBufferDetectionAndSubpixelAccuracy() {
        // Mock a bounding box in normalized coordinates:
        // ymin = 0.70, xmin = 0.45, ymax = 0.76, xmax = 0.55
        // Expected centroid: cx = 0.50, cy = 0.73
        val mockBoxes = arrayOf(floatArrayOf(0.70f, 0.45f, 0.76f, 0.55f))
        val mockScores = floatArrayOf(0.88f)
        val mockRunner = MockTFLiteRunner(mockBoxes, mockScores)
        val detector = TFLiteBallDetector.createForTesting(mockRunner)

        val width = 1080
        val height = 1920
        val directBuffer = ByteBuffer.allocateDirect(width * height)
        for (i in 0 until (width * height)) {
            directBuffer.put(128.toByte())
        }
        directBuffer.rewind()

        val result = detector.detect(directBuffer, width, height)
        assertNotNull("Should detect ball from direct ByteBuffer", result)
        assertEquals(0.88f, result!!.confidence, 0.001f)

        val expectedScreenX = 0.50 * width   // 540.0
        val expectedScreenY = 0.73 * height  // 1401.6

        assertEquals(expectedScreenX, result.centroid.x, 0.01)
        assertEquals(expectedScreenY, result.centroid.y, 0.01)
        assertTrue("Latency should be non-negative", result.latencyMs >= 0.0)
    }

    @Test
    fun testLowConfidenceRejection() {
        // Mock detection below threshold (0.42 < 0.50)
        val mockBoxes = arrayOf(floatArrayOf(0.50f, 0.50f, 0.60f, 0.60f))
        val mockScores = floatArrayOf(0.42f)
        val mockRunner = MockTFLiteRunner(mockBoxes, mockScores)
        val detector = TFLiteBallDetector.createForTesting(mockRunner, confidenceThreshold = 0.50f)

        val imageBytes = ByteArray(640 * 480) { 100.toByte() }
        val centroid = detector.detectBall(imageBytes, 640, 480)
        assertNull("Detections below confidence threshold must return null", centroid)
    }

    @Test
    fun testParityComparisonWithOpticalBallDetector() {
        // Setup shared lane homography
        val calibrator = LaneCalibrator()
        val foulLeft = Point2D(100.0, 1800.0)
        val foulRight = Point2D(980.0, 1800.0)
        val arrowsLeft = Point2D(300.0, 1000.0)
        val arrowsRight = Point2D(780.0, 1000.0)
        val (_, homography) = calibrator.calibrate(foulLeft, foulRight, arrowsLeft, arrowsRight)!!

        val opticalDetector = OpticalBallDetector()
        opticalDetector.updateHomography(homography)

        val width = 1080
        val height = 1920
        val stride = width
        val size = width * height

        // 1. Prime optical detector baseline frame
        val baselineFrame = ByteArray(size) { 100.toByte() }
        opticalDetector.detectBall(baselineFrame, width, height, stride)

        // 2. Synthesize ball at (540, 1400)
        val targetX = 540
        val targetY = 1400
        val radius = 12

        val testFrame = baselineFrame.copyOf()
        for (y in (targetY - radius)..(targetY + radius)) {
            for (x in (targetX - radius)..(targetX + radius)) {
                val dx = x - targetX
                val dy = y - targetY
                if (dx * dx + dy * dy <= radius * radius) {
                    testFrame[y * stride + x] = 220.toByte()
                }
            }
        }

        // Run Optical Detector
        val opticalCentroid = opticalDetector.detectBall(testFrame, width, height, stride)
        assertNotNull("Optical detector must detect ball", opticalCentroid)

        // Configure ML Detector to predict the same region with high confidence
        val normYmin = (targetY - radius).toFloat() / height
        val normXmin = (targetX - radius).toFloat() / width
        val normYmax = (targetY + radius).toFloat() / height
        val normXmax = (targetX + radius).toFloat() / width

        val mlRunner = MockTFLiteRunner(
            mockBoxes = arrayOf(floatArrayOf(normYmin, normXmin, normYmax, normXmax)),
            mockScores = floatArrayOf(0.95f)
        )
        val mlDetector = TFLiteBallDetector.createForTesting(mlRunner)

        // Run ML Detector
        val mlCentroid = mlDetector.detectBall(testFrame, width, height, stride)
        assertNotNull("ML detector must detect ball", mlCentroid)

        // 3. Parity assertion between classical optical differencing and ML detection
        assertEquals("X coordinates between optical and ML must align within 5px",
            opticalCentroid!!.x, mlCentroid!!.x, 5.0)
        assertEquals("Y coordinates between optical and ML must align within 5px",
            opticalCentroid.y, mlCentroid.y, 5.0)

        // 4. Verification that both feed into TrajectoryTracker seamlessly
        val tracker = TrajectoryTracker(homography)
        val trajPoint1 = tracker.onBallCentroidDetected(opticalCentroid, 1000L)
        tracker.reset()
        val trajPoint2 = tracker.onBallCentroidDetected(mlCentroid, 1000L)

        assertNotNull("Tracker should accept optical centroid", trajPoint1)
        assertNotNull("Tracker should accept ML centroid", trajPoint2)
        assertEquals(trajPoint1!!.xBoard, trajPoint2!!.xBoard, 0.5)
        assertEquals(trajPoint1.yFt, trajPoint2.yFt, 0.5)
    }

    @Test
    fun testBallDetectorEnginePolymorphism() {
        val mockRunner = MockTFLiteRunner()
        val engine: BallDetectorEngine = TFLiteBallDetector.createForTesting(mockRunner)

        val imageBytes = ByteArray(320 * 320) { 120.toByte() }
        val pt = engine.detectBall(imageBytes, 320, 320)
        assertNotNull("Polymorphic call via BallDetectorEngine must succeed", pt)
        assertTrue("Latency must be positive", engine.getLastInferenceLatencyMs() >= 0.0)
    }

    @Test
    fun testYoloOutputFormatParsingAndCentroidComputation() {
        val mockRunner = MockYoloRunner(
            mockCx = 208.0f,  // 208 / 416 = 0.50 normalized
            mockCy = 312.0f,  // 312 / 416 = 0.75 normalized
            mockW = 20.8f,    // 20.8 / 416 = 0.05 normalized
            mockH = 20.8f,
            mockConf = 0.88f
        )
        val detector = TFLiteBallDetector.createForTesting(mockRunner, inputSize = 416)

        val width = 1080
        val height = 1920
        val directBuffer = ByteBuffer.allocateDirect(width * height)
        val result = detector.detect(directBuffer, width, height)

        assertNotNull("YOLO detector should detect ball", result)
        assertEquals(0.88f, result!!.confidence, 0.001f)

        val expectedScreenX = 0.50 * width   // 540.0
        val expectedScreenY = 0.75 * height  // 1440.0
        assertEquals(expectedScreenX, result.centroid.x, 0.01)
        assertEquals(expectedScreenY, result.centroid.y, 0.01)

        val radius = detector.getLastBallRadiusPx()
        assertTrue("Radius should be positive", radius >= 8)
        assertTrue("Latency should be non-negative", result.latencyMs >= 0.0)
    }

    @Test
    fun testMultiClassYoloDetectionForPinRackAndBall() {
        class MockMultiClassYoloRunner : TFLiteRunner {
            override fun isYoloFormat(): Boolean = true
            override fun getYoloAnchorCount(): Int = 3549
            override fun getYoloChannelCount(): Int = 11 // 4 bbox coords + 7 classes
            override fun getInputChannels(): Int = 3

            override fun run(input: ByteBuffer, outputs: Map<Int, Any>) {
                @Suppress("UNCHECKED_CAST")
                val yoloOut = outputs[0] as? Array<Array<FloatArray>>
                if (yoloOut != null) {
                    // Anchor 10: Class 0 (bowling_ball) at cx=0.50, cy=0.75
                    yoloOut[0][0][10] = 208f
                    yoloOut[0][1][10] = 312f
                    yoloOut[0][2][10] = 20f
                    yoloOut[0][3][10] = 20f
                    yoloOut[0][4][10] = 0.89f

                    // Anchor 20: Class 1 (pin_rack) at cx=0.50, cy=0.20
                    yoloOut[0][0][20] = 208f
                    yoloOut[0][1][20] = 83f
                    yoloOut[0][2][20] = 50f
                    yoloOut[0][3][20] = 30f
                    yoloOut[0][5][20] = 0.96f

                    // Anchor 30: Class 3 (foul_line) at cx=0.50, cy=0.55
                    yoloOut[0][0][30] = 208f
                    yoloOut[0][1][30] = 228f
                    yoloOut[0][2][30] = 240f
                    yoloOut[0][3][30] = 10f
                    yoloOut[0][7][30] = 0.92f

                    // Anchor 40: Class 4 (arrows) at cx=0.50, cy=0.46
                    yoloOut[0][0][40] = 208f
                    yoloOut[0][1][40] = 191f
                    yoloOut[0][2][40] = 200f
                    yoloOut[0][3][40] = 20f
                    yoloOut[0][8][40] = 0.94f

                    // Anchor 50: Class 5 (lane) at cx=0.50, cy=0.45
                    yoloOut[0][0][50] = 208f
                    yoloOut[0][1][50] = 187f
                    yoloOut[0][2][50] = 220f
                    yoloOut[0][3][50] = 150f
                    yoloOut[0][9][50] = 0.97f

                    // Anchor 60: Class 6 (slide_foot) at cx=0.45, cy=0.60
                    yoloOut[0][0][60] = 187f
                    yoloOut[0][1][60] = 250f
                    yoloOut[0][2][60] = 40f
                    yoloOut[0][3][60] = 50f
                    yoloOut[0][10][60] = 0.85f
                }
            }
            override fun close() {}
        }

        val runner = MockMultiClassYoloRunner()
        val detector = TFLiteBallDetector.createForTesting(runner, inputSize = 416)
        assertEquals(7, detector.yoloClassCount)

        val width = 1080
        val height = 1920
        val directBuffer = ByteBuffer.allocateDirect(width * height)

        // 1. Test ball detection (Class 0)
        val ballCentroid = detector.detectBall(directBuffer, width, height)
        assertNotNull("Should detect ball from class 0", ballCentroid)
        assertEquals(540.0, ballCentroid!!.x, 0.5)
        assertEquals(1440.0, ballCentroid.y, 0.5)

        // 2. Test pin rack detection (Class 1)
        val pinRack = detector.detectPinRack(directBuffer, width, height)
        assertNotNull("Should detect pin rack from class 1", pinRack)
        assertEquals("pin_rack", pinRack!!.className)
        assertEquals(1, pinRack.classId)
        assertEquals(0.96f, pinRack.confidence, 0.001f)

        // 3. Test foul line detection (Class 3)
        val foulLine = detector.detectFoulLine(directBuffer, width, height)
        assertNotNull("Should detect foul line from class 3", foulLine)
        assertEquals("foul_line", foulLine!!.className)
        assertEquals(3, foulLine.classId)
        assertEquals(0.92f, foulLine.confidence, 0.001f)

        // 4. Test arrows detection (Class 4)
        val arrows = detector.detectArrows(directBuffer, width, height)
        assertNotNull("Should detect arrows from class 4", arrows)
        assertEquals("arrows", arrows!!.className)
        assertEquals(4, arrows.classId)
        assertEquals(0.94f, arrows.confidence, 0.001f)

        // 5. Test lane detection (Class 5)
        val lane = detector.detectLane(directBuffer, width, height)
        assertNotNull("Should detect lane from class 5", lane)
        assertEquals("lane", lane!!.className)
        assertEquals(5, lane.classId)
        assertEquals(0.97f, lane.confidence, 0.001f)

        // 6. Test slide foot detection (Class 6)
        val slideFoot = detector.detectSlideFoot(directBuffer, width, height)
        assertNotNull("Should detect slide foot from class 6", slideFoot)
        assertEquals("slide_foot", slideFoot!!.className)
        assertEquals(6, slideFoot.classId)
        assertEquals(0.85f, slideFoot.confidence, 0.001f)
    }

    @Test
    fun testBowlingBallModelAssetFileIntegrity() {
        val modelFile = java.io.File("src/main/assets/models/bowling_ball_v1.tflite")
        assertTrue("TFLite model asset must exist in src/main/assets/models/", modelFile.exists())
        assertTrue("TFLite model asset must be greater than 1MB", modelFile.length() > 1_000_000)
    }
}

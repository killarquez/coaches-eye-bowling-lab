package com.example.cebowlinglabtrack.domain.ml

import com.example.cebowlinglabtrack.domain.kinematics.PoseFrame
import com.example.cebowlinglabtrack.domain.model.Point2D

/**
 * Optical ball detector engine interface.
 * Supports ONNX Runtime Mobile, TensorFlow Lite, and Simulated engine.
 */
interface BallDetectorEngine {
    /**
     * Detects ball centroid in image coordinates (u, v).
     * Returns null if no ball detected in current frame.
     */
    fun detectBall(
        imageBytes: ByteArray,
        width: Int,
        height: Int,
        stride: Int = width
    ): Point2D?

    /**
     * Estimated inference latency in milliseconds.
     */
    fun getLastInferenceLatencyMs(): Double
}

/**
 * Skeletal pose estimator engine interface.
 * Supports MediaPipe Pose, RTMPose ONNX, and Simulated engine.
 */
interface PoseEstimatorEngine {
    /**
     * Runs pose estimation on bowler approach frame.
     */
    fun estimatePose(
        imageBytes: ByteArray,
        width: Int,
        height: Int,
        timestampMs: Long
    ): PoseFrame?

    /**
     * Estimated inference latency in milliseconds.
     */
    fun getLastInferenceLatencyMs(): Double
}

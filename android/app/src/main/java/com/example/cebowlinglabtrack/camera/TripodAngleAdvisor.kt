package com.example.cebowlinglabtrack.camera

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Real-time Tripod Alignment & Angle Advisor.
 *
 * Monitors device orientation via hardware IMU sensors to guide the bowler to
 * optimal tripod height, tilt angle, and level alignment.
 *
 * Ideal specs for bowling optical tracking:
 * - Roll: 0.0° ± 1.0° (Level with the ground)
 * - Pitch: -6.0° to -10.0° (Tilted down-lane toward headpin)
 */
class TripodAngleAdvisor(context: Context) : SensorEventListener {

    data class TripodStatus(
        val pitchDeg: Float = -8.0f,
        val rollDeg: Float = 0.0f,
        val isPitchOptimal: Boolean = true,
        val isRollOptimal: Boolean = true,
        val isLevel: Boolean = true,
        val adviceText: String = "TRIPOD ANGLE OPTIMAL"
    )

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val _status = MutableStateFlow(TripodStatus())
    val status: StateFlow<TripodStatus> = _status.asStateFlow()

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    fun startListening() {
        rotationSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            SensorManager.getOrientation(rotationMatrix, orientationAngles)

            // Pitch: rotation around X axis (tilt forward/backward in portrait)
            val pitch = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
            // Roll: rotation around Y axis (tilt left/right in portrait)
            val roll = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()

            evaluateAngles(pitch, roll)
        } else if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val ax = event.values[0]
            val ay = event.values[1]
            val az = event.values[2]

            val pitch = Math.toDegrees(atan2(-ay.toDouble(), sqrt((ax * ax + az * az).toDouble()))).toFloat()
            val roll = Math.toDegrees(atan2(ax.toDouble(), az.toDouble())).toFloat()

            evaluateAngles(pitch, roll)
        }
    }

    private fun evaluateAngles(pitch: Float, roll: Float) {
        // In typical Android portrait mounting on tripod:
        // Ideal pitch is -6.0° to -10.0° (pointing slightly down towards the 60 ft deck)
        val isPitchOptimal = pitch in -11.0f..-5.0f
        val isRollOptimal = abs(roll) <= 1.5f
        val isLevel = isPitchOptimal && isRollOptimal

        val advice = when {
            abs(roll) > 1.5f && roll > 0 -> "LEVEL TRIPOD: TILT LEFT ${String.format("%.1f", abs(roll))}°"
            abs(roll) > 1.5f && roll < 0 -> "LEVEL TRIPOD: TILT RIGHT ${String.format("%.1f", abs(roll))}°"
            pitch > -5.0f -> "TILT PHONE DOWN TOWARD PINS (${String.format("%.1f", pitch)}°)"
            pitch < -11.0f -> "TILT PHONE UPWARD (${String.format("%.1f", pitch)}°)"
            else -> "TRIPOD ALIGNMENT OPTIMAL (120 FPS READY)"
        }

        _status.value = TripodStatus(
            pitchDeg = pitch,
            rollDeg = roll,
            isPitchOptimal = isPitchOptimal,
            isRollOptimal = isRollOptimal,
            isLevel = isLevel,
            adviceText = advice
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}

package com.example.cebowlinglabtrack.domain.tracking

import com.example.cebowlinglabtrack.domain.model.LaneConstants
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * 5-State Extended Kalman Filter for Bowling Ball Trajectory Tracking.
 *
 * State Vector:
 * x = [X, Y, vx, vy, ax]^T
 *   X  : Lateral position across boards [1..39]
 *   Y  : Down-lane distance in feet [0..60]
 *   vx : Lateral velocity (boards/sec)
 *   vy : Longitudinal down-lane velocity (ft/sec)
 *   ax : Lateral hook acceleration (boards/sec^2)
 *
 * Measurement Vector:
 * z = [X_meas, Y_meas]^T (from camera homography projection)
 */
class ExtendedKalmanFilter(
    initialX: Double = 20.0,
    initialY: Double = 0.0,
    initialVx: Double = 0.0,
    initialVy: Double = 25.0, // ~17 mph initial forward speed
    initialAx: Double = 0.0
) {
    // State vector [X, Y, vx, vy, ax]
    val x = doubleArrayOf(initialX, initialY, initialVx, initialVy, initialAx)

    // Covariance matrix P (5x5)
    private val p = Array(5) { DoubleArray(5) }

    // Measurement noise covariance R (2x2)
    // Board resolution ~0.25 boards, distance resolution ~0.3 ft
    private val r = arrayOf(
        doubleArrayOf(0.15, 0.0),
        doubleArrayOf(0.0, 0.20)
    )

    // Process noise intensities
    private val qX = 0.5    // Lateral position jitter
    private val qY = 0.2    // Longitudinal position jitter
    private val qVx = 1.0   // Lateral velocity fluctuation
    private val qVy = 0.5   // Forward speed fluctuation (oil drag)
    private val qAx = 3.0   // Hook acceleration dynamic change rate

    var consecutiveCoastFrames: Int = 0
        private set

    val xBoard: Double get() = x[0]
    val yFt: Double get() = x[1]
    val vx: Double get() = x[2]
    val vy: Double get() = x[3]
    val ax: Double get() = x[4]

    /** Ball speed down-lane in miles per hour (mph) */
    val speedMph: Double get() = vy * LaneConstants.FT_PER_SEC_TO_MPH

    init {
        // Initial state uncertainties
        p[0][0] = 1.0   // X uncertainty: 1 board
        p[1][1] = 1.5   // Y uncertainty: 1.5 ft
        p[2][2] = 4.0   // vx uncertainty: 2 boards/sec
        p[3][3] = 4.0   // vy uncertainty: 2 ft/sec
        p[4][4] = 9.0   // ax uncertainty: 3 boards/sec^2
    }

    /**
     * Time-update (prediction step) for interval dt (seconds).
     */
    fun predict(dt: Double) {
        val dtSafe = dt.coerceIn(0.001, 0.2)
        val dt2 = 0.5 * dtSafe * dtSafe

        // State prediction: x = F * x
        val newX = x[0] + x[2] * dtSafe + x[4] * dt2
        val newY = x[1] + x[3] * dtSafe
        val newVx = x[2] + x[4] * dtSafe
        val newVy = x[3] // assuming negligible forward acceleration over small dt
        val newAx = x[4]

        x[0] = newX
        x[1] = newY
        x[2] = newVx
        x[3] = newVy
        x[4] = newAx

        // State transition Jacobian F (5x5):
        // [ 1  0  dt 0  0.5*dt^2 ]
        // [ 0  1  0  dt 0        ]
        // [ 0  0  1  0  dt       ]
        // [ 0  0  0  1  0        ]
        // [ 0  0  0  0  1        ]
        val f = arrayOf(
            doubleArrayOf(1.0, 0.0, dtSafe, 0.0, dt2),
            doubleArrayOf(0.0, 1.0, 0.0, dtSafe, 0.0),
            doubleArrayOf(0.0, 0.0, 1.0, 0.0, dtSafe),
            doubleArrayOf(0.0, 0.0, 0.0, 1.0, 0.0),
            doubleArrayOf(0.0, 0.0, 0.0, 0.0, 1.0)
        )

        // Process noise Q (5x5)
        val q = Array(5) { DoubleArray(5) }
        q[0][0] = qX * dtSafe
        q[1][1] = qY * dtSafe
        q[2][2] = qVx * dtSafe
        q[3][3] = qVy * dtSafe
        q[4][4] = qAx * dtSafe

        // P = F * P * F^T + Q
        val fp = Array(5) { DoubleArray(5) }
        for (i in 0..4) {
            for (j in 0..4) {
                var s = 0.0
                for (k in 0..4) {
                    s += f[i][k] * p[k][j]
                }
                fp[i][j] = s
            }
        }

        for (i in 0..4) {
            for (j in 0..4) {
                var s = 0.0
                for (k in 0..4) {
                    s += fp[i][k] * f[j][k]
                }
                p[i][j] = s + q[i][j]
            }
        }
    }

    /**
     * Measurement update with optical observation (zX = board, zY = distanceFt).
     *
     * @return true if measurement was accepted within Mahalanobis gating; false if rejected (glare/outlier).
     */
    fun update(zX: Double, zY: Double, mahalanobisThreshold: Double = 16.0): Boolean {
        // Innovation y = z - H * x
        val y0 = zX - x[0]
        val y1 = zY - x[1]

        // Innovation covariance S = H * P * H^T + R
        // Since H is [1 0 0 0 0; 0 1 0 0 0], H * P * H^T is the top-left 2x2 submatrix of P
        val s00 = p[0][0] + r[0][0]
        val s01 = p[0][1] + r[0][1]
        val s10 = p[1][0] + r[1][0]
        val s11 = p[1][1] + r[1][1]

        val detS = s00 * s11 - s01 * s10
        if (abs(detS) < 1e-9) {
            consecutiveCoastFrames++
            return false
        }

        // S^-1
        val invS00 = s11 / detS
        val invS01 = -s01 / detS
        val invS10 = -s10 / detS
        val invS11 = s00 / detS

        // Mahalanobis distance squared: d_M^2 = y^T * S^-1 * y
        val dM2 = y0 * (invS00 * y0 + invS01 * y1) + y1 * (invS10 * y0 + invS11 * y1)
        if (dM2 > mahalanobisThreshold) {
            // Outlier rejected (reflection, glare, or false detection)
            consecutiveCoastFrames++
            return false
        }

        consecutiveCoastFrames = 0

        // Kalman Gain K = P * H^T * S^-1
        // P * H^T is the first 2 columns of P (5x2)
        val k = Array(5) { DoubleArray(2) }
        for (i in 0..4) {
            val ph0 = p[i][0]
            val ph1 = p[i][1]
            k[i][0] = ph0 * invS00 + ph1 * invS10
            k[i][1] = ph0 * invS01 + ph1 * invS11
        }

        // Update state: x = x + K * y
        for (i in 0..4) {
            x[i] += k[i][0] * y0 + k[i][1] * y1
        }

        // Clamp board to physical boundaries [0.5, 39.5]
        x[0] = x[0].coerceIn(0.5, 39.5)
        // Distance cannot run backwards
        x[1] = x[1].coerceIn(-5.0, 65.0)

        // Update covariance: P = (I - K * H) * P
        val ikh = Array(5) { DoubleArray(5) }
        for (i in 0..4) {
            for (j in 0..4) {
                val kh = (if (j == 0) k[i][0] else 0.0) + (if (j == 1) k[i][1] else 0.0)
                ikh[i][j] = (if (i == j) 1.0 else 0.0) - kh
            }
        }

        val newP = Array(5) { DoubleArray(5) }
        for (i in 0..4) {
            for (j in 0..4) {
                var sum = 0.0
                for (m in 0..4) {
                    sum += ikh[i][m] * p[m][j]
                }
                newP[i][j] = sum
            }
        }

        for (i in 0..4) {
            for (j in 0..4) {
                // Ensure symmetry
                p[i][j] = 0.5 * (newP[i][j] + newP[j][i])
            }
        }

        return true
    }

    /**
     * Coast step when ball is temporarily occluded by glare or oil reflections.
     */
    fun coast(dt: Double) {
        predict(dt)
        consecutiveCoastFrames++
    }
}

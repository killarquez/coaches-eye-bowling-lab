package com.example.cebowlinglabtrack.domain.calibration

import com.example.cebowlinglabtrack.domain.model.LanePoint
import com.example.cebowlinglabtrack.domain.model.Point2D
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * 3x3 Homography Transformation Matrix for perspective projection.
 *
 * Maps real-world bowling lane coordinates (Board X in [1..39], Distance Y in [0..60] ft)
 * to/from camera image pixel coordinates (u, v).
 */
class HomographyMatrix(
    val data: DoubleArray
) {
    init {
        require(data.size == 9) { "Homography matrix must contain exactly 9 elements (3x3)." }
    }

    constructor(
        h00: Double, h01: Double, h02: Double,
        h10: Double, h11: Double, h12: Double,
        h20: Double, h21: Double, h22: Double
    ) : this(doubleArrayOf(h00, h01, h02, h10, h11, h12, h20, h21, h22))

    val elements: List<Double> get() = data.toList()

    /**
     * Projects a real-world lane coordinate (board, distanceFt) into camera pixel space (u, v).
     */
    fun forward(lanePoint: LanePoint): Point2D {
        val x = lanePoint.board
        val y = lanePoint.distanceFt
        val w = data[6] * x + data[7] * y + data[8]
        if (abs(w) < 1e-10) return Point2D(0.0, 0.0)
        val u = (data[0] * x + data[1] * y + data[2]) / w
        val v = (data[3] * x + data[4] * y + data[5]) / w
        return Point2D(u, v)
    }

    /**
     * Projects physical lane space (board 1..39, distance 0..60 ft)
     * back into screen pixel coordinates (u, v).
     */
    fun projectLaneToPixel(board: Double, distanceFt: Double): Point2D {
        return forward(LanePoint(board, distanceFt))
    }

    /**
     * Projects a camera pixel coordinate (u, v) back to real-world lane coordinates (board, distanceFt).
     * Uses the precomputed inverse matrix.
     */
    fun inverse(screenPoint: Point2D): LanePoint {
        val inv = invert() ?: return LanePoint(20.0, 0.0)
        val u = screenPoint.x
        val v = screenPoint.y
        val w = inv.data[6] * u + inv.data[7] * v + inv.data[8]
        if (abs(w) < 1e-10) return LanePoint(20.0, 0.0)
        val board = (inv.data[0] * u + inv.data[1] * v + inv.data[2]) / w
        val dist = (inv.data[3] * u + inv.data[4] * v + inv.data[5]) / w
        return LanePoint(board, dist)
    }

    /**
     * Inverts the 3x3 matrix using the classical adjugate / cofactor formula.
     * Returns null if matrix is singular (determinant == 0).
     */
    fun invert(): HomographyMatrix? {
        val a = data[0]; val b = data[1]; val c = data[2]
        val d = data[3]; val e = data[4]; val f = data[5]
        val g = data[6]; val h = data[7]; val i = data[8]

        val det = a * (e * i - f * h) -
                b * (d * i - f * g) +
                c * (d * h - e * g)

        if (abs(det) < 1e-12) return null

        val invDet = 1.0 / det

        val invData = DoubleArray(9)
        invData[0] = (e * i - f * h) * invDet
        invData[1] = (c * h - b * i) * invDet
        invData[2] = (b * f - c * e) * invDet

        invData[3] = (f * g - d * i) * invDet
        invData[4] = (a * i - c * g) * invDet
        invData[5] = (c * d - a * f) * invDet

        invData[6] = (d * h - e * g) * invDet
        invData[7] = (b * g - a * h) * invDet
        invData[8] = (a * e - b * d) * invDet

        return HomographyMatrix(invData)
    }

    /**
     * Multiplies this matrix by another 3x3 matrix: result = this * other.
     */
    fun multiply(other: HomographyMatrix): HomographyMatrix {
        val res = DoubleArray(9)
        for (row in 0..2) {
            for (col in 0..2) {
                var sum = 0.0
                for (k in 0..2) {
                    sum += data[row * 3 + k] * other.data[k * 3 + col]
                }
                res[row * 3 + col] = sum
            }
        }
        return HomographyMatrix(res)
    }

    /**
     * Scales this homography for a camera hardware or digital zoom factor relative to the
     * calibration zoom factor, preserving optical alignment around the viewport center (cx, cy).
     *
     * @param scaleFactor Ratio of current zoom / calibration zoom (e.g. 2.5x / 1.0x = 2.5)
     * @param centerX Optical / Viewport center X (e.g. width / 2)
     * @param centerY Optical / Viewport center Y (e.g. height / 2)
     */
    fun scaleForZoom(scaleFactor: Double, centerX: Double, centerY: Double): HomographyMatrix {
        if (abs(scaleFactor - 1.0) < 1e-4) return this
        val mZoom = HomographyMatrix(
            scaleFactor, 0.0, centerX * (1.0 - scaleFactor),
            0.0, scaleFactor, centerY * (1.0 - scaleFactor),
            0.0, 0.0, 1.0
        )
        return mZoom.multiply(this)
    }

    companion object {
        fun identity(): HomographyMatrix = HomographyMatrix(
            doubleArrayOf(
                1.0, 0.0, 0.0,
                0.0, 1.0, 0.0,
                0.0, 0.0, 1.0
            )
        )

        /**
         * Solves for the 3x3 Homography Matrix mapping source points (lane coords)
         * to destination points (image screen pixels) using Direct Linear Transformation (DLT)
         * with isotropic Hartley normalization for numerical stability.
         *
         * @param src 4 or more points in source space (e.g. LanePoint(board, distanceFt))
         * @param dst 4 or more points in destination space (e.g. Point2D(u, v))
         * @return HomographyMatrix or null if degenerate
         */
        fun computeDLT(src: List<LanePoint>, dst: List<Point2D>): HomographyMatrix? {
            if (src.size < 4 || dst.size < 4 || src.size != dst.size) return null

            val n = src.size

            // 1. Hartley normalization for source points
            var meanSrcX = 0.0; var meanSrcY = 0.0
            for (p in src) {
                meanSrcX += p.board
                meanSrcY += p.distanceFt
            }
            meanSrcX /= n; meanSrcY /= n

            var distSrc = 0.0
            for (p in src) {
                val dx = p.board - meanSrcX
                val dy = p.distanceFt - meanSrcY
                distSrc += sqrt(dx * dx + dy * dy)
            }
            distSrc /= n
            val scaleSrc = if (distSrc > 1e-6) sqrt(2.0) / distSrc else 1.0

            val tSrc = HomographyMatrix(
                scaleSrc, 0.0, -scaleSrc * meanSrcX,
                0.0, scaleSrc, -scaleSrc * meanSrcY,
                0.0, 0.0, 1.0
            )

            // 2. Hartley normalization for destination points
            var meanDstX = 0.0; var meanDstY = 0.0
            for (p in dst) {
                meanDstX += p.x
                meanDstY += p.y
            }
            meanDstX /= n; meanDstY /= n

            var distDst = 0.0
            for (p in dst) {
                val dx = p.x - meanDstX
                val dy = p.y - meanDstY
                distDst += sqrt(dx * dx + dy * dy)
            }
            distDst /= n
            val scaleDst = if (distDst > 1e-6) sqrt(2.0) / distDst else 1.0

            val tDst = HomographyMatrix(
                scaleDst, 0.0, -scaleDst * meanDstX,
                0.0, scaleDst, -scaleDst * meanDstY,
                0.0, 0.0, 1.0
            )

            // 3. Set up the 2N x 8 linear system: A * h = b (assuming h33 = 1)
            // For normalized coordinates:
            val numEq = 2 * n
            val aMatrix = Array(numEq) { DoubleArray(8) }
            val bVector = DoubleArray(numEq)

            for (i in 0 until n) {
                val sX = (src[i].board - meanSrcX) * scaleSrc
                val sY = (src[i].distanceFt - meanSrcY) * scaleSrc
                val dX = (dst[i].x - meanDstX) * scaleDst
                val dY = (dst[i].y - meanDstY) * scaleDst

                val row1 = 2 * i
                val row2 = 2 * i + 1

                // First equation: -sX*h00 - sY*h01 - h02 + 0 + 0 + 0 + dX*sX*h20 + dX*sY*h21 = -dX
                aMatrix[row1][0] = -sX
                aMatrix[row1][1] = -sY
                aMatrix[row1][2] = -1.0
                aMatrix[row1][3] = 0.0
                aMatrix[row1][4] = 0.0
                aMatrix[row1][5] = 0.0
                aMatrix[row1][6] = dX * sX
                aMatrix[row1][7] = dX * sY
                bVector[row1] = -dX

                // Second equation: 0 + 0 + 0 - sX*h10 - sY*h11 - h12 + dY*sX*h20 + dY*sY*h21 = -dY
                aMatrix[row2][0] = 0.0
                aMatrix[row2][1] = 0.0
                aMatrix[row2][2] = 0.0
                aMatrix[row2][3] = -sX
                aMatrix[row2][4] = -sY
                aMatrix[row2][5] = -1.0
                aMatrix[row2][6] = dY * sX
                aMatrix[row2][7] = dY * sY
                bVector[row2] = -dY
            }

            // 4. Solve normal equations: (A^T * A) * h = A^T * b (works for N=4 or N>4 overdetermined)
            val ata = Array(8) { DoubleArray(8) }
            val atb = DoubleArray(8)

            for (r in 0..7) {
                for (c in 0..7) {
                    var sum = 0.0
                    for (k in 0 until numEq) {
                        sum += aMatrix[k][r] * aMatrix[k][c]
                    }
                    ata[r][c] = sum
                }
                var sumB = 0.0
                for (k in 0 until numEq) {
                    sumB += aMatrix[k][r] * bVector[k]
                }
                atb[r] = sumB
            }

            // Solve 8x8 system using Gaussian Elimination with partial pivoting
            val hNormVec = solveLinearSystem8x8(ata, atb) ?: return null

            val hNorm = HomographyMatrix(
                hNormVec[0], hNormVec[1], hNormVec[2],
                hNormVec[3], hNormVec[4], hNormVec[5],
                hNormVec[6], hNormVec[7], 1.0
            )

            // 5. Denormalize: H = T_dst^-1 * H_norm * T_src
            val tDstInv = tDst.invert() ?: return null
            val h = tDstInv.multiply(hNorm).multiply(tSrc)

            // Normalize so h22 = 1.0 (or largest entry)
            val scale = if (abs(h.data[8]) > 1e-10) 1.0 / h.data[8] else 1.0
            for (k in 0..8) {
                h.data[k] *= scale
            }

            return h
        }

        /**
         * Solves an 8x8 linear system M * x = b using Gaussian elimination with partial pivoting.
         */
        private fun solveLinearSystem8x8(m: Array<DoubleArray>, b: DoubleArray): DoubleArray? {
            val n = 8
            val a = Array(n) { r -> m[r].clone() }
            val x = b.clone()

            for (i in 0 until n) {
                var maxRow = i
                var maxVal = abs(a[i][i])
                for (k in i + 1 until n) {
                    if (abs(a[k][i]) > maxVal) {
                        maxVal = abs(a[k][i])
                        maxRow = k
                    }
                }

                if (maxVal < 1e-12) return null // Singular matrix

                if (maxRow != i) {
                    val tempRow = a[i]
                    a[i] = a[maxRow]
                    a[maxRow] = tempRow

                    val tempB = x[i]
                    x[i] = x[maxRow]
                    x[maxRow] = tempB
                }

                for (k in i + 1 until n) {
                    val factor = a[k][i] / a[i][i]
                    for (j in i until n) {
                        a[k][j] -= factor * a[i][j]
                    }
                    x[k] -= factor * x[i]
                }
            }

            val solution = DoubleArray(n)
            for (i in n - 1 downTo 0) {
                var sum = x[i]
                for (j in i + 1 until n) {
                    sum -= a[i][j] * solution[j]
                }
                solution[i] = sum / a[i][i]
            }

            return solution
        }

        /**
         * Calculates Root Mean Square Reprojection Error in pixels.
         */
        fun calculateRmse(
            homography: HomographyMatrix,
            src: List<LanePoint>,
            dst: List<Point2D>
        ): Double {
            if (src.isEmpty() || src.size != dst.size) return 0.0
            var sumSqErr = 0.0
            for (i in src.indices) {
                val proj = homography.forward(src[i])
                val dx = proj.x - dst[i].x
                val dy = proj.y - dst[i].y
                sumSqErr += dx * dx + dy * dy
            }
            return sqrt(sumSqErr / src.size)
        }
    }
}

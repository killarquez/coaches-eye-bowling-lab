package com.example.cebowlinglabtrack.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import com.example.cebowlinglabtrack.domain.kinematics.PoseFrame
import com.example.cebowlinglabtrack.domain.kinematics.PoseLandmarkIndex
import com.example.cebowlinglabtrack.theme.ElectricAmber
import com.example.cebowlinglabtrack.theme.NeonCyan
import com.example.cebowlinglabtrack.theme.NeonStrikeGreen

/**
 * Renders the bowler skeletal stick figure on top of the camera viewfinder.
 */
@Composable
fun SkeletonOverlay(
    pose: PoseFrame?,
    modifier: Modifier = Modifier
) {
    if (pose == null || pose.landmarks.isEmpty()) return

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        fun landmarkToOffset(idx: Int): Offset? {
            val lm = pose.getOrNull(idx) ?: return null
            if (lm.visibility < 0.3f) return null
            return Offset(lm.x * w, lm.y * h)
        }

        // Standard bone connections
        val bonePairs = listOf(
            // Shoulders & Torso
            Pair(PoseLandmarkIndex.LEFT_SHOULDER, PoseLandmarkIndex.RIGHT_SHOULDER),
            Pair(PoseLandmarkIndex.LEFT_SHOULDER, PoseLandmarkIndex.LEFT_HIP),
            Pair(PoseLandmarkIndex.RIGHT_SHOULDER, PoseLandmarkIndex.RIGHT_HIP),
            Pair(PoseLandmarkIndex.LEFT_HIP, PoseLandmarkIndex.RIGHT_HIP),

            // Left Arm
            Pair(PoseLandmarkIndex.LEFT_SHOULDER, PoseLandmarkIndex.LEFT_ELBOW),
            Pair(PoseLandmarkIndex.LEFT_ELBOW, PoseLandmarkIndex.LEFT_WRIST),

            // Right Arm
            Pair(PoseLandmarkIndex.RIGHT_SHOULDER, PoseLandmarkIndex.RIGHT_ELBOW),
            Pair(PoseLandmarkIndex.RIGHT_ELBOW, PoseLandmarkIndex.RIGHT_WRIST),

            // Left Leg (Lead slide leg for right-hander)
            Pair(PoseLandmarkIndex.LEFT_HIP, PoseLandmarkIndex.LEFT_KNEE),
            Pair(PoseLandmarkIndex.LEFT_KNEE, PoseLandmarkIndex.LEFT_ANKLE),
            Pair(PoseLandmarkIndex.LEFT_ANKLE, PoseLandmarkIndex.LEFT_FOOT_INDEX),

            // Right Leg
            Pair(PoseLandmarkIndex.RIGHT_HIP, PoseLandmarkIndex.RIGHT_KNEE),
            Pair(PoseLandmarkIndex.RIGHT_KNEE, PoseLandmarkIndex.RIGHT_ANKLE),
            Pair(PoseLandmarkIndex.RIGHT_ANKLE, PoseLandmarkIndex.RIGHT_FOOT_INDEX)
        )

        // Draw Bones
        for ((idx1, idx2) in bonePairs) {
            val p1 = landmarkToOffset(idx1)
            val p2 = landmarkToOffset(idx2)
            if (p1 != null && p2 != null) {
                val isLeadLeg = (idx1 == PoseLandmarkIndex.LEFT_HIP && idx2 == PoseLandmarkIndex.LEFT_KNEE) ||
                        (idx1 == PoseLandmarkIndex.LEFT_KNEE && idx2 == PoseLandmarkIndex.LEFT_ANKLE)

                val boneColor = if (isLeadLeg) NeonStrikeGreen else NeonCyan.copy(alpha = 0.85f)
                val strokeWidth = if (isLeadLeg) 6f else 4f

                drawLine(
                    color = boneColor,
                    start = p1,
                    end = p2,
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }
        }

        // Draw Joint Dots
        for (i in 0..32) {
            val pt = landmarkToOffset(i) ?: continue
            val isSlideKnee = (i == PoseLandmarkIndex.LEFT_KNEE)
            val color = if (isSlideKnee) ElectricAmber else Color.White
            val radius = if (isSlideKnee) 6f else 3.5f

            drawCircle(
                color = color,
                radius = radius,
                center = pt
            )
        }
    }
}

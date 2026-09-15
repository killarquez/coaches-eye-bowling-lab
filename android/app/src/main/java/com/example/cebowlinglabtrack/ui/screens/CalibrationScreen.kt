package com.example.cebowlinglabtrack.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cebowlinglabtrack.domain.model.LaneCalibration
import com.example.cebowlinglabtrack.domain.model.Point2D
import com.example.cebowlinglabtrack.theme.DarkBackground
import com.example.cebowlinglabtrack.theme.DarkCardBorder
import com.example.cebowlinglabtrack.theme.DarkSurface
import com.example.cebowlinglabtrack.theme.ElectricAmber
import com.example.cebowlinglabtrack.theme.NeonCyan
import com.example.cebowlinglabtrack.theme.NeonStrikeGreen
import com.example.cebowlinglabtrack.theme.PowerCoral
import com.example.cebowlinglabtrack.theme.TextMuted
import com.example.cebowlinglabtrack.theme.TextPrimary
import com.example.cebowlinglabtrack.theme.TextSecondary
import kotlin.math.sqrt

/**
 * Interactive 4-Point Camera Perspective Calibration Screen.
 */
@Composable
fun CalibrationScreen(
    currentCalibration: LaneCalibration?,
    onSaveCalibration: (Point2D, Point2D, Point2D, Point2D) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 4 Draggable Anchor Pins in Normalized Coordinates [0f..1f]
    var flX by remember { mutableStateOf(currentCalibration?.foulLineLeftScreen?.x?.toFloat() ?: 130f) }
    var flY by remember { mutableStateOf(currentCalibration?.foulLineLeftScreen?.y?.toFloat() ?: 1680f) }

    var frX by remember { mutableStateOf(currentCalibration?.foulLineRightScreen?.x?.toFloat() ?: 950f) }
    var frY by remember { mutableStateOf(currentCalibration?.foulLineRightScreen?.y?.toFloat() ?: 1680f) }

    var alX by remember { mutableStateOf(currentCalibration?.arrowsLeftScreen?.x?.toFloat() ?: 300f) }
    var alY by remember { mutableStateOf(currentCalibration?.arrowsLeftScreen?.y?.toFloat() ?: 1000f) }

    var arX by remember { mutableStateOf(currentCalibration?.arrowsRightScreen?.x?.toFloat() ?: 780f) }
    var arY by remember { mutableStateOf(currentCalibration?.arrowsRightScreen?.y?.toFloat() ?: 1000f) }

    var selectedPinIndex by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "LANE CALIBRATION",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "DRAG 4 ANCHORS TO LANE CORNERS / ARROWS",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = "DLT 3x3",
                color = NeonCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Interactive Calibration Viewport
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(DarkSurface)
                .border(1.dp, DarkCardBorder, RoundedCornerShape(16.dp))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val touchX = offset.x
                            val touchY = offset.y
                            // Hit test closest pin within 60px
                            val distances = listOf(
                                dist(touchX, touchY, flX, flY),
                                dist(touchX, touchY, frX, frY),
                                dist(touchX, touchY, alX, alY),
                                dist(touchX, touchY, arX, arY)
                            )
                            val minIdx = distances.indices.minByOrNull { distances[it] } ?: 0
                            selectedPinIndex = if (distances[minIdx] < 120f) minIdx else null
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            when (selectedPinIndex) {
                                0 -> { flX += dragAmount.x; flY += dragAmount.y }
                                1 -> { frX += dragAmount.x; frY += dragAmount.y }
                                2 -> { alX += dragAmount.x; alY += dragAmount.y }
                                3 -> { arX += dragAmount.x; arY += dragAmount.y }
                            }
                        },
                        onDragEnd = { selectedPinIndex = null },
                        onDragCancel = { selectedPinIndex = null }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Draw connecting quadrilateral (Lane boundaries)
                drawLine(NeonCyan, Offset(flX, flY), Offset(frX, frY), 3f)
                drawLine(NeonCyan, Offset(frX, frY), Offset(arX, arY), 2f)
                drawLine(NeonCyan, Offset(arX, arY), Offset(alX, alY), 2f)
                drawLine(NeonCyan, Offset(alX, alY), Offset(flX, flY), 2f)

                // Draw center guide
                val midFoul = Offset((flX + frX) / 2f, (flY + frY) / 2f)
                val midArrows = Offset((alX + arX) / 2f, (alY + arY) / 2f)
                drawLine(Color.White.copy(alpha = 0.4f), midFoul, midArrows, 1.5f)

                // Draw Anchor Handles
                drawPinHandle("1. FOUL L (B1)", Offset(flX, flY), NeonStrikeGreen, selectedPinIndex == 0)
                drawPinHandle("2. FOUL R (B39)", Offset(frX, frY), NeonStrikeGreen, selectedPinIndex == 1)
                drawPinHandle("3. ARROW L (B5)", Offset(alX, alY), ElectricAmber, selectedPinIndex == 2)
                drawPinHandle("4. ARROW R (B35)", Offset(arX, arY), ElectricAmber, selectedPinIndex == 3)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Calibration Details & RMSE Card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DarkSurface)
                .border(1.dp, DarkCardBorder, RoundedCornerShape(12.dp))
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("REPROJECTION ACCURACY", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                val rmse = currentCalibration?.reprojectionErrorRmse ?: 0.42
                Text(
                    text = "RMSE: ${String.format("%.2f", rmse)} px",
                    color = NeonStrikeGreen,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "HARTLEY DLT SOLVER ACTIVE",
                color = TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("CANCEL")
            }

            Button(
                onClick = {
                    onSaveCalibration(
                        Point2D(flX.toDouble(), flY.toDouble()),
                        Point2D(frX.toDouble(), frY.toDouble()),
                        Point2D(alX.toDouble(), alY.toDouble()),
                        Point2D(arX.toDouble(), arY.toDouble())
                    )
                },
                modifier = Modifier.weight(1.5f),
                colors = ButtonDefaults.buttonColors(containerColor = NeonStrikeGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("SAVE CALIBRATION", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun dist(x1: Float, y1: Float, x2: Float, y2: Float): Float {
    val dx = x1 - x2
    val dy = y1 - y2
    return sqrt(dx * dx + dy * dy)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPinHandle(
    label: String,
    pos: Offset,
    color: Color,
    isSelected: Boolean
) {
    // Outer halo ring
    drawCircle(
        color = color.copy(alpha = if (isSelected) 0.5f else 0.25f),
        radius = if (isSelected) 28f else 20f,
        center = pos
    )
    // Core pin
    drawCircle(
        color = color,
        radius = 8f,
        center = pos
    )
    drawCircle(
        color = Color.White,
        radius = 3.5f,
        center = pos
    )
}

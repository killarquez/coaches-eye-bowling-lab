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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import com.example.cebowlinglabtrack.camera.CameraPreviewView
import com.example.cebowlinglabtrack.domain.model.LaneCalibration
import com.example.cebowlinglabtrack.domain.model.Point2D
import com.example.cebowlinglabtrack.theme.DarkBackground
import com.example.cebowlinglabtrack.theme.DarkCardBorder
import com.example.cebowlinglabtrack.theme.DarkSurface
import com.example.cebowlinglabtrack.theme.ElectricAmber
import com.example.cebowlinglabtrack.theme.NeonCyan
import com.example.cebowlinglabtrack.theme.NeonStrikeGreen
import com.example.cebowlinglabtrack.theme.TextMuted
import com.example.cebowlinglabtrack.theme.TextPrimary
import com.example.cebowlinglabtrack.theme.TextSecondary
import kotlin.math.sqrt

/**
 * Interactive 4-Point Camera Perspective Calibration Screen with Live Camera Viewfinder
 * and 1-Click Computer Vision Auto-Lane Detection.
 */
@Composable
fun CalibrationScreen(
    currentCalibration: LaneCalibration?,
    onSaveCalibration: (Point2D, Point2D, Point2D, Point2D) -> Unit,
    onAutoDetectLane: ((imageBytes: ByteArray, width: Int, height: Int, stride: Int) -> Boolean)? = null,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 4 Draggable Anchor Pins in Screen Coordinates
    var flX by remember { mutableStateOf(currentCalibration?.foulLineLeftScreen?.x?.toFloat() ?: 130f) }
    var flY by remember { mutableStateOf(currentCalibration?.foulLineLeftScreen?.y?.toFloat() ?: 1680f) }

    var frX by remember { mutableStateOf(currentCalibration?.foulLineRightScreen?.x?.toFloat() ?: 950f) }
    var frY by remember { mutableStateOf(currentCalibration?.foulLineRightScreen?.y?.toFloat() ?: 1680f) }

    var alX by remember { mutableStateOf(currentCalibration?.arrowsLeftScreen?.x?.toFloat() ?: 300f) }
    var alY by remember { mutableStateOf(currentCalibration?.arrowsLeftScreen?.y?.toFloat() ?: 1000f) }

    var arX by remember { mutableStateOf(currentCalibration?.arrowsRightScreen?.x?.toFloat() ?: 780f) }
    var arY by remember { mutableStateOf(currentCalibration?.arrowsRightScreen?.y?.toFloat() ?: 1000f) }

    var selectedPinIndex by remember { mutableStateOf<Int?>(null) }
    var autoDetectionStatus by remember { mutableStateOf<String?>(null) }

    // Store latest live camera frame for 1-click Auto-Detect
    var latestFrameBytes by remember { mutableStateOf<ByteArray?>(null) }
    var frameWidth by remember { mutableStateOf(0) }
    var frameHeight by remember { mutableStateOf(0) }
    var frameStride by remember { mutableStateOf(0) }

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
                    text = "ALIGN 4 ANCHORS TO GUTTERS & FOUL LINE",
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

        // Interactive Calibration Viewport with Live Camera Background
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, DarkCardBorder, RoundedCornerShape(16.dp))
        ) {
            // Live Camera Feed running behind the calibration handles
            CameraPreviewView(
                onFrameAvailable = { bytes, w, h, s, _ ->
                    latestFrameBytes = bytes
                    frameWidth = w
                    frameHeight = h
                    frameStride = s
                },
                modifier = Modifier.fillMaxSize()
            )

            // Touch interaction layer & AR Guide Rendering
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val touchX = offset.x
                                val touchY = offset.y
                                val distances = listOf(
                                    dist(touchX, touchY, flX, flY),
                                    dist(touchX, touchY, frX, frY),
                                    dist(touchX, touchY, alX, alY),
                                    dist(touchX, touchY, arX, arY)
                                )
                                val minIdx = distances.indices.minByOrNull { distances[it] } ?: 0
                                selectedPinIndex = if (distances[minIdx] < 140f) minIdx else null
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
                    drawLine(NeonCyan, Offset(flX, flY), Offset(frX, frY), 3.5f)
                    drawLine(NeonCyan, Offset(frX, frY), Offset(arX, arY), 2.5f)
                    drawLine(NeonCyan, Offset(arX, arY), Offset(alX, alY), 2.5f)
                    drawLine(NeonCyan, Offset(alX, alY), Offset(flX, flY), 2.5f)

                    // Draw center guide
                    val midFoul = Offset((flX + frX) / 2f, (flY + frY) / 2f)
                    val midArrows = Offset((alX + arX) / 2f, (alY + arY) / 2f)
                    drawLine(Color.White.copy(alpha = 0.5f), midFoul, midArrows, 2f)

                    // Draw 4 Anchor Handles
                    drawPinHandle("1. FOUL L (B1)", Offset(flX, flY), NeonStrikeGreen, selectedPinIndex == 0)
                    drawPinHandle("2. FOUL R (B39)", Offset(frX, frY), NeonStrikeGreen, selectedPinIndex == 1)
                    drawPinHandle("3. ARROW L (B5)", Offset(alX, alY), ElectricAmber, selectedPinIndex == 2)
                    drawPinHandle("4. ARROW R (B35)", Offset(arX, arY), ElectricAmber, selectedPinIndex == 3)
                }
            }

            // Auto-detect status banner if triggered
            autoDetectionStatus?.let { status ->
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurface.copy(alpha = 0.9f))
                        .border(1.dp, NeonCyan, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = status,
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 1-Click Auto-Detect Lane Button
        Button(
            onClick = {
                val frame = latestFrameBytes
                if (frame != null && onAutoDetectLane != null) {
                    val success = onAutoDetectLane(frame, frameWidth, frameHeight, frameStride)
                    if (success) {
                        currentCalibration?.let {
                            flX = it.foulLineLeftScreen.x.toFloat()
                            flY = it.foulLineLeftScreen.y.toFloat()
                            frX = it.foulLineRightScreen.x.toFloat()
                            frY = it.foulLineRightScreen.y.toFloat()
                            alX = it.arrowsLeftScreen.x.toFloat()
                            alY = it.arrowsLeftScreen.y.toFloat()
                            arX = it.arrowsRightScreen.x.toFloat()
                            arY = it.arrowsRightScreen.y.toFloat()
                        }
                        autoDetectionStatus = "✓ LANE AUTO-DETECTED & SNAPPED"
                    } else {
                        autoDetectionStatus = "AUTO-DETECT ADJUSTED: FINE-TUNE HANDLES"
                    }
                } else {
                    autoDetectionStatus = "POINT CAMERA AT LANE TO AUTO-DETECT"
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "Auto Detect",
                tint = Color.Black,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "AUTO-DETECT LANE (1-CLICK CV)",
                color = Color.Black,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Action Buttons: Cancel and Save
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
                modifier = Modifier.weight(1.4f),
                colors = ButtonDefaults.buttonColors(containerColor = NeonStrikeGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Save",
                    tint = Color.Black,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "APPLY & ARM",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPinHandle(
    label: String,
    center: Offset,
    color: Color,
    isSelected: Boolean
) {
    val outerRadius = if (isSelected) 28f else 22f
    val innerRadius = if (isSelected) 10f else 7f

    drawCircle(
        color = color.copy(alpha = if (isSelected) 0.6f else 0.3f),
        radius = outerRadius,
        center = center
    )
    drawCircle(
        color = color,
        radius = innerRadius,
        center = center
    )
    drawCircle(
        color = Color.White,
        radius = 3f,
        center = center
    )
}

private fun dist(x1: Float, y1: Float, x2: Float, y2: Float): Float {
    val dx = x1 - x2
    val dy = y1 - y2
    return sqrt(dx * dx + dy * dy)
}

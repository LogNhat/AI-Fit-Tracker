package com.example.aifittracker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import kotlin.math.max

@Composable
fun PoseOverlay(
    pose: Pose?,
    imageWidth: Int,
    imageHeight: Int,
    isFrontCamera: Boolean,
    squatStateStr: String
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val errorColor = MaterialTheme.colorScheme.error

    // Cache/remember paint and stroke objects to avoid allocation on every draw call
    val textPaint = remember {
        android.graphics.Paint().apply {
            textSize = 30f
            typeface = android.graphics.Typeface.MONOSPACE
            isFakeBoldText = true
        }
    }
    val stroke5 = remember { Stroke(width = 5f) }
    val stroke5Round = remember { Stroke(width = 5f, cap = StrokeCap.Round) }

    Canvas(modifier = Modifier.fillMaxSize()) {
        if (pose == null || imageWidth <= 0 || imageHeight <= 0) return@Canvas

        val color = when (squatStateStr) {
            "DESCENDING", "FLEXING" -> primaryColor
            "BOTTOM", "CORRECT", "FLEXED", "OPEN" -> secondaryColor
            "ASCENDING", "EXTENDING" -> tertiaryColor
            else -> errorColor
        }

        val landmarks = pose.allPoseLandmarks
        if (landmarks.isEmpty()) return@Canvas

        // Calculate Scale and Offsets according to FILL_CENTER
        val canvasWidth = size.width
        val canvasHeight = size.height

        val scale = max(canvasWidth / imageWidth.toFloat(), canvasHeight / imageHeight.toFloat())
        val scaledWidth = imageWidth * scale
        val scaledHeight = imageHeight * scale
        val offsetX = (canvasWidth - scaledWidth) / 2f
        val offsetY = (canvasHeight - scaledHeight) / 2f

        // Precompute scaled coordinate offsets for all 33 landmark points
        // to avoid redundant calculations inside drawing loops.
        val landmarkOffsets = Array<Offset?>(33) { null }
        for (landmark in landmarks) {
            val type = landmark.landmarkType
            if (type in 0..32 && landmark.inFrameLikelihood > 0.5f) {
                val adjustedX = if (isFrontCamera) imageWidth - landmark.position.x else landmark.position.x
                landmarkOffsets[type] = Offset(adjustedX * scale + offsetX, landmark.position.y * scale + offsetY)
            }
        }

        // Draw connections with double-draw neon glowing effect
        fun drawLineBetween(start: Int, end: Int) {
            val startOffset = landmarkOffsets.getOrNull(start)
            val endOffset = landmarkOffsets.getOrNull(end)
            if (startOffset != null && endOffset != null) {
                // 1. Thick neon colored background line (outer glow)
                drawLine(
                    color = color.copy(alpha = 0.25f),
                    start = startOffset,
                    end = endOffset,
                    strokeWidth = 22f,
                    cap = StrokeCap.Round
                )
                // 2. Bright white core line (inner bone core)
                drawLine(
                    color = Color.White.copy(alpha = 0.9f),
                    start = startOffset,
                    end = endOffset,
                    strokeWidth = 6f,
                    cap = StrokeCap.Round
                )
            }
        }

        // Draw dynamic circular gauges and text for joint angles
        fun drawAngleAtJoint(
            firstJoint: Int,
            middleJoint: Int,
            lastJoint: Int
        ) {
            val first = pose.getPoseLandmark(firstJoint)
            val middle = pose.getPoseLandmark(middleJoint)
            val last = pose.getPoseLandmark(lastJoint)
            
            val center = landmarkOffsets.getOrNull(middleJoint)
            if (first != null && middle != null && last != null && center != null) {
                val firstOffset = landmarkOffsets.getOrNull(firstJoint)
                val lastOffset = landmarkOffsets.getOrNull(lastJoint)
                if (firstOffset != null && lastOffset != null) {
                    val angle = com.example.aifittracker.analysis.PoseUtils.calculateAngle(
                        first.position.x, first.position.y,
                        middle.position.x, middle.position.y,
                        last.position.x, last.position.y
                    )
                    
                    // Draw a nice glowing circle ring around the joint
                    drawCircle(
                        color = color.copy(alpha = 0.2f),
                        radius = 35f,
                        center = center,
                        style = stroke5
                    )
                    
                    // Draw active part of the ring
                    val sweepAngle = (angle.toFloat() / 180f) * 360f
                    drawArc(
                        color = color,
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = Offset(center.x - 35f, center.y - 35f),
                        size = androidx.compose.ui.geometry.Size(70f, 70f),
                        style = stroke5Round
                    )
                    
                    // Draw the angle text using Android native canvas for rich drop-shadow and glow
                    val colorArgb = color.toArgb()
                    textPaint.color = colorArgb
                    textPaint.setShadowLayer(8f, 0f, 0f, colorArgb)

                    drawContext.canvas.nativeCanvas.drawText(
                        "${angle.toInt()}°",
                        center.x + 40f,
                        center.y + 10f,
                        textPaint
                    )
                }
            }
        }

        // Draw HUD lines (Bones)
        drawLineBetween(PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER)
        drawLineBetween(PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_HIP)
        drawLineBetween(PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_HIP)
        drawLineBetween(PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP)

        // Left Leg
        drawLineBetween(PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE)
        drawLineBetween(PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_ANKLE)

        // Right Leg
        drawLineBetween(PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE)
        drawLineBetween(PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_ANKLE)

        // Left Arm
        drawLineBetween(PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_ELBOW)
        drawLineBetween(PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_WRIST)

        // Right Arm
        drawLineBetween(PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_ELBOW)
        drawLineBetween(PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_WRIST)

        // Draw dynamic angles HUD
        drawAngleAtJoint(PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_ANKLE)
        drawAngleAtJoint(PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_ANKLE)
        drawAngleAtJoint(PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_WRIST)
        drawAngleAtJoint(PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_WRIST)

        // Draw joints dot tracker
        for (landmark in landmarks) {
            val type = landmark.landmarkType
            if (type in 0..32) {
                val offset = landmarkOffsets[type]
                if (offset != null) {
                    // Outer glowing circle
                    drawCircle(
                        color = Color.White,
                        radius = 8f,
                        center = offset
                    )
                    // Inner colored core
                    drawCircle(
                        color = color,
                        radius = 4f,
                        center = offset
                    )
                }
            }
        }
    }
}

package com.example.aifittracker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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

        fun PoseLandmark.toOffset(): Offset {
            val adjustedX = if (isFrontCamera) imageWidth - position.x else position.x
            return Offset(adjustedX * scale + offsetX, position.y * scale + offsetY)
        }

        // Draw connections with double-draw neon glowing effect
        fun drawLineBetween(start: Int, end: Int) {
            val startLandmark = pose.getPoseLandmark(start)
            val endLandmark = pose.getPoseLandmark(end)
            if (startLandmark != null && endLandmark != null &&
                startLandmark.inFrameLikelihood > 0.5f && endLandmark.inFrameLikelihood > 0.5f) {
                
                val startOffset = startLandmark.toOffset()
                val endOffset = endLandmark.toOffset()

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
            
            if (first != null && middle != null && last != null &&
                first.inFrameLikelihood > 0.5f && middle.inFrameLikelihood > 0.5f && last.inFrameLikelihood > 0.5f) {
                
                val angle = com.example.aifittracker.analysis.PoseUtils.calculateAngle(
                    first.position.x, first.position.y,
                    middle.position.x, middle.position.y,
                    last.position.x, last.position.y
                )
                
                val center = middle.toOffset()
                
                // Draw a nice glowing circle ring around the joint
                drawCircle(
                    color = color.copy(alpha = 0.2f),
                    radius = 35f,
                    center = center,
                    style = Stroke(width = 5f)
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
                    style = Stroke(width = 5f, cap = StrokeCap.Round)
                )
                
                // Draw the angle text using Android native canvas for rich drop-shadow and glow
                drawContext.canvas.nativeCanvas.drawText(
                    "${angle.toInt()}°",
                    center.x + 40f,
                    center.y + 10f,
                    android.graphics.Paint().apply {
                        this.color = color.toArgb()
                        this.textSize = 30f
                        this.typeface = android.graphics.Typeface.MONOSPACE
                        this.isFakeBoldText = true
                        this.setShadowLayer(8f, 0f, 0f, color.toArgb())
                    }
                )
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
        landmarks.forEach { landmark ->
            if (landmark.inFrameLikelihood > 0.5f) {
                val offset = landmark.toOffset()
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

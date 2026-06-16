package com.example.aifittracker.analysis

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions

class PoseAnalyzer(
    private val exerciseType: ExerciseType,
    private val onPoseDetected: (Pose, Int, Int) -> Unit,
    private val onRepDetected: () -> Unit,
    private val onStateChanged: (String) -> Unit,
    private val onFeedbackChanged: (String) -> Unit,
    private val onActiveLegChanged: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val options = PoseDetectorOptions.Builder()
        .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
        .build()

    private val detector = PoseDetection.getClient(options)

    private val activeAnalyzer: ExerciseAnalyzer = when (exerciseType) {
        ExerciseType.SQUAT -> SquatAnalyzer()
        ExerciseType.PUSH_UP -> PushUpAnalyzer()
        ExerciseType.PLANK -> PlankAnalyzer()
        ExerciseType.JUMPING_JACK -> JumpingJackAnalyzer()
        ExerciseType.BICEP_CURL -> BicepCurlAnalyzer()
    }

    // Power saving mode state
    private var lastValidPoseTimestamp: Long = System.currentTimeMillis()
    private var frameCounter = 0

    private fun checkPoseVisibility(pose: Pose): Boolean {
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)

        var visibleCount = 0
        if (leftShoulder != null && leftShoulder.inFrameLikelihood > 0.5f) visibleCount++
        if (rightShoulder != null && rightShoulder.inFrameLikelihood > 0.5f) visibleCount++
        if (leftHip != null && leftHip.inFrameLikelihood > 0.5f) visibleCount++
        if (rightHip != null && rightHip.inFrameLikelihood > 0.5f) visibleCount++

        return visibleCount >= 2
    }

    private var isClosed = false

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        if (isClosed) {
            imageProxy.close()
            return
        }

        val currentTime = System.currentTimeMillis()
        val timeSinceLastValidPose = currentTime - lastValidPoseTimestamp

        // Nếu không phát hiện tư thế người trong 10 giây, giảm tần suất quét xuống 1/10 frame để tiết kiệm pin
        if (timeSinceLastValidPose > 10000) {
            frameCounter++
            if (frameCounter % 10 != 0) {
                imageProxy.close()
                return
            }
        } else {
            frameCounter = 0
        }

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            val isRotated = rotationDegrees == 90 || rotationDegrees == 270
            val width = if (isRotated) imageProxy.height else imageProxy.width
            val height = if (isRotated) imageProxy.width else imageProxy.height

            val image = InputImage.fromMediaImage(mediaImage, rotationDegrees)
            if (isClosed) {
                imageProxy.close()
                return
            }
            try {
                detector.process(image)
                    .addOnSuccessListener { pose ->
                        if (isClosed) return@addOnSuccessListener
                        if (checkPoseVisibility(pose)) {
                            lastValidPoseTimestamp = System.currentTimeMillis()
                        }
                        onPoseDetected(pose, width, height)
                        activeAnalyzer.analyze(
                            pose = pose,
                            onFeedback = onFeedbackChanged,
                            onState = onStateChanged,
                            onRep = onRepDetected,
                            onActiveLeg = onActiveLegChanged
                        )
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            } catch (e: Exception) {
                imageProxy.close()
            }
        } else {
            imageProxy.close()
        }
    }

    fun close() {
        isClosed = true
        try {
            detector.close()
        } catch (e: Exception) {
            // ignore
        }
    }
}

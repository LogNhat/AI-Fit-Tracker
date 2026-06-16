package com.example.aifittracker.analysis

import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark

class JumpingJackAnalyzer : ExerciseAnalyzer {
    private var jumpingJackState = JumpingJackState.CLOSED
    private var jackReachedOpen = false

    override fun analyze(
        pose: Pose,
        onFeedback: (String) -> Unit,
        onState: (String) -> Unit,
        onRep: () -> Unit,
        onActiveLeg: (String) -> Unit
    ) {
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)

        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)
        val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)

        val isPoseVisible = leftShoulder != null && leftWrist != null && leftAnkle != null && leftHip != null &&
                rightShoulder != null && rightWrist != null && rightAnkle != null && rightHip != null &&
                leftShoulder.inFrameLikelihood > 0.5f && leftWrist.inFrameLikelihood > 0.5f && leftAnkle.inFrameLikelihood > 0.5f &&
                rightShoulder.inFrameLikelihood > 0.5f && rightWrist.inFrameLikelihood > 0.5f && rightAnkle.inFrameLikelihood > 0.5f

        if (isPoseVisible) {
            onActiveLeg("Full Body")

            // Check if hands are raised above shoulders (Y decreases going up)
            val isHandsUp = leftWrist!!.position.y < leftShoulder!!.position.y &&
                    rightWrist!!.position.y < rightShoulder!!.position.y

            // Check if legs are spread out (Ankle width > 1.4x Hip width)
            val hipWidth = Math.abs(leftHip!!.position.x - rightHip!!.position.x)
            val ankleDistance = Math.abs(leftAnkle!!.position.x - rightAnkle!!.position.x)
            val isLegsSpread = ankleDistance > (hipWidth * 1.5f)

            when (jumpingJackState) {
                JumpingJackState.CLOSED -> {
                    if (isHandsUp && isLegsSpread) {
                        jumpingJackState = JumpingJackState.OPEN
                        jackReachedOpen = true
                        onState(jumpingJackState.name)
                        onFeedback("Open!")
                    } else if (isHandsUp && !isLegsSpread) {
                        onFeedback("Spread your legs!")
                    } else if (!isHandsUp && isLegsSpread) {
                        onFeedback("Raise your hands!")
                    }
                }
                JumpingJackState.OPEN -> {
                    if (!isHandsUp && !isLegsSpread) {
                        if (jackReachedOpen) {
                            onRep()
                            onFeedback("Good jack!")
                        }
                        jumpingJackState = JumpingJackState.CLOSED
                        jackReachedOpen = false
                        onState(jumpingJackState.name)
                    } else if (!isHandsUp && isLegsSpread) {
                        onFeedback("Raise your hands!")
                    } else if (isHandsUp && !isLegsSpread) {
                        onFeedback("Close your legs!")
                    }
                }
            }
        } else {
            onActiveLeg("None")
            if (jumpingJackState != JumpingJackState.CLOSED) {
                jumpingJackState = JumpingJackState.CLOSED
                jackReachedOpen = false
                onState(jumpingJackState.name)
                onFeedback("Ready")
            }
        }
    }

    override fun reset() {
        jumpingJackState = JumpingJackState.CLOSED
        jackReachedOpen = false
    }
}

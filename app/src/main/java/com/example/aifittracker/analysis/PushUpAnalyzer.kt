package com.example.aifittracker.analysis

import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark

class PushUpAnalyzer : ExerciseAnalyzer {
    private var pushUpState = PushUpState.UP
    private var pushUpReachedBottom = false

    override fun analyze(
        pose: Pose,
        onFeedback: (String) -> Unit,
        onState: (String) -> Unit,
        onRep: () -> Unit,
        onActiveLeg: (String) -> Unit
    ) {
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val leftElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW)
        val leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)

        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val rightElbow = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW)
        val rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)

        val leftSideVisible = leftShoulder != null && leftElbow != null && leftWrist != null &&
                leftShoulder.inFrameLikelihood > 0.5f && leftElbow.inFrameLikelihood > 0.5f && leftWrist.inFrameLikelihood > 0.5f

        val rightSideVisible = rightShoulder != null && rightElbow != null && rightWrist != null &&
                rightShoulder.inFrameLikelihood > 0.5f && rightElbow.inFrameLikelihood > 0.5f && rightWrist.inFrameLikelihood > 0.5f

        if (leftSideVisible || rightSideVisible) {
            val isLeftActive = if (leftSideVisible && rightSideVisible) {
                val leftAvgConf = (leftShoulder!!.inFrameLikelihood + leftElbow!!.inFrameLikelihood + leftWrist!!.inFrameLikelihood) / 3f
                val rightAvgConf = (rightShoulder!!.inFrameLikelihood + rightElbow!!.inFrameLikelihood + rightWrist!!.inFrameLikelihood) / 3f
                leftAvgConf >= rightAvgConf
            } else {
                leftSideVisible
            }

            onActiveLeg(if (isLeftActive) "Left Arm" else "Right Arm")

            val elbowAngle = if (isLeftActive) {
                PoseUtils.calculateAngle(
                    leftShoulder!!.position.x, leftShoulder.position.y,
                    leftElbow!!.position.x, leftElbow.position.y,
                    leftWrist!!.position.x, leftWrist.position.y
                )
            } else {
                PoseUtils.calculateAngle(
                    rightShoulder!!.position.x, rightShoulder.position.y,
                    rightElbow!!.position.x, rightElbow.position.y,
                    rightWrist!!.position.x, rightWrist.position.y
                )
            }

            when (pushUpState) {
                PushUpState.UP -> {
                    if (elbowAngle < 150) {
                        pushUpState = PushUpState.DESCENDING
                        onState(pushUpState.name)
                        onFeedback("Going down...")
                    }
                }
                PushUpState.DESCENDING -> {
                    if (elbowAngle < 90) {
                        pushUpState = PushUpState.BOTTOM
                        pushUpReachedBottom = true
                        onState(pushUpState.name)
                        onFeedback("Good depth!")
                    } else if (elbowAngle > 160) {
                        pushUpState = PushUpState.UP
                        onState(pushUpState.name)
                        onFeedback("Too shallow! Go lower.")
                    }
                }
                PushUpState.BOTTOM -> {
                    if (elbowAngle > 100) {
                        pushUpState = PushUpState.ASCENDING
                        onState(pushUpState.name)
                        onFeedback("Push up!")
                    }
                }
                PushUpState.ASCENDING -> {
                    if (elbowAngle > 150) {
                        if (pushUpReachedBottom) {
                            onRep()
                            onFeedback("Good rep!")
                        } else {
                            onFeedback("Go lower next time!")
                        }
                        pushUpState = PushUpState.UP
                        pushUpReachedBottom = false
                        onState(pushUpState.name)
                    }
                }
            }
        } else {
            onActiveLeg("None")
            if (pushUpState != PushUpState.UP) {
                pushUpState = PushUpState.UP
                pushUpReachedBottom = false
                onState(pushUpState.name)
                onFeedback("Ready")
            }
        }
    }

    override fun reset() {
        pushUpState = PushUpState.UP
        pushUpReachedBottom = false
    }
}

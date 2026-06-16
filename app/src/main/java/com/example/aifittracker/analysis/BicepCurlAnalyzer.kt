package com.example.aifittracker.analysis

import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark

class BicepCurlAnalyzer : ExerciseAnalyzer {
    private var bicepCurlState = BicepCurlState.EXTENDED
    private var bicepCurlReachedPeak = false

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

            val shoulder = if (isLeftActive) leftShoulder!! else rightShoulder!!
            val elbow = if (isLeftActive) leftElbow!! else rightElbow!!
            val wrist = if (isLeftActive) leftWrist!! else rightWrist!!

            val elbowAngle = PoseUtils.calculateAngle(
                shoulder.position.x, shoulder.position.y,
                elbow.position.x, elbow.position.y,
                wrist.position.x, wrist.position.y
            )

            when (bicepCurlState) {
                BicepCurlState.EXTENDED -> {
                    if (elbowAngle < 140) {
                        bicepCurlState = BicepCurlState.FLEXING
                        onState(bicepCurlState.name)
                        onFeedback("Curl up...")
                    }
                }
                BicepCurlState.FLEXING -> {
                    if (elbowAngle < 45) {
                        bicepCurlState = BicepCurlState.FLEXED
                        bicepCurlReachedPeak = true
                        onState(bicepCurlState.name)
                        onFeedback("Squeeze!")
                    } else if (elbowAngle > 150) {
                        bicepCurlState = BicepCurlState.EXTENDED
                        onState(bicepCurlState.name)
                        onFeedback("Ready")
                    }
                }
                BicepCurlState.FLEXED -> {
                    if (elbowAngle > 55) {
                        bicepCurlState = BicepCurlState.EXTENDING
                        onState(bicepCurlState.name)
                        onFeedback("Lower down...")
                    }
                }
                BicepCurlState.EXTENDING -> {
                    if (elbowAngle > 140) {
                        if (bicepCurlReachedPeak) {
                            onRep()
                            onFeedback("Good curl!")
                        } else {
                            onFeedback("Curl higher next time!")
                        }
                        bicepCurlState = BicepCurlState.EXTENDED
                        bicepCurlReachedPeak = false
                        onState(bicepCurlState.name)
                    } else if (elbowAngle < 50) {
                        // Went back up
                        bicepCurlState = BicepCurlState.FLEXED
                        onState(bicepCurlState.name)
                        onFeedback("Squeeze!")
                    }
                }
            }
        } else {
            onActiveLeg("None")
            if (bicepCurlState != BicepCurlState.EXTENDED) {
                bicepCurlState = BicepCurlState.EXTENDED
                bicepCurlReachedPeak = false
                onState(bicepCurlState.name)
                onFeedback("Ready")
            }
        }
    }

    override fun reset() {
        bicepCurlState = BicepCurlState.EXTENDED
        bicepCurlReachedPeak = false
    }
}

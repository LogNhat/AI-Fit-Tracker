package com.example.aifittracker.analysis

import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark

class PlankAnalyzer : ExerciseAnalyzer {
    private var plankState = PlankState.INCORRECT

    override fun analyze(
        pose: Pose,
        onFeedback: (String) -> Unit,
        onState: (String) -> Unit,
        onRep: () -> Unit,
        onActiveLeg: (String) -> Unit
    ) {
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)

        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)
        val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)

        val leftSideVisible = leftShoulder != null && leftHip != null && leftKnee != null && leftAnkle != null &&
                leftShoulder.inFrameLikelihood > 0.5f && leftHip.inFrameLikelihood > 0.5f && leftKnee.inFrameLikelihood > 0.5f && leftAnkle.inFrameLikelihood > 0.5f

        val rightSideVisible = rightShoulder != null && rightHip != null && rightKnee != null && rightAnkle != null &&
                rightShoulder.inFrameLikelihood > 0.5f && rightHip.inFrameLikelihood > 0.5f && rightKnee.inFrameLikelihood > 0.5f && rightAnkle.inFrameLikelihood > 0.5f

        if (leftSideVisible || rightSideVisible) {
            val isLeftActive = if (leftSideVisible && rightSideVisible) {
                val leftAvgConf = (leftShoulder!!.inFrameLikelihood + leftHip!!.inFrameLikelihood + leftKnee!!.inFrameLikelihood + leftAnkle!!.inFrameLikelihood) / 4f
                val rightAvgConf = (rightShoulder!!.inFrameLikelihood + rightHip!!.inFrameLikelihood + rightKnee!!.inFrameLikelihood + rightAnkle!!.inFrameLikelihood) / 4f
                leftAvgConf >= rightAvgConf
            } else {
                leftSideVisible
            }

            onActiveLeg(if (isLeftActive) "Left Profile" else "Right Profile")

            val shoulder = if (isLeftActive) leftShoulder!! else rightShoulder!!
            val hip = if (isLeftActive) leftHip!! else rightHip!!
            val knee = if (isLeftActive) leftKnee!! else rightKnee!!
            val ankle = if (isLeftActive) leftAnkle!! else rightAnkle!!

            val hipAngle = PoseUtils.calculateAngle(
                shoulder.position.x, shoulder.position.y,
                hip.position.x, hip.position.y,
                knee.position.x, knee.position.y
            )
            val kneeAngle = PoseUtils.calculateAngle(
                hip.position.x, hip.position.y,
                knee.position.x, knee.position.y,
                ankle.position.x, ankle.position.y
            )

            val isBackStraight = hipAngle in 160.0..200.0
            val areLegsStraight = kneeAngle > 160.0

            if (isBackStraight && areLegsStraight) {
                plankState = PlankState.CORRECT
                onState(plankState.name)
                onFeedback("Perfect form! Keep holding.")
            } else {
                plankState = PlankState.INCORRECT
                onState(plankState.name)
                if (hipAngle < 160.0) {
                    onFeedback("Lower your hips!")
                } else if (hipAngle > 200.0) {
                    onFeedback("Straighten your back!")
                } else {
                    onFeedback("Keep legs straight!")
                }
            }
        } else {
            onActiveLeg("None")
            if (plankState != PlankState.INCORRECT) {
                plankState = PlankState.INCORRECT
                onState(plankState.name)
                onFeedback("Ready")
            }
        }
    }

    override fun reset() {
        plankState = PlankState.INCORRECT
    }
}

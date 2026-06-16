package com.example.aifittracker.analysis

import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark

class SquatAnalyzer(
    private val onRepDetected: (() -> Unit)? = null,
    private val onStateChanged: ((SquatState) -> Unit)? = null,
    private val onFeedbackChanged: ((String) -> Unit)? = null
) : ExerciseAnalyzer {
    var squatState = SquatState.STANDING
        private set
    var squatReachedBottom = false
        private set

    fun processKneeAngle(kneeAngle: Double) {
        processKneeAngle(
            kneeAngle = kneeAngle,
            onFeedback = { onFeedbackChanged?.invoke(it) },
            onState = { state -> onStateChanged?.invoke(SquatState.valueOf(state)) },
            onRep = { onRepDetected?.invoke() }
        )
    }

    private fun processKneeAngle(
        kneeAngle: Double,
        onFeedback: (String) -> Unit,
        onState: (String) -> Unit,
        onRep: () -> Unit
    ) {
        when (squatState) {
            SquatState.STANDING -> {
                if (kneeAngle < 160) {
                    squatState = SquatState.DESCENDING
                    onState(squatState.name)
                    onFeedback("Going down...")
                }
            }
            SquatState.DESCENDING -> {
                if (kneeAngle < 95) {
                    squatState = SquatState.BOTTOM
                    squatReachedBottom = true
                    onState(squatState.name)
                    onFeedback("Nice depth!")
                } else if (kneeAngle > 165) {
                    squatState = SquatState.STANDING
                    onState(squatState.name)
                    onFeedback("Too shallow! Go deeper.")
                }
            }
            SquatState.BOTTOM -> {
                if (kneeAngle > 105) {
                    squatState = SquatState.ASCENDING
                    onState(squatState.name)
                    onFeedback("Push up!")
                }
            }
            SquatState.ASCENDING -> {
                if (kneeAngle > 160) {
                    if (squatReachedBottom) {
                        onRep()
                        onFeedback("Good rep!")
                    } else {
                        onFeedback("Go deeper next time!")
                    }
                    squatState = SquatState.STANDING
                    squatReachedBottom = false
                    onState(squatState.name)
                }
            }
        }
    }

    override fun analyze(
        pose: Pose,
        onFeedback: (String) -> Unit,
        onState: (String) -> Unit,
        onRep: () -> Unit,
        onActiveLeg: (String) -> Unit
    ) {
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)

        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)
        val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)

        val leftSideVisible = leftHip != null && leftKnee != null && leftAnkle != null &&
                leftHip.inFrameLikelihood > 0.5f && leftKnee.inFrameLikelihood > 0.5f && leftAnkle.inFrameLikelihood > 0.5f

        val rightSideVisible = rightHip != null && rightKnee != null && rightAnkle != null &&
                rightHip.inFrameLikelihood > 0.5f && rightKnee.inFrameLikelihood > 0.5f && rightAnkle.inFrameLikelihood > 0.5f

        if (leftSideVisible || rightSideVisible) {
            val isLeftActive = if (leftSideVisible && rightSideVisible) {
                val leftAvgConf = (leftHip!!.inFrameLikelihood + leftKnee!!.inFrameLikelihood + leftAnkle!!.inFrameLikelihood) / 3f
                val rightAvgConf = (rightHip!!.inFrameLikelihood + rightKnee!!.inFrameLikelihood + rightAnkle!!.inFrameLikelihood) / 3f
                leftAvgConf >= rightAvgConf
            } else {
                leftSideVisible
            }

            onActiveLeg(if (isLeftActive) "Left Leg" else "Right Leg")

            val kneeAngle = if (isLeftActive) {
                PoseUtils.calculateAngle(
                    leftHip!!.position.x, leftHip.position.y,
                    leftKnee!!.position.x, leftKnee.position.y,
                    leftAnkle!!.position.x, leftAnkle.position.y
                )
            } else {
                PoseUtils.calculateAngle(
                    rightHip!!.position.x, rightHip.position.y,
                    rightKnee!!.position.x, rightKnee.position.y,
                    rightAnkle!!.position.x, rightAnkle.position.y
                )
            }

            processKneeAngle(kneeAngle, onFeedback, onState, onRep)
        } else {
            onActiveLeg("None")
            if (squatState != SquatState.STANDING) {
                squatState = SquatState.STANDING
                squatReachedBottom = false
                onState(squatState.name)
                onFeedback("Ready")
            }
        }
    }

    override fun reset() {
        if (squatState != SquatState.STANDING) {
            squatState = SquatState.STANDING
            squatReachedBottom = false
            onStateChanged?.invoke(squatState)
            onFeedbackChanged?.invoke("Ready")
        }
    }
}

package com.example.aifittracker.analysis

class SquatAnalyzer(
    private val onRepDetected: () -> Unit,
    private val onStateChanged: (SquatState) -> Unit,
    private val onFeedbackChanged: (String) -> Unit
) {
    var squatState = SquatState.STANDING
        private set
    var squatReachedBottom = false
        private set

    fun processKneeAngle(kneeAngle: Double) {
        when (squatState) {
            SquatState.STANDING -> {
                if (kneeAngle < 160) {
                    squatState = SquatState.DESCENDING
                    onStateChanged(squatState)
                    onFeedbackChanged("Going down...")
                }
            }
            SquatState.DESCENDING -> {
                if (kneeAngle < 95) {
                    squatState = SquatState.BOTTOM
                    squatReachedBottom = true
                    onStateChanged(squatState)
                    onFeedbackChanged("Nice depth!")
                } else if (kneeAngle > 165) {
                    squatState = SquatState.STANDING
                    onStateChanged(squatState)
                    onFeedbackChanged("Too shallow! Go deeper.")
                }
            }
            SquatState.BOTTOM -> {
                if (kneeAngle > 105) {
                    squatState = SquatState.ASCENDING
                    onStateChanged(squatState)
                    onFeedbackChanged("Push up!")
                }
            }
            SquatState.ASCENDING -> {
                if (kneeAngle > 160) {
                    if (squatReachedBottom) {
                        onRepDetected()
                        onFeedbackChanged("Good rep!")
                    } else {
                        onFeedbackChanged("Go deeper next time!")
                    }
                    squatState = SquatState.STANDING
                    squatReachedBottom = false
                    onStateChanged(squatState)
                }
            }
        }
    }

    fun reset() {
        if (squatState != SquatState.STANDING) {
            squatState = SquatState.STANDING
            squatReachedBottom = false
            onStateChanged(squatState)
            onFeedbackChanged("Ready")
        }
    }
}

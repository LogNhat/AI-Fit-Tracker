package com.example.aifittracker.analysis

import com.google.mlkit.vision.pose.Pose

interface ExerciseAnalyzer {
    fun analyze(
        pose: Pose,
        onFeedback: (String) -> Unit,
        onState: (String) -> Unit,
        onRep: () -> Unit,
        onActiveLeg: (String) -> Unit
    )
    fun reset()
}

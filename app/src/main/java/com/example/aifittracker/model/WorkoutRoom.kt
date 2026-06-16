package com.example.aifittracker.model

import com.example.aifittracker.analysis.ExerciseType

data class WorkoutRoom(
    val id: String,
    val name: String,
    val hostName: String,
    val exerciseType: ExerciseType,
    val participantCount: Int,
    val maxParticipants: Int
)

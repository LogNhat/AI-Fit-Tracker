package com.example.aifittracker.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_logs")
data class WorkoutLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    @ColumnInfo(name = "exercise_type") val exerciseType: String,
    val reps: Int,
    val duration: Int,
    val timestamp: Long = System.currentTimeMillis()
)

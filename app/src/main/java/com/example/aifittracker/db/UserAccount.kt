package com.example.aifittracker.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_accounts")
data class UserAccount(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val username: String,
    val name: String,
    val height: Double = 175.0, // in cm
    val weight: Double = 70.0,  // in kg
    val age: Int = 25,
    val targetGoal: String = "Tăng cơ",
    val password: String = ""
)

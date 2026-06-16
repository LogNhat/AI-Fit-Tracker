package com.example.aifittracker.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_vouchers")
data class UserVoucher(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val title: String,
    val category: String,
    val code: String,
    val timestamp: Long = System.currentTimeMillis()
)

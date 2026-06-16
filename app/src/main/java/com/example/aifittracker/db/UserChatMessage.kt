package com.example.aifittracker.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class UserChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val senderName: String,
    val messageText: String,
    val isCoach: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

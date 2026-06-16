package com.example.aifittracker.model

data class LeaderboardUser(
    val rank: Int,
    val name: String,
    val score: String,
    val isCurrentUser: Boolean = false
)

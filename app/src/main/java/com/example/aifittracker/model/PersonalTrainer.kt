package com.example.aifittracker.model

data class PersonalTrainer(
    val id: String,
    val name: String,
    val rating: Float,
    val clientsCount: Int,
    val specialties: List<String>,
    val isConnected: Boolean = false
)

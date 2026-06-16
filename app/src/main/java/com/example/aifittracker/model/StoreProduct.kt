package com.example.aifittracker.model

data class StoreProduct(
    val id: String,
    val name: String,
    val priceCash: String,
    val priceCoins: Int,
    val category: String, // "Equipment", "Nutrition", "Voucher"
    val description: String
)

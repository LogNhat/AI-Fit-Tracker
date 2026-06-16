package com.example.aifittracker.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_wallet")
data class UserWallet(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "coin_balance") val coinBalance: Int = 150
)

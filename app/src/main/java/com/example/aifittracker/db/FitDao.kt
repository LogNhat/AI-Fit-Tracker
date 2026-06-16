package com.example.aifittracker.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FitDao {

    @Query("SELECT coin_balance FROM user_wallet WHERE id = :userId")
    suspend fun getCoinBalance(userId: Int): Int?

    @Query("INSERT OR REPLACE INTO user_wallet (id, coin_balance) VALUES (:userId, :balance)")
    suspend fun updateCoinBalance(userId: Int, balance: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutLog(log: WorkoutLog)

    @Query("SELECT COUNT(*) FROM workout_logs WHERE userId = :userId")
    suspend fun getWorkoutLogsCount(userId: Int): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoucher(voucher: UserVoucher)

    @Query("SELECT * FROM user_vouchers WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAllVouchers(userId: Int): Flow<List<UserVoucher>>

    @Query("SELECT * FROM workout_logs WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAllWorkoutLogs(userId: Int): Flow<List<WorkoutLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: UserChatMessage)

    @Query("SELECT * FROM chat_messages WHERE userId = :userId ORDER BY timestamp ASC")
    fun getAllChatMessages(userId: Int): Flow<List<UserChatMessage>>

    @Query("SELECT * FROM user_accounts WHERE id = 1")
    suspend fun getUserAccount(): UserAccount?

    // --- User Account Operations ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserAccount(user: UserAccount): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateUserAccount(user: UserAccount)

    @Query("SELECT * FROM user_accounts WHERE username = :username LIMIT 1")
    suspend fun getUserAccountByUsername(username: String): UserAccount?

    @Query("SELECT * FROM user_accounts WHERE id = :userId LIMIT 1")
    suspend fun getUserAccountById(userId: Int): UserAccount?
}

package com.example.aifittracker.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [UserWallet::class, WorkoutLog::class, UserVoucher::class, UserChatMessage::class, UserAccount::class], version = 2, exportSchema = false)
abstract class FitDatabase : RoomDatabase() {

    abstract fun fitDao(): FitDao

    companion object {
        @Volatile
        private var INSTANCE: FitDatabase? = null

        fun getDatabase(context: Context): FitDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FitDatabase::class.java,
                    "fit_tracker_room.db"
                )
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Initialize user with default 150 coins on database creation
                        db.execSQL("INSERT OR IGNORE INTO user_wallet (id, coin_balance) VALUES (1, 150)")
                        // Initialize user account
                        db.execSQL("INSERT OR IGNORE INTO user_accounts (id, username, name, height, weight, age, targetGoal, password) VALUES (1, '@you_longnhat', 'Long Nhất', 175.0, 70.0, 25, 'Tăng cơ', '')")
                    }
                })
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

package com.cristiancogollo.biblion.feature.bibi.data

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

private const val TAG = "BibiHistoryDatabase"

@Database(entities = [BibiHistoryEntity::class], version = 1, exportSchema = false)
abstract class BibiHistoryDatabase : RoomDatabase() {

    abstract fun bibiHistoryDao(): BibiHistoryDao

    companion object {
        @Volatile
        private var instance: BibiHistoryDatabase? = null

        fun getInstance(context: Context): BibiHistoryDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    BibiHistoryDatabase::class.java,
                    "bibi_history.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
        }
    }
}

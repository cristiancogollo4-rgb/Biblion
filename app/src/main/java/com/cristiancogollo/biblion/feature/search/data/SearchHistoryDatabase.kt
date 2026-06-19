package com.cristiancogollo.biblion.feature.search.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [SearchHistoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class SearchHistoryDatabase : RoomDatabase() {
    abstract fun searchHistoryDao(): SearchHistoryDao

    companion object {
        @Volatile
        private var instance: SearchHistoryDatabase? = null

        fun getInstance(context: Context): SearchHistoryDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    SearchHistoryDatabase::class.java,
                    "search_history.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
        }
    }
}

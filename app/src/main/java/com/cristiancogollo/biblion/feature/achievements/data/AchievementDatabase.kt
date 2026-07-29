package com.cristiancogollo.biblion.feature.achievements.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [AchievementProgressEntity::class, AchievementUniqueValueEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AchievementDatabase : RoomDatabase() {
    abstract fun achievementDao(): AchievementDao

    companion object {
        @Volatile private var instance: AchievementDatabase? = null

        fun getInstance(context: Context): AchievementDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AchievementDatabase::class.java,
                    "achievements.db",
                ).build().also { instance = it }
            }
    }
}

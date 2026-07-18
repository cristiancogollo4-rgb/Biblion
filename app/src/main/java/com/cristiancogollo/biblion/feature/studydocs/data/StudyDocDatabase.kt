package com.cristiancogollo.biblion.feature.studydocs.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [StudyDocEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class StudyDocDatabase : RoomDatabase() {

    abstract fun studyDocDao(): StudyDocDao

    companion object {
        private const val DB_NAME = "study_docs.db"

        @Volatile
        private var instance: StudyDocDatabase? = null

        fun getInstance(context: Context): StudyDocDatabase = instance ?: synchronized(this) {
            instance ?: build(context).also { instance = it }
        }

        fun build(context: Context): StudyDocDatabase = Room.databaseBuilder(
            context.applicationContext,
            StudyDocDatabase::class.java,
            DB_NAME,
        ).fallbackToDestructiveMigration().build()

        fun resetInstance() { synchronized(this) { instance = null } }
    }
}

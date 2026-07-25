package com.cristiancogollo.biblion.feature.studydocs.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [StudyDocEntity::class],
    version = 3,
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
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()

        /** v2 eliminó el historial antiguo; los documentos permanecen intactos. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE IF EXISTS doc_edit_history")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Existing documents were already visible; only new autosaved drafts start hidden.
                database.execSQL(
                    "ALTER TABLE study_docs ADD COLUMN is_published INTEGER NOT NULL DEFAULT 1",
                )
            }
        }

        fun resetInstance() { synchronized(this) { instance = null } }
    }
}

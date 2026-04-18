package com.cristiancogollo.biblion

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.migration.Migration
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyDao {
    @Query("SELECT * FROM study_notebooks WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    fun observeNotebooks(): Flow<List<StudyNotebookEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotebook(notebook: StudyNotebookEntity): Long

    @Update
    suspend fun updateNotebook(notebook: StudyNotebookEntity)

    @Query("SELECT * FROM study_notebooks WHERE id = :id LIMIT 1")
    suspend fun getNotebook(id: Long): StudyNotebookEntity?

    @Query("SELECT * FROM study_notebooks WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getNotebookByRemoteId(remoteId: String): StudyNotebookEntity?

    @Query("SELECT * FROM study_notebooks ORDER BY updatedAt DESC")
    suspend fun getAllNotebooksForSync(): List<StudyNotebookEntity>

    @Query("SELECT COUNT(*) FROM study_notebooks")
    suspend fun getNotebookCount(): Int

    @Query("SELECT * FROM studies WHERE notebookId = :notebookId AND deletedAt IS NULL ORDER BY updatedAt DESC")
    fun observeStudies(notebookId: Long): Flow<List<StudyEntity>>

    @Query("SELECT * FROM studies WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    fun observeAllStudies(): Flow<List<StudyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudy(study: StudyEntity): Long

    @Update
    suspend fun updateStudy(study: StudyEntity)

    @Query("SELECT * FROM studies WHERE id = :id LIMIT 1")
    suspend fun getStudy(id: Long): StudyEntity?

    @Query("SELECT * FROM studies WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getStudyByRemoteId(remoteId: String): StudyEntity?

    @Query("SELECT * FROM studies ORDER BY updatedAt DESC")
    suspend fun getAllStudiesForSync(): List<StudyEntity>

    @Query("DELETE FROM studies WHERE id = :id")
    suspend fun deleteStudy(id: Long)

    @Query("DELETE FROM linked_citations WHERE estudioId = :studyId")
    suspend fun deleteCitationsForStudy(studyId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLinkedCitations(citations: List<LinkedCitationEntity>)

    @Query("SELECT * FROM linked_citations WHERE estudioId = :studyId")
    suspend fun getLinkedCitations(studyId: Long): List<LinkedCitationEntity>

    @Transaction
    suspend fun replaceCitations(studyId: Long, citations: List<LinkedCitationEntity>) {
        deleteCitationsForStudy(studyId)
        if (citations.isNotEmpty()) {
            insertLinkedCitations(citations)
        }
    }
}

@Database(
    entities = [StudyNotebookEntity::class, StudyEntity::class, LinkedCitationEntity::class],
    version = 2,
    exportSchema = false
)
abstract class StudyDatabase : RoomDatabase() {
    abstract fun studyDao(): StudyDao

    companion object {
        @Volatile
        private var instance: StudyDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE study_notebooks ADD COLUMN remoteId TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE study_notebooks ADD COLUMN ownerUid TEXT")
                database.execSQL("ALTER TABLE study_notebooks ADD COLUMN deletedAt INTEGER")
                database.execSQL("ALTER TABLE study_notebooks ADD COLUMN lastSyncedAt INTEGER")
                database.execSQL("ALTER TABLE study_notebooks ADD COLUMN syncVersion INTEGER NOT NULL DEFAULT 0")

                database.execSQL("ALTER TABLE studies ADD COLUMN remoteId TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE studies ADD COLUMN notebookRemoteId TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE studies ADD COLUMN ownerUid TEXT")
                database.execSQL("ALTER TABLE studies ADD COLUMN deletedAt INTEGER")
                database.execSQL("ALTER TABLE studies ADD COLUMN lastSyncedAt INTEGER")
                database.execSQL("ALTER TABLE studies ADD COLUMN syncVersion INTEGER NOT NULL DEFAULT 0")

                val notebookIdsToRemoteIds = mutableMapOf<Long, String>()
                database.query("SELECT id FROM study_notebooks").use { cursor ->
                    val idIndex = cursor.getColumnIndexOrThrow("id")
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idIndex)
                        val remoteId = CuidGenerator.create()
                        notebookIdsToRemoteIds[id] = remoteId
                        database.execSQL(
                            "UPDATE study_notebooks SET remoteId = ? WHERE id = ?",
                            arrayOf(remoteId, id)
                        )
                    }
                }

                database.query("SELECT id, notebookId FROM studies").use { cursor ->
                    val idIndex = cursor.getColumnIndexOrThrow("id")
                    val notebookIdIndex = cursor.getColumnIndexOrThrow("notebookId")
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idIndex)
                        val notebookId = cursor.getLong(notebookIdIndex)
                        val remoteId = CuidGenerator.create()
                        val notebookRemoteId = notebookIdsToRemoteIds[notebookId].orEmpty()
                        database.execSQL(
                            "UPDATE studies SET remoteId = ?, notebookRemoteId = ? WHERE id = ?",
                            arrayOf(remoteId, notebookRemoteId, id)
                        )
                    }
                }

                database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_study_notebooks_remoteId ON study_notebooks(remoteId)"
                )
                database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_studies_remoteId ON studies(remoteId)"
                )
            }
        }

        fun getInstance(context: Context): StudyDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    StudyDatabase::class.java,
                    "study_mode.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
        }
    }
}

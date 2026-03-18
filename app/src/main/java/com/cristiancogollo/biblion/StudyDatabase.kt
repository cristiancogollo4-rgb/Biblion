package com.cristiancogollo.biblion

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyDao {
    @Query("SELECT * FROM study_notebooks ORDER BY updatedAt DESC")
    fun observeNotebooks(): Flow<List<StudyNotebookEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotebook(notebook: StudyNotebookEntity): Long

    @Update
    suspend fun updateNotebook(notebook: StudyNotebookEntity)

    @Query("SELECT * FROM studies WHERE notebookId = :notebookId ORDER BY updatedAt DESC")
    fun observeStudies(notebookId: Long): Flow<List<StudyEntity>>

    @Query(
        """
        SELECT studies.*
        FROM studies
        INNER JOIN study_notebooks ON study_notebooks.id = studies.notebookId
        WHERE studies.title LIKE '%' || :query || '%'
           OR studies.contentSerialized LIKE '%' || :query || '%'
           OR study_notebooks.title LIKE '%' || :query || '%'
        ORDER BY studies.updatedAt DESC
        """
    )
    suspend fun searchStudies(query: String): List<StudyEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudy(study: StudyEntity): Long

    @Update
    suspend fun updateStudy(study: StudyEntity)

    @Query("DELETE FROM studies WHERE id = :studyId")
    suspend fun deleteStudy(studyId: Long)

    @Query("SELECT * FROM studies WHERE id = :id LIMIT 1")
    suspend fun getStudy(id: Long): StudyEntity?

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
    version = 1,
    exportSchema = false
)
abstract class StudyDatabase : RoomDatabase() {
    abstract fun studyDao(): StudyDao

    companion object {
        @Volatile
        private var instance: StudyDatabase? = null

        fun getInstance(context: Context): StudyDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    StudyDatabase::class.java,
                    "study_mode.db"
                ).build().also { instance = it }
            }
        }
    }
}

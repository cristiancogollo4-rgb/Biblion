package com.cristiancogollo.biblion.feature.studydocs.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.cristiancogollo.biblion.feature.studydocs.model.DocId
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyDocDao {

    @Query("SELECT * FROM study_docs WHERE deleted_at IS NULL AND is_published = 1 ORDER BY updated_at DESC")
    fun observeAll(): Flow<List<StudyDocEntity>>

    @Query("SELECT * FROM study_docs WHERE deleted_at IS NULL AND is_published = 1 AND notebook_remote_id = :notebookRemoteId ORDER BY updated_at DESC")
    fun observeByNotebook(notebookRemoteId: String): Flow<List<StudyDocEntity>>

    @Query("SELECT * FROM study_docs WHERE deleted_at IS NULL AND is_published = 1 AND owner_uid = :ownerUid ORDER BY updated_at DESC")
    fun observeByOwner(ownerUid: String): Flow<List<StudyDocEntity>>

    @Query("SELECT * FROM study_docs WHERE id = :id")
    suspend fun getById(id: Long): StudyDocEntity?

    @Query("SELECT * FROM study_docs WHERE remote_id = :remoteId")
    suspend fun getByRemoteId(remoteId: String): StudyDocEntity?

    @Query("SELECT * FROM study_docs WHERE (title LIKE :query OR tags_csv LIKE :query) AND deleted_at IS NULL AND is_published = 1 ORDER BY updated_at DESC")
    fun search(query: String): Flow<List<StudyDocEntity>>

    @Query(
        """
        SELECT * FROM study_docs
        WHERE is_dirty = 1
            AND deleted_at IS NULL
            AND is_published = 1
            AND (owner_uid IS NULL OR owner_uid = :ownerUid)
        """
    )
    suspend fun getDirtyForSync(ownerUid: String): List<StudyDocEntity>

    @Query(
        """
        SELECT * FROM study_docs
        WHERE deleted_at IS NOT NULL
            AND is_published = 1
            AND (owner_uid IS NULL OR owner_uid = :ownerUid)
        """
    )
    suspend fun getDeletedForSync(ownerUid: String): List<StudyDocEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: StudyDocEntity): Long

    @Update
    suspend fun update(entity: StudyDocEntity)

    @Query(
        """
        UPDATE study_docs
        SET deleted_at = :deletedAt, updated_at = :deletedAt, is_dirty = 1
        WHERE id = :id
        """
    )
    suspend fun softDelete(id: Long, deletedAt: Long)

    @Query("DELETE FROM study_docs WHERE id = :id")
    suspend fun hardDelete(id: Long)

    @Query("DELETE FROM study_docs WHERE remote_id = :remoteId")
    suspend fun hardDeleteByRemoteId(remoteId: String)

    @Query(
        """
        UPDATE study_docs
        SET last_synced_at = :syncedAt,
            sync_version = :syncVersion,
            owner_uid = :ownerUid,
            is_dirty = 0
        WHERE id = :id AND updated_at = :expectedUpdatedAt
        """
    )
    suspend fun markSyncedIfUnchanged(
        id: Long,
        expectedUpdatedAt: Long,
        syncedAt: Long,
        syncVersion: Long,
        ownerUid: String,
    ): Int

    @Query("SELECT COUNT(*) FROM study_docs WHERE deleted_at IS NULL AND is_published = 1")
    suspend fun countActive(): Int
}

package com.cristiancogollo.biblion.feature.studydocs.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DocVersionDao {

    @Query("SELECT * FROM doc_versions WHERE doc_remote_id = :docRemoteId ORDER BY version_number DESC")
    fun observeVersions(docRemoteId: String): Flow<List<DocVersionEntity>>

    @Query("SELECT * FROM doc_versions WHERE doc_remote_id = :docRemoteId ORDER BY version_number DESC")
    suspend fun getVersions(docRemoteId: String): List<DocVersionEntity>

    @Query("SELECT * FROM doc_versions WHERE id = :id")
    suspend fun getById(id: Long): DocVersionEntity?

    @Query("SELECT * FROM doc_versions WHERE doc_remote_id = :docRemoteId AND version_number = :versionNumber")
    suspend fun getByVersionNumber(docRemoteId: String, versionNumber: Int): DocVersionEntity?

    @Query("SELECT IFNULL(MAX(version_number), 0) FROM doc_versions WHERE doc_remote_id = :docRemoteId")
    suspend fun maxVersionNumber(docRemoteId: String): Int

    @Query("SELECT COUNT(*) FROM doc_versions WHERE doc_remote_id = :docRemoteId")
    suspend fun countVersions(docRemoteId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(version: DocVersionEntity): Long

    @Query("DELETE FROM doc_versions WHERE doc_remote_id = :docRemoteId")
    suspend fun deleteAllForDoc(docRemoteId: String)

    @Query("DELETE FROM doc_versions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query(
        """
        DELETE FROM doc_versions
        WHERE id IN (
          SELECT id FROM doc_versions
          WHERE doc_remote_id = :docRemoteId
          ORDER BY version_number ASC
          LIMIT :excess
        )
        """,
    )
    suspend fun trimToMaxVersions(docRemoteId: String, excess: Int)
}

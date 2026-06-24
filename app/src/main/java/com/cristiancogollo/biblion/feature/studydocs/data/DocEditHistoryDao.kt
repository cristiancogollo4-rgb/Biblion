package com.cristiancogollo.biblion.feature.studydocs.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface DocEditHistoryDao {

    @Query("SELECT * FROM doc_edit_history WHERE doc_remote_id = :docRemoteId ORDER BY seq ASC")
    fun observeByDoc(docRemoteId: String): Flow<List<DocEditHistoryEntity>>

    @Query("SELECT * FROM doc_edit_history WHERE doc_remote_id = :docRemoteId ORDER BY seq ASC")
    suspend fun getByDoc(docRemoteId: String): List<DocEditHistoryEntity>

    @Query("SELECT COUNT(*) FROM doc_edit_history WHERE doc_remote_id = :docRemoteId")
    suspend fun countByDoc(docRemoteId: String): Int

    @Query("SELECT IFNULL(MAX(seq), -1) FROM doc_edit_history WHERE doc_remote_id = :docRemoteId")
    suspend fun maxSeq(docRemoteId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: DocEditHistoryEntity): Long

    @Query("DELETE FROM doc_edit_history WHERE doc_remote_id = :docRemoteId")
    suspend fun clearForDoc(docRemoteId: String)

    @Transaction
    suspend fun appendAndTrim(
        docRemoteId: String,
        commandType: String,
        payloadJson: String,
        description: String,
        maxEntries: Int,
        createdAt: Long,
    ): Int {
        val nextSeq = maxSeq(docRemoteId) + 1
        insert(
            DocEditHistoryEntity(
                docRemoteId = docRemoteId,
                seq = nextSeq,
                commandType = commandType,
                payloadJson = payloadJson,
                description = description,
                createdAt = createdAt,
            ),
        )
        val total = countByDoc(docRemoteId)
        if (total > maxEntries) {
            deleteOldest(docRemoteId, total - maxEntries)
        }
        return nextSeq
    }

    @Query(
        """
        DELETE FROM doc_edit_history
        WHERE id IN (
          SELECT id FROM doc_edit_history
          WHERE doc_remote_id = :docRemoteId
          ORDER BY seq ASC
          LIMIT :excess
        )
        """,
    )
    suspend fun deleteOldest(docRemoteId: String, excess: Int)
}

package com.cristiancogollo.biblion.feature.bibi.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

@Dao
interface ChatSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: ChatSessionEntity): Long

    @Update
    suspend fun update(session: ChatSessionEntity)

    @Query("SELECT * FROM chat_sessions WHERE owner_uid = :ownerUid ORDER BY updated_at DESC")
    suspend fun getAll(ownerUid: String): List<ChatSessionEntity>

    @Query("SELECT * FROM chat_sessions WHERE id = :id AND owner_uid = :ownerUid")
    suspend fun getById(id: Long, ownerUid: String): ChatSessionEntity?

    @Query("SELECT * FROM chat_sessions WHERE owner_uid = :ownerUid ORDER BY updated_at DESC LIMIT 1")
    suspend fun getMostRecent(ownerUid: String): ChatSessionEntity?

    @Query(
        "SELECT * FROM chat_sessions " +
            "WHERE mode = :mode AND owner_uid = :ownerUid ORDER BY updated_at DESC LIMIT 1"
    )
    suspend fun getMostRecentByMode(mode: String, ownerUid: String): ChatSessionEntity?

    @Query(
        "SELECT * FROM chat_sessions " +
            "WHERE mode = :mode AND owner_uid = :ownerUid ORDER BY updated_at DESC"
    )
    suspend fun getByMode(mode: String, ownerUid: String): List<ChatSessionEntity>

    @Delete
    suspend fun delete(session: ChatSessionEntity)

    @Query("DELETE FROM chat_sessions WHERE id = :id AND owner_uid = :ownerUid")
    suspend fun deleteById(id: Long, ownerUid: String)

    @Query("DELETE FROM chat_sessions WHERE owner_uid = :ownerUid")
    suspend fun deleteAllByOwner(ownerUid: String)
}

@Dao
interface ChatMessageDao {

    @Insert
    suspend fun insert(message: ChatMessageEntity): Long

    @Query("SELECT * FROM chat_messages WHERE session_id = :sessionId ORDER BY created_at ASC")
    suspend fun getBySession(sessionId: Long): List<ChatMessageEntity>

    @Query("DELETE FROM chat_messages WHERE session_id = :sessionId")
    suspend fun deleteBySession(sessionId: Long)
}

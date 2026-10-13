package com.cristiancogollo.biblion.feature.bibi.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

const val GUEST_CHAT_OWNER = "__guest__"

@Entity(
    tableName = "chat_sessions",
    indices = [Index(value = ["owner_uid", "updated_at"])],
)
data class ChatSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "first_query")
    val firstQuery: String,

    @ColumnInfo(name = "mode")
    val mode: String,

    @ColumnInfo(name = "owner_uid")
    val ownerUid: String = GUEST_CHAT_OWNER,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

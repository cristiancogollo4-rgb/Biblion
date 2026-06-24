package com.cristiancogollo.biblion.feature.studydocs.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "doc_edit_history",
    indices = [
        Index(value = ["doc_remote_id", "seq"], unique = true),
        Index(value = ["doc_remote_id"]),
    ],
)
data class DocEditHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "doc_remote_id") val docRemoteId: String,
    @ColumnInfo(name = "seq") val seq: Int,
    @ColumnInfo(name = "command_type") val commandType: String,
    @ColumnInfo(name = "payload_json") val payloadJson: String,
    @ColumnInfo(name = "description") val description: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

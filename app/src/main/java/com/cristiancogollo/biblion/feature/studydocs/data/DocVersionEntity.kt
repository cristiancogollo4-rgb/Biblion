package com.cristiancogollo.biblion.feature.studydocs.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "doc_versions",
    indices = [
        Index(value = ["doc_remote_id", "version_number"], unique = true),
        Index(value = ["doc_remote_id"]),
        Index(value = ["created_at"]),
    ],
)
data class DocVersionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "doc_remote_id") val docRemoteId: String,
    @ColumnInfo(name = "version_number") val versionNumber: Int,
    @ColumnInfo(name = "doc_json") val docJson: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "block_count") val blockCount: Int,
    @ColumnInfo(name = "word_count") val wordCount: Int,
    @ColumnInfo(name = "change_description") val changeDescription: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

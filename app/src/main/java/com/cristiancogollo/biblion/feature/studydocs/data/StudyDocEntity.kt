package com.cristiancogollo.biblion.feature.studydocs.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "study_docs",
    indices = [
        Index(value = ["remote_id"], unique = true),
        Index(value = ["owner_uid"]),
        Index(value = ["updated_at"]),
    ],
)
data class StudyDocEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "remote_id") val remoteId: String,
    val title: String,
    @ColumnInfo(name = "notebook_remote_id") val notebookRemoteId: String? = null,
    @ColumnInfo(name = "owner_uid") val ownerUid: String? = null,
    @ColumnInfo(name = "tags_csv") val tagsCsv: String = "",
    @ColumnInfo(name = "block_count") val blockCount: Int = 0,
    val version: Int = 1,
    @ColumnInfo(name = "doc_json") val docJson: String = "{}",
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
    @ColumnInfo(name = "last_synced_at") val lastSyncedAt: Long? = null,
    @ColumnInfo(name = "sync_version") val syncVersion: Long = 0,
    @ColumnInfo(name = "is_dirty") val isDirty: Boolean = true,
)

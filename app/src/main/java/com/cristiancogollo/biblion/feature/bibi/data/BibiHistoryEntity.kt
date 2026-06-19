package com.cristiancogollo.biblion.feature.bibi.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bibi_history")
data class BibiHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "query")
    val query: String,

    @ColumnInfo(name = "response_text")
    val responseText: String,

    @ColumnInfo(name = "category")
    val category: String,

    @ColumnInfo(name = "bible_version")
    val bibleVersion: String = "",

    @ColumnInfo(name = "book")
    val book: String = "",

    @ColumnInfo(name = "chapter")
    val chapter: Int = 0,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "is_ai_response")
    val isAiResponse: Boolean = false
)

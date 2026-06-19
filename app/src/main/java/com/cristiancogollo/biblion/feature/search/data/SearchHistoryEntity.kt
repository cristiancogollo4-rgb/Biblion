package com.cristiancogollo.biblion.feature.search.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Historial de búsquedas del usuario en el buscador de versículos.
 * Permite mostrar búsquedas recientes y conteo de uso.
 */
@Entity(
    tableName = "search_history",
    indices = [Index(value = ["normalized_query"], unique = true)]
)
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "query")
    val query: String,

    @ColumnInfo(name = "normalized_query")
    val normalizedQuery: String,

    @ColumnInfo(name = "use_count")
    val useCount: Int = 1,

    @ColumnInfo(name = "last_used_at")
    val lastUsedAt: Long = System.currentTimeMillis()
)

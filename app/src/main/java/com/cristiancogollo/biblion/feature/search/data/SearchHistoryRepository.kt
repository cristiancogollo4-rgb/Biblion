package com.cristiancogollo.biblion.feature.search.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class SearchHistoryEntry(
    val id: Long,
    val query: String,
    val useCount: Int,
    val lastUsedAt: Long
)

object SearchHistoryRepository {

    private suspend fun dao(context: Context) =
        SearchHistoryDatabase.getInstance(context).searchHistoryDao()

    /**
     * Normaliza la query para deduplicar búsquedas equivalentes
     * (sin acentos, en minúsculas, sin espacios extras).
     */
    fun normalizeQuery(query: String): String {
        val accents = mapOf(
            'á' to 'a', 'é' to 'e', 'í' to 'i', 'ó' to 'o', 'ú' to 'u',
            'Á' to 'A', 'É' to 'E', 'Í' to 'I', 'Ó' to 'O', 'Ú' to 'U',
            'ñ' to 'n', 'Ñ' to 'N'
        )
        return query.lowercase()
            .trim()
            .map { accents[it] ?: it }
            .joinToString("")
    }

    suspend fun recordQuery(context: Context, query: String) = withContext(Dispatchers.IO) {
        try {
            val normalized = normalizeQuery(query)
            if (normalized.length < 2) return@withContext
            dao(context).recordQuery(normalized, query.trim())
        } catch (e: Exception) {
            android.util.Log.e("SearchHistory", "Error recording query", e)
        }
    }

    suspend fun getRecent(context: Context, limit: Int = 5): List<SearchHistoryEntry> =
        withContext(Dispatchers.IO) {
            try {
                dao(context).getRecent(limit).map { it.toDomain() }
            } catch (e: Exception) {
                emptyList()
            }
        }

    suspend fun getMostUsed(context: Context, limit: Int = 10): List<SearchHistoryEntry> =
        withContext(Dispatchers.IO) {
            try {
                dao(context).getMostUsed(limit).map { it.toDomain() }
            } catch (e: Exception) {
                emptyList()
            }
        }

    suspend fun deleteEntry(context: Context, id: Long) = withContext(Dispatchers.IO) {
        try {
            dao(context).deleteById(id)
        } catch (e: Exception) {
            android.util.Log.e("SearchHistory", "Error deleting entry", e)
        }
    }

    suspend fun clearAll(context: Context) = withContext(Dispatchers.IO) {
        try {
            dao(context).clearAll()
        } catch (e: Exception) {
            android.util.Log.e("SearchHistory", "Error clearing history", e)
        }
    }

    private fun SearchHistoryEntity.toDomain() = SearchHistoryEntry(
        id = id,
        query = query,
        useCount = useCount,
        lastUsedAt = lastUsedAt
    )
}

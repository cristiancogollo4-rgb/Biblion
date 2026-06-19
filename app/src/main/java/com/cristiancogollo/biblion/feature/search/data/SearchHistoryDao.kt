package com.cristiancogollo.biblion.feature.search.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchHistoryDao {

    @Query("SELECT * FROM search_history ORDER BY last_used_at DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 5): List<SearchHistoryEntity>

    @Query("SELECT * FROM search_history ORDER BY last_used_at DESC LIMIT :limit")
    fun getRecentFlow(limit: Int = 5): Flow<List<SearchHistoryEntity>>

    @Query("SELECT * FROM search_history ORDER BY use_count DESC, last_used_at DESC LIMIT :limit")
    suspend fun getMostUsed(limit: Int = 10): List<SearchHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SearchHistoryEntity)

    /**
     * Inserta o actualiza una búsqueda: si ya existe (mismo normalized_query),
     * incrementa el contador de uso y actualiza la fecha.
     */
    @Transaction
    suspend fun recordQuery(normalizedQuery: String, originalQuery: String) {
        val existing = getByNormalizedQuery(normalizedQuery)
        if (existing == null) {
            insert(
                SearchHistoryEntity(
                    query = originalQuery,
                    normalizedQuery = normalizedQuery
                )
            )
        } else {
            updateUsage(normalizedQuery, existing.useCount + 1)
        }
    }

    @Query("SELECT * FROM search_history WHERE normalized_query = :normalizedQuery LIMIT 1")
    suspend fun getByNormalizedQuery(normalizedQuery: String): SearchHistoryEntity?

    @Query(
        """
        UPDATE search_history
        SET use_count = :newCount, last_used_at = :timestamp
        WHERE normalized_query = :normalizedQuery
        """
    )
    suspend fun updateUsage(normalizedQuery: String, newCount: Int, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM search_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM search_history")
    suspend fun clearAll()
}

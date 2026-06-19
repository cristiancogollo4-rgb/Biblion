package com.cristiancogollo.biblion.feature.bibi.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BibiHistoryDao {

    @Insert
    suspend fun insert(entry: BibiHistoryEntity)

    @Query("SELECT * FROM bibi_history ORDER BY created_at DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 5): List<BibiHistoryEntity>

    @Query("SELECT * FROM bibi_history ORDER BY created_at DESC LIMIT :limit")
    fun getRecentFlow(limit: Int = 5): Flow<List<BibiHistoryEntity>>

    @Query("DELETE FROM bibi_history")
    suspend fun clearAll()

    @Query("SELECT * FROM bibi_history WHERE query LIKE :term ORDER BY created_at DESC LIMIT 5")
    suspend fun getByTerm(term: String): List<BibiHistoryEntity>
}

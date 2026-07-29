package com.cristiancogollo.biblion.feature.achievements.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertProgress(rows: List<AchievementProgressEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEvidence(value: AchievementUniqueValueEntity): Long

    @Query("SELECT * FROM achievement_progress ORDER BY achievement_id")
    fun observeProgress(): Flow<List<AchievementProgressEntity>>

    @Query("SELECT * FROM achievement_progress WHERE achievement_id = :id")
    suspend fun getProgress(id: String): AchievementProgressEntity?

    @Query("SELECT COUNT(*) FROM achievement_unique_values WHERE namespace = :namespace")
    suspend fun evidenceCount(namespace: String): Int

    @Query("SELECT value_key FROM achievement_unique_values WHERE namespace = :namespace ORDER BY value_key")
    suspend fun evidenceValues(namespace: String): List<String>

    @Query(
        """
        UPDATE achievement_progress
        SET current_value = :value,
            is_unlocked = :unlocked,
            unlocked_at = :unlockedAt,
            notification_seen = CASE WHEN :newlyUnlocked THEN 0 ELSE notification_seen END,
            updated_at = :updatedAt
        WHERE achievement_id = :id
        """
    )
    suspend fun updateProgress(
        id: String,
        value: Int,
        unlocked: Boolean,
        unlockedAt: Long?,
        newlyUnlocked: Boolean,
        updatedAt: Long,
    )

    @Query("SELECT * FROM achievement_progress WHERE is_unlocked = 1 AND notification_seen = 0 ORDER BY unlocked_at")
    suspend fun getPendingNotifications(): List<AchievementProgressEntity>

    @Query("UPDATE achievement_progress SET notification_seen = 1 WHERE achievement_id = :id")
    suspend fun markNotificationSeen(id: String)

    @Query("DELETE FROM achievement_unique_values")
    suspend fun clearEvidence()

    @Query("DELETE FROM achievement_progress")
    suspend fun clearProgress()
}

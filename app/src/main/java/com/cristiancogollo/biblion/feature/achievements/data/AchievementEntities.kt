package com.cristiancogollo.biblion.feature.achievements.data

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = "achievement_progress", primaryKeys = ["achievement_id"])
data class AchievementProgressEntity(
    @ColumnInfo(name = "achievement_id") val achievementId: String,
    @ColumnInfo(name = "current_value") val currentValue: Int,
    @ColumnInfo(name = "target_value") val targetValue: Int,
    @ColumnInfo(name = "is_unlocked") val isUnlocked: Boolean,
    @ColumnInfo(name = "unlocked_at") val unlockedAt: Long?,
    @ColumnInfo(name = "notification_seen") val notificationSeen: Boolean,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Entity(tableName = "achievement_unique_values", primaryKeys = ["namespace", "value_key"])
data class AchievementUniqueValueEntity(
    val namespace: String,
    @ColumnInfo(name = "value_key") val valueKey: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

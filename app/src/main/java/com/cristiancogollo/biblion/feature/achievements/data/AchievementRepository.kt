package com.cristiancogollo.biblion.feature.achievements.data

import android.content.Context
import com.cristiancogollo.biblion.feature.achievements.domain.AchievementCatalog
import com.cristiancogollo.biblion.feature.achievements.domain.AchievementProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AchievementRepository(context: Context) {
    private val dao = AchievementDatabase.getInstance(context).achievementDao()

    fun observeProgress(): Flow<List<AchievementProgress>> =
        dao.observeProgress().map { rows ->
            val byId = rows.associateBy(AchievementProgressEntity::achievementId)
            AchievementCatalog.all.map { definition ->
                val row = byId[definition.id]
                AchievementProgress(
                    definition = definition,
                    currentValue = row?.currentValue ?: 0,
                    isUnlocked = row?.isUnlocked == true,
                    unlockedAt = row?.unlockedAt,
                    notificationSeen = row?.notificationSeen == true,
                )
            }
        }

    suspend fun pendingNotifications(): List<AchievementProgress> =
        dao.getPendingNotifications().mapNotNull { row ->
            AchievementCatalog.byId[row.achievementId]?.let { definition ->
                AchievementProgress(
                    definition,
                    row.currentValue,
                    row.isUnlocked,
                    row.unlockedAt,
                    row.notificationSeen,
                )
            }
        }

    suspend fun markNotificationSeen(id: String) = dao.markNotificationSeen(id)
}

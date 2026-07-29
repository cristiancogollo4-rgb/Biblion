package com.cristiancogollo.biblion.feature.achievements.domain

enum class AchievementCategory {
    READING,
    SEARCH,
    TOPICS,
    BIBI,
    STUDY,
    GROWTH,
}

enum class AchievementTier {
    DISCOVERY,
    PROGRESS,
    MASTERY,
    CONSISTENCY,
}

data class AchievementDefinition(
    val id: String,
    val category: AchievementCategory,
    val title: String,
    val description: String,
    val icon: String,
    val target: Int,
    val tier: AchievementTier,
)

data class AchievementProgress(
    val definition: AchievementDefinition,
    val currentValue: Int,
    val isUnlocked: Boolean,
    val unlockedAt: Long?,
    val notificationSeen: Boolean,
) {
    val normalizedProgress: Float
        get() = (currentValue.toFloat() / definition.target.coerceAtLeast(1))
            .coerceIn(0f, 1f)
}

object AchievementIds {
    const val FIRST_LIGHT = "reading_first_light"
    const val BOTH_WORLDS = "reading_both_worlds"
    const val TWENTY_CHAPTERS = "reading_20_chapters"
    const val FOUR_COLORS = "reading_four_colors"
    const val FIRST_SEARCH = "search_first"
    const val OLD_TESTAMENT_SEARCH = "search_old_testament"
    const val NEW_TESTAMENT_SEARCH = "search_new_testament"
    const val TOPIC_CATEGORY = "search_topic_category"
    const val CONNECTED = "topics_reference_opened"
    const val FIRST_TOPIC = "topics_first_expanded"
    const val TEN_TOPICS = "topics_10_expanded"
    const val TWENTY_FIVE_TOPICS = "topics_25_expanded"
    const val BIBI_WHO = "bibi_who"
    const val BIBI_WHERE = "bibi_where"
    const val BIBI_DEFINE = "bibi_define"
    const val BIBI_DIVE_DEEPER = "bibi_dive_deeper"
    const val FIRST_LOCAL_STUDY = "study_first_local_save"
    const val SIX_BLOCK_TYPES = "study_six_block_types"
    const val VERSE_INSERTED = "study_verse_inserted"
    const val VERSION_COMPARED = "study_version_compared"
    const val DICTIONARY_PERSON = "dictionary_person"
    const val DICTIONARY_PLACE = "dictionary_place"
    const val IDENTITY_COMPLETE = "profile_identity_complete"
    const val THREE_DAY_STREAK = "usage_three_day_streak"
}

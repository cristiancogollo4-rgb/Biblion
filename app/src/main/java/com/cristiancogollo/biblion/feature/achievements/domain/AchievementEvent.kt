package com.cristiancogollo.biblion.feature.achievements.domain

sealed interface AchievementEvent {
    data class DailyVerseOpened(val book: String, val chapter: Int) : AchievementEvent
    data class ChapterRead(val book: String, val chapter: Int, val testament: String) : AchievementEvent
    data class HighlightCreated(val verseKey: String, val colorKey: String) : AchievementEvent
    data class SearchCompleted(
        val testament: String,
        val resultCount: Int,
        val isTutorial: Boolean,
    ) : AchievementEvent
    data class TopicCategoryOpened(val category: String) : AchievementEvent
    data class TopicExpanded(val slug: String) : AchievementEvent
    data class TopicReferenceOpened(val topicSlug: String, val referenceKey: String) : AchievementEvent
    data class BibiAnswerCompleted(
        val intent: String,
        val isTutorial: Boolean,
        val wasSuccessful: Boolean,
    ) : AchievementEvent
    data class StudySavedLocally(
        val documentId: String,
        val hasTitle: Boolean,
        val hasContent: Boolean,
        val isExplicitSave: Boolean,
    ) : AchievementEvent
    data class StudyBlockTypesChanged(val documentId: String, val blockTypes: Set<String>) : AchievementEvent
    data class VerseBlockInserted(val documentId: String) : AchievementEvent
    data class VerseVersionCompared(val documentId: String, val versionCount: Int) : AchievementEvent
    data class DictionaryEntryOpened(val entryId: Long, val category: String) : AchievementEvent
    data class ProfileSaved(val hasName: Boolean, val hasLastName: Boolean, val hasAlias: Boolean) : AchievementEvent
    data class MeaningfulUse(val localDate: String? = null) : AchievementEvent
}

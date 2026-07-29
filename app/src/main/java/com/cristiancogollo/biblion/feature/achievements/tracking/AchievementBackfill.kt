package com.cristiancogollo.biblion.feature.achievements.tracking

import android.content.Context
import com.cristiancogollo.biblion.AppPreferencesSyncStore
import com.cristiancogollo.biblion.feature.achievements.domain.AchievementEvent
import com.cristiancogollo.biblion.feature.studydocs.data.StudyDocDatabase
import com.cristiancogollo.biblion.feature.studydocs.data.StudyDocRepository
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import kotlinx.coroutines.flow.first

object AchievementBackfill {
    private const val PREFS = "achievement_backfill"
    private const val KEY_VERSION = "version"
    private const val VERSION = 1

    suspend fun runOnce(context: Context) {
        val preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (preferences.getInt(KEY_VERSION, 0) >= VERSION) return

        AppPreferencesSyncStore.getAllHighlightChapters(context).forEach { chapter ->
            chapter.verses.forEach { (verse, colorIndex) ->
                AchievementTracker.track(
                    context,
                    AchievementEvent.HighlightCreated(
                        verseKey = "${chapter.book}|${chapter.chapter}|$verse",
                        colorKey = "highlight_$colorIndex",
                    ),
                    notify = false,
                    countAsMeaningfulUse = false,
                )
            }
        }

        val repository = StudyDocRepository(StudyDocDatabase.getInstance(context).studyDocDao())
        repository.observeAll().first().forEach { doc ->
            val documentId = doc.remoteId ?: doc.id.value
            AchievementTracker.track(
                context,
                AchievementEvent.StudySavedLocally(
                    documentId = documentId,
                    hasTitle = doc.title.isNotBlank(),
                    hasContent = doc.blocks.any { it.plainText().isNotBlank() },
                    isExplicitSave = true,
                ),
                notify = false,
                countAsMeaningfulUse = false,
            )
            AchievementTracker.track(
                context,
                AchievementEvent.StudyBlockTypesChanged(
                    documentId,
                    doc.blocks.map { it::class.simpleName.orEmpty() }.toSet(),
                ),
                notify = false,
                countAsMeaningfulUse = false,
            )
            if (doc.blocks.any { it is StudyBlock.Verse }) {
                AchievementTracker.track(
                    context,
                    AchievementEvent.VerseBlockInserted(documentId),
                    notify = false,
                    countAsMeaningfulUse = false,
                )
            }
            val versions = doc.blocks.filterIsInstance<StudyBlock.Verse>()
                .maxOfOrNull { it.displayedVersions().size } ?: 0
            if (versions >= 2) {
                AchievementTracker.track(
                    context,
                    AchievementEvent.VerseVersionCompared(documentId, versions),
                    notify = false,
                    countAsMeaningfulUse = false,
                )
            }
        }

        preferences.edit().putInt(KEY_VERSION, VERSION).apply()
    }
}

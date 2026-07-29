package com.cristiancogollo.biblion

import android.content.Context
import androidx.core.content.edit
import org.json.JSONObject
import java.text.Normalizer
import java.util.Locale

data class AppPreferencesSnapshot(
    val darkModeEnabled: Boolean,
    val darkModeUpdatedAt: Long,
    val selectedBibleVersion: String,
    val selectedBibleVersionUpdatedAt: Long,
    val readerFontSizeSp: Int,
    val readerFontSizeUpdatedAt: Long
)

data class HighlightChapterSnapshot(
    val documentId: String,
    val book: String,
    val chapter: Int,
    val verses: Map<String, Int>,
    val updatedAt: Long
)

data class LastReading(
    val book: String,
    val chapter: Int,
    val verse: String?
)

object AppPreferencesSyncStore {
    const val PREFS_NAME = "BiblionAppPrefs"
    const val KEY_DARK_MODE_ENABLED = "darkModeEnabled"
    const val KEY_SELECTED_BIBLE_VERSION = "selectedBibleVersion"
    const val KEY_FONT_SIZE = "fontSize"
    const val KEY_VERSE_HIGHLIGHTS = "verseHighlights"
    const val KEY_HAS_SEEN_FULL_TUTORIAL = "onboarding.hasSeenFullTutorial"
    const val KEY_ACTIVE_GUIDED_TUTORIAL = "onboarding.activeGuidedTutorial"
    const val KEY_ACTIVE_GUIDED_TUTORIAL_STEP = "onboarding.activeGuidedTutorialStep"
    const val KEY_CURRENT_READING_GUIDE_STEP = "onboarding.currentReadingGuideStep"
    const val KEY_HAS_COMPLETED_READING_GUIDE = "onboarding.hasCompletedReadingGuide"
    const val KEY_HAS_COMPLETED_STUDY_GUIDE = "onboarding.hasCompletedStudyGuide"
    const val KEY_HAS_COMPLETED_EXPLORE_GUIDE = "onboarding.hasCompletedExploreGuide"
    const val KEY_LAST_READING_BOOK = "reading.lastBook"
    const val KEY_LAST_READING_CHAPTER = "reading.lastChapter"
    const val KEY_LAST_READING_VERSE = "reading.lastVerse"

    private const val KEY_DARK_MODE_UPDATED_AT = "sync.darkMode.updatedAt"
    private const val KEY_SELECTED_VERSION_UPDATED_AT = "sync.selectedBibleVersion.updatedAt"
    private const val KEY_FONT_SIZE_UPDATED_AT = "sync.fontSize.updatedAt"
    private const val KEY_HIGHLIGHT_CHAPTER_UPDATED_AT = "sync.highlightChapter.updatedAt"
    private const val DEFAULT_BIBLE_VERSION = "rv1960"
    private const val DEFAULT_FONT_SIZE_SP = 18

    fun getDarkModeEnabled(context: Context, defaultValue: Boolean): Boolean {
        return prefs(context).getBoolean(KEY_DARK_MODE_ENABLED, defaultValue)
    }

    fun setDarkModeEnabled(
        context: Context,
        enabled: Boolean,
        updatedAt: Long = System.currentTimeMillis(),
        triggerSync: Boolean = true
    ) {
        prefs(context).edit {
            putBoolean(KEY_DARK_MODE_ENABLED, enabled)
            putLong(KEY_DARK_MODE_UPDATED_AT, updatedAt)
        }
        if (triggerSync) {
            FirestoreSyncManager.requestPreferencesSync()
        }
    }

    fun getSelectedBibleVersion(context: Context): String {
        return prefs(context)
            .getString(KEY_SELECTED_BIBLE_VERSION, DEFAULT_BIBLE_VERSION)
            ?.trim()
            ?.lowercase(Locale.ROOT)
            ?.ifBlank { DEFAULT_BIBLE_VERSION }
            ?: DEFAULT_BIBLE_VERSION
    }

    fun setSelectedBibleVersion(
        context: Context,
        versionKey: String,
        updatedAt: Long = System.currentTimeMillis(),
        triggerSync: Boolean = true
    ) {
        val normalized = versionKey.trim().lowercase(Locale.ROOT)
        if (normalized.isBlank()) return
        prefs(context).edit {
            putString(KEY_SELECTED_BIBLE_VERSION, normalized)
            putLong(KEY_SELECTED_VERSION_UPDATED_AT, updatedAt)
        }
        if (triggerSync) {
            FirestoreSyncManager.requestPreferencesSync()
        }
    }

    fun getReaderFontSizeSp(context: Context): Int {
        return prefs(context).getInt(KEY_FONT_SIZE, DEFAULT_FONT_SIZE_SP)
    }

    fun getLastReading(context: Context): LastReading? {
        val preferences = prefs(context)
        val book = preferences.getString(KEY_LAST_READING_BOOK, null)?.trim().orEmpty()
        if (book.isBlank()) return null
        return LastReading(
            book = book,
            chapter = preferences.getInt(KEY_LAST_READING_CHAPTER, 1).coerceAtLeast(1),
            verse = preferences.getString(KEY_LAST_READING_VERSE, null)?.takeIf { it.isNotBlank() },
        )
    }

    fun setLastReading(context: Context, book: String, chapter: Int, verse: String? = null) {
        prefs(context).edit {
            putString(KEY_LAST_READING_BOOK, book)
            putInt(KEY_LAST_READING_CHAPTER, chapter.coerceAtLeast(1))
            putString(KEY_LAST_READING_VERSE, verse)
        }
    }

    fun setReaderFontSizeSp(
        context: Context,
        fontSizeSp: Int,
        updatedAt: Long = System.currentTimeMillis(),
        triggerSync: Boolean = true
    ) {
        prefs(context).edit {
            putInt(KEY_FONT_SIZE, fontSizeSp)
            putLong(KEY_FONT_SIZE_UPDATED_AT, updatedAt)
        }
        if (triggerSync) {
            FirestoreSyncManager.requestPreferencesSync()
        }
    }

    fun hasSeenFullTutorial(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_HAS_SEEN_FULL_TUTORIAL, false)
    }

    fun setHasSeenFullTutorial(context: Context, seen: Boolean) {
        prefs(context).edit {
            putBoolean(KEY_HAS_SEEN_FULL_TUTORIAL, seen)
        }
    }

    fun getActiveGuidedTutorial(context: Context): GuidedTutorialProgress? {
        val guideId = GuidedTutorialId.fromRouteArg(
            prefs(context).getString(KEY_ACTIVE_GUIDED_TUTORIAL, null)
        ) ?: return null
        val stepIndex = if (guideId == GuidedTutorialId.READING) {
            prefs(context).getInt(
                KEY_CURRENT_READING_GUIDE_STEP,
                prefs(context).getInt(KEY_ACTIVE_GUIDED_TUTORIAL_STEP, 0)
            )
        } else {
            prefs(context).getInt(KEY_ACTIVE_GUIDED_TUTORIAL_STEP, 0)
        }.coerceAtLeast(0)
        return GuidedTutorialProgress(guideId = guideId, stepIndex = stepIndex)
    }

    fun startGuidedTutorial(context: Context, guideId: GuidedTutorialId, stepIndex: Int = 0) {
        prefs(context).edit {
            putString(KEY_ACTIVE_GUIDED_TUTORIAL, guideId.routeArg)
            putInt(KEY_ACTIVE_GUIDED_TUTORIAL_STEP, stepIndex.coerceAtLeast(0))
            if (guideId == GuidedTutorialId.READING) {
                putInt(KEY_CURRENT_READING_GUIDE_STEP, stepIndex.coerceAtLeast(0))
                putBoolean(KEY_HAS_COMPLETED_READING_GUIDE, false)
            }
        }
    }

    fun updateGuidedTutorialStep(context: Context, guideId: GuidedTutorialId, stepIndex: Int) {
        prefs(context).edit {
            putString(KEY_ACTIVE_GUIDED_TUTORIAL, guideId.routeArg)
            putInt(KEY_ACTIVE_GUIDED_TUTORIAL_STEP, stepIndex.coerceAtLeast(0))
            if (guideId == GuidedTutorialId.READING) {
                putInt(KEY_CURRENT_READING_GUIDE_STEP, stepIndex.coerceAtLeast(0))
            }
        }
    }

    fun clearActiveGuidedTutorial(context: Context) {
        prefs(context).edit {
            remove(KEY_ACTIVE_GUIDED_TUTORIAL)
            remove(KEY_ACTIVE_GUIDED_TUTORIAL_STEP)
        }
    }

    fun completeGuidedTutorial(context: Context, guideId: GuidedTutorialId) {
        prefs(context).edit {
            remove(KEY_ACTIVE_GUIDED_TUTORIAL)
            remove(KEY_ACTIVE_GUIDED_TUTORIAL_STEP)
            when (guideId) {
                GuidedTutorialId.READING -> putBoolean(KEY_HAS_COMPLETED_READING_GUIDE, true)
                GuidedTutorialId.STUDY -> putBoolean(KEY_HAS_COMPLETED_STUDY_GUIDE, true)
                GuidedTutorialId.EXPLORE -> {
                    putBoolean(KEY_HAS_COMPLETED_EXPLORE_GUIDE, true)
                    putBoolean(KEY_HAS_SEEN_FULL_TUTORIAL, true)
                }
            }
        }
    }

    fun hasCompletedReadingGuide(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_HAS_COMPLETED_READING_GUIDE, false)
    }

    fun hasCompletedStudyGuide(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_HAS_COMPLETED_STUDY_GUIDE, false)
    }

    fun getAppPreferencesSnapshot(context: Context, defaultDarkMode: Boolean): AppPreferencesSnapshot {
        return AppPreferencesSnapshot(
            darkModeEnabled = getDarkModeEnabled(context, defaultDarkMode),
            darkModeUpdatedAt = prefs(context).getLong(KEY_DARK_MODE_UPDATED_AT, 0L),
            selectedBibleVersion = getSelectedBibleVersion(context),
            selectedBibleVersionUpdatedAt = prefs(context).getLong(KEY_SELECTED_VERSION_UPDATED_AT, 0L),
            readerFontSizeSp = getReaderFontSizeSp(context),
            readerFontSizeUpdatedAt = prefs(context).getLong(KEY_FONT_SIZE_UPDATED_AT, 0L)
        )
    }

    fun getRawHighlights(context: Context): String {
        return prefs(context).getString(KEY_VERSE_HIGHLIGHTS, "{}") ?: "{}"
    }

    fun setRawHighlights(
        context: Context,
        rawHighlights: String,
        triggerSync: Boolean = false
    ) {
        prefs(context).edit {
            putString(KEY_VERSE_HIGHLIGHTS, rawHighlights)
        }
        if (triggerSync) {
            FirestoreSyncManager.requestHighlightsFullSync()
        }
    }

    fun updateHighlightChapter(
        context: Context,
        book: String,
        chapter: Int,
        verses: Map<String, Int>,
        updatedAt: Long = System.currentTimeMillis(),
        triggerSync: Boolean = true
    ) {
        val current = JSONObject(getRawHighlights(context))
        verses.forEach { (verseNumber, colorIndex) ->
            current.put(localVerseKey(book, chapter, verseNumber), colorIndex)
        }
        prefs(context).edit {
            putString(KEY_VERSE_HIGHLIGHTS, current.toString())
            putString(
                KEY_HIGHLIGHT_CHAPTER_UPDATED_AT,
                chapterUpdatedAtJson(context)
                    .put(chapterDocumentId(book, chapter), updatedAt)
                    .toString()
            )
        }
        if (triggerSync) {
            FirestoreSyncManager.requestHighlightsSync(book, chapter, verses)
        }
    }

    fun applyRemoteHighlightChapter(
        context: Context,
        book: String,
        chapter: Int,
        verses: Map<String, Int>,
        remoteUpdatedAt: Long
    ) {
        val currentMeta = chapterUpdatedAtJson(context)
        val docId = chapterDocumentId(book, chapter)
        val localUpdatedAt = currentMeta.optLong(docId, 0L)
        if (remoteUpdatedAt < localUpdatedAt) return

        val raw = JSONObject(getRawHighlights(context))
        verses.forEach { (verseNumber, colorIndex) ->
            raw.put(localVerseKey(book, chapter, verseNumber), colorIndex)
        }
        currentMeta.put(docId, remoteUpdatedAt)
        prefs(context).edit {
            putString(KEY_VERSE_HIGHLIGHTS, raw.toString())
            putString(KEY_HIGHLIGHT_CHAPTER_UPDATED_AT, currentMeta.toString())
        }
    }

    fun getAllHighlightChapters(context: Context): List<HighlightChapterSnapshot> {
        val raw = JSONObject(getRawHighlights(context))
        val grouped = linkedMapOf<Pair<String, Int>, MutableMap<String, Int>>()
        val keys = raw.keys()
        while (keys.hasNext()) {
            val fullKey = keys.next()
            val parts = fullKey.split("|")
            if (parts.size != 3) continue
            val book = parts[0]
            val chapter = parts[1].toIntOrNull() ?: continue
            val verse = parts[2]
            val chapterMap = grouped.getOrPut(book to chapter) { linkedMapOf() }
            chapterMap[verse] = raw.optInt(fullKey, 0)
        }

        val timestamps = chapterUpdatedAtJson(context)
        return grouped.map { (key, verses) ->
            val (book, chapter) = key
            val docId = chapterDocumentId(book, chapter)
            HighlightChapterSnapshot(
                documentId = docId,
                book = book,
                chapter = chapter,
                verses = verses.toMap(),
                updatedAt = timestamps.optLong(docId, 0L)
            )
        }
    }

    fun chapterDocumentId(book: String, chapter: Int): String {
        val normalized = Normalizer.normalize(book.trim().lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
            .replace("[^a-z0-9]+".toRegex(), "_")
            .trim('_')
        return "${normalized.ifBlank { "book" }}__$chapter"
    }

    private fun localVerseKey(book: String, chapter: Int, verseNumber: String): String {
        return "$book|$chapter|$verseNumber"
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun chapterUpdatedAtJson(context: Context): JSONObject {
        val raw = prefs(context).getString(KEY_HIGHLIGHT_CHAPTER_UPDATED_AT, "{}") ?: "{}"
        return runCatching { JSONObject(raw) }.getOrDefault(JSONObject())
    }
}

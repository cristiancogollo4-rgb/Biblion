package com.cristiancogollo.biblion

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.FileNotFoundException
import java.util.Locale

/**
 * Repositorio centralizado para acceso a Biblia local.
 *
 * - Carga y cachea la versión seleccionada en memoria.
 * - Soporta invalidación manual y TTL opcional para escenarios futuros.
 */
object BibleRepository {
    private const val PREFS_NAME = "BiblionAppPrefs"
    private const val KEY_SELECTED_BIBLE_VERSION = "selectedBibleVersion"
    private const val DEFAULT_BIBLE_VERSION = "rvr1960"

    @Volatile
    private var cachedBibleByVersion: MutableMap<String, JSONObject> = mutableMapOf()

    @Volatile
    private var cacheLoadedAtMsByVersion: MutableMap<String, Long> = mutableMapOf()

    @Volatile
    private var cachedTitlesByVersion: MutableMap<String, JSONObject> = mutableMapOf()

    /**
     * Si es null, la cache vive durante todo el proceso.
     * Si se define, invalida cache al superar el tiempo.
     */
    @Volatile
    var cacheTtlMs: Long? = null

    private fun isCacheValid(versionKey: String, now: Long): Boolean {
        val cachedBible = cachedBibleByVersion[versionKey]
        val loadedAt = cacheLoadedAtMsByVersion[versionKey] ?: 0L
        val ttl = cacheTtlMs ?: return cachedBible != null
        return cachedBible != null && (now - loadedAt) <= ttl
    }

    fun clearCache() {
        synchronized(this) {
            cachedBibleByVersion.clear()
            cacheLoadedAtMsByVersion.clear()
            cachedTitlesByVersion.clear()
        }
    }

    fun getSelectedVersionKey(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_SELECTED_BIBLE_VERSION, DEFAULT_BIBLE_VERSION)
            ?.trim()
            ?.lowercase(Locale.ROOT)
            ?.ifBlank { DEFAULT_BIBLE_VERSION }
            ?: DEFAULT_BIBLE_VERSION
    }

    fun setSelectedVersionKey(context: Context, versionKey: String) {
        val normalized = versionKey.trim().lowercase(Locale.ROOT)
        if (normalized.isBlank()) return
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SELECTED_BIBLE_VERSION, normalized)
            .apply()
    }

    fun getAvailableVersions(context: Context): List<BibleVersionOption> {
        val availableAssets = context.assets.list("")?.toSet().orEmpty()
        val known = listOf(
            BibleVersionOption("rvr1960", "Reina Valera 1960"),
            BibleVersionOption("rvr1960s", "Reina Valera 1960S"),
            BibleVersionOption("nvi", "Nueva Versión Internacional (NVI)"),
            BibleVersionOption("dhh", "Dios Habla Hoy (DHH)"),
            BibleVersionOption("pdt", "Palabra de Dios para Todos (PDT)")
        )

        val knownAvailable = known.filter { "${it.key}.json" in availableAssets }

        val dynamic = availableAssets
            .asSequence()
            .filter { it.endsWith(".json") && !it.endsWith("_titles.json") }
            .map { it.removeSuffix(".json") }
            .filterNot { key -> known.any { it.key == key } }
            .map { key -> BibleVersionOption(key, key.uppercase(Locale.ROOT)) }
            .toList()

        return (knownAvailable + dynamic).ifEmpty {
            listOf(BibleVersionOption(DEFAULT_BIBLE_VERSION, "Reina Valera 1960"))
        }
    }

    private fun getBible(context: Context): JSONObject {
        val versionKey = getSelectedVersionKey(context)
        val assetName = "$versionKey.json"
        val now = System.currentTimeMillis()
        if (isCacheValid(versionKey, now)) {
            return cachedBibleByVersion[versionKey]!!
        }

        return synchronized(this) {
            val nowInLock = System.currentTimeMillis()
            if (isCacheValid(versionKey, nowInLock)) {
                return@synchronized cachedBibleByVersion[versionKey]!!
            }

            val jsonString = context.assets.open(assetName).bufferedReader().use { it.readText() }
            JSONObject(jsonString).also {
                cachedBibleByVersion[versionKey] = it
                cacheLoadedAtMsByVersion[versionKey] = nowInLock
            }
        }
    }

    private fun getTitles(context: Context): JSONObject {
        val versionKey = getSelectedVersionKey(context)
        val cached = cachedTitlesByVersion[versionKey]
        if (cached != null) {
            return cached
        }

        return synchronized(this) {
            cachedTitlesByVersion[versionKey]?.let { return@synchronized it }

            val titlesAssetName = "${versionKey}_titles.json"
            val titlesJson = try {
                val jsonString = context.assets.open(titlesAssetName).bufferedReader().use { it.readText() }
                JSONObject(jsonString)
            } catch (_: FileNotFoundException) {
                JSONObject()
            } catch (_: Exception) {
                JSONObject()
            }

            cachedTitlesByVersion[versionKey] = titlesJson
            titlesJson
        }
    }

    suspend fun getRandomVerse(context: Context): DailyVerse = withContext(Dispatchers.IO) {
        val bible = getBible(context)

        val books = bible.keys().asSequence().toList()
        val randomBookName = books.random()
        val book = bible.getJSONObject(randomBookName)

        val chapters = book.keys().asSequence().toList()
        val randomChapterNum = chapters.random()
        val chapter = book.getJSONObject(randomChapterNum)

        val verses = chapter.keys().asSequence().toList()
        val randomVerseNum = verses.random()
        val verseText = chapter.getString(randomVerseNum)

        DailyVerse(
            text = verseText,
            reference = "$randomBookName $randomChapterNum:$randomVerseNum"
        )
    }

    suspend fun getChapter(
        context: Context,
        bookName: String,
        chapterNumber: Int
    ): ChapterContent = withContext(Dispatchers.IO) {
        val bible = getBible(context)
        val titlesJsonRoot = getTitles(context)
        val bookJson = bible.optJSONObject(bookName)
        val bookTitlesJson = titlesJsonRoot.optJSONObject(bookName)

        val chapterCount = bookJson?.length() ?: 0
        val chapterJson = bookJson?.optJSONObject(chapterNumber.toString())
        val chapterTitlesJson = bookTitlesJson?.optJSONObject(chapterNumber.toString())

        val verses = buildList {
            chapterJson?.keys()?.forEach { key ->
                add(key to chapterJson.getString(key))
            }
        }.sortedBy { it.first.toIntOrNull() ?: Int.MAX_VALUE }

        val titlesByVerse = buildMap {
            chapterTitlesJson?.keys()?.forEach { key ->
                val title = chapterTitlesJson.optString(key).trim()
                if (title.isNotBlank()) {
                    put(key, title)
                }
            }
        }

        ChapterContent(chapterCount = chapterCount, verses = verses, titlesByVerse = titlesByVerse)
    }

    suspend fun searchVerses(context: Context, query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        val bible = getBible(context)
        val results = mutableListOf<SearchResult>()

        val books = bible.keys()
        while (books.hasNext()) {
            val bookName = books.next()
            val book = bible.getJSONObject(bookName)

            val chapters = book.keys()
            while (chapters.hasNext()) {
                val chapterNum = chapters.next()
                val chapter = book.getJSONObject(chapterNum)

                val verses = chapter.keys()
                while (verses.hasNext()) {
                    val verseNum = verses.next()
                    val verseText = chapter.getString(verseNum)

                    if (verseText.contains(query, ignoreCase = true)) {
                        results.add(SearchResult("$bookName $chapterNum:$verseNum", verseText))
                    }
                }
            }
        }

        results
    }
}

data class ChapterContent(
    val chapterCount: Int,
    val verses: List<Pair<String, String>>,
    val titlesByVerse: Map<String, String> = emptyMap()
)

data class BibleVersionOption(
    val key: String,
    val label: String
)

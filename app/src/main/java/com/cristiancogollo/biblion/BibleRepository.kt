package com.cristiancogollo.biblion

import android.content.Context
import com.cristiancogollo.biblion.data.repository.cache.BibleLruCache
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
    private const val DEFAULT_BIBLE_VERSION = "rv1960"
    private const val MAX_CACHED_VERSIONS = 3

    private val cachedBibleByVersion = BibleLruCache<String, JSONObject>(MAX_CACHED_VERSIONS)
    private val cacheLoadedAtMsByVersion = BibleLruCache<String, Long>(MAX_CACHED_VERSIONS)
    private val cachedTitlesByVersion = BibleLruCache<String, JSONObject>(MAX_CACHED_VERSIONS)
    private val cachedBookKeyMapByVersion = BibleLruCache<String, Map<String, String>>(MAX_CACHED_VERSIONS)
    private val cachedTitleKeyMapByVersion = BibleLruCache<String, Map<String, String>>(MAX_CACHED_VERSIONS)
    private val cachedSearchIndexByVersion = BibleLruCache<String, List<SearchIndexEntry>>(MAX_CACHED_VERSIONS)

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
        cachedBibleByVersion.clear()
        cacheLoadedAtMsByVersion.clear()
        cachedTitlesByVersion.clear()
        cachedBookKeyMapByVersion.clear()
        cachedTitleKeyMapByVersion.clear()
        cachedSearchIndexByVersion.clear()
    }

    fun clearVersionCache(versionKey: String) {
        val normalized = versionKey.trim().lowercase(Locale.ROOT)
        if (normalized.isBlank()) return
        cachedBibleByVersion.remove(normalized)
        cacheLoadedAtMsByVersion.remove(normalized)
        cachedTitlesByVersion.remove(normalized)
        cachedBookKeyMapByVersion.remove(normalized)
        cachedTitleKeyMapByVersion.remove(normalized)
        cachedSearchIndexByVersion.remove(normalized)
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
            BibleVersionOption("rv1960", "Reina Valera 1960"),
            BibleVersionOption("nvi", "Nueva Versión Internacional (NVI)"),
            BibleVersionOption("dhh", "Dios Habla Hoy (DHH)"),
            //BibleVersionOption("pdt", "Palabra de Dios para Todos (PDT)"),
            BibleVersionOption("tla", "Traducción en Lenguaje Actual (TLA)"),
            BibleVersionOption("ntv", "Nueva Traducción Viviente (NTV)")

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
        return getBibleForVersion(context, getSelectedVersionKey(context))
    }

    private fun getBibleForVersion(context: Context, versionKey: String): JSONObject {
        val assetName = "$versionKey.json"

        return synchronized(this) {
            val nowInLock = System.currentTimeMillis()
            if (isCacheValid(versionKey, nowInLock)) {
                cachedBibleByVersion[versionKey]?.let { return@synchronized it }
            }

            val jsonString = try {
                context.assets.open(assetName).bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                // Fallback a versión por defecto si no se encuentra el asset
                context.assets.open("$DEFAULT_BIBLE_VERSION.json").bufferedReader().use { it.readText() }
            }

            JSONObject(jsonString).also {
                cachedBibleByVersion[versionKey] = it
                cacheLoadedAtMsByVersion[versionKey] = nowInLock
                cachedBookKeyMapByVersion.remove(versionKey)
                cachedSearchIndexByVersion.remove(versionKey)
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
            cachedTitleKeyMapByVersion.remove(versionKey)
            titlesJson
        }
    }

    private fun String.normalizeBookName(): String {
        val accents = mapOf('á' to 'a', 'é' to 'e', 'í' to 'i', 'ó' to 'o', 'ú' to 'u', 'ñ' to 'n')
        return lowercase(Locale.ROOT)
            .replace(" ", "")
            .map { accents[it] ?: it }
            .joinToString("")
    }

    private fun getBibleBookKeyMap(versionKey: String, bible: JSONObject): Map<String, String> {
        cachedBookKeyMapByVersion[versionKey]?.let { return it }
        return synchronized(this) {
            cachedBookKeyMapByVersion[versionKey]?.let { return@synchronized it }
            val built = bible.keys().asSequence().associateBy(
                keySelector = { it.normalizeBookName() },
                valueTransform = { it }
            )
            cachedBookKeyMapByVersion[versionKey] = built
            built
        }
    }

    private fun getTitleBookKeyMap(versionKey: String, titlesJson: JSONObject): Map<String, String> {
        cachedTitleKeyMapByVersion[versionKey]?.let { return it }
        return synchronized(this) {
            cachedTitleKeyMapByVersion[versionKey]?.let { return@synchronized it }
            val built = titlesJson.keys().asSequence().associateBy(
                keySelector = { it.normalizeBookName() },
                valueTransform = { it }
            )
            cachedTitleKeyMapByVersion[versionKey] = built
            built
        }
    }

    private fun getSearchIndex(versionKey: String, bible: JSONObject): List<SearchIndexEntry> {
        cachedSearchIndexByVersion[versionKey]?.let { return it }
        return synchronized(this) {
            cachedSearchIndexByVersion[versionKey]?.let { return@synchronized it }
            val index = buildList {
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
                            add(
                                SearchIndexEntry(
                                    bookName = bookName,
                                    chapter = chapterNum.toIntOrNull() ?: 1,
                                    verse = verseNum,
                                    verseText = verseText
                                )
                            )
                        }
                    }
                }
            }
            cachedSearchIndexByVersion[versionKey] = index
            index
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

    /**
     * Obtiene una referencia aleatoria (Libro, Capítulo, Versículo).
     */
    suspend fun getRandomVerseReference(context: Context): Triple<String, String, String> = withContext(Dispatchers.IO) {
        val bible = getBible(context)
        val books = bible.keys().asSequence().toList()
        val bookName = books.random()
        val book = bible.getJSONObject(bookName)
        val chapters = book.keys().asSequence().toList()
        val chapterNum = chapters.random()
        val chapter = book.getJSONObject(chapterNum)
        val verses = chapter.keys().asSequence().toList()
        val verseNum = verses.random()
        Triple(bookName, chapterNum, verseNum)
    }

    /**
     * Obtiene el texto de un versículo específico para una versión dada.
     */
    suspend fun getVerseText(
        context: Context,
        versionKey: String,
        bookName: String,
        chapter: String,
        verse: String
    ): DailyVerse = withContext(Dispatchers.IO) {
        val bible = getBibleForVersion(context, versionKey)
        val normalizedBook = bookName.normalizeBookName()
        val bibleKey = getBibleBookKeyMap(versionKey, bible)[normalizedBook] ?: bookName
        
        val bookJson = bible.optJSONObject(bibleKey)
        val chapterJson = bookJson?.optJSONObject(chapter)
        val verseText = chapterJson?.optString(verse) ?: ""

        DailyVerse(
            text = verseText,
            reference = "$bibleKey $chapter:$verse"
        )
    }

    suspend fun getChapter(
        context: Context,
        bookName: String,
        chapterNumber: Int
    ): ChapterContent = withContext(Dispatchers.IO) {
        val versionKey = getSelectedVersionKey(context)
        val bible = getBible(context)
        val titlesJsonRoot = getTitles(context)

        val searchNormalized = bookName.normalizeBookName()

        // 1. Buscar la llave correcta en el JSON de la Biblia
        val bibleKey = getBibleBookKeyMap(versionKey, bible)[searchNormalized] ?: bookName
        val bookJson = bible.optJSONObject(bibleKey)

        // 2. Buscar la llave correcta en el JSON de Títulos (independientemente)
        val titlesKey = getTitleBookKeyMap(versionKey, titlesJsonRoot)[searchNormalized] ?: bibleKey
        val bookTitlesJson = titlesJsonRoot.optJSONObject(titlesKey)

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
        val versionKey = getSelectedVersionKey(context)
        val bible = getBibleForVersion(context, versionKey)
        getSearchIndex(versionKey, bible)
            .asSequence()
            .filter { it.verseText.contains(query, ignoreCase = true) }
            .map { entry ->
                SearchResult(
                    reference = "${entry.bookName} ${entry.chapter}:${entry.verse}",
                    text = entry.verseText,
                    bookName = entry.bookName,
                    chapter = entry.chapter,
                    verse = entry.verse
                )
            }
            .toList()
    }
}

private data class SearchIndexEntry(
    val bookName: String,
    val chapter: Int,
    val verse: String,
    val verseText: String
)

data class ChapterContent(
    val chapterCount: Int,
    val verses: List<Pair<String, String>>,
    val titlesByVerse: Map<String, String> = emptyMap()
)

data class BibleVersionOption(
    val key: String,
    val label: String
)

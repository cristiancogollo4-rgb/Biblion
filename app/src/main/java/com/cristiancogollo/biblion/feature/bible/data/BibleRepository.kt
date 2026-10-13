package com.cristiancogollo.biblion

import android.content.Context
import android.util.LruCache
import com.cristiancogollo.biblion.feature.search.ui.SearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Repositorio centralizado para acceso a la Biblia local en SQLite.
 */
object BibleRepository {
    private const val DEFAULT_BIBLE_VERSION = "rv1960"
    private const val FIRST_BOOK_INDEX = 0
    private const val OLD_TESTAMENT_LAST_BOOK_INDEX = 38
    private const val LAST_BOOK_INDEX = 65
    private const val MAX_CACHED_CHAPTERS = 24
    private const val MAX_CACHED_BOOK_COUNTS = 384

    private data class BookCacheKey(
        val versionKey: String,
        val normalizedBookName: String,
    )

    private data class ChapterCacheKey(
        val book: BookCacheKey,
        val chapter: Int,
    )

    private val cacheLock = Any()
    private val chapterLoadMutex = Mutex()
    private val versionsLoadMutex = Mutex()
    private val chapterCache = LruCache<ChapterCacheKey, ChapterContent>(MAX_CACHED_CHAPTERS)
    private val chapterCountCache = LruCache<BookCacheKey, Int>(MAX_CACHED_BOOK_COUNTS)
    private var availableVersionsCache: List<BibleVersionOption>? = null

    /**
     * Conservado por compatibilidad con pruebas/call sites antiguos. La Biblia ya se lee desde SQLite.
     */
    @Volatile
    var cacheTtlMs: Long? = null

    fun clearCache() {
        synchronized(cacheLock) {
            chapterCache.evictAll()
            chapterCountCache.evictAll()
            availableVersionsCache = null
        }
    }

    fun clearVersionCache(versionKey: String) {
        val normalizedVersionKey = normalizeVersionKey(versionKey)
        synchronized(cacheLock) {
            chapterCache.snapshot().keys
                .filter { it.book.versionKey == normalizedVersionKey }
                .forEach(chapterCache::remove)
            chapterCountCache.snapshot().keys
                .filter { it.versionKey == normalizedVersionKey }
                .forEach(chapterCountCache::remove)
            availableVersionsCache = null
        }
    }

    fun getSelectedVersionKey(context: Context): String {
        return AppPreferencesSyncStore.getSelectedBibleVersion(context)
    }

    fun setSelectedVersionKey(context: Context, versionKey: String) {
        AppPreferencesSyncStore.setSelectedBibleVersion(context, versionKey)
    }

    suspend fun getAvailableVersions(context: Context): List<BibleVersionOption> = withContext(Dispatchers.IO) {
        synchronized(cacheLock) { availableVersionsCache }
            ?.let { return@withContext it }

        versionsLoadMutex.withLock {
            synchronized(cacheLock) { availableVersionsCache }
                ?.let { return@withLock it }

            val availableVersionKeys = bibleDao(context)
                .getAvailableVersionKeys()
                .map { normalizeVersionKey(it) }
                .toSet()
            val known = listOf(
                BibleVersionOption("rv1960", "Reina Valera 1960"),
                BibleVersionOption("nvi", "Nueva Versi\u00f3n Internacional (NVI)"),
                BibleVersionOption("dhh", "Dios Habla Hoy (DHH)"),
                //BibleVersionOption("pdt", "Palabra de Dios para Todos (PDT)"),
                BibleVersionOption("tla", "Traducci\u00f3n en Lenguaje Actual (TLA)"),
                BibleVersionOption("ntv", "Nueva Traducci\u00f3n Viviente (NTV)")
            )

            val knownAvailable = known.filter { it.key in availableVersionKeys }
            val dynamic = availableVersionKeys
                .asSequence()
                .filterNot { key -> known.any { it.key == key } }
                .map { key -> BibleVersionOption(key, key.uppercase(Locale.ROOT)) }
                .toList()

            (knownAvailable + dynamic).ifEmpty {
                listOf(BibleVersionOption(DEFAULT_BIBLE_VERSION, "Reina Valera 1960"))
            }.also { versions ->
                synchronized(cacheLock) {
                    availableVersionsCache = versions
                }
            }
        }
    }

    private fun bibleDao(context: Context): BibleDao {
        return BibleDatabase.getInstance(context).bibleDao()
    }

    private fun String.normalizeBookName(): String {
        val accents = mapOf(
            '\u00e1' to 'a',
            '\u00e9' to 'e',
            '\u00ed' to 'i',
            '\u00f3' to 'o',
            '\u00fa' to 'u',
            '\u00f1' to 'n'
        )
        return lowercase(Locale.ROOT)
            .replace(" ", "")
            .map { accents[it] ?: it }
            .joinToString("")
    }

    internal fun normalizedBookNameForVersion(bookName: String, versionKey: String): String {
        val normalizedName = bookName.normalizeBookName()
        val canonicalName = when (normalizedName) {
            "cantardeloscantares" -> "cantares"
            "hechosdelosapostoles" -> "hechos"
            else -> normalizedName
        }
        if (normalizeVersionKey(versionKey) != "ntv") return canonicalName
        return when (canonicalName) {
            "cantares" -> "cantardeloscantares"
            "hechos" -> "hechosdelosapostoles"
            else -> canonicalName
        }
    }

    private fun normalizeVersionKey(versionKey: String): String {
        return versionKey.trim().lowercase(Locale.ROOT).ifBlank { DEFAULT_BIBLE_VERSION }
    }

    suspend fun getRandomVerse(context: Context): DailyVerse = withContext(Dispatchers.IO) {
        val versionKey = normalizeVersionKey(getSelectedVersionKey(context))
        val random = bibleDao(context).getRandomVerse(versionKey)
            ?: return@withContext DailyVerse(text = "", reference = "")

        DailyVerse(
            text = random.text,
            reference = "${random.bookName} ${random.chapter}:${random.verse}"
        )
    }

    /**
     * Obtiene una referencia aleatoria (Libro, Cap??tulo, Vers??culo).
     */
    suspend fun getRandomVerseReference(context: Context): Triple<String, String, String> = withContext(Dispatchers.IO) {
        getRandomVerseReferenceForVersion(context, normalizeVersionKey(getSelectedVersionKey(context)))
    }

    suspend fun getRandomVerseReference(
        context: Context,
        versionKey: String
    ): Triple<String, String, String> = withContext(Dispatchers.IO) {
        getRandomVerseReferenceForVersion(context, normalizeVersionKey(versionKey))
    }

    private suspend fun getRandomVerseReferenceForVersion(
        context: Context,
        versionKey: String
    ): Triple<String, String, String> {
        val random = bibleDao(context).getRandomReference(versionKey)
            ?: return Triple("Juan", "3", "16")
        return Triple(random.bookName, random.chapter.toString(), random.verse.toString())
    }

    /**
     * Obtiene el texto de un vers??culo espec??fico para una versi??n dada.
     */
    suspend fun getVerseText(
        context: Context,
        versionKey: String,
        bookName: String,
        chapter: String,
        verse: String
    ): DailyVerse = withContext(Dispatchers.IO) {
        val normalizedVersionKey = normalizeVersionKey(versionKey)
        val row = bibleDao(context).getVerse(
            versionKey = normalizedVersionKey,
            normalizedBookName = normalizedBookNameForVersion(bookName, normalizedVersionKey),
            chapter = chapter.toIntOrNull() ?: 1,
            verse = verse.toIntOrNull() ?: 1
        )

        DailyVerse(
            text = row?.text.orEmpty(),
            reference = "${row?.bookName ?: bookName} $chapter:$verse"
        )
    }

    suspend fun getChapter(
        context: Context,
        bookName: String,
        chapterNumber: Int
    ): ChapterContent = getChapter(
        context = context,
        bookName = bookName,
        chapterNumber = chapterNumber,
        versionKey = getSelectedVersionKey(context),
    )

    suspend fun getChapter(
        context: Context,
        bookName: String,
        chapterNumber: Int,
        versionKey: String,
    ): ChapterContent = withContext(Dispatchers.IO) {
        val normalizedVersionKey = normalizeVersionKey(versionKey)
        val searchNormalized = normalizedBookNameForVersion(bookName, normalizedVersionKey)
        val bookCacheKey = BookCacheKey(normalizedVersionKey, searchNormalized)
        val chapterCacheKey = ChapterCacheKey(bookCacheKey, chapterNumber)

        synchronized(cacheLock) { chapterCache.get(chapterCacheKey) }
            ?.let { return@withContext it }

        chapterLoadMutex.withLock {
            synchronized(cacheLock) { chapterCache.get(chapterCacheKey) }
                ?.let { return@withLock it }

            val dao = bibleDao(context)
            val cachedChapterCount = synchronized(cacheLock) {
                chapterCountCache.get(bookCacheKey)
            }
            val chapterCount = cachedChapterCount ?: dao
                .getChapterCount(normalizedVersionKey, searchNormalized)
                .also { count ->
                    synchronized(cacheLock) {
                        chapterCountCache.put(bookCacheKey, count)
                    }
                }
            val rows = dao.getChapterContent(
                normalizedVersionKey,
                searchNormalized,
                chapterNumber,
            )
            val verses = ArrayList<Pair<String, String>>(rows.size)
            val titlesByVerse = LinkedHashMap<String, String>()
            rows.forEach { row ->
                val verseNumber = row.verse.toString()
                verses += verseNumber to row.text
                row.title?.takeIf { it.isNotBlank() }?.let { title ->
                    titlesByVerse[verseNumber] = title
                }
            }

            ChapterContent(
                chapterCount = chapterCount,
                verses = verses,
                titlesByVerse = titlesByVerse
            ).also { content ->
                synchronized(cacheLock) {
                    chapterCache.put(chapterCacheKey, content)
                }
            }
        }
    }

    suspend fun searchVerses(context: Context, query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        searchVerses(context, query, BibleSearchFilter())
    }

    suspend fun searchVerses(
        context: Context,
        query: String,
        filter: BibleSearchFilter
    ): List<SearchResult> = withContext(Dispatchers.IO) {
        val versionKey = normalizeVersionKey(getSelectedVersionKey(context))
        val normalizedBook = filter.bookName?.takeIf { it.isNotBlank() }
            ?.let { normalizedBookNameForVersion(it, versionKey) }
        val (minBookIndex, maxBookIndex) = when (filter.testament) {
            BibleSearchTestament.OLD -> FIRST_BOOK_INDEX to OLD_TESTAMENT_LAST_BOOK_INDEX
            BibleSearchTestament.NEW -> (OLD_TESTAMENT_LAST_BOOK_INDEX + 1) to LAST_BOOK_INDEX
            BibleSearchTestament.ALL -> null to null
        }
        bibleDao(context).searchVersesFiltered(
            versionKey = versionKey,
            query = query,
            normalizedBookName = normalizedBook,
            minBookIndex = minBookIndex,
            maxBookIndex = maxBookIndex
        )
            .map { row ->
                SearchResult(
                    reference = "${row.bookName} ${row.chapter}:${row.verse}",
                    text = row.text,
                    bookName = row.bookName,
                    chapter = row.chapter,
                    verse = row.verse.toString()
                )
            }
    }
}

data class BibleSearchFilter(
    val testament: BibleSearchTestament = BibleSearchTestament.ALL,
    val bookName: String? = null
)

enum class BibleSearchTestament {
    ALL,
    OLD,
    NEW
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

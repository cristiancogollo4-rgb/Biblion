package com.cristiancogollo.biblion

import android.content.Context
import com.cristiancogollo.biblion.feature.search.ui.SearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Repositorio centralizado para acceso a la Biblia local en SQLite.
 */
object BibleRepository {
    private const val DEFAULT_BIBLE_VERSION = "rv1960"
    private const val OLD_TESTAMENT_LAST_BOOK_INDEX = 39

    /**
     * Conservado por compatibilidad con pruebas/call sites antiguos. La Biblia ya se lee desde SQLite.
     */
    @Volatile
    var cacheTtlMs: Long? = null

    fun clearCache() = Unit

    fun clearVersionCache(versionKey: String) = Unit

    fun getSelectedVersionKey(context: Context): String {
        return AppPreferencesSyncStore.getSelectedBibleVersion(context)
    }

    fun setSelectedVersionKey(context: Context, versionKey: String) {
        AppPreferencesSyncStore.setSelectedBibleVersion(context, versionKey)
    }

    suspend fun getAvailableVersions(context: Context): List<BibleVersionOption> = withContext(Dispatchers.IO) {
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
            normalizedBookName = bookName.normalizeBookName(),
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
        val dao = bibleDao(context)
        val searchNormalized = bookName.normalizeBookName()
        val chapterCount = dao.getChapterCount(normalizedVersionKey, searchNormalized)
        val verses = dao.getChapterVerses(normalizedVersionKey, searchNormalized, chapterNumber)
            .map { it.verse.toString() to it.text }
        val titlesByVerse = dao.getChapterTitles(normalizedVersionKey, searchNormalized, chapterNumber)
            .associate { it.verse.toString() to it.title }

        ChapterContent(
            chapterCount = chapterCount,
            verses = verses,
            titlesByVerse = titlesByVerse
        )
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
        val normalizedBook = filter.bookName?.takeIf { it.isNotBlank() }?.normalizeBookName()
        val (minBookIndex, maxBookIndex) = when (filter.testament) {
            BibleSearchTestament.OLD -> 1 to OLD_TESTAMENT_LAST_BOOK_INDEX
            BibleSearchTestament.NEW -> (OLD_TESTAMENT_LAST_BOOK_INDEX + 1) to 66
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

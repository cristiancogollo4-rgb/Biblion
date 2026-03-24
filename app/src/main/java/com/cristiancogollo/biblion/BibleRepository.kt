package com.cristiancogollo.biblion

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Repositorio centralizado para acceso a Biblia local.
 *
 * - Carga y cachea `rvr1960.json` en memoria.
 * - Soporta invalidación manual y TTL opcional para escenarios futuros.
 */
object BibleRepository {
    private const val BIBLE_ASSET = "rvr1960.json"

    @Volatile
    private var cachedBible: JSONObject? = null

    @Volatile
    private var cacheLoadedAtMs: Long = 0L

    /**
     * Si es null, la cache vive durante todo el proceso.
     * Si se define, invalida cache al superar el tiempo.
     */
    @Volatile
    var cacheTtlMs: Long? = null

    private fun isCacheValid(now: Long): Boolean {
        val ttl = cacheTtlMs ?: return cachedBible != null
        return cachedBible != null && (now - cacheLoadedAtMs) <= ttl
    }

    fun clearCache() {
        synchronized(this) {
            cachedBible = null
            cacheLoadedAtMs = 0L
        }
    }

    private fun getBible(context: Context): JSONObject {
        val now = System.currentTimeMillis()
        if (isCacheValid(now)) {
            return cachedBible!!
        }

        return synchronized(this) {
            val nowInLock = System.currentTimeMillis()
            if (isCacheValid(nowInLock)) {
                return@synchronized cachedBible!!
            }

            val jsonString = context.assets.open(BIBLE_ASSET).bufferedReader().use { it.readText() }
            JSONObject(jsonString).also {
                cachedBible = it
                cacheLoadedAtMs = nowInLock
            }
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
        val bookJson = bible.optJSONObject(bookName)

        val chapterCount = bookJson?.length() ?: 0
        val chapterJson = bookJson?.optJSONObject(chapterNumber.toString())

        val verses = buildList {
            chapterJson?.keys()?.forEach { key ->
                add(key to chapterJson.getString(key))
            }
        }.sortedBy { it.first.toIntOrNull() ?: Int.MAX_VALUE }

        ChapterContent(chapterCount = chapterCount, verses = verses)
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
    val verses: List<Pair<String, String>>
)

package com.cristiancogollo.biblion

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Repositorio centralizado para acceso a Biblia local.
 *
 * - Carga y cachea `rvr1960.json` una sola vez por proceso.
 * - Expone helpers para operaciones frecuentes (capítulo, búsqueda, versículo diario).
 */
object BibleRepository {
    private const val BIBLE_ASSET = "rvr1960.json"

    @Volatile
    private var cachedBible: JSONObject? = null

    private fun getBible(context: Context): JSONObject {
        cachedBible?.let { return it }

        return synchronized(this) {
            cachedBible ?: run {
                val jsonString = context.assets.open(BIBLE_ASSET).bufferedReader().use { it.readText() }
                JSONObject(jsonString).also { cachedBible = it }
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

package com.cristiancogollo.biblion.feature.reader.ui

import com.cristiancogollo.biblion.ChapterContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ParallelListSlotsTest {

    private fun chapter(count: Int, verses: Int, titles: Map<String, String> = emptyMap()) =
        ChapterContent(
            chapterCount = count,
            verses = (1..verses).map { it.toString() to "Texto $it" },
            titlesByVerse = titles,
        )

    private fun slots(
        current: Int = 3,
        previous: ChapterContent? = null,
        next: ChapterContent? = null,
        continuous: Boolean = true,
        flowing: Boolean = false,
        currentContent: ChapterContent = chapter(10, 5),
    ) = buildParallelListSlots(
        currentChapter = current,
        current = currentContent,
        previous = previous,
        next = next,
        continuousScrolling = continuous,
        flowingLayout = flowing,
        nextLoadFailed = false,
    )

    @Test
    fun chapterModeHasVersesAndEndsWithAPause() {
        val result = slots(previous = chapter(10, 4), next = chapter(10, 6), continuous = false)
        assertEquals(6, result.size)
        assertEquals("chapter-3-verse-1", result.first().key)
        assertEquals(2, result.indexOfVerse(chapter = 3, verse = 3))
        assertTrue(result.last() is ParallelListSlot.ChapterEnd)
    }

    @Test
    fun continuousModeShowsEveryChapterStartAndANextLoader() {
        val result = slots(previous = chapter(10, 4))
        // header(2) + 4 versos + header(3) + 5 versos + cargador del 4
        assertEquals(12, result.size)
        assertEquals("chapter-2-header", result.first().key)
        assertEquals("chapter-3-header", result[5].key)
        assertEquals(6, result.indexOfVerse(chapter = 3, verse = 1))
        assertEquals("chapter-4-loading", result.last().key)
    }

    @Test
    fun advancingAChapterKeepsEveryVisibleKeyStable() {
        val before = slots(previous = chapter(10, 4), next = chapter(10, 6)).map { it.key }
        val after = slots(current = 4, currentContent = chapter(10, 6), previous = chapter(10, 5))
            .map { it.key }
        val shared = before.filter { it.startsWith("chapter-3-") || it.startsWith("chapter-4-") }
        assertTrue(after.containsAll(shared))
    }

    @Test
    fun lastChapterEndsTheBook() {
        val result = slots(current = 10, currentContent = chapter(10, 3))
        assertTrue(result.last() is ParallelListSlot.ChapterEnd)
    }

    @Test
    fun flowingLayoutFindsSectionContainingVerse() {
        val result = slots(
            current = 1,
            currentContent = chapter(1, 6, titles = mapOf("1" to "Inicio", "4" to "Segunda parte")),
            continuous = false,
            flowing = true,
        )
        assertEquals(3, result.size)
        assertEquals(0, result.indexOfVerse(chapter = 1, verse = 3))
        assertEquals(1, result.indexOfVerse(chapter = 1, verse = 5))
        assertEquals(-1, result.indexOfVerse(chapter = 2, verse = 1))
    }
}

package com.cristiancogollo.biblion.feature.bibi.domain

import com.cristiancogollo.biblion.ChapterContent
import com.cristiancogollo.biblion.feature.bibi.engine.VerseResolver
import com.cristiancogollo.biblion.feature.bibi.model.BibiContext
import com.cristiancogollo.biblion.feature.bibi.model.BibiPassage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BibiReferenceContextResolverTest {
    private val openReaderContext = BibiContext.Reader(
        book = "Genesis",
        chapter = 1,
        passages = listOf(BibiPassage("Genesis", 1, 1, "En el principio")),
        bibleVersion = "rv1960",
    )

    @Test
    fun `explicit reference replaces currently open reader passage`() {
        val resolution = explicitResolution("Juan", 3, 16)
        val chapter = ChapterContent(
            chapterCount = 21,
            verses = listOf("16" to "Porque de tal manera amó Dios al mundo"),
        )

        val result = BibiReferenceContextResolver.resolveLoadedReference(
            openReaderContext,
            resolution,
            chapter,
        )

        val reader = result.context as BibiContext.Reader
        assertEquals("Juan", reader.book)
        assertEquals(3, reader.chapter)
        assertEquals("Juan 3:16", reader.passages.single().reference)
        assertTrue(result.error == null)
    }

    @Test
    fun `range loads every requested verse in canonical order`() {
        val resolution = explicitResolution("1 Corintios", 13, 4, 7)
        val chapter = ChapterContent(
            chapterCount = 16,
            verses = (1..13).map { it.toString() to "Texto $it" },
        )

        val result = BibiReferenceContextResolver.resolveLoadedReference(
            openReaderContext,
            resolution,
            chapter,
        )

        assertEquals(listOf(4, 5, 6, 7), result.explicitPassages.map { it.verse })
    }

    @Test
    fun `missing verse reports error and does not reuse open passage`() {
        val resolution = explicitResolution("Juan", 3, 99)
        val chapter = ChapterContent(
            chapterCount = 21,
            verses = listOf("16" to "Texto"),
        )

        val result = BibiReferenceContextResolver.resolveLoadedReference(
            openReaderContext,
            resolution,
            chapter,
        )

        assertNotNull(result.error)
        assertTrue(result.explicitPassages.isEmpty())
        assertEquals(openReaderContext, result.context)
    }

    private fun explicitResolution(
        book: String,
        chapter: Int,
        start: Int,
        end: Int = start,
    ) = VerseResolver.Resolution(
        book = book,
        chapter = chapter,
        verse = start,
        verseEnd = end,
        source = VerseResolver.Source.EXPLICIT_IN_QUESTION,
        confidence = VerseResolver.Confidence.HIGH,
    )
}

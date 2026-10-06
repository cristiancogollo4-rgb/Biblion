package com.cristiancogollo.biblion.feature.reader

import org.junit.Assert.assertEquals
import org.junit.Test

class BibleTextSectionsTest {

    private val verses = listOf(
        "1" to "Primer versículo",
        "2" to "Segundo versículo",
        "3" to "Tercer versículo",
        "4" to "Cuarto versículo",
    )

    @Test
    fun headingsStartNewFlowingSectionsWithoutLosingVerses() {
        val sections = buildBibleTextSections(
            verses = verses,
            titlesByVerse = mapOf("1" to "Inicio", "3" to "Segunda parte"),
        )

        assertEquals(2, sections.size)
        assertEquals("Inicio", sections[0].heading)
        assertEquals(listOf("1", "2"), sections[0].verses.map { it.first })
        assertEquals("Segunda parte", sections[1].heading)
        assertEquals(listOf("3", "4"), sections[1].verses.map { it.first })
    }

    @Test
    fun flowingIndexesMapEveryVerseToItsSection() {
        val titles = mapOf("1" to "Inicio", "3" to "Segunda parte")

        assertEquals(2, chapterBodyItemCount(verses, titles, ReaderTextLayout.FLOWING))
        assertEquals(0, chapterBodyIndexForVerse("2", verses, titles, ReaderTextLayout.FLOWING))
        assertEquals(1, chapterBodyIndexForVerse("4", verses, titles, ReaderTextLayout.FLOWING))
        assertEquals(
            "3",
            firstVerseAtBodyIndex(1, verses, titles, ReaderTextLayout.FLOWING)?.first,
        )
    }

    @Test
    fun separatedLayoutKeepsOneLazyItemPerVerse() {
        assertEquals(
            verses.size,
            chapterBodyItemCount(verses, emptyMap(), ReaderTextLayout.VERSE_BLOCKS),
        )
        assertEquals(
            2,
            chapterBodyIndexForVerse("3", verses, emptyMap(), ReaderTextLayout.VERSE_BLOCKS),
        )
    }
}

package com.cristiancogollo.biblion.feature.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ContinuousReaderPositionTest {

    @Test
    fun chapterIsReadFromStableHeaderAndVerseKeys() {
        assertEquals(2, chapterForReaderItemKey("chapter-2-header"))
        assertEquals(2, chapterForReaderItemKey("chapter-2-verse-1"))
        assertEquals(3, chapterForReaderItemKey("chapter-3-section-12"))
        assertNull(chapterForReaderItemKey("chapter-3-loading"))
        assertNull(chapterForReaderItemKey("chapter-wrong-verse-1"))
        assertNull(chapterForReaderItemKey(null))
    }

    @Test
    fun visibleItemChangesTheChapterInBothDirections() {
        assertEquals(1, continuousChapterDirection("chapter-2-header", 1, false, true))
        assertEquals(1, continuousChapterDirection("chapter-3-verse-1", 2, true, true))
        assertEquals(-1, continuousChapterDirection("chapter-2-verse-8", 3, true, true))
        assertEquals(-1, continuousChapterDirection("chapter-1-verse-9", 2, true, true))
        assertNull(continuousChapterDirection("chapter-2-verse-1", 2, true, true))
        assertNull(continuousChapterDirection("chapter-3-loading", 2, true, true))
        assertNull(continuousChapterDirection("chapter-3-header", 2, true, false))
    }

    @Test
    fun windowCanAdvanceTwiceAndReturnTwiceWithoutLosingTheAdjacentChapter() {
        val first = ContinuousChapterWindow(
            chapter = 1,
            current = "Numbers 1",
            previous = null,
            next = "Numbers 2",
        )
        val second = first.shift(1)!!
        assertEquals(2, second.chapter)
        assertEquals("Numbers 1", second.previous)
        assertNull(second.next)

        val secondWithPrefetch = second.copy(next = "Numbers 3")
        val third = secondWithPrefetch.shift(1)!!
        assertEquals(3, third.chapter)
        assertEquals("Numbers 2", third.previous)

        val backToSecond = third.shift(-1)!!
        assertEquals(2, backToSecond.chapter)
        assertEquals("Numbers 3", backToSecond.next)

        val backToFirst = backToSecond.copy(previous = "Numbers 1").shift(-1)!!
        assertEquals(1, backToFirst.chapter)
        assertEquals("Numbers 2", backToFirst.next)
        assertNull(backToFirst.previous)
    }
}

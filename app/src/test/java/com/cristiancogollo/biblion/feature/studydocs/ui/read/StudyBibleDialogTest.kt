package com.cristiancogollo.biblion.feature.studydocs.ui.read

import com.cristiancogollo.biblion.feature.studydocs.model.BlockId
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StudyBibleDialogTest {

    @Test
    fun citation_becomes_a_bible_target_with_its_exact_verses_and_version() {
        val verse = StudyBlock.Verse(
            id = BlockId("verse-target"),
            bookId = "Genesis",
            chapter = 1,
            verseStart = 1,
            verseEnd = 5,
            verseNumbers = listOf(1, 4, 5),
            sourceVersion = "nvi",
            contents = mapOf("nvi" to "Texto"),
        )

        val target = verse.toStudyBibleTarget()

        assertEquals("Genesis", target.bookName)
        assertEquals(1, target.chapter)
        assertEquals(setOf(1, 4, 5), target.verseNumbers)
        assertEquals("nvi", target.preferredVersion)
    }

    @Test
    fun target_highlight_only_matches_its_original_book_and_chapter() {
        val target = StudyBibleTarget(
            bookName = "Juan",
            chapter = 3,
            verseNumbers = setOf(16),
        )

        assertTrue(target.matches("juan", 3))
        assertFalse(target.matches("Juan", 4))
        assertFalse(target.matches("Romanos", 3))
    }

    @Test
    fun free_bible_navigation_exposes_all_canonical_books() {
        assertEquals(66, studyBibleBooks.size)
        assertEquals("Genesis", studyBibleBooks.first())
        assertEquals("Apocalipsis", studyBibleBooks.last())
    }
}

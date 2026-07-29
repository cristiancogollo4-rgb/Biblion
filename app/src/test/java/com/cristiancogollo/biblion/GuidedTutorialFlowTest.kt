package com.cristiancogollo.biblion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GuidedTutorialFlowTest {

    @Test
    fun `reading guide preserves the complete feature sequence`() {
        val steps = guidedTutorialSteps(GuidedTutorialId.READING)

        assertEquals(
            listOf(
                "reading-welcome",
                "reading-testament",
                "book-description",
                "reading-book",
                "reading-chapter",
                "reader-text",
                "reader-highlight",
                "reader-version",
                "reader-bibi-chat",
                "reader-bibi",
                "reader-deeper-path",
                "reader-dictionary",
                "search-components",
                "reading-finish"
            ),
            steps.map { it.id }
        )
    }

    @Test
    fun `reading guide targets the redesigned navigation and reader controls`() {
        val targetsByStep = guidedTutorialSteps(GuidedTutorialId.READING)
            .associate { it.id to it.targetKey }

        assertEquals(GuidedTutorialTargets.NAV_BIBLE, targetsByStep["reading-testament"])
        assertEquals(GuidedTutorialTargets.BOOKS_LONG_PRESS, targetsByStep["book-description"])
        assertEquals(GuidedTutorialTargets.BOOKS_FIRST_BOOK, targetsByStep["reading-book"])
        assertEquals(
            GuidedTutorialTargets.READER_CHAPTER_SELECTOR,
            targetsByStep["reading-chapter"]
        )
        assertEquals(GuidedTutorialTargets.READER_FIRST_VERSE, targetsByStep["reader-highlight"])
        assertEquals(GuidedTutorialTargets.READER_VERSION_SELECTOR, targetsByStep["reader-version"])
        assertEquals(GuidedTutorialTargets.READER_BIBI_BUTTON, targetsByStep["reader-bibi-chat"])
        assertEquals(GuidedTutorialTargets.NAV_SEARCH, targetsByStep["reader-dictionary"])
    }

    @Test
    fun `skip is hidden on first install and visible only after restart`() {
        assertFalse(shouldShowGuidedTutorialSecondaryAction(isRestart = false))
        assertTrue(shouldShowGuidedTutorialSecondaryAction(isRestart = true))
    }

    @Test
    fun `floating navigation targets stay in the lower capsule`() {
        val bibleTarget = guidedNavigationTargetRect(
            targetKey = GuidedTutorialTargets.NAV_BIBLE,
            screenWidthPx = 1080f,
            screenHeightPx = 1920f,
            density = 3f
        )

        assertTrue(bibleTarget != null)
        assertTrue(bibleTarget!!.top > 1500f)
        assertTrue(bibleTarget.bottom <= 1920f)
        assertTrue(bibleTarget.left < bibleTarget.right)
    }
}

package com.cristiancogollo.biblion

import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
        assertEquals(GuidedTutorialTargets.BOOKS_FIRST_BOOK, targetsByStep["reading-book"])
        assertEquals(
            GuidedTutorialTargets.READER_CHAPTER_SELECTOR,
            targetsByStep["reading-chapter"]
        )
        assertEquals(GuidedTutorialTargets.READER_FIRST_VERSE, targetsByStep["reader-highlight"])
        assertEquals(GuidedTutorialTargets.READER_VERSION_SELECTOR, targetsByStep["reader-version"])
        assertEquals(GuidedTutorialTargets.READER_BIBI_BUTTON, targetsByStep["reader-bibi-chat"])
        assertEquals(GuidedTutorialTargets.READER_BIBI_CHAT_PANEL, targetsByStep["reader-bibi"])
        assertEquals(GuidedTutorialTargets.NAV_SEARCH, targetsByStep["reader-dictionary"])
    }

    @Test
    fun `bibi button step comes before the chat step`() {
        val steps = guidedTutorialSteps(GuidedTutorialId.READING)
        val button = steps.first { it.id == "reader-bibi-chat" }
        val chat = steps.first { it.id == "reader-bibi" }

        assertEquals(R.string.guide_reader_bibi_body, button.descriptionRes)
        assertEquals(R.string.guide_reader_bibi_chat_body, chat.descriptionRes)
        assertEquals("chat_exchange_completed", chat.advanceOnEvent)
    }

    @Test
    fun `chat steps render inside the bibi window with a chat hint`() {
        val steps = guidedTutorialSteps(GuidedTutorialId.READING).associateBy { it.id }

        assertEquals(
            listOf("reader-bibi", "reader-deeper-path"),
            steps.values.filter(::guidedStepRendersInsideBibiChat).map { it.id }
        )
        assertEquals(R.string.guide_action_chat, guidedTutorialActionHintRes(steps.getValue("reader-bibi")))
        assertEquals(
            R.string.guide_action_required,
            guidedTutorialActionHintRes(steps.getValue("reader-bibi-chat"))
        )
    }

    @Test
    fun `every step offers a way out of the tour`() {
        GuidedTutorialId.entries.flatMap(::guidedTutorialSteps).forEach { step ->
            val expected = if (step.restartsGuide) R.string.guide_restart_tour else R.string.guide_skip_tour
            assertEquals(step.id, expected, step.secondaryLabelRes)
        }
    }

    @Test
    fun `steps that wait for the user always get a fallback`() {
        GuidedTutorialId.entries.flatMap(::guidedTutorialSteps).forEach { step ->
            val withTarget = guidedTutorialFallbackDelayMs(step, targetFound = true)
            val withoutTarget = guidedTutorialFallbackDelayMs(step, targetFound = false)
            if (step.waitsForUser) {
                assertEquals(step.id, GUIDE_FALLBACK_DELAY_MS, withTarget)
                val expectedMissing = if (step.targetKey != null) {
                    GUIDE_MISSING_TARGET_DELAY_MS
                } else {
                    GUIDE_FALLBACK_DELAY_MS
                }
                assertEquals(step.id, expectedMissing, withoutTarget)
            } else {
                assertNull(step.id, withTarget)
                assertNull(step.id, withoutTarget)
            }
        }
    }

    @Test
    fun `step position counts within its own guide`() {
        val total = guidedTutorialSteps(GuidedTutorialId.READING).size

        assertEquals(1 to total, guidedTutorialStepPosition("reading-welcome"))
        assertEquals(total to total, guidedTutorialStepPosition("reading-finish"))
        assertEquals(1 to 1, guidedTutorialStepPosition("study-entry"))
        assertNull(guidedTutorialStepPosition("book-description"))
    }

    @Test
    fun `routes map to the tutorial screen they host`() {
        assertEquals(GuidedTutorialScreenTarget.HOME, guidedTutorialScreenForRoute(Screen.Home.route))
        assertEquals(GuidedTutorialScreenTarget.BOOKS, guidedTutorialScreenForRoute(Screen.Books.route))
        assertEquals(
            GuidedTutorialScreenTarget.READER,
            guidedTutorialScreenForRoute(Screen.ReaderWithBook.route)
        )
        assertEquals(GuidedTutorialScreenTarget.SEARCH, guidedTutorialScreenForRoute(Screen.Search.route))
        assertNull(guidedTutorialScreenForRoute(Screen.Profile.route))
        assertNull(guidedTutorialScreenForRoute(null))
    }

    @Test
    fun `bubble moves to the top for low or tall targets`() {
        val screenHeight = 2000f

        assertFalse(shouldPlaceGuideBubbleAtTop(null, screenHeight))
        assertFalse(shouldPlaceGuideBubbleAtTop(Rect(0f, 100f, 500f, 250f), screenHeight))
        assertTrue(shouldPlaceGuideBubbleAtTop(Rect(900f, 1600f, 1000f, 1700f), screenHeight))
        assertTrue(shouldPlaceGuideBubbleAtTop(Rect(0f, 80f, 1080f, 1900f), screenHeight))
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

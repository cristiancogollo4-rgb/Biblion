package com.cristiancogollo.biblion.feature.bibi

import com.cristiancogollo.biblion.feature.bibi.engine.toRelatedVerse
import com.cristiancogollo.biblion.feature.bibi.model.ResolvedVerse
import com.cristiancogollo.biblion.feature.bibi.model.RelatedVerse
import com.cristiancogollo.biblion.feature.bibi.engine.CrossReferenceVoteEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Tests para BiblicalCrossReference y ResolvedVerse.
 * Solo testea parsing de referencias, no resolución (requiere DB).
 */
class BiblicalCrossReferenceTest {

    // ── ResolvedVerse ──────────────────────────────────────────

    @Test
    fun `ResolvedVerse has all required fields`() {
        val verse = ResolvedVerse(
            reference = "Genesis 1:1",
            book = "Genesis",
            chapter = 1,
            verseStart = 1,
            verseEnd = 1,
            text = "En el principio creó Dios los cielos y la tierra."
        )
        assertEquals("Genesis 1:1", verse.reference)
        assertEquals("Genesis", verse.book)
        assertEquals(1, verse.chapter)
        assertEquals(1, verse.verseStart)
        assertEquals(1, verse.verseEnd)
        assertEquals("En el principio creó Dios los cielos y la tierra.", verse.text)
    }

    @Test
    fun `ResolvedVerse supports range`() {
        val verse = ResolvedVerse(
            reference = "Juan 1:1-3",
            book = "Juan",
            chapter = 1,
            verseStart = 1,
            verseEnd = 3,
            text = "1 En el principio... 2 Este era... 3 Todas las cosas..."
        )
        assertEquals(1, verse.verseStart)
        assertEquals(3, verse.verseEnd)
    }

    @Test
    fun `toRelatedVerse converts correctly`() {
        val resolved = ResolvedVerse(
            reference = "Genesis 1:1",
            book = "Genesis",
            chapter = 1,
            verseStart = 1,
            verseEnd = 1,
            text = "En el principio..."
        )
        val related = resolved.toRelatedVerse()
        assertEquals("Genesis 1:1", related.reference)
        assertEquals("Genesis", related.book)
        assertEquals(1, related.chapter)
        assertEquals(1, related.verseStart)
        assertEquals(1, related.verseEnd)
        assertEquals("En el principio...", related.text)
    }

    @Test
    fun `toRelatedVerse does not expose votes`() {
        val resolved = ResolvedVerse(
            reference = "Genesis 1:1",
            book = "Genesis",
            chapter = 1,
            verseStart = 1,
            verseEnd = 1,
            text = "En el principio..."
        )
        val related = resolved.toRelatedVerse()
        val fields = related::class.java.declaredFields.map { it.name }
        // RelatedVerse should NOT have votes/score/qualityScore fields
        assertFalse("RelatedVerse must not have 'votes'", fields.contains("votes"))
        assertFalse("RelatedVerse must not have 'score'", fields.contains("score"))
        assertFalse("RelatedVerse must not have 'qualityScore'", fields.contains("qualityScore"))
    }

    // ── RelatedVerse ───────────────────────────────────────────

    @Test
    fun `RelatedVerse has correct structure`() {
        val verse = RelatedVerse(
            reference = "Juan 1:1",
            book = "Juan",
            chapter = 1,
            verseStart = 1,
            verseEnd = 1,
            text = "En el principio era el Verbo..."
        )
        assertEquals("Juan 1:1", verse.reference)
        assertEquals("Juan", verse.book)
        assertEquals(1, verse.chapter)
        assertEquals(1, verse.verseStart)
        assertEquals(1, verse.verseEnd)
        assertEquals("En el principio era el Verbo...", verse.text)
    }

    @Test
    fun `RelatedVerse supports range`() {
        val verse = RelatedVerse(
            reference = "Genesis 1:1-3",
            book = "Genesis",
            chapter = 1,
            verseStart = 1,
            verseEnd = 3,
            text = "1 En el principio... 2 Y la tierra... 3 Y dijo Dios..."
        )
        assertEquals(1, verse.verseStart)
        assertEquals(3, verse.verseEnd)
    }

    // ── CrossReferenceVoteEngine ───────────────────────────────

    @Test
    fun `DEFAULT_MIN_VOTES is 10`() {
        assertEquals(10, CrossReferenceVoteEngine.DEFAULT_MIN_VOTES)
    }
}

package com.cristiancogollo.biblion.feature.bibi

import com.cristiancogollo.biblion.feature.bibi.engine.DictionaryEngine
import com.cristiancogollo.biblion.feature.bibi.model.BibiSuggestion
import com.cristiancogollo.biblion.feature.bibi.model.ChatExchange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DictionaryEngineTest {

    // ── formatYear ─────────────────────────────────────────────

    @Test
    fun `formatYear negative year shows BC`() {
        assertEquals("1997 a.C.", DictionaryEngine.formatYear("-1997"))
    }

    @Test
    fun `formatYear zero shows 1 BC`() {
        assertEquals("1 a.C.", DictionaryEngine.formatYear("0"))
    }

    @Test
    fun `formatYear positive year shows AD`() {
        assertEquals("1997 d.C.", DictionaryEngine.formatYear("1997"))
    }

    @Test
    fun `formatYear non-numeric returns raw string`() {
        assertEquals("abc", DictionaryEngine.formatYear("abc"))
    }

    @Test
    fun `formatYear empty string returns empty`() {
        assertEquals("", DictionaryEngine.formatYear(""))
    }

    @Test
    fun `formatYear year 1 shows 1 AD`() {
        assertEquals("1 d.C.", DictionaryEngine.formatYear("1"))
    }

    @Test
    fun `formatYear year -1 shows 1 BC`() {
        assertEquals("1 a.C.", DictionaryEngine.formatYear("-1"))
    }

    // ── translateFeatureType ───────────────────────────────────

    @Test
    fun `translateFeatureType city`() {
        assertEquals("Ciudad", DictionaryEngine.translateFeatureType("city"))
    }

    @Test
    fun `translateFeatureType region`() {
        assertEquals("Región", DictionaryEngine.translateFeatureType("region"))
    }

    @Test
    fun `translateFeatureType country`() {
        assertEquals("País", DictionaryEngine.translateFeatureType("country"))
    }

    @Test
    fun `translateFeatureType mountain`() {
        assertEquals("Monte", DictionaryEngine.translateFeatureType("mountain"))
    }

    @Test
    fun `translateFeatureType river`() {
        assertEquals("Río", DictionaryEngine.translateFeatureType("river"))
    }

    @Test
    fun `translateFeatureType water`() {
        assertEquals("Agua/Mar", DictionaryEngine.translateFeatureType("water"))
    }

    @Test
    fun `translateFeatureType valley`() {
        assertEquals("Valle", DictionaryEngine.translateFeatureType("valley"))
    }

    @Test
    fun `translateFeatureType desert`() {
        assertEquals("Desierto", DictionaryEngine.translateFeatureType("desert"))
    }

    @Test
    fun `translateFeatureType island`() {
        assertEquals("Isla", DictionaryEngine.translateFeatureType("island"))
    }

    @Test
    fun `translateFeatureType plain`() {
        assertEquals("Llanura", DictionaryEngine.translateFeatureType("plain"))
    }

    @Test
    fun `translateFeatureType unknown returns raw`() {
        assertEquals("forest", DictionaryEngine.translateFeatureType("forest"))
    }

    @Test
    fun `translateFeatureType case insensitive`() {
        assertEquals("Ciudad", DictionaryEngine.translateFeatureType("CITY"))
        assertEquals("Monte", DictionaryEngine.translateFeatureType("Mountain"))
    }

    // ── extractSignificantWords ────────────────────────────────

    @Test
    fun `extractSignificantWords filters stopwords`() {
        val words = DictionaryEngine.extractSignificantWords(
            "El que esta con ellos es el que los ama"
        )
        assertFalse(words.any { it.lowercase() in setOf("el", "que", "con", "los", "es", "los") })
    }

    @Test
    fun `extractSignificantWords keeps words longer than 3 chars`() {
        val words = DictionaryEngine.extractSignificantWords(
            "Jesús amó al mundo y lo salvó"
        )
        assertTrue("Debe contener Jesus", words.any { it.lowercase() == "jesus" || it.lowercase() == "jesús" })
        assertTrue("Debe contener mundo", words.any { it.lowercase() == "mundo" })
    }

    @Test
    fun `extractSignificantWords removes duplicates`() {
        val words = DictionaryEngine.extractSignificantWords("Dios amo a Dios y Dios es amor")
        assertEquals(words.size, words.distinct().size)
    }

    @Test
    fun `extractSignificantWords handles empty string`() {
        val words = DictionaryEngine.extractSignificantWords("")
        assertTrue(words.isEmpty())
    }

    @Test
    fun `extractSignificantWords handles punctuation`() {
        val words = DictionaryEngine.extractSignificantWords(
            "Abraham; Isaac, Jacob... Israel!"
        )
        assertTrue(words.any { it.lowercase() == "abraham" })
        assertTrue(words.any { it.lowercase() == "isaac" })
        assertTrue(words.any { it.lowercase() == "jacob" })
        assertTrue(words.any { it.lowercase() == "israel" })
    }

    // ── buildSuggestions ───────────────────────────────────────

    @Test
    fun `buildSuggestions without history has pasajes and profundizar`() {
        val suggestions = DictionaryEngine.buildSuggestions("Gracia")
        assertEquals(2, suggestions.size)
        assertTrue(suggestions[0].label.contains("Gracia"))
        assertTrue(suggestions[0].isAi)
        assertEquals("Profundizar con IA", suggestions[1].label)
        assertTrue(suggestions[1].isAi)
    }

    @Test
    fun `buildSuggestions with history shows comparison`() {
        val history = listOf(
            ChatExchange(
                question = "¿quién fue Abraham?",
                response = "Abraham es...",
                resolvedTerm = "Abraham",
                intent = "WHO"
            )
        )
        val suggestions = DictionaryEngine.buildSuggestions("Fe", history)
        assertEquals(2, suggestions.size)
        assertTrue(suggestions[0].label.contains("Fe"))
        assertEquals("Comparar con Abraham", suggestions[1].label)
    }

    @Test
    fun `buildSuggestions ignores same term in history`() {
        val history = listOf(
            ChatExchange(
                question = "¿qué es Fe?",
                response = "Fe es...",
                resolvedTerm = "Fe",
                intent = "DEFINE"
            )
        )
        val suggestions = DictionaryEngine.buildSuggestions("Fe", history)
        assertEquals(2, suggestions.size)
        assertEquals("Profundizar con IA", suggestions[1].label)
    }

    @Test
    fun `buildSuggestions uses last different term`() {
        val history = listOf(
            ChatExchange(question = "", response = "", resolvedTerm = "Abraham", intent = "WHO"),
            ChatExchange(question = "", response = "", resolvedTerm = "Pacto", intent = "DEFINE")
        )
        val suggestions = DictionaryEngine.buildSuggestions("Fe", history)
        assertEquals("Comparar con Pacto", suggestions[1].label)
    }

    // ── truncateText ───────────────────────────────────────────

    @Test
    fun `truncateText short text not truncated`() {
        val text = "Short text."
        assertEquals(text, DictionaryEngine.truncateText(text, 100))
    }

    @Test
    fun `truncateText long text truncated with ellipsis`() {
        val text = "This is a very long text that should be truncated at some point."
        val result = DictionaryEngine.truncateText(text, 30)
        assertTrue(result.length <= 34) // 30 + "..."
        assertTrue(result.endsWith("..."))
    }

    @Test
    fun `truncateText cuts at sentence boundary when possible`() {
        val text = "First sentence. Second sentence that goes on and on and on."
        val result = DictionaryEngine.truncateText(text, 35)
        assertTrue(result.endsWith("."))
    }
}

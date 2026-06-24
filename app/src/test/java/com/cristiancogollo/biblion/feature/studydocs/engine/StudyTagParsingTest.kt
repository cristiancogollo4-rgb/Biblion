package com.cristiancogollo.biblion.feature.studydocs.engine

import com.cristiancogollo.biblion.feature.studydocs.model.DocTagGroups
import com.cristiancogollo.biblion.feature.studydocs.ui.editor.normalizeStudyTag
import com.cristiancogollo.biblion.feature.studydocs.ui.editor.parseStudyTags
import com.cristiancogollo.biblion.feature.studydocs.ui.editor.validateRequiredStudyTags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StudyTagParsingTest {

    @Test fun `parseStudyTags returns empty for empty input`() {
        assertEquals(emptyList<String>(), parseStudyTags(""))
    }

    @Test fun `parseStudyTags returns empty for whitespace input`() {
        assertEquals(emptyList<String>(), parseStudyTags("   "))
    }

    @Test fun `parseStudyTags splits by comma`() {
        assertEquals(listOf("predicacion", "jovenes", "fe"), parseStudyTags("predicacion, jovenes, fe"))
    }

    @Test fun `parseStudyTags filters out empty entries`() {
        assertEquals(listOf("predicacion", "jovenes"), parseStudyTags("predicacion,, jovenes,,"))
    }

    @Test fun `parseStudyTags removes duplicates`() {
        assertEquals(listOf("predicacion", "jovenes"), parseStudyTags("predicacion, jovenes, predicacion"))
    }

    @Test fun `parseStudyTags trims whitespace`() {
        assertEquals(listOf("predicacion", "jovenes"), parseStudyTags("  predicacion  ,  jovenes  "))
    }

    @Test fun `normalizeStudyTag lowercases and replaces spaces with dashes`() {
        assertEquals("predicacion", normalizeStudyTag("Predicacion"))
        assertEquals("discipulado", normalizeStudyTag("Discipulado"))
        assertEquals("identidad-en-cristo", normalizeStudyTag("Identidad en Cristo"))
    }

    @Test fun `normalizeStudyTag removes accents`() {
        assertEquals("oracion", normalizeStudyTag("Oración"))
        assertEquals("gracia", normalizeStudyTag("Gracia"))
        assertEquals("nino", normalizeStudyTag("Niño"))
    }

    @Test fun `normalizeStudyTag filters invalid characters`() {
        assertEquals("customtag", normalizeStudyTag("custom!@#tag"))
        // Caracteres invalidos (no letra, no digito, no '-') se filtran sin reemplazo
        assertEquals("helloworld", normalizeStudyTag("hello" + "$" + "world"))
    }

    @Test fun `normalizeStudyTag returns empty for whitespace only`() {
        assertEquals("", normalizeStudyTag("   "))
        assertEquals("", normalizeStudyTag(""))
    }

    @Test fun `validateRequiredStudyTags returns null for complete tags`() {
        val tags = listOf("predicacion", "jovenes", "fe", "borrador")
        assertNull(validateRequiredStudyTags(tags))
    }

    @Test fun `validateRequiredStudyTags fails without purpose`() {
        val tags = listOf("jovenes", "fe", "borrador")
        val result = validateRequiredStudyTags(tags)
        assertTrue("Should fail without purpose: $result", result?.contains("proposito") == true)
    }

    @Test fun `validateRequiredStudyTags fails without audience`() {
        val tags = listOf("predicacion", "fe", "borrador")
        val result = validateRequiredStudyTags(tags)
        assertTrue("Should fail without audience: $result", result?.contains("audiencia") == true)
    }

    @Test fun `validateRequiredStudyTags fails without topic`() {
        val tags = listOf("predicacion", "jovenes", "borrador")
        val result = validateRequiredStudyTags(tags)
        assertTrue("Should fail without topic: $result", result?.contains("tema") == true)
    }

    @Test fun `validateRequiredStudyTags fails without state`() {
        val tags = listOf("predicacion", "jovenes", "fe")
        val result = validateRequiredStudyTags(tags)
        assertTrue("Should fail without state: $result", result?.contains("estado") == true)
    }

    @Test fun `validateRequiredStudyTags fails with empty list`() {
        val result = validateRequiredStudyTags(emptyList())
        assertTrue("Should fail with empty: $result", result != null)
    }

    @Test fun `DocTagGroups PURPOSE contains predicacion`() {
        assertTrue(DocTagGroups.PURPOSE_TAGS.contains("predicacion"))
        assertTrue(DocTagGroups.PURPOSE_TAGS.contains("devocional"))
        assertTrue(DocTagGroups.PURPOSE_TAGS.contains("estudio-biblico"))
    }

    @Test fun `DocTagGroups STATE contains borrador`() {
        assertTrue(DocTagGroups.STATE_TAGS.contains("borrador"))
        assertTrue(DocTagGroups.STATE_TAGS.contains("en-preparacion"))
        assertTrue(DocTagGroups.STATE_TAGS.contains("finalizado"))
    }

    @Test fun `parseStudyTags preserves order`() {
        val result = parseStudyTags("z, a, m, b")
        assertEquals(listOf("z", "a", "m", "b"), result)
    }
}

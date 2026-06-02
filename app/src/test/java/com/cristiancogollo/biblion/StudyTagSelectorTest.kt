package com.cristiancogollo.biblion

import junit.framework.TestCase.assertEquals
import org.junit.Test

class StudyTagSelectorTest {

    @Test
    fun parse_study_tags_normalizes_custom_input() {
        val tags = parseStudyTags(" Fe, #Jovenes, estudio biblico, fe,  ")

        assertEquals(listOf("fe", "jovenes", "estudio-biblico"), tags)
    }

    @Test
    fun validate_required_study_tags_accepts_all_required_sections() {
        val tags = listOf("predicacion", "jovenes", "fe", "borrador")

        assertEquals(null, validateRequiredStudyTags(tags))
    }

    @Test
    fun validate_required_study_tags_rejects_missing_section() {
        val tags = listOf("predicacion", "jovenes", "borrador")

        assertEquals("Debes seleccionar al menos una etiqueta en Tema.", validateRequiredStudyTags(tags))
    }

    @Test
    fun validate_required_study_tags_rejects_multiple_statuses() {
        val tags = listOf("predicacion", "jovenes", "fe", "borrador", "finalizado")

        assertEquals("Debes seleccionar solo un estado.", validateRequiredStudyTags(tags))
    }
}

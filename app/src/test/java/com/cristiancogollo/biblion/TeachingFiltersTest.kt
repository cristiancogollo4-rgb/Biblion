package com.cristiancogollo.biblion

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Test

class TeachingFiltersTest {

    @Test
    fun tag_filter_matches_any_tag_inside_same_group_and_all_selected_groups() {
        val groups = listOf(
            TeachingTagFilterGroup("Proposito", listOf("predicacion", "devocional")),
            TeachingTagFilterGroup("Audiencia", listOf("jovenes", "iglesia"))
        )
        val selected = setOf("predicacion", "devocional", "jovenes")

        assertTrue(
            matchesSelectedTeachingTags(
                studyTags = listOf("devocional", "jovenes"),
                selectedTags = selected,
                tagGroups = groups
            )
        )
        assertFalse(
            matchesSelectedTeachingTags(
                studyTags = listOf("devocional", "iglesia"),
                selectedTags = selected,
                tagGroups = groups
            )
        )
    }

    @Test
    fun tag_filter_groups_include_predefined_groups_and_available_custom_tags() {
        val groups = buildTeachingTagFilterGroups(
            listOf("jovenes", "fe", "borrador", "serie-romanos", "jovenes")
        )

        assertEquals(
            listOf("Proposito", "Audiencia", "Tema", "Estado", "Personalizadas"),
            groups.map { it.title }
        )
        assertTrue("predicacion" in groups.first().tags)
        assertEquals(listOf("serie-romanos"), groups.last().tags)
    }
}

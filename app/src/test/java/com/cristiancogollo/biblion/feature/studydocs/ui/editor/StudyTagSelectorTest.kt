package com.cristiancogollo.biblion.feature.studydocs.ui.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import com.cristiancogollo.biblion.R

class StudyTagSelectorTest {

    @Test
    fun stateSelectionReplacesOnlyThePreviousState() {
        val selected = listOf("predicacion", "jovenes", "fe", "borrador", "nota-personal")

        assertEquals(
            listOf("predicacion", "jovenes", "fe", "nota-personal", "finalizado"),
            toggleStudyTag(selected, suggestedStudyTagGroups.last(), "finalizado"),
        )
    }

    @Test
    fun purposeAllowsMultipleSelectionsAndTogglingOff() {
        val purpose = suggestedStudyTagGroups.first()

        assertEquals(
            listOf("predicacion", "devocional"),
            toggleStudyTag(listOf("predicacion"), purpose, "devocional"),
        )
        assertEquals(
            listOf("devocional"),
            toggleStudyTag(listOf("predicacion", "devocional"), purpose, "predicacion"),
        )
    }

    @Test
    fun labelsAreReadableWithoutChangingStoredTagKeys() {
        assertEquals(R.string.study_tag_estudio_biblico, studyTagDisplayNameRes("estudio-biblico"))
        assertNull(studyTagDisplayNameRes("notas-personales"))
        assertEquals(listOf("estudio-biblico", "notas-personales"), parseStudyTags("Estudio bíblico, Notas personales"))
    }

    @Test
    fun extraTagsDoNotReplaceRequiredCategories() {
        assertEquals(0, firstIncompleteStudyTagGroupIndex(listOf("notas-personales")))
        assertEquals(R.string.study_tags_error_purpose, studyTagValidationMessageRes(0))
        assertEquals(1, firstIncompleteStudyTagGroupIndex(listOf("predicacion")))
        assertEquals(2, firstIncompleteStudyTagGroupIndex(listOf("predicacion", "jovenes")))
        assertEquals(3, firstIncompleteStudyTagGroupIndex(listOf("predicacion", "jovenes", "fe")))
        assertNull(firstIncompleteStudyTagGroupIndex(listOf("predicacion", "jovenes", "fe", "finalizado", "notas-personales")))
    }

    @Test
    fun unfinishedCustomTagIsIncludedWhenSaving() {
        assertEquals(
            listOf("predicacion", "jovenes", "fe", "finalizado", "notas-personales"),
            studyTagsForSave("predicacion, jovenes, fe, finalizado", "Notas personales"),
        )
    }
}

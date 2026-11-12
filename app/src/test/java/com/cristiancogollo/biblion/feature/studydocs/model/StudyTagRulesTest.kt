package com.cristiancogollo.biblion.feature.studydocs.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StudyTagRulesTest {

    @Test
    fun completeTagsAreValid() {
        assertTrue(StudyTagRules.isValid(listOf("predicacion", "jovenes", "fe", "finalizado")))
    }

    @Test
    fun customTagsDoNotSatisfyRequiredGroups() {
        assertEquals(0, StudyTagRules.firstIncompleteGroupIndex(listOf("notas-personales")))
    }

    @Test
    fun reportsFirstMissingGroupInOrder() {
        assertEquals(1, StudyTagRules.firstIncompleteGroupIndex(listOf("predicacion")))
        assertEquals(2, StudyTagRules.firstIncompleteGroupIndex(listOf("predicacion", "jovenes")))
        assertEquals(3, StudyTagRules.firstIncompleteGroupIndex(listOf("predicacion", "jovenes", "fe")))
    }

    @Test
    fun stateAllowsExactlyOneTag() {
        val tags = listOf("predicacion", "jovenes", "fe", "borrador", "finalizado")
        assertFalse(StudyTagRules.isValid(tags))
        assertEquals(3, StudyTagRules.firstIncompleteGroupIndex(tags))
    }

    @Test
    fun repeatedStateTagCountsOnce() {
        assertNull(
            StudyTagRules.firstIncompleteGroupIndex(listOf("clase", "ninos", "amor", "finalizado", "finalizado")),
        )
    }
}

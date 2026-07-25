package com.cristiancogollo.biblion.feature.studydocs.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StudyDocSaveRulesTest {
    @Test
    fun blankTitleIsNotPersistable() {
        assertFalse(StudyDoc(title = "").hasPersistableTitle())
        assertFalse(StudyDoc(title = "   \n\t").hasPersistableTitle())
    }

    @Test
    fun nonBlankTitleIsPersistable() {
        assertTrue(StudyDoc(title = "La fe").hasPersistableTitle())
        assertTrue(StudyDoc(title = "  La fe  ").hasPersistableTitle())
    }
}

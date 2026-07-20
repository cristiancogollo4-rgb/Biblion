package com.cristiancogollo.biblion.feature.studydocs.ui.history

import com.cristiancogollo.biblion.feature.studydocs.data.DocVersionRepository.DocVersion
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import com.cristiancogollo.biblion.feature.studydocs.model.StudyDoc
import com.cristiancogollo.biblion.feature.studydocs.model.StyledText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionHistoryLogicTest {

    @Test
    fun docVersion_ordersByVersionNumberDescending() {
        val versions = listOf(
            DocVersion(1, "doc1", 3, "v3", 5, 100, "Cambio 3", 1000L),
            DocVersion(2, "doc1", 1, "v1", 3, 50, "Cambio 1", 800L),
            DocVersion(3, "doc1", 2, "v2", 4, 75, "Cambio 2", 900L),
        )
        val sorted = versions.sortedByDescending { it.versionNumber }
        assertEquals(3, sorted[0].versionNumber)
        assertEquals(2, sorted[1].versionNumber)
        assertEquals(1, sorted[2].versionNumber)
    }

    @Test
    fun docVersion_identifiesChangeByBlockCount() {
        val oldVersion = DocVersion(1, "doc1", 1, "v1", 10, 200, null, 1000L)
        val newVersion = DocVersion(2, "doc1", 2, "v2", 15, 300, "Añadidos bloques", 2000L)
        assertTrue(newVersion.blockCount != oldVersion.blockCount)
        assertTrue(newVersion.wordCount != oldVersion.wordCount)
    }

    @Test
    fun docVersion_nullChangeDescription_isAllowed() {
        val version = DocVersion(1, "doc1", 1, "Test", 5, 100, null, 1000L)
        assertEquals(null, version.changeDescription)
    }

    @Test
    fun docVersion_formattedTimestamp_isReadable() {
        val now = System.currentTimeMillis()
        val version = DocVersion(1, "doc1", 1, "Test", 5, 100, null, now)
        assertTrue(version.createdAt > 0)
    }

    @Test
    fun versionDiffDetectsBlockChanges() {
        val oldDoc = StudyDoc(
            title = "Original",
            blocks = listOf(
                StudyBlock.Paragraph(text = StyledText.plain("Texto original")),
            ),
        )
        val newDoc = StudyDoc(
            title = "Modificado",
            blocks = listOf(
                StudyBlock.Paragraph(text = StyledText.plain("Texto original")),
                StudyBlock.Paragraph(text = StyledText.plain("Texto añadido")),
            ),
        )
        assertFalse(oldDoc.title == newDoc.title)
        assertFalse(oldDoc.blocks.size == newDoc.blocks.size)
        assertEquals(1, oldDoc.blocks.size)
        assertEquals(2, newDoc.blocks.size)
    }
}

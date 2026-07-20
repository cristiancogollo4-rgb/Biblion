package com.cristiancogollo.biblion.feature.studydocs.data

import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import com.cristiancogollo.biblion.feature.studydocs.model.StudyDoc
import com.cristiancogollo.biblion.feature.studydocs.model.StyledText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DocVersionRepositoryTest {

    @Test
    fun docVersion_hasCorrectProperties() {
        val version = DocVersionRepository.DocVersion(
            id = 1L,
            docRemoteId = "doc_123",
            versionNumber = 1,
            title = "Test",
            blockCount = 5,
            wordCount = 100,
            changeDescription = "Versión inicial",
            createdAt = System.currentTimeMillis(),
        )
        assertEquals(1L, version.id)
        assertEquals("doc_123", version.docRemoteId)
        assertEquals(1, version.versionNumber)
        assertEquals("Test", version.title)
        assertEquals(5, version.blockCount)
        assertEquals(100, version.wordCount)
        assertEquals("Versión inicial", version.changeDescription)
    }

    @Test
    fun docVersion_defaultMaxVersionsIs50() {
        assertEquals(50, DocVersionRepository.DEFAULT_MAX_VERSIONS)
    }

    @Test
    fun studyDoc_wordCount_returnsCorrectCount() {
        val doc = StudyDoc(
            blocks = listOf(
                StudyBlock.Paragraph(text = StyledText.plain("Primer párrafo con algunas palabras aquí")),
                StudyBlock.Heading(text = StyledText.plain("Segundo título")),
            ),
        )
        val count = doc.wordCount()
        assertTrue(count > 0)
        assertEquals(8, count)
    }

    @Test
    fun studyDoc_charCount_returnsCorrectCount() {
        val doc = StudyDoc(
            blocks = listOf(
                StudyBlock.Paragraph(text = StyledText.plain("Hola mundo")),
                StudyBlock.Heading(text = StyledText.plain("Adios")),
            ),
        )
        val count = doc.charCount()
        assertEquals(15, count)
    }

    @Test
    fun studyDoc_plainText_includesAllBlocks() {
        val doc = StudyDoc(
            title = "Mi Doc",
            blocks = listOf(
                StudyBlock.Paragraph(text = StyledText.plain("Párrafo")),
                StudyBlock.Heading(text = StyledText.plain("Título")),
            ),
        )
        val text = doc.plainText()
        assertTrue(text.contains("Mi Doc"))
        assertTrue(text.contains("Párrafo"))
        assertTrue(text.contains("Título"))
    }

    @Test
    fun studyDoc_blockById_returnsCorrectBlock() {
        val block1 = StudyBlock.Paragraph(text = StyledText.plain("Primero"))
        val block2 = StudyBlock.Heading(text = StyledText.plain("Segundo"))
        val doc = StudyDoc(blocks = listOf(block1, block2))
        val found = doc.blockById(block1.id)
        assertNotNull(found)
        assertEquals(block1.id, found!!.id)
    }

    @Test
    fun studyDoc_blockById_returnsNullForMissing() {
        val doc = StudyDoc(blocks = listOf(StudyBlock.Paragraph(text = StyledText.plain("Test"))))
        val fakeId = com.cristiancogollo.biblion.feature.studydocs.model.BlockId("no_existe")
        assertEquals(null, doc.blockById(fakeId))
    }

    @Test
    fun studyDoc_isEmpty_whenNoContent() {
        val doc = StudyDoc()
        assertTrue(doc.isEmpty)
    }

    @Test
    fun studyDoc_heading_hasCorrectLevel() {
        val h = StudyBlock.Heading(level = 2, text = StyledText.plain("Test"))
        assertEquals(2, h.level)
    }

    @Test
    fun verseRef_displayShort_showsRange() {
        val ref = com.cristiancogollo.biblion.feature.studydocs.model.VerseRef(
            book = "Génesis", chapter = 1, verseStart = 1, verseEnd = 3, version = "RVR1960",
        )
        assertEquals("Génesis 1:1-3", ref.displayShort())
    }

    @Test
    fun verseRef_displayShort_singleVerse() {
        val ref = com.cristiancogollo.biblion.feature.studydocs.model.VerseRef(
            book = "Juan", chapter = 3, verseStart = 16, version = "RVR1960",
        )
        assertEquals("Juan 3:16", ref.displayShort())
    }
}

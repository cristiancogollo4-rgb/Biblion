package com.cristiancogollo.biblion.feature.studydocs.data

import com.cristiancogollo.biblion.feature.studydocs.model.DocMetadata
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import com.cristiancogollo.biblion.feature.studydocs.model.StudyDoc
import com.cristiancogollo.biblion.feature.studydocs.model.StyledText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File

class BibStudyPackageTest {

    @Test
    fun `export and import preserves the complete study document`() {
        val document = StudyDoc(
            title = "La fe",
            metadata = DocMetadata(tags = listOf("estudio-biblico", "fe")),
            blocks = listOf(
                StudyBlock.Paragraph(text = StyledText("Una ensenanza")),
                StudyBlock.BulletList(items = listOf(StyledText("Uno"), StyledText("Dos"))),
            ),
        )
        val file = File.createTempFile("biblion-study", ".bib")
        try {
            BibStudyPackage.export(document, file)
            val imported = file.inputStream().use(BibStudyPackage::read)
            assertEquals(document.title, imported.document.title)
            assertEquals(document.blocks.size, imported.document.blocks.size)
            assertEquals(document.metadata.tags, imported.document.metadata.tags)
            assertTrue(imported.verifiedHash)
        } finally {
            file.delete()
        }
    }

    @Test
    fun `hash is deterministic`() {
        val payload = "biblion"
        assertEquals(StudyContentHash.sha256(payload), StudyContentHash.sha256(payload))
        assertEquals(64, StudyContentHash.sha256(payload).length)
    }
}

package com.cristiancogollo.biblion.feature.studydocs.data

import com.cristiancogollo.biblion.feature.studydocs.model.DocMetadata
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import com.cristiancogollo.biblion.feature.studydocs.model.StudyDoc
import com.cristiancogollo.biblion.feature.studydocs.model.StyledText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

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

    @Test(expected = IllegalArgumentException::class)
    fun `import rejects an unsupported format version`() {
        val documentJson = StudyDocJson.encode(StudyDoc(title = "Version futura"))
        val archive = zipOf(
            "manifest.json" to
                """{"format":"biblion-study","formatVersion":99,"contentHash":"${StudyContentHash.sha256(documentJson)}","exportedAt":1,"title":"Version futura"}""",
            "document.json" to documentJson,
        )

        BibStudyPackage.read(ByteArrayInputStream(archive))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `import rejects an oversized document entry`() {
        val archive = zipOf(
            "manifest.json" to
                """{"format":"biblion-study","formatVersion":1,"contentHash":"","exportedAt":1,"title":"Grande"}""",
            "document.json" to "x".repeat(2 * 1024 * 1024 + 1),
        )

        BibStudyPackage.read(ByteArrayInputStream(archive))
    }

    private fun zipOf(vararg entries: Pair<String, String>): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            entries.forEach { (name, value) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(value.toByteArray())
                zip.closeEntry()
            }
        }
        return output.toByteArray()
    }
}

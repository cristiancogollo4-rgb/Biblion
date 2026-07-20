package com.cristiancogollo.biblion.feature.studydocs.data.export

import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import com.cristiancogollo.biblion.feature.studydocs.model.StudyDoc
import com.cristiancogollo.biblion.feature.studydocs.model.StyledText
import com.cristiancogollo.biblion.feature.studydocs.model.VerseRef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownExportImportTest {

    @Test
    fun exportEmptyDoc_returnsEmptyString() {
        val doc = StudyDoc(title = "", blocks = listOf(StudyBlock.Paragraph(text = StyledText.Empty)))
        val md = MarkdownExporter.export(doc)
        assertEquals("", md.trim())
    }

    @Test
    fun exportDocWithTitle_includesHeader() {
        val doc = StudyDoc(title = "Mi Estudio", blocks = listOf(StudyBlock.Paragraph(text = StyledText.Empty)))
        val md = MarkdownExporter.export(doc)
        assertTrue(md.startsWith("# Mi Estudio"))
    }

    @Test
    fun exportParagraph_wrapsAsPlainText() {
        val doc = StudyDoc(
            blocks = listOf(
                StudyBlock.Paragraph(text = StyledText.plain("En el principio creó Dios los cielos y la tierra."))
            ),
        )
        val md = MarkdownExporter.export(doc)
        assertTrue(md.contains("En el principio creó Dios los cielos y la tierra."))
    }

    @Test
    fun exportHeading_usesCorrectPrefix() {
        val doc = StudyDoc(
            blocks = listOf(
                StudyBlock.Heading(level = 1, text = StyledText.plain("Título Principal")),
                StudyBlock.Heading(level = 2, text = StyledText.plain("Subtítulo")),
                StudyBlock.Heading(level = 3, text = StyledText.plain("Sección")),
            ),
        )
        val md = MarkdownExporter.export(doc)
        assertTrue(md.contains("# Título Principal"))
        assertTrue(md.contains("## Subtítulo"))
        assertTrue(md.contains("### Sección"))
    }

    @Test
    fun exportBulletList_usesDashPrefix() {
        val doc = StudyDoc(
            blocks = listOf(
                StudyBlock.BulletList(
                    items = listOf(
                        StyledText.plain("Primer item"),
                        StyledText.plain("Segundo item"),
                    ),
                ),
            ),
        )
        val md = MarkdownExporter.export(doc)
        assertTrue(md.contains("- Primer item"))
        assertTrue(md.contains("- Segundo item"))
    }

    @Test
    fun exportNumberedList_usesNumberPrefix() {
        val doc = StudyDoc(
            blocks = listOf(
                StudyBlock.NumberedList(
                    items = listOf(
                        StyledText.plain("Primero"),
                        StyledText.plain("Segundo"),
                    ),
                ),
            ),
        )
        val md = MarkdownExporter.export(doc)
        assertTrue(md.contains("1. Primero"))
        assertTrue(md.contains("2. Segundo"))
    }

    @Test
    fun exportQuote_prependsGreaterThan() {
        val doc = StudyDoc(
            blocks = listOf(
                StudyBlock.Quote(
                    text = StyledText.plain("Esta es una cita."),
                    attribution = "Autor",
                ),
            ),
        )
        val md = MarkdownExporter.export(doc)
        assertTrue(md.contains("> Esta es una cita."))
        assertTrue(md.contains("> — Autor"))
    }

    @Test
    fun exportVerse_showsReferenceAndText() {
        val doc = StudyDoc(
            blocks = listOf(
                StudyBlock.Verse(
                    reference = VerseRef(book = "Génesis", chapter = 1, verseStart = 1, verseEnd = 3, version = "RVR1960"),
                    primaryText = StyledText.plain("1 En el principio..."),
                    primaryVersion = "RVR1960",
                ),
            ),
        )
        val md = MarkdownExporter.export(doc)
        assertTrue(md.contains("Génesis 1:1-3"))
        assertTrue(md.contains("En el principio"))
    }

    @Test
    fun exportDivider_usesThreeDashes() {
        val doc = StudyDoc(
            blocks = listOf(
                StudyBlock.Divider(),
            ),
        )
        val md = MarkdownExporter.export(doc)
        assertTrue(md.contains("---"))
    }

    @Test
    fun exportBoldText_usesDoubleAsterisks() {
        val text = StyledText(
            raw = "Hola mundo",
            ranges = listOf(
                com.cristiancogollo.biblion.feature.studydocs.model.StyleRange(
                    start = 0, endExclusive = 4, bold = true,
                ),
            ),
        )
        val doc = StudyDoc(blocks = listOf(StudyBlock.Paragraph(text = text)))
        val md = MarkdownExporter.export(doc)
        assertTrue(md.contains("**Hola** mundo"))
    }

    @Test
    fun exportItalicText_usesSingleAsterisks() {
        val text = StyledText(
            raw = "Hola mundo",
            ranges = listOf(
                com.cristiancogollo.biblion.feature.studydocs.model.StyleRange(
                    start = 5, endExclusive = 10, italic = true,
                ),
            ),
        )
        val doc = StudyDoc(blocks = listOf(StudyBlock.Paragraph(text = text)))
        val md = MarkdownExporter.export(doc)
        assertTrue(md.contains("Hola *mundo*"))
    }

    @Test
    fun importSimpleMarkdown_createsParagraph() {
        val md = "Este es un párrafo de prueba."
        val doc = MarkdownImporter.import(md)
        assertEquals(1, doc.blocks.size)
        val block = doc.blocks.first()
        assertTrue(block is StudyBlock.Paragraph)
        assertEquals("Este es un párrafo de prueba.", (block as StudyBlock.Paragraph).text.raw)
    }

    @Test
    fun importHeading_createsHeadingBlock() {
        val md = "## Subtítulo"
        val doc = MarkdownImporter.import(md)
        val hasHeading = doc.blocks.any { it is StudyBlock.Heading }
        assertTrue(hasHeading)
        val heading = doc.blocks.filterIsInstance<StudyBlock.Heading>().first()
        assertEquals(2, heading.level)
        assertEquals("Subtítulo", heading.text.raw)
    }

    @Test
    fun importBulletList_createsBulletListBlock() {
        val md = "- Item uno\n- Item dos\n- Item tres"
        val doc = MarkdownImporter.import(md)
        assertTrue(doc.blocks.first() is StudyBlock.BulletList)
        val list = doc.blocks.first() as StudyBlock.BulletList
        assertEquals(3, list.items.size)
        assertEquals("Item uno", list.items[0].raw)
    }

    @Test
    fun importNumberedList_createsNumberedListBlock() {
        val md = "1. Primero\n2. Segundo"
        val doc = MarkdownImporter.import(md)
        assertTrue(doc.blocks.first() is StudyBlock.NumberedList)
        val list = doc.blocks.first() as StudyBlock.NumberedList
        assertEquals(2, list.items.size)
    }

    @Test
    fun importQuote_createsQuoteBlock() {
        val md = "> Cita importante"
        val doc = MarkdownImporter.import(md)
        assertTrue(doc.blocks.first() is StudyBlock.Quote)
        val quote = doc.blocks.first() as StudyBlock.Quote
        assertEquals("Cita importante", quote.text.raw)
    }

    @Test
    fun importDivider_createsDividerBlock() {
        val md = "---"
        val doc = MarkdownImporter.import(md)
        assertTrue(doc.blocks.first() is StudyBlock.Divider)
    }

    @Test
    fun roundtrip_exportThenImport_preservesContent() {
        val original = StudyDoc(
            title = "Test",
            blocks = listOf(
                StudyBlock.Heading(level = 1, text = StyledText.plain("Introducción")),
                StudyBlock.Paragraph(text = StyledText.plain("Contenido del párrafo.")),
                StudyBlock.BulletList(
                    items = listOf(
                        StyledText.plain("Punto A"),
                        StyledText.plain("Punto B"),
                    ),
                ),
                StudyBlock.Heading(level = 2, text = StyledText.plain("Conclusión")),
                StudyBlock.Paragraph(text = StyledText.plain("Conclusión final.")),
            ),
        )
        val md = MarkdownExporter.export(original)
        val restored = MarkdownImporter.import(md)
        assertEquals("Test", restored.title)
        assertTrue(restored.blocks.isNotEmpty())
    }
}

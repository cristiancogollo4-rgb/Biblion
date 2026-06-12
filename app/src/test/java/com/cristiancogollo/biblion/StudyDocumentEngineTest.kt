package com.cristiancogollo.biblion

import junit.framework.TestCase.assertEquals
import org.junit.Test

class StudyDocumentEngineTest {

    @Test
    fun snapshot_includes_column_embedded_blocks() {
        val blocks = listOf(
            StudyBlockNode.Paragraph(
                text = "Columna base",
                parallelText = "Columna paralela",
                role = "columns",
                embeddedBlocks = listOf(
                    ColumnEmbeddedBlock(type = "note", title = "Nota", text = "Idea pastoral")
                ),
                parallelEmbeddedBlocks = listOf(
                    ColumnEmbeddedBlock(type = "reflection", title = "Reflexion", text = "Aplicacion")
                )
            )
        )

        val snapshot = StudyDocumentEngine.buildPlainTextSnapshot(blocks)

        assertEquals(
            "Columna base\nNota\nIdea pastoral\nColumna paralela\nReflexion\nAplicacion",
            snapshot
        )
    }

    @Test
    fun normalize_keeps_paragraph_with_only_column_embedded_blocks() {
        val blocks = listOf(
            StudyBlockNode.Paragraph(text = "Introduccion"),
            StudyBlockNode.Paragraph(
                role = "columns",
                embeddedBlocks = listOf(ColumnEmbeddedBlock(type = "note", title = "Nota", text = "Contenido"))
            )
        )

        val normalized = StudyDocumentEngine.normalizeStudyFlow(blocks)

        assertEquals(2, normalized.size)
        assertEquals("columns", (normalized[1] as StudyBlockNode.Paragraph).role)
    }

    @Test
    fun column_embedded_update_only_changes_selected_side() {
        val main = ColumnEmbeddedBlock(blockId = "main-note", type = "note", text = "A")
        val parallel = ColumnEmbeddedBlock(blockId = "parallel-note", type = "note", text = "B")
        val blocks = listOf(
            StudyBlockNode.Paragraph(
                blockId = "p1",
                role = "columns",
                embeddedBlocks = listOf(main),
                parallelEmbeddedBlocks = listOf(parallel)
            )
        )

        val updated = StudyDocumentEngine.updateColumnEmbeddedBlocks(
            blocks = blocks,
            fallbackHtml = "",
            paragraphBlockId = "p1",
            source = "parallel"
        ) { current ->
            current.map { it.copy(collapsed = true) }
        }

        val paragraph = updated.single() as StudyBlockNode.Paragraph
        assertEquals(false, paragraph.embeddedBlocks.single().collapsed)
        assertEquals(true, paragraph.parallelEmbeddedBlocks.single().collapsed)
    }

    @Test
    fun column_flow_interleaves_blocks_at_saved_position() {
        val flow = StudyDocumentEngine.buildColumnFlow(
            text = "Antes despues",
            blocks = listOf(ColumnEmbeddedBlock(type = "note", title = "Nota", text = "Centro", position = 6))
        )

        assertEquals("Antes ", flow[0].text)
        assertEquals("Nota", flow[0].blocksAfter.single().title)
        assertEquals("despues", flow[1].text)
    }

    @Test
    fun editing_text_before_column_block_moves_position_forward() {
        val blocks = listOf(
            StudyBlockNode.Paragraph(
                blockId = "p1",
                text = "Antes despues",
                role = "columns",
                embeddedBlocks = listOf(
                    ColumnEmbeddedBlock(blockId = "note", type = "note", text = "Centro", position = 6)
                )
            )
        )

        val updated = StudyDocumentEngine.updateParagraphText(
            blocks = blocks,
            fallbackHtml = "",
            blockId = "p1",
            text = "Muy antes despues"
        )

        val paragraph = updated.single() as StudyBlockNode.Paragraph
        assertEquals(10, paragraph.embeddedBlocks.single().position)
        assertEquals("Muy antes ", StudyDocumentEngine.buildColumnFlow(paragraph.text, paragraph.embeddedBlocks)[0].text)
    }

    @Test
    fun clear_style_preserves_unselected_segments() {
        val blocks = listOf(
            StudyBlockNode.Paragraph(
                blockId = "p1",
                text = "Gracia y verdad",
                styles = listOf(TextStyleRange(start = 0, end = 15, bold = true))
            )
        )

        val updated = StudyDocumentEngine.clearParagraphTextStyle(
            blocks = blocks,
            fallbackHtml = "",
            blockId = "p1",
            source = "main",
            start = 7,
            end = 8,
            clearColor = true,
            clearBackground = true,
            clearBold = true,
            clearItalic = true,
            clearUnderline = true,
            clearFontSize = true
        )

        val styles = (updated.single() as StudyBlockNode.Paragraph).styles
        assertEquals(listOf(0 to 7, 8 to 15), styles.map { it.start to it.end })
    }

    @Test
    fun detect_references_accepts_accented_book_names() {
        val references = StudyDocumentEngine.detectReferences("\u00C9xodo 3:2 y Genesis 1:1-3")

        assertEquals(2, references.size)
        assertEquals("\u00C9xodo", references[0].book)
        assertEquals(3, references[1].verseEnd)
    }
}

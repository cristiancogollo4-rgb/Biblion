package com.cristiancogollo.biblion.feature.studydocs.domain

import com.cristiancogollo.biblion.feature.studydocs.model.BlockId
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import com.cristiancogollo.biblion.feature.studydocs.model.StudyDoc
import com.cristiancogollo.biblion.feature.studydocs.model.StyleRange
import com.cristiancogollo.biblion.feature.studydocs.model.StyledText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EditCommandUndoTest {

    private fun para(id: String, text: String) =
        StudyBlock.Paragraph(id = BlockId(id), text = StyledText.plain(text))

    @Test
    fun delete_block_undo_restores_block() {
        val a = para("a", "Alpha")
        val b = para("b", "Beta")
        val doc = StudyDoc(blocks = listOf(a, b))
        val stack = EditCommandStack()

        val afterDelete = stack.execute(DeleteBlockCommand(BlockId("a")), doc)
        assertEquals(1, afterDelete.blocks.size)
        assertEquals("Beta", (afterDelete.blocks[0] as StudyBlock.Paragraph).text.plain())

        val afterUndo = stack.undo(afterDelete)
        assertEquals(2, afterUndo.blocks.size)
        assertEquals("Alpha", (afterUndo.blocks[0] as StudyBlock.Paragraph).text.plain())
        assertEquals("Beta", (afterUndo.blocks[1] as StudyBlock.Paragraph).text.plain())
    }

    @Test
    fun delete_block_redo_re_deletes() {
        val a = para("a", "Alpha")
        val b = para("b", "Beta")
        val doc = StudyDoc(blocks = listOf(a, b))
        val stack = EditCommandStack()

        val afterDelete = stack.execute(DeleteBlockCommand(BlockId("a")), doc)
        val afterUndo = stack.undo(afterDelete)
        val afterRedo = stack.redo(afterUndo)

        assertEquals(1, afterRedo.blocks.size)
        assertEquals("Beta", (afterRedo.blocks[0] as StudyBlock.Paragraph).text.plain())
    }

    @Test
    fun replace_block_undo_restores_original() {
        val a = para("a", "Original")
        val doc = StudyDoc(blocks = listOf(a))
        val stack = EditCommandStack()

        val replacement = para("a", "Replaced")
        val afterReplace = stack.execute(ReplaceBlockCommand(BlockId("a"), replacement), doc)
        assertEquals("Replaced", (afterReplace.blocks[0] as StudyBlock.Paragraph).text.plain())

        val afterUndo = stack.undo(afterReplace)
        assertEquals("Original", (afterUndo.blocks[0] as StudyBlock.Paragraph).text.plain())
    }

    @Test
    fun replace_block_redo_re_replaces() {
        val a = para("a", "Original")
        val doc = StudyDoc(blocks = listOf(a))
        val stack = EditCommandStack()

        val replacement = para("a", "Replaced")
        val afterReplace = stack.execute(ReplaceBlockCommand(BlockId("a"), replacement), doc)
        val afterUndo = stack.undo(afterReplace)
        val afterRedo = stack.redo(afterUndo)

        assertEquals("Replaced", (afterRedo.blocks[0] as StudyBlock.Paragraph).text.plain())
    }

    @Test
    fun merge_blocks_preserves_styles() {
        val a = StudyBlock.Paragraph(
            id = BlockId("a"),
            text = StyledText("Hello", listOf(StyleRange(start = 0, endExclusive = 5, bold = true))),
        )
        val b = StudyBlock.Paragraph(
            id = BlockId("b"),
            text = StyledText("World", listOf(StyleRange(start = 0, endExclusive = 5, italic = true))),
        )
        val doc = StudyDoc(blocks = listOf(a, b))
        val stack = EditCommandStack()

        val cmd = MergeBlocksCommand(
            targetIndex = 0,
            targetOriginalText = a.text,
            sourceIndex = 1,
            sourceOriginalText = b.text,
        )
        val afterMerge = stack.execute(cmd, doc)

        assertEquals(1, afterMerge.blocks.size)
        val merged = afterMerge.blocks[0] as StudyBlock.Paragraph
        assertEquals("HelloWorld", merged.text.plain())
        assertEquals(2, merged.text.ranges.size)
        assertTrue(merged.text.ranges.any { it.bold })
        assertTrue(merged.text.ranges.any { it.italic })
    }

    @Test
    fun merge_blocks_undo_restores_both_blocks() {
        val a = para("a", "Hello")
        val b = para("b", "World")
        val doc = StudyDoc(blocks = listOf(a, b))
        val stack = EditCommandStack()

        val cmd = MergeBlocksCommand(
            targetIndex = 0,
            targetOriginalText = a.text,
            sourceIndex = 1,
            sourceOriginalText = b.text,
        )
        val afterMerge = stack.execute(cmd, doc)
        val afterUndo = stack.undo(afterMerge)

        assertEquals(2, afterUndo.blocks.size)
        assertEquals("Hello", (afterUndo.blocks[0] as StudyBlock.Paragraph).text.plain())
        assertEquals("World", (afterUndo.blocks[1] as StudyBlock.Paragraph).text.plain())
    }

    @Test
    fun insert_block_undo_removes_it() {
        val doc = StudyDoc(blocks = listOf(para("a", "A")))
        val stack = EditCommandStack()

        val newBlock = para("b", "B")
        val afterInsert = stack.execute(InsertBlockCommand(1, newBlock), doc)
        assertEquals(2, afterInsert.blocks.size)

        val afterUndo = stack.undo(afterInsert)
        assertEquals(1, afterUndo.blocks.size)
        assertEquals("A", (afterUndo.blocks[0] as StudyBlock.Paragraph).text.plain())
    }

    @Test
    fun undo_on_empty_stack_returns_current_doc() {
        val doc = StudyDoc(blocks = listOf(para("a", "A")))
        val stack = EditCommandStack()

        val result = stack.undo(doc)
        assertEquals(doc, result)
    }

    @Test
    fun redo_on_empty_stack_returns_current_doc() {
        val doc = StudyDoc(blocks = listOf(para("a", "A")))
        val stack = EditCommandStack()

        val result = stack.redo(doc)
        assertEquals(doc, result)
    }
}

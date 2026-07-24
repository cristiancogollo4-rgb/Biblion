package com.cristiancogollo.biblion.feature.studydocs.domain

import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import com.cristiancogollo.biblion.feature.studydocs.model.StudyDoc
import com.cristiancogollo.biblion.feature.studydocs.model.StyledText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorHistoryTest {

    @Test
    fun `materializa todos los elementos de una lista`() {
        val list = StudyBlock.BulletList(
            items = listOf(StyledText("uno"), StyledText("dos")),
        )
        val doc = StudyDoc(blocks = listOf(list))

        val result = doc.withEditorTexts(
            mapOf(
                EditorTextKey(list.id, 0) to StyledText("primer elemento"),
                EditorTextKey(list.id, 1) to StyledText("segundo elemento"),
            ),
        )

        assertEquals(
            listOf("primer elemento", "segundo elemento"),
            (result.blocks.single() as StudyBlock.BulletList).items.map { it.raw },
        )
    }

    @Test
    fun `undo y redo recorren checkpoints`() {
        val first = StudyDoc(blocks = listOf(StudyBlock.Paragraph()))
        val second = first.copy(title = "Segunda version")
        val history = EditorHistory()

        history.pushBeforeChange(EditorCheckpoint(first, emptyMap(), first.blocks.first().id))
        val undone = history.undo(EditorCheckpoint(second, emptyMap(), second.blocks.first().id))

        assertEquals(first, undone?.doc)
        assertTrue(history.canRedo)
        assertFalse(history.canUndo)

        val redone = history.redo(EditorCheckpoint(first, emptyMap(), first.blocks.first().id))
        assertEquals(second, redone?.doc)
        assertTrue(history.canUndo)
        assertFalse(history.canRedo)
    }
}

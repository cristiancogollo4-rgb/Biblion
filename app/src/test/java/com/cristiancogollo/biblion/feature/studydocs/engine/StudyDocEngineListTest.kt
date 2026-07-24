package com.cristiancogollo.biblion.feature.studydocs.engine

import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import com.cristiancogollo.biblion.feature.studydocs.model.StudyDoc
import com.cristiancogollo.biblion.feature.studydocs.model.StyledText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StudyDocEngineListTest {

    @Test
    fun `convertir una lista conserva todos sus elementos`() {
        val list = StudyBlock.BulletList(
            items = listOf(StyledText("uno"), StyledText("dos")),
        )
        val (result, status) = StudyDocEngine.apply(
            StudyDoc(blocks = listOf(list)),
            StudyOp.ChangeBlockType(list.id, "paragraph"),
        )

        assertTrue(status.isSuccess)
        assertEquals("uno\ndos", result.blocks.single().plainText())
    }

    @Test
    fun `convertir entre listas conserva los items separados`() {
        val list = StudyBlock.BulletList(
            items = listOf(StyledText("uno"), StyledText("dos")),
        )

        val (result, status) = StudyDocEngine.apply(
            StudyDoc(blocks = listOf(list)),
            StudyOp.ChangeBlockType(list.id, "numbered"),
        )

        assertTrue(status.isSuccess)
        val numbered = result.blocks.single() as StudyBlock.OrderedList
        assertEquals(listOf("uno", "dos"), numbered.items.map { it.raw })
    }

    @Test
    fun `dividir un bloque actualiza izquierda y derecha en una sola operacion`() {
        val original = StudyBlock.Paragraph(text = StyledText("texto completo"))
        val left = original.copy(text = StyledText("texto"))
        val right = StudyBlock.Paragraph(text = StyledText("completo"))

        val (result, status) = StudyDocEngine.apply(
            StudyDoc(blocks = listOf(original)),
            StudyOp.SplitBlock(original.id, right, updatedSplitBlock = left),
        )

        assertTrue(status.isSuccess)
        assertEquals(listOf("texto", "completo"), result.blocks.map { it.plainText() })
    }
}

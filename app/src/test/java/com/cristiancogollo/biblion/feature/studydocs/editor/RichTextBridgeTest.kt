package com.cristiancogollo.biblion.feature.studydocs.editor

import com.cristiancogollo.biblion.feature.studydocs.model.StyleRange
import com.cristiancogollo.biblion.feature.studydocs.model.StyledText
import com.cristiancogollo.biblion.feature.studydocs.ui.editor.toRichHtml
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RichTextBridgeTest {

    @Test
    fun `toRichHtml conserva estilos inline`() {
        val text = StyledText(
            raw = "Enseñanza",
            ranges = listOf(
                StyleRange(
                    start = 0,
                    endExclusive = 9,
                    bold = true,
                    italic = true,
                    underline = true,
                    color = 0xFF1E88E5.toInt(),
                    background = 0xFFFFF2CC.toInt(),
                    fontSizeSp = 18f,
                ),
            ),
        )

        val html = text.toRichHtml()

        assertTrue(html.contains("<strong>"))
        assertTrue(html.contains("<em>"))
        assertTrue(html.contains("<u>"))
        assertTrue(html.contains("color:#1E88E5"))
        assertTrue(html.contains("background-color:#FFF2CC"))
        assertTrue(html.contains("font-size:18.0px"))
    }

    @Test
    fun `cambiar tamano conserva color y demas estilos`() {
        val color = 0xFF1E88E5.toInt()
        val background = 0xFFFFF2CC.toInt()
        val resized = StyledText(
            raw = "Ensenanza",
            ranges = listOf(
                StyleRange(
                    start = 0,
                    endExclusive = 9,
                    bold = true,
                    color = color,
                    background = background,
                ),
            ),
        ).withFontSize(start = 2, endExclusive = 7, fontSizeSp = 20f)

        val middle = resized.ranges.single { it.start == 2 && it.endExclusive == 7 }

        assertEquals(color, middle.color)
        assertEquals(background, middle.background)
        assertEquals(20f, middle.fontSizeSp)
        assertTrue(middle.bold)
        assertTrue(resized.ranges.all { it.color == color && it.background == background && it.bold })
        assertTrue(resized.toRichHtml().contains("color:#1E88E5"))
        assertTrue(resized.toRichHtml().contains("font-size:20.0px"))
    }
}

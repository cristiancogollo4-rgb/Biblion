package com.cristiancogollo.biblion.feature.studydocs.editor

import com.cristiancogollo.biblion.feature.studydocs.model.StyleRange
import com.cristiancogollo.biblion.feature.studydocs.model.StyledText
import com.cristiancogollo.biblion.feature.studydocs.ui.editor.toRichHtml
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
}

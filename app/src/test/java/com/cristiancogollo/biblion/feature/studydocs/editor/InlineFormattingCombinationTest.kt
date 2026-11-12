package com.cristiancogollo.biblion.feature.studydocs.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnitType
import com.cristiancogollo.biblion.feature.studydocs.domain.InlineColorTarget
import com.cristiancogollo.biblion.feature.studydocs.domain.applyFontSizePreservingStyles
import com.cristiancogollo.biblion.feature.studydocs.domain.setInlineColor
import com.cristiancogollo.biblion.feature.studydocs.model.StyledText
import com.cristiancogollo.biblion.feature.studydocs.ui.editor.fromAnnotatedString
import com.cristiancogollo.biblion.feature.studydocs.ui.editor.syncFromStyledText
import com.cristiancogollo.biblion.feature.studydocs.ui.editor.toAnnotatedString
import com.mohamedrejeb.richeditor.model.RichTextState
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Drives the real rich editor with the same calls the toolbar makes and checks that
 * every character keeps its visible style after a save/reload round trip
 * (RichTextState -> StyledText -> RichTextState) and in the read-mode renderer.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class InlineFormattingCombinationTest {

    private data class CharStyle(
        val bold: Boolean,
        val italic: Boolean,
        val underline: Boolean,
        val strike: Boolean,
        val color: Int?,
        val background: Int?,
        val size: Float?,
    )

    private val red = Color(0xFFC62828).toArgb()
    private val blue = Color(0xFF1565C0).toArgb()
    private val yellow = Color(0xFFFFF59D).toArgb()

    private fun editor(text: String) = RichTextState().apply { setHtml("<p>$text</p>") }

    private fun RichTextState.select(start: Int, end: Int) {
        selection = TextRange(start, end)
    }

    /**
     * Style as Compose draws it: spans applied in order, later specified values win.
     * (getSpanStyle merges differently and can hide a span that covers a highlight.)
     */
    private fun RichTextState.styleAt(index: Int): CharStyle =
        annotatedString.spanStyles
            .filter { it.start <= index && it.end > index }
            .fold(SpanStyle()) { acc, r -> acc.merge(r.item) }
            .toCharStyle()

    private fun SpanStyle.toCharStyle() = CharStyle(
        bold = fontWeight == FontWeight.Bold,
        italic = fontStyle == FontStyle.Italic,
        underline = textDecoration?.contains(TextDecoration.Underline) == true,
        strike = textDecoration?.contains(TextDecoration.LineThrough) == true,
        color = color.takeIf { it != Color.Unspecified && it.alpha > 0f }?.toArgb(),
        background = background.takeIf { it != Color.Unspecified && it.alpha > 0f }?.toArgb(),
        size = fontSize.takeIf { it.type == TextUnitType.Sp }?.value,
    )

    private fun RichTextState.styles(): List<CharStyle> =
        annotatedString.text.indices.map { styleAt(it) }

    /** Styles as the read screen renders them from the persisted model. */
    private fun StyledText.readModeStyles(): List<CharStyle> {
        val annotated = toAnnotatedString()
        return raw.indices.map { i ->
            annotated.spanStyles
                .filter { it.start <= i && it.end > i }
                .fold(SpanStyle()) { acc, r -> acc.merge(r.item) }
                .toCharStyle()
        }
    }

    /** Asserts editor, reloaded editor and read mode all show the same styles. */
    private fun assertSurvivesRoundTrip(state: RichTextState) {
        val expected = state.styles()
        val persisted = StyledText.fromAnnotatedString(state.annotatedString)
        val reloaded = RichTextState().apply { syncFromStyledText(persisted) }

        assertEquals("texto tras recargar", state.annotatedString.text, reloaded.annotatedString.text)
        assertEquals("estilos en el editor tras recargar", expected, reloaded.styles())
        assertEquals("estilos en modo lectura", expected, persisted.readModeStyles())
    }

    @Test
    fun boldItalicUnderlineStrikeOverlapping() {
        val state = editor("Gracia y paz a vosotros")
        state.select(0, 23); state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
        state.select(7, 15); state.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic))
        state.select(3, 10); state.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.Underline))
        state.select(8, 18); state.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))

        val mid = state.styleAt(9)
        assertEquals(true, mid.bold)
        assertEquals(true, mid.italic)
        assertEquals("subrayado + tachado juntos", true, mid.underline && mid.strike)
        assertSurvivesRoundTrip(state)
    }

    @Test
    fun removingBoldFromMiddleOfBoldText() {
        val state = editor("Bienaventurados")
        state.select(0, 15); state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
        state.select(4, 8); state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))

        assertEquals(false, state.styleAt(5).bold)
        assertEquals(true, state.styleAt(1).bold)
        assertSurvivesRoundTrip(state)
    }

    @Test
    fun removingUnderlineKeepsStrikethrough() {
        val state = editor("Fe esperanza amor")
        state.select(0, 17)
        state.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.Underline))
        state.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
        state.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.Underline))

        val style = state.styleAt(4)
        assertEquals(false, style.underline)
        assertEquals(true, style.strike)
        assertSurvivesRoundTrip(state)
    }

    @Test
    fun recoloringPartOfColoredText() {
        val state = editor("El Senor es mi pastor")
        state.select(0, 21); state.setInlineColor(InlineColorTarget.TEXT, red)
        state.select(3, 8); state.setInlineColor(InlineColorTarget.TEXT, blue)

        assertEquals(blue, state.styleAt(4).color)
        assertEquals(red, state.styleAt(0).color)
        assertSurvivesRoundTrip(state)
    }

    @Test
    fun colorHighlightAndSizeKeepEmphasis() {
        val state = editor("Lampara es a mis pies")
        state.select(0, 21)
        state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
        state.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic))
        state.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.Underline))
        state.select(2, 15)
        state.setInlineColor(InlineColorTarget.TEXT, red)
        state.setInlineColor(InlineColorTarget.BACKGROUND, yellow)
        state.applyFontSizePreservingStyles(TextRange(5, 12), 24f)

        val style = state.styleAt(6)
        assertEquals(CharStyle(true, true, true, false, red, yellow, 24f), style)
        assertEquals(null, state.styleAt(0).size)
        assertSurvivesRoundTrip(state)
    }

    @Test
    fun clearingTextColorKeepsHighlightAndBold() {
        val state = editor("Palabra viva")
        state.select(0, 12)
        state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
        state.setInlineColor(InlineColorTarget.TEXT, blue)
        state.setInlineColor(InlineColorTarget.BACKGROUND, yellow)
        state.setInlineColor(InlineColorTarget.TEXT, null)

        assertEquals(CharStyle(true, false, false, false, null, yellow, null), state.styleAt(3))
        assertSurvivesRoundTrip(state)
    }

    @Test
    fun applyingBoldAfterSizeKeepsSize() {
        val state = editor("Amaras al Senor")
        state.select(0, 15); state.applyFontSizePreservingStyles(TextRange(0, 15), 20f)
        state.select(0, 6); state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))

        assertEquals(20f, state.styleAt(2).size)
        assertEquals(true, state.styleAt(2).bold)
        assertSurvivesRoundTrip(state)
    }

    @Test
    fun legacyTransparentBackgroundDoesNotTurnBlack() {
        val legacy = StyledText(
            raw = "Bienaventurados",
            ranges = listOf(
                com.cristiancogollo.biblion.feature.studydocs.model.StyleRange(4, 8, background = 0),
            ),
        )
        val reloaded = RichTextState().apply { syncFromStyledText(legacy) }

        assertEquals(null, reloaded.styleAt(5).background)
    }

    @Test
    fun underlineAndStrikeSurviveTogetherWithColorAndSize() {
        val state = editor("Cristo vive")
        state.select(0, 11)
        state.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.Underline))
        state.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
        state.setInlineColor(InlineColorTarget.TEXT, red)
        state.applyFontSizePreservingStyles(TextRange(0, 6), 28f)

        assertEquals(CharStyle(false, false, true, true, red, null, 28f), state.styleAt(2))
        assertSurvivesRoundTrip(state)
    }

    @Test
    fun highlightIsVisibleRightAfterApplyingIt() {
        val orange = Color(0xFFFB8C00).toArgb()
        val highlight = Color(0xFF1565C0).toArgb()
        val state = editor("Bienaventurados los de limpio corazon")
        state.select(0, 37); state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
        state.select(16, 22); state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
        state.select(0, 37); state.setInlineColor(InlineColorTarget.TEXT, orange)
        state.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.Underline))
        state.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
        state.select(23, 29); state.setInlineColor(InlineColorTarget.BACKGROUND, highlight)

        assertEquals(CharStyle(true, false, true, true, orange, highlight, null), state.styleAt(25))
        assertEquals(CharStyle(false, false, true, true, orange, null, null), state.styleAt(18))
        assertSurvivesRoundTrip(state)
    }
}

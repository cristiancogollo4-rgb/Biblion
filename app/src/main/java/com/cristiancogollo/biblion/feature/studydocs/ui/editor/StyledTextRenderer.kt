package com.cristiancogollo.biblion.feature.studydocs.ui.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.cristiancogollo.biblion.feature.studydocs.model.StyledText

object StyledTextRenderer {

    fun toAnnotatedString(text: StyledText, defaultColor: Color = Color.Unspecified): AnnotatedString = buildAnnotatedString {
        append(text.plain())
        text.ranges.forEach { range ->
            addStyle(
                style = SpanStyle(
                    color = if (range.color != null) Color(range.color) else defaultColor,
                    fontWeight = if (range.bold) FontWeight.Bold else null,
                    fontStyle = if (range.italic) FontStyle.Italic else null,
                    textDecoration = when {
                        range.underline && range.strikethrough -> TextDecoration.combine(
                            listOf(TextDecoration.Underline, TextDecoration.LineThrough),
                        )
                        range.underline -> TextDecoration.Underline
                        range.strikethrough -> TextDecoration.LineThrough
                        else -> null
                    },
                ),
                start = range.start,
                end = range.endExclusive,
            )
        }
    }
}

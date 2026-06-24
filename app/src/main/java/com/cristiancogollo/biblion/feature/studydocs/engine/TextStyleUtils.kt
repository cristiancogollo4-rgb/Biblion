package com.cristiancogollo.biblion.feature.studydocs.engine

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration

enum class SpanType {
    Bold,
    Italic,
    Underline,
    Strikethrough,
    Color,
}

fun TextFieldValue.hasSpan(type: SpanType): Boolean {
    if (selection.collapsed) return false
    val selStart = selection.start
    val selEnd = selection.end
    return annotatedString.spanStyles.any { range ->
        val style = range.item
        range.start <= selStart && range.end >= selEnd && matchesType(style, type)
    }
}

private fun matchesType(style: SpanStyle, type: SpanType): Boolean = when (type) {
    SpanType.Bold -> style.fontWeight == FontWeight.Bold
    SpanType.Italic -> style.fontStyle == FontStyle.Italic
    SpanType.Underline -> style.textDecoration?.contains(TextDecoration.Underline) == true
    SpanType.Strikethrough -> style.textDecoration?.contains(TextDecoration.LineThrough) == true
    SpanType.Color -> style.color != androidx.compose.ui.graphics.Color.Unspecified
}

fun TextFieldValue.countSpansOfType(type: SpanType): Int {
    if (selection.collapsed) return 0
    val selStart = selection.start
    val selEnd = selection.end
    return annotatedString.spanStyles.count { range ->
        range.start <= selStart && range.end >= selEnd && matchesType(range.item, type)
    }
}

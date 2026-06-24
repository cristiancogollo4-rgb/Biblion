package com.cristiancogollo.biblion.feature.studydocs.model

import kotlinx.serialization.Serializable

@Serializable
data class StyleRange(
    val start: Int,
    val endExclusive: Int,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val strikethrough: Boolean = false,
    val color: Int? = null,
    val background: Int? = null,
    val fontSizeSp: Float? = null,
    val link: String? = null,
) {
    fun asRange(): IntRange = start until endExclusive
    val isEmpty: Boolean
        get() = !bold && !italic && !underline && !strikethrough &&
            color == null && background == null && fontSizeSp == null && link == null
}

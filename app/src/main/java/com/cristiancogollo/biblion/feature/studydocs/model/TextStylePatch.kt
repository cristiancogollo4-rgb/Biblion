package com.cristiancogollo.biblion.feature.studydocs.model

import kotlinx.serialization.Serializable

@Serializable
data class TextStylePatch(
    val bold: Boolean? = null,
    val italic: Boolean? = null,
    val underline: Boolean? = null,
    val strikethrough: Boolean? = null,
    val color: Int? = null,
    val background: Int? = null,
    val fontSizeSp: Float? = null,
    val link: String? = null,
) {
    val isEmpty: Boolean
        get() = bold == null && italic == null && underline == null && strikethrough == null &&
            color == null && background == null && fontSizeSp == null && link == null
}

package com.cristiancogollo.biblion.feature.studydocs.model

import kotlinx.serialization.Serializable

@Serializable
data class VerseRef(
    val book: String,
    val chapter: Int,
    val verseStart: Int,
    val verseEnd: Int = verseStart,
    val version: String = "RVR1960",
) {
    init {
        require(book.isNotBlank()) { "book must not be blank" }
        require(chapter > 0) { "chapter must be positive" }
        require(verseStart > 0) { "verseStart must be positive" }
        require(verseEnd >= verseStart) { "verseEnd must be >= verseStart" }
        require(version.isNotBlank()) { "version must not be blank" }
    }

    fun displayShort(): String = "$book $chapter:$verseStart" +
        if (verseEnd > verseStart) "-$verseEnd" else ""
}

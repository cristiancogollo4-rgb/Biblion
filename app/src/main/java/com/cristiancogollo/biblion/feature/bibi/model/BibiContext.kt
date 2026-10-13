package com.cristiancogollo.biblion.feature.bibi.model

import kotlinx.serialization.Serializable

sealed interface BibiContext {
    val bibleVersion: String

    data class Reader(
        val book: String,
        val chapter: Int,
        val passages: List<BibiPassage> = emptyList(),
        override val bibleVersion: String = "rv1960",
        val hasExplicitSelection: Boolean = true,
    ) : BibiContext

    data class Study(
        val documentId: String,
        val title: String,
        val tags: List<String>,
        val selectedText: String,
        val outline: List<String>,
        val notes: List<String>,
        val passages: List<BibiPassage>,
        override val bibleVersion: String = "rv1960",
    ) : BibiContext
}

@Serializable
data class BibiPassage(
    val book: String,
    val chapter: Int,
    val verse: Int,
    val text: String,
) {
    val reference: String
        get() = "$book $chapter:$verse"

    fun asPromptText(): String = "$reference $text".trim()
}

fun formatBibiPassageSelection(passages: List<BibiPassage>): String {
    if (passages.isEmpty()) return ""
    return passages.groupBy { it.book to it.chapter }
        .entries
        .joinToString("; ") { (location, chapterPassages) ->
            val verseNumbers = chapterPassages.map { it.verse }.distinct().sorted()
            val ranges = mutableListOf<String>()
            var start = verseNumbers.first()
            var end = start
            for (verse in verseNumbers.drop(1)) {
                if (verse == end + 1) {
                    end = verse
                } else {
                    ranges += if (start == end) "$start" else "$start-$end"
                    start = verse
                    end = verse
                }
            }
            ranges += if (start == end) "$start" else "$start-$end"
            "${location.first} ${location.second}:${ranges.joinToString(", ")}"
        }
}

data class BibiQueryResult(
    val answer: String,
    val suggestions: List<BibiSuggestion> = emptyList(),
    val references: List<BibiReference> = emptyList(),
    val suggestedBlocks: List<String> = emptyList(),
    val disclaimer: String? = null,
    val confidence: Confidence = Confidence.MEDIUM,
    val source: Source,
    val resolvedTerm: String? = null,
    val intent: String,
)

data class BibiReference(
    val reference: String,
    val reason: String = "",
)

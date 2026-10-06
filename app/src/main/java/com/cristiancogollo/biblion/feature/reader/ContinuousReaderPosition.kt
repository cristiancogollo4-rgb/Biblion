package com.cristiancogollo.biblion.feature.reader

/** Chapter item keys stay stable when a chapter moves between adjacent and active slots. */
internal fun chapterForReaderItemKey(key: Any?): Int? {
    val itemKey = key as? String ?: return null
    if (!itemKey.startsWith("chapter-")) return null
    val remainder = itemKey.removePrefix("chapter-")
    val chapter = remainder.substringBefore('-').toIntOrNull()?.takeIf { it > 0 }
        ?: return null
    val itemKind = remainder.substringAfter('-', missingDelimiterValue = "")
    if (
        itemKind != "header" &&
        !itemKind.startsWith("verse-") &&
        !itemKind.startsWith("section-")
    ) return null
    return chapter
}

internal fun continuousChapterDirection(
    firstVisibleItemKey: Any?,
    currentChapter: Int,
    hasPreviousChapter: Boolean,
    hasNextChapter: Boolean,
): Int? = when (chapterForReaderItemKey(firstVisibleItemKey)) {
    currentChapter - 1 -> if (hasPreviousChapter) -1 else null
    currentChapter + 1 -> if (hasNextChapter) 1 else null
    else -> null
}

internal data class ContinuousChapterWindow<T>(
    val chapter: Int,
    val current: T,
    val previous: T?,
    val next: T?,
) {
    fun shift(direction: Int): ContinuousChapterWindow<T>? = when (direction) {
        -1 -> previous?.let {
            ContinuousChapterWindow(chapter - 1, it, null, current)
        }
        1 -> next?.let {
            ContinuousChapterWindow(chapter + 1, it, current, null)
        }
        else -> null
    }
}

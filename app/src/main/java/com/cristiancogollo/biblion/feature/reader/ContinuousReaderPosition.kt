package com.cristiancogollo.biblion.feature.reader

/**
 * In continuous mode every chapter in the window starts with a header item, so the current
 * chapter body starts after the previous chapter (header + body) and its own header.
 */
internal fun currentChapterBodyStartIndex(
    continuousScrolling: Boolean,
    hasPreviousChapter: Boolean,
    previousBodyItemCount: Int,
): Int = when {
    !continuousScrolling -> 0
    hasPreviousChapter -> previousBodyItemCount + 2
    else -> 1
}

/** Capitulo activo y sus vecinos cargados. Avanzar conserva el actual como anterior. */
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

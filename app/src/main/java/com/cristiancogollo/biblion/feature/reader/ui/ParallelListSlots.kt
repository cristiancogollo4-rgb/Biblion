package com.cristiancogollo.biblion.feature.reader.ui

import androidx.compose.runtime.Immutable
import com.cristiancogollo.biblion.ChapterContent
import com.cristiancogollo.biblion.feature.reader.BibleTextSection
import com.cristiancogollo.biblion.feature.reader.buildBibleTextSections

/**
 * Elementos de la lista de un panel paralelo. La lista visible se genera desde aqui,
 * de modo que el indice de cada versiculo es exacto para saltos y scroll sincronizado.
 */
@Immutable
internal sealed interface ParallelListSlot {
    val key: String
    val chapter: Int
    val verseNumbers: List<Int>

    /** Inicio de capitulo en lectura continua. */
    data class ChapterHeader(
        override val chapter: Int,
        val separateFromPrevious: Boolean,
    ) : ParallelListSlot {
        override val key: String = "chapter-$chapter-header"
        override val verseNumbers: List<Int> = emptyList()
    }

    data class Section(override val chapter: Int, val section: BibleTextSection) : ParallelListSlot {
        override val key: String = "chapter-$chapter-section-${section.firstVerseNumber}"
        override val verseNumbers: List<Int> = section.verses.mapNotNull { it.first.toIntOrNull() }
    }

    data class Verse(
        override val chapter: Int,
        val number: String,
        val text: String,
        val title: String?,
    ) : ParallelListSlot {
        override val key: String = "chapter-$chapter-verse-$number"
        override val verseNumbers: List<Int> = listOfNotNull(number.toIntOrNull())
    }

    data class NextLoader(override val chapter: Int, val failed: Boolean) : ParallelListSlot {
        override val key: String = "chapter-$chapter-loading"
        override val verseNumbers: List<Int> = emptyList()
    }

    /** Cierre del capitulo cuando no hay un siguiente en la misma lista. */
    data class ChapterEnd(override val chapter: Int) : ParallelListSlot {
        override val key: String = "chapter-$chapter-end"
        override val verseNumbers: List<Int> = emptyList()
    }
}

/** Punto de lectura compartido entre paneles vinculados. */
@Immutable
internal data class ParallelScrollAnchor(
    val pane: Int,
    val chapter: Int,
    val verse: Int,
    val offsetFraction: Float,
)

internal fun buildParallelListSlots(
    currentChapter: Int,
    current: ChapterContent,
    previous: ChapterContent?,
    next: ChapterContent?,
    continuousScrolling: Boolean,
    flowingLayout: Boolean,
    nextLoadFailed: Boolean,
): List<ParallelListSlot> = buildList {
    if (current.verses.isEmpty()) return@buildList

    fun addChapter(chapter: Int, content: ChapterContent, showHeader: Boolean, separate: Boolean) {
        if (showHeader) add(ParallelListSlot.ChapterHeader(chapter, separate))
        if (flowingLayout) {
            buildBibleTextSections(content.verses, content.titlesByVerse).forEach { section ->
                add(ParallelListSlot.Section(chapter, section))
            }
        } else {
            content.verses.forEach { (number, text) ->
                add(ParallelListSlot.Verse(chapter, number, text, content.titlesByVerse[number]))
            }
        }
    }

    val shownPrevious = previous.takeIf { continuousScrolling }
    val shownNext = next.takeIf { continuousScrolling }
    shownPrevious?.let { addChapter(currentChapter - 1, it, showHeader = true, separate = false) }
    addChapter(currentChapter, current, showHeader = continuousScrolling, separate = shownPrevious != null)
    when {
        shownNext != null -> addChapter(currentChapter + 1, shownNext, showHeader = true, separate = true)
        continuousScrolling && currentChapter < current.chapterCount ->
            add(ParallelListSlot.NextLoader(currentChapter + 1, nextLoadFailed))
        else -> add(ParallelListSlot.ChapterEnd(currentChapter))
    }
}

internal fun List<ParallelListSlot>.indexOfVerse(chapter: Int, verse: Int): Int =
    indexOfFirst { it.chapter == chapter && verse in it.verseNumbers }

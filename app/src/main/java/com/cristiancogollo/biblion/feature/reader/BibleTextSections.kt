package com.cristiancogollo.biblion.feature.reader

data class BibleTextSection(
    val firstVerseNumber: String,
    val heading: String?,
    val verses: List<Pair<String, String>>,
)

fun buildBibleTextSections(
    verses: List<Pair<String, String>>,
    titlesByVerse: Map<String, String>,
): List<BibleTextSection> {
    if (verses.isEmpty()) return emptyList()

    val sections = mutableListOf<BibleTextSection>()
    var currentHeading: String? = null
    var currentVerses = mutableListOf<Pair<String, String>>()

    fun flushSection() {
        if (currentVerses.isEmpty()) return
        sections += BibleTextSection(
            firstVerseNumber = currentVerses.first().first,
            heading = currentHeading,
            verses = currentVerses.toList(),
        )
        currentVerses = mutableListOf()
    }

    verses.forEach { verse ->
        val heading = titlesByVerse[verse.first]?.takeIf(String::isNotBlank)
        if (heading != null && currentVerses.isNotEmpty()) {
            flushSection()
        }
        if (heading != null) {
            currentHeading = heading
        }
        currentVerses += verse
    }
    flushSection()
    return sections
}

fun chapterBodyItemCount(
    verses: List<Pair<String, String>>,
    titlesByVerse: Map<String, String>,
    textLayout: ReaderTextLayout,
): Int = when (textLayout) {
    ReaderTextLayout.VERSE_BLOCKS -> verses.size
    ReaderTextLayout.FLOWING -> buildBibleTextSections(verses, titlesByVerse).size
}

fun chapterBodyIndexForVerse(
    verseNumber: String,
    verses: List<Pair<String, String>>,
    titlesByVerse: Map<String, String>,
    textLayout: ReaderTextLayout,
): Int = when (textLayout) {
    ReaderTextLayout.VERSE_BLOCKS -> verses.indexOfFirst { it.first == verseNumber }
    ReaderTextLayout.FLOWING -> buildBibleTextSections(verses, titlesByVerse)
        .indexOfFirst { section -> section.verses.any { it.first == verseNumber } }
}

fun firstVerseAtBodyIndex(
    index: Int,
    verses: List<Pair<String, String>>,
    titlesByVerse: Map<String, String>,
    textLayout: ReaderTextLayout,
): Pair<String, String>? = when (textLayout) {
    ReaderTextLayout.VERSE_BLOCKS -> verses.getOrNull(index)
    ReaderTextLayout.FLOWING -> buildBibleTextSections(verses, titlesByVerse)
        .getOrNull(index)
        ?.verses
        ?.firstOrNull()
}

package com.cristiancogollo.biblion

import junit.framework.TestCase.assertEquals
import org.junit.Test

class ReaderCitationGroupingTest {

    @Test
    fun contiguous_verses_are_grouped_as_single_reference_range() {
        val groups = buildCitationVerseGroups(
            bookName = "Genesis",
            chapter = 1,
            selections = listOf(
                VerseAction(number = "1", text = "En el principio creo Dios los cielos y la tierra."),
                VerseAction(number = "2", text = "Y la tierra estaba desordenada y vacia."),
                VerseAction(number = "3", text = "Y dijo Dios: Sea la luz.")
            )
        )

        assertEquals(1, groups.size)
        assertEquals("Genesis 1:1-3", groups.first().reference)
        assertEquals(
            "1 En el principio creo Dios los cielos y la tierra. " +
                "2 Y la tierra estaba desordenada y vacia. " +
                "3 Y dijo Dios: Sea la luz.",
            groups.first().text
        )
    }

    @Test
    fun non_contiguous_verses_are_kept_in_one_citation_block() {
        val groups = buildCitationVerseGroups(
            bookName = "Genesis",
            chapter = 1,
            selections = listOf(
                VerseAction(number = "1", text = "Texto 1"),
                VerseAction(number = "2", text = "Texto 2"),
                VerseAction(number = "4", text = "Texto 4")
            )
        )

        assertEquals(1, groups.size)
        assertEquals("Genesis 1:1-2,4", groups.first().reference)
        assertEquals(listOf(1, 2, 4), groups.first().verseNumbers)
        assertEquals("1 Texto 1 2 Texto 2 4 Texto 4", groups.first().text)
    }

    @Test
    fun selected_verse_range_positions_are_detected_for_visual_blocks() {
        val selected = setOf(1, 2, 3, 5)

        assertEquals(VerseSelectionRangePosition.Start, verseSelectionRangePosition("1", selected))
        assertEquals(VerseSelectionRangePosition.Middle, verseSelectionRangePosition("2", selected))
        assertEquals(VerseSelectionRangePosition.End, verseSelectionRangePosition("3", selected))
        assertEquals(VerseSelectionRangePosition.Single, verseSelectionRangePosition("5", selected))
        assertEquals(VerseSelectionRangePosition.None, verseSelectionRangePosition("4", selected))
        assertEquals(VerseSelectionRangePosition.None, verseSelectionRangePosition("intro", selected))
    }
}

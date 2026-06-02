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
    fun non_contiguous_verses_are_kept_as_separate_groups() {
        val groups = buildCitationVerseGroups(
            bookName = "Genesis",
            chapter = 1,
            selections = listOf(
                VerseAction(number = "1", text = "Texto 1"),
                VerseAction(number = "2", text = "Texto 2"),
                VerseAction(number = "4", text = "Texto 4")
            )
        )

        assertEquals(2, groups.size)
        assertEquals("Genesis 1:1-2", groups[0].reference)
        assertEquals("Genesis 1:4", groups[1].reference)
    }
}

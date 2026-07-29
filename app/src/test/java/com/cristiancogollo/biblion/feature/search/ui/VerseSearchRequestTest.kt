package com.cristiancogollo.biblion.feature.search.ui

import com.cristiancogollo.biblion.BibleSearchTestament
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class VerseSearchRequestTest {

    @Test
    fun `same query with different testament creates a new request`() {
        val allBible = VerseSearchRequest(
            query = "amor",
            testament = BibleSearchTestament.ALL,
        )
        val oldTestament = allBible.copy(testament = BibleSearchTestament.OLD)
        val newTestament = allBible.copy(testament = BibleSearchTestament.NEW)

        assertNotEquals(allBible, oldTestament)
        assertNotEquals(oldTestament, newTestament)
    }

    @Test
    fun `same query with a selected book creates a new request`() {
        val wholeTestament = VerseSearchRequest(
            query = "amor",
            testament = BibleSearchTestament.NEW,
        )
        val johnOnly = wholeTestament.copy(bookName = "Juan")

        assertNotEquals(wholeTestament, johnOnly)
    }

    @Test
    fun `request maps every selected filter to repository filter`() {
        val request = VerseSearchRequest(
            query = "amor",
            testament = BibleSearchTestament.NEW,
            bookName = "1 Juan",
        )

        val filter = request.toFilter()

        assertEquals(BibleSearchTestament.NEW, filter.testament)
        assertEquals("1 Juan", filter.bookName)
    }
}

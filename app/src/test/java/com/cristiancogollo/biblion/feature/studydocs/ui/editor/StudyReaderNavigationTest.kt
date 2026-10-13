package com.cristiancogollo.biblion.feature.studydocs.ui.editor

import com.cristiancogollo.biblion.Screen
import com.cristiancogollo.biblion.feature.studydocs.ui.read.studyBibleBooks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StudyReaderNavigationTest {
    @Test
    fun allBibleBooksRoundTripThroughStudyReaderRoute() {
        assertEquals(66, studyBibleBooks.size)

        studyBibleBooks.forEach { book ->
            val route = Screen.Reader.createRoute(bookName = book, chapter = 3, verse = "5")
            val encodedBook = route.substringAfter("reader/").substringBefore('?')

            assertEquals(book, decodeStudyReaderBookArg(encodedBook))
            assertTrue(route.contains("chapter=3"))
            assertTrue(route.contains("verse=5"))
        }
    }

    @Test
    fun numberedBookRouteDecodesSpacesBeforeDatabaseLookup() {
        assertEquals("1 Timoteo", decodeStudyReaderBookArg("1+Timoteo"))
        assertEquals("2 Cronicas", decodeStudyReaderBookArg("2+Cronicas"))
        assertEquals(null, decodeStudyReaderBookArg(null))
    }
}

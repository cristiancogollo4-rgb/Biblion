package com.cristiancogollo.biblion.feature.search

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cristiancogollo.biblion.BibleRepository
import com.cristiancogollo.biblion.BibleSearchFilter
import com.cristiancogollo.biblion.BibleSearchTestament
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.text.Normalizer
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class BibleSearchFilterIntegrationTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        BibleRepository.setSelectedVersionKey(context, "rv1960")
    }

    @Test
    fun amorReturnsDifferentAndCorrectlyScopedTestamentResults() = runBlocking {
        val all = search(BibleSearchTestament.ALL)
        val old = search(BibleSearchTestament.OLD)
        val new = search(BibleSearchTestament.NEW)

        assertTrue(all.isNotEmpty())
        assertTrue(old.isNotEmpty())
        assertTrue(new.isNotEmpty())
        assertTrue(all.size > old.size)
        assertTrue(all.size > new.size)
        assertEquals(all.size, old.size + new.size)
        assertTrue(old.map { it.reference }.toSet() subtract all.map { it.reference }.toSet() == emptySet<String>())
        assertTrue(new.map { it.reference }.toSet() subtract all.map { it.reference }.toSet() == emptySet<String>())
        val newBooksInOldResults = old
            .map { it.bookName }
            .filter { it.normalizedBookName() in newTestamentBooks }
            .distinct()
        val nonNewBooksInNewResults = new
            .map { it.bookName }
            .filterNot { it.normalizedBookName() in newTestamentBooks }
            .distinct()
        assertTrue(
            "New Testament books returned by OLD filter: $newBooksInOldResults",
            newBooksInOldResults.isEmpty(),
        )
        assertTrue(
            "Non-New Testament books returned by NEW filter: $nonNewBooksInNewResults",
            nonNewBooksInNewResults.isEmpty(),
        )
    }

    @Test
    fun selectedBookNeverReturnsVersesFromAnotherBook() = runBlocking {
        val newTestament = search(BibleSearchTestament.NEW)
        val john = BibleRepository.searchVerses(
            context = context,
            query = "amor",
            filter = BibleSearchFilter(
                testament = BibleSearchTestament.NEW,
                bookName = "Juan",
            ),
        )

        assertTrue(john.isNotEmpty())
        assertTrue(john.size < newTestament.size)
        assertTrue(john.all { it.bookName == "Juan" })
    }

    private suspend fun search(testament: BibleSearchTestament) =
        BibleRepository.searchVerses(
            context = context,
            query = "amor",
            filter = BibleSearchFilter(testament = testament),
        )

    private fun String.normalizedBookName(): String =
        Normalizer.normalize(lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")

    private companion object {
        val newTestamentBooks = setOf(
            "Mateo",
            "Marcos",
            "Lucas",
            "Juan",
            "Hechos",
            "Romanos",
            "1 Corintios",
            "2 Corintios",
            "Galatas",
            "Efesios",
            "Filipenses",
            "Colosenses",
            "1 Tesalonicenses",
            "2 Tesalonicenses",
            "1 Timoteo",
            "2 Timoteo",
            "Tito",
            "Filemon",
            "Hebreos",
            "Santiago",
            "1 Pedro",
            "2 Pedro",
            "1 Juan",
            "2 Juan",
            "3 Juan",
            "Judas",
            "Apocalipsis",
        ).map { book ->
            Normalizer.normalize(book.lowercase(Locale.ROOT), Normalizer.Form.NFD)
                .replace("\\p{Mn}+".toRegex(), "")
        }.toSet()
    }
}

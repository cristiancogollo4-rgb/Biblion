package com.cristiancogollo.biblion

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BibleRepositoryCacheIntegrationTest {

    @Test
    fun reusesChapterUntilItsVersionCacheIsCleared() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        BibleRepository.clearCache()

        val first = BibleRepository.getChapter(
            context = context,
            bookName = "Genesis",
            chapterNumber = 1,
            versionKey = "rv1960",
        )
        val cached = BibleRepository.getChapter(
            context = context,
            bookName = "Genesis",
            chapterNumber = 1,
            versionKey = "rv1960",
        )

        assertSame(first, cached)
        assertEquals(31, first.verses.size)
        assertEquals("La creación", first.titlesByVerse["1"])

        BibleRepository.clearVersionCache("rv1960")
        val reloaded = BibleRepository.getChapter(
            context = context,
            bookName = "Genesis",
            chapterNumber = 1,
            versionKey = "rv1960",
        )

        assertNotSame(first, reloaded)
        assertEquals(first, reloaded)
    }

    @Test
    fun concurrentRequestsShareTheSameLoadedChapter() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        BibleRepository.clearVersionCache("rv1960")

        val results = coroutineScope {
            List(8) {
                async(Dispatchers.Default) {
                    BibleRepository.getChapter(
                        context = context,
                        bookName = "Genesis",
                        chapterNumber = 2,
                        versionKey = "rv1960",
                    )
                }
            }.awaitAll()
        }

        val first = results.first()
        results.drop(1).forEach { result -> assertSame(first, result) }
        assertEquals(25, first.verses.size)
    }
}

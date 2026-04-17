package com.cristiancogollo.biblion.reader.cache

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HighlightsCacheTest {

    private var now = 0L

    private fun cache(
        maxVersions: Int = 2,
        maxChaptersPerVersion: Int = 3,
        ttlMillis: Long = 60_000L
    ): HighlightsCache {
        return HighlightsCache(
            maxVersions = maxVersions,
            maxChaptersPerVersion = maxChaptersPerVersion,
            entryTtlMillis = ttlMillis,
            nowProvider = { now }
        )
    }

    @Test
    fun keepsOnlyConfiguredNumberOfChaptersPerVersion() {
        val cache = cache(maxChaptersPerVersion = 3)
        val verses = listOf("1" to "a")

        (1..8).forEach { chapter ->
            cache.loadChapterHighlights(
                versionKey = "RVR1960",
                rawHighlights = "{}",
                bookName = "Juan",
                chapter = chapter,
                verses = verses,
                verseKeyProvider = { verse -> "Juan|$chapter|$verse" },
                validColorIndices = 0..4
            )
            now += 1_000
        }

        val snapshot = cache.debugSnapshot()
        assertEquals(1, snapshot.size)
        assertEquals(3, snapshot["RVR1960"])
    }

    @Test
    fun evictsLeastRecentlyUsedVersionWhenLimitIsReached() {
        val cache = cache(maxVersions = 2)
        val verses = listOf("1" to "a")

        cache.loadChapterHighlights("RVR1960", "{}", "Juan", 1, verses, { "Juan|1|$it" }, 0..4)
        now += 500
        cache.loadChapterHighlights("NTV", "{}", "Juan", 1, verses, { "Juan|1|$it" }, 0..4)
        now += 500
        cache.loadChapterHighlights("NVI", "{}", "Juan", 1, verses, { "Juan|1|$it" }, 0..4)

        val snapshot = cache.debugSnapshot()
        assertEquals(2, snapshot.size)
        assertTrue("RVR1960 should be evicted as LRU", !snapshot.containsKey("RVR1960"))
    }

    @Test
    fun removesEntriesAfterTtl() {
        val cache = cache(ttlMillis = 5_000L)
        val verses = listOf("1" to "a")

        cache.loadChapterHighlights("RVR1960", "{}", "Juan", 1, verses, { "Juan|1|$it" }, 0..4)
        assertEquals(1, cache.debugSnapshot().size)

        now = 6_000L
        cache.loadChapterHighlights("NTV", "{}", "Juan", 1, verses, { "Juan|1|$it" }, 0..4)

        val snapshot = cache.debugSnapshot()
        assertEquals(1, snapshot.size)
        assertTrue(snapshot.containsKey("NTV"))
    }

    @Test
    fun navigationProfilingScenarioDoesNotGrowIndefinitely() {
        val cache = cache(maxVersions = 2, maxChaptersPerVersion = 10)
        val verses = (1..30).map { it.toString() to "verse $it" }

        repeat(50) { chapterIndex ->
            val chapter = (chapterIndex % 25) + 1
            cache.loadChapterHighlights(
                versionKey = "RVR1960",
                rawHighlights = "{}",
                bookName = "Romanos",
                chapter = chapter,
                verses = verses,
                verseKeyProvider = { verse -> "Romanos|$chapter|$verse" },
                validColorIndices = 0..4
            )
            now += 250
        }

        val snapshot = cache.debugSnapshot()
        assertEquals(1, snapshot.size)
        assertEquals(10, snapshot["RVR1960"])
    }
}

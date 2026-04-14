package com.cristiancogollo.biblion.reader.cache

import org.json.JSONObject

/**
 * Caché en memoria para highlights del lector.
 *
 * - LRU por versión.
 * - LRU por capítulo dentro de cada versión.
 * - TTL corto para entradas poco usadas.
 */
class HighlightsCache(
    private val maxVersions: Int = 2,
    private val maxChaptersPerVersion: Int = 12,
    private val entryTtlMillis: Long = 2 * 60 * 1000,
    private val nowProvider: () -> Long = { System.currentTimeMillis() }
) {
    init {
        require(maxVersions > 0) { "maxVersions must be greater than 0" }
        require(maxChaptersPerVersion > 0) { "maxChaptersPerVersion must be greater than 0" }
        require(entryTtlMillis > 0) { "entryTtlMillis must be greater than 0" }
    }

    data class SaveResult(
        val updatedRaw: String,
        val updatedChapterHighlights: Map<String, Int>
    )

    private data class ChapterEntry(
        var highlights: Map<String, Int>,
        var lastAccessAt: Long
    )

    private data class VersionEntry(
        var raw: String,
        var json: JSONObject,
        var lastAccessAt: Long,
        val chapters: LinkedHashMap<String, ChapterEntry>
    )

    private val versionEntries = LinkedHashMap<String, VersionEntry>(maxVersions, 0.75f, true)

    fun loadChapterHighlights(
        versionKey: String,
        rawHighlights: String,
        bookName: String?,
        chapter: Int,
        verses: List<Pair<String, String>>,
        verseKeyProvider: (String) -> String,
        validColorIndices: IntRange
    ): Map<String, Int> {
        val now = nowProvider()
        pruneExpired(now)
        val versionEntry = getOrCreateVersionEntry(versionKey, rawHighlights, now)
        val chapterKey = chapterKey(bookName, chapter)

        versionEntry.chapters[chapterKey]?.let { cached ->
            cached.lastAccessAt = now
            versionEntry.lastAccessAt = now
            return cached.highlights
        }

        val loaded = mutableMapOf<String, Int>()
        verses.forEach { (verseNumber, _) ->
            val saved = versionEntry.json.optInt(verseKeyProvider(verseNumber), 0)
            if (saved in validColorIndices) {
                loaded[verseNumber] = saved
            }
        }

        versionEntry.chapters[chapterKey] = ChapterEntry(
            highlights = loaded,
            lastAccessAt = now
        )
        trimChapterLru(versionEntry)
        versionEntry.lastAccessAt = now
        return loaded
    }

    fun saveHighlight(
        versionKey: String,
        rawHighlights: String,
        bookName: String?,
        chapter: Int,
        verseNumber: String,
        colorIndex: Int,
        verseKeyProvider: (String) -> String,
        currentChapterHighlights: Map<String, Int>
    ): SaveResult {
        val now = nowProvider()
        pruneExpired(now)
        val versionEntry = getOrCreateVersionEntry(versionKey, rawHighlights, now)
        versionEntry.json.put(verseKeyProvider(verseNumber), colorIndex)
        val updatedRaw = versionEntry.json.toString()
        versionEntry.raw = updatedRaw
        versionEntry.lastAccessAt = now

        val chapterKey = chapterKey(bookName, chapter)
        val base = versionEntry.chapters[chapterKey]?.highlights ?: currentChapterHighlights
        val updatedChapter = base + (verseNumber to colorIndex)
        versionEntry.chapters[chapterKey] = ChapterEntry(updatedChapter, now)
        trimChapterLru(versionEntry)

        return SaveResult(updatedRaw = updatedRaw, updatedChapterHighlights = updatedChapter)
    }

    fun clearVersion(versionKey: String) {
        versionEntries.remove(versionKey)
    }

    fun clearAll() {
        versionEntries.clear()
    }

    internal fun debugSnapshot(): Map<String, Int> {
        return versionEntries.mapValues { (_, value) -> value.chapters.size }
    }

    private fun getOrCreateVersionEntry(versionKey: String, rawHighlights: String, now: Long): VersionEntry {
        val existing = versionEntries[versionKey]
        if (existing != null) {
            existing.lastAccessAt = now
            if (existing.raw != rawHighlights) {
                existing.raw = rawHighlights
                existing.json = JSONObject(rawHighlights)
                existing.chapters.clear()
            }
            trimVersionLru()
            return existing
        }

        val created = VersionEntry(
            raw = rawHighlights,
            json = JSONObject(rawHighlights),
            lastAccessAt = now,
            chapters = LinkedHashMap(maxChaptersPerVersion, 0.75f, true)
        )
        versionEntries[versionKey] = created
        trimVersionLru()
        return created
    }

    private fun pruneExpired(now: Long) {
        val versionIterator = versionEntries.entries.iterator()
        while (versionIterator.hasNext()) {
            val versionEntry = versionIterator.next().value
            if (now - versionEntry.lastAccessAt > entryTtlMillis) {
                versionIterator.remove()
                continue
            }

            val chapterIterator = versionEntry.chapters.entries.iterator()
            while (chapterIterator.hasNext()) {
                val chapterEntry = chapterIterator.next().value
                if (now - chapterEntry.lastAccessAt > entryTtlMillis) {
                    chapterIterator.remove()
                }
            }
        }
    }

    private fun trimVersionLru() {
        while (versionEntries.size > maxVersions) {
            val iterator = versionEntries.entries.iterator()
            if (iterator.hasNext()) {
                iterator.next()
                iterator.remove()
            }
        }
    }

    private fun trimChapterLru(versionEntry: VersionEntry) {
        while (versionEntry.chapters.size > maxChaptersPerVersion) {
            val iterator = versionEntry.chapters.entries.iterator()
            if (iterator.hasNext()) {
                iterator.next()
                iterator.remove()
            }
        }
    }

    private fun chapterKey(bookName: String?, chapter: Int): String = "${bookName ?: ""}|$chapter"
}

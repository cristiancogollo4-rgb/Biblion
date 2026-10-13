package com.cristiancogollo.biblion.feature.bibi.domain

import com.cristiancogollo.biblion.feature.bibi.engine.VerseResolver
import com.cristiancogollo.biblion.feature.bibi.ui.WorkerClient
import kotlinx.coroutines.CancellationException

/** Verifies structured AI references against the locally installed Bible before offering them. */
object BibiReferenceValidator {
    suspend fun validate(
        references: List<WorkerClient.WorkerReference>,
        versionKey: String,
        versesForChapter: suspend (book: String, chapter: Int, version: String) -> Set<Int>,
    ): List<WorkerClient.WorkerReference> {
        val chapterCache = mutableMapOf<Pair<String, Int>, Set<Int>>()
        return references.take(10).mapNotNull { item ->
            val resolution = VerseResolver.resolve(
                VerseResolver.ReaderSnapshot(null, 0, emptySet()),
                item.reference,
            )
            val start = resolution.verse ?: return@mapNotNull null
            val end = resolution.verseEnd ?: start
            if (resolution.source != VerseResolver.Source.EXPLICIT_IN_QUESTION ||
                end < start || end - start > 20
            ) return@mapNotNull null

            val key = resolution.book to resolution.chapter
            val availableVerses = try {
                chapterCache[key] ?: versesForChapter(
                    resolution.book, resolution.chapter, versionKey,
                ).also { chapterCache[key] = it }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                return@mapNotNull null
            }
            if (!(start..end).all { it in availableVerses }) return@mapNotNull null

            item.copy(
                reference = "${resolution.book} ${resolution.chapter}:$start" +
                    (if (end == start) "" else "-$end"),
            )
        }.distinctBy { it.reference }
    }
}

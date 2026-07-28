package com.cristiancogollo.biblion.feature.bibi.domain

import android.content.Context
import com.cristiancogollo.biblion.BibleRepository
import com.cristiancogollo.biblion.ChapterContent
import com.cristiancogollo.biblion.feature.bibi.engine.VerseResolver
import com.cristiancogollo.biblion.feature.bibi.model.BibiContext
import com.cristiancogollo.biblion.feature.bibi.model.BibiPassage

data class BibiResolvedContext(
    val context: BibiContext,
    val explicitPassages: List<BibiPassage> = emptyList(),
    val error: String? = null,
)

object BibiReferenceContextResolver {
    suspend fun resolve(
        androidContext: Context,
        question: String,
        bibiContext: BibiContext,
    ): BibiResolvedContext {
        val reader = bibiContext.asReaderContext()
        val resolution = VerseResolver.resolve(
            reader = VerseResolver.ReaderSnapshot(
                bookName = reader.book.takeIf { it.isNotBlank() },
                chapter = reader.chapter,
                selectedVerses = reader.passages.map { it.verse }.toSet(),
            ),
            question = question,
        )
        if (resolution.source != VerseResolver.Source.EXPLICIT_IN_QUESTION) {
            return BibiResolvedContext(bibiContext)
        }

        val chapter = BibleRepository.getChapter(
            context = androidContext,
            bookName = resolution.book,
            chapterNumber = resolution.chapter,
            versionKey = bibiContext.bibleVersion,
        )
        return resolveLoadedReference(bibiContext, resolution, chapter)
    }

    internal fun resolveLoadedReference(
        originalContext: BibiContext,
        resolution: VerseResolver.Resolution,
        chapter: ChapterContent,
    ): BibiResolvedContext {
        val start = resolution.verse ?: return BibiResolvedContext(originalContext)
        val end = resolution.verseEnd ?: start
        val passages = chapter.verses.mapNotNull { (number, text) ->
            val verse = number.toIntOrNull() ?: return@mapNotNull null
            if (verse !in start..end) return@mapNotNull null
            BibiPassage(resolution.book, resolution.chapter, verse, text)
        }

        if (chapter.chapterCount <= 0 || passages.size != end - start + 1) {
            val reference = if (start == end) {
                "${resolution.book} ${resolution.chapter}:$start"
            } else {
                "${resolution.book} ${resolution.chapter}:$start-$end"
            }
            return BibiResolvedContext(
                context = originalContext,
                error = "No encontré $reference en la versión seleccionada. Revisa la referencia e inténtalo de nuevo.",
            )
        }

        val resolvedContext = when (originalContext) {
            is BibiContext.Reader -> originalContext.copy(
                book = resolution.book,
                chapter = resolution.chapter,
                passages = passages,
            )
            is BibiContext.Study -> originalContext.copy(passages = passages)
        }
        return BibiResolvedContext(
            context = resolvedContext,
            explicitPassages = passages,
        )
    }

    private fun BibiContext.asReaderContext(): BibiContext.Reader = when (this) {
        is BibiContext.Reader -> this
        is BibiContext.Study -> BibiContext.Reader(
            book = passages.firstOrNull()?.book.orEmpty(),
            chapter = passages.firstOrNull()?.chapter ?: 0,
            passages = passages,
            bibleVersion = bibleVersion,
        )
    }
}

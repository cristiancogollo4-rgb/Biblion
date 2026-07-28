package com.cristiancogollo.biblion.feature.bibi.engine

/**
 * Resuelve la referencia biblica solicitada combinando la pregunta y el estado
 * actual del lector. Una referencia explicita siempre gana sobre la seleccion.
 */
object VerseResolver {
    enum class Source { SELECTION, EXPLICIT_IN_QUESTION, CHAPTER_ONLY, NONE }

    enum class Confidence { HIGH, MEDIUM, LOW }

    data class ReaderSnapshot(
        val bookName: String?,
        val chapter: Int,
        val selectedVerses: Set<Int>,
    )

    data class Resolution(
        val book: String,
        val chapter: Int,
        val verse: Int?,
        val verseEnd: Int? = verse,
        val source: Source,
        val confidence: Confidence,
    ) {
        val hasVerse: Boolean get() = verse != null
    }

    fun resolve(reader: ReaderSnapshot, question: String): Resolution {
        parseVerseFromText(question)?.let {
            return Resolution(
                book = it.book,
                chapter = it.chapter,
                verse = it.verse,
                verseEnd = it.verseEnd,
                source = Source.EXPLICIT_IN_QUESTION,
                confidence = Confidence.HIGH,
            )
        }

        if (reader.selectedVerses.isNotEmpty() && !reader.bookName.isNullOrBlank()) {
            val first = reader.selectedVerses.minOrNull() ?: 1
            return Resolution(
                book = reader.bookName,
                chapter = reader.chapter,
                verse = first,
                verseEnd = reader.selectedVerses.maxOrNull() ?: first,
                source = Source.SELECTION,
                confidence = Confidence.HIGH,
            )
        }

        if (!reader.bookName.isNullOrBlank() && reader.chapter > 0) {
            return Resolution(
                book = reader.bookName,
                chapter = reader.chapter,
                verse = null,
                source = Source.CHAPTER_ONLY,
                confidence = Confidence.LOW,
            )
        }

        return Resolution(
            book = "",
            chapter = 0,
            verse = null,
            source = Source.NONE,
            confidence = Confidence.LOW,
        )
    }

    private fun parseVerseFromText(text: String): VerseInText? {
        if (text.isBlank()) return null

        val naturalPattern = Regex(
            """(?iu)\b((?:[1-3]\s+|primera\s+de\s+|segunda\s+de\s+|tercera\s+de\s+)?[\p{L}.]+(?:\s+[\p{L}.]+){0,2})\s+(\d+):(\d+)(?:\s*[-–]\s*(\d+))?"""
        )
        for (match in naturalPattern.findAll(text)) {
            val book = resolveBookCandidate(match.groupValues[1]) ?: continue
            val chapter = match.groupValues[2].toIntOrNull() ?: continue
            val verse = match.groupValues[3].toIntOrNull() ?: continue
            val verseEnd = match.groupValues[4].toIntOrNull() ?: verse
            if (chapter > 0 && verse > 0 && verseEnd >= verse) {
                return VerseInText(book, chapter, verse, verseEnd)
            }
        }

        val osisPattern = Regex("""\b([A-Z][A-Za-z0-9]+)\.(\d+)\.(\d+)\b""")
        val osisMatch = osisPattern.find(text) ?: return null
        val book = BibleBookMapper.toSpanish(osisMatch.groupValues[1])
        val chapter = osisMatch.groupValues[2].toIntOrNull() ?: return null
        val verse = osisMatch.groupValues[3].toIntOrNull() ?: return null
        if (!isKnownBook(book) || chapter <= 0 || verse <= 0) return null
        return VerseInText(book, chapter, verse, verse)
    }

    private fun resolveBookCandidate(rawCandidate: String): String? {
        val normalizedOrdinal = rawCandidate
            .replace(Regex("(?i)^primera\\s+de\\s+"), "1 ")
            .replace(Regex("(?i)^segunda\\s+de\\s+"), "2 ")
            .replace(Regex("(?i)^tercera\\s+de\\s+"), "3 ")
        val words = normalizedOrdinal.trim().split(Regex("\\s+"))
        for (start in words.indices) {
            val book = BibleBookMapper.toSpanish(words.drop(start).joinToString(" "))
            if (isKnownBook(book)) return book
        }
        return null
    }

    private fun isKnownBook(book: String): Boolean {
        return BibleBookMapper.normalizeForDb(book) in knownBooks
    }

    private val knownBooks = setOf(
        "genesis", "exodo", "levitico", "numeros", "deuteronomio",
        "josue", "jueces", "rut", "1samuel", "2samuel",
        "1reyes", "2reyes", "1cronicas", "2cronicas", "esdras",
        "nehemias", "ester", "job", "salmos", "proverbios",
        "eclesiastes", "cantares", "isaias", "jeremias", "lamentaciones",
        "ezequiel", "daniel", "oseas", "joel", "amos",
        "abdias", "jonas", "miqueas", "nahum", "habacuc",
        "sofonias", "hageo", "zacarias", "malaquias",
        "mateo", "marcos", "lucas", "juan", "hechos",
        "romanos", "1corintios", "2corintios", "galatas", "efesios",
        "filipenses", "colosenses", "1tesalonicenses", "2tesalonicenses",
        "1timoteo", "2timoteo", "tito", "filemon", "hebreos",
        "santiago", "1pedro", "2pedro", "1juan", "2juan",
        "3juan", "judas", "apocalipsis",
    )

    private data class VerseInText(
        val book: String,
        val chapter: Int,
        val verse: Int,
        val verseEnd: Int,
    )
}

package com.cristiancogollo.biblion.feature.bibi.engine

private const val TAG = "VerseResolver"

/**
 * Resolver del versiculo activo para Bibi.
 *
 * Bibi necesita saber el versiculo exacto que el usuario esta leyendo
 * (o que el usuario escribio en su pregunta) para devolver
 * referencias precisas.
 *
 * Estrategia con dos formas en orden de prioridad:
 *
 * **Forma 1 (primaria):** Versiculo seleccionado explicitamente.
 * El usuario hizo long-press en un versiculo del lector y Bibi
 * debe usar ese versiculo.
 *
 * **Forma 2 (fallback):** Versiculo extraido del texto de la pregunta.
 * El usuario escribio "versiculos relacionados con Juan 3:16" y
 * Bibi extrae (Juan, 3, 16) del texto.
 *
 * Si ninguna aplica, Bibi responde a nivel de capitulo o pide
 * aclaracion al usuario.
 */
object VerseResolver {

    enum class Source { SELECTION, EXPLICIT_IN_QUESTION, CHAPTER_ONLY, NONE }

    enum class Confidence { HIGH, MEDIUM, LOW }

    /**
     * Snapshot inmutable del estado del lector que el resolver necesita.
     */
    data class ReaderSnapshot(
        val bookName: String?,
        val chapter: Int,
        /** Versiculos seleccionados en el lector (1, 2, 3...). Vacio si ninguno. */
        val selectedVerses: Set<Int>
    )

    /**
     * Resultado de la resolucion del versiculo activo.
     */
    data class Resolution(
        val book: String,
        val chapter: Int,
        /** Null si no se pudo determinar el versiculo exacto. */
        val verse: Int?,
        val source: Source,
        val confidence: Confidence
    ) {
        val hasVerse: Boolean get() = verse != null
    }

    /**
     * Resuelve el versiculo activo combinando Forma 1 y Forma 2.
     *
     * @param reader Estado actual del lector (libro, cap, versiculos seleccionados)
     * @param question Texto de la pregunta del usuario (para Forma 2)
     */
    fun resolve(reader: ReaderSnapshot, question: String): Resolution {
        // Forma 1: versiculo seleccionado explicitamente (mayor prioridad)
        if (reader.selectedVerses.isNotEmpty() && !reader.bookName.isNullOrBlank()) {
            val firstSelected = reader.selectedVerses.minOrNull() ?: 1
            return Resolution(
                book = reader.bookName,
                chapter = reader.chapter,
                verse = firstSelected,
                source = Source.SELECTION,
                confidence = Confidence.HIGH
            )
        }

        // Forma 2: versiculo en el texto de la pregunta
        val fromQuestion = parseVerseFromText(question)
        if (fromQuestion != null) {
            return Resolution(
                book = fromQuestion.book,
                chapter = fromQuestion.chapter,
                verse = fromQuestion.verse,
                source = Source.EXPLICIT_IN_QUESTION,
                confidence = Confidence.HIGH
            )
        }

        // Fallback: solo tenemos capitulo
        if (!reader.bookName.isNullOrBlank() && reader.chapter > 0) {
            return Resolution(
                book = reader.bookName,
                chapter = reader.chapter,
                verse = null,
                source = Source.CHAPTER_ONLY,
                confidence = Confidence.LOW
            )
        }

        return Resolution(
            book = "",
            chapter = 0,
            verse = null,
            source = Source.NONE,
            confidence = Confidence.LOW
        )
    }

    /**
     * Extrae la primera referencia biblica valida del texto.
     * Acepta formatos OSIS ("Juan.3.16") y formato natural ("Juan 3:16", "1 Juan 3:16").
     */
    private fun parseVerseFromText(text: String): VerseInText? {
        if (text.isBlank()) return null

        // Patron natural: "Libro Cap:Ver" con soporte para prefijo numerico y rangos
        val naturalPattern = Regex(
            """\b(\d\s+)?([A-ZÁÉÍÓÚÑa-záéíóúñ]{2,})\s+(\d+):(\d+)(?:-(\d+))?"""
        )
        val naturalMatch = naturalPattern.find(text)
        if (naturalMatch != null) {
            val numPrefix = naturalMatch.groupValues[1].trim()
            val bookName = naturalMatch.groupValues[2]
            val chapter = naturalMatch.groupValues[3].toIntOrNull() ?: return null
            val verse = naturalMatch.groupValues[4].toIntOrNull() ?: return null
            val fullBook = if (numPrefix.isNotEmpty()) "$numPrefix$bookName" else bookName
            val spanishBook = BibleBookMapper.toSpanish(fullBook)
            if (!isKnownBook(spanishBook)) {
                return null
            }
            return VerseInText(spanishBook, chapter, verse)
        }

        // Patron OSIS: "Juan.3.16"
        val osisPattern = Regex("""\b([A-Z][A-Za-z0-9]+)\.(\d+)\.(\d+)\b""")
        val osisMatch = osisPattern.find(text)
        if (osisMatch != null) {
            val bookAbbr = osisMatch.groupValues[1]
            val chapter = osisMatch.groupValues[2].toIntOrNull() ?: return null
            val verse = osisMatch.groupValues[3].toIntOrNull() ?: return null
            val spanishBook = BibleBookMapper.toSpanish(bookAbbr)
            if (!isKnownBook(spanishBook)) {
                return null
            }
            return VerseInText(spanishBook, chapter, verse)
        }

        return null
    }

    /**
     * Verifica si un nombre de libro (ya en espanol Biblion) es uno de los 66
     * libros canonicos. Esto evita falsos positivos cuando el regex matchea
     * palabras como "necesito 5:1" y BibleBookMapper.toSpanish devuelve el
     * input sin cambios.
     */
    private fun isKnownBook(bookEs: String): Boolean {
        val normalized = BibleBookMapper.normalizeForDb(bookEs)
        return KNOWN_BOOKS_NORMALIZED.contains(normalized)
    }

    private val KNOWN_BOOKS_NORMALIZED: Set<String> = setOf(
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
        "3juan", "judas", "apocalipsis"
    )

    private data class VerseInText(val book: String, val chapter: Int, val verse: Int)
}

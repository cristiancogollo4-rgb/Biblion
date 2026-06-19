package com.cristiancogollo.biblion.feature.bibi

import android.content.Context
import android.util.Log
import com.cristiancogollo.biblion.BibleRepository
import com.cristiancogollo.biblion.DailyVerse
import java.util.regex.Pattern

private const val TAG = "BiblicalCrossRef"

/**
 * Conector universal entre referencias biblicas y el texto real de la Biblia.
 *
 * Resuelve referencias desde cualquier fuente (diccionario Easton's, TSK,
 * Theographic, Strong's) al texto real de la version seleccionada en Biblion.
 *
 * Ejemplo:
 *   resolve("Exodo 32:11") -> "Y oró Moisés a Jehová su Dios...")
 *   resolve("Juan 1:1-3") -> versiculos 1, 2 y 3 concatenados
 *   resolve("Gen.12.1") -> OSIS format -> Genesis 12:1
 */
object BiblicalCrossReference {

    // Patron para "Libro Capitulo:Versiculo" o "Libro Capitulo:Versiculo-VersiculoFin"
    // Acepta nombres con espacios, numeros iniciales (1, 2, 3) y acentos
    private val refPattern: Pattern = Pattern.compile(
        """(?:(\d)\s+)?([A-ZÁÉÍÓÚÑa-záéíóúñ]+)\.?\s+(\d+):(\d+)(?:-(\d+))?"""
    )

    // Patron OSIS: "Gen.12.1"
    private val osisPattern: Pattern = Pattern.compile(
        """([A-Za-z0-9]+)\.(\d+)\.(\d+)"""
    )

    /**
     * Resuelve una referencia biblica al texto real del versiculo.
     * Soporta formatos:
     *   - "Genesis 1:1"
     *   - "Exodo 32:11"
     *   - "1 Reyes 2:3"
     *   - "Juan 1:1-3" (rango: concatena versiculos 1, 2 y 3)
     *   - "Gen.12.1" (OSIS)
     *
     * @param context Contexto de la app
     * @param reference Referencia biblica en cualquier formato
     * @param maxVerses Maximo de versiculos a resolver para rangos (default 5)
     * @return Versiculo resuelto con texto real, o null si no se encuentra
     */
    suspend fun resolve(
        context: Context,
        reference: String,
        maxVerses: Int = 5
    ): ResolvedVerse? {
        val cleanRef = reference.trim()
        if (cleanRef.isEmpty()) return null

        // Intentar formato OSIS primero: "Gen.12.1"
        val osisMatch = osisPattern.matcher(cleanRef)
        if (osisMatch.matches()) {
            val book = BibleBookMapper.toSpanish(osisMatch.group(1) ?: return null)
            val chapter = osisMatch.group(2)?.toIntOrNull() ?: return null
            val verse = osisMatch.group(3)?.toIntOrNull() ?: return null
            return resolveSingle(context, book, chapter, verse)
        }

        // Formato standard: "Libro Cap:Vers" o "Libro Cap:Vers-VersFin"
        val match = refPattern.matcher(cleanRef)
        if (!match.find()) {
            Log.w(TAG, "No se pudo parsear referencia: $cleanRef")
            return null
        }

        val numPrefix = match.group(1) // "1", "2", "3" o null
        val bookEn = match.group(2) ?: return null
        val chapter = match.group(3)?.toIntOrNull() ?: return null
        val verseStart = match.group(4)?.toIntOrNull() ?: return null
        val verseEnd = match.group(5)?.toIntOrNull() ?: verseStart

        val fullBook = if (numPrefix != null) "$numPrefix $bookEn" else bookEn
        val spanishBook = BibleBookMapper.toSpanish(fullBook)

        // Versiculo unico
        if (verseStart == verseEnd) {
            return resolveSingle(context, spanishBook, chapter, verseStart)
        }

        // Rango de versiculos
        return resolveRange(context, spanishBook, chapter, verseStart, verseEnd, maxVerses)
    }

    /**
     * Resuelve un versiculo unico al texto real.
     */
    private suspend fun resolveSingle(
        context: Context,
        book: String,
        chapter: Int,
        verse: Int
    ): ResolvedVerse? {
        return try {
            val versionKey = BibleRepository.getSelectedVersionKey(context)
            val result = BibleRepository.getVerseText(
                context = context,
                versionKey = versionKey,
                bookName = book,
                chapter = chapter.toString(),
                verse = verse.toString()
            )
            if (result.text.isNotEmpty()) {
                ResolvedVerse(
                    reference = "$book $chapter:$verse",
                    book = book,
                    chapter = chapter,
                    verseStart = verse,
                    verseEnd = verse,
                    text = result.text
                )
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Error resolviendo $book $chapter:$verse - ${e.message}")
            null
        }
    }

    /**
     * Resuelve un rango de versiculos, concatenando los textos.
     */
    private suspend fun resolveRange(
        context: Context,
        book: String,
        chapter: Int,
        verseStart: Int,
        verseEnd: Int,
        maxVerses: Int
    ): ResolvedVerse? {
        val safeEnd = minOf(verseEnd, verseStart + maxVerses - 1)
        val verseTexts = mutableListOf<String>()

        for (v in verseStart..safeEnd) {
            try {
                val versionKey = BibleRepository.getSelectedVersionKey(context)
                val result = BibleRepository.getVerseText(
                    context = context,
                    versionKey = versionKey,
                    bookName = book,
                    chapter = chapter.toString(),
                    verse = v.toString()
                )
                if (result.text.isNotEmpty()) {
                    verseTexts.add("${v} ${result.text}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Versiculo no encontrado: $book $chapter:$v")
            }
        }

        if (verseTexts.isEmpty()) return null

        val displayRef = if (verseEnd > safeEnd) {
            "$book $chapter:$verseStart-$safeEnd..."
        } else {
            "$book $chapter:$verseStart-$verseEnd"
        }

        return ResolvedVerse(
            reference = displayRef,
            book = book,
            chapter = chapter,
            verseStart = verseStart,
            verseEnd = safeEnd,
            text = verseTexts.joinToString(" ")
        )
    }

    /**
     * Resuelve multiples referencias separadas por "|".
     * TSK usa este formato: "Prov 8:22-24|Prov 16:4|John 1:1-3"
     *
     * @return Lista de versiculos resueltos (puede ser vacia)
     */
    suspend fun resolveMultiple(
        context: Context,
        referencesPipeSeparated: String,
        maxPerRef: Int = 3,
        maxTotal: Int = 8
    ): List<ResolvedVerse> {
        val refs = referencesPipeSeparated.split("|").map { it.trim() }.filter { it.isNotEmpty() }
        val results = mutableListOf<ResolvedVerse>()

        for (ref in refs.take(maxTotal)) {
            val resolved = resolve(context, ref, maxPerRef)
            if (resolved != null) {
                results.add(resolved)
            }
        }

        return results
    }
}

/**
 * Versiculo biblico resuelto con texto real de la version seleccionada.
 */
data class ResolvedVerse(
    val reference: String,      // "Genesis 1:1" o "Juan 1:1-3"
    val book: String,           // "Genesis"
    val chapter: Int,           // 1
    val verseStart: Int,        // 1
    val verseEnd: Int,          // 1 (o 3 si es rango)
    val text: String            // "En el principio creó Dios..."
)

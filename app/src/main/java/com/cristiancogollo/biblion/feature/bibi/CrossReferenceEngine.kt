package com.cristiancogollo.biblion.feature.bibi

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "CrossReferenceEngine"

/**
 * Fase 2: Motor de referencias cruzadas TSK.
 *
 * Cuando el usuario pregunta "¿qué otros pasajes se relacionan?" o
 * "¿qué más dice la Biblia sobre esto?", este motor busca en la base
 * de datos TSK y resuelve los versiculos cruzados al texto real.
 *
 * Sin IA: usa plantillas conversacionales en espanol.
 */
object CrossReferenceEngine {

    /**
     * Busca referencias cruzadas para un versiculo especifico.
     *
     * @param context Contexto de la app
     * @param book Nombre del libro (ej: "Genesis")
     * @param chapter Numero de capitulo
     * @param verse Numero de versiculo
     * @return Respuesta conversacional con pasajes relacionados, o null
     */
    suspend fun getRelatedPassages(
        context: Context,
        book: String,
        chapter: Int,
        verse: Int
    ): String? {
        Log.d(TAG, "Buscando referencias para $book $chapter:$verse")

        val normalizedBook = BibleBookMapper.normalizeForDb(book)
        val dao = CrossReferenceDatabase.getInstance(context).crossReferenceDao()
        val crossRefs = withContext(Dispatchers.IO) {
            dao.getCrossReferences(normalizedBook, chapter, verse)
        }

        if (crossRefs.isEmpty()) {
            Log.d(TAG, "No se encontraron referencias")
            return null
        }

        // Recopilar todas las referencias target unicas
        val allTargetRefs = mutableSetOf<String>()
        for (xref in crossRefs) {
            val refs = xref.targetReferences.split("|").map { it.trim() }
            allTargetRefs.addAll(refs)
        }

        if (allTargetRefs.isEmpty()) return null

        // Resolver versiculos al texto real (maximo 6)
        val resolvedVerses = BiblicalCrossReference.resolveMultiple(
            context = context,
            referencesPipeSeparated = allTargetRefs.take(20).joinToString("|"),
            maxPerRef = 1,
            maxTotal = 6
        )

        return formatCrossRefResponse(book, chapter, verse, resolvedVerses, allTargetRefs.size)
    }

    /**
     * Formatea la respuesta conversacional con los pasajes relacionados.
     */
    private fun formatCrossRefResponse(
        book: String,
        chapter: Int,
        verse: Int,
        resolvedVerses: List<ResolvedVerse>,
        totalRefs: Int
    ): String {
        val sb = StringBuilder()

        sb.append("Estás leyendo $book $chapter:$verse. Estos son otros pasajes ")
        sb.append("que se relacionan con lo que estás leyendo:\n\n")

        sb.append("─────────────────────────────────\n")
        sb.append("🔗 Pasajes relacionados:\n\n")

        for (verse in resolvedVerses) {
            sb.append("• ${verse.reference}: \"${truncateText(verse.text, 120)}\"\n\n")
        }

        if (totalRefs > resolvedVerses.size) {
            sb.append("Hay $totalRefs referencias cruzadas en total para este versículo. ")
            sb.append("Te mostré las más relevantes.\n\n")
        }

        sb.append("¿Quieres que profundice en alguno de estos pasajes?")
        return sb.toString()
    }

    private fun truncateText(text: String, maxLen: Int): String {
        if (text.length <= maxLen) return text
        val truncated = text.take(maxLen)
        val lastPeriod = truncated.lastIndexOf(".")
        return if (lastPeriod > maxLen * 0.6) {
            truncated.take(lastPeriod + 1)
        } else {
            "$truncated..."
        }
    }
}

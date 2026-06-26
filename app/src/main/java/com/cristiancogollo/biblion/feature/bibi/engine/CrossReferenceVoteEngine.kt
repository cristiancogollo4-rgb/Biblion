package com.cristiancogollo.biblion.feature.bibi.engine

import android.content.Context
import android.util.Log
import com.cristiancogollo.biblion.feature.bibi.data.CrossReferenceVoteDatabase
import com.cristiancogollo.biblion.feature.bibi.model.BiblicalCrossReference
import com.cristiancogollo.biblion.feature.bibi.model.RelatedVerse
import com.cristiancogollo.biblion.feature.bibi.model.ResolvedVerse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "CrossRefVoteEngine"

/**
 * Motor de referencias cruzadas basado en openbible.info (con voto crowdsourced).
 *
 * Reemplaza al antiguo CrossReferenceEngine que usaba TSK.
 *
 * Regla de UX: el campo `votes` se usa INTERNAMENTE solo para
 * ranking y filtrado. NUNCA se expone al usuario en la UI.
 *
 * Los versiculos retornados son los mejor votados que superen
 * el umbral minimo (default 10 votos = 12.6% mas confiable del dataset).
 */
object CrossReferenceVoteEngine {

    /** Umbral minimo de votos por defecto. ~13% del dataset. */
    const val DEFAULT_MIN_VOTES = 10

    /**
     * Obtiene versiculos relacionados al versiculo origen.
     * Usa el voto internamente para filtrar y ordenar.
     *
     * @param context Contexto Android
     * @param book Libro en formato Biblion ("Genesis")
     * @param chapter Numero de capitulo
     * @param verse Numero de versiculo
     * @param minVotes Umbral minimo de voto (default 10)
     * @param maxTotal Maximo de versiculos a devolver (default 6)
     * @return Lista de [RelatedVerse] ordenados por relevancia (sin votos)
     */
    suspend fun getRelatedBySource(
        context: Context,
        book: String,
        chapter: Int,
        verse: Int,
        minVotes: Int = DEFAULT_MIN_VOTES,
        maxTotal: Int = 6
    ): List<RelatedVerse> {
        val normalizedBook = BibleBookMapper.normalizeForDb(book)
        val dao = CrossReferenceVoteDatabase.getInstance(context).crossReferenceVoteDao()

        val entities = withContext(Dispatchers.IO) {
            dao.getBySource(
                normalizedBook = normalizedBook,
                chapter = chapter,
                verse = verse,
                minVotes = minVotes,
                limit = maxTotal * 2  // pedimos mas para compensar los que fallen al resolver
            )
        }

        if (entities.isEmpty()) {
            Log.d(TAG, "Sin referencias para $book $chapter:$verse (minVotes=$minVotes)")
            return emptyList()
        }

        // Resolver cada target_references a texto real
        // (puede fallar si la version biblica no tiene el versiculo)
        val results = mutableListOf<RelatedVerse>()
        for (entity in entities) {
            if (results.size >= maxTotal) break
            val resolved = BiblicalCrossReference.resolve(
                context = context,
                reference = entity.targetReferences
            )
            if (resolved != null) {
                results.add(resolved.toRelatedVerse())
            } else {
                Log.w(TAG, "No se pudo resolver '${entity.targetReferences}' para $book $chapter:$verse")
            }
        }

        Log.d(TAG, "getRelatedBySource($book $chapter:$verse): ${entities.size} candidatos, ${results.size} resueltos")
        return results
    }

    /**
     * Variante que retorna los N versiculos mejor votados sin filtrar.
     * Usar con precaucion - incluye votos de 1.
     */
    suspend fun getTopForSource(
        context: Context,
        book: String,
        chapter: Int,
        verse: Int,
        maxTotal: Int = 12
    ): List<RelatedVerse> {
        return getRelatedBySource(
            context = context,
            book = book,
            chapter = chapter,
            verse = verse,
            minVotes = 1,
            maxTotal = maxTotal
        )
    }

    /**
     * Cuenta cuantos versiculos hay para un versiculo dado.
     * Util para saber si vale la pena mostrar la seccion.
     */
    suspend fun countForSource(
        context: Context,
        book: String,
        chapter: Int,
        verse: Int,
        minVotes: Int = DEFAULT_MIN_VOTES
    ): Int {
        val normalizedBook = BibleBookMapper.normalizeForDb(book)
        val dao = CrossReferenceVoteDatabase.getInstance(context).crossReferenceVoteDao()
        return withContext(Dispatchers.IO) {
            dao.countWithMinVotes(normalizedBook, chapter, verse, minVotes)
        }
    }
}

/**
 * Extension para convertir ResolvedVerse (con metadatos) a RelatedVerse (DTO publico sin votos).
 * Esto evita que el field `votes` se filtre al UI.
 */
fun ResolvedVerse.toRelatedVerse(): RelatedVerse = RelatedVerse(
    reference = this.reference,
    book = this.book,
    chapter = this.chapter,
    verseStart = this.verseStart,
    verseEnd = this.verseEnd,
    text = this.text
)

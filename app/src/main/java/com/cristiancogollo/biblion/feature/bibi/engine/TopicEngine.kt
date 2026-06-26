package com.cristiancogollo.biblion.feature.bibi.engine

import android.content.Context
import android.util.Log
import com.cristiancogollo.biblion.feature.bibi.data.CategoryCount
import com.cristiancogollo.biblion.feature.bibi.data.TopicDatabase
import com.cristiancogollo.biblion.feature.bibi.data.TopicEntity
import com.cristiancogollo.biblion.feature.bibi.data.TopicWithAlias
import com.cristiancogollo.biblion.feature.bibi.data.TopicWithReference
import com.cristiancogollo.biblion.feature.bibi.model.BiblicalCrossReference
import com.cristiancogollo.biblion.feature.bibi.model.RelatedVerse
import com.cristiancogollo.biblion.feature.bibi.model.TopicInfo

private const val TAG = "TopicEngine"

/**
 * Motor de busqueda por temas Biblion (schema v3 con taxonomia canonica).
 *
 * Permite tres casos de uso:
 * 1. "Que versiculos hablan sobre el perdon?" - getVersesForTopic
 * 2. "De que temas habla este versiculo?" - getTopicsForVerse
 * 3. Busqueda fuzzy de temas - searchTopics
 * 4. Listado de hijos/jerarquia - getChildren
 *
 * Regla de UX: el score y qualityScore son INTERNOS. El usuario ve
 * temas relevantes y versiculos, no numeros.
 */
object TopicEngine {

    /** Umbral minimo de score por defecto para references. */
    const val DEFAULT_MIN_SCORE = 5

    /**
     * Obtiene los versiculos mejor evaluados para un topic.
     * Usado por Bibi cuando el usuario pregunta "versiculos sobre X".
     *
     * @param query Texto del topic: "perdon", "amor de dios", "idolatria"
     *              Se busca primero por slug exacto, luego por alias,
     *              luego por LIKE sobre name_es / name_en.
     */
    suspend fun getVersesForTopic(
        context: Context,
        query: String,
        minScore: Int = DEFAULT_MIN_SCORE,
        maxTotal: Int = 10
    ): List<RelatedVerse> {
        val dao = TopicDatabase.getInstance(context).topicDao()
        val results = mutableListOf<RelatedVerse>()

        // 1) Intentar match exacto por slug
        val slug = queryToSlug(query)
        val directHits = getBySlug(context, slug, minScore, maxTotal)
        if (directHits.isNotEmpty()) {
            return directHits
        }

        // 2) Buscar por nombre exacto
        val topic = dao.findByName(query)
        if (topic != null) {
            val verses = getBySlug(context, topic.slug, minScore, maxTotal)
            if (verses.isNotEmpty()) return verses
        }

        // 3) Buscar por alias exacto
        val aliasMatch = dao.findByAlias(query)
        if (aliasMatch != null) {
            val verses = getBySlug(context, aliasMatch.topicSlug, minScore, maxTotal)
            if (verses.isNotEmpty()) return verses
        }

        // 4) Fallback: fuzzy sobre aliases (excluyendo low_confidence)
        val aliasHits = dao.searchByAlias(query, maxTotal)
        val seenSlugs = mutableSetOf<String>()
        for (hit in aliasHits) {
            if (results.size >= maxTotal) break
            if (hit.topicSlug in seenSlugs) continue
            seenSlugs.add(hit.topicSlug)
            val verses = getBySlug(context, hit.topicSlug, minScore, maxTotal - results.size)
            results.addAll(verses)
        }

        if (results.isNotEmpty()) {
            Log.d(TAG, "getVersesForTopic($query): ${results.size} resultados via alias fuzzy")
            return results
        }

        // 5) Ultimo fallback: fuzzy sobre name_es/name_en
        val nameHits = dao.searchByName(query, maxTotal)
        for (hit in nameHits) {
            if (results.size >= maxTotal) break
            val verses = getBySlug(context, hit.slug, minScore, maxTotal - results.size)
            results.addAll(verses)
        }

        Log.d(TAG, "getVersesForTopic($query): ${results.size} resultados fuzzy name")
        return results
    }

    private suspend fun getBySlug(
        context: Context,
        slug: String,
        minScore: Int,
        maxTotal: Int
    ): List<RelatedVerse> {
        val dao = TopicDatabase.getInstance(context).topicDao()
        val hits = dao.getVersesForTopicSlug(slug, minScore, maxTotal)
        return hits.mapNotNull { resolveReference(context, it) }
    }

    private suspend fun resolveReference(
        context: Context,
        hit: TopicWithReference
    ): RelatedVerse? {
        val refText = if (hit.verseStart == hit.verseEnd) {
            "${hit.book} ${hit.chapter}:${hit.verseStart}"
        } else {
            "${hit.book} ${hit.chapter}:${hit.verseStart}-${hit.verseEnd}"
        }
        val resolved = BiblicalCrossReference.resolve(context, refText)
        return resolved?.toRelatedVerse()
    }

    /**
     * Obtiene los topics asociados a un versiculo especifico.
     * Usado por Bibi cuando pregunta "¿de que temas habla este versiculo?".
     */
    suspend fun getTopicsForVerse(
        context: Context,
        book: String,
        chapter: Int,
        verse: Int,
        maxTotal: Int = 8
    ): List<TopicInfo> {
        val dao = TopicDatabase.getInstance(context).topicDao()
        val entities = dao.getTopicsForVerse(book, chapter, verse, maxTotal)
        return entities.map { entity ->
            TopicInfo(
                key = entity.slug,
                displayEs = entity.nameEs,
                displayEn = entity.nameEn,
                qualityScore = entity.verseCount
            )
        }
    }

    /**
     * Busca topics por texto mediante un pipeline de 6 fases binarias.
     * Cada fase añade candidatos a la lista de resultados sin scoring;
     * el orden dentro de cada fase lo define la query SQL (verse_count DESC).
     * El pipeline se detiene cuando se alcanza `maxTotal`.
     *
     * Las fases son progresivamente mas permisivas:
     * 1. Slug exacto (max 1 candidato, ej. "fe" -> topic "fe")
     * 2. Nombre exacto ES o EN (ej. "Fe" -> topic "fe")
     * 3. Palabra completa dentro del nombre (ej. "fe" -> "Falta de fe")
     * 4. Alias exacto o palabra completa (ej. "fear" -> "temor-de-dios")
     * 5. Prefijo en alias con score >= 0.92 (ej. "fe" -> "fear of the lord")
     * 6. Prefijo de palabra en nombre (filtrado Kotlin, ej. "feli" -> Felipe)
     *
     * Esto evita el ruido de "fe" -> esposa, enfermedad, confesion, etc.
     * que el sistema de scoring anterior permitia por substring.
     */
    suspend fun searchTopics(
        context: Context,
        query: String,
        maxTotal: Int = 20
    ): List<TopicEntity> {
        val dao = TopicDatabase.getInstance(context).topicDao()
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        val word = q.lowercase()

        val results = mutableListOf<TopicEntity>()
        val seen = mutableSetOf<String>()

        fun add(topic: TopicEntity) {
            if (topic.verseCount <= 0) return
            if (topic.slug !in seen && results.size < maxTotal) {
                results.add(topic)
                seen.add(topic.slug)
            }
        }

        // Fase 1: slug exacto
        dao.findBySlug(queryToSlug(word))?.let { add(it) }
        if (results.size >= maxTotal) return results

        // Fase 2: nombre exacto
        dao.findByNameExact(q).forEach { add(it) }
        if (results.size >= maxTotal) return results

        // Fase 3: palabra completa en nombre
        dao.findByWordInName(word).forEach { add(it) }
        if (results.size >= maxTotal) return results

        // Fase 4: alias exacto o palabra completa
        dao.findByWordInAlias(word).forEach { add(it) }
        if (results.size >= maxTotal) return results

        // Fase 5: prefijo en alias (alias completo o palabra) con score alto
        dao.findByPrefixInAlias(word).forEach { add(it) }
        if (results.size >= maxTotal) return results

        // Fase 6: prefijo de palabra en nombre (filtrado en Kotlin porque
        // la logica de "primera palabra" no es trivial en SQL).
        val allTopics = dao.getTopicsPaged(limit = 1000, offset = 0)
        val prefixMatches = allTopics
            .filter { t ->
                if (t.verseCount <= 0) return@filter false
                val combined = (t.nameEs + " " + t.nameEn).lowercase()
                val words = combined.split(Regex("[^\\p{L}\\p{N}]+"))
                    .filter { it.isNotEmpty() }
                words.any { it.startsWith(word) }
            }
            .sortedByDescending { it.verseCount }
        prefixMatches.forEach { add(it) }

        return results
    }

    /**
     * Busca topics por texto y retorna DTOs con aliases.
     */
    suspend fun searchTopicsWithAliases(
        context: Context,
        query: String,
        maxTotal: Int = 20
    ): List<TopicWithAlias> {
        val dao = TopicDatabase.getInstance(context).topicDao()
        return dao.searchByAlias(query, maxTotal)
    }

    /**
     * Lista topics por categoria.
     */
    suspend fun getTopicsByCategory(
        context: Context,
        category: String,
        maxTotal: Int = 50
    ): List<TopicEntity> {
        val dao = TopicDatabase.getInstance(context).topicDao()
        return dao.getByCategory(category).take(maxTotal)
    }

    /**
     * Obtiene un topic por slug.
     */
    suspend fun getBySlug(
        context: Context,
        slug: String
    ): TopicEntity? {
        val dao = TopicDatabase.getInstance(context).topicDao()
        return dao.findBySlug(slug)
    }

    /**
     * Obtiene los hijos de un topic en la jerarquia.
     */
    suspend fun getChildren(
        context: Context,
        parentSlug: String
    ): List<TopicEntity> {
        val dao = TopicDatabase.getInstance(context).topicDao()
        return dao.getChildren(parentSlug)
    }

    /**
     * Obtiene el padre de un topic en la jerarquia.
     */
    suspend fun getParent(
        context: Context,
        childSlug: String
    ): TopicEntity? {
        val dao = TopicDatabase.getInstance(context).topicDao()
        return dao.getParent(childSlug)
    }

    /**
     * Lista todos los topics raiz (sin padre).
     */
    suspend fun getRootTopics(
        context: Context
    ): List<TopicEntity> {
        val dao = TopicDatabase.getInstance(context).topicDao()
        return dao.getRootTopics()
    }

    /**
     * Cuenta topics por categoria.
     */
    suspend fun countByCategory(
        context: Context
    ): List<CategoryCount> {
        val dao = TopicDatabase.getInstance(context).topicDao()
        return dao.countByCategory()
    }

    private fun queryToSlug(query: String): String {
        return query.lowercase()
            .replace(Regex("[^\\w\\s-]"), "")
            .replace(Regex("\\s+"), "-")
            .trim('-')
            .take(120)
    }
}

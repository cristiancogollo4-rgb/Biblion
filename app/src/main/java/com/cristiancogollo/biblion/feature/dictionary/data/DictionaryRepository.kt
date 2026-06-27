package com.cristiancogollo.biblion.feature.dictionary.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private const val TAG = "DictionaryRepository"

/**
 * Modelo de dominio para una entrada del diccionario biblico.
 * Separado de la entidad Room para mantener independencia de capa.
 * Incluye campos de Theographic para personas y lugares.
 */
data class DictionaryEntry(
    val id: Long,
    val term: String,
    val definition: String,
    val references: List<String>,
    val category: DictionaryCategory,
    // Theographic metadata (null si no aplica)
    val displayTitle: String? = null,
    val gender: String? = null,
    val birthYear: String? = null,
    val deathYear: String? = null,
    val latitude: String? = null,
    val longitude: String? = null,
    val aliases: String? = null,
    val featureType: String? = null
)

/**
 * Categoria semantica de una entrada del diccionario.
 */
enum class DictionaryCategory(val displayName: String) {
    PERSON("Persona"),
    PLACE("Lugar"),
    CONCEPT("Concepto"),
    OBJECT("Objeto"),
    PRACTICE("Practica"),
    EVENT("Evento"),
    BOOK("Libro"),
    OTHER("General")
}

/**
 * Resultado de busqueda con termino resaltado.
 */
data class DictionarySearchResultUi(
    val id: Long,
    val term: String,
    val definitionPreview: String,
    val category: DictionaryCategory,
    val rank: Double
)

/**
 * Repositorio para acceso al diccionario biblico Easton's.
 * Centraliza todas las operaciones de consulta y proporciona
 * modelos de dominio limpios para la capa de presentacion.
 */
object DictionaryRepository {

    private const val SEARCH_LIMIT = 50
    private const val PREFIX_LIMIT = 20
    private const val CATEGORY_LIMIT = 100

    /**
     * Obtiene el DAO del diccionario desde la base de datos singleton.
     */
    private fun dao(context: Context): DictionaryDao {
        return DictionaryDatabase.getInstance(context).dictionaryDao()
    }

    /**
     * Busca entradas del diccionario por termino.
     * Usa LIKE para busqueda parcial en term, definition y normalized_term.
     *
     * @param context Contexto de la aplicacion
     * @param query Termino de busqueda
     * @return Lista de resultados ordenados por relevancia
     */
    suspend fun searchEntries(
        context: Context,
        query: String
    ): List<DictionarySearchResultUi> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()

        try {
            // Normalizar la query quitando acentos para busqueda insensible a tildes
            // "oracion" o "oración" -> "oracion" -> encuentra "Oración" en normalized_term
            val normalizedQuery = normalizeTerm(query)
            val searchQuery = "%${normalizedQuery}%"

            dao(context).searchEntries(searchQuery, SEARCH_LIMIT)
                .map { result ->
                    DictionarySearchResultUi(
                        id = result.id,
                        term = result.term,
                        definitionPreview = truncateDefinition(result.definition, 150),
                        category = parseCategory(result.category),
                        rank = result.relevanceScore
                    )
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error searching entries: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Busqueda reactiva como Flow para actualizacion en tiempo real.
     */
    fun searchEntriesFlow(
        context: Context,
        query: String
    ): Flow<List<DictionarySearchResultUi>> {
        if (query.isBlank()) {
            return kotlinx.coroutines.flow.flowOf(emptyList())
        }

        val normalizedQuery = normalizeTerm(query)
        val searchQuery = "%${normalizedQuery}%"

        return dao(context).searchEntriesFlow(searchQuery, SEARCH_LIMIT)
            .map { results ->
                results.map { result ->
                    DictionarySearchResultUi(
                        id = result.id,
                        term = result.term,
                        definitionPreview = truncateDefinition(result.definition, 150),
                        category = parseCategory(result.category),
                        rank = result.relevanceScore
                    )
                }
            }
            .flowOn(Dispatchers.IO)
    }

    /**
     * Obtiene una entrada completa por su ID.
     */
    suspend fun getEntryById(
        context: Context,
        id: Long
    ): DictionaryEntry? = withContext(Dispatchers.IO) {
        dao(context).getEntryById(id)?.toDomain()
    }

    /**
     * Busca una entrada por termino exacto (normalizado).
     * Usado para lookup directo desde texto seleccionado.
     *
     * @param context Contexto de la aplicacion
     * @param term Termino a buscar (se normaliza automaticamente)
     * @return Entrada encontrada o null
     */
    suspend fun getEntryByTerm(
        context: Context,
        term: String
    ): DictionaryEntry? = withContext(Dispatchers.IO) {
        val normalized = normalizeTerm(term)
        dao(context).getEntryByNormalizedTerm(normalized)?.toDomain()
    }

    /**
     * Obtiene sugerencias de autocompletado por prefijo.
     */
    suspend fun getAutocompleteSuggestions(
        context: Context,
        prefix: String
    ): List<DictionaryEntry> = withContext(Dispatchers.IO) {
        if (prefix.length < 2) return@withContext emptyList()

        val normalizedPrefix = normalizeTerm(prefix)
        dao(context).getEntriesByPrefix(normalizedPrefix, PREFIX_LIMIT)
            .map { it.toDomain() }
    }

    /**
     * Obtiene todas las entradas que comienzan con una letra.
     * Retorna un Flow para observacion reactiva.
     */
    fun getEntriesByLetter(
        context: Context,
        letter: String
    ): Flow<List<DictionaryEntry>> {
        val normalizedLetter = letter.lowercase().take(1)

        return dao(context).getEntriesByLetterFlow(normalizedLetter)
            .map { entities ->
                entities.map { it.toDomain() }
            }
            .flowOn(Dispatchers.IO)
    }

    /**
     * Obtiene entradas por categoria semantica.
     */
    suspend fun getEntriesByCategory(
        context: Context,
        category: DictionaryCategory
    ): List<DictionaryEntry> = withContext(Dispatchers.IO) {
        try {
            dao(context).getEntriesByCategory(category.name.lowercase(), 9999)
                .map { it.toDomain() }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting entries by category: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Obtiene entradas aleatorias para descubrimiento.
     */
    suspend fun getRandomEntries(
        context: Context,
        limit: Int = 3
    ): List<DictionaryEntry> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Getting $limit random entries from dictionary")
            val entries = dao(context).getRandomEntries(limit)
            Log.d(TAG, "Retrieved ${entries.size} random entries")
            entries.map { it.toDomain() }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting random entries: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Cuenta total de entradas en el diccionario.
     */
    suspend fun getEntryCount(context: Context): Int = withContext(Dispatchers.IO) {
        try {
            dao(context).getEntryCount()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting entry count: ${e.message}", e)
            0
        }
    }

    suspend fun getCategoryCounts(context: Context): List<Pair<DictionaryCategory, Int>> =
        withContext(Dispatchers.IO) {
            try {
                dao(context).getCategoryCounts().mapNotNull { cc ->
                    val cat = DictionaryCategory.entries.find {
                        it.name.equals(cc.category, ignoreCase = true)
                    } ?: return@mapNotNull null
                    cat to cc.count
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting category counts: ${e.message}", e)
                emptyList()
            }
        }

    //region Helpers

    /**
     * Normaliza un termino para busqueda consistente.
     * Minusculas, sin acentos, sin espacios.
     */
    private fun normalizeTerm(term: String): String {
        val accents = mapOf(
            '\u00e1' to 'a', '\u00e9' to 'e', '\u00ed' to 'i',
            '\u00f3' to 'o', '\u00fa' to 'u', '\u00f1' to 'n',
            '\u00c1' to 'A', '\u00c9' to 'E', '\u00cd' to 'I',
            '\u00d3' to 'O', '\u00da' to 'U', '\u00d1' to 'N'
        )
        return term.lowercase()
            .replace(" ", "")
            .map { accents[it] ?: it }
            .joinToString("")
    }

    /**
     * Trunca la definicion para previews.
     */
    private fun truncateDefinition(definition: String, maxLength: Int): String {
        if (definition.length <= maxLength) return definition
        return definition.take(maxLength).trimEnd() + "..."
    }

    /**
     * Parsea el string de categoria a enum.
     */
    private fun parseCategory(category: String): DictionaryCategory {
        return try {
            DictionaryCategory.valueOf(category.uppercase())
        } catch (e: IllegalArgumentException) {
            DictionaryCategory.OTHER
        }
    }

    /**
     * Convierte entidad Room a modelo de dominio.
     */
    private fun DictionaryEntryEntity.toDomain(): DictionaryEntry {
        return DictionaryEntry(
            id = id,
            term = term,
            definition = definition,
            references = parseReferencesJson(referencesJson),
            category = parseCategory(category),
            displayTitle = displayTitle,
            gender = gender,
            birthYear = birthYear,
            deathYear = deathYear,
            latitude = latitude,
            longitude = longitude,
            aliases = aliases,
            featureType = featureType
        )
    }

    /**
     * Parsea JSON de referencias a lista de strings.
     */
    private fun parseReferencesJson(json: String): List<String> {
        return try {
            json.trim()
                .removeSurrounding("[", "]")
                .split(",")
                .map { it.trim().removeSurrounding("\"") }
                .filter { it.isNotBlank() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    //endregion
}

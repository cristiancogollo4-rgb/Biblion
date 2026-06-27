package com.cristiancogollo.biblion.feature.dictionary.data

import android.content.Context
import android.util.Log
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import kotlinx.coroutines.flow.Flow

private const val TAG = "DictionaryDatabase"

/**
 * Entidad Room para entradas del diccionario biblico Easton's.
 * Mapea la tabla dictionary_entries. Contiene tanto definiciones
 * de Easton's como metadata de Theographic (personas, lugares).
 */
@Entity(
    tableName = "dictionary_entries",
    indices = [
        Index(value = ["normalized_term"], name = "idx_dict_normalized"),
        Index(value = ["category"], name = "idx_dict_category"),
        Index(value = ["term"], name = "idx_dict_term")
    ]
)
data class DictionaryEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Termino principal (ej: "Abraham", "Gracia", "Jerusalem") */
    @ColumnInfo(name = "term")
    val term: String,

    /** Version normalizada del termino para busquedas consistentes */
    @ColumnInfo(name = "normalized_term")
    val normalizedTerm: String,

    /** Definicion completa de Easton's Bible Dictionary */
    @ColumnInfo(name = "definition")
    val definition: String,

    /** Referencias biblicas asociadas como JSON array */
    @ColumnInfo(name = "references_json")
    val referencesJson: String = "[]",

    /** Categoria semantica: person, place, concept, object, practice, event, other */
    @ColumnInfo(name = "category")
    val category: String = "other",

    // === Campos de Theographic Metadata (null si no aplica) ===

    /** Titulo display (ej: "Abraham (Abram)") */
    @ColumnInfo(name = "display_title")
    val displayTitle: String? = null,

    /** Genero de la persona: Male, Female */
    @ColumnInfo(name = "gender")
    val gender: String? = null,

    /** Ano de nacimiento (ISO astronomico, ej: "-2000" = 2001 a.C.) */
    @ColumnInfo(name = "birth_year")
    val birthYear: String? = null,

    /** Ano de muerte */
    @ColumnInfo(name = "death_year")
    val deathYear: String? = null,

    /** Latitud GPS del lugar */
    @ColumnInfo(name = "latitude")
    val latitude: String? = null,

    /** Longitud GPS del lugar */
    @ColumnInfo(name = "longitude")
    val longitude: String? = null,

    /** Nombres alternativos / alias (ej: "Salem, Jebus, Sion") */
    @ColumnInfo(name = "aliases")
    val aliases: String? = null,

    /** Tipo de lugar: City, Region, Mountain, River, etc. */
    @ColumnInfo(name = "feature_type")
    val featureType: String? = null
)

data class CategoryCount(
    val category: String,
    val count: Int
)

/**
 * Resultado de busqueda FTS5 con score de relevancia.
 */
data class DictionarySearchResult(
    val id: Long,
    val term: String,
    val definition: String,
    @ColumnInfo(name = "references_json")
    val referencesJson: String,
    val category: String,
    val relevanceScore: Double
)

/**
 * DAO para operaciones de consulta del diccionario biblico.
 * Incluye busqueda FTS5, lookup exacto y browsing alfabetico.
 */
@Dao
interface DictionaryDao {

    /**
     * Busqueda full-text con relevancia.
     * Ordena por coincidencia: termino exacto > prefijo > contiene > definicion.
     * @param query Termino de busqueda (ya normalizado: sin acentos, lowercase)
     * @param limit Maximo de resultados (default 50)
     */
    @Query(
        """
        SELECT e.id, e.term, e.definition, e.references_json, e.category,
            CASE 
                WHEN e.normalized_term = :query THEN 10
                WHEN e.normalized_term LIKE :query || '%' COLLATE NOCASE THEN 8
                WHEN e.normalized_term LIKE '%' || :query || '%' COLLATE NOCASE THEN 5
                WHEN e.definition LIKE '%' || :query || '%' COLLATE NOCASE THEN 2
                ELSE 1
            END as relevanceScore
        FROM dictionary_entries e
        WHERE e.normalized_term LIKE '%' || :query || '%' COLLATE NOCASE
           OR e.definition LIKE '%' || :query || '%' COLLATE NOCASE
        ORDER BY relevanceScore DESC, e.normalized_term ASC
        LIMIT :limit
        """
    )
    suspend fun searchEntries(query: String, limit: Int = 50): List<DictionarySearchResult>

    /**
     * Busqueda como Flow para observacion reactiva.
     */
    @Query(
        """
        SELECT e.id, e.term, e.definition, e.references_json, e.category,
            CASE 
                WHEN e.normalized_term = :query THEN 10
                WHEN e.normalized_term LIKE :query || '%' COLLATE NOCASE THEN 8
                WHEN e.normalized_term LIKE '%' || :query || '%' COLLATE NOCASE THEN 5
                WHEN e.definition LIKE '%' || :query || '%' COLLATE NOCASE THEN 2
                ELSE 1
            END as relevanceScore
        FROM dictionary_entries e
        WHERE e.normalized_term LIKE '%' || :query || '%' COLLATE NOCASE
           OR e.definition LIKE '%' || :query || '%' COLLATE NOCASE
        ORDER BY relevanceScore DESC, e.normalized_term ASC
        LIMIT :limit
        """
    )
    fun searchEntriesFlow(query: String, limit: Int = 50): Flow<List<DictionarySearchResult>>

    /**
     * Busqueda exacta por termino normalizado.
     * Usado para lookup directo desde terminos identificados en texto.
     */
    @Query(
        """
        SELECT * FROM dictionary_entries
        WHERE normalized_term = :normalizedTerm
        LIMIT 1
        """
    )
    suspend fun getEntryByNormalizedTerm(normalizedTerm: String): DictionaryEntryEntity?

    /**
     * Busqueda por prefijo del termino (para autocompletado).
     * @param prefix Prefijo del termino (ej: "abr" -> "Abraham", "Abra", etc.)
     */
    @Query(
        """
        SELECT * FROM dictionary_entries
        WHERE normalized_term LIKE :prefix || '%'
        ORDER BY normalized_term ASC
        LIMIT :limit
        """
    )
    suspend fun getEntriesByPrefix(prefix: String, limit: Int = 20): List<DictionaryEntryEntity>

    /**
     * Obtiene todas las entradas que comienzan con una letra especifica.
     * Usado para browsing alfabetico.
     * @param letter Letra inicial (a-z)
     */
    @Query(
        """
        SELECT * FROM dictionary_entries
        WHERE normalized_term LIKE :letter || '%'
        ORDER BY normalized_term ASC
        """
    )
    fun getEntriesByLetterFlow(letter: String): Flow<List<DictionaryEntryEntity>>

    /**
     * Obtiene una entrada por su ID.
     */
    @Query("SELECT * FROM dictionary_entries WHERE id = :id LIMIT 1")
    suspend fun getEntryById(id: Long): DictionaryEntryEntity?

    /**
     * Obtiene entradas por categoria.
     */
    @Query(
        """
        SELECT * FROM dictionary_entries
        WHERE category = :category
        ORDER BY normalized_term ASC
        LIMIT :limit
        """
    )
    suspend fun getEntriesByCategory(category: String, limit: Int = 100): List<DictionaryEntryEntity>

    /**
     * Cuenta total de entradas en el diccionario.
     */
    @Query("SELECT COUNT(*) FROM dictionary_entries")
    suspend fun getEntryCount(): Int

    @Query("SELECT category, COUNT(*) as count FROM dictionary_entries GROUP BY category")
    suspend fun getCategoryCounts(): List<CategoryCount>

    /**
     * Obtiene entradas aleatorias para el versiculo del dia o descubrimiento.
     */
    @Query(
        """
        SELECT * FROM dictionary_entries
        ORDER BY RANDOM()
        LIMIT :limit
        """
    )
    suspend fun getRandomEntries(limit: Int = 3): List<DictionaryEntryEntity>
}

/**
 * TypeConverter para listas de referencias.
 * Usa kotlinx.serialization para JSON parsing.
 */
class DictionaryTypeConverters {
    @TypeConverter
    fun fromReferencesJson(json: String): List<String> {
        return try {
            // Simple JSON array parsing without external dependencies
            json.trim()
                .removeSurrounding("[", "]")
                .split(",")
                .map { it.trim().removeSurrounding("\"") }
                .filter { it.isNotBlank() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun toReferencesJson(references: List<String>): String {
        return references.joinToString(
            prefix = "[",
            postfix = "]",
            separator = ","
        ) { "\"$it\"" }
    }
}

/**
 * Base de datos Room para el diccionario biblico Easton's.
 * Se crea desde el asset dictionary.db preempaquetado.
 */
@Database(
    entities = [DictionaryEntryEntity::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(DictionaryTypeConverters::class)
abstract class DictionaryDatabase : RoomDatabase() {

    abstract fun dictionaryDao(): DictionaryDao

    companion object {
        @Volatile
        private var instance: DictionaryDatabase? = null

        /**
         * Obtiene la instancia singleton de DictionaryDatabase.
         * Crea la base de datos desde el asset dictionary.db.
         * Si falla, intenta reconstruir la base de datos.
         */
        fun getInstance(context: android.content.Context): DictionaryDatabase {
            return instance ?: synchronized(this) {
                instance ?: try {
                    Log.d(TAG, "Creating DictionaryDatabase from asset")
                    Room.databaseBuilder(
                        context.applicationContext,
                        DictionaryDatabase::class.java,
                        "dictionary.db"
                    )
                        .createFromAsset("databases/dictionary_v2.db")
                        .fallbackToDestructiveMigration()
                        .build()
                        .also {
                            instance = it
                            Log.d(TAG, "DictionaryDatabase created successfully")
                        }
                } catch (e: Exception) {
                    Log.e(TAG, "Error creating DictionaryDatabase: ${e.message}", e)
                    // Clear corrupted instance and retry
                    instance = null
                    context.applicationContext.deleteDatabase("dictionary.db")
                    throw e
                }
            }
        }
    }
}

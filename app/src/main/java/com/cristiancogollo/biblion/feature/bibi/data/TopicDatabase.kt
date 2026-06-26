package com.cristiancogollo.biblion.feature.bibi.data

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Schema v3 de la DB de temas Biblion.
 *
 * Taxonomia canonica: 693 temas biblicos en 11 categorias (BOOK, PERSON, PLACE,
 * EVENT, ATTRIBUTE_OF_GOD, PROPHECY, COMPARATIVE_RELIGION, SIN, DOCTRINE,
 * CHURCH, CHRISTIAN_LIFE) con descripciones curadas o generadas por Bibi Worker.
 *
 * Tablas:
 *  - topics: 693 canonicos con descripcion y jerarquia
 *  - topic_aliases: 1689 aliases de OpenBible con score (0.85-0.94)
 *  - topic_references: 4704 versiculos del OSIS original
 *  - topic_relationships: 236 jerarquias parent/child
 *
 * El score (en topic_references y el campo score en topic_aliases) es INTERNO.
 * No exponer al usuario en la UI de Bibi (regla critica de UX documentada en AGENTS.md).
 *
 * Esquema v3. Reemplaza al v2 (2071 clusters openbile.info).
 */
@Entity(
    tableName = "topics",
    indices = [
        Index(value = ["slug"], name = "idx_topics_slug", unique = true),
        Index(value = ["category"], name = "idx_topics_category"),
        Index(value = ["name_es"], name = "idx_topics_name_es"),
        Index(value = ["name_en"], name = "idx_topics_name_en"),
        Index(value = ["parent_slug"], name = "idx_topics_parent")
    ]
)
data class TopicEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Slug unico del canonico: "matrimonio", "gracia", "jesucristo" */
    @ColumnInfo(name = "slug")
    val slug: String,

    /** Nombre en espanol (curado o auto-traducido) */
    @ColumnInfo(name = "name_es")
    val nameEs: String,

    /** Nombre original canonico (en) */
    @ColumnInfo(name = "name_en")
    val nameEn: String,

    /** Categoria Biblion: DOCTRINE, CHRISTIAN_LIFE, SIN, CHURCH, etc. */
    @ColumnInfo(name = "category")
    val category: String,

    /** Descripcion de 1-2 oraciones generada por Worker o manual. */
    @ColumnInfo(name = "description")
    val description: String = "",

    /** Origen: "worker" o "manual" */
    @ColumnInfo(name = "description_source")
    val descriptionSource: String = "manual",

    /** Slug del canonico padre (jerarquia) o null si es raiz */
    @ColumnInfo(name = "parent_slug")
    val parentSlug: String? = null,

    /** Cantidad de versiculos de las referencias biblicas */
    @ColumnInfo(name = "verse_count")
    val verseCount: Int = 0,

    /** Cantidad de aliases de OpenBible mapeados a este canonico */
    @ColumnInfo(name = "total_aliases")
    val totalAliases: Int = 0
)

@Entity(
    tableName = "topic_aliases",
    indices = [
        Index(value = ["topic_slug"], name = "idx_aliases_topic"),
        Index(value = ["alias_en"], name = "idx_aliases_text"),
        Index(value = ["low_confidence"], name = "idx_aliases_low_conf")
    ],
    foreignKeys = [
        ForeignKey(
            entity = TopicEntity::class,
            parentColumns = ["slug"],
            childColumns = ["topic_slug"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TopicAliasEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "topic_slug")
    val topicSlug: String,

    /** Nombre alternativo en ingles (de OpenBible) */
    @ColumnInfo(name = "alias_en")
    val aliasEn: String,

    /** Score coseno bge-small-en-v1.5 (0.85-0.94). INTERNO. */
    @ColumnInfo(name = "score")
    val score: Float,

    /** Cantidad de versiculos en OpenBible para este alias */
    @ColumnInfo(name = "verse_count")
    val verseCount: Int,

    /** Quality score maximo (de OpenBible, 2-100). INTERNO. */
    @ColumnInfo(name = "max_quality")
    val maxQuality: Int,

    /** 1 si score < 0.86 (zona de posible ruido) */
    @ColumnInfo(name = "low_confidence")
    val lowConfidence: Int = 0
)

@Entity(
    tableName = "topic_references",
    indices = [
        Index(value = ["topic_slug"], name = "idx_refs_topic"),
        Index(value = ["book"], name = "idx_refs_book"),
        Index(value = ["book", "chapter"], name = "idx_refs_ref"),
        Index(value = ["topic_slug", "score"], name = "idx_refs_topic_score")
    ],
    foreignKeys = [
        ForeignKey(
            entity = TopicEntity::class,
            parentColumns = ["slug"],
            childColumns = ["topic_slug"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TopicReferenceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "topic_slug")
    val topicSlug: String,

    /** Libro en formato Biblion: "Genesis", "Exodo", "1 Samuel" */
    @ColumnInfo(name = "book")
    val book: String,

    @ColumnInfo(name = "chapter")
    val chapter: Int,

    @ColumnInfo(name = "verse_start")
    val verseStart: Int,

    /** Igual a verse_start si es versiculo simple */
    @ColumnInfo(name = "verse_end")
    val verseEnd: Int,

    /** OSIS original: "Exod.20.1-Exod.20.26" */
    @ColumnInfo(name = "osis")
    val osis: String,

    /** Score de openbile.info (2-100). INTERNO - no exponer al usuario. */
    @ColumnInfo(name = "score")
    val score: Int
)

@Entity(
    tableName = "topic_relationships",
    indices = [
        Index(value = ["parent_slug"], name = "idx_rel_parent"),
        Index(value = ["child_slug"], name = "idx_rel_child"),
        Index(
            value = ["parent_slug", "child_slug", "relationship_type"],
            unique = true,
            name = "index_topic_relationships_parent_slug_child_slug_relationship_type"
        )
    ],
    foreignKeys = [
        ForeignKey(
            entity = TopicEntity::class,
            parentColumns = ["slug"],
            childColumns = ["parent_slug"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TopicEntity::class,
            parentColumns = ["slug"],
            childColumns = ["child_slug"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TopicRelationshipEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "parent_slug")
    val parentSlug: String,

    @ColumnInfo(name = "child_slug")
    val childSlug: String,

    /** Tipo de relacion (siempre "parent_child" en v3) */
    @ColumnInfo(name = "relationship_type")
    val relationshipType: String = "parent_child"
)

/**
 * DTO join: topic + reference combinados para queries eficientes.
 */
data class TopicWithReference(
    val topicSlug: String,
    val topicNameEs: String,
    val topicNameEn: String,
    val category: String,
    val description: String,
    val book: String,
    val chapter: Int,
    val verseStart: Int,
    val verseEnd: Int,
    val score: Int
)

/**
 * DTO con topic + info de alias.
 */
data class TopicWithAlias(
    val topicSlug: String,
    val topicNameEs: String,
    val topicNameEn: String,
    val category: String,
    val alias: String,
    val aliasScore: Float,
    val lowConfidence: Int
)

@Dao
interface TopicDao {

    /**
     * Busca un topic por slug exacto.
     */
    @Query("SELECT * FROM topics WHERE slug = :slug LIMIT 1")
    suspend fun findBySlug(slug: String): TopicEntity?

    /**
     * Busca un topic por nombre ES o EN (case-insensitive).
     */
    @Query(
        """
        SELECT * FROM topics
        WHERE LOWER(name_es) = LOWER(:name)
            OR LOWER(name_en) = LOWER(:name)
        LIMIT 1
        """
    )
    suspend fun findByName(name: String): TopicEntity?

    /**
     * Fase 2 del pipeline: nombre exacto ES o EN.
     * Retorna hasta 10 candidatos. Sin scoring, solo match binario.
     */
    @Query(
        """
        SELECT * FROM topics
        WHERE LOWER(name_es) = LOWER(:q)
            OR LOWER(name_en) = LOWER(:q)
        ORDER BY verse_count DESC
        LIMIT 10
        """
    )
    suspend fun findByNameExact(q: String): List<TopicEntity>

    /**
     * Fase 3 del pipeline: el query aparece como palabra completa
     * dentro del nombre ES o EN. Usa delimitadores de espacio para
     * evitar matches por silabas tipo "fe" en "confesion".
     */
    @Query(
        """
        SELECT * FROM topics
        WHERE ' ' || LOWER(name_es) || ' ' LIKE '% ' || LOWER(:word) || ' %'
            OR ' ' || LOWER(name_en) || ' ' LIKE '% ' || LOWER(:word) || ' %'
        ORDER BY verse_count DESC
        LIMIT 10
        """
    )
    suspend fun findByWordInName(word: String): List<TopicEntity>

    /**
     * Fase 4 del pipeline: el query es palabra completa (o alias exacto)
     * en la tabla de aliases. Solo considera aliases con
     * low_confidence = 0. Agrupa por topic para evitar duplicados.
     */
    @Query(
        """
        SELECT t.* FROM topics t
        JOIN topic_aliases a ON a.topic_slug = t.slug
        WHERE a.low_confidence = 0
            AND (
                LOWER(a.alias_en) = LOWER(:word)
                OR ' ' || LOWER(a.alias_en) || ' ' LIKE '% ' || LOWER(:word) || ' %'
            )
        GROUP BY t.slug
        ORDER BY MAX(a.score) DESC
        LIMIT 10
        """
    )
    suspend fun findByWordInAlias(word: String): List<TopicEntity>

    /**
     * Fase 5 del pipeline: el query es prefijo del alias completo
     * o prefijo de una palabra dentro del alias. Solo se aceptan
     * aliases con score >= 0.92 (alta calidad) para evitar ruido
     * tipo "fe" -> "wife" (Wife = esposa).
     */
    @Query(
        """
        SELECT t.* FROM topics t
        JOIN topic_aliases a ON a.topic_slug = t.slug
        WHERE a.low_confidence = 0
            AND a.score >= 0.92
            AND (
                LOWER(a.alias_en) LIKE LOWER(:prefix) || '%'
                OR ' ' || LOWER(a.alias_en) || ' ' LIKE '% ' || LOWER(:prefix) || '%'
            )
        GROUP BY t.slug
        ORDER BY MAX(a.score) DESC
        LIMIT 10
        """
    )
    suspend fun findByPrefixInAlias(prefix: String): List<TopicEntity>

    /**
     * Lista todos los topics por categoria, ordenados por nombre.
     */
    @Query("SELECT * FROM topics WHERE category = :category ORDER BY name_es ASC")
    suspend fun getByCategory(category: String): List<TopicEntity>

    /**
     * Lista todos los topics con paginacion.
     */
    @Query("SELECT * FROM topics ORDER BY category ASC, name_es ASC LIMIT :limit OFFSET :offset")
    suspend fun getTopicsPaged(limit: Int, offset: Int): List<TopicEntity>

    /**
     * Cuenta el total de topics.
     */
    @Query("SELECT COUNT(*) FROM topics")
    suspend fun countTopics(): Int

    /**
     * Cuenta topics por categoria.
     */
    @Query("SELECT category, COUNT(*) as count FROM topics GROUP BY category")
    suspend fun countByCategory(): List<CategoryCount>

    /**
     * Lista todos los topics con al menos un versiculo disponible,
     * ordenados por categoria y nombre. Usado por ExploreTopicsScreen.
     */
    @Query(
        """
        SELECT * FROM topics
        WHERE verse_count > 0
        ORDER BY category ASC, name_es ASC
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun getTopicsWithVerses(limit: Int, offset: Int): List<TopicEntity>

    /**
     * Lista los temas con versiculos de una categoria especifica.
     * Usado por ExploreCategoryScreen.
     */
    @Query(
        """
        SELECT * FROM topics
        WHERE verse_count > 0
            AND category = :category
        ORDER BY name_es ASC
        """
    )
    suspend fun getTopicsByCategory(category: String): List<TopicEntity>

    /**
     * Lista los temas con versiculos ordenados por cantidad de versiculos
     * descendente. Usado como pool de rotacion para el carrusel de
     * "Temas populares" en SearchScreen. Incluye los 424 temas canonicos
     * con versiculos para maxima variedad.
     */
    @Query(
        """
        SELECT * FROM topics
        WHERE verse_count > 0
        ORDER BY verse_count DESC, name_es ASC
        LIMIT :limit
        """
    )
    suspend fun getRotatableTopics(limit: Int): List<TopicEntity>

    /**
     * Busca aliases que coincidan exactamente.
     */
    @Query(
        """
        SELECT t.slug AS topicSlug, t.name_es AS topicNameEs, t.name_en AS topicNameEn,
               t.category, a.alias_en AS alias, a.score AS aliasScore, a.low_confidence AS lowConfidence
        FROM topics t
        JOIN topic_aliases a ON a.topic_slug = t.slug
        WHERE LOWER(a.alias_en) = LOWER(:alias)
        ORDER BY a.score DESC
        LIMIT 1
        """
    )
    suspend fun findByAlias(alias: String): TopicWithAlias?

    /**
     * Busca aliases fuzzy (LIKE).
     * Solo trae aliases que contengan el query como PALABRA COMPLETA
     * (delimitada por espacios) o como substring dentro de una palabra
     * corta (<= 2 * length(query)). Asi "fe" no matchea "confession"
     * (10 chars) pero si matchea "wife" (4 chars).
     * El scoring de relevancia se hace en TopicEngine.searchTopics.
     */
    @Query(
        """
        SELECT t.slug AS topicSlug, t.name_es AS topicNameEs, t.name_en AS topicNameEn,
               t.category, a.alias_en AS alias, a.score AS aliasScore, a.low_confidence AS lowConfidence
        FROM topics t
        JOIN topic_aliases a ON a.topic_slug = t.slug
        WHERE a.low_confidence = 0
            AND (
                LOWER(a.alias_en) = LOWER(:query)
                OR ' ' || LOWER(a.alias_en) || ' ' LIKE '% ' || LOWER(:query) || ' %'
                OR LOWER(a.alias_en) LIKE '%' || LOWER(:query) || '%'
                   AND length(a.alias_en) <= :maxWordLen
            )
        ORDER BY a.score DESC
        LIMIT :limit
        """
    )
    suspend fun searchByAlias(query: String, limit: Int = 20, maxWordLen: Int = 100): List<TopicWithAlias>

    /**
     * Busca topics por nombre.
     * Solo trae candidatos donde el query es palabra completa del nombre
     * o substring dentro de una palabra corta (<= 2 * length(query)).
     * El ranking de relevancia (exacto, empieza-con, palabra completa,
     * contiene) se hace en TopicEngine.searchTopics para soportar
     * scoring en memoria.
     */
    @Query(
        """
        SELECT * FROM topics
        WHERE LOWER(name_es) = LOWER(:query)
            OR LOWER(name_en) = LOWER(:query)
            OR ' ' || LOWER(name_es) || ' ' LIKE '% ' || LOWER(:query) || ' %'
            OR ' ' || LOWER(name_en) || ' ' LIKE '% ' || LOWER(:query) || ' %'
            OR (LOWER(name_es) LIKE '%' || LOWER(:query) || '%'
                AND length(name_es) <= :maxWordLen)
            OR (LOWER(name_en) LIKE '%' || LOWER(:query) || '%'
                AND length(name_en) <= :maxWordLen)
        ORDER BY verse_count DESC, total_aliases DESC
        LIMIT :limit
        """
    )
    suspend fun searchByName(query: String, limit: Int = 20, maxWordLen: Int = 100): List<TopicEntity>

    /**
     * Obtiene los versiculos mejor evaluados para un topic (por slug).
     * Ordenados por score descendente.
     */
    @Query(
        """
        SELECT t.slug AS topicSlug, t.name_es AS topicNameEs, t.name_en AS topicNameEn,
               t.category, t.description,
               r.book, r.chapter, r.verse_start AS verseStart, r.verse_end AS verseEnd, r.score
        FROM topics t
        JOIN topic_references r ON r.topic_slug = t.slug
        WHERE t.slug = :slug
            AND r.score >= :minScore
        ORDER BY r.score DESC
        LIMIT :limit
        """
    )
    suspend fun getVersesForTopicSlug(
        slug: String,
        minScore: Int = 5,
        limit: Int = 20
    ): List<TopicWithReference>

    /**
     * Obtiene los topics asociados a un versiculo.
     */
    @Query(
        """
        SELECT DISTINCT t.* FROM topics t
        JOIN topic_references r ON r.topic_slug = t.slug
        WHERE r.book = :book
            AND r.chapter = :chapter
            AND r.verse_start <= :verse
            AND r.verse_end >= :verse
        ORDER BY t.verse_count DESC
        LIMIT :limit
        """
    )
    suspend fun getTopicsForVerse(
        book: String,
        chapter: Int,
        verse: Int,
        limit: Int = 8
    ): List<TopicEntity>

    /**
     * Obtiene los hijos de un topic padre (jerarquia).
     */
    @Query(
        """
        SELECT t.* FROM topics t
        JOIN topic_relationships r ON r.child_slug = t.slug
        WHERE r.parent_slug = :parentSlug
            AND r.relationship_type = 'parent_child'
        ORDER BY t.name_es ASC
        """
    )
    suspend fun getChildren(parentSlug: String): List<TopicEntity>

    /**
     * Obtiene el padre de un topic (jerarquia inversa).
     */
    @Query(
        """
        SELECT t.* FROM topics t
        JOIN topic_relationships r ON r.parent_slug = t.slug
        WHERE r.child_slug = :childSlug
            AND r.relationship_type = 'parent_child'
        LIMIT 1
        """
    )
    suspend fun getParent(childSlug: String): TopicEntity?

    /**
     * Lista los topics raiz (sin padre).
     */
    @Query(
        """
        SELECT * FROM topics
        WHERE parent_slug IS NULL
        ORDER BY category ASC, name_es ASC
        """
    )
    suspend fun getRootTopics(): List<TopicEntity>
}

data class CategoryCount(
    val category: String,
    val count: Int
)

@Database(
    entities = [
        TopicEntity::class,
        TopicAliasEntity::class,
        TopicReferenceEntity::class,
        TopicRelationshipEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class TopicDatabase : RoomDatabase() {
    abstract fun topicDao(): TopicDao

    companion object {
        @Volatile
        private var instance: TopicDatabase? = null

        fun getInstance(context: Context): TopicDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    TopicDatabase::class.java,
                    "topics.db"
                )
                    .createFromAsset("databases/topics.db")
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
        }
    }
}

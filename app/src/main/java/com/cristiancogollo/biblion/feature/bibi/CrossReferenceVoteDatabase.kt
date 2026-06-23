package com.cristiancogollo.biblion.feature.bibi

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/**
 * Entidad para referencias cruzadas de openbible.info con voto crowdsourced.
 *
 * Cada fila representa una relacion (source -> target) votada por usuarios.
 * El voto se usa INTERNAMENTE para ranking y filtrado de calidad.
 * NUNCA debe exponerse al usuario en la UI.
 *
 * Esquema v1 - reemplaza la antigua tabla TSK con anchor/anchor_es.
 */
@Entity(
    tableName = "cross_reference_votes",
    indices = [
        Index(value = ["source_normalized_book", "source_chapter", "source_verse"], name = "idx_xref_votes_source"),
        Index(value = ["source_normalized_book", "source_chapter", "source_verse", "votes"], name = "idx_xref_votes_source_count")
    ]
)
data class CrossReferenceVoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "source_book")
    val sourceBook: String,

    @ColumnInfo(name = "source_normalized_book")
    val sourceNormalizedBook: String,

    @ColumnInfo(name = "source_chapter")
    val sourceChapter: Int,

    @ColumnInfo(name = "source_verse")
    val sourceVerse: Int,

    /** "Juan 1:1-3" o "Genesis 1:1" - formato Biblion, parseable por BiblicalCrossReference */
    @ColumnInfo(name = "target_references")
    val targetReferences: String,

    /** Voto crowdsourced de openbible.info. INTERNO - no exponer en UI. */
    @ColumnInfo(name = "votes")
    val votes: Int
)

@Dao
interface CrossReferenceVoteDao {

    /**
     * Obtiene las referencias para un versiculo, filtradas por voto minimo.
     * Ordenadas por voto descendente.
     *
     * @param minVotes Umbral minimo de calidad (default 10 en produccion)
     * @param limit Maximo de resultados a devolver
     */
    @Query(
        """
        SELECT * FROM cross_reference_votes
        WHERE source_normalized_book = :normalizedBook
            AND source_chapter = :chapter
            AND source_verse = :verse
            AND votes >= :minVotes
        ORDER BY votes DESC
        LIMIT :limit
        """
    )
    suspend fun getBySource(
        normalizedBook: String,
        chapter: Int,
        verse: Int,
        minVotes: Int = 10,
        limit: Int = 6
    ): List<CrossReferenceVoteEntity>

    /**
     * Obtiene TODAS las referencias para un versiculo (sin filtrar por voto).
     * Usado para analisis; aplicar filtro externo si es necesario.
     */
    @Query(
        """
        SELECT * FROM cross_reference_votes
        WHERE source_normalized_book = :normalizedBook
            AND source_chapter = :chapter
            AND source_verse = :verse
        ORDER BY votes DESC
        """
    )
    suspend fun getAllForSource(
        normalizedBook: String,
        chapter: Int,
        verse: Int
    ): List<CrossReferenceVoteEntity>

    /**
     * Cuenta total de referencias para un versiculo (sin filtrar).
     */
    @Query(
        """
        SELECT COUNT(*) FROM cross_reference_votes
        WHERE source_normalized_book = :normalizedBook
            AND source_chapter = :chapter
            AND source_verse = :verse
        """
    )
    suspend fun countForSource(
        normalizedBook: String,
        chapter: Int,
        verse: Int
    ): Int

    /**
     * Cuenta referencias con voto >= minVotes para un versiculo.
     */
    @Query(
        """
        SELECT COUNT(*) FROM cross_reference_votes
        WHERE source_normalized_book = :normalizedBook
            AND source_chapter = :chapter
            AND source_verse = :verse
            AND votes >= :minVotes
        """
    )
    suspend fun countWithMinVotes(
        normalizedBook: String,
        chapter: Int,
        verse: Int,
        minVotes: Int
    ): Int
}

@Database(
    entities = [CrossReferenceVoteEntity::class],
    version = 1,
    exportSchema = false
)
abstract class CrossReferenceVoteDatabase : RoomDatabase() {
    abstract fun crossReferenceVoteDao(): CrossReferenceVoteDao

    companion object {
        @Volatile
        private var instance: CrossReferenceVoteDatabase? = null

        fun getInstance(context: Context): CrossReferenceVoteDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    CrossReferenceVoteDatabase::class.java,
                    "cross_references_votes.db"
                )
                    .createFromAsset("databases/cross_references_votes.db")
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
        }
    }
}

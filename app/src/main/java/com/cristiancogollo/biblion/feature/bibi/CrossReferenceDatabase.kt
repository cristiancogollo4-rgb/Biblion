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
 * Entidad para referencias cruzadas TSK (Treasury of Scripture Knowledge).
 * Cada fila representa las referencias cruzadas ancladas a una frase de un versiculo.
 */
@Entity(
    tableName = "cross_references",
    indices = [
        Index(value = ["source_normalized_book", "source_chapter", "source_verse"], name = "idx_xref_source")
    ]
)
data class CrossReferenceEntity(
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

    @ColumnInfo(name = "anchor")
    val anchor: String = "",

    @ColumnInfo(name = "target_references")
    val targetReferences: String  // "Proverbios 8:22-24|Proverbios 16:4|Juan 1:1-3"
)

@Dao
interface CrossReferenceDao {

    /**
     * Obtiene todas las referencias cruzadas de un versiculo especifico.
     */
    @Query(
        """
        SELECT * FROM cross_references
        WHERE source_normalized_book = :normalizedBook
            AND source_chapter = :chapter
            AND source_verse = :verse
        """
    )
    suspend fun getCrossReferences(
        normalizedBook: String,
        chapter: Int,
        verse: Int
    ): List<CrossReferenceEntity>

    /**
     * Obtiene todas las referencias cruzadas de un versiculo como Flow.
     */
    @Query(
        """
        SELECT * FROM cross_references
        WHERE source_normalized_book = :normalizedBook
            AND source_chapter = :chapter
            AND source_verse = :verse
        """
    )
    fun getCrossReferencesFlow(
        normalizedBook: String,
        chapter: Int,
        verse: Int
    ): Flow<List<CrossReferenceEntity>>

    /**
     * Obtiene todas las referencias cruzadas de un capitulo completo.
     */
    @Query(
        """
        SELECT * FROM cross_references
        WHERE source_normalized_book = :normalizedBook
            AND source_chapter = :chapter
        ORDER BY source_verse ASC
        """
    )
    suspend fun getChapterCrossReferences(
        normalizedBook: String,
        chapter: Int
    ): List<CrossReferenceEntity>

    /**
     * Cuenta total de referencias cruzadas.
     */
    @Query("SELECT COUNT(*) FROM cross_references")
    suspend fun getCount(): Int
}

@Database(
    entities = [CrossReferenceEntity::class],
    version = 1,
    exportSchema = false
)
abstract class CrossReferenceDatabase : RoomDatabase() {
    abstract fun crossReferenceDao(): CrossReferenceDao

    companion object {
        @Volatile
        private var instance: CrossReferenceDatabase? = null

        fun getInstance(context: Context): CrossReferenceDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    CrossReferenceDatabase::class.java,
                    "cross_references.db"
                )
                    .createFromAsset("databases/cross_references.db")
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
        }
    }
}

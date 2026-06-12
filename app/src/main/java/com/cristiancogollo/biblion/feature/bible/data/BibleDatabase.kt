package com.cristiancogollo.biblion

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Entity(
    tableName = "bible_verses",
    indices = [
        Index(value = ["versionKey", "normalizedBookName", "chapter", "verse"], unique = true),
        Index(value = ["versionKey", "bookIndex", "chapter"]),
        Index(value = ["versionKey", "text"])
    ]
)
data class BibleVerseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val versionKey: String,
    val bookName: String,
    val normalizedBookName: String,
    val bookIndex: Int,
    val chapter: Int,
    val verse: Int,
    val text: String
)

@Entity(
    tableName = "bible_titles",
    indices = [
        Index(value = ["versionKey", "normalizedBookName", "chapter", "verse"], unique = true)
    ]
)
data class BibleTitleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val versionKey: String,
    val bookName: String,
    val normalizedBookName: String,
    val chapter: Int,
    val verse: Int,
    val title: String
)

data class BibleChapterVerseRow(
    val verse: Int,
    val text: String
)

data class BibleSearchRow(
    val bookName: String,
    val chapter: Int,
    val verse: Int,
    val text: String
)

data class BibleReferenceRow(
    val bookName: String,
    val chapter: Int,
    val verse: Int
)

@Dao
interface BibleDao {
    @Query("SELECT COUNT(*) FROM bible_verses WHERE versionKey = :versionKey")
    suspend fun getVerseCount(versionKey: String): Int

    @Query("SELECT DISTINCT versionKey FROM bible_verses ORDER BY versionKey ASC")
    suspend fun getAvailableVersionKeys(): List<String>

    @Query("DELETE FROM bible_verses WHERE versionKey = :versionKey")
    suspend fun deleteVersesForVersion(versionKey: String)

    @Query("DELETE FROM bible_titles WHERE versionKey = :versionKey")
    suspend fun deleteTitlesForVersion(versionKey: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVerses(verses: List<BibleVerseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTitles(titles: List<BibleTitleEntity>)

    @Transaction
    suspend fun replaceVersion(
        versionKey: String,
        verses: List<BibleVerseEntity>,
        titles: List<BibleTitleEntity>
    ) {
        deleteVersesForVersion(versionKey)
        deleteTitlesForVersion(versionKey)
        if (verses.isNotEmpty()) {
            insertVerses(verses)
        }
        if (titles.isNotEmpty()) {
            insertTitles(titles)
        }
    }

    @Query(
        """
        SELECT COUNT(DISTINCT chapter)
        FROM bible_verses
        WHERE versionKey = :versionKey AND normalizedBookName = :normalizedBookName
        """
    )
    suspend fun getChapterCount(versionKey: String, normalizedBookName: String): Int

    @Query(
        """
        SELECT verse, text
        FROM bible_verses
        WHERE versionKey = :versionKey
            AND normalizedBookName = :normalizedBookName
            AND chapter = :chapter
        ORDER BY verse ASC
        """
    )
    suspend fun getChapterVerses(
        versionKey: String,
        normalizedBookName: String,
        chapter: Int
    ): List<BibleChapterVerseRow>

    @Query(
        """
        SELECT verse, title
        FROM bible_titles
        WHERE versionKey = :versionKey
            AND normalizedBookName = :normalizedBookName
            AND chapter = :chapter
        ORDER BY verse ASC
        """
    )
    suspend fun getChapterTitles(
        versionKey: String,
        normalizedBookName: String,
        chapter: Int
    ): List<BibleTitleLookup>

    @Query(
        """
        SELECT bookName, chapter, verse, text
        FROM bible_verses
        WHERE versionKey = :versionKey
            AND normalizedBookName = :normalizedBookName
            AND chapter = :chapter
            AND verse = :verse
        LIMIT 1
        """
    )
    suspend fun getVerse(
        versionKey: String,
        normalizedBookName: String,
        chapter: Int,
        verse: Int
    ): BibleSearchRow?

    @Query(
        """
        SELECT bookName, chapter, verse, text
        FROM bible_verses
        WHERE versionKey = :versionKey
            AND text LIKE '%' || :query || '%' COLLATE NOCASE
        ORDER BY bookIndex ASC, chapter ASC, verse ASC
        """
    )
    suspend fun searchVerses(versionKey: String, query: String): List<BibleSearchRow>

    @Query(
        """
        SELECT bookName, chapter, verse, text
        FROM bible_verses
        WHERE versionKey = :versionKey
            AND text LIKE '%' || :query || '%' COLLATE NOCASE
            AND (:normalizedBookName IS NULL OR normalizedBookName = :normalizedBookName)
            AND (:minBookIndex IS NULL OR bookIndex >= :minBookIndex)
            AND (:maxBookIndex IS NULL OR bookIndex <= :maxBookIndex)
        ORDER BY bookIndex ASC, chapter ASC, verse ASC
        """
    )
    suspend fun searchVersesFiltered(
        versionKey: String,
        query: String,
        normalizedBookName: String?,
        minBookIndex: Int?,
        maxBookIndex: Int?
    ): List<BibleSearchRow>

    @Query(
        """
        SELECT bookName, chapter, verse
        FROM bible_verses
        WHERE versionKey = :versionKey
        ORDER BY RANDOM()
        LIMIT 1
        """
    )
    suspend fun getRandomReference(versionKey: String): BibleReferenceRow?

    @Query(
        """
        SELECT bookName, chapter, verse, text
        FROM bible_verses
        WHERE versionKey = :versionKey
        ORDER BY RANDOM()
        LIMIT 1
        """
    )
    suspend fun getRandomVerse(versionKey: String): BibleSearchRow?
}

data class BibleTitleLookup(
    val verse: Int,
    val title: String
)

@Database(
    entities = [BibleVerseEntity::class, BibleTitleEntity::class],
    version = 2,
    exportSchema = false
)
abstract class BibleDatabase : RoomDatabase() {
    abstract fun bibleDao(): BibleDao

    companion object {
        @Volatile
        private var instance: BibleDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP INDEX IF EXISTS index_bible_verses_versionKey_normalizedBookName_chapter_verse")
                database.execSQL(
                    """
                    CREATE UNIQUE INDEX index_bible_verses_versionKey_normalizedBookName_chapter_verse
                    ON bible_verses(versionKey, normalizedBookName, chapter, verse)
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): BibleDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    BibleDatabase::class.java,
                    "bible_content.db"
                )
                    .createFromAsset("databases/bible_content.db")
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
        }
    }
}

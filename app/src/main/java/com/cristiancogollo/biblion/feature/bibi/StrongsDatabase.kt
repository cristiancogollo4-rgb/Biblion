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

/**
 * Entidad para entradas del diccionario Strong's (hebreo y griego).
 */
@Entity(
    tableName = "strongs_entries",
    indices = [
        Index(value = ["language"], name = "idx_strongs_lang"),
        Index(value = ["number"], name = "idx_strongs_num")
    ]
)
data class StrongsEntryEntity(
    @PrimaryKey
    @ColumnInfo(name = "strongs_number")
    val strongsNumber: String,  // "H0001", "G0001"

    @ColumnInfo(name = "number")
    val number: Int,

    @ColumnInfo(name = "language")
    val language: String,  // "hebrew" o "greek"

    @ColumnInfo(name = "lemma")
    val lemma: String,  // palabra original en Unicode

    @ColumnInfo(name = "transliteration")
    val transliteration: String = "",

    @ColumnInfo(name = "pronunciation")
    val pronunciation: String = "",

    @ColumnInfo(name = "derivation")
    val derivation: String = "",

    @ColumnInfo(name = "definition")
    val definition: String = "",

    @ColumnInfo(name = "kjv_renderings")
    val kjvRenderings: String = ""
)

@Dao
interface StrongsDao {

    @Query("SELECT * FROM strongs_entries WHERE strongs_number = :strongsNumber LIMIT 1")
    suspend fun getByNumber(strongsNumber: String): StrongsEntryEntity?

    @Query("SELECT * FROM strongs_entries WHERE number = :number AND language = :language LIMIT 1")
    suspend fun getByNumberAndLanguage(number: Int, language: String): StrongsEntryEntity?

    @Query("SELECT * FROM strongs_entries WHERE language = :language ORDER BY number ASC LIMIT :limit OFFSET :offset")
    suspend fun getByLanguage(language: String, limit: Int, offset: Int): List<StrongsEntryEntity>

    @Query("SELECT * FROM strongs_entries WHERE transliteration LIKE '%' || :query || '%' COLLATE NOCASE OR definition LIKE '%' || :query || '%' COLLATE NOCASE LIMIT :limit")
    suspend fun search(query: String, limit: Int = 20): List<StrongsEntryEntity>

    @Query("SELECT COUNT(*) FROM strongs_entries WHERE language = 'hebrew'")
    suspend fun getHebrewCount(): Int

    @Query("SELECT COUNT(*) FROM strongs_entries WHERE language = 'greek'")
    suspend fun getGreekCount(): Int
}

@Database(
    entities = [StrongsEntryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class StrongsDatabase : RoomDatabase() {
    abstract fun strongsDao(): StrongsDao

    companion object {
        @Volatile
        private var instance: StrongsDatabase? = null

        fun getInstance(context: Context): StrongsDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    StrongsDatabase::class.java,
                    "strongs.db"
                )
                    .createFromAsset("databases/strongs.db")
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
        }
    }
}

package com.cristiancogollo.biblion.feature.bibi

import android.app.Application
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.cristiancogollo.biblion.feature.bibi.engine.CrossReferenceVoteEngine
import com.cristiancogollo.biblion.feature.bibi.engine.TopicEngine
import com.cristiancogollo.biblion.feature.bibi.model.BiblicalCrossReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Tests para CrossReferenceVoteEngine y TopicEngine usando las DBs pre-empaquetadas.
 * Usa SQLite raw para queries de verificacion (como DatabaseSchemaValidationTest).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CrossReferenceVoteEngineTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var app: Application

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        app = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── CrossReferenceVoteEngine ───────────────────────────────

    @Test
    fun `xref DB has data for Genesis 1 1`() = runBlocking {
        val db = openAssetDb("databases/cross_references_votes.db")
        try {
            val cursor = db.rawQuery(
                "SELECT COUNT(*) FROM cross_reference_votes WHERE source_normalized_book='genesis' AND source_chapter=1 AND source_verse=1",
                null
            )
            val count = if (cursor.moveToFirst()) cursor.getLong(0) else 0L
            cursor.close()
            assertTrue("cross_reference_votes must have data for Gen 1:1", count > 0)
        } finally {
            db.close()
        }
    }

    @Test
    fun `xref DB has correct schema`() = runBlocking {
        val db = openAssetDb("databases/cross_references_votes.db")
        try {
            val columns = getColumnNames(db, "cross_reference_votes")
            val required = setOf(
                "id", "source_book", "source_normalized_book",
                "source_chapter", "source_verse", "target_references", "votes"
            )
            assertTrue("Missing columns: ${required - columns}", columns.containsAll(required))
        } finally {
            db.close()
        }
    }

    @Test
    fun `xref votes are sorted descending for a source verse`() = runBlocking {
        val db = openAssetDb("databases/cross_references_votes.db")
        try {
            val cursor = db.rawQuery(
                """SELECT votes FROM cross_reference_votes
                   WHERE source_normalized_book='genesis' AND source_chapter=1 AND source_verse=1
                   ORDER BY votes DESC LIMIT 20""",
                null
            )
            val votes = mutableListOf<Int>()
            while (cursor.moveToNext()) {
                votes.add(cursor.getInt(0))
            }
            cursor.close()
            assertTrue("Should have votes for Gen 1:1", votes.isNotEmpty())
            // Verify descending order
            for (i in 1 until votes.size) {
                assertTrue(
                    "Votes should be descending: ${votes[i-1]} >= ${votes[i]}",
                    votes[i - 1] >= votes[i]
                )
            }
        } finally {
            db.close()
        }
    }

    @Test
    fun `xref target_references format is Biblion compatible`() = runBlocking {
        val db = openAssetDb("databases/cross_references_votes.db")
        try {
            val cursor = db.rawQuery(
                """SELECT target_references FROM cross_reference_votes
                   WHERE source_normalized_book='genesis' AND source_chapter=1 AND source_verse=1
                   LIMIT 5""",
                null
            )
            val refs = mutableListOf<String>()
            while (cursor.moveToNext()) {
                refs.add(cursor.getString(0))
            }
            cursor.close()
            assertTrue("Should have refs", refs.isNotEmpty())
            refs.forEach { ref ->
                // Biblion format: "Book Chapter:Verse" or "Book Chapter:Verse-Verse"
                assertTrue(
                    "Ref '$ref' should match Biblion format (contains ':' and a number)",
                    ref.contains(":") && ref.contains(Regex("\\d"))
                )
            }
        } finally {
            db.close()
        }
    }

    @Test
    fun `xref exists for John 3 16`() = runBlocking {
        val db = openAssetDb("databases/cross_references_votes.db")
        try {
            val cursor = db.rawQuery(
                "SELECT COUNT(*) FROM cross_reference_votes WHERE source_normalized_book='juan' AND source_chapter=3 AND source_verse=16",
                null
            )
            val count = if (cursor.moveToFirst()) cursor.getLong(0) else 0L
            cursor.close()
            assertTrue("cross_reference_votes should have data for John 3:16", count > 0)
        } finally {
            db.close()
        }
    }

    @Test
    fun `xref votes column exists and has values`() = runBlocking {
        val db = openAssetDb("databases/cross_references_votes.db")
        try {
            val cursor = db.rawQuery(
                "SELECT MIN(votes), MAX(votes), AVG(votes) FROM cross_reference_votes",
                null
            )
            if (cursor.moveToFirst()) {
                val min = cursor.getInt(0)
                val max = cursor.getInt(1)
                assertTrue("Min votes should be >= 1", min >= 1)
                assertTrue("Max votes should be >= min", max >= min)
            }
            cursor.close()
        } finally {
            db.close()
        }
    }

    @Test
    fun `xref total rows is reasonable for openbile dataset`() = runBlocking {
        val db = openAssetDb("databases/cross_references_votes.db")
        try {
            val cursor = db.rawQuery("SELECT COUNT(*) FROM cross_reference_votes", null)
            val count = if (cursor.moveToFirst()) cursor.getLong(0) else 0L
            cursor.close()
            assertTrue("cross_reference_votes should have > 100K rows, has $count", count > 100_000)
        } finally {
            db.close()
        }
    }

    // ── TopicEngine DB ─────────────────────────────────────────

    @Test
    fun `topics DB has correct schema v3`() = runBlocking {
        val db = openAssetDb("databases/topics.db")
        try {
            val topicsCols = getColumnNames(db, "topics")
            val required = setOf(
                "id", "slug", "name_es", "name_en", "category",
                "description", "description_source", "parent_slug",
                "verse_count", "total_aliases"
            )
            assertTrue("Missing columns in topics: ${required - topicsCols}", topicsCols.containsAll(required))

            val aliasCols = getColumnNames(db, "topic_aliases")
            val aliasRequired = setOf("id", "topic_slug", "alias_en", "score", "verse_count", "max_quality", "low_confidence")
            assertTrue("Missing columns in topic_aliases: ${aliasRequired - aliasCols}", aliasCols.containsAll(aliasRequired))

            val refCols = getColumnNames(db, "topic_references")
            val refRequired = setOf("id", "topic_slug", "book", "chapter", "verse_start", "verse_end", "osis", "score")
            assertTrue("Missing columns in topic_references: ${refRequired - refCols}", refCols.containsAll(refRequired))
        } finally {
            db.close()
        }
    }

    @Test
    fun `topics has 693 canonical topics`() = runBlocking {
        val db = openAssetDb("databases/topics.db")
        try {
            val cursor = db.rawQuery("SELECT COUNT(*) FROM topics", null)
            val count = if (cursor.moveToFirst()) cursor.getLong(0) else 0L
            cursor.close()
            assertEquals("topics should have 693 canonicals", 693L, count)
        } finally {
            db.close()
        }
    }

    @Test
    fun `topics has 424 topics with verses`() = runBlocking {
        val db = openAssetDb("databases/topics.db")
        try {
            val cursor = db.rawQuery("SELECT COUNT(*) FROM topics WHERE verse_count > 0", null)
            val count = if (cursor.moveToFirst()) cursor.getLong(0) else 0L
            cursor.close()
            assertTrue("Should have ~424 topics with verses, has $count", count in 400..450)
        } finally {
            db.close()
        }
    }

    @Test
    fun `topics references exist for Genesis 1 1`() = runBlocking {
        val db = openAssetDb("databases/topics.db")
        try {
            val cursor = db.rawQuery(
                """SELECT t.slug, t.name_es FROM topics t
                   JOIN topic_references r ON r.topic_slug = t.slug
                   WHERE r.book = 'Genesis' AND r.chapter = 1 AND r.verse_start <= 1 AND r.verse_end >= 1
                   LIMIT 5""",
                null
            )
            val topics = mutableListOf<String>()
            while (cursor.moveToNext()) {
                topics.add(cursor.getString(1))
            }
            cursor.close()
            assertTrue("Genesis 1:1 should have associated topics", topics.isNotEmpty())
        } finally {
            db.close()
        }
    }

    @Test
    fun `topics has parent-child relationships`() = runBlocking {
        val db = openAssetDb("databases/topics.db")
        try {
            val cursor = db.rawQuery(
                "SELECT COUNT(*) FROM topic_relationships WHERE relationship_type = 'parent_child'",
                null
            )
            val count = if (cursor.moveToFirst()) cursor.getLong(0) else 0L
            cursor.close()
            assertTrue("Should have parent-child relationships, has $count", count > 100)
        } finally {
            db.close()
        }
    }

    @Test
    fun `topic_aliases has aliases with good scores`() = runBlocking {
        val db = openAssetDb("databases/topics.db")
        try {
            val cursor = db.rawQuery(
                "SELECT COUNT(*) FROM topic_aliases WHERE score >= 0.92 AND low_confidence = 0",
                null
            )
            val count = if (cursor.moveToFirst()) cursor.getLong(0) else 0L
            cursor.close()
            assertTrue("Should have high-quality aliases, has $count", count > 100)
        } finally {
            db.close()
        }
    }

    @Test
    fun `topic_references has data for multiple books`() = runBlocking {
        val db = openAssetDb("databases/topics.db")
        try {
            val cursor = db.rawQuery(
                "SELECT DISTINCT book FROM topic_references LIMIT 20",
                null
            )
            val books = mutableSetOf<String>()
            while (cursor.moveToNext()) {
                books.add(cursor.getString(0))
            }
            cursor.close()
            assertTrue("Should have references from multiple books, has ${books.size}", books.size >= 10)
        } finally {
            db.close()
        }
    }

    // ── Helpers ────────────────────────────────────────────────

    private fun openAssetDb(assetPath: String): SQLiteDatabase {
        val context = app
        val targetFile = File(context.filesDir, "test_${assetPath.replace("/", "_")}")
        if (!targetFile.exists()) {
            val inputStream = try {
                context.assets.open(assetPath)
            } catch (e: Exception) {
                val workingDir = File(".").absolutePath
                val candidates = listOf(
                    File("$workingDir/src/main/assets/$assetPath"),
                    File("$workingDir/../src/main/assets/$assetPath"),
                    File("src/main/assets/$assetPath")
                )
                val found = candidates.firstOrNull { it.exists() }
                    ?: throw IllegalStateException("Cannot find $assetPath")
                java.io.FileInputStream(found)
            }
            inputStream.use { input ->
                targetFile.outputStream().use { output -> input.copyTo(output) }
            }
        }
        return SQLiteDatabase.openDatabase(
            targetFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY
        )
    }

    private fun getColumnNames(db: SQLiteDatabase, table: String): Set<String> {
        val cursor = db.rawQuery("PRAGMA table_info($table)", null)
        return buildSet {
            cursor.use {
                val nameIdx = it.getColumnIndexOrThrow("name")
                while (it.moveToNext()) {
                    add(it.getString(nameIdx))
                }
            }
        }
    }
}

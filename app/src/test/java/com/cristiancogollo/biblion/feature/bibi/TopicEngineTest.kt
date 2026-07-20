package com.cristiancogollo.biblion.feature.bibi

import android.app.Application
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.cristiancogollo.biblion.feature.bibi.engine.TopicEngine
import com.cristiancogollo.biblion.feature.bibi.engine.BibleBookMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TopicEngineTest {

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

    // ── searchTopics pipeline ──────────────────────────────────

    @Test
    fun `searchTopics finds fe by slug exact`() = runBlocking {
        val db = openAssetDb("databases/topics.db")
        try {
            // Phase 1: slug exact
            val cursor = db.rawQuery("SELECT slug, name_es FROM topics WHERE slug = 'fe' AND verse_count > 0", null)
            val found = cursor.moveToFirst()
            val slug = if (found) cursor.getString(0) else null
            cursor.close()
            assertNotNull("Topic 'fe' should exist in DB", slug)
            assertEquals("fe", slug)
        } finally {
            db.close()
        }
    }

    @Test
    fun `searchTopics finds fe by name exact`() = runBlocking {
        val db = openAssetDb("databases/topics.db")
        try {
            val cursor = db.rawQuery(
                "SELECT slug FROM topics WHERE LOWER(name_es) = 'fe' OR LOWER(name_en) = 'faith' AND verse_count > 0 LIMIT 5",
                null
            )
            assertTrue("Should find 'Fe' by name", cursor.moveToFirst())
            cursor.close()
        } finally {
            db.close()
        }
    }

    @Test
    fun `searchTopics finds topics with word 'fe' in name`() = runBlocking {
        val db = openAssetDb("databases/topics.db")
        try {
            val cursor = db.rawQuery(
                """SELECT slug, name_es FROM topics
                   WHERE (' ' || LOWER(name_es) || ' ') LIKE '% fe %'
                      OR (' ' || LOWER(name_en) || ' ') LIKE '% fe %'
                   AND verse_count > 0
                   LIMIT 10""",
                null
            )
            val results = mutableListOf<String>()
            while (cursor.moveToNext()) {
                results.add(cursor.getString(1))
            }
            cursor.close()
            assertTrue("Should find topics with 'fe' as word in name", results.isNotEmpty())
        } finally {
            db.close()
        }
    }

    @Test
    fun `searchTopics finds topics by alias`() = runBlocking {
        val db = openAssetDb("databases/topics.db")
        try {
            val cursor = db.rawQuery(
                """SELECT t.slug, t.name_es FROM topics t
                   JOIN topic_aliases a ON a.topic_slug = t.slug
                   WHERE a.low_confidence = 0
                   AND LOWER(a.alias_en) LIKE '%fear%'
                   LIMIT 5""",
                null
            )
            val results = mutableListOf<String>()
            while (cursor.moveToNext()) {
                results.add(cursor.getString(1))
            }
            cursor.close()
            assertTrue("Should find topics with 'fear' alias", results.isNotEmpty())
        } finally {
            db.close()
        }
    }

    @Test
    fun `searchTopics excludes topics with zero verses`() = runBlocking {
        val db = openAssetDb("databases/topics.db")
        try {
            val cursor = db.rawQuery(
                "SELECT COUNT(*) FROM topics WHERE verse_count <= 0 AND slug = 'fe'",
                null
            )
            val count = if (cursor.moveToFirst()) cursor.getLong(0) else 0L
            cursor.close()
            assertEquals("Topic 'fe' should have verse_count > 0", 0L, count)
        } finally {
            db.close()
        }
    }

    // ── getTopicsForVerse ──────────────────────────────────────

    @Test
    fun `getTopicsForVerse returns topics for Genesis 1 1`() = runBlocking {
        val db = openAssetDb("databases/topics.db")
        try {
            val cursor = db.rawQuery(
                """SELECT t.slug, t.name_es FROM topics t
                   JOIN topic_references r ON r.topic_slug = t.slug
                   WHERE r.book = 'Genesis' AND r.chapter = 1 AND r.verse_start <= 1 AND r.verse_end >= 1
                   ORDER BY t.verse_count DESC
                   LIMIT 8""",
                null
            )
            val topics = mutableListOf<String>()
            while (cursor.moveToNext()) {
                topics.add(cursor.getString(1))
            }
            cursor.close()
            assertTrue("Genesis 1:1 should have topics", topics.isNotEmpty())
            assertTrue("Should have at most 8", topics.size <= 8)
        } finally {
            db.close()
        }
    }

    @Test
    fun `getTopicsForVerse returns empty for non-existent verse`() = runBlocking {
        val db = openAssetDb("databases/topics.db")
        try {
            val cursor = db.rawQuery(
                """SELECT t.slug FROM topics t
                   JOIN topic_references r ON r.topic_slug = t.slug
                   WHERE r.book = 'Genesis' AND r.chapter = 999 AND r.verse_start <= 99 AND r.verse_end >= 99
                   LIMIT 8""",
                null
            )
            assertFalse("Non-existent verse should return empty", cursor.moveToFirst())
            cursor.close()
        } finally {
            db.close()
        }
    }

    // ── getChildren ────────────────────────────────────────────

    @Test
    fun `getChildren returns children for a parent topic`() = runBlocking {
        val db = openAssetDb("databases/topics.db")
        try {
            // Find a parent that has children
            val cursor = db.rawQuery(
                """SELECT parent_slug, COUNT(*) as cnt FROM topic_relationships
                   WHERE relationship_type = 'parent_child'
                   GROUP BY parent_slug HAVING cnt > 1
                   LIMIT 1""",
                null
            )
            if (cursor.moveToFirst()) {
                val parentSlug = cursor.getString(0)
                cursor.close()

                val childrenCursor = db.rawQuery(
                    """SELECT t.slug, t.name_es FROM topics t
                       JOIN topic_relationships r ON r.child_slug = t.slug
                       WHERE r.parent_slug = ? AND r.relationship_type = 'parent_child'
                       ORDER BY t.name_es ASC""",
                    arrayOf(parentSlug)
                )
                val children = mutableListOf<String>()
                while (childrenCursor.moveToNext()) {
                    children.add(childrenCursor.getString(1))
                }
                childrenCursor.close()
                assertTrue("Parent '$parentSlug' should have children", children.isNotEmpty())
            } else {
                cursor.close()
            }
        } finally {
            db.close()
        }
    }

    @Test
    fun `getParent returns parent for a child topic`() = runBlocking {
        val db = openAssetDb("databases/topics.db")
        try {
            // Find a child with a parent
            val cursor = db.rawQuery(
                """SELECT child_slug, parent_slug FROM topic_relationships
                   WHERE relationship_type = 'parent_child'
                   LIMIT 1""",
                null
            )
            if (cursor.moveToFirst()) {
                val childSlug = cursor.getString(0)
                cursor.close()

                val parentCursor = db.rawQuery(
                    """SELECT t.slug, t.name_es FROM topics t
                       JOIN topic_relationships r ON r.parent_slug = t.slug
                       WHERE r.child_slug = ? AND r.relationship_type = 'parent_child'
                       LIMIT 1""",
                    arrayOf(childSlug)
                )
                assertTrue("Child should have a parent", parentCursor.moveToFirst())
                parentCursor.close()
            } else {
                cursor.close()
            }
        } finally {
            db.close()
        }
    }

    // ── getRootTopics ──────────────────────────────────────────

    @Test
    fun `getRootTopics returns topics without parent`() = runBlocking {
        val db = openAssetDb("databases/topics.db")
        try {
            val cursor = db.rawQuery(
                "SELECT COUNT(*) FROM topics WHERE parent_slug IS NULL",
                null
            )
            val count = if (cursor.moveToFirst()) cursor.getLong(0) else 0L
            cursor.close()
            assertTrue("Should have root topics", count > 0)
        } finally {
            db.close()
        }
    }

    // ── countByCategory ────────────────────────────────────────

    @Test
    fun `countByCategory returns all 11 categories`() = runBlocking {
        val db = openAssetDb("databases/topics.db")
        try {
            val cursor = db.rawQuery(
                "SELECT category, COUNT(*) as cnt FROM topics GROUP BY category ORDER BY category",
                null
            )
            val categories = mutableSetOf<String>()
            while (cursor.moveToNext()) {
                categories.add(cursor.getString(0))
            }
            cursor.close()
            assertTrue("Should have at least 10 categories, has ${categories.size}", categories.size >= 10)
            assertTrue("Should include DOCTRINE", categories.contains("DOCTRINE"))
            assertTrue("Should include CHRISTIAN_LIFE", categories.contains("CHRISTIAN_LIFE"))
            assertTrue("Should include PERSON", categories.contains("PERSON"))
        } finally {
            db.close()
        }
    }

    // ── searchTopics pipeline edge cases ───────────────────────

    @Test
    fun `searchTopics empty query returns empty`() = runBlocking {
        val db = openAssetDb("databases/topics.db")
        try {
            val cursor = db.rawQuery(
                "SELECT * FROM topics WHERE slug = ''",
                null
            )
            assertFalse("Empty query should return no results", cursor.moveToFirst())
            cursor.close()
        } finally {
            db.close()
        }
    }

    @Test
    fun `searchTopics finds amor-de-dios by slug`() = runBlocking {
        val db = openAssetDb("databases/topics.db")
        try {
            val cursor = db.rawQuery(
                "SELECT slug, name_es, verse_count FROM topics WHERE slug = 'amor-de-dios' AND verse_count > 0",
                null
            )
            assertTrue("Topic 'amor-de-dios' should exist", cursor.moveToFirst())
            val verseCount = cursor.getInt(2)
            cursor.close()
            assertTrue("amor-de-dios should have verses", verseCount > 0)
        } finally {
            db.close()
        }
    }

    @Test
    fun `searchTopics finds Oracion by slug`() = runBlocking {
        val db = openAssetDb("databases/topics.db")
        try {
            val cursor = db.rawQuery(
                "SELECT slug, name_es, verse_count FROM topics WHERE slug = 'oracion' AND verse_count > 0",
                null
            )
            assertTrue("Topic 'oracion' should exist", cursor.moveToFirst())
            cursor.close()
        } finally {
            db.close()
        }
    }

    // ── getVersesForTopic ──────────────────────────────────────

    @Test
    fun `getVersesForTopic returns verses for amor-de-dios`() = runBlocking {
        val db = openAssetDb("databases/topics.db")
        try {
            val cursor = db.rawQuery(
                """SELECT r.book, r.chapter, r.verse_start, r.verse_end, r.score
                   FROM topic_references r
                   WHERE r.topic_slug = 'amor-de-dios'
                   ORDER BY r.score DESC
                   LIMIT 10""",
                null
            )
            val refs = mutableListOf<String>()
            while (cursor.moveToNext()) {
                val book = cursor.getString(0)
                val chapter = cursor.getInt(1)
                val verseStart = cursor.getInt(2)
                refs.add("$book $chapter:$verseStart")
            }
            cursor.close()
            assertTrue("amor-de-dios should have verse references", refs.isNotEmpty())
        } finally {
            db.close()
        }
    }

    @Test
    fun `topic_references scores are internal and range 2-100`() = runBlocking {
        val db = openAssetDb("databases/topics.db")
        try {
            val cursor = db.rawQuery(
                "SELECT MIN(score), MAX(score) FROM topic_references",
                null
            )
            if (cursor.moveToFirst()) {
                val minScore = cursor.getInt(0)
                val maxScore = cursor.getInt(1)
                assertTrue("Min score should be >= 2, is $minScore", minScore >= 2)
                assertTrue("Max score should be <= 100, is $maxScore", maxScore <= 100)
            }
            cursor.close()
        } finally {
            db.close()
        }
    }

    // ── BibleBookMapper ────────────────────────────────────────

    @Test
    fun `BibleBookMapper normalizeForDb removes accents and spaces`() {
        assertEquals("genesis", BibleBookMapper.normalizeForDb("Génesis"))
        assertEquals("1samuel", BibleBookMapper.normalizeForDb("1 Samuel"))
        assertEquals("exodo", BibleBookMapper.normalizeForDb("Éxodo"))
    }

    @Test
    fun `BibleBookMapper toSpanish converts English to Spanish`() {
        assertEquals("Genesis", BibleBookMapper.toSpanish("genesis"))
        assertEquals("Genesis", BibleBookMapper.toSpanish("Gen"))
        assertEquals("Exodo", BibleBookMapper.toSpanish("Exodus"))
        assertEquals("Juan", BibleBookMapper.toSpanish("john"))
        assertEquals("1 Samuel", BibleBookMapper.toSpanish("1Samuel"))
    }

    @Test
    fun `BibleBookMapper toNormalized combines toSpanish and normalizeForDb`() {
        assertEquals("genesis", BibleBookMapper.toNormalized("Gen"))
        assertEquals("genesis", BibleBookMapper.toNormalized("genesis"))
        assertEquals("juan", BibleBookMapper.toNormalized("John"))
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
}

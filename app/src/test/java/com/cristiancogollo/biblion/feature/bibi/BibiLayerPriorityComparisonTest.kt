package com.cristiancogollo.biblion.feature.bibi

import android.app.Application
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Compara el flujo actual de Bibi con el flujo propuesto para priorizar capas.
 *
 * Esta suite usa SQLite directo sobre los assets preempaquetados para evitar
 * depender de Room en el entorno de test y para que la comparación sea
 * estable y reproducible.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class BibiLayerPriorityComparisonTest {

    private val app: Application = ApplicationProvider.getApplicationContext()

    @Test
    fun `current flow uses topic layer for define gracia`() {
        val comparison = compareQuestion(
            intent = Intent.DEFINE,
            topicSlug = "gracia",
            dictionaryTerm = "gracia"
        )

        assertNotNull(comparison.topic)
        assertNotNull(comparison.dictionary)
        assertEquals(Layer.TOPIC, comparison.current)
        assertEquals(Layer.DICTIONARY, comparison.proposed)
    }

    @Test
    fun `current flow uses topic layer for who abraham`() {
        val comparison = compareQuestion(
            intent = Intent.WHO,
            topicSlug = "abraham",
            dictionaryTerm = "abraham"
        )

        assertNotNull(comparison.topic)
        assertNotNull(comparison.dictionary)
        assertEquals(Layer.TOPIC, comparison.current)
        assertEquals(Layer.DICTIONARY, comparison.proposed)
    }

    @Test
    fun `current flow uses topic layer for where jerusalen`() {
        val comparison = compareQuestion(
            intent = Intent.WHERE,
            topicSlug = "jerusalen",
            dictionaryTerm = "jerusalen"
        )

        assertNotNull(comparison.topic)
        assertNotNull(comparison.dictionary)
        assertEquals(Layer.TOPIC, comparison.current)
        assertEquals(Layer.DICTIONARY, comparison.proposed)
    }

    @Test
    fun `current and proposed agree on topics intent for Genesis 1 1`() {
        val comparison = compareQuestion(
            intent = Intent.TOPICS,
            topicSlug = "genesis",
            dictionaryTerm = "genesis"
        )

        assertNotNull(comparison.topic)
        assertNotNull(comparison.dictionary)
        assertEquals(Layer.TOPIC, comparison.current)
        assertEquals(Layer.TOPIC, comparison.proposed)
    }

    @Test
    fun `comparison reveals the current priority gap for define who and where`() {
        val comparisons = listOf(
            compareQuestion(Intent.DEFINE, "gracia", "gracia"),
            compareQuestion(Intent.WHO, "abraham", "abraham"),
            compareQuestion(Intent.WHERE, "jerusalen", "jerusalen")
        )

        assertTrue("At least one comparison must differ", comparisons.any { it.current != it.proposed })
        comparisons.forEach { comparison ->
            assertEquals(Layer.TOPIC, comparison.current)
            assertEquals(Layer.DICTIONARY, comparison.proposed)
        }
    }

    private data class Comparison(
        val topic: TopicRow?,
        val dictionary: DictionaryRow?,
        val current: Layer,
        val proposed: Layer
    )

    private data class TopicRow(
        val slug: String,
        val nameEs: String,
        val description: String,
        val verseCount: Int
    )

    private data class DictionaryRow(
        val term: String,
        val definition: String,
        val category: String
    )

    private enum class Intent {
        DEFINE,
        WHO,
        WHERE,
        TOPICS
    }

    private enum class Layer {
        TOPIC,
        DICTIONARY,
        NONE
    }

    private fun compareQuestion(
        intent: Intent,
        topicSlug: String,
        dictionaryTerm: String
    ): Comparison {
        val topic = loadTopic(topicSlug)
        val dictionary = loadDictionary(dictionaryTerm)

        assertNotNull("Expected topic for $topicSlug", topic)
        assertNotNull("Expected dictionary entry for $dictionaryTerm", dictionary)

        return Comparison(
            topic = topic,
            dictionary = dictionary,
            current = currentPrimaryLayer(intent, topic != null, dictionary != null),
            proposed = proposedPrimaryLayer(intent, topic != null, dictionary != null, dictionary)
        )
    }

    private fun currentPrimaryLayer(
        intent: Intent,
        hasTopic: Boolean,
        hasDictionary: Boolean
    ): Layer {
        return when (intent) {
            Intent.DEFINE,
            Intent.WHO,
            Intent.WHERE -> when {
                hasTopic -> Layer.TOPIC
                hasDictionary -> Layer.DICTIONARY
                else -> Layer.NONE
            }

            Intent.TOPICS -> if (hasTopic) Layer.TOPIC else Layer.NONE
        }
    }

    private fun proposedPrimaryLayer(
        intent: Intent,
        hasTopic: Boolean,
        hasDictionary: Boolean,
        dictionary: DictionaryRow?
    ): Layer {
        return when (intent) {
            Intent.DEFINE -> if (hasDictionary) Layer.DICTIONARY else if (hasTopic) Layer.TOPIC else Layer.NONE
            Intent.WHO -> if (dictionary?.category == "person") Layer.DICTIONARY else if (hasTopic) Layer.TOPIC else Layer.NONE
            Intent.WHERE -> if (dictionary?.category == "place") Layer.DICTIONARY else if (hasTopic) Layer.TOPIC else Layer.NONE
            Intent.TOPICS -> if (hasTopic) Layer.TOPIC else Layer.NONE
        }
    }

    private fun loadTopic(slugOrName: String): TopicRow? {
        val db = openAssetDb("databases/topics.db")
        return try {
            val cursor = db.rawQuery(
                """
                SELECT slug, name_es, description, verse_count
                FROM topics
                WHERE slug = ? OR lower(name_es) = ? OR lower(name_en) = ?
                LIMIT 1
                """.trimIndent(),
                arrayOf(slugOrName, slugOrName, slugOrName)
            )
            cursor.use {
                if (!it.moveToFirst()) return null
                TopicRow(
                    slug = it.getString(0),
                    nameEs = it.getString(1),
                    description = it.getString(2),
                    verseCount = it.getInt(3)
                )
            }
        } finally {
            db.close()
        }
    }

    private fun loadDictionary(term: String): DictionaryRow? {
        val db = openAssetDb("databases/dictionary_v2.db")
        return try {
            val cursor = db.rawQuery(
                """
                SELECT term, definition, category
                FROM dictionary_entries
                WHERE normalized_term = ?
                LIMIT 1
                """.trimIndent(),
                arrayOf(term)
            )
            cursor.use {
                if (!it.moveToFirst()) return null
                DictionaryRow(
                    term = it.getString(0),
                    definition = it.getString(1),
                    category = it.getString(2)
                )
            }
        } finally {
            db.close()
        }
    }

    private fun openAssetDb(assetPath: String): SQLiteDatabase {
        val context = app
        val targetFile = File(context.filesDir, "test_${assetPath.replace("/", "_")}")
        if (!targetFile.exists()) {
            val inputStream = try {
                context.assets.open(assetPath)
            } catch (_: Exception) {
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
            targetFile.absolutePath,
            null,
            SQLiteDatabase.OPEN_READONLY
        )
    }
}

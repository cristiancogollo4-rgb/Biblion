package com.cristiancogollo.biblion.feature.bibi

import android.app.Application
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.cristiancogollo.biblion.feature.bibi.engine.DictionaryEngine
import com.cristiancogollo.biblion.feature.bibi.model.BibiResponse
import com.cristiancogollo.biblion.feature.bibi.model.BibiSuggestion
import com.cristiancogollo.biblion.feature.bibi.model.BibiVerse
import com.cristiancogollo.biblion.feature.bibi.model.ChatExchange
import com.cristiancogollo.biblion.feature.bibi.model.Confidence
import com.cristiancogollo.biblion.feature.bibi.model.Source
import com.cristiancogollo.biblion.feature.dictionary.data.DictionaryCategory
import com.cristiancogollo.biblion.feature.dictionary.data.DictionaryEntry
import com.cristiancogollo.biblion.feature.dictionary.data.DictionaryRepository
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

/**
 * Tests para DictionaryEngine con la DB real dictionary_v2.db.
 * Incluye tests de integracion (defineTerm) y tests de formato de respuesta.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DictionaryEngineIntegrationTest {

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

    // ── DB data verification ───────────────────────────────────

    @Test
    fun `dictionary_v2 has entries for Abraham`() = runBlocking {
        val db = openAssetDb("databases/dictionary_v2.db")
        try {
            val cursor = db.rawQuery(
                "SELECT term, category, gender, birth_year FROM dictionary_entries WHERE normalized_term = 'abraham' LIMIT 1",
                null
            )
            assertTrue("Abraham should exist in dictionary", cursor.moveToFirst())
            assertEquals("person", cursor.getString(1))
            assertEquals("Male", cursor.getString(2))
            cursor.close()
        } finally {
            db.close()
        }
    }

    @Test
    fun `dictionary_v2 has entries for Jerusalem`() = runBlocking {
        val db = openAssetDb("databases/dictionary_v2.db")
        try {
            val cursor = db.rawQuery(
                "SELECT term, category, latitude, longitude FROM dictionary_entries WHERE normalized_term LIKE '%jerusalen%' LIMIT 1",
                null
            )
            assertTrue("Jerusalem should exist", cursor.moveToFirst())
            assertEquals("place", cursor.getString(1))
            assertNotNull("Jerusalem should have latitude", cursor.getString(2))
            cursor.close()
        } finally {
            db.close()
        }
    }

    @Test
    fun `dictionary_v2 has entries for gracia concept`() = runBlocking {
        val db = openAssetDb("databases/dictionary_v2.db")
        try {
            val cursor = db.rawQuery(
                "SELECT term, category FROM dictionary_entries WHERE normalized_term = 'gracia' LIMIT 1",
                null
            )
            assertTrue("Gracia should exist", cursor.moveToFirst())
            cursor.close()
        } finally {
            db.close()
        }
    }

    @Test
    fun `dictionary_v2 has references_json for Abraham`() = runBlocking {
        val db = openAssetDb("databases/dictionary_v2.db")
        try {
            val cursor = db.rawQuery(
                "SELECT references_json FROM dictionary_entries WHERE normalized_term = 'abraham' LIMIT 1",
                null
            )
            assertTrue("Abraham should have references", cursor.moveToFirst())
            val refs = cursor.getString(0)
            assertFalse("references_json should not be empty", refs.isBlank() || refs == "[]")
            cursor.close()
        } finally {
            db.close()
        }
    }

    @Test
    fun `dictionary_v2 has 69 book entries`() = runBlocking {
        val db = openAssetDb("databases/dictionary_v2.db")
        try {
            val cursor = db.rawQuery(
                "SELECT COUNT(*) FROM dictionary_entries WHERE category = 'book'",
                null
            )
            val count = if (cursor.moveToFirst()) cursor.getLong(0) else 0L
            cursor.close()
            assertEquals("Should have 69 book entries (66 canonical + 3 meta)", 69L, count)
        } finally {
            db.close()
        }
    }

    // ── formatYear / translateFeatureType via DictionaryEngine ──

    @Test
    fun `DictionaryEngine formatYear handles ISO astronomic years`() {
        assertEquals("2000 a.C.", DictionaryEngine.formatYear("-2000"))
        assertEquals("1 a.C.", DictionaryEngine.formatYear("0"))
        assertEquals("1 d.C.", DictionaryEngine.formatYear("1"))
        assertEquals("33 d.C.", DictionaryEngine.formatYear("33"))
    }

    @Test
    fun `DictionaryEngine translateFeatureType covers all OSM types`() {
        assertEquals("Ciudad", DictionaryEngine.translateFeatureType("city"))
        assertEquals("Región", DictionaryEngine.translateFeatureType("region"))
        assertEquals("País", DictionaryEngine.translateFeatureType("country"))
        assertEquals("Monte", DictionaryEngine.translateFeatureType("mountain"))
        assertEquals("Río", DictionaryEngine.translateFeatureType("river"))
        assertEquals("Valle", DictionaryEngine.translateFeatureType("valley"))
        assertEquals("Desierto", DictionaryEngine.translateFeatureType("desert"))
        assertEquals("Isla", DictionaryEngine.translateFeatureType("island"))
        assertEquals("Llanura", DictionaryEngine.translateFeatureType("plain"))
        assertEquals("Agua/Mar", DictionaryEngine.translateFeatureType("water"))
    }

    // ── extractSignificantWords ────────────────────────────────

    @Test
    fun `extractSignificantWords handles biblical text`() {
        val words = DictionaryEngine.extractSignificantWords(
            "Porque de tal manera amó Dios al mundo que dio a su Hijo unigénito"
        )
        assertTrue("Should find 'Dios'", words.any { it.lowercase() == "dios" })
        assertTrue("Should find 'mundo'", words.any { it.lowercase() == "mundo" })
        assertTrue("Should find 'Hijo'", words.any { it.lowercase() == "hijo" })
        assertFalse("Should not find stopwords", words.any { it.lowercase() == "que" })
        assertFalse("Should not find stopwords", words.any { it.lowercase() == "porque" })
    }

    // ── buildSuggestions ───────────────────────────────────────

    @Test
    fun `buildSuggestions produces 2 suggestions without history`() {
        val suggestions = DictionaryEngine.buildSuggestions("Amor")
        assertEquals(2, suggestions.size)
        assertTrue("First should be pasajes", suggestions[0].label.contains("Amor"))
        assertTrue("Second should be profundizar", suggestions[1].label.contains("Profundizar"))
        suggestions.forEach { assertTrue("All should be AI", it.isAi) }
    }

    @Test
    fun `buildSuggestions produces comparison with history`() {
        val history = listOf(
            ChatExchange(question = "", response = "", resolvedTerm = "Fe", intent = "DEFINE")
        )
        val suggestions = DictionaryEngine.buildSuggestions("Amor", history)
        assertEquals(2, suggestions.size)
        assertTrue("Should have pasajes", suggestions[0].label.contains("Amor"))
        assertEquals("Comparar con Fe", suggestions[1].label)
    }

    @Test
    fun `buildSuggestions ignores same term in history`() {
        val history = listOf(
            ChatExchange(question = "", response = "", resolvedTerm = "Amor", intent = "DEFINE")
        )
        val suggestions = DictionaryEngine.buildSuggestions("Amor", history)
        assertEquals("Profundizar con IA", suggestions[1].label)
    }

    // ── BibiResponse buildChatText ─────────────────────────────

    @Test
    fun `BibiResponse includes verses with empty text`() {
        val response = BibiResponse(
            title = "Abraham",
            definition = "Padre de multitudes.",
            verses = listOf(BibiVerse(ref = "Génesis 12:1", text = "")),
            followUp = "¿Más info?"
        )
        val text = response.buildChatText()
        assertTrue("Should include verse ref", text.contains("Génesis 12:1"))
    }

    @Test
    fun `BibiResponse includes verses with text`() {
        val response = BibiResponse(
            title = "Fe",
            definition = "La certeza de lo que se espera.",
            verses = listOf(BibiVerse(ref = "Hebreos 11:1", text = "Es, pues, la fe...")),
            followUp = "¿Más?"
        )
        val text = response.buildChatText()
        assertTrue("Should include verse text", text.contains("Hebreos 11:1"))
        assertTrue("Should include verse content", text.contains("certeza de lo que se espera"))
    }

    @Test
    fun `BibiResponse includes metadata facts`() {
        val response = BibiResponse(
            title = "Abraham",
            definition = "Padre de multitudes.",
            metadata = listOf(
                com.cristiancogollo.biblion.feature.bibi.model.MetadataFact("", "Género", "Masculino"),
                com.cristiancogollo.biblion.feature.bibi.model.MetadataFact("", "Nacimiento", "2001 a.C.")
            ),
            followUp = "¿Más?"
        )
        val text = response.buildChatText()
        assertTrue("Should show Género", text.contains("Género: Masculino"))
        assertTrue("Should show Nacimiento", text.contains("Nacimiento: 2001 a.C."))
    }

    @Test
    fun `BibiResponse with icon and label shows both`() {
        val response = BibiResponse(
            title = "Jerusalén",
            definition = "Ciudad santa.",
            metadata = listOf(
                com.cristiancogollo.biblion.feature.bibi.model.MetadataFact("📍", "Ubicación", "31.78, 35.21")
            ),
            followUp = "¿Más?"
        )
        val text = response.buildChatText()
        assertTrue("Should show icon", text.contains("📍"))
        assertTrue("Should show label", text.contains("Ubicación:"))
        assertTrue("Should show value", text.contains("31.78, 35.21"))
    }

    @Test
    fun `BibiResponse greeting appears first`() {
        val response = BibiResponse(
            greeting = "¡Hola Juan!",
            title = "Test",
            definition = "Test def.",
            followUp = "¿Test?"
        )
        assertTrue(response.buildChatText().startsWith("¡Hola Juan!"))
    }

    @Test
    fun `BibiResponse confidence defaults to HIGH`() {
        val response = BibiResponse(title = "T", definition = "D", followUp = "F")
        assertEquals(Confidence.HIGH, response.confidence)
    }

    @Test
    fun `BibiResponse source defaults to LOCAL`() {
        val response = BibiResponse(title = "T", definition = "D", followUp = "F")
        assertEquals(Source.LOCAL, response.source)
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

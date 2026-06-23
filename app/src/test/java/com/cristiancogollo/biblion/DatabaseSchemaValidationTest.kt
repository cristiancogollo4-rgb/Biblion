package com.cristiancogollo.biblion

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Tests que validan que el schema de las DBs pre-empaquetadas coincide
 * con la entity Room correspondiente.
 *
 * Esto previene el crash "Pre-packaged database has an invalid schema"
 * que ocurre cuando Room detecta diferencias entre el schema esperado
 * y el schema real del archivo .db.
 *
 * Si estos tests fallan:
 * 1. Revisar si se cambio el entity Kotlin (columnas, default values, indices)
 * 2. Ejecutar tools/build_crossrefs_votes_sqlite.py o tools/build_topics_sqlite.py
 *    para regenerar la DB desde el script Python
 * 3. O ajustar el script Python para que coincida con el entity
 *
 * La validacion equivalente se hace en build-time dentro del script Python
 * (ver verify_schema en cada script). Este test es la red de seguridad
 * para builds incrementales donde el script no se ejecuta.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DatabaseSchemaValidationTest {

    @Test
    fun `cross_reference_votes - votes NO tiene default value`() {
        copyAssetToInternalStorage("databases/cross_references_votes.db", "xref_test.db")
        try {
            val db = openTestDb("xref_test.db")
            val defaultValues = getTableInfo(db, "cross_reference_votes")
            db.close()
            assertTrue(
                "votes debe NO tener default value. Encontrado: '${defaultValues["votes"]}'. " +
                "Si el entity no declara defaultValue, el script Python tampoco debe usar DEFAULT.",
                defaultValues["votes"] == null
            )
        } finally {
            deleteTestDb("xref_test.db")
        }
    }

    @Test
    fun `cross_reference_votes - indices coinciden con entity`() {
        copyAssetToInternalStorage("databases/cross_references_votes.db", "xref_test.db")
        try {
            val db = openTestDb("xref_test.db")
            val expected = setOf("idx_xref_votes_source", "idx_xref_votes_source_count")
            val actual = getUserIndices(db, "cross_reference_votes")
            db.close()
            assertEquals(
                "Indices deben coincidir con @Index del entity. " +
                "Si agregaste un indice al entity, regenera la DB con el script Python.",
                expected, actual
            )
        } finally {
            deleteTestDb("xref_test.db")
        }
    }

    @Test
    fun `cross_reference_votes - columnas requeridas existen`() {
        copyAssetToInternalStorage("databases/cross_references_votes.db", "xref_test.db")
        try {
            val db = openTestDb("xref_test.db")
            val required = setOf(
                "id", "source_book", "source_normalized_book", "source_chapter",
                "source_verse", "target_references", "votes"
            )
            val actual = getColumnNames(db, "cross_reference_votes")
            db.close()
            assertTrue(
                "Faltan columnas: ${required - actual}",
                actual.containsAll(required)
            )
        } finally {
            deleteTestDb("xref_test.db")
        }
    }

    @Test
    fun `topics - score NO tiene default value`() {
        copyAssetToInternalStorage("databases/topics.db", "topics_test.db")
        try {
            val db = openTestDb("topics_test.db")
            val defaultValues = getTableInfo(db, "topic_references")
            db.close()
            assertTrue(
                "score debe NO tener default value. Encontrado: '${defaultValues["score"]}'",
                defaultValues["score"] == null
            )
        } finally {
            deleteTestDb("topics_test.db")
        }
    }

    @Test
    fun `topics - indices coinciden con entity (schema v3)`() {
        copyAssetToInternalStorage("databases/topics.db", "topics_test.db")
        try {
            val db = openTestDb("topics_test.db")
            val expected = setOf(
                "idx_topics_slug",
                "idx_topics_category",
                "idx_topics_name_es",
                "idx_topics_name_en",
                "idx_topics_parent"
            )
            val actual = getUserIndices(db, "topics")
            db.close()
            assertEquals(
                "Indices deben coincidir con @Index del entity",
                expected, actual
            )
        } finally {
            deleteTestDb("topics_test.db")
        }
    }

    @Test
    fun `topics - columnas requeridas existen (schema v3)`() {
        copyAssetToInternalStorage("databases/topics.db", "topics_test.db")
        try {
            val db = openTestDb("topics_test.db")
            val required = setOf(
                "id", "slug", "name_es", "name_en", "category",
                "description", "description_source", "parent_slug",
                "verse_count", "total_aliases"
            )
            val actual = getColumnNames(db, "topics")
            db.close()
            assertTrue(
                "Faltan columnas: ${required - actual}",
                actual.containsAll(required)
            )
        } finally {
            deleteTestDb("topics_test.db")
        }
    }

    @Test
    fun `topic_aliases - columnas requeridas existen (schema v3)`() {
        copyAssetToInternalStorage("databases/topics.db", "topics_test.db")
        try {
            val db = openTestDb("topics_test.db")
            val required = setOf(
                "id", "topic_slug", "alias_en", "score",
                "verse_count", "max_quality", "low_confidence"
            )
            val actual = getColumnNames(db, "topic_aliases")
            db.close()
            assertTrue(
                "Faltan columnas: ${required - actual}",
                actual.containsAll(required)
            )
        } finally {
            deleteTestDb("topics_test.db")
        }
    }

    @Test
    fun `topic_relationships - columnas requeridas existen (schema v3)`() {
        copyAssetToInternalStorage("databases/topics.db", "topics_test.db")
        try {
            val db = openTestDb("topics_test.db")
            val required = setOf(
                "id", "parent_slug", "child_slug", "relationship_type"
            )
            val actual = getColumnNames(db, "topic_relationships")
            db.close()
            assertTrue(
                "Faltan columnas: ${required - actual}",
                actual.containsAll(required)
            )
        } finally {
            deleteTestDb("topics_test.db")
        }
    }

    @Test
    fun `topics - tiene 693 filas (taxonomia canonica)`() {
        copyAssetToInternalStorage("databases/topics.db", "topics_test.db")
        try {
            val db = openTestDb("topics_test.db")
            val count = db.compileStatement("SELECT COUNT(*) FROM topics").simpleQueryForLong()
            db.close()
            assertEquals("topics debe tener 693 canonicos", 693L, count)
        } finally {
            deleteTestDb("topics_test.db")
        }
    }

    @Test
    fun `topic_aliases - tiene al menos 1500 filas`() {
        copyAssetToInternalStorage("databases/topics.db", "topics_test.db")
        try {
            val db = openTestDb("topics_test.db")
            val count = db.compileStatement("SELECT COUNT(*) FROM topic_aliases").simpleQueryForLong()
            db.close()
            assertTrue("topic_aliases debe tener >=1500, tiene $count", count >= 1500)
        } finally {
            deleteTestDb("topics_test.db")
        }
    }

    // --- Helpers ---

    private fun getContext(): Context =
        ApplicationProvider.getApplicationContext()

    private fun copyAssetToInternalStorage(assetPath: String, targetName: String) {
        val context = getContext()
        val targetFile = File(context.filesDir, targetName)
        // Eliminar si ya existe de un test previo
        if (targetFile.exists()) {
            targetFile.delete()
        }
        // Intentar primero via assets (requiere @Config(assets = ...) en test).
        // Si falla, caer al classpath (ubicacion directa de archivos en build).
        val inputStream = try {
            context.assets.open(assetPath)
        } catch (e: Exception) {
            // Fallback: leer del filesystem. Gradle test working dir es `app/`.
            val workingDir = File(".").absolutePath
            val candidates = listOf(
                File("$workingDir/src/main/assets/$assetPath"),
                File("$workingDir/../src/main/assets/$assetPath"),
                File("src/main/assets/$assetPath")
            )
            val found = candidates.firstOrNull { it.exists() }
                ?: throw IllegalStateException(
                    "Cannot find DB at asset path '$assetPath'. Working dir: $workingDir. " +
                    "Probada: $candidates"
                )
            java.io.FileInputStream(found)
        }
        inputStream.use { input ->
            targetFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun openTestDb(name: String): android.database.sqlite.SQLiteDatabase {
        val context = getContext()
        val file = File(context.filesDir, name)
        if (!file.exists()) {
            throw IllegalStateException("Test DB not found: ${file.absolutePath}. " +
                "Did you call copyAssetToInternalStorage first?")
        }
        return android.database.sqlite.SQLiteDatabase.openDatabase(
            file.absolutePath,
            null,
            android.database.sqlite.SQLiteDatabase.OPEN_READONLY
        )
    }

    private fun deleteTestDb(name: String) {
        val context = getContext()
        File(context.filesDir, name).delete()
    }

    private fun getTableInfo(
        db: android.database.sqlite.SQLiteDatabase,
        table: String
    ): Map<String, String?> {
        val cursor = db.rawQuery("PRAGMA table_info($table)", null)
        val result = mutableMapOf<String, String?>()
        cursor.use {
            val nameIdx = it.getColumnIndexOrThrow("name")
            val dfltIdx = it.getColumnIndexOrThrow("dflt_value")
            while (it.moveToNext()) {
                result[it.getString(nameIdx)] = if (it.isNull(dfltIdx)) null else it.getString(dfltIdx)
            }
        }
        return result
    }

    private fun getColumnNames(
        db: android.database.sqlite.SQLiteDatabase,
        table: String
    ): Set<String> {
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

    private fun getUserIndices(
        db: android.database.sqlite.SQLiteDatabase,
        table: String
    ): Set<String> {
        val cursor = db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='index' AND tbl_name=? AND sql IS NOT NULL",
            arrayOf(table)
        )
        return buildSet {
            cursor.use {
                val nameIdx = it.getColumnIndexOrThrow("name")
                while (it.moveToNext()) {
                    val name = it.getString(nameIdx)
                    if (!name.startsWith("sqlite_autoindex_")) {
                        add(name)
                    }
                }
            }
        }
    }
}

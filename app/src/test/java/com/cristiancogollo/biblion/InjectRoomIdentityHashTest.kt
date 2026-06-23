package com.cristiancogollo.biblion

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.cristiancogollo.biblion.feature.bibi.CrossReferenceVoteDatabase
import com.cristiancogollo.biblion.feature.bibi.TopicDatabase
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Inyecta el identityHash correcto de Room en las DBs pre-empaquetadas.
 *
 * El script Python genera la DB con datos correctos pero usa un algoritmo
 * de hash diferente al de Room. Este test usa Room para calcular el
 * identityHash REAL y lo escribe en la DB pre-empaquetada.
 *
 * Pasos:
 * 1. python tools/build_crossrefs_votes_sqlite.py  (genera DB con datos)
 * 2. python tools/build_topics_sqlite.py             (genera DB con datos)
 * 3. ./gradlew.bat :app:testDebugUnitTest --tests "*InjectRoomIdentityHashTest*"
 *    (inyecta el identityHash correcto)
 * 4. ./gradlew.bat :app:assembleDebug                 (genera APK)
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class InjectRoomIdentityHashTest {

    @Test
    fun `inject identity hash into cross_references_votes db`() {
        injectHashXref()
    }

    @Test
    fun `inject identity hash into topics db`() {
        injectHashTopic()
    }

    private fun injectHashXref() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        val tempDbName = "temp_identity_xref.db"
        val tempDbFile = context.getDatabasePath(tempDbName)
        if (tempDbFile.exists()) tempDbFile.delete()

        val roomDb = Room.databaseBuilder(
            context, CrossReferenceVoteDatabase::class.java, tempDbName
        ).build()
        roomDb.openHelper.writableDatabase
        roomDb.close()

        val identityHash = readIdentityHash(tempDbFile)
        tempDbFile.delete()

        println("=== cross_references_votes.db ===")
        println("Identity hash de Room: $identityHash")

        injectIntoTarget("cross_references_votes.db", identityHash)
    }

    private fun injectHashTopic() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        val tempDbName = "temp_identity_topic.db"
        val tempDbFile = context.getDatabasePath(tempDbName)
        if (tempDbFile.exists()) tempDbFile.delete()

        val roomDb = Room.databaseBuilder(
            context, TopicDatabase::class.java, tempDbName
        ).build()
        roomDb.openHelper.writableDatabase
        roomDb.close()

        val identityHash = readIdentityHash(tempDbFile)
        tempDbFile.delete()

        println("=== topics.db ===")
        println("Identity hash de Room: $identityHash")

        injectIntoTarget("topics.db", identityHash)
    }

    private fun readIdentityHash(dbFile: File): String {
        val conn = android.database.sqlite.SQLiteDatabase.openDatabase(
            dbFile.absolutePath, null, android.database.sqlite.SQLiteDatabase.OPEN_READONLY
        )
        val c = conn.rawQuery("SELECT identity_hash FROM room_master_table WHERE id = 42", null)
        val result: String
        c.use {
            if (!it.moveToFirst()) {
                throw IllegalStateException("No se pudo leer identityHash de ${dbFile.name}")
            }
            result = it.getString(0)
        }
        conn.close()
        return result
    }

    private fun injectIntoTarget(targetDbName: String, identityHash: String) {
        val targetFile = File("src/main/assets/databases", targetDbName)
        if (!targetFile.exists()) {
            throw IllegalStateException(
                "${targetFile.absolutePath} no existe. " +
                "Ejecuta primero el script Python para generar la DB con datos."
            )
        }

        val targetConn = android.database.sqlite.SQLiteDatabase.openDatabase(
            targetFile.absolutePath, null, android.database.sqlite.SQLiteDatabase.OPEN_READWRITE
        )

        val checkCursor = targetConn.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='table' AND name='room_master_table'",
            null
        )
        val tableExists = checkCursor.use { it.count > 0 }
        checkCursor.close()

        if (!tableExists) {
            targetConn.execSQL(
                "CREATE TABLE room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)"
            )
        }
        targetConn.execSQL(
            "INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES (?, ?)",
            arrayOf(42, identityHash)
        )
        targetConn.close()

        println("Identity hash inyectado en $targetDbName")
    }
}

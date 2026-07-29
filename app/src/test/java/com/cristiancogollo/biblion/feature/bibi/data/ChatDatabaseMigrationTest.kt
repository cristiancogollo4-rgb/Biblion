package com.cristiancogollo.biblion.feature.bibi.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ChatDatabaseMigrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val databaseName = "bibi_chat_migration_test.db"

    @After
    fun cleanUp() {
        context.deleteDatabase(databaseName)
    }

    @Test
    fun `migration 1 to 2 preserves messages and adds passage context`() = runBlocking {
        context.deleteDatabase(databaseName)
        context.openOrCreateDatabase(databaseName, Context.MODE_PRIVATE, null).use { db ->
            db.execSQL(
                """
                CREATE TABLE chat_sessions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    title TEXT NOT NULL,
                    first_query TEXT NOT NULL,
                    mode TEXT NOT NULL,
                    created_at INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE chat_messages (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    session_id INTEGER NOT NULL,
                    role TEXT NOT NULL,
                    content TEXT NOT NULL,
                    resolved_term TEXT,
                    intent TEXT,
                    created_at INTEGER NOT NULL,
                    FOREIGN KEY(session_id) REFERENCES chat_sessions(id) ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX idx_msg_session ON chat_messages(session_id)")
            db.execSQL(
                "INSERT INTO chat_sessions VALUES (1, 'Genesis', 'Explica Genesis 1:1', 'reader', 1, 1)"
            )
            db.execSQL(
                "INSERT INTO chat_messages VALUES (1, 1, 'assistant', 'Respuesta guardada', NULL, 'EXPLAIN_VERSE', 1)"
            )
            db.version = 1
        }

        val migrated = Room.databaseBuilder(context, ChatDatabase::class.java, databaseName)
            .addMigrations(ChatDatabase.MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()

        val messages = migrated.messageDao().getBySession(1)
        assertEquals(1, messages.size)
        assertEquals("Respuesta guardada", messages.single().content)
        assertEquals(null, messages.single().contextPassagesJson)

        migrated.openHelper.readableDatabase.query("PRAGMA table_info(chat_messages)").use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            val columns = buildSet {
                while (cursor.moveToNext()) add(cursor.getString(nameIndex))
            }
            assertTrue("La migracion debe agregar context_passages_json", "context_passages_json" in columns)
        }
        migrated.close()
    }
}

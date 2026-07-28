package com.cristiancogollo.biblion.feature.bibi.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatSessionLimitTest {
    @Test
    fun `sixth chat removes only the oldest created session`() {
        val sessions = listOf(6L, 2L, 5L, 1L, 4L, 3L).map { createdAt ->
            ChatSessionEntity(
                id = createdAt,
                title = "Chat $createdAt",
                firstQuery = "Pregunta $createdAt",
                mode = "reader",
                createdAt = createdAt,
                updatedAt = 100L - createdAt,
            )
        }

        val pruned = sessionsToPrune(sessions)

        assertEquals(listOf(1L), pruned.map { it.id })
        assertTrue(pruned.none { it.id == 6L })
    }

    @Test
    fun `five chats or fewer are preserved`() {
        val sessions = (1L..5L).map { createdAt ->
            ChatSessionEntity(
                id = createdAt,
                title = "Chat $createdAt",
                firstQuery = "Pregunta $createdAt",
                mode = "study",
                createdAt = createdAt,
            )
        }

        assertTrue(sessionsToPrune(sessions).isEmpty())
    }
}

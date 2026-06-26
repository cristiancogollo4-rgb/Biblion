package com.cristiancogollo.biblion.feature.bibi.data

import android.content.Context
import com.cristiancogollo.biblion.feature.bibi.model.ChatExchange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ChatSession(
    val id: Long,
    val title: String,
    val firstQuery: String,
    val mode: String,
    val createdAt: Long,
    val updatedAt: Long
)

data class ChatMessage(
    val id: Long,
    val sessionId: Long,
    val role: String,
    val content: String,
    val resolvedTerm: String?,
    val intent: String?,
    val createdAt: Long
)

object ChatSessionRepository {

    private suspend fun sessionDao(context: Context) =
        ChatDatabase.getInstance(context).sessionDao()

    private suspend fun messageDao(context: Context) =
        ChatDatabase.getInstance(context).messageDao()

    suspend fun createSession(
        context: Context,
        title: String,
        firstQuery: String,
        mode: String
    ): Long = withContext(Dispatchers.IO) {
        try {
            val entity = ChatSessionEntity(
                title = title,
                firstQuery = firstQuery,
                mode = mode
            )
            sessionDao(context).insert(entity)
        } catch (e: Exception) {
            android.util.Log.e("ChatSession", "Error creating session", e)
            0L
        }
    }

    suspend fun updateSessionTimestamp(context: Context, sessionId: Long) = withContext(Dispatchers.IO) {
        try {
            val dao = sessionDao(context)
            val existing = dao.getById(sessionId) ?: return@withContext
            dao.update(existing.copy(updatedAt = System.currentTimeMillis()))
        } catch (e: Exception) {
            android.util.Log.e("ChatSession", "Error updating session", e)
        }
    }

    suspend fun saveUserMessage(
        context: Context,
        sessionId: Long,
        content: String
    ): Long = withContext(Dispatchers.IO) {
        try {
            messageDao(context).insert(
                ChatMessageEntity(
                    sessionId = sessionId,
                    role = "user",
                    content = content
                )
            )
        } catch (e: Exception) {
            android.util.Log.e("ChatSession", "Error saving user message", e)
            0L
        }
    }

    suspend fun saveAssistantMessage(
        context: Context,
        sessionId: Long,
        content: String,
        resolvedTerm: String?,
        intent: String?
    ): Long = withContext(Dispatchers.IO) {
        try {
            messageDao(context).insert(
                ChatMessageEntity(
                    sessionId = sessionId,
                    role = "assistant",
                    content = content,
                    resolvedTerm = resolvedTerm,
                    intent = intent
                )
            )
        } catch (e: Exception) {
            android.util.Log.e("ChatSession", "Error saving assistant message", e)
            0L
        }
    }

    suspend fun getAllSessions(context: Context): List<ChatSession> = withContext(Dispatchers.IO) {
        try {
            sessionDao(context).getAll().map { it.toDomain() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getMostRecentSession(context: Context): ChatSession? = withContext(Dispatchers.IO) {
        try {
            sessionDao(context).getMostRecent()?.toDomain()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getSessionMessages(context: Context, sessionId: Long): List<ChatMessage> =
        withContext(Dispatchers.IO) {
            try {
                messageDao(context).getBySession(sessionId).map { it.toDomain() }
            } catch (e: Exception) {
                emptyList()
            }
        }

    suspend fun deleteSession(context: Context, sessionId: Long) = withContext(Dispatchers.IO) {
        try {
            sessionDao(context).deleteById(sessionId)
        } catch (e: Exception) {
            android.util.Log.e("ChatSession", "Error deleting session", e)
        }
    }

    suspend fun deleteAllSessions(context: Context) = withContext(Dispatchers.IO) {
        try {
            val dao = sessionDao(context)
            val all = dao.getAll()
            for (session in all) {
                dao.delete(session)
            }
        } catch (e: Exception) {
            android.util.Log.e("ChatSession", "Error clearing all sessions", e)
        }
    }

    fun buildChatHistory(messages: List<ChatMessage>): List<ChatExchange> {
        val history = mutableListOf<ChatExchange>()
        val grouped = messages.windowed(2, 2, false)
        for (group in grouped) {
            val user = group.getOrNull(0)
            val assistant = group.getOrNull(1)
            if (user != null && assistant != null) {
                history.add(
                    ChatExchange(
                        question = user.content,
                        response = assistant.content,
                        resolvedTerm = assistant.resolvedTerm,
                        intent = assistant.intent ?: "FALLBACK"
                    )
                )
            }
        }
        return history.takeLast(10)
    }

    private fun ChatSessionEntity.toDomain() = ChatSession(
        id = id,
        title = title,
        firstQuery = firstQuery,
        mode = mode,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun ChatMessageEntity.toDomain() = ChatMessage(
        id = id,
        sessionId = sessionId,
        role = role,
        content = content,
        resolvedTerm = resolvedTerm,
        intent = intent,
        createdAt = createdAt
    )
}

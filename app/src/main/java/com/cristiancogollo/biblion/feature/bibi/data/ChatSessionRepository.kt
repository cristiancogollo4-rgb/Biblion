package com.cristiancogollo.biblion.feature.bibi.data

import android.content.Context
import com.cristiancogollo.biblion.feature.bibi.model.BibiPassage
import com.cristiancogollo.biblion.feature.bibi.model.ChatExchange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import com.google.firebase.auth.FirebaseAuth

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
    val contextPassages: List<BibiPassage>,
    val createdAt: Long
)

object ChatSessionRepository {
    const val MAX_SESSIONS = 5
    private val json = Json { ignoreUnknownKeys = true }

    private suspend fun sessionDao(context: Context) =
        ChatDatabase.getInstance(context).sessionDao()

    private suspend fun messageDao(context: Context) =
        ChatDatabase.getInstance(context).messageDao()

    private fun currentOwnerUid(): String =
        FirebaseAuth.getInstance().currentUser?.uid ?: GUEST_CHAT_OWNER

    suspend fun createSession(
        context: Context,
        title: String,
        firstQuery: String,
        mode: String
    ): Long = withContext(Dispatchers.IO) {
        try {
            val ownerUid = currentOwnerUid()
            val entity = ChatSessionEntity(
                title = title,
                firstQuery = firstQuery,
                mode = mode,
                ownerUid = ownerUid,
            )
            val dao = sessionDao(context)
            val sessionId = dao.insert(entity)
            sessionsToPrune(dao.getAll(ownerUid)).forEach { session ->
                messageDao(context).deleteBySession(session.id)
                dao.delete(session)
            }
            sessionId
        } catch (e: Exception) {
            android.util.Log.e("ChatSession", "Error creating session", e)
            0L
        }
    }

    suspend fun updateSessionTimestamp(context: Context, sessionId: Long) = withContext(Dispatchers.IO) {
        try {
            val dao = sessionDao(context)
            val existing = dao.getById(sessionId, currentOwnerUid()) ?: return@withContext
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
            if (sessionDao(context).getById(sessionId, currentOwnerUid()) == null) {
                return@withContext 0L
            }
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
        intent: String?,
        contextPassages: List<BibiPassage> = emptyList(),
    ): Long = withContext(Dispatchers.IO) {
        try {
            if (sessionDao(context).getById(sessionId, currentOwnerUid()) == null) {
                return@withContext 0L
            }
            messageDao(context).insert(
                ChatMessageEntity(
                    sessionId = sessionId,
                    role = "assistant",
                    content = content,
                    resolvedTerm = resolvedTerm,
                    intent = intent,
                    contextPassagesJson = contextPassages
                        .takeIf { it.isNotEmpty() }
                        ?.let { json.encodeToString(ListSerializer(BibiPassage.serializer()), it) }
                )
            )
        } catch (e: Exception) {
            android.util.Log.e("ChatSession", "Error saving assistant message", e)
            0L
        }
    }

    suspend fun getAllSessions(context: Context): List<ChatSession> = withContext(Dispatchers.IO) {
        try {
            sessionDao(context).getAll(currentOwnerUid()).map { it.toDomain() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getMostRecentSession(context: Context): ChatSession? = withContext(Dispatchers.IO) {
        try {
            sessionDao(context).getMostRecent(currentOwnerUid())?.toDomain()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getMostRecentSession(context: Context, mode: String): ChatSession? =
        withContext(Dispatchers.IO) {
            try {
                sessionDao(context).getMostRecentByMode(mode, currentOwnerUid())?.toDomain()
            } catch (e: Exception) {
                null
            }
        }

    suspend fun getSessionMessages(context: Context, sessionId: Long): List<ChatMessage> =
        withContext(Dispatchers.IO) {
            try {
                if (sessionDao(context).getById(sessionId, currentOwnerUid()) == null) {
                    return@withContext emptyList()
                }
                messageDao(context).getBySession(sessionId).map { it.toDomain() }
            } catch (e: Exception) {
                emptyList()
            }
        }

    suspend fun deleteSession(context: Context, sessionId: Long) = withContext(Dispatchers.IO) {
        try {
            val ownerUid = currentOwnerUid()
            if (sessionDao(context).getById(sessionId, ownerUid) == null) {
                return@withContext
            }
            messageDao(context).deleteBySession(sessionId)
            sessionDao(context).deleteById(sessionId, ownerUid)
        } catch (e: Exception) {
            android.util.Log.e("ChatSession", "Error deleting session", e)
        }
    }

    suspend fun deleteAllSessions(context: Context) = withContext(Dispatchers.IO) {
        try {
            val dao = sessionDao(context)
            val ownerUid = currentOwnerUid()
            val ownedSessions = dao.getAll(ownerUid)
            for (session in ownedSessions) {
                messageDao(context).deleteBySession(session.id)
            }
            dao.deleteAllByOwner(ownerUid)
        } catch (e: Exception) {
            android.util.Log.e("ChatSession", "Error clearing all sessions", e)
        }
    }

    fun buildChatHistory(messages: List<ChatMessage>): List<ChatExchange> {
        val history = mutableListOf<ChatExchange>()
        var pendingUser: ChatMessage? = null
        for (message in messages.sortedBy { it.createdAt }) {
            when (message.role) {
                "user" -> pendingUser = message
                "assistant" -> {
                    val user = pendingUser
                    if (user == null) continue
                history.add(
                    ChatExchange(
                        question = user.content,
                            response = message.content,
                            resolvedTerm = message.resolvedTerm,
                            intent = message.intent ?: "FALLBACK"
                    )
                )
                    pendingUser = null
                }
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
        contextPassages = contextPassagesJson?.let { encoded ->
            runCatching {
                json.decodeFromString(ListSerializer(BibiPassage.serializer()), encoded)
            }.getOrDefault(emptyList())
        }.orEmpty(),
        createdAt = createdAt
    )
}

internal fun sessionsToPrune(
    sessions: List<ChatSessionEntity>,
    maxSessions: Int = ChatSessionRepository.MAX_SESSIONS,
): List<ChatSessionEntity> {
    return sessions
        .sortedBy { it.createdAt }
        .dropLast(maxSessions.coerceAtLeast(0))
}

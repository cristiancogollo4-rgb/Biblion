package com.cristiancogollo.biblion.feature.bibi.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cristiancogollo.biblion.feature.bibi.data.ChatSessionRepository
import com.cristiancogollo.biblion.feature.bibi.data.ChatSession
import com.cristiancogollo.biblion.feature.bibi.domain.BibiOrchestrator
import com.cristiancogollo.biblion.feature.bibi.domain.BibiReferenceContextResolver
import com.cristiancogollo.biblion.feature.bibi.model.BibiContext
import com.cristiancogollo.biblion.feature.bibi.model.BibiPassage
import com.cristiancogollo.biblion.feature.bibi.model.BibiQueryResult
import com.cristiancogollo.biblion.feature.bibi.model.BibiSuggestion
import com.cristiancogollo.biblion.feature.bibi.model.ChatExchange
import com.cristiancogollo.biblion.feature.bibi.model.Confidence
import com.cristiancogollo.biblion.feature.bibi.model.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class BibiChatMessage(
    val role: String,
    val text: String,
    val suggestions: List<BibiSuggestion> = emptyList(),
    val suggestedBlocks: List<String> = emptyList(),
    val contextPassages: List<BibiPassage> = emptyList(),
)

data class BibiChatUiState(
    val messages: List<BibiChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val isInitialized: Boolean = false,
    val completedExchanges: Int = 0,
    val sessions: List<ChatSession> = emptyList(),
    val activeSessionId: Long? = null,
)

class BibiViewModel(
    context: Context,
    private val orchestrator: BibiOrchestrator = BibiOrchestrator(),
) : ViewModel() {
    private val appContext = context.applicationContext
    private val _uiState = MutableStateFlow(BibiChatUiState())
    val uiState: StateFlow<BibiChatUiState> = _uiState.asStateFlow()

    private var currentSessionId: Long? = null
    private var chatHistory: List<ChatExchange> = emptyList()
    private var lastQueries: List<String> = emptyList()
    private var initializedMode: String? = null
    private var initialGreeting: String = ""
    private var initialSuggestions: List<BibiSuggestion> = emptyList()

    fun initialize(mode: String, greeting: String, suggestions: List<BibiSuggestion>) {
        if (initializedMode == mode) return
        initializedMode = mode
        initialGreeting = greeting
        initialSuggestions = suggestions
        viewModelScope.launch {
            val restored = withContext(Dispatchers.IO) {
                val session = ChatSessionRepository.getMostRecentSession(appContext, mode)
                val messages = session?.let {
                    ChatSessionRepository.getSessionMessages(appContext, it.id)
                }.orEmpty()
                RestoredChat(
                    session = session,
                    messages = messages,
                    history = ChatSessionRepository.buildChatHistory(messages),
                    sessions = ChatSessionRepository.getAllSessions(appContext),
                )
            }
            currentSessionId = restored.session?.id
            chatHistory = restored.history
            lastQueries = chatHistory.map { it.question }.takeLast(5)
            val restoredMessages = restored.messages.map {
                BibiChatMessage(role = it.role, text = it.content)
            }
            _uiState.value = _uiState.value.copy(
                messages = restoredMessages.ifEmpty {
                    listOf(
                        BibiChatMessage(
                            role = "assistant",
                            text = greeting,
                            suggestions = suggestions,
                        )
                    )
                },
                isInitialized = true,
                sessions = restored.sessions,
                activeSessionId = restored.session?.id,
            )
        }
    }

    fun startNewChat() {
        if (_uiState.value.isLoading) return
        currentSessionId = null
        resetConversation()
    }

    fun openChat(sessionId: Long) {
        if (_uiState.value.isLoading || sessionId == currentSessionId) return
        viewModelScope.launch {
            val restored = withContext(Dispatchers.IO) {
                val messages = ChatSessionRepository.getSessionMessages(appContext, sessionId)
                messages to ChatSessionRepository.buildChatHistory(messages)
            }
            currentSessionId = sessionId
            chatHistory = restored.second
            lastQueries = chatHistory.map { it.question }.takeLast(5)
            _uiState.value = _uiState.value.copy(
                messages = restored.first.map {
                    BibiChatMessage(role = it.role, text = it.content)
                },
                activeSessionId = sessionId,
                completedExchanges = 0,
            )
        }
    }

    fun deleteCurrentChat() {
        if (_uiState.value.isLoading) return
        val sessionId = currentSessionId
        currentSessionId = null
        resetConversation()
        if (sessionId != null && sessionId > 0) {
            viewModelScope.launch(Dispatchers.IO) {
                ChatSessionRepository.deleteSession(appContext, sessionId)
                val sessions = ChatSessionRepository.getAllSessions(appContext)
                _uiState.value = _uiState.value.copy(sessions = sessions)
            }
        }
    }

    private fun resetConversation() {
        chatHistory = emptyList()
        lastQueries = emptyList()
        _uiState.value = _uiState.value.copy(
            messages = listOf(
                BibiChatMessage(
                    role = "assistant",
                    text = initialGreeting,
                    suggestions = initialSuggestions,
                )
            ),
            completedExchanges = 0,
            activeSessionId = null,
        )
    }

    fun sendQuestion(
        question: String,
        bibiContext: BibiContext,
        userName: String?,
        forceRemote: Boolean = false,
    ) {
        val query = question.trim()
        if (query.isEmpty() || _uiState.value.isLoading) return
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + BibiChatMessage("user", query),
            isLoading = true,
        )

        viewModelScope.launch {
            val resolvedContext = withContext(Dispatchers.IO) {
                BibiReferenceContextResolver.resolve(appContext, query, bibiContext)
            }
            val result = if (resolvedContext.error != null) {
                BibiQueryResult(
                    answer = resolvedContext.error,
                    confidence = Confidence.HIGH,
                    source = Source.LOCAL,
                    intent = "EXPLAIN_VERSE",
                )
            } else {
                withContext(Dispatchers.IO) {
                    orchestrator.answer(
                        context = appContext,
                        question = query,
                        bibiContext = resolvedContext.context,
                        userName = userName,
                        chatHistory = chatHistory,
                        lastQueries = lastQueries,
                        forceRemote = forceRemote,
                    )
                }
            }
            val displayAnswer = buildString {
                append(result.answer)
                result.disclaimer?.takeIf { it.isNotBlank() }?.let {
                    append("\n\nNota: ")
                    append(it)
                }
            }
            val exchange = ChatExchange(
                question = query,
                response = displayAnswer,
                resolvedTerm = result.resolvedTerm,
                intent = result.intent,
            )
            chatHistory = (chatHistory + exchange).takeLast(10)
            lastQueries = (lastQueries + query).takeLast(5)
            persistExchange(
                query = query,
                answer = displayAnswer,
                resolvedTerm = result.resolvedTerm,
                intent = result.intent,
                mode = if (bibiContext is BibiContext.Reader) "reader" else "study",
                title = when (bibiContext) {
                    is BibiContext.Reader -> bibiContext.book.ifBlank { "Lector" }
                    is BibiContext.Study -> bibiContext.title.ifBlank { "Estudio" }
                },
            )
            _uiState.value = _uiState.value.copy(
                messages = _uiState.value.messages + BibiChatMessage(
                    role = "assistant",
                    text = displayAnswer,
                    suggestions = result.suggestions,
                    suggestedBlocks = result.suggestedBlocks,
                    contextPassages = resolvedContext.explicitPassages,
                ),
                isLoading = false,
                completedExchanges = _uiState.value.completedExchanges + 1,
            )
        }
    }

    private suspend fun persistExchange(
        query: String,
        answer: String,
        resolvedTerm: String?,
        intent: String,
        mode: String,
        title: String,
    ) = withContext(Dispatchers.IO) {
        var sessionId = currentSessionId
        if (sessionId == null || sessionId <= 0) {
            sessionId = ChatSessionRepository.createSession(
                context = appContext,
                title = title,
                firstQuery = query,
                mode = mode,
            )
            currentSessionId = sessionId
        }
        if (sessionId > 0) {
            ChatSessionRepository.saveUserMessage(appContext, sessionId, query)
            ChatSessionRepository.saveAssistantMessage(
                context = appContext,
                sessionId = sessionId,
                content = answer,
                resolvedTerm = resolvedTerm,
                intent = intent,
            )
            ChatSessionRepository.updateSessionTimestamp(appContext, sessionId)
            val sessions = ChatSessionRepository.getAllSessions(appContext)
            _uiState.value = _uiState.value.copy(
                sessions = sessions,
                activeSessionId = sessionId,
            )
        }
    }

    class Factory(
        private val context: Context,
        private val orchestrator: BibiOrchestrator = BibiOrchestrator(),
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BibiViewModel(context, orchestrator) as T
        }
    }
}

private data class RestoredChat(
    val session: ChatSession?,
    val messages: List<com.cristiancogollo.biblion.feature.bibi.data.ChatMessage>,
    val history: List<ChatExchange>,
    val sessions: List<ChatSession>,
)

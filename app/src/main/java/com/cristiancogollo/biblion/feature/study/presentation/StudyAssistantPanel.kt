package com.cristiancogollo.biblion

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cristiancogollo.biblion.feature.bibi.BibiSuggestion
import com.cristiancogollo.biblion.feature.bibi.ChatExchange
import com.cristiancogollo.biblion.feature.bibi.data.BibiHistoryRepository
import com.cristiancogollo.biblion.feature.bibi.data.ChatSession
import com.cristiancogollo.biblion.feature.bibi.data.ChatSessionRepository
import com.cristiancogollo.biblion.ui.theme.BiblionNavy
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.Normalizer

private enum class StudyAssistantAuthor {
    USER,
    ASSISTANT
}

private data class StudyAssistantChatMessage(
    val id: String = CuidGenerator.create(),
    val author: StudyAssistantAuthor,
    val text: String,
    val suggestions: List<BibiSuggestion> = emptyList()
)

private val DEFAULT_SUGGESTIONS = listOf(
    BibiSuggestion("Buscar un personaje", "¿quién fue Moisés?"),
    BibiSuggestion("Buscar un lugar", "¿dónde queda Jerusalén?"),
    BibiSuggestion("Definir un concepto", "¿qué significa pacto?"),
    BibiSuggestion("Explicar este versículo", "explícame este versículo")
)

@Composable
fun StudyAssistantOverlay(
    studyTitle: String,
    studyTags: List<String>,
    selectedText: String,
    currentOutline: List<String> = emptyList(),
    notes: List<String> = emptyList(),
    currentUserName: String? = null,
    onInsertNote: ((String) -> Unit)?,
    onInsertReflection: ((topic: String, text: String) -> Unit)?,
    modifier: Modifier = Modifier
) {
    BibiAssistantOverlay(
        mode = StudyAssistantMode.STUDY,
        studyTitle = studyTitle,
        studyTags = studyTags,
        selectedText = selectedText,
        currentOutline = currentOutline,
        notes = notes,
        currentUserName = currentUserName,
        initialMessage = "Hola, soy Bibi, tu asistente de estudio en Biblion. Puedo ayudarte con ideas, pasajes relacionados, contexto bíblico y reflexiones para tu enseñanza.",
        inputPlaceholder = "Pregunta sobre tu enseñanza...",
        onInsertNote = onInsertNote,
        onInsertReflection = onInsertReflection,
        modifier = modifier
    )
}

@Composable
fun ReaderAssistantOverlay(
    bookName: String?,
    chapter: Int,
    selectedText: String,
    currentUserName: String? = null,
    onOpen: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val title = listOfNotNull(bookName, chapter.takeIf { it > 0 }?.let { "capítulo $it" })
        .joinToString(" ")
    BibiAssistantOverlay(
        mode = StudyAssistantMode.READER,
        studyTitle = title,
        studyTags = emptyList(),
        selectedText = selectedText,
        currentOutline = emptyList(),
        notes = emptyList(),
        currentUserName = currentUserName,
        initialMessage = "Hola, soy Bibi, tu asistente bíblico en Biblion. Puedo responder preguntas sencillas sobre el pasaje que estás leyendo.",
        inputPlaceholder = "Pregunta sobre este pasaje...",
        onInsertNote = null,
        onInsertReflection = null,
        onOpen = onOpen,
        modifier = modifier
    )
}

@Composable
private fun BibiAssistantOverlay(
    mode: StudyAssistantMode,
    studyTitle: String,
    studyTags: List<String>,
    selectedText: String,
    currentOutline: List<String>,
    notes: List<String>,
    currentUserName: String?,
    initialMessage: String,
    inputPlaceholder: String,
    onInsertNote: ((String) -> Unit)?,
    onInsertReflection: ((topic: String, text: String) -> Unit)?,
    onOpen: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val fallbackUserName = remember {
        FirebaseAuth.getInstance().currentUser?.let { user ->
            user.displayName?.takeIf { it.isNotBlank() }
                ?: user.email?.substringBefore("@")
        }
    }
    val personalizedInitialMessage = remember(initialMessage, currentUserName, fallbackUserName) {
        personalizeBibiGreeting(initialMessage, currentUserName ?: fallbackUserName)
    }
    val initialAssistantMessage = remember(personalizedInitialMessage) {
        StudyAssistantChatMessage(
            author = StudyAssistantAuthor.ASSISTANT,
            text = personalizedInitialMessage,
            suggestions = DEFAULT_SUGGESTIONS
        )
    }
    var isOpen by rememberSaveable { mutableStateOf(false) }
    var input by rememberSaveable { mutableStateOf("") }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var availableBibleVersions by remember { mutableStateOf<List<StudyAssistantBibleVersion>>(emptyList()) }
    var selectedBibleVersion by remember { mutableStateOf("rv1960") }
    val assistantRepository = remember(context) { HttpStudyAssistantRepository(appContext = context.applicationContext) }
    val messages = remember(personalizedInitialMessage) {
        mutableStateOf(listOf(initialAssistantMessage))
    }
    var recentQueries by remember { mutableStateOf<List<String>>(emptyList()) }
    var chatHistory by remember { mutableStateOf<List<ChatExchange>>(emptyList()) }
    var currentSessionId by remember { mutableStateOf<Long?>(null) }
    var showHistory by remember { mutableStateOf(false) }
    var historySessions by remember { mutableStateOf<List<ChatSession>>(emptyList()) }
    var skipLastQueries by remember { mutableStateOf(false) }

    fun sendQuestion(question: String) {
        if (question.isBlank() || isLoading) return
        messages.value = messages.value + StudyAssistantChatMessage(
            author = StudyAssistantAuthor.USER,
            text = question
        )
        input = ""
        isLoading = true
        scope.launch {
            val recentHistory = if (skipLastQueries) emptyList()
                else BibiHistoryRepository.getRecentQueries(context, 5)
            if (skipLastQueries) skipLastQueries = false

            // Detectar intención (fuente única, también se envía al Worker)
            val intent = com.cristiancogollo.biblion.feature.bibi.KnowledgeEngine.detectIntent(question)
            val needsChapterContext = intent in setOf(
                com.cristiancogollo.biblion.feature.bibi.KnowledgeEngine.BibiIntent.EXPLAIN_VERSE,
                com.cristiancogollo.biblion.feature.bibi.KnowledgeEngine.BibiIntent.RELATED,
                com.cristiancogollo.biblion.feature.bibi.KnowledgeEngine.BibiIntent.FALLBACK,
                com.cristiancogollo.biblion.feature.bibi.KnowledgeEngine.BibiIntent.DIVE_DEEPER
            )

            // Crear sesión en la primera pregunta
            if (currentSessionId == null) {
                currentSessionId = ChatSessionRepository.createSession(
                    context = context,
                    title = question.take(60),
                    firstQuery = question,
                    mode = mode.name
                )
            } else {
                ChatSessionRepository.updateSessionTimestamp(context, currentSessionId!!)
            }
            ChatSessionRepository.saveUserMessage(context, currentSessionId!!, question)

            val response = assistantRepository.ask(
                StudyAssistantRequest(
                    question = question,
                    studyTitle = if (needsChapterContext) studyTitle else "",
                    studyTags = studyTags,
                    selectedText = if (needsChapterContext) selectedText else "",
                    mode = mode,
                    intent = mapBibiIntentToApi(intent),
                    currentOutline = if (needsChapterContext) currentOutline else emptyList(),
                    notes = if (needsChapterContext) notes else emptyList(),
                    bibleVersions = availableBibleVersions,
                    bibleVersion = selectedBibleVersion,
                    userName = currentUserName ?: fallbackUserName,
                    lastQueries = recentHistory,
                    chatHistory = chatHistory
                )
            )
            val suggestions = response.bibiResponse?.suggestions ?: DEFAULT_SUGGESTIONS
            messages.value = messages.value + StudyAssistantChatMessage(
                author = StudyAssistantAuthor.ASSISTANT,
                text = response.answer,
                suggestions = suggestions
            )
            if (response.bibiResponse != null) {
                BibiHistoryRepository.save(
                    context = context,
                    query = question,
                    response = response.bibiResponse,
                    userContext = com.cristiancogollo.biblion.feature.bibi.BibiUserContext(
                        userName = currentUserName ?: fallbackUserName,
                        bibleVersion = selectedBibleVersion
                    )
                )
            }

            // Issue 2: solo guardar resolvedTerm si la respuesta tiene confianza real
            // (evita contaminar chatHistory con "No encontré X" como término válido)
            val resolvedTerm = response.bibiResponse
                ?.takeIf { it.confidence != com.cristiancogollo.biblion.feature.bibi.Confidence.LOW }
                ?.title?.let { title ->
                    title.removePrefix("Lugar: ").removePrefix("Sobre ").takeIf { it.length > 2 }
                }

            ChatSessionRepository.saveAssistantMessage(
                context = context,
                sessionId = currentSessionId!!,
                content = response.answer,
                resolvedTerm = resolvedTerm,
                intent = intent.name
            )

            // Issue 4: actualizar chatHistory también cuando el Worker responde
            // (DIVE_DEEPER o FALLBACK donde bibiResponse es null) para no perder el hilo
            val exchangeIntent = intent.name
            chatHistory = (chatHistory + ChatExchange(
                question = question,
                response = response.answer,
                resolvedTerm = resolvedTerm,
                intent = exchangeIntent
            )).takeLast(10)
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        availableBibleVersions = BibleRepository.getAvailableVersions(context)
            .map { version ->
                StudyAssistantBibleVersion(
                    key = version.key,
                    label = version.label
                )
            }
        selectedBibleVersion = BibleRepository.getSelectedVersionKey(context)
    }

    LaunchedEffect(messages.value.isEmpty()) {
        if (messages.value.isEmpty() || messages.value.size == 1) {
            recentQueries = BibiHistoryRepository.getRecentQueries(context, 5)
        }
    }

    Box(modifier = modifier) {
        if (isOpen) {
            StudyAssistantPanel(
                messages = messages.value,
                input = input,
                isLoading = isLoading,
                recentQueries = recentQueries,
                inputPlaceholder = inputPlaceholder,
                onInputChange = { input = it },
                onClose = { isOpen = false },
                onReset = {
                    messages.value = listOf(
                        initialAssistantMessage.copy(id = CuidGenerator.create())
                    )
                    input = ""
                    isLoading = false
                    currentSessionId = null
                    chatHistory = emptyList()
                    skipLastQueries = true
                },
                onClear = {
                    scope.launch {
                        BibiHistoryRepository.clearHistory(context)
                        currentSessionId?.let { ChatSessionRepository.deleteSession(context, it) }
                    }
                    messages.value = emptyList()
                    input = ""
                    isLoading = false
                    currentSessionId = null
                    chatHistory = emptyList()
                },
                onOpenHistory = {
                    scope.launch {
                        historySessions = ChatSessionRepository.getAllSessions(context)
                        showHistory = true
                    }
                },
                onSend = { sendQuestion(input.trim()) },
                onSuggestionClick = { query -> sendQuestion(query) },
                onInsertNote = onInsertNote,
                onInsertReflection = onInsertReflection,
                modifier = Modifier.align(Alignment.BottomEnd)
            )

            if (showHistory) {
                ChatHistoryDialog(
                    sessions = historySessions,
                    currentSessionId = currentSessionId,
                    onSelect = { sessionId ->
                        scope.launch {
                            val savedMessages = ChatSessionRepository.getSessionMessages(context, sessionId)
                            if (savedMessages.isNotEmpty()) {
                                val restored = mutableListOf<StudyAssistantChatMessage>()
                                restored.add(initialAssistantMessage.copy(id = CuidGenerator.create()))
                                for (msg in savedMessages) {
                                    val author = if (msg.role == "user") {
                                        StudyAssistantAuthor.USER
                                    } else {
                                        StudyAssistantAuthor.ASSISTANT
                                    }
                                    restored.add(
                                        StudyAssistantChatMessage(
                                            author = author,
                                            text = msg.content
                                        )
                                    )
                                }
                                messages.value = restored
                                chatHistory = ChatSessionRepository.buildChatHistory(savedMessages)
                                currentSessionId = sessionId
                                ChatSessionRepository.updateSessionTimestamp(context, sessionId)
                            }
                            showHistory = false
                        }
                    },
                    onDelete = { sessionId ->
                        scope.launch {
                            ChatSessionRepository.deleteSession(context, sessionId)
                            historySessions = ChatSessionRepository.getAllSessions(context)
                        }
                    },
                    onDismiss = { showHistory = false }
                )
            }
        } else {
            FloatingActionButton(
                onClick = {
                    isOpen = true
                    onOpen()
                    // Cargar la sesión más reciente al abrir, si existe
                    scope.launch {
                        val lastSession = ChatSessionRepository.getMostRecentSession(context)
                        if (lastSession != null) {
                            val savedMessages = ChatSessionRepository.getSessionMessages(context, lastSession.id)
                            if (savedMessages.isNotEmpty()) {
                                val restored = mutableListOf<StudyAssistantChatMessage>()
                                restored.add(initialAssistantMessage.copy(id = CuidGenerator.create()))
                                for (msg in savedMessages) {
                                    val author = if (msg.role == "user") {
                                        StudyAssistantAuthor.USER
                                    } else {
                                        StudyAssistantAuthor.ASSISTANT
                                    }
                                    restored.add(
                                        StudyAssistantChatMessage(
                                            author = author,
                                            text = msg.content
                                        )
                                    )
                                }
                                messages.value = restored
                                chatHistory = ChatSessionRepository.buildChatHistory(savedMessages)
                                currentSessionId = lastSession.id
                            }
                        }
                    }
                },
                modifier = Modifier.align(Alignment.BottomEnd),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Image(
                    painter = painterResource(id = R.drawable.bibi_logo),
                    contentDescription = "Abrir Bibi",
                    modifier = Modifier.size(50.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

private fun personalizeBibiGreeting(initialMessage: String, userName: String?): String {
    val cleanName = userName
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?.substringBefore(" ")
        ?: return initialMessage
    return initialMessage.replace("Hola,", "Hola $cleanName,")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StudyAssistantPanel(
    messages: List<StudyAssistantChatMessage>,
    input: String,
    isLoading: Boolean,
    recentQueries: List<String>,
    inputPlaceholder: String,
    onInputChange: (String) -> Unit,
    onClose: () -> Unit,
    onReset: () -> Unit,
    onClear: () -> Unit,
    onOpenHistory: () -> Unit,
    onSend: () -> Unit,
    onSuggestionClick: (String) -> Unit,
    onInsertNote: ((String) -> Unit)?,
    onInsertReflection: ((topic: String, text: String) -> Unit)?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .widthIn(min = 320.dp, max = 420.dp)
            .fillMaxHeight(0.72f),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(56.dp),
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.bibi_logo),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = "Bibi",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onReset) {
                        Icon(Icons.Default.RestartAlt, contentDescription = "Nuevo chat")
                    }
                    IconButton(onClick = onOpenHistory) {
                        Icon(Icons.Default.History, contentDescription = "Historial de chats")
                    }
                    IconButton(onClick = onClear) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Eliminar chat")
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar asistente")
                    }
                }
            }

            if (messages.isEmpty()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Chat eliminado. Escribe una pregunta o inicia un nuevo chat con Bibi.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
                    )
                    if (recentQueries.isNotEmpty()) {
                        Spacer(modifier = Modifier.size(16.dp))
                        Text(
                            text = "Consultas recientes:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            recentQueries.forEach { query ->
                                AssistChip(
                                    onClick = { onSuggestionClick(query) },
                                    label = { Text(query, maxLines = 1) }
                                )
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages, key = { it.id }) { message ->
                        StudyAssistantMessageBubble(
                            message = message,
                            onInsertNote = onInsertNote,
                            onInsertReflection = onInsertReflection,
                            onSuggestionClick = onSuggestionClick
                        )
                    }
                    if (isLoading) {
                        item(key = "bibi-loading") {
                            StudyAssistantLoadingBubble()
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = onInputChange,
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading,
                    minLines = 1,
                    maxLines = 4,
                    placeholder = {
                        Text(if (isLoading) "Bibi está pensando..." else inputPlaceholder)
                    }
                )
                IconButton(onClick = onSend, enabled = !isLoading) {
                    Icon(Icons.Default.Send, contentDescription = "Enviar pregunta")
                }
            }
        }
    }
}

@Composable
private fun StudyAssistantLoadingBubble() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.72f),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = 0.dp,
                bottomEnd = 16.dp
            )
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Bibi está pensando...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StudyAssistantMessageBubble(
    message: StudyAssistantChatMessage,
    onInsertNote: ((String) -> Unit)?,
    onInsertReflection: ((topic: String, text: String) -> Unit)?,
    onSuggestionClick: (String) -> Unit
) {
    val isUser = message.author == StudyAssistantAuthor.USER
    val bubbleColor = if (isUser) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f)
    }
    val bubbleShape = if (isUser) {
        RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomStart = 16.dp,
            bottomEnd = 0.dp
        )
    } else {
        RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomStart = 0.dp,
            bottomEnd = 16.dp
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(if (isUser) 0.86f else 0.94f),
            color = bubbleColor,
            shape = bubbleShape
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(10.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Optional "Nota" / "Reflexión" chips (solo en modo estudio, en respuestas de Bibi)
        if (!isUser && (onInsertNote != null || onInsertReflection != null)) {
            Row(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (onInsertNote != null) {
                    AssistChip(
                        onClick = { onInsertNote(message.text) },
                        label = { Text("Nota") },
                        leadingIcon = { Icon(Icons.Default.NoteAdd, contentDescription = null) }
                    )
                }
                if (onInsertReflection != null) {
                    AssistChip(
                        onClick = {
                            onInsertReflection("Bibi", message.text)
                        },
                        label = { Text("Reflexión") },
                        leadingIcon = { Icon(Icons.Default.Lightbulb, contentDescription = null) }
                    )
                }
            }
        }

        // Suggestion chips (solo en respuestas de Bibi)
        if (!isUser && message.suggestions.isNotEmpty()) {
            Spacer(modifier = Modifier.size(4.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                message.suggestions.forEach { suggestion ->
                    AssistChip(
                        onClick = { onSuggestionClick(suggestion.query) },
                        label = { Text(suggestion.label, maxLines = 1) }
                    )
                }
            }
        }
    }
}

/**
 * Mapea BibiIntent (interno, usado por la lógica local) a StudyAssistantIntent
 * (enviado al Worker en la API). Esto garantiza que el Worker reciba el mismo
 * intent que la lógica local detectó.
 */
private fun mapBibiIntentToApi(intent: com.cristiancogollo.biblion.feature.bibi.KnowledgeEngine.BibiIntent): StudyAssistantIntent {
    return when (intent) {
        com.cristiancogollo.biblion.feature.bibi.KnowledgeEngine.BibiIntent.DEFINE -> StudyAssistantIntent.DEFINE
        com.cristiancogollo.biblion.feature.bibi.KnowledgeEngine.BibiIntent.WHO,
        com.cristiancogollo.biblion.feature.bibi.KnowledgeEngine.BibiIntent.WHERE -> StudyAssistantIntent.EXPLAIN
        com.cristiancogollo.biblion.feature.bibi.KnowledgeEngine.BibiIntent.RELATED -> StudyAssistantIntent.CROSS_REFERENCE
        com.cristiancogollo.biblion.feature.bibi.KnowledgeEngine.BibiIntent.ORIGINAL_LANG -> StudyAssistantIntent.EXPLAIN
        com.cristiancogollo.biblion.feature.bibi.KnowledgeEngine.BibiIntent.EXPLAIN_VERSE -> StudyAssistantIntent.EXPLAIN
        com.cristiancogollo.biblion.feature.bibi.KnowledgeEngine.BibiIntent.GREETING -> StudyAssistantIntent.QUESTION
        com.cristiancogollo.biblion.feature.bibi.KnowledgeEngine.BibiIntent.DIVE_DEEPER -> StudyAssistantIntent.QUESTION
        com.cristiancogollo.biblion.feature.bibi.KnowledgeEngine.BibiIntent.FALLBACK -> StudyAssistantIntent.QUESTION
    }
}

@Composable
private fun ChatHistoryDialog(
    sessions: List<ChatSession>,
    currentSessionId: Long?,
    onSelect: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Historial de chats") },
        text = {
            if (sessions.isEmpty()) {
                Text(
                    "Aún no tienes chats guardados. Inicia una conversación con Bibi para empezar.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
                )
            } else {
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp)
                ) {
                    items(sessions, key = { it.id }) { session ->
                        ChatHistoryRow(
                            session = session,
                            isCurrent = session.id == currentSessionId,
                            onSelect = { onSelect(session.id) },
                            onDelete = { onDelete(session.id) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}

@Composable
private fun ChatHistoryRow(
    session: ChatSession,
    isCurrent: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onSelect),
            color = if (isCurrent) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
            } else {
                androidx.compose.ui.graphics.Color.Transparent
            },
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                Text(
                    text = session.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.size(2.dp))
                Text(
                    text = formatSessionDate(session.updatedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
            }
        }
        androidx.compose.material3.IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.DeleteSweep,
                contentDescription = "Eliminar sesión",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

private fun formatSessionDate(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000 -> "hace un momento"
        diff < 3_600_000 -> "hace ${diff / 60_000} min"
        diff < 86_400_000 -> "hace ${diff / 3_600_000} h"
        diff < 7 * 86_400_000 -> "hace ${diff / 86_400_000} días"
        else -> {
            val sdf = java.text.SimpleDateFormat("dd MMM", java.util.Locale.getDefault())
            sdf.format(java.util.Date(timestamp))
        }
    }
}

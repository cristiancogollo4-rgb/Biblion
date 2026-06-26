package com.cristiancogollo.biblion.feature.bibi.ui

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cristiancogollo.biblion.R
import com.cristiancogollo.biblion.feature.bibi.data.ChatSessionRepository
import com.cristiancogollo.biblion.feature.bibi.engine.KnowledgeEngine
import com.cristiancogollo.biblion.feature.bibi.model.BibiResponse
import com.cristiancogollo.biblion.feature.bibi.model.BibiSuggestion
import com.cristiancogollo.biblion.feature.bibi.model.ChatExchange
import com.cristiancogollo.biblion.feature.bibi.model.Confidence
import com.cristiancogollo.biblion.GuidedTutorialTargets
import com.cristiancogollo.biblion.guidedTutorialTarget
import com.cristiancogollo.biblion.ui.theme.BiblionBluePrimary
import kotlinx.coroutines.launch

/**
 * Overlay de Bibi para el modo lector.
 *
 * Muestra un FAB con el logo de Bibi que abre un panel de chat flotante.
 * Usa KnowledgeEngine para respuestas locales y WorkerClient como fallback online.
 */
@Composable
fun BibiReaderOverlay(
    bookName: String?,
    chapter: Int,
    currentUserName: String?,
    tutorialTargetBounds: MutableMap<String, androidx.compose.ui.geometry.Rect>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var isOpen by remember { mutableStateOf(false) }
    var messages by remember { mutableStateOf<List<ChatMessageUi>>(emptyList()) }
    var input by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var chatHistory by remember { mutableStateOf<List<ChatExchange>>(emptyList()) }
    var lastQueries by remember { mutableStateOf<List<String>>(emptyList()) }
    var currentSessionId by remember { mutableStateOf<Long?>(null) }

    fun scrollToBottom() {
        if (messages.isNotEmpty()) {
            scope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    fun sendQuestion(overrideQuery: String? = null) {
        val q = overrideQuery?.trim() ?: input.trim()
        if (q.isBlank() || isLoading) return

        input = ""
        messages = messages + ChatMessageUi(role = "user", text = q)
        isLoading = true
        scrollToBottom()

        scope.launch {
            val result = processBibiQuery(
                context = context,
                question = q,
                bookName = bookName,
                chapter = chapter,
                currentUserName = currentUserName,
                chatHistory = chatHistory,
                lastQueries = lastQueries,
                onLocalHistorySave = { query, response, resolvedTerm, intent ->
                    chatHistory = chatHistory + ChatExchange(
                        question = query,
                        response = response,
                        resolvedTerm = resolvedTerm,
                        intent = intent
                    )
                    lastQueries = (lastQueries + query).takeLast(5)
                }
            )

            messages = messages + ChatMessageUi(
                role = "assistant",
                text = result.first,
                suggestions = result.second
            )
            isLoading = false
            scrollToBottom()

            if (currentSessionId == null) {
                currentSessionId = ChatSessionRepository.createSession(
                    context,
                    title = bookName ?: "Lector",
                    firstQuery = q,
                    mode = "reader"
                )
            }
            val sid = currentSessionId
            if (sid != null && sid > 0) {
                ChatSessionRepository.saveUserMessage(context, sid, q)
                ChatSessionRepository.saveAssistantMessage(context, sid, result.first, null, null)
                ChatSessionRepository.updateSessionTimestamp(context, sid)
            }
        }
    }

    val greetingText = remember(currentUserName) {
        val name = currentUserName
        if (!name.isNullOrBlank()) {
            context.getString(R.string.bibi_greeting, name)
        } else {
            context.getString(R.string.bibi_greeting_no_name)
        }
    }

    LaunchedEffect(isOpen) {
        if (isOpen && messages.isEmpty()) {
            messages = listOf(
                ChatMessageUi(
                    role = "assistant",
                    text = greetingText,
                    suggestions = listOf(
                        BibiSuggestion("¿Quién fue Moisés?", "¿quién fue Moisés?"),
                        BibiSuggestion("¿Dónde queda Jerusalén?", "¿dónde queda Jerusalén?"),
                        BibiSuggestion("¿Qué significa pacto?", "¿qué significa pacto?")
                    )
                )
            )
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = isOpen,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it }),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxHeight(0.75f)
                    .width(320.dp)
                    .padding(8.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    BibiChatHeader(
                        onClose = { isOpen = false }
                    )

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(messages) { msg ->
                            BibiChatBubble(
                                msg = msg,
                                onSuggestionClick = { query -> sendQuestion(query) }
                            )
                        }
                        if (isLoading) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp,
                                        color = BiblionBluePrimary
                                    )
                                }
                            }
                        }
                    }

                    BibiChatInput(
                        value = input,
                        onValueChange = { input = it },
                        onSend = { sendQuestion() },
                        isLoading = isLoading
                    )
                }
            }
        }

        if (!isOpen) {
            FloatingActionButton(
                onClick = { isOpen = true },
                containerColor = BiblionBluePrimary,
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 90.dp)
                    .size(56.dp)
                    .guidedTutorialTarget(
                        GuidedTutorialTargets.READER_BIBI_BUTTON,
                        tutorialTargetBounds
                    )
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.bibi_logo),
                    contentDescription = stringResource(R.string.cd_bibi)
                )
            }
        }
    }
}

private data class ChatMessageUi(
    val role: String,
    val text: String,
    val suggestions: List<BibiSuggestion> = emptyList()
)

@Composable
private fun BibiChatHeader(onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BiblionBluePrimary)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = R.drawable.bibi_logo),
            contentDescription = null,
            modifier = Modifier.size(28.dp),
            tint = Color.White
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Bibi",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Cerrar",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun BibiChatBubble(
    msg: ChatMessageUi,
    onSuggestionClick: (String) -> Unit
) {
    val isUser = msg.role == "user"
    val bubbleColor = if (isUser) {
        BiblionBluePrimary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = if (isUser) {
        Color.White
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val shape = if (isUser) {
        RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Surface(
            shape = shape,
            color = bubbleColor,
            modifier = Modifier.widthIn(max = 260.dp)
        ) {
            Text(
                text = msg.text,
                color = textColor,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }

        if (!isUser && msg.suggestions.isNotEmpty()) {
            Column(
                modifier = Modifier.padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                msg.suggestions.forEach { suggestion ->
                    SuggestionChip(
                        onClick = { onSuggestionClick(suggestion.query) },
                        label = {
                            Text(
                                text = suggestion.label,
                                maxLines = 1,
                                fontSize = 12.sp
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BibiChatInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    isLoading: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Pregunta sobre este pasaje...", fontSize = 13.sp) },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BiblionBluePrimary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )
        Spacer(modifier = Modifier.width(6.dp))
        IconButton(
            onClick = { onSend() },
            enabled = value.isNotBlank() && !isLoading,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    if (value.isNotBlank() && !isLoading) BiblionBluePrimary
                    else MaterialTheme.colorScheme.surfaceVariant
                )
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Send,
                contentDescription = "Enviar",
                tint = if (value.isNotBlank() && !isLoading) Color.White
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private suspend fun processBibiQuery(
    context: Context,
    question: String,
    bookName: String?,
    chapter: Int,
    currentUserName: String?,
    chatHistory: List<ChatExchange>,
    lastQueries: List<String>,
    onLocalHistorySave: (String, String, String?, String) -> Unit
): Pair<String, List<BibiSuggestion>> {
    val userContext = KnowledgeEngine.UserContext(
        book = bookName.orEmpty(),
        chapter = chapter,
        userName = currentUserName,
        lastQueries = lastQueries,
        chatHistory = chatHistory
    )

    val localResponse = KnowledgeEngine.answer(context, question, userContext)

    if (localResponse != null && localResponse.confidence != Confidence.LOW) {
        val text = localResponse.buildChatText()
        onLocalHistorySave(question, text, localResponse.title, localResponse.source.name)
        return text to localResponse.suggestions
    }

    val onlineAnswer = WorkerClient.ask(
        question = question,
        bookName = bookName,
        chapter = chapter,
        chatHistory = chatHistory,
        lastQueries = lastQueries
    )

    if (!onlineAnswer.isNullOrBlank()) {
        onLocalHistorySave(question, onlineAnswer, null, "AI")
        return onlineAnswer to emptyList()
    }

    val fallback = localResponse?.buildChatText()
        ?: "No pude procesar tu pregunta. Intenta con algo como: ¿Quién fue Abraham?, ¿Dónde está Jerusalén?, o ¿Qué significa gracia?"
    onLocalHistorySave(question, fallback, null, "FALLBACK")
    return fallback to emptyList()
}

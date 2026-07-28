package com.cristiancogollo.biblion.feature.bibi.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cristiancogollo.biblion.R
import com.cristiancogollo.biblion.feature.bibi.model.BibiContext
import com.cristiancogollo.biblion.feature.bibi.model.BibiSuggestion

@Composable
fun BibiChatPanel(
    viewModelKey: String,
    mode: String,
    greeting: String,
    initialSuggestions: List<BibiSuggestion>,
    bibiContext: BibiContext,
    currentUserName: String?,
    subtitle: String,
    placeholder: String,
    onClose: () -> Unit,
    onCompletedExchange: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val bibiViewModel: BibiViewModel = viewModel(
        key = viewModelKey,
        factory = remember(context) { BibiViewModel.Factory(context) },
    )
    val uiState by bibiViewModel.uiState.collectAsState()
    var input by remember { mutableStateOf("") }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }

    LaunchedEffect(mode) {
        bibiViewModel.initialize(
            mode = mode,
            greeting = greeting,
            suggestions = initialSuggestions,
        )
    }
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.lastIndex)
        }
    }
    LaunchedEffect(uiState.completedExchanges) {
        if (uiState.completedExchanges > 0) onCompletedExchange()
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.bibi_logo),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(30.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Bibi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(
                onClick = { showHistory = true },
                enabled = !uiState.isLoading && uiState.sessions.isNotEmpty(),
            ) {
                Icon(Icons.Default.History, contentDescription = "Historial de chats")
            }
            IconButton(
                onClick = {
                    input = ""
                    bibiViewModel.startNewChat()
                },
                enabled = !uiState.isLoading,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Crear nuevo chat")
            }
            IconButton(
                onClick = { showDeleteConfirmation = true },
                enabled = !uiState.isLoading &&
                    uiState.messages.any { it.role == "user" },
            ) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = "Eliminar chat",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Cerrar Bibi")
            }
        }
        HorizontalDivider()
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(uiState.messages) { message ->
                BibiUnifiedChatMessage(
                    message = message,
                    onSendSuggestion = { suggestion ->
                        bibiViewModel.sendQuestion(
                            question = suggestion.query,
                            bibiContext = bibiContext,
                            userName = currentUserName,
                            forceRemote = suggestion.isAi,
                        )
                    },
                )
            }
            if (uiState.isLoading) {
                item {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(12.dp)
                            .size(24.dp),
                        strokeWidth = 2.dp,
                    )
                }
            }
        }
        HorizontalDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text(placeholder) },
                modifier = Modifier.weight(1f),
                maxLines = 3,
            )
            IconButton(
                enabled = input.isNotBlank() && !uiState.isLoading,
                onClick = {
                    val query = input.trim()
                    input = ""
                    bibiViewModel.sendQuestion(
                        question = query,
                        bibiContext = bibiContext,
                        userName = currentUserName,
                    )
                },
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar")
            }
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Eliminar conversación") },
            text = {
                Text("Se eliminarán permanentemente los mensajes de este chat.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmation = false
                        input = ""
                        bibiViewModel.deleteCurrentChat()
                    },
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancelar")
                }
            },
        )
    }

    if (showHistory) {
        AlertDialog(
            onDismissRequest = { showHistory = false },
            title = { Text("Historial de conversaciones") },
            text = {
                if (uiState.sessions.isEmpty()) {
                    Text("Todavía no tienes conversaciones guardadas.")
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(uiState.sessions, key = { it.id }) { session ->
                            Surface(
                                onClick = {
                                    showHistory = false
                                    input = ""
                                    bibiViewModel.openChat(session.id)
                                },
                                color = if (session.id == uiState.activeSessionId) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                contentColor = if (session.id == uiState.activeSessionId) {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(3.dp),
                                ) {
                                    Text(
                                        session.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        session.firstQuery,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 2,
                                    )
                                    Text(
                                        if (session.mode == "study") "Estudio" else "Lectura",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHistory = false }) {
                    Text("Cerrar")
                }
            },
        )
    }
}

@Composable
private fun BibiUnifiedChatMessage(
    message: BibiChatMessage,
    onSendSuggestion: (BibiSuggestion) -> Unit,
) {
    val isUser = message.role == "user"
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (!isUser && message.contextPassages.isNotEmpty()) {
            val first = message.contextPassages.first()
            val last = message.contextPassages.last()
            val reference = if (first.verse == last.verse) {
                first.reference
            } else {
                "${first.book} ${first.chapter}:${first.verse}-${last.verse}"
            }
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(0.94f),
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        reference,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        message.contextPassages.joinToString(" ") {
                            "${it.verse} ${it.text}"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        Surface(
            color = if (isUser) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            contentColor = if (isUser) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            shape = MaterialTheme.shapes.large,
        ) {
            Text(message.text, modifier = Modifier.padding(12.dp))
        }
        message.suggestions.forEach {
            OutlinedButton(onClick = { onSendSuggestion(it) }) {
                Text(it.label)
            }
        }
        message.suggestedBlocks.forEach { suggestion ->
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(0.94f),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        "Idea sugerida",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(suggestion, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

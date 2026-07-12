package com.cristiancogollo.biblion.feature.studydocs.ui.bibi

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cristiancogollo.biblion.feature.studydocs.model.StudyDoc
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import com.cristiancogollo.biblion.feature.studydocs.model.StyledText
import com.cristiancogollo.biblion.ui.theme.BiblionBluePrimary

data class BibiChatMessage(
    val id: String,
    val role: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibiOverlayPanel(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onInsertAsBlock: (StudyBlock) -> Unit,
    docContext: StudyDoc,
    modifier: Modifier = Modifier,
) {
    if (!isVisible) return
    
    var messages by remember { mutableStateOf(listOf<BibiChatMessage>()) }
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }
    
    Surface(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BiblionBluePrimary)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onPrimary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "📖",
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Bibi",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Text(
                            text = "Asistente bíblico",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
            
            // Messages
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                state = listState,
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(messages) { message ->
                    BibiMessageCard(
                        message = message,
                        onInsertAsBlock = onInsertAsBlock,
                    )
                }
            }
            
            // Input
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Pregúntale a Bibi...") },
                    maxLines = 3,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            val userMessage = BibiChatMessage(
                                id = System.currentTimeMillis().toString(),
                                role = "user",
                                content = inputText,
                            )
                            messages = messages + userMessage
                            
                            val response = generateBibiResponse(inputText, docContext)
                            val assistantMessage = BibiChatMessage(
                                id = (System.currentTimeMillis() + 1).toString(),
                                role = "assistant",
                                content = response,
                            )
                            messages = messages + assistantMessage
                            
                            inputText = ""
                        }
                    },
                    enabled = inputText.isNotBlank(),
                    shape = CircleShape,
                    modifier = Modifier.size(48.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BiblionBluePrimary,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Enviar",
                    )
                }
            }
        }
    }
}

@Composable
private fun BibiMessageCard(
    message: BibiChatMessage,
    onInsertAsBlock: (StudyBlock) -> Unit,
) {
    val isUser = message.role == "user"
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isUser) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = if (isUser) "Tú" else "Bibi",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (isUser) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    BiblionBluePrimary
                },
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isUser) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            
            if (!isUser && message.content.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        val noteBlock = StudyBlock.Note(
                            text = StyledText.plain(message.content)
                        )
                        onInsertAsBlock(noteBlock)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BiblionBluePrimary,
                    ),
                ) {
                    Text("Insertar como nota")
                }
            }
        }
    }
}

private fun generateBibiResponse(query: String, docContext: StudyDoc): String {
    val lowerQuery = query.lowercase()
    
    return when {
        lowerQuery.contains("versículo") || lowerQuery.contains("pasaje") -> {
            "Basado en el contexto de tu documento, te recomiendo considerar ${docContext.blocks.size} bloques de contenido. " +
            "¿Hay algún pasaje específico que te gustaría explorar?"
        }
        lowerQuery.contains("tema") || lowerQuery.contains("tópico") -> {
            "Analizando tu documento, veo que tienes ${docContext.blocks.size} bloques. " +
            "¿Te gustaría que te ayude a identificar el tema principal?"
        }
        lowerQuery.contains("resumen") -> {
            "Tu documento tiene ${docContext.blocks.size} bloques. " +
            "¿Quieres que te ayude a crear un resumen ejecutivo?"
        }
        else -> {
            "Entiendo tu pregunta sobre '${query}'. Basado en el contexto de tu documento con ${docContext.blocks.size} bloques, " +
            "te sugiero revisar los pasajes relevantes y considerar cómo se relacionan con tu tema principal. " +
            "¿Hay algo específico en lo que pueda ayudarte?"
        }
    }
}

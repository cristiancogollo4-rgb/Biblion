package com.cristiancogollo.biblion

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cristiancogollo.biblion.ui.theme.BiblionNavy

private enum class StudyAssistantAuthor {
    USER,
    ASSISTANT
}

private data class StudyAssistantChatMessage(
    val id: String = CuidGenerator.create(),
    val author: StudyAssistantAuthor,
    val text: String
)

@Composable
fun StudyAssistantOverlay(
    studyTitle: String,
    studyTags: List<String>,
    selectedText: String,
    onInsertNote: (String) -> Unit,
    onInsertReflection: (topic: String, text: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val initialAssistantMessage = remember {
        StudyAssistantChatMessage(
            author = StudyAssistantAuthor.ASSISTANT,
            text = "Hola, soy Bibi, tu asistente de estudio en Biblion. Puedes pedirme ideas, pasajes relacionados o una reflexion para tu ensenanza."
        )
    }
    var isOpen by rememberSaveable { mutableStateOf(false) }
    var input by rememberSaveable { mutableStateOf("") }
    val messages = remember {
        mutableStateOf(listOf(initialAssistantMessage))
    }

    Box(modifier = modifier) {
        if (isOpen) {
            StudyAssistantPanel(
                messages = messages.value,
                input = input,
                onInputChange = { input = it },
                onClose = { isOpen = false },
                onReset = {
                    messages.value = listOf(
                        initialAssistantMessage.copy(id = CuidGenerator.create())
                    )
                    input = ""
                },
                onClear = {
                    messages.value = emptyList()
                    input = ""
                },
                onSend = {
                    val question = input.trim()
                    if (question.isBlank()) return@StudyAssistantPanel
                    val answer = buildStudyAssistantLocalAnswer(
                        question = question,
                        studyTitle = studyTitle,
                        studyTags = studyTags,
                        selectedText = selectedText
                    )
                    messages.value = messages.value + StudyAssistantChatMessage(
                        author = StudyAssistantAuthor.USER,
                        text = question
                    ) + StudyAssistantChatMessage(
                        author = StudyAssistantAuthor.ASSISTANT,
                        text = answer
                    )
                    input = ""
                },
                onInsertNote = onInsertNote,
                onInsertReflection = onInsertReflection,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        } else {
            FloatingActionButton(
                onClick = { isOpen = true },
                modifier = Modifier.align(Alignment.BottomEnd),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Image(
                    painter = painterResource(id = R.drawable.bibi_logo),
                    contentDescription = "Abrir Bibi",
                    modifier = Modifier.size(70.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

@Composable
private fun StudyAssistantPanel(
    messages: List<StudyAssistantChatMessage>,
    input: String,
    onInputChange: (String) -> Unit,
    onClose: () -> Unit,
    onReset: () -> Unit,
    onClear: () -> Unit,
    onSend: () -> Unit,
    onInsertNote: (String) -> Unit,
    onInsertReflection: (topic: String, text: String) -> Unit,
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
                            modifier = Modifier.padding(2.dp),
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
                    IconButton(onClick = onClear) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Eliminar chat")
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar asistente")
                    }
                }
            }

            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Chat eliminado. Escribe una pregunta o inicia un nuevo chat con Bibi.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
                    )
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
                            onInsertReflection = onInsertReflection
                        )
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
                    minLines = 1,
                    maxLines = 4,
                    placeholder = { Text("Pregunta sobre tu ensenanza...") }
                )
                IconButton(onClick = onSend) {
                    Icon(Icons.Default.Send, contentDescription = "Enviar pregunta")
                }
            }
        }
    }
}

@Composable
private fun StudyAssistantMessageBubble(
    message: StudyAssistantChatMessage,
    onInsertNote: (String) -> Unit,
    onInsertReflection: (topic: String, text: String) -> Unit
) {
    val isUser = message.author == StudyAssistantAuthor.USER
    val bubbleColor = if (isUser) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(if (isUser) 0.86f else 0.94f),
            color = bubbleColor,
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(10.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        if (!isUser) {
            Row(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AssistChip(
                    onClick = { onInsertNote(message.text) },
                    label = { Text("Nota") },
                    leadingIcon = { Icon(Icons.Default.NoteAdd, contentDescription = null) }
                )
                AssistChip(
                    onClick = {
                        onInsertReflection("Bibi", message.text)
                    },
                    label = { Text("Reflexion") },
                    leadingIcon = { Icon(Icons.Default.Lightbulb, contentDescription = null) }
                )
            }
        }
    }
}

private fun buildStudyAssistantLocalAnswer(
    question: String,
    studyTitle: String,
    studyTags: List<String>,
    selectedText: String
): String {
    val normalized = question.lowercase()
    val context = buildList {
        studyTitle.takeIf { it.isNotBlank() }?.let { add("titulo: $it") }
        studyTags.takeIf { it.isNotEmpty() }?.let { add("etiquetas: ${it.joinToString(", ")}") }
        selectedText.takeIf { it.isNotBlank() }?.let { add("seleccion: $it") }
    }.joinToString("; ")

    val baseContext = if (context.isBlank()) {
        "Aun no tengo contexto guardado de la ensenanza."
    } else {
        "Estoy tomando como contexto $context."
    }

    return when {
        "creacion" in normalized || "creacion" in normalized.removeAccents() -> {
            "$baseContext Para hablar de la creacion, revisa Genesis 1:1-31, Genesis 2:1-3, Juan 1:1-3 y Hebreos 11:3. Puedes usar Genesis como texto base y Juan 1 para conectar la creacion con Cristo como Verbo eterno."
        }
        "amor" in normalized -> {
            "$baseContext El amor es central en la Biblia. Sugiero 1 Corintios 13 (el himno al amor), 1 Juan 4:7-21 (Dios es amor) y Juan 3:16."
        }
        "fe" in normalized -> {
            "$baseContext Para estudiar la fe, Hebreos 11 es indispensable. Tambien considera Santiago 2:14-26 sobre la fe y las obras, y Romanos 10:17 sobre como viene la fe."
        }
        "gracia" in normalized -> {
            "$baseContext La gracia de Dios se explica muy bien en Efesios 2:8-9, Romanos 3:24 y Tito 2:11."
        }
        "perdon" in normalized -> {
            "$baseContext El perdon es vital. Mira Mateo 18:21-35 (la parabola del siervo que no perdono), Colosenses 3:13 y Efesios 4:32."
        }
        "ideas" in normalized || "ayuda" in normalized || "sugerencia" in normalized -> {
            "$baseContext Como sugerencia, podrias estructurar tu ensenanza con: 1) Una introduccion basada en el contexto actual, 2) Tres puntos clave extraidos del texto seleccionado, y 3) Una aplicacion practica para la vida diaria."
        }
        "reflexion" in normalized || "enseñanza" in normalized.removeAccents() -> {
            "Basado en $context, una reflexion profunda podria ser: 'La Palabra de Dios no solo nos informa, sino que nos transforma cuando permitimos que su verdad penetre nuestro corazon'. Considera como los tags ${studyTags.joinToString()} se conectan con tu vida hoy."
        }
        else -> {
            "$baseContext No tengo una respuesta especifica para esa pregunta, pero puedo ayudarte a reflexionar mas sobre $studyTitle si me das mas detalles."
        }
    }
}

private fun String.removeAccents(): String {
    val map = mapOf(
        'á' to 'a', 'é' to 'e', 'í' to 'i', 'ó' to 'o', 'ú' to 'u',
        'Á' to 'A', 'É' to 'E', 'Í' to 'I', 'Ó' to 'O', 'Ú' to 'U',
        'ñ' to 'n', 'Ñ' to 'N'
    )
    return this.map { map[it] ?: it }.joinToString("")
}

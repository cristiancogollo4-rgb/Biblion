package com.cristiancogollo.biblion.feature.studydocs.ui.editor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.cristiancogollo.biblion.ui.theme.BiblionGoldPrimary

@Composable
fun SaveTeachingDialog(
    currentTitle: String,
    currentTags: List<String>,
    onSave: (String, List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    var title by remember { mutableStateOf(currentTitle) }
    val tagsInput = remember { mutableStateOf(currentTags.joinToString(", ")) }
    var error by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            shadowElevation = 8.dp,
        ) {
            Column(Modifier.fillMaxWidth()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Guardar ensenanza",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, "Cerrar")
                    }
                }

                // Scrollable content (toma el espacio disponible)
                Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp)) {
                    Column(Modifier.verticalScroll(rememberScrollState()).padding(vertical = 8.dp)) {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it; error = null },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Titulo") },
                            singleLine = true,
                            placeholder = { Text("Ej: La fe en tiempos dificiles") },
                            isError = error != null,
                        )

                        Spacer(Modifier.height(16.dp))

                        Text("Etiquetas", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Elige sugeridas o escribe personalizadas separadas por comas.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(8.dp))

                        StudyTagSelector(
                            value = tagsInput.value,
                            onValueChange = { tagsInput.value = it },
                            modifier = Modifier.fillMaxWidth(),
                            isCompact = true,
                        )

                        if (error != null) {
                            Spacer(Modifier.height(12.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                Text(error ?: "", modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                    }
                }

                // Botones SIEMPRE visibles al final
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancelar") }
                    Button(
                        onClick = {
                            val parsed = parseStudyTags(tagsInput.value)
                            val validationError = validateRequiredStudyTags(parsed)
                            if (title.isBlank() || validationError != null) {
                                error = validationError ?: "El titulo no puede estar vacio."
                            } else {
                                onSave(title, parsed)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BiblionGoldPrimary),
                    ) {
                        Text("Guardar")
                    }
                }
            }
        }
    }
}

package com.cristiancogollo.biblion.feature.studydocs.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cristiancogollo.biblion.feature.studydocs.model.DocTagGroups

/**
 * Grupo de tags sugerido para el selector de ensenanzas.
 *
 * Replica la estructura de v1: 4 grupos (Proposito, Audiencia, Tema, Estado)
 * con tags curados. Estado usa singleSelection.
 */
data class StudyTagGroup(
    val title: String,
    val tags: List<String>,
    val singleSelection: Boolean = false,
)

/**
 * Lista de los 4 grupos canonicos. Se usa en StudyTagSelector y en
 * TeachingTagFilterRow para mantener consistencia con el sistema de tags v2
 * (DocTagGroups).
 */
val suggestedStudyTagGroups: List<StudyTagGroup> = listOf(
    StudyTagGroup(
        title = "Proposito",
        tags = DocTagGroups.PURPOSE_TAGS,
    ),
    StudyTagGroup(
        title = "Audiencia",
        tags = DocTagGroups.AUDIENCE_TAGS,
    ),
    StudyTagGroup(
        title = "Tema",
        tags = DocTagGroups.TOPIC_TAGS,
    ),
    StudyTagGroup(
        title = "Estado",
        tags = DocTagGroups.STATE_TAGS,
        singleSelection = true,
    ),
)

/**
 * Selector de tags estilo v1: chips por grupo, input de tags custom, output
 * como lista de tags normalizados.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun StudyTagSelector(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false,
) {
    val selectedTags = remember(value) { parseStudyTags(value) }
    val expandedSections = remember {
        mutableStateListOf(true, false, false, false)
    }
    var customTagInput by remember { mutableStateOf("") }

    Column(modifier = modifier) {
        // Etiquetas seleccionadas como chips removibles
        if (selectedTags.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                ),
            ) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    selectedTags.forEach { tag ->
                        AssistChip(
                            onClick = {
                                val next = selectedTags - tag
                                onValueChange(next.joinToString(", "))
                            },
                            label = { Text("# $tag") },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (!isCompact) {
            // Input para tags custom
            OutlinedTextField(
                value = customTagInput,
                onValueChange = { customTagInput = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Anade una etiqueta personalizada") },
                singleLine = true,
                trailingIcon = {
                    IconButton(
                        onClick = {
                            val normalized = parseStudyTags(customTagInput).firstOrNull()
                            if (normalized != null && normalized !in selectedTags) {
                                val next = selectedTags + normalized
                                onValueChange(next.joinToString(", "))
                                customTagInput = ""
                            }
                        },
                        enabled = customTagInput.isNotBlank(),
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Anadir")
                    }
                },
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Grupos de tags
        suggestedStudyTagGroups.forEachIndexed { index, group ->
            StudyTagGroupSection(
                group = group,
                selectedTags = selectedTags,
                expanded = expandedSections[index],
                onToggleExpanded = { expandedSections[index] = !expandedSections[index] },
                onTagClick = { tag ->
                    val next = when {
                        tag in selectedTags -> selectedTags - tag
                        group.singleSelection -> (selectedTags - group.tags) + tag
                        else -> selectedTags + tag
                    }
                    onValueChange(next.joinToString(", "))
                },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun StudyTagGroupSection(
    group: StudyTagGroup,
    selectedTags: List<String>,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onTagClick: (String) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.small,
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = group.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onToggleExpanded) {
                    Text(
                        text = if (expanded) "-" else "+",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            if (expanded) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    group.tags.forEach { tag ->
                        val isSelected = tag in selectedTags
                        AssistChip(
                            onClick = { onTagClick(tag) },
                            label = { Text("# $tag") },
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                    )
                                }
                            } else null,
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                },
                                labelColor = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            ),
                        )
                    }
                }
            }
            Divider(
                modifier = Modifier.padding(top = 4.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
            )
        }
    }
}

/**
 * Parsea una cadena de tags separados por coma a una lista normalizada.
 * Tags vacios o duplicados se descartan.
 */
fun parseStudyTags(input: String): List<String> =
    input.split(",")
        .map { normalizeStudyTag(it) }
        .filter { it.isNotBlank() }
        .distinct()

/**
 * Normaliza un tag: lowercase, sin acentos, guiones en lugar de espacios,
 * solo caracteres alfanumericos y guiones.
 */
fun normalizeStudyTag(tag: String): String {
    val accents = mapOf(
        '\u00e1' to 'a', '\u00e9' to 'e', '\u00ed' to 'i',
        '\u00f3' to 'o', '\u00fa' to 'u', '\u00f1' to 'n',
        '\u00c1' to 'A', '\u00c9' to 'E', '\u00cd' to 'I',
        '\u00d3' to 'O', '\u00da' to 'U', '\u00d1' to 'N',
    )
    return tag.trim().lowercase()
        .replace(Regex("\\s+"), "-")
        .map { accents[it] ?: it }
        .joinToString("")
        .filter { it.isLetterOrDigit() || it == '-' }
}

/**
 * Valida que una lista de tags cumple los requisitos de v1:
 * al menos 1 proposito, 1 audiencia, 1 tema, 1 estado.
 * Retorna null si todo OK, o un mensaje de error.
 */
fun validateRequiredStudyTags(tags: List<String>): String? {
    val hasPurpose = tags.any { it in DocTagGroups.PURPOSE_TAGS }
    val hasAudience = tags.any { it in DocTagGroups.AUDIENCE_TAGS }
    val hasTopic = tags.any { it in DocTagGroups.TOPIC_TAGS }
    val hasState = tags.any { it in DocTagGroups.STATE_TAGS }
    return when {
        !hasPurpose -> "Selecciona al menos un proposito (predicacion, devocional, estudio biblico, etc)."
        !hasAudience -> "Selecciona al menos una audiencia (jovenes, iglesia, lideres, etc)"
        !hasTopic -> "Selecciona al menos un tema (identidad, fe, gracia, etc)"
        !hasState -> "Selecciona un estado (borrador, en preparacion, finalizado)"
        else -> null
    }
}

@Suppress("unused")
private fun listAllTags(): List<String> = suggestedStudyTagGroups.flatMap { it.tags }

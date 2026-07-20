package com.cristiancogollo.biblion.feature.studydocs.ui.editor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cristiancogollo.biblion.feature.studydocs.model.DocTagGroups

data class StudyTagGroup(
    val title: String,
    val tags: List<String>,
    val singleSelection: Boolean = false,
)

val suggestedStudyTagGroups: List<StudyTagGroup> = listOf(
    StudyTagGroup("Proposito", DocTagGroups.PURPOSE_TAGS),
    StudyTagGroup("Audiencia", DocTagGroups.AUDIENCE_TAGS),
    StudyTagGroup("Tema", DocTagGroups.TOPIC_TAGS),
    StudyTagGroup("Estado", DocTagGroups.STATE_TAGS, singleSelection = true),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudyTagSelector(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false,
) {
    val selectedTags = remember(value) { parseStudyTags(value) }
    val expandedSections = remember { mutableStateListOf(true, false, false, false) }
    var customTagInput by remember { mutableStateOf("") }

    Column(modifier = modifier) {
        if (selectedTags.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
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
                            trailingIcon = { Icon(Icons.Filled.Close, null, modifier = Modifier.size(14.dp)) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        if (!isCompact) {
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
                                onValueChange((selectedTags + normalized).joinToString(", "))
                                customTagInput = ""
                            }
                        },
                        enabled = customTagInput.isNotBlank(),
                    ) { Icon(Icons.Filled.Add, "Anadir") }
                },
            )
            Spacer(Modifier.height(12.dp))
        }

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

@OptIn(ExperimentalLayoutApi::class)
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
                Text(group.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                IconButton(onClick = onToggleExpanded) {
                    Text(if (expanded) "-" else "+", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
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
                            leadingIcon = if (isSelected) { { Icon(Icons.Filled.Check, null, modifier = Modifier.size(14.dp)) } } else null,
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                labelColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                    }
                }
            }
            HorizontalDivider(modifier = Modifier.padding(top = 4.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        }
    }
}

fun parseStudyTags(input: String): List<String> =
    input.split(",").map { normalizeStudyTag(it) }.filter { it.isNotBlank() }.distinct()

fun normalizeStudyTag(tag: String): String {
    val accents = mapOf('á' to 'a', 'é' to 'e', 'í' to 'i', 'ó' to 'o', 'ú' to 'u', 'ñ' to 'n', 'Á' to 'A', 'É' to 'E', 'Í' to 'I', 'Ó' to 'O', 'Ú' to 'U', 'Ñ' to 'N')
    return tag.trim().lowercase().replace(Regex("\\s+"), "-").map { accents[it] ?: it }.joinToString("").filter { it.isLetterOrDigit() || it == '-' }
}

fun validateRequiredStudyTags(tags: List<String>): String? = when {
    !tags.any { it in DocTagGroups.PURPOSE_TAGS } -> "Selecciona al menos un proposito (predicacion, devocional, estudio biblico, etc)."
    !tags.any { it in DocTagGroups.AUDIENCE_TAGS } -> "Selecciona al menos una audiencia (jovenes, iglesia, lideres, etc)"
    !tags.any { it in DocTagGroups.TOPIC_TAGS } -> "Selecciona al menos un tema (identidad, fe, gracia, etc)"
    !tags.any { it in DocTagGroups.STATE_TAGS } -> "Selecciona un estado (borrador, en preparacion, finalizado)"
    else -> null
}

package com.cristiancogollo.biblion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cristiancogollo.biblion.ui.theme.BiblionGoldPrimary
import com.cristiancogollo.biblion.ui.theme.BiblionNavy

internal data class StudyTagGroup(
    val title: String,
    val tags: List<String>,
    val singleSelection: Boolean = false
)

internal val suggestedStudyTagGroups = listOf(
    StudyTagGroup(
        title = "Proposito",
        tags = listOf("predicacion", "devocional", "estudio-biblico", "clase", "discipulado", "formacion")
    ),
    StudyTagGroup(
        title = "Audiencia",
        tags = listOf(
            "jovenes",
            "iglesia",
            "lideres",
            "universitarios",
            "familias",
            "simpatizantes",
            "ninos",
            "mujeres",
            "hombres",
            "ancianos",
            "grupos-especiales",
            "pastores"
        )
    ),
    StudyTagGroup(
        title = "Tema",
        tags = listOf(
            "identidad",
            "fe",
            "gracia",
            "proposito",
            "oracion",
            "evangelismo",
            "servicio",
            "esperanza",
            "doctrina",
            "amor",
            "misiones",
            "adoracion"
        )
    ),
    StudyTagGroup(
        title = "Estado",
        tags = listOf("borrador", "en-preparacion", "finalizado"),
        singleSelection = true
    )
)

internal fun parseStudyTags(input: String): List<String> {
    return input.split(",")
        .map { normalizeStudyTag(it) }
        .filter { it.isNotBlank() }
        .distinct()
}

private fun normalizeStudyTag(tag: String): String {
    return tag.trim()
        .removePrefix("#")
        .lowercase()
        .replace(Regex("[áà]"), "a")
        .replace(Regex("[éèë]"), "e")
        .replace(Regex("[íìï]"), "i")
        .replace(Regex("[óòö]"), "o")
        .replace(Regex("[úùü]"), "u")
        .replace(Regex("[ñ]"), "n")
        .replace(Regex("\\s+"), "-")
}

private fun tagsToInput(tags: List<String>): String = tags.joinToString(", ")

private fun StudyTagGroup.selectedTags(selectedTags: List<String>): List<String> {
    return selectedTags.filter { it in tags }
}

internal fun validateRequiredStudyTags(tags: List<String>): String? {
    val missingGroup = suggestedStudyTagGroups.firstOrNull { group ->
        group.selectedTags(tags).isEmpty()
    }
    return when {
        missingGroup != null -> "Debes seleccionar al menos una etiqueta en ${missingGroup.title}."
        suggestedStudyTagGroups.last().selectedTags(tags).size != 1 ->
            "Debes seleccionar solo un estado."
        else -> null
    }
}

internal fun withDefaultStateTag(tags: List<String>): List<String> {
    val stateGroup = suggestedStudyTagGroups.last()
    return if (stateGroup.selectedTags(tags).isEmpty()) {
        tags + "borrador"
    } else {
        tags
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudyTagSelector(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Etiquetas"
) {
    val selectedTags = parseStudyTags(value)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = false,
            minLines = 1,
            maxLines = 3,
            label = { Text(label) },
            placeholder = { Text("Ej: fe, jovenes, predicacion") },
            supportingText = { Text("Elige sugeridas o escribe etiquetas separadas por comas.") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BiblionNavy,
                unfocusedBorderColor = BiblionGoldPrimary,
                focusedLabelColor = BiblionNavy,
                cursorColor = BiblionNavy
            ),
            modifier = Modifier.fillMaxWidth()
        )

        suggestedStudyTagGroups.forEach { group ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = group.title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    group.tags.forEach { tag ->
                        val selected = tag in selectedTags
                        FilterChip(
                            selected = selected,
                            onClick = {
                                val nextTags = if (selected) {
                                    selectedTags - tag
                                } else if (group.singleSelection) {
                                    selectedTags - group.tags + tag
                                } else {
                                    selectedTags + tag
                                }
                                onValueChange(tagsToInput(nextTags))
                            },
                            label = { Text(tag) }
                        )
                    }
                }
            }
        }

        val customTags = selectedTags.filterNot { tag ->
            suggestedStudyTagGroups.any { group -> tag in group.tags }
        }
        if (customTags.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Personalizadas",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    customTags.forEach { tag ->
                        AssistChip(
                            onClick = {
                                onValueChange(tagsToInput(selectedTags - tag))
                            },
                            label = { Text("$tag x") }
                        )
                    }
                }
            }
        }
    }
}

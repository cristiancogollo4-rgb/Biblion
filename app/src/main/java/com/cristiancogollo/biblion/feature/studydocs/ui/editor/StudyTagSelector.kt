package com.cristiancogollo.biblion.feature.studydocs.ui.editor

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.cristiancogollo.biblion.R
import com.cristiancogollo.biblion.feature.studydocs.model.DocTagGroups
import com.cristiancogollo.biblion.ui.theme.BiblionBluePrimary
import com.cristiancogollo.biblion.ui.theme.BiblionGoldPrimary

data class StudyTagGroup(
    @StringRes val titleRes: Int,
    @StringRes val hintRes: Int,
    val tags: List<String>,
    val singleSelection: Boolean = false,
)

val suggestedStudyTagGroups: List<StudyTagGroup> = listOf(
    StudyTagGroup(R.string.study_tags_group_purpose, R.string.study_tags_group_purpose_hint, DocTagGroups.PURPOSE_TAGS),
    StudyTagGroup(R.string.study_tags_group_audience, R.string.study_tags_group_audience_hint, DocTagGroups.AUDIENCE_TAGS),
    StudyTagGroup(R.string.study_tags_group_topic, R.string.study_tags_group_topic_hint, DocTagGroups.TOPIC_TAGS),
    StudyTagGroup(R.string.study_tags_group_state, R.string.study_tags_group_state_hint, DocTagGroups.STATE_TAGS, singleSelection = true),
)

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun StudyTagSelector(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false,
    onPendingCustomTagChange: (String) -> Unit = {},
    validationGroupIndex: Int? = null,
    enabled: Boolean = true,
) {
    val selectedTags = remember(value) { parseStudyTags(value) }
    val completedGroups = suggestedStudyTagGroups.count { group ->
        group.tags.any { it in selectedTags }
    }
    var expandedSection by remember {
        mutableIntStateOf(
            suggestedStudyTagGroups.indexOfFirst { group ->
                group.tags.none { it in selectedTags }
            }.coerceAtLeast(0),
        )
    }
    var customTagInput by remember { mutableStateOf("") }
    val groupRequesters = remember { List(suggestedStudyTagGroups.size) { BringIntoViewRequester() } }

    LaunchedEffect(validationGroupIndex) {
        if (validationGroupIndex != null && validationGroupIndex in groupRequesters.indices) {
            expandedSection = validationGroupIndex
            withFrameNanos { }
            groupRequesters[validationGroupIndex].bringIntoView()
        }
    }

    fun addCustomTags() {
        val parsed = parseStudyTags(customTagInput)
        val additions = parsed.filterNot { it in selectedTags }
        if (additions.isNotEmpty()) {
            onValueChange((selectedTags + additions).joinToString(", "))
        }
        if (parsed.isNotEmpty()) {
            customTagInput = ""
            onPendingCustomTagChange("")
        }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(if (isCompact) 8.dp else 12.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
            border = BorderStroke(1.dp, BiblionGoldPrimary.copy(alpha = 0.38f)),
        ) {
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.study_tags_title),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(R.string.study_tags_progress, completedGroups, suggestedStudyTagGroups.size),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    suggestedStudyTagGroups.forEach { group ->
                        val complete = group.tags.any { it in selectedTags }
                        Surface(
                            modifier = Modifier.weight(1f).height(5.dp),
                            shape = RoundedCornerShape(4.dp),
                            color = if (complete) BiblionGoldPrimary else MaterialTheme.colorScheme.outlineVariant,
                        ) {}
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.study_tags_required_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (selectedTags.isNotEmpty()) {
            Column {
                Text(
                    text = stringResource(R.string.study_tags_selected, selectedTags.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    selectedTags.forEach { tag ->
                        val label = studyTagDisplayName(tag)
                        AssistChip(
                            onClick = { onValueChange((selectedTags - tag).joinToString(", ")) },
                            enabled = enabled,
                            label = { Text(label) },
                            trailingIcon = {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = stringResource(R.string.study_tags_remove, label),
                                    modifier = Modifier.size(14.dp),
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = BiblionBluePrimary.copy(alpha = 0.11f),
                                labelColor = MaterialTheme.colorScheme.onSurface,
                                trailingIconContentColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                    }
                }
            }
        }

        suggestedStudyTagGroups.forEachIndexed { index, group ->
            StudyTagGroupSection(
                group = group,
                selectedTags = selectedTags,
                expanded = expandedSection == index,
                onToggleExpanded = { expandedSection = if (expandedSection == index) -1 else index },
                onTagClick = { tag ->
                    val next = toggleStudyTag(selectedTags, group, tag)
                    onValueChange(next.joinToString(", "))
                },
                modifier = Modifier.bringIntoViewRequester(groupRequesters[index]),
                validationError = if (validationGroupIndex == index) studyTagValidationMessageRes(index) else null,
                enabled = enabled,
            )
        }

        Column {
            Text(
                text = stringResource(R.string.study_tags_custom_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.study_tags_custom_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = customTagInput,
                onValueChange = {
                    customTagInput = it
                    onPendingCustomTagChange(it)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                placeholder = { Text(stringResource(R.string.study_tags_custom_placeholder)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { addCustomTags() }),
                trailingIcon = {
                    IconButton(
                        onClick = ::addCustomTags,
                        enabled = enabled && parseStudyTags(customTagInput).isNotEmpty(),
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.study_tags_add))
                    }
                },
            )
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun StudyTagGroupSection(
    group: StudyTagGroup,
    selectedTags: List<String>,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onTagClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    @StringRes validationError: Int? = null,
    enabled: Boolean = true,
) {
    val isComplete = group.tags.any { it in selectedTags }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (isComplete) BiblionGoldPrimary.copy(alpha = 0.62f) else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Column {
            Row(
                modifier = modifier.fillMaxWidth()
                    .clickable(enabled = enabled, onClick = onToggleExpanded)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = CircleShape,
                    color = if (isComplete) BiblionGoldPrimary else MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isComplete) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = BiblionBluePrimary, modifier = Modifier.size(18.dp))
                        } else {
                            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.outline, modifier = Modifier.size(8.dp)) {}
                        }
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(group.titleRes),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(group.hintRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = stringResource(if (isComplete) R.string.study_tags_group_done else R.string.study_tags_group_pending),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isComplete) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Icon(
                    if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (expanded) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    group.tags.forEach { tag ->
                        val selected = tag in selectedTags
                        FilterChip(
                            selected = selected,
                            onClick = { onTagClick(tag) },
                            enabled = enabled,
                            label = { Text(studyTagDisplayName(tag)) },
                            leadingIcon = if (selected) {
                                { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BiblionBluePrimary,
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = BiblionGoldPrimary,
                            ),
                        )
                    }
                }
            }
            if (validationError != null) {
                Text(
                    text = stringResource(validationError),
                    modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                        .semantics { liveRegion = LiveRegionMode.Polite },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
fun studyTagDisplayName(tag: String): String =
    studyTagDisplayNameRes(tag)?.let { stringResource(it) }
        ?: tag.replace('-', ' ').replaceFirstChar { it.uppercaseChar() }

@StringRes
internal fun studyTagDisplayNameRes(tag: String): Int? = when (tag) {
    "predicacion" -> R.string.study_tag_predicacion
    "devocional" -> R.string.study_tag_devocional
    "estudio-biblico" -> R.string.study_tag_estudio_biblico
    "clase" -> R.string.study_tag_clase
    "discipulado" -> R.string.study_tag_discipulado
    "formacion" -> R.string.study_tag_formacion
    "jovenes" -> R.string.study_tag_jovenes
    "iglesia" -> R.string.study_tag_iglesia
    "lideres" -> R.string.study_tag_lideres
    "universitarios" -> R.string.study_tag_universitarios
    "familias" -> R.string.study_tag_familias
    "simpatizantes" -> R.string.study_tag_simpatizantes
    "ninos" -> R.string.study_tag_ninos
    "mujeres" -> R.string.study_tag_mujeres
    "hombres" -> R.string.study_tag_hombres
    "ancianos" -> R.string.study_tag_ancianos
    "grupos-especiales" -> R.string.study_tag_grupos_especiales
    "pastores" -> R.string.study_tag_pastores
    "identidad" -> R.string.study_tag_identidad
    "fe" -> R.string.study_tag_fe
    "gracia" -> R.string.study_tag_gracia
    "proposito" -> R.string.study_tag_proposito
    "oracion" -> R.string.study_tag_oracion
    "evangelismo" -> R.string.study_tag_evangelismo
    "servicio" -> R.string.study_tag_servicio
    "esperanza" -> R.string.study_tag_esperanza
    "doctrina" -> R.string.study_tag_doctrina
    "amor" -> R.string.study_tag_amor
    "misiones" -> R.string.study_tag_misiones
    "adoracion" -> R.string.study_tag_adoracion
    "borrador" -> R.string.study_tag_borrador
    "en-preparacion" -> R.string.study_tag_en_preparacion
    "finalizado" -> R.string.study_tag_finalizado
    else -> null
}

internal fun toggleStudyTag(selectedTags: List<String>, group: StudyTagGroup, tag: String): List<String> = when {
    tag in selectedTags -> selectedTags - tag
    group.singleSelection -> (selectedTags - group.tags) + tag
    else -> selectedTags + tag
}

fun parseStudyTags(input: String): List<String> =
    input.split(",").map { normalizeStudyTag(it) }.filter { it.isNotBlank() }.distinct()

fun studyTagsForSave(selectedInput: String, pendingCustomInput: String): List<String> =
    parseStudyTags("$selectedInput,$pendingCustomInput")

fun normalizeStudyTag(tag: String): String {
    val accents = mapOf('á' to 'a', 'é' to 'e', 'í' to 'i', 'ó' to 'o', 'ú' to 'u', 'ñ' to 'n', 'Á' to 'A', 'É' to 'E', 'Í' to 'I', 'Ó' to 'O', 'Ú' to 'U', 'Ñ' to 'N')
    return tag.trim().lowercase().replace(Regex("\\s+"), "-").map { accents[it] ?: it }.joinToString("").filter { it.isLetterOrDigit() || it == '-' }
}

fun firstIncompleteStudyTagGroupIndex(tags: List<String>): Int? =
    suggestedStudyTagGroups.indexOfFirst { group -> group.tags.none { it in tags } }
        .takeIf { it >= 0 }

@StringRes
fun studyTagValidationMessageRes(index: Int): Int = when (index) {
    0 -> R.string.study_tags_error_purpose
    1 -> R.string.study_tags_error_audience
    2 -> R.string.study_tags_error_topic
    else -> R.string.study_tags_error_state
}

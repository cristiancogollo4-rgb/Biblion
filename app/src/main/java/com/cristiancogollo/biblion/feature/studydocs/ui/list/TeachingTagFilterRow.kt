package com.cristiancogollo.biblion.feature.studydocs.ui.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cristiancogollo.biblion.feature.studydocs.model.DocTagGroups
import com.cristiancogollo.biblion.ui.theme.BiblionGoldPrimary

data class TagGroup(
    val title: String,
    val tags: List<String>,
)

val suggestedStudyTagGroups = listOf(
    TagGroup("Propósito", DocTagGroups.PURPOSE_TAGS),
    TagGroup("Audiencia", DocTagGroups.AUDIENCE_TAGS),
    TagGroup("Tema", DocTagGroups.TOPIC_TAGS),
    TagGroup("Estado", DocTagGroups.STATE_TAGS),
)

@Composable
fun TeachingTagFilterRow(
    selectedTags: Set<String>,
    onTagToggled: (String) -> Unit,
    onClearTags: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expandedGroup by remember { mutableStateOf<String?>(null) }

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
    ) {
        if (selectedTags.isNotEmpty()) {
            item("clear") {
                AssistChip(
                    onClick = onClearTags,
                    label = { Text("Limpiar") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = null,
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        labelColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                )
            }
        }
        items(suggestedStudyTagGroups) { group ->
            val activeCount = group.tags.count { it in selectedTags }
            AssistChip(
                onClick = {
                    expandedGroup = if (expandedGroup == group.title) null else group.title
                },
                label = {
                    Text(
                        text = if (activeCount > 0) {
                            "${group.title} ($activeCount)"
                        } else {
                            group.title
                        },
                    )
                },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Filled.ExpandMore,
                        contentDescription = null,
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (activeCount > 0) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    labelColor = if (activeCount > 0) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                ),
                border = AssistChipDefaults.assistChipBorder(
                    enabled = true,
                    borderColor = if (activeCount > 0) {
                        BiblionGoldPrimary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    },
                ),
            )
        }
        if (expandedGroup != null) {
            val groupTags = suggestedStudyTagGroups
                .firstOrNull { it.title == expandedGroup }
                ?.tags
                .orEmpty()
            items(groupTags) { tag ->
                FilterChip(
                    selected = tag in selectedTags,
                    onClick = { onTagToggled(tag) },
                    label = { Text("#$tag") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BiblionGoldPrimary,
                        selectedLabelColor = MaterialTheme.colorScheme.surface,
                    ),
                )
            }
        }
        items(selectedTags.toList()) { tag ->
            FilterChip(
                selected = true,
                onClick = { onTagToggled(tag) },
                label = { Text("#$tag", fontSize = 12.sp) },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Quitar filtro $tag",
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = BiblionGoldPrimary,
                    selectedLabelColor = MaterialTheme.colorScheme.surface,
                    selectedTrailingIconColor = MaterialTheme.colorScheme.surface,
                ),
            )
        }
    }
}

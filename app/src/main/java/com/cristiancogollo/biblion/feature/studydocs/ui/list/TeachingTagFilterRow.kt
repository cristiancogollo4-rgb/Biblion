package com.cristiancogollo.biblion.feature.studydocs.ui.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cristiancogollo.biblion.feature.studydocs.model.DocTagGroups

data class TagGroup(val title: String)

val suggestedStudyTagGroups = listOf(
    TagGroup("Proposito"),
    TagGroup("Audiencia"),
    TagGroup("Tema"),
    TagGroup("Estado"),
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
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        labelColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                )
            }
        }
        items(suggestedStudyTagGroups) { group ->
            val groupTags = when (group.title) {
                "Proposito" -> DocTagGroups.PURPOSE_TAGS
                "Audiencia" -> DocTagGroups.AUDIENCE_TAGS
                "Tema" -> DocTagGroups.TOPIC_TAGS
                "Estado" -> DocTagGroups.STATE_TAGS
                else -> emptyList()
            }
            val activeCount = groupTags.count { it in selectedTags }
            if (activeCount > 0) {
                AssistChip(
                    onClick = { groupTags.forEach(onTagToggled) },
                    label = { Text("${group.title} ($activeCount)") },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )
            } else {
                AssistChip(
                    onClick = {
                        expandedGroup = if (expandedGroup == group.title) null else group.title
                    },
                    label = { Text(group.title) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            }
        }
        if (expandedGroup != null) {
            val groupTags = when (expandedGroup) {
                "Proposito" -> DocTagGroups.PURPOSE_TAGS
                "Audiencia" -> DocTagGroups.AUDIENCE_TAGS
                "Tema" -> DocTagGroups.TOPIC_TAGS
                "Estado" -> DocTagGroups.STATE_TAGS
                else -> emptyList()
            }
            items(groupTags) { tag ->
                FilterChip(
                    selected = tag in selectedTags,
                    onClick = { onTagToggled(tag) },
                    label = { Text("#$tag") },
                )
            }
        }
        items(selectedTags.toList()) { tag ->
            FilterChip(
                selected = true,
                onClick = { onTagToggled(tag) },
                label = { Text("#$tag") },
            )
        }
    }
}

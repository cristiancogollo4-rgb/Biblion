@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.cristiancogollo.biblion.feature.studydocs.ui.repository

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cristiancogollo.biblion.feature.studydocs.model.DocTagGroups
import com.cristiancogollo.biblion.feature.studydocs.model.PublicTeaching

private val repositoryTags = (
    DocTagGroups.PURPOSE_TAGS +
        DocTagGroups.AUDIENCE_TAGS +
        DocTagGroups.TOPIC_TAGS
    ).distinct()

@Composable
fun PublicTeachingScreen(
    viewModel: PublicTeachingViewModel,
    ownerUid: String?,
    onBack: () -> Unit,
    onOpenDownloaded: (String) -> Unit,
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Biblion", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Buscar") },
                placeholder = { Text("Buscar por titulo o autor") },
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                repositoryTags.take(4).forEach { tag ->
                    FilterChip(
                        selected = tag in state.selectedTags,
                        onClick = { viewModel.toggleTag(tag) },
                        label = { Text(tag.replace('-', ' '), maxLines = 1) },
                    )
                }
            }
            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (state.visiblePublications.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        state.error ?: "No hay ensenanzas publicadas con estos filtros.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.visiblePublications, key = { it.publicationId }) { teaching ->
                        PublicTeachingCard(
                            teaching = teaching,
                            isDownloading = teaching.publicationId == state.downloadingId,
                            onDownload = {
                                viewModel.download(teaching, ownerUid) { localId ->
                                    localId?.let(onOpenDownloaded)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PublicTeachingCard(
    teaching: PublicTeaching,
    isDownloading: Boolean,
    onDownload: () -> Unit,
) {
    androidx.compose.material3.Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                teaching.title.ifBlank { "Ensenanza sin titulo" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Por ${teaching.author.alias.ifBlank { teaching.author.displayName.ifBlank { "Autor Biblion" } }}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            teaching.rootAuthor
                ?.takeIf { it.uid != teaching.author.uid }
                ?.let { root ->
                    Text(
                        "Obra original de ${root.alias.ifBlank { root.displayName.ifBlank { "Autor Biblion" } }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            Text(
                teaching.tags.joinToString("  ·  ") { it.replace('-', ' ') },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
            )
            teaching.currentRevisionDiff?.let { diff ->
                Text(
                    "Cambios: +${diff.blocksAdded} bloques, ${diff.blocksModified} modificados, " +
                        "${diff.similarityPercent ?: 0}% de similitud",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Text(
                        "Revision ${teaching.currentRevisionId.takeLast(6)}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Button(onClick = onDownload, enabled = !isDownloading) {
                    if (isDownloading) {
                        CircularProgressIndicator(modifier = Modifier.width(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.Download, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Usar como base")
                    }
                }
            }
        }
    }
}

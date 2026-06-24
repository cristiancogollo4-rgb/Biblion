package com.cristiancogollo.biblion.feature.studydocs.ui.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.cristiancogollo.biblion.feature.studydocs.model.StudyDoc

/**
 * Pantalla principal de la lista de ensenanzas (visual v1).
 *
 * TopAppBar: "Mis ensenanzas"
 * SearchField: filtro por titulo
 * TeachingTagFilterRow: chips de filtro por tag
 * LazyColumn: TeachingCard por cada doc visible
 * FAB: nuevo documento
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyDocsListScreen(
    viewModel: StudyDocsListViewModel,
    onBack: () -> Unit,
    onOpenDoc: (StudyDoc) -> Unit,
    onEditDoc: (StudyDoc) -> Unit,
    onNewDoc: () -> Unit,
    onShareText: (StudyDoc) -> Unit = {},
    onShareBiblion: (StudyDoc) -> Unit = {},
    onEditMetadata: (StudyDoc) -> Unit = {},
) {
    val state by viewModel.state.collectAsState()
    var pendingDelete by remember { mutableStateOf<StudyDoc?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis ensenanzas", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewDoc) {
                Icon(Icons.Filled.Add, contentDescription = "Nueva ensenanza")
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OutlinedTextField(
                value = state.titleFilter,
                onValueChange = viewModel::setTitleFilter,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Buscar por titulo") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                trailingIcon = if (state.titleFilter.isNotEmpty()) {
                    {
                        IconButton(onClick = { viewModel.setTitleFilter("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Limpiar")
                        }
                    }
                } else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                shape = RoundedCornerShape(12.dp),
            )

            TeachingTagFilterRow(
                selectedTags = state.selectedTagFilters,
                onTagToggled = viewModel::toggleTagFilter,
                onClearTags = viewModel::clearTagFilters,
                modifier = Modifier.padding(horizontal = 12.dp),
            )

            Spacer(modifier = Modifier.height(4.dp))

            val visible = state.visibleDocs
            if (visible.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (state.docs.isEmpty()) {
                        Text(
                            "Aun no tienes ensenanzas guardadas.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text(
                            "Ninguna ensenanza coincide con los filtros.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(visible, key = { it.id.value }) { doc ->
                        TeachingCard(
                            doc = doc,
                            onOpen = { onOpenDoc(doc) },
                            onEdit = { onEditDoc(doc) },
                            onEditMetadata = { onEditMetadata(doc) },
                            onShareText = { onShareText(doc) },
                            onShareBiblion = { onShareBiblion(doc) },
                            onDelete = { pendingDelete = doc },
                        )
                    }
                }
            }
        }
    }

    pendingDelete?.let { doc ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Eliminar ensenanza") },
            text = { Text("Vas a eliminar '${doc.title.ifBlank { "Sin titulo" }}'. Esta accion no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(doc)
                    pendingDelete = null
                }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Cancelar")
                }
            },
        )
    }
}

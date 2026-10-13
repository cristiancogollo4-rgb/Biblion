package com.cristiancogollo.biblion.feature.studydocs.ui.list

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.verticalScroll
import androidx.navigation.NavController
import androidx.compose.foundation.text.KeyboardOptions
import com.cristiancogollo.biblion.feature.studydocs.model.StudyDoc
import com.cristiancogollo.biblion.feature.studydocs.data.StudyShareFormat
import com.cristiancogollo.biblion.BiblionBottomNavigation
import com.cristiancogollo.biblion.R
import com.cristiancogollo.biblion.BiblionTopAppBar
import com.cristiancogollo.biblion.Screen
import com.cristiancogollo.biblion.Testament
import com.cristiancogollo.biblion.biblionLogoResForCurrentTheme
import com.cristiancogollo.biblion.navigateTopLevel
import com.cristiancogollo.biblion.feature.studydocs.ui.Screen as StudyDocScreen
import com.cristiancogollo.biblion.feature.studydocs.ui.editor.StudyTagSelector
import com.cristiancogollo.biblion.feature.studydocs.ui.editor.studyTagsForSave
import com.cristiancogollo.biblion.feature.studydocs.ui.editor.firstIncompleteStudyTagGroupIndex
import com.cristiancogollo.biblion.ui.theme.BiblionGoldPrimary
import com.cristiancogollo.biblion.ui.theme.BiblionGoldSoft
import com.cristiancogollo.biblion.ui.theme.BiblionBluePrimary

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
    navController: NavController,
    viewModel: StudyDocsListViewModel,
    onBack: () -> Unit,
    onOpenDoc: (StudyDoc) -> Unit,
    onEditDoc: (StudyDoc) -> Unit,
    onNewDoc: () -> Unit,
    onShare: (StudyDoc, StudyShareFormat) -> Unit = { _, _ -> },
    onRequestPublication: (StudyDoc) -> Unit = {},
) {
    val state by viewModel.state.collectAsState()
    val visibleDocs = state.visibleDocs
    val hasActiveFilters = state.titleFilter.isNotBlank() ||
        state.selectedTagFilters.isNotEmpty()
    var pendingDelete by remember { mutableStateOf<StudyDoc?>(null) }
    var pendingShare by remember { mutableStateOf<StudyDoc?>(null) }
    var pendingTagEdit by remember { mutableStateOf<StudyDoc?>(null) }
    var isSavingTags by remember { mutableStateOf(false) }
    var tagEditError by remember { mutableStateOf<String?>(null) }
    val tagSaveFailureText = stringResource(R.string.study_tags_save_failed)
    val isKeyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    Scaffold(
        topBar = {
            BiblionTopAppBar(
                logoResId = biblionLogoResForCurrentTheme(),
                showNavigationIcon = false,
                showSearchIcon = false,
            )
        },
        bottomBar = {
            if (!isKeyboardVisible) {
                BiblionBottomNavigation(
                    currentRoute = StudyDocScreen.StudyDocsList.route,
                    onHome = { navController.navigateTopLevel(Screen.Home.route) },
                    onBible = { navController.navigateTopLevel(Screen.Books.createRoute(Testament.OLD)) },
                    onSearch = { navController.navigateTopLevel(Screen.Search.createRoute()) },
                    onStudy = { },
                    onProfile = { navController.navigateTopLevel(Screen.Profile.route) }
                )
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 1120.dp),
            ) {
                StudyLibraryHeader(
                    documentCount = state.docs.size,
                    filteredDocumentCount = visibleDocs.size,
                    hasActiveFilters = hasActiveFilters,
                    onNewDoc = onNewDoc,
                )

                StudyFilterShelf(
                    titleFilter = state.titleFilter,
                    selectedTags = state.selectedTagFilters,
                    onTitleFilterChange = viewModel::setTitleFilter,
                    onTagToggled = viewModel::toggleTagFilter,
                    onClearTags = viewModel::clearTagFilters,
                )

                if (state.isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        color = BiblionGoldPrimary,
                        trackColor = BiblionGoldSoft.copy(alpha = 0.2f),
                    )
                }

                if (visibleDocs.isEmpty() && !state.isLoading) {
                    StudyLibraryEmptyState(
                        hasDocuments = state.docs.isNotEmpty(),
                        hasActiveFilters = hasActiveFilters,
                        onNewDoc = onNewDoc,
                        onClearFilters = {
                            viewModel.setTitleFilter("")
                            viewModel.clearTagFilters()
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                    )
                } else if (visibleDocs.isNotEmpty()) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 340.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        contentPadding = PaddingValues(top = 10.dp, bottom = 124.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(visibleDocs, key = { it.id.value }) { doc ->
                            TeachingCard(
                                doc = doc,
                                syncStatus = state.syncStatuses[doc.remoteId ?: doc.id.value],
                                onOpen = { onOpenDoc(doc) },
                                onEdit = { onEditDoc(doc) },
                                onEditMetadata = {
                                    tagEditError = null
                                    pendingTagEdit = doc
                                },
                                onShare = { pendingShare = doc },
                                onDelete = { pendingDelete = doc },
                            )
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { doc ->
        val untitled = stringResource(R.string.study_library_untitled)
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.study_library_delete_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.study_library_delete_message,
                        doc.title.ifBlank { untitled },
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(doc)
                    pendingDelete = null
                }) {
                    Text(stringResource(R.string.study_library_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    pendingShare?.let { doc ->
        ModalBottomSheet(
            onDismissRequest = { pendingShare = null },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            StudyShareSheet(
                title = doc.title.ifBlank { "Sin título" },
                onSharePdf = {
                    pendingShare = null
                    onShare(doc, StudyShareFormat.PDF)
                },
                onShareBib = {
                    pendingShare = null
                    onShare(doc, StudyShareFormat.BIB)
                },
                onRequestPublication = {
                    pendingShare = null
                    onRequestPublication(doc)
                },
            )
        }
    }

    pendingTagEdit?.let { doc ->
        ModalBottomSheet(
            onDismissRequest = {
                if (!isSavingTags) {
                    pendingTagEdit = null
                    tagEditError = null
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            EditTeachingTagsSheet(
                doc = doc,
                isSaving = isSavingTags,
                errorMessage = tagEditError,
                onDismiss = {
                    pendingTagEdit = null
                    tagEditError = null
                },
                onSave = { tags ->
                    isSavingTags = true
                    tagEditError = null
                    viewModel.updateTags(doc, tags) { error ->
                        isSavingTags = false
                        if (error == null) {
                            pendingTagEdit = null
                        } else {
                            tagEditError = tagSaveFailureText
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun EditTeachingTagsSheet(
    doc: StudyDoc,
    isSaving: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit,
) {
    var tagsInput by remember(doc.id.value) {
        mutableStateOf(doc.metadata.tags.joinToString(", "))
    }
    var pendingCustomTag by remember(doc.id.value) { mutableStateOf("") }
    var validationGroupIndex by remember(doc.id.value) { mutableStateOf<Int?>(null) }
    val untitledTeaching = stringResource(R.string.study_tags_untitled)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.88f)
            .navigationBarsPadding(),
    ) {
        Surface(color = BiblionBluePrimary) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 10.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(13.dp),
                    color = BiblionGoldPrimary,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Label,
                            contentDescription = null,
                            tint = BiblionBluePrimary,
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.study_tags_edit_title),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = Color.White,
                    )
                    Text(
                        text = doc.title.ifBlank { untitledTeaching },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = onDismiss, enabled = !isSaving) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.study_tags_close), tint = Color.White)
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .widthIn(max = 680.dp)
                .align(Alignment.CenterHorizontally)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.study_tags_edit_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            StudyTagSelector(
                value = tagsInput,
                onValueChange = {
                    tagsInput = it
                    validationGroupIndex = null
                },
                modifier = Modifier.fillMaxWidth(),
                onPendingCustomTagChange = { pendingCustomTag = it },
                validationGroupIndex = validationGroupIndex,
                enabled = !isSaving,
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 680.dp)
                .align(Alignment.CenterHorizontally)
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            if (errorMessage != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                ) {
                    Text(
                        text = errorMessage,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss, enabled = !isSaving) {
                    Text(stringResource(R.string.study_save_cancel))
                }
                Button(
                    onClick = {
                        val tags = studyTagsForSave(tagsInput, pendingCustomTag)
                        validationGroupIndex = firstIncompleteStudyTagGroupIndex(tags)
                        if (validationGroupIndex == null) onSave(tags)
                    },
                    enabled = !isSaving,
                    shape = RoundedCornerShape(13.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BiblionBluePrimary,
                        contentColor = Color.White,
                    ),
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(stringResource(if (isSaving) R.string.study_tags_saving else R.string.study_tags_save_action))
                }
            }
        }
    }
}

@Composable
private fun StudyLibraryHeader(
    documentCount: Int,
    filteredDocumentCount: Int,
    hasActiveFilters: Boolean,
    onNewDoc: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Mis enseñanzas",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = if (hasActiveFilters) {
                    val documentLabel = if (documentCount == 1) "enseñanza" else "enseñanzas"
                    "$filteredDocumentCount de $documentCount $documentLabel"
                } else if (documentCount == 1) {
                    "1 enseñanza guardada"
                } else {
                    "$documentCount enseñanzas guardadas"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Button(
            onClick = onNewDoc,
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.study_library_new), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun StudyFilterShelf(
    titleFilter: String,
    selectedTags: Set<String>,
    onTitleFilterChange: (String) -> Unit,
    onTagToggled: (String) -> Unit,
    onClearTags: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 2.dp,
    ) {
        Column(modifier = Modifier.padding(vertical = 10.dp)) {
            OutlinedTextField(
                value = titleFilter,
                onValueChange = onTitleFilterChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                placeholder = { Text(stringResource(R.string.study_library_search_title)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = BiblionGoldPrimary,
                    )
                },
                trailingIcon = if (titleFilter.isNotEmpty()) {
                    {
                        IconButton(onClick = { onTitleFilterChange("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.action_clear))
                        }
                    }
                } else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BiblionGoldPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                ),
            )

            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Bookmark,
                    contentDescription = null,
                    tint = BiblionGoldPrimary,
                    modifier = Modifier.size(17.dp),
                )
                Spacer(modifier = Modifier.width(7.dp))
                Text(
                    text = "Filtrar por",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            TeachingTagFilterRow(
                selectedTags = selectedTags,
                onTagToggled = onTagToggled,
                onClearTags = onClearTags,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
    }
}

@Composable
private fun StudyLibraryEmptyState(
    hasDocuments: Boolean,
    hasActiveFilters: Boolean,
    onNewDoc: () -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.widthIn(max = 460.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Surface(
                    modifier = Modifier.size(64.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = BiblionGoldSoft.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, BiblionGoldSoft.copy(alpha = 0.45f)),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (hasDocuments) Icons.Filled.Search else Icons.Filled.AutoStories,
                            contentDescription = null,
                            tint = BiblionGoldPrimary,
                            modifier = Modifier.size(30.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (hasDocuments) {
                        "No encontramos enseñanzas"
                    } else {
                        "Tu espacio de estudio comienza aquí"
                    },
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (hasDocuments) {
                        "Prueba otra búsqueda o limpia los filtros seleccionados."
                    } else {
                        "Escribe, organiza y guarda tu primera enseñanza en BIBLION."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = if (hasDocuments && hasActiveFilters) onClearFilters else onNewDoc,
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(
                        imageVector = if (hasDocuments && hasActiveFilters) {
                            Icons.Filled.Clear
                        } else {
                            Icons.Filled.Add
                        },
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (hasDocuments && hasActiveFilters) {
                            "Limpiar filtros"
                        } else {
                            "Crear mi primera enseñanza"
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun StudyShareSheet(
    title: String,
    onSharePdf: () -> Unit,
    onShareBib: () -> Unit,
    onRequestPublication: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp),
    ) {
        Text(
            text = "Compartir enseñanza",
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
            ),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(18.dp))
        StudyShareAction(
            icon = Icons.Filled.Share,
            title = "Compartir como PDF",
            subtitle = "Documento listo para leer o imprimir",
            onClick = onSharePdf,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        StudyShareAction(
            icon = Icons.AutoMirrored.Filled.MenuBook,
            title = "Exportar archivo .bib",
            subtitle = "Formato editable compatible con BIBLION",
            onClick = onShareBib,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        StudyShareAction(
            icon = Icons.Filled.Bookmark,
            title = "Solicitar publicación",
            subtitle = "Envía la enseñanza para revisión",
            onClick = onRequestPublication,
        )
    }
}

@Composable
private fun StudyShareAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(44.dp),
            shape = RoundedCornerShape(13.dp),
            color = BiblionGoldSoft.copy(alpha = 0.16f),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = BiblionGoldPrimary,
                    modifier = Modifier.size(21.dp),
                )
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

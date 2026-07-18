package com.cristiancogollo.biblion.feature.studydocs.ui.editor

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TextDecrease
import androidx.compose.material.icons.filled.TextIncrease
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.cristiancogollo.biblion.feature.studydocs.domain.StudyDocViewModel
import com.cristiancogollo.biblion.feature.studydocs.domain.TextStyleKind
import com.cristiancogollo.biblion.feature.studydocs.model.BlockId
import com.cristiancogollo.biblion.feature.studydocs.model.DocConfig
import com.cristiancogollo.biblion.feature.studydocs.model.DocTagGroups

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun StudyDocEditorScreen(
    splitViewModel: com.cristiancogollo.biblion.feature.studydocs.domain.StudyDocSplitViewModel? = null,
    viewModel: com.cristiancogollo.biblion.feature.studydocs.domain.StudyDocViewModel? = null,
    remoteId: String? = null,
    onBack: () -> Unit = {},
    isSplitMode: Boolean = false,
    onFocusModeChanged: () -> Unit = {}
) {
    // Usar splitViewModel si está disponible (modo split), sino usar viewModel (modo standalone)
    val editorState by (splitViewModel?.editorState ?: viewModel?.uiState)?.collectAsState()
        ?: remember { mutableStateOf(com.cristiancogollo.biblion.feature.studydocs.domain.StudyEditorUiState()) }

    val focusRequesters = remember { mutableStateMapOf<BlockId, FocusRequester>() }
    var showSaveDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(remoteId) {
        if (remoteId != null) {
            splitViewModel?.loadByRemoteId(remoteId) ?: viewModel?.loadByRemoteId(remoteId)
        } else {
            splitViewModel?.newDraft() ?: viewModel?.newDraft()
        }
    }

    LaunchedEffect(editorState.lastSavedAt) {
        if (editorState.lastSavedAt != null && !editorState.isSaving) {
            Toast.makeText(context, "Guardado", Toast.LENGTH_SHORT).show()
        }
    }

    if (showSaveDialog) {
        SaveTeachingDialog(
            currentTitle = editorState.doc.title,
            currentTags = editorState.doc.metadata.tags,
            onSave = { title, tags ->
                splitViewModel?.saveNow(title, tags) ?: viewModel?.saveNow(title, tags)
                showSaveDialog = false
            },
            onDismiss = { showSaveDialog = false },
        )
    }

    if (isSplitMode) {
        StudyModeEditorPanel(
            editorState = editorState,
            focusRequesters = focusRequesters,
            splitViewModel = splitViewModel,
            viewModel = viewModel,
            onSaveClick = { showSaveDialog = true },
            onBack = onBack,
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showSaveDialog = true }) {
                            Icon(Icons.Filled.Save, "Guardar")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                EditorToolbar(
                    activeFormat = editorState.activeFormat,
                    currentFontSize = editorState.doc.blocks
                        .firstOrNull { it.id == editorState.activeBlockId }?.fontSize
                        ?: DocConfig.DEFAULT_FONT_SIZE,
                    currentFontFamily = editorState.doc.blocks
                        .firstOrNull { it.id == editorState.activeBlockId }?.fontFamily,
                    onToggleStyle = { kind ->
                        splitViewModel?.applyStyleToActive(kind) ?: viewModel?.applyStyleToActive(kind)
                    },
                    onTextColor = { color ->
                        splitViewModel?.setActiveTextColor(color) ?: viewModel?.setActiveTextColor(color)
                    },
                    onBackgroundColor = { color ->
                        splitViewModel?.setActiveBackgroundColor(color) ?: viewModel?.setActiveBackgroundColor(color)
                    },
                    onClearColor = {
                        splitViewModel?.clearActiveColor() ?: viewModel?.clearActiveColor()
                    },
                    onStepFontSize = { delta ->
                        splitViewModel?.stepFontSizeActive(delta) ?: viewModel?.stepFontSizeActive(delta)
                    },
                    onFontFamily = { family ->
                        splitViewModel?.setActiveFontFamily(family) ?: viewModel?.setActiveFontFamily(family)
                    },
                    onCycleAlignment = {
                        val blockId = editorState.activeBlockId
                        if (blockId != null) {
                            splitViewModel?.cycleBlockAlignment(blockId) ?: viewModel?.cycleBlockAlignment(blockId)
                        }
                    },
                    onInsertBlock = { type ->
                        splitViewModel?.insertBlock(editorState.activeBlockId, type)
                            ?: viewModel?.insertBlock(editorState.activeBlockId, type)
                    },
                )
                HorizontalDivider()

                EditorLazyColumn(
                    blocks = editorState.doc.blocks,
                    activeBlockId = editorState.activeBlockId,
                    focusRequesters = focusRequesters,
                    splitViewModel = splitViewModel,
                    viewModel = viewModel,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
private fun SplitModeEditorContent(
    editorState: com.cristiancogollo.biblion.feature.studydocs.domain.StudyEditorUiState,
    focusRequesters: SnapshotStateMap<BlockId, FocusRequester>,
    splitViewModel: com.cristiancogollo.biblion.feature.studydocs.domain.StudyDocSplitViewModel?,
    viewModel: StudyDocViewModel?,
    onSaveClick: () -> Unit,
    onFocusModeChanged: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Toolbar deslizable en la parte superior
        SwipeableEditorToolbar(
            activeFormat = editorState.activeFormat,
            currentFontSize = editorState.doc.blocks
                .firstOrNull { it.id == editorState.activeBlockId }?.fontSize ?: 16,
            onToggleStyle = { kind ->
                splitViewModel?.applyStyleToActive(kind) ?: viewModel?.applyStyleToActive(kind)
            },
            onTextColor = { color ->
                splitViewModel?.setActiveTextColor(color) ?: viewModel?.setActiveTextColor(color)
            },
            onBackgroundColor = { color ->
                splitViewModel?.setActiveBackgroundColor(color) ?: viewModel?.setActiveBackgroundColor(color)
            },
            onClearColor = {
                splitViewModel?.clearActiveColor() ?: viewModel?.clearActiveColor()
            },
            onStepFontSize = { delta ->
                splitViewModel?.stepFontSizeActive(delta) ?: viewModel?.stepFontSizeActive(delta)
            },
            onCycleAlignment = {
                editorState.activeBlockId?.let {
                    splitViewModel?.cycleBlockAlignment(it) ?: viewModel?.cycleBlockAlignment(it)
                }
            },
            onInsertBlock = { type ->
                splitViewModel?.insertBlock(editorState.activeBlockId, type)
                    ?: viewModel?.insertBlock(editorState.activeBlockId, type)
            },
            onSave = onSaveClick,
            modifier = Modifier.fillMaxWidth()
        )

        HorizontalDivider()

        // Contenido del editor
        Box(modifier = Modifier.weight(1f)) {
            EditorLazyColumn(
                blocks = editorState.doc.blocks,
                activeBlockId = editorState.activeBlockId,
                focusRequesters = focusRequesters,
                splitViewModel = splitViewModel,
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize(),
            )

            // IME Accessory Bar (cuando el teclado está visible)
            val imeVisible = WindowInsets.isImeVisible
            if (imeVisible) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .imePadding()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ToolbarChip(Icons.Filled.FormatBold, editorState.activeFormat.bold) {
                        splitViewModel?.applyStyleToActive(TextStyleKind.Bold)
                            ?: viewModel?.applyStyleToActive(TextStyleKind.Bold)
                    }
                    ToolbarChip(Icons.Filled.FormatItalic, editorState.activeFormat.italic) {
                        splitViewModel?.applyStyleToActive(TextStyleKind.Italic)
                            ?: viewModel?.applyStyleToActive(TextStyleKind.Italic)
                    }
                    ToolbarChip(Icons.Filled.FormatUnderlined, editorState.activeFormat.underline) {
                        splitViewModel?.applyStyleToActive(TextStyleKind.Underline)
                            ?: viewModel?.applyStyleToActive(TextStyleKind.Underline)
                    }

                    Spacer(Modifier.width(8.dp))

                    IconButton(onClick = {
                        splitViewModel?.stepFontSizeActive(-1) ?: viewModel?.stepFontSizeActive(-1)
                    }) {
                        Icon(Icons.Filled.TextDecrease, "Reducir", Modifier.size(18.dp))
                    }
                    Text(
                        "${editorState.doc.blocks.firstOrNull { it.id == editorState.activeBlockId }?.fontSize ?: 16}",
                        style = MaterialTheme.typography.labelSmall,
                    )
                    IconButton(onClick = {
                        splitViewModel?.stepFontSizeActive(1) ?: viewModel?.stepFontSizeActive(1)
                    }) {
                        Icon(Icons.Filled.TextIncrease, "Aumentar", Modifier.size(18.dp))
                    }

                    Spacer(Modifier.width(8.dp))

                    var showInsertMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showInsertMenu = true }) {
                            Icon(Icons.Filled.Add, "Insertar", Modifier.size(18.dp))
                        }
                        DropdownMenu(expanded = showInsertMenu, onDismissRequest = { showInsertMenu = false }) {
                            listOf(
                                "paragraph" to "Parrafo", "heading1" to "Titulo 1",
                                "heading2" to "Titulo 2", "heading3" to "Titulo 3",
                                "bullet" to "Vinetas", "ordered" to "Numerada", "quote" to "Cita",
                            ).forEach { (type, name) ->
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = {
                                        splitViewModel?.insertBlock(editorState.activeBlockId, type)
                                            ?: viewModel?.insertBlock(editorState.activeBlockId, type)
                                        showInsertMenu = false
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorLazyColumn(
    blocks: List<com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock>,
    activeBlockId: BlockId?,
    focusRequesters: SnapshotStateMap<BlockId, FocusRequester>,
    splitViewModel: com.cristiancogollo.biblion.feature.studydocs.domain.StudyDocSplitViewModel?,
    viewModel: StudyDocViewModel?,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
        itemsIndexed(
            items = blocks,
            key = { _, block -> block.id.value },
        ) { index, block ->
            val richState = splitViewModel?.blockRichStates?.get(block.id)
                ?: viewModel?.blockRichStates?.get(block.id)
            if (richState != null) {
                BlockRenderer(
                    block = block,
                    blockIndex = index,
                    richState = richState,
                    isActive = activeBlockId == block.id,
                    focusRequesters = focusRequesters,
                    splitViewModel = splitViewModel,
                    viewModel = viewModel,
                )
            }
        }
    }
}

@Composable
private fun ToolbarChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        Icon(
            icon, null,
            tint = if (isActive) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun SaveTeachingDialog(
    currentTitle: String,
    currentTags: List<String>,
    onSave: (String, List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    var title by remember { mutableStateOf(currentTitle) }
    val selectedTags = remember { mutableStateListOf<String>().also { it.addAll(currentTags) } }
    var showError by remember { mutableStateOf(false) }

    fun validate(): Boolean {
        val hasPurpose = selectedTags.any { it in DocTagGroups.PURPOSE_TAGS }
        val hasAudience = selectedTags.any { it in DocTagGroups.AUDIENCE_TAGS }
        val hasTopic = selectedTags.any { it in DocTagGroups.TOPIC_TAGS }
        val hasState = selectedTags.any { it in DocTagGroups.STATE_TAGS }
        return hasPurpose && hasAudience && hasTopic && hasState
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Guardar ensenanza") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; showError = false },
                    label = { Text("Titulo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                TagsSection("Proposito", DocTagGroups.PURPOSE_TAGS, selectedTags, showError)
                TagsSection("Audiencia", DocTagGroups.AUDIENCE_TAGS, selectedTags, showError)
                TagsSection("Tema", DocTagGroups.TOPIC_TAGS, selectedTags, showError)
                TagsSection("Estado", DocTagGroups.STATE_TAGS, selectedTags, showError, singleSelect = true)

                if (showError) {
                    Text(
                        "Selecciona al menos una etiqueta en cada seccion.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!validate()) {
                        showError = true
                    } else {
                        onSave(title.ifBlank { "Sin titulo" }, selectedTags.toList())
                    }
                },
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}

@Composable
private fun TagsSection(
    label: String,
    tags: List<String>,
    selectedTags: MutableList<String>,
    showError: Boolean = false,
    singleSelect: Boolean = false,
) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (showError) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.primary,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            tags.forEach { tag ->
                val isSelected = tag in selectedTags
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        if (singleSelect) {
                            // Single-select: quitar todos los de este grupo, agregar solo el nuevo
                            tags.forEach { selectedTags.remove(it) }
                            if (!isSelected) selectedTags.add(tag)
                        } else {
                            if (isSelected) selectedTags.remove(tag)
                            else selectedTags.add(tag)
                        }
                    },
                    label = { Text("#$tag", style = MaterialTheme.typography.labelSmall) },
                )
            }
        }
    }
}

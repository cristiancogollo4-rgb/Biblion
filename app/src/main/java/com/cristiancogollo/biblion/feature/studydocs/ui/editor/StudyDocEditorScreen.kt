package com.cristiancogollo.biblion.feature.studydocs.ui.editor

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.cristiancogollo.biblion.core.ui.StudyModeLandscapeLock
import com.cristiancogollo.biblion.feature.studydocs.domain.StudyDocViewModel
import com.cristiancogollo.biblion.feature.studydocs.model.DocConfig
import com.cristiancogollo.biblion.feature.studydocs.model.hasPersistableTitle
import com.cristiancogollo.biblion.feature.studydocs.ui.pagination.PaginatedSheet

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun StudyDocEditorScreen(
    splitViewModel: com.cristiancogollo.biblion.feature.studydocs.domain.StudyDocSplitViewModel? = null,
    viewModel: com.cristiancogollo.biblion.feature.studydocs.domain.StudyDocViewModel? = null,
    remoteId: String? = null,
    onBack: () -> Unit = {},
    isSplitMode: Boolean = false,
    onFocusModeChanged: () -> Unit = {},
    navController: androidx.navigation.NavController? = null,
    isDarkTheme: Boolean = false,
    onToggleDarkTheme: (Boolean) -> Unit = {},
    isActive: Boolean = true,
) {
    StudyModeLandscapeLock()

    val configuration = LocalConfiguration.current
    val useCompactStudyLayout = isSplitMode && shouldUseCompactStudyLayout(
        widthDp = configuration.screenWidthDp,
        heightDp = configuration.screenHeightDp,
    )
    var compactStudyPane by rememberSaveable { mutableStateOf("document") }
    val compactDocumentActive = compactStudyPane == "document"
    val focusManager = LocalFocusManager.current

    // Usar splitViewModel si está disponible (modo split), sino usar viewModel (modo standalone)
    val editorState by (splitViewModel?.editorState ?: viewModel?.uiState)?.collectAsState()
        ?: remember { mutableStateOf(com.cristiancogollo.biblion.feature.studydocs.domain.StudyEditorUiState()) }

    var showSaveDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val standaloneZoomState = rememberDocumentZoomState()
    var isFullScreen by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    val requestExit: (String) -> Unit = { source ->
        val hasDraftContent = editorState.doc.blocks.size > 1 || editorState.doc.blocks.any { block ->
            block.toStyledTextList().any { it.raw.isNotBlank() }
        }
        val isExistingTeaching = editorState.doc.remoteId != null
        val hasPendingNewTeaching = !isExistingTeaching &&
            (editorState.hasUnsavedChanges || hasDraftContent || editorState.doc.title.isNotBlank())
        val shouldWarn = !editorState.isSaving && if (isExistingTeaching) {
            editorState.hasUnsavedChanges
        } else {
            hasPendingNewTeaching
        }
        Log.d(
            "BIBLION_STUDY_EXIT",
            "event source=$source existing=$isExistingTeaching unsaved=${editorState.hasUnsavedChanges} " +
                "saving=${editorState.isSaving} blocks=${editorState.doc.blocks.size} " +
                "hasDraftContent=$hasDraftContent titleBlank=${editorState.doc.title.isBlank()} " +
                "showBefore=$showDiscardDialog shouldWarn=$shouldWarn",
        )
        if (shouldWarn) {
            showDiscardDialog = true
            Log.d("BIBLION_STUDY_EXIT", "event source=$source action=show_discard_dialog")
        } else {
            Log.d("BIBLION_STUDY_EXIT", "event source=$source action=onBack_without_dialog")
            onBack()
        }
    }

    BackHandler(enabled = isActive && (!useCompactStudyLayout || compactDocumentActive)) {
        Log.d("BIBLION_STUDY_EXIT", "event source=system_back handler_invoked")
        requestExit("system_back")
    }

    LaunchedEffect(showDiscardDialog) {
        Log.d("BIBLION_STUDY_EXIT", "dialog_state showDiscardDialog=$showDiscardDialog")
    }

    LaunchedEffect(remoteId) {
        Log.d("BIBLION_STUDY", "StudyDocEditorScreen LaunchedEffect remoteId=$remoteId viewModel=${viewModel != null} splitViewModel=${splitViewModel != null}")
    }

    LaunchedEffect(
        isSplitMode,
        configuration.screenWidthDp,
        configuration.screenHeightDp,
        useCompactStudyLayout,
    ) {
        if (isSplitMode) {
            Log.d(
                "BIBLION_STUDY_LAYOUT",
                "editor widthDp=${configuration.screenWidthDp} " +
                    "heightDp=${configuration.screenHeightDp} compact=$useCompactStudyLayout",
            )
        }
    }

    LaunchedEffect(editorState.isLoading, editorState.doc.blocks.size) {
        Log.d("BIBLION_STUDY", "StudyDocEditorScreen state isLoading=${editorState.isLoading} blocks=${editorState.doc.blocks.size} activeBlockId=${editorState.activeBlockId} lastError=${editorState.lastError}")
    }

    LaunchedEffect(remoteId) {
        val isNewDocument = remoteId == null || remoteId == "new"
        Log.d(
            "BIBLION_STUDY_EXIT",
            "initializeEditor remoteId=$remoteId isNewDocument=$isNewDocument",
        )
        if (!isNewDocument) {
            splitViewModel?.loadByRemoteId(remoteId) ?: viewModel?.loadByRemoteId(remoteId)
        } else {
            splitViewModel?.newDraft() ?: viewModel?.newDraft()
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
        if (editorState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            // Split 50/50 con el editor a la derecha y la app a la izquierda.
            // BiblionTheme envuelve ambos paneles para que el dark mode los afecte.
            com.cristiancogollo.biblion.ui.theme.BiblionTheme(darkTheme = isDarkTheme) {
                if (useCompactStudyLayout) {
                    val activePane = if (compactDocumentActive) {
                        CompactStudyPane.Document
                    } else {
                        CompactStudyPane.Bible
                    }
                    CompactStudyLayoutController(
                        activePane = activePane,
                        onPaneSelected = { pane ->
                            focusManager.clearFocus(force = true)
                            compactStudyPane = if (pane == CompactStudyPane.Document) {
                                "document"
                            } else {
                                "bible"
                            }
                        },
                        biblePane = {
                            BibleReaderPane(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.surface),
                                navController = navController,
                                isDarkTheme = isDarkTheme,
                                onToggleDarkTheme = onToggleDarkTheme,
                            )
                        },
                        documentPane = {
                            StudyModeEditorPanel(
                                modifier = Modifier.fillMaxSize(),
                                editorState = editorState,
                                splitViewModel = splitViewModel,
                                viewModel = viewModel,
                                onSaveClick = { showSaveDialog = true },
                                onBack = { requestExit("panel_back") },
                                isFullScreen = true,
                                isDarkTheme = isDarkTheme,
                                onToggleDarkTheme = onToggleDarkTheme,
                                navController = navController,
                                showFullScreenToggle = false,
                                isCompactLayout = true,
                            )
                        },
                    )
                } else {
                    Row(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))) {
                        if (!isFullScreen) {
                            BibleReaderPane(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(MaterialTheme.colorScheme.surface),
                                navController = navController,
                                isDarkTheme = isDarkTheme,
                                onToggleDarkTheme = onToggleDarkTheme,
                            )
                            VerticalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant,
                                thickness = 1.dp,
                            )
                        }
                        StudyModeEditorPanel(
                            modifier = if (isFullScreen) Modifier.fillMaxSize() else Modifier.weight(1f).fillMaxHeight(),
                            editorState = editorState,
                            splitViewModel = splitViewModel,
                            viewModel = viewModel,
                            onSaveClick = { showSaveDialog = true },
                            onBack = { requestExit("panel_back") },
                            isFullScreen = isFullScreen,
                            onToggleFullScreen = { isFullScreen = !isFullScreen },
                            isDarkTheme = isDarkTheme,
                            onToggleDarkTheme = onToggleDarkTheme,
                            navController = navController,
                        )
                    }
                }
            }
        }
    } else {
        if (editorState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = when {
                                editorState.lastError != null -> "Error al guardar"
                                editorState.isSaving -> "Guardando..."
                                editorState.hasUnsavedChanges -> "Cambios sin guardar"
                                else -> "Guardado"
                            },
                            style = MaterialTheme.typography.labelMedium,
                        )
                    },
                    navigationIcon = {
                            IconButton(onClick = {
                                Log.d("BIBLION_STUDY_EXIT", "event source=top_bar_back clicked")
                                requestExit("top_bar_back")
                            }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                        }
                    },
                    actions = {
                        ZoomMenu(zoomState = standaloneZoomState, showStepButtons = true)
                        IconButton(
                            onClick = { splitViewModel?.undo() ?: viewModel?.undo() },
                            enabled = editorState.canUndo,
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Undo, "Deshacer")
                        }
                        IconButton(
                            onClick = { splitViewModel?.redo() ?: viewModel?.redo() },
                            enabled = editorState.canRedo,
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Redo, "Rehacer")
                        }
                        IconButton(
                            onClick = { showSaveDialog = true },
                            enabled = editorState.doc.hasPersistableTitle(),
                        ) {
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
                    onSetFontSize = { size ->
                        splitViewModel?.setFontSizeActive(size) ?: viewModel?.setFontSizeActive(size)
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

                PaginatedSheet(
                    blocks = editorState.doc.blocks,
                    isEditing = true,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    zoomState = standaloneZoomState,
                    contentFragmentRenderer = { fragment, _ ->
                        val itemIndex = when (fragment) {
                            is com.cristiancogollo.biblion.feature.studydocs.ui.pagination.PageFragment.ListItemSlice -> fragment.itemIndex
                            is com.cristiancogollo.biblion.feature.studydocs.ui.pagination.PageFragment.OrderedListItemSlice -> fragment.itemIndex
                            else -> null
                        }
                        val richState = splitViewModel?.richStateFor(fragment.originBlockId, itemIndex)
                            ?: viewModel?.richStateFor(fragment.originBlockId, itemIndex)
                        val isFirstFragment = when (fragment) {
                            is com.cristiancogollo.biblion.feature.studydocs.ui.pagination.PageFragment.ParagraphSlice -> fragment.charStart == 0
                            is com.cristiancogollo.biblion.feature.studydocs.ui.pagination.PageFragment.HeadingSlice -> fragment.charStart == 0
                            is com.cristiancogollo.biblion.feature.studydocs.ui.pagination.PageFragment.ListItemSlice -> fragment.charStart == 0
                            is com.cristiancogollo.biblion.feature.studydocs.ui.pagination.PageFragment.OrderedListItemSlice -> fragment.charStart == 0
                            is com.cristiancogollo.biblion.feature.studydocs.ui.pagination.PageFragment.VerseSlice -> fragment.charStart == 0
                            is com.cristiancogollo.biblion.feature.studydocs.ui.pagination.PageFragment.QuoteSlice -> fragment.charStart == 0
                            is com.cristiancogollo.biblion.feature.studydocs.ui.pagination.PageFragment.Whole -> true
                        }
                        val isOwner = editorState.activeBlockId == fragment.originBlockId &&
                            (itemIndex == null || editorState.activeListItemIndex == itemIndex) &&
                            isFirstFragment
                        if (richState != null ||
                            fragment is com.cristiancogollo.biblion.feature.studydocs.ui.pagination.PageFragment.VerseSlice ||
                            fragment is com.cristiancogollo.biblion.feature.studydocs.ui.pagination.PageFragment.ListItemSlice ||
                            fragment is com.cristiancogollo.biblion.feature.studydocs.ui.pagination.PageFragment.OrderedListItemSlice
                        ) {
                            UnifiedBlockRenderer(
                                fragment = fragment,
                                allBlocks = editorState.doc.blocks,
                                isEditing = true,
                                isOwnerFragment = isOwner,
                                richState = richState,
                                isActive = isOwner,
                                focusRequest = editorState.focusRequest,
                                splitViewModel = splitViewModel,
                                viewModel = viewModel,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    },
                    contentBlockRenderer = { _, _ -> },
                    contentPagedEditorRenderer = { unit, baseDensity ->
                        val richState = splitViewModel?.richStateFor(
                            unit.key.blockId,
                            unit.key.itemIndex,
                        ) ?: viewModel?.richStateFor(
                            unit.key.blockId,
                            unit.key.itemIndex,
                        )
                        if (richState != null) {
                            PagedEditorUnitRenderer(
                                unit = unit,
                                allBlocks = editorState.doc.blocks,
                                richState = richState,
                                isActive = editorState.activeBlockId == unit.key.blockId &&
                                    editorState.activeListItemIndex == unit.key.itemIndex,
                                focusRequest = editorState.focusRequest,
                                baseDensity = baseDensity,
                                splitViewModel = splitViewModel,
                                viewModel = viewModel,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    },
                )
            }
        }
    }
    }

    if (showDiscardDialog) {
        Dialog(
            onDismissRequest = {
                Log.d("BIBLION_STUDY_EXIT", "dialog_event action=custom_dismiss_request")
                showDiscardDialog = false
            },
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Cambios sin guardar", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Tienes contenido en esta enseñanza que todavía no has guardado. Si sales ahora, se perderá.",
                        modifier = Modifier.padding(top = 16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End,
                    ) {
                        TextButton(onClick = {
                            Log.d("BIBLION_STUDY_EXIT", "dialog_event action=custom_continue_editing")
                            showDiscardDialog = false
                        }) { Text("Seguir editando") }
                        TextButton(onClick = {
                            Log.d("BIBLION_STUDY_EXIT", "dialog_event action=custom_discard_and_exit")
                            showDiscardDialog = false
                            splitViewModel?.discardDraft() ?: viewModel?.discardDraft()
                            onBack()
                        }) { Text("Salir sin guardar", color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
    }
}

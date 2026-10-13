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
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.cristiancogollo.biblion.feature.bibi.ui.BibiStudyFloatingWindow
import com.cristiancogollo.biblion.R
import com.cristiancogollo.biblion.feature.achievements.domain.AchievementEvent
import com.cristiancogollo.biblion.feature.achievements.tracking.AchievementTracker
import com.cristiancogollo.biblion.feature.bibi.ui.StudyBibiController
import com.cristiancogollo.biblion.feature.studydocs.domain.StudyDocViewModel
import com.cristiancogollo.biblion.feature.studydocs.model.DocConfig
import com.cristiancogollo.biblion.feature.studydocs.model.capabilities
import com.cristiancogollo.biblion.feature.studydocs.ui.pagination.PaginatedSheet
import com.cristiancogollo.biblion.feature.studydocs.ui.pagination.PagedEditorUnitKey
import com.cristiancogollo.biblion.feature.studydocs.ui.pagination.SheetViewMode
import com.cristiancogollo.biblion.ui.theme.BiblionThemeMode
import kotlinx.coroutines.launch

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
    themeMode: BiblionThemeMode = if (isDarkTheme) BiblionThemeMode.DARK else BiblionThemeMode.LIGHT,
    onThemeModeChange: (BiblionThemeMode) -> Unit = {},
    isActive: Boolean = true,
    currentUserName: String? = null,
    bibiController: StudyBibiController? = null,
) {
    val configuration = LocalConfiguration.current
    val useCompactStudyLayout = isSplitMode && shouldUseCompactStudyLayout(
        widthDp = configuration.screenWidthDp,
        heightDp = configuration.screenHeightDp,
    )
    var compactStudyPane by rememberSaveable { mutableStateOf("document") }
    val compactDocumentActive = compactStudyPane == "document"
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Usar splitViewModel si está disponible (modo split), sino usar viewModel (modo standalone)
    val editorState by (splitViewModel?.editorState ?: viewModel?.uiState)?.collectAsState()
        ?: remember { mutableStateOf(com.cristiancogollo.biblion.feature.studydocs.domain.StudyEditorUiState()) }

    var showSaveDialog by remember { mutableStateOf(false) }
    var explicitSavePending by remember { mutableStateOf(false) }
    var pendingSaveBaseline by remember { mutableStateOf<Long?>(null) }
    var showSavingExitNotice by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val standaloneZoomState = rememberDocumentZoomState()
    val standaloneFluidDefault = configuration.screenWidthDp < 900 ||
        configuration.screenHeightDp < 480
    var standaloneViewMode by rememberSaveable(standaloneFluidDefault) {
        mutableStateOf(
            if (standaloneFluidDefault) SheetViewMode.PAGELESS else SheetViewMode.PAGINATED
        )
    }
    var compactActionsExpanded by remember { mutableStateOf(false) }
    var isFullScreen by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    val internalBibiController = remember { StudyBibiController() }
    val activeBibiController = bibiController ?: internalBibiController
    var requestedBibiPassage by remember {
        mutableStateOf<com.cristiancogollo.biblion.feature.bibi.model.BibiPassage?>(null)
    }

    val onVerseComparisonSelected:
        (com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock.Verse, String?) -> Unit =
        { block, version ->
            if (version == null) {
                splitViewModel?.updateVerseComparisons(block.id, emptyList(), emptyMap())
                    ?: viewModel?.updateVerseComparisons(block.id, emptyList(), emptyMap())
            } else {
                scope.launch {
                    val text = loadVerseRangeText(context, version, block)
                    splitViewModel?.updateVerseComparisons(
                        block.id,
                        listOf(version),
                        mapOf(version to text),
                    ) ?: viewModel?.updateVerseComparisons(
                        block.id,
                        listOf(version),
                        mapOf(version to text),
                    )
                }
            }
    }
    val onVerseSelected:
        (com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock.Verse) -> Unit =
        { block ->
            splitViewModel?.setActiveBlock(block.id) ?: viewModel?.setActiveBlock(block.id)
        }
    val onVerseDelete:
        (com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock.Verse) -> Unit =
        { block ->
            splitViewModel?.deleteBlock(block.id) ?: viewModel?.deleteBlock(block.id)
        }

    val requestExit: (String) -> Unit = { source ->
        if (editorState.isSaving) {
            showSavingExitNotice = true
        } else {
            val hasDraftContent = editorState.doc.blocks.size > 1 || editorState.doc.blocks.any { block ->
                block.toStyledTextList().any { it.raw.isNotBlank() }
            }
            val isExistingTeaching = editorState.doc.remoteId != null
            val hasPendingNewTeaching = !isExistingTeaching &&
                (editorState.hasUnsavedChanges || hasDraftContent || editorState.doc.title.isNotBlank())
            val shouldWarn = if (isExistingTeaching) {
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
    }

    BackHandler(enabled = isActive && (!useCompactStudyLayout || compactDocumentActive)) {
        if (activeBibiController.isOpen) {
            activeBibiController.close()
        } else {
            Log.d("BIBLION_STUDY_EXIT", "event source=system_back handler_invoked")
            requestExit("system_back")
        }
    }

    LaunchedEffect(showDiscardDialog) {
        Log.d("BIBLION_STUDY_EXIT", "dialog_state showDiscardDialog=$showDiscardDialog")
    }

    LaunchedEffect(editorState.isSaving) {
        if (!editorState.isSaving) showSavingExitNotice = false
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

    LaunchedEffect(editorState.lastSavedAt, editorState.isSaving, editorState.lastError) {
        if (explicitSavePending && !editorState.isSaving && editorState.lastSavedAt != null &&
            editorState.lastSavedAt != pendingSaveBaseline && editorState.lastError == null
        ) {
            explicitSavePending = false
            showSaveDialog = false
            val doc = editorState.doc
            AchievementTracker.track(
                context,
                AchievementEvent.StudySavedLocally(
                    documentId = doc.remoteId ?: doc.id.value,
                    hasTitle = doc.title.isNotBlank(),
                    hasContent = doc.blocks.any { it.plainText().isNotBlank() },
                    isExplicitSave = true,
                ),
            )
            if (!isSplitMode) {
                snackbarHostState.showSnackbar(context.getString(R.string.study_save_done))
            }
        }
    }

    val achievementSnapshot = remember(editorState.doc.blocks) {
        val blocks = editorState.doc.blocks
        val types = blocks
            .map { it::class.simpleName.orEmpty() }
            .filter { it.isNotBlank() }
            .toSet()
        val hasVerse = blocks.any {
            it is com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock.Verse
        }
        val maxVersions = blocks
            .filterIsInstance<com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock.Verse>()
            .maxOfOrNull { block -> block.displayedVersions().size }
            ?: 0
        Triple(types, hasVerse, maxVersions)
    }
    val achievementDocumentId = editorState.doc.remoteId ?: editorState.doc.id.value
    LaunchedEffect(achievementDocumentId, achievementSnapshot) {
        val (types, hasVerse, maxVersions) = achievementSnapshot
        AchievementTracker.track(
            context,
            AchievementEvent.StudyBlockTypesChanged(achievementDocumentId, types),
            countAsMeaningfulUse = false,
        )
        if (hasVerse) {
            AchievementTracker.track(
                context,
                AchievementEvent.VerseBlockInserted(achievementDocumentId),
                countAsMeaningfulUse = false,
            )
        }
        if (maxVersions >= 2) {
            AchievementTracker.track(
                context,
                AchievementEvent.VerseVersionCompared(achievementDocumentId, maxVersions),
                countAsMeaningfulUse = false,
            )
        }
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
            isSaving = editorState.isSaving,
            saveError = if (explicitSavePending) editorState.lastError else null,
            onSave = { title, tags ->
                val baseline = editorState.lastSavedAt
                val accepted = splitViewModel?.saveNow(title, tags)
                    ?: viewModel?.saveNow(title, tags)
                    ?: false
                if (accepted) {
                    explicitSavePending = true
                    pendingSaveBaseline = baseline
                }
            },
            onDismiss = {
                if (!editorState.isSaving) {
                    showSaveDialog = false
                    explicitSavePending = false
                }
            },
        )
    }

    if (showSavingExitNotice) {
        AlertDialog(
            onDismissRequest = { showSavingExitNotice = false },
            text = { Text(stringResource(R.string.study_save_wait_exit)) },
            confirmButton = {
                TextButton(onClick = { showSavingExitNotice = false }) {
                    Text(stringResource(android.R.string.ok))
                }
            },
        )
    }

    if (isSplitMode) {
        if (editorState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            // Split 50/50 con el editor a la derecha y la app a la izquierda.
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
                                isDarkTheme = isDarkTheme,
                                onToggleDarkTheme = onToggleDarkTheme,
                                themeMode = themeMode,
                                onThemeModeChange = onThemeModeChange,
                                requestedPassage = requestedBibiPassage,
                                onRequestedPassageConsumed = { requestedBibiPassage = null },
                                onInsertVerseCitation = { group, version ->
                                    splitViewModel?.insertVerseAsQuote(
                                        book = group.bookName,
                                        chapter = group.chapter,
                                        verseStart = group.verseStart,
                                        verseEnd = group.verseEnd,
                                        text = group.text,
                                        version = version,
                                        verseNumbers = group.verseNumbers,
                                    ) ?: viewModel?.insertVerseCitation(
                                        book = group.bookName,
                                        chapter = group.chapter,
                                        verseStart = group.verseStart,
                                        verseEnd = group.verseEnd,
                                        text = group.text,
                                        version = version,
                                        verseNumbers = group.verseNumbers,
                                    )
                                },
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
                                themeMode = themeMode,
                                onThemeModeChange = onThemeModeChange,
                                navController = navController,
                                showFullScreenToggle = false,
                                isCompactLayout = true,
                                onBibiClick = activeBibiController::open,
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
                                isDarkTheme = isDarkTheme,
                                onToggleDarkTheme = onToggleDarkTheme,
                                themeMode = themeMode,
                                onThemeModeChange = onThemeModeChange,
                                requestedPassage = requestedBibiPassage,
                                onRequestedPassageConsumed = { requestedBibiPassage = null },
                                onInsertVerseCitation = { group, version ->
                                    splitViewModel?.insertVerseAsQuote(
                                        book = group.bookName,
                                        chapter = group.chapter,
                                        verseStart = group.verseStart,
                                        verseEnd = group.verseEnd,
                                        text = group.text,
                                        version = version,
                                        verseNumbers = group.verseNumbers,
                                    ) ?: viewModel?.insertVerseCitation(
                                        book = group.bookName,
                                        chapter = group.chapter,
                                        verseStart = group.verseStart,
                                        verseEnd = group.verseEnd,
                                        text = group.text,
                                        version = version,
                                        verseNumbers = group.verseNumbers,
                                    )
                                },
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
                            themeMode = themeMode,
                            onThemeModeChange = onThemeModeChange,
                            navController = navController,
                            onBibiClick = activeBibiController::open,
                        )
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
                        ZoomMenu(zoomState = standaloneZoomState, showStepButtons = !standaloneFluidDefault)
                        if (standaloneFluidDefault) {
                            Box {
                                IconButton(onClick = { compactActionsExpanded = true }) {
                                    Icon(Icons.Default.MoreVert, stringResource(R.string.study_action_options))
                                }
                                DropdownMenu(
                                    expanded = compactActionsExpanded,
                                    onDismissRequest = { compactActionsExpanded = false },
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.study_action_undo)) },
                                        onClick = {
                                            compactActionsExpanded = false
                                            splitViewModel?.undo() ?: viewModel?.undo()
                                        },
                                        enabled = editorState.canUndo,
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.study_action_redo)) },
                                        onClick = {
                                            compactActionsExpanded = false
                                            splitViewModel?.redo() ?: viewModel?.redo()
                                        },
                                        enabled = editorState.canRedo,
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(
                                            if (standaloneViewMode == SheetViewMode.PAGINATED) R.string.view_mode_pageless
                                            else R.string.view_mode_paginated
                                        )) },
                                        onClick = {
                                            compactActionsExpanded = false
                                            standaloneViewMode = if (standaloneViewMode == SheetViewMode.PAGINATED) {
                                                SheetViewMode.PAGELESS
                                            } else SheetViewMode.PAGINATED
                                        },
                                    )
                                }
                            }
                        } else {
                            IconButton(onClick = {
                                standaloneViewMode = if (standaloneViewMode == SheetViewMode.PAGINATED) {
                                    SheetViewMode.PAGELESS
                                } else SheetViewMode.PAGINATED
                            }) {
                                Icon(
                                    if (standaloneViewMode == SheetViewMode.PAGINATED) Icons.Default.ViewStream
                                    else Icons.Default.Description,
                                    contentDescription = stringResource(
                                        if (standaloneViewMode == SheetViewMode.PAGINATED) R.string.view_mode_pageless
                                        else R.string.view_mode_paginated,
                                    ),
                                )
                            }
                            IconButton(
                                onClick = { splitViewModel?.undo() ?: viewModel?.undo() },
                                enabled = editorState.canUndo,
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Undo, stringResource(R.string.study_action_undo))
                            }
                            IconButton(
                                onClick = { splitViewModel?.redo() ?: viewModel?.redo() },
                                enabled = editorState.canRedo,
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Redo, stringResource(R.string.study_action_redo))
                            }
                        }
                        IconButton(
                            onClick = { showSaveDialog = true },
                            enabled = !editorState.isSaving,
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
            floatingActionButton = {
                androidx.compose.material3.FloatingActionButton(
                    onClick = activeBibiController::open,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(
                        painter = painterResource(com.cristiancogollo.biblion.R.drawable.bibi_logo),
                        contentDescription = "Abrir Bibi",
                    )
                }
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                val activeCapabilities = editorState.doc.blocks
                    .firstOrNull { it.id == editorState.activeBlockId }
                    ?.capabilities()
                EditorToolbar(
                    activeFormat = editorState.activeFormat,
                    inlineFormattingEnabled =
                        activeCapabilities?.supportsInlineFormatting != false,
                    fontFamilyEnabled =
                        activeCapabilities?.supportsInlineFormatting != false,
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
                    onClearTextColor = {
                        splitViewModel?.clearActiveTextColor() ?: viewModel?.clearActiveTextColor()
                    },
                    onClearBackgroundColor = {
                        splitViewModel?.clearActiveBackgroundColor() ?: viewModel?.clearActiveBackgroundColor()
                    },
                    onClearFormatting = {
                        splitViewModel?.clearActiveFormatting() ?: viewModel?.clearActiveFormatting()
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
                    viewMode = standaloneViewMode,
                    activeEditorKey = editorState.activeBlockId?.let {
                        PagedEditorUnitKey(it, editorState.activeListItemIndex)
                    },
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
                                activeListItemIndex = editorState.activeListItemIndex,
                                focusRequest = editorState.focusRequest,
                                splitViewModel = splitViewModel,
                                viewModel = viewModel,
                                onVerseClick = onVerseSelected,
                                onVerseComparisonSelected = onVerseComparisonSelected,
                                onVerseDelete = onVerseDelete,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    },
                    contentBlockRenderer = { block, index ->
                        val richState = splitViewModel?.blockRichStates?.get(block.id)
                            ?: viewModel?.blockRichStates?.get(block.id)
                        if (block is com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock.Verse ||
                            block is com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock.BulletList ||
                            block is com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock.OrderedList ||
                            richState != null
                        ) {
                            UnifiedBlockRenderer(
                                block = block,
                                blockIndex = index,
                                richState = richState,
                                isActive = editorState.activeBlockId == block.id,
                                isEditing = true,
                                allBlocks = editorState.doc.blocks,
                                activeListItemIndex = editorState.activeListItemIndex,
                                focusRequest = editorState.focusRequest,
                                splitViewModel = splitViewModel,
                                viewModel = viewModel,
                                onVerseClick = onVerseSelected,
                                onVerseComparisonSelected = onVerseComparisonSelected,
                                onVerseDelete = onVerseDelete,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    },
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

    if (activeBibiController.isOpen) {
        BibiStudyFloatingWindow(
            editorState = editorState,
            currentUserName = currentUserName,
            onClose = activeBibiController::close,
            onOpenPassage = { passage ->
                requestedBibiPassage = passage
                isFullScreen = false
                compactStudyPane = "bible"
                activeBibiController.close()
            },
        )
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

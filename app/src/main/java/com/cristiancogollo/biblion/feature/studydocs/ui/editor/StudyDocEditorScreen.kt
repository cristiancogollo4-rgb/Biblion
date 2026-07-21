package com.cristiancogollo.biblion.feature.studydocs.ui.editor

import android.util.Log
import android.widget.Toast
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.cristiancogollo.biblion.feature.studydocs.model.BlockId
import com.cristiancogollo.biblion.feature.studydocs.model.DocConfig
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
) {
    // Usar splitViewModel si está disponible (modo split), sino usar viewModel (modo standalone)
    val editorState by (splitViewModel?.editorState ?: viewModel?.uiState)?.collectAsState()
        ?: remember { mutableStateOf(com.cristiancogollo.biblion.feature.studydocs.domain.StudyEditorUiState()) }

    val focusRequesters = remember { mutableStateMapOf<BlockId, FocusRequester>() }
    var showSaveDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val standaloneZoomState = rememberDocumentZoomState()
    var isFullScreen by remember { mutableStateOf(false) }
    var isDarkTheme by remember { mutableStateOf(false) }

    LaunchedEffect(remoteId) {
        Log.d("BIBLION_STUDY", "StudyDocEditorScreen LaunchedEffect remoteId=$remoteId viewModel=${viewModel != null} splitViewModel=${splitViewModel != null}")
    }

    LaunchedEffect(editorState.isLoading, editorState.doc.blocks.size) {
        Log.d("BIBLION_STUDY", "StudyDocEditorScreen state isLoading=${editorState.isLoading} blocks=${editorState.doc.blocks.size} activeBlockId=${editorState.activeBlockId} lastError=${editorState.lastError}")
    }

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
        if (editorState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            // Split 50/50 con el editor a la derecha y la app a la izquierda.
            // BiblionTheme envuelve ambos paneles para que el dark mode los afecte.
            com.cristiancogollo.biblion.ui.theme.BiblionTheme(darkTheme = isDarkTheme) {
                Row(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))) {
                    if (!isFullScreen) {
                        BibleReaderPane(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.surface),
                            navController = navController,
                            isDarkTheme = isDarkTheme,
                            onToggleDarkTheme = { isDarkTheme = it },
                        )
                        VerticalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            thickness = 1.dp,
                        )
                    }
                    StudyModeEditorPanel(
                        modifier = if (isFullScreen) Modifier.fillMaxSize() else Modifier.weight(1f).fillMaxHeight(),
                        editorState = editorState,
                        focusRequesters = focusRequesters,
                        splitViewModel = splitViewModel,
                        viewModel = viewModel,
                        onSaveClick = { showSaveDialog = true },
                        onBack = onBack,
                        isFullScreen = isFullScreen,
                        onToggleFullScreen = { isFullScreen = !isFullScreen },
                        isDarkTheme = isDarkTheme,
                        navController = navController,
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
                    title = { },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                        }
                    },
                    actions = {
                        ZoomMenu(zoomState = standaloneZoomState, showStepButtons = true)
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

                PaginatedSheet(
                    blocks = editorState.doc.blocks,
                    isEditing = true,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    zoomState = standaloneZoomState,
                ) { fragment, _ ->
                    val richState = splitViewModel?.blockRichStates?.get(fragment.originBlockId)
                        ?: viewModel?.blockRichStates?.get(fragment.originBlockId)
                    val isOwner = editorState.activeBlockId == fragment.originBlockId
                    Log.d("BIBLION_STUDY", "EditorScreen fragment blockId=${fragment.originBlockId} richState=${richState != null} isOwner=$isOwner activeBlockId=${editorState.activeBlockId}")
                    if (richState != null || fragment is com.cristiancogollo.biblion.feature.studydocs.ui.pagination.PageFragment.VerseSlice) {
                        UnifiedBlockRenderer(
                            fragment = fragment,
                            allBlocks = editorState.doc.blocks,
                            isEditing = true,
                            isOwnerFragment = isOwner,
                            richState = richState,
                            isActive = isOwner,
                            focusRequesters = focusRequesters,
                            splitViewModel = splitViewModel,
                            viewModel = viewModel,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
    }
}


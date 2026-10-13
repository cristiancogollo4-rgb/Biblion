package com.cristiancogollo.biblion.feature.studydocs.ui.read

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cristiancogollo.biblion.feature.studydocs.domain.StudyDocViewModel
import com.cristiancogollo.biblion.feature.studydocs.model.BlockId
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import com.cristiancogollo.biblion.feature.studydocs.model.VerseBusinessRules
import com.cristiancogollo.biblion.feature.studydocs.ui.editor.UnifiedBlockRenderer
import com.cristiancogollo.biblion.feature.studydocs.ui.editor.ZoomMenu
import com.cristiancogollo.biblion.feature.studydocs.ui.editor.loadVerseRangeText
import com.cristiancogollo.biblion.feature.studydocs.ui.editor.rememberDocumentZoomState
import com.cristiancogollo.biblion.feature.studydocs.ui.pagination.PageFragment
import com.cristiancogollo.biblion.feature.studydocs.ui.pagination.PaginatedSheet
import com.cristiancogollo.biblion.feature.studydocs.ui.pagination.SheetViewMode
import com.cristiancogollo.biblion.R
import kotlinx.coroutines.launch

private data class BibleDialogRequest(
    val target: StudyBibleTarget?,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyDocReadScreen(
    viewModel: StudyDocViewModel,
    remoteId: String? = null,
    onBack: () -> Unit,
    onEdit: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val readZoomState = rememberDocumentZoomState()
    val narrowViewport = LocalConfiguration.current.screenWidthDp < 900
    var viewMode by rememberSaveable(narrowViewport) {
        mutableStateOf(if (narrowViewport) SheetViewMode.PAGELESS else SheetViewMode.PAGINATED)
    }
    var compactActionsExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var verseOverrides by remember {
        mutableStateOf<Map<BlockId, StudyBlock.Verse>>(emptyMap())
    }
    var bibleDialogRequest by remember {
        mutableStateOf<BibleDialogRequest?>(null)
    }
    val displayedBlocks = remember(uiState.doc.blocks, verseOverrides) {
        uiState.doc.blocks.map { block -> verseOverrides[block.id] ?: block }
    }
    val onVerseComparisonSelected: (StudyBlock.Verse, String?) -> Unit = { block, version ->
        if (version == null) {
            verseOverrides = verseOverrides + (
                block.id to VerseBusinessRules.withComparison(block, null, null)
            )
        } else {
            scope.launch {
                val text = loadVerseRangeText(context, version, block)
                verseOverrides = verseOverrides + (
                    block.id to VerseBusinessRules.withComparison(block, version, text)
                )
            }
        }
    }

    bibleDialogRequest?.let { request ->
        StudyBibleDialog(
            initialTarget = request.target,
            onDismiss = { bibleDialogRequest = null },
        )
    }

    LaunchedEffect(remoteId) {
        Log.d("BIBLION_STUDY", "StudyDocReadScreen LaunchedEffect remoteId=$remoteId")
        verseOverrides = emptyMap()
        bibleDialogRequest = null
    }

    LaunchedEffect(uiState.isLoading, uiState.doc.blocks.size) {
        Log.d("BIBLION_STUDY", "StudyDocReadScreen state isLoading=${uiState.isLoading} blocks=${uiState.doc.blocks.size} lastError=${uiState.lastError}")
    }

    LaunchedEffect(remoteId) {
        if (remoteId != null) viewModel.loadByRemoteId(remoteId)
    }

    Scaffold(
        modifier = Modifier.blur(if (bibleDialogRequest != null) 6.dp else 0.dp),
        topBar = {
            TopAppBar(
                title = {
                    Text(uiState.doc.title.ifBlank { "Sin titulo" }, fontWeight = FontWeight.SemiBold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (narrowViewport) {
                        Box {
                            IconButton(onClick = { compactActionsExpanded = true }) {
                                Icon(Icons.Default.MoreVert, stringResource(R.string.study_action_options))
                            }
                            DropdownMenu(
                                expanded = compactActionsExpanded,
                                onDismissRequest = { compactActionsExpanded = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.study_action_open_bible)) },
                                    onClick = {
                                        compactActionsExpanded = false
                                        bibleDialogRequest = BibleDialogRequest(target = null)
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(
                                        if (viewMode == SheetViewMode.PAGINATED) R.string.view_mode_pageless
                                        else R.string.view_mode_paginated
                                    )) },
                                    onClick = {
                                        compactActionsExpanded = false
                                        viewMode = if (viewMode == SheetViewMode.PAGINATED) {
                                            SheetViewMode.PAGELESS
                                        } else SheetViewMode.PAGINATED
                                    },
                                )
                            }
                        }
                    } else {
                        IconButton(onClick = { bibleDialogRequest = BibleDialogRequest(target = null) }) {
                            Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = stringResource(R.string.study_action_open_bible))
                        }
                        IconButton(onClick = {
                            viewMode = if (viewMode == SheetViewMode.PAGINATED) SheetViewMode.PAGELESS
                            else SheetViewMode.PAGINATED
                        }) {
                            Icon(
                                if (viewMode == SheetViewMode.PAGINATED) Icons.Default.ViewStream
                                else Icons.Default.Description,
                                contentDescription = stringResource(
                                    if (viewMode == SheetViewMode.PAGINATED) R.string.view_mode_pageless
                                    else R.string.view_mode_paginated,
                                ),
                            )
                        }
                    }
                    ZoomMenu(zoomState = readZoomState, showStepButtons = !narrowViewport)
                    IconButton(onClick = onEdit, enabled = uiState.lastError == null) {
                        Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.study_library_edit))
                    }
                },
            )
        },
    ) { padding ->
        if (
            uiState.isLoading ||
            (remoteId != null && uiState.doc.remoteId != remoteId && uiState.lastError == null)
        ) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.lastError != null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.study_document_unavailable),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (remoteId != null) {
                    TextButton(onClick = { viewModel.loadByRemoteId(remoteId) }) {
                        Text(stringResource(R.string.action_retry))
                    }
                }
                TextButton(onClick = onBack) { Text(stringResource(R.string.cd_back)) }
            }
        } else if (displayedBlocks.all { it.plainText().isBlank() }) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    stringResource(R.string.study_document_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            PaginatedSheet(
                blocks = displayedBlocks,
                isEditing = false,
                modifier = Modifier.fillMaxSize().padding(padding),
                zoomState = readZoomState,
                viewMode = viewMode,
                contentFragmentRenderer = { fragment, _ ->
                    UnifiedBlockRenderer(
                        fragment = fragment,
                        allBlocks = displayedBlocks,
                        isEditing = false,
                        isOwnerFragment = false,
                        richState = null,
                        isActive = false,
                        splitViewModel = null,
                        viewModel = null,
                        onVerseClick = { verse ->
                            bibleDialogRequest = BibleDialogRequest(
                                target = verse.toStudyBibleTarget(),
                            )
                        },
                        onVerseComparisonSelected = onVerseComparisonSelected,
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                contentBlockRenderer = { block, index ->
                    UnifiedBlockRenderer(
                        block = block,
                        blockIndex = index,
                        richState = null,
                        isActive = false,
                        isEditing = false,
                        allBlocks = displayedBlocks,
                        onVerseClick = { verse ->
                            bibleDialogRequest = BibleDialogRequest(
                                target = verse.toStudyBibleTarget(),
                            )
                        },
                        onVerseComparisonSelected = onVerseComparisonSelected,
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
            )
        }
    }
}

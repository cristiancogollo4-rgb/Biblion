package com.cristiancogollo.biblion.feature.studydocs.ui.read

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.VerticalDivider
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
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.navigation.compose.rememberNavController
import com.cristiancogollo.biblion.Screen
import com.cristiancogollo.biblion.Testament
import com.cristiancogollo.biblion.navigateTopLevel
import com.cristiancogollo.biblion.feature.bibi.model.BibiPassage
import com.cristiancogollo.biblion.feature.studydocs.ui.editor.BibleReaderPane
import com.cristiancogollo.biblion.ui.theme.BiblionThemeMode
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/** Pedido de abrir la Biblia de la app; [passage] lleva al versiculo citado, null abre los libros. */
private data class BibleOpenRequest(
    val passage: BibiPassage?,
)

private fun StudyBlock.Verse.toBiblePassage(): BibiPassage? =
    bookId.takeIf { it.isNotBlank() }?.let { book ->
        BibiPassage(
            book = book,
            chapter = chapter.coerceAtLeast(1),
            verse = selectedVerseNumbers().minOrNull() ?: 1,
            text = "",
        )
    }

/** Como se presenta la Biblia junto a la ensenanza segun el espacio disponible. */
internal enum class StudyReadBibleLayout { SIDE_BY_SIDE, STACKED, BOTTOM_SHEET }

internal fun studyReadBibleLayout(
    widthDp: Int,
    heightDp: Int,
    smallestWidthDp: Int,
): StudyReadBibleLayout = when {
    widthDp > heightDp && widthDp >= 640 -> StudyReadBibleLayout.SIDE_BY_SIDE
    smallestWidthDp >= 600 -> StudyReadBibleLayout.STACKED
    else -> StudyReadBibleLayout.BOTTOM_SHEET
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyDocReadScreen(
    viewModel: StudyDocViewModel,
    remoteId: String? = null,
    readOnly: Boolean = false,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    canOpenBible: Boolean = true,
    isDarkTheme: Boolean = false,
    onToggleDarkTheme: (Boolean) -> Unit = {},
    themeMode: BiblionThemeMode = if (isDarkTheme) BiblionThemeMode.DARK else BiblionThemeMode.LIGHT,
    onThemeModeChange: (BiblionThemeMode) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val readZoomState = rememberDocumentZoomState()
    val configuration = LocalConfiguration.current
    val narrowViewport = configuration.screenWidthDp < 900
    val bibleLayout = studyReadBibleLayout(
        widthDp = configuration.screenWidthDp,
        heightDp = configuration.screenHeightDp,
        smallestWidthDp = configuration.smallestScreenWidthDp,
    )
    val usesBottomSheet = bibleLayout == StudyReadBibleLayout.BOTTOM_SHEET
    val bibleSheetPeekHeight = (configuration.screenHeightDp * 0.5f).dp
    var viewMode by rememberSaveable(narrowViewport) {
        mutableStateOf(if (narrowViewport) SheetViewMode.PAGELESS else SheetViewMode.PAGINATED)
    }
    var compactActionsExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var verseOverrides by remember {
        mutableStateOf<Map<BlockId, StudyBlock.Verse>>(emptyMap())
    }
    var bibleRequest by remember {
        mutableStateOf<BibleOpenRequest?>(null)
    }
    // Lector real de la app (el mismo del icono Biblia de la barra) embebido junto a la ensenanza.
    val biblePaneNavController = rememberNavController()
    var pendingPassage by remember { mutableStateOf<BibiPassage?>(null) }
    LaunchedEffect(bibleRequest) {
        bibleRequest?.passage?.let { pendingPassage = it }
    }
    val openBibleAt: (StudyBlock.Verse) -> Unit = { verse ->
        if (canOpenBible) bibleRequest = BibleOpenRequest(passage = verse.toBiblePassage())
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

    val isBibleOpen = bibleRequest != null
    val toggleBible: () -> Unit = {
        bibleRequest = if (isBibleOpen) null else BibleOpenRequest(passage = null)
    }

    // En telefono vertical la Biblia vive en una hoja inferior; bibleRequest es la fuente
    // de verdad y la hoja solo la refleja.
    val bibleSheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.Hidden,
        skipHiddenState = false,
    )
    LaunchedEffect(isBibleOpen, usesBottomSheet) {
        if (!usesBottomSheet) return@LaunchedEffect
        if (isBibleOpen && bibleSheetState.currentValue == SheetValue.Hidden) {
            bibleSheetState.partialExpand()
        } else if (!isBibleOpen && bibleSheetState.currentValue != SheetValue.Hidden) {
            bibleSheetState.hide()
        }
    }
    LaunchedEffect(bibleSheetState) {
        snapshotFlow { bibleSheetState.currentValue }
            .drop(1)
            .collect { value ->
                if (value == SheetValue.Hidden) bibleRequest = null
            }
    }

    // Con la Biblia al lado la hoja paginada no cabe en media pantalla: se pasa a vista continua
    // mientras el panel esta abierto y se restaura el modo anterior al cerrarlo.
    var viewModeBeforeBible by remember { mutableStateOf<SheetViewMode?>(null) }
    LaunchedEffect(isBibleOpen, usesBottomSheet) {
        if (isBibleOpen && !usesBottomSheet) {
            if (viewModeBeforeBible == null) {
                viewModeBeforeBible = viewMode
                viewMode = SheetViewMode.PAGELESS
            }
        } else if (!isBibleOpen) {
            viewModeBeforeBible?.let { viewMode = it }
            viewModeBeforeBible = null
        }
    }

    BackHandler(enabled = isBibleOpen) {
        bibleRequest = null
    }

    LaunchedEffect(remoteId) {
        Log.d("BIBLION_STUDY", "StudyDocReadScreen LaunchedEffect remoteId=$remoteId")
        verseOverrides = emptyMap()
        bibleRequest = null
    }

    LaunchedEffect(uiState.isLoading, uiState.doc.blocks.size) {
        Log.d("BIBLION_STUDY", "StudyDocReadScreen state isLoading=${uiState.isLoading} blocks=${uiState.doc.blocks.size} lastError=${uiState.lastError}")
    }

    LaunchedEffect(remoteId) {
        if (remoteId != null) viewModel.loadByRemoteId(remoteId)
    }

    val documentContent: @Composable (Modifier) -> Unit = { contentModifier ->
        if (
            uiState.isLoading ||
            (remoteId != null && uiState.doc.remoteId != remoteId && uiState.lastError == null)
        ) {
            Box(
                modifier = contentModifier,
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.lastError != null) {
            Column(
                modifier = contentModifier.padding(24.dp),
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
                modifier = contentModifier.padding(24.dp),
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
                modifier = contentModifier,
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
                        onVerseClick = openBibleAt,
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
                        onVerseClick = openBibleAt,
                        onVerseComparisonSelected = onVerseComparisonSelected,
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
            )
        }
    }

    val biblePane: @Composable (Modifier) -> Unit = { paneModifier ->
        BibleReaderPane(
            localNavController = biblePaneNavController,
            modifier = paneModifier.background(MaterialTheme.colorScheme.surface),
            isDarkTheme = isDarkTheme,
            onToggleDarkTheme = onToggleDarkTheme,
            themeMode = themeMode,
            onThemeModeChange = onThemeModeChange,
            requestedPassage = pendingPassage,
            onRequestedPassageConsumed = { pendingPassage = null },
        )
        LaunchedEffect(Unit) {
            // Sin versiculo citado se abre en los libros, como el icono Biblia de la barra.
            if (pendingPassage == null &&
                biblePaneNavController.currentDestination?.route == Screen.Home.route
            ) {
                biblePaneNavController.navigateTopLevel(Screen.Books.createRoute(Testament.OLD))
            }
        }
    }

    val readTopBar: @Composable () -> Unit = {
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
                            if (canOpenBible) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(bibleActionLabel(isBibleOpen))) },
                                    onClick = {
                                        compactActionsExpanded = false
                                        toggleBible()
                                    },
                                )
                            }
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
                    if (canOpenBible) {
                        IconButton(onClick = toggleBible) {
                            Icon(
                                Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = stringResource(bibleActionLabel(isBibleOpen)),
                                tint = if (isBibleOpen) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    LocalContentColor.current
                                },
                            )
                        }
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
                if (!readOnly) {
                    IconButton(onClick = onEdit, enabled = uiState.lastError == null) {
                        Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.study_library_edit))
                    }
                }
            },
        )
    }

    if (usesBottomSheet) {
        BottomSheetScaffold(
            scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = bibleSheetState),
            topBar = readTopBar,
            sheetPeekHeight = bibleSheetPeekHeight,
            sheetContent = {
                // Altura fija para que los anclajes de la hoja no cambien al cargar el capitulo.
                Box(Modifier.fillMaxWidth().fillMaxHeight(0.92f)) {
                    if (isBibleOpen) biblePane(Modifier.fillMaxSize())
                }
            },
        ) {
            // El padding del scaffold siempre reserva la altura de la hoja, aun oculta; el cuerpo ya
            // queda bajo la barra superior, asi que solo se reserva espacio con la Biblia abierta.
            documentContent(
                Modifier
                    .fillMaxSize()
                    .padding(bottom = if (isBibleOpen) bibleSheetPeekHeight else 0.dp),
            )
        }
        return
    }

    Scaffold(topBar = readTopBar) { padding ->
        // El documento conserva su posicion en el arbol para no perder el scroll al abrir la Biblia.
        when (bibleLayout) {
            StudyReadBibleLayout.SIDE_BY_SIDE -> Row(Modifier.fillMaxSize().padding(padding)) {
                if (isBibleOpen) {
                    biblePane(Modifier.weight(1f).fillMaxHeight())
                    VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                documentContent(Modifier.weight(1f).fillMaxHeight())
            }
            StudyReadBibleLayout.STACKED -> Column(Modifier.fillMaxSize().padding(padding)) {
                if (isBibleOpen) {
                    biblePane(Modifier.weight(1f).fillMaxWidth())
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                documentContent(Modifier.weight(1f).fillMaxWidth())
            }
            StudyReadBibleLayout.BOTTOM_SHEET -> Unit
        }
    }
}

private fun bibleActionLabel(isOpen: Boolean): Int =
    if (isOpen) R.string.study_action_close_bible else R.string.study_action_open_bible

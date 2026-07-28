package com.cristiancogollo.biblion.feature.studydocs.ui.editor

import android.content.res.Configuration
import android.widget.Toast
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cristiancogollo.biblion.addSharedPrimaryDestinations
import com.cristiancogollo.biblion.AuthDialog
import com.cristiancogollo.biblion.AuthDialogMode
import com.cristiancogollo.biblion.AuthViewModel
import com.cristiancogollo.biblion.AuthIntent
import com.cristiancogollo.biblion.feature.auth.data.GoogleCredentialsAuth
import com.cristiancogollo.biblion.ProfileScreen
import com.cristiancogollo.biblion.ProfileViewModel
import com.cristiancogollo.biblion.Screen
import com.cristiancogollo.biblion.feature.studydocs.domain.TextStyleKind
import com.cristiancogollo.biblion.feature.studydocs.debug.StudyEditorDebugLog
import com.cristiancogollo.biblion.feature.studydocs.model.BlockId
import com.cristiancogollo.biblion.feature.studydocs.model.DocConfig
import com.cristiancogollo.biblion.feature.studydocs.model.DocTagGroups
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import com.cristiancogollo.biblion.feature.studydocs.model.capabilities
import com.cristiancogollo.biblion.feature.studydocs.ui.pagination.PageFragment
import com.cristiancogollo.biblion.feature.studydocs.ui.pagination.PaginatedSheet
import com.cristiancogollo.biblion.feature.studydocs.ui.pagination.SheetViewMode
import com.cristiancogollo.biblion.ui.theme.BiblionThemeMode
import kotlinx.coroutines.launch

private val textColorPalette = listOf(
    Color(0xFF0F172A), Color(0xFF9E9E9E), Color(0xFFE53935), Color(0xFFFB8C00),
    Color(0xFFFDD835), Color(0xFF43A047), Color(0xFF1E88E5), Color(0xFF8E24AA),
    Color(0xFFEF9A9A), Color(0xFF80CBC4),
)

private val highlightPaletteLight = listOf(
    Color(0xFFFEF3C7), Color(0xFFD1FAE5), Color(0xFFFFD0D0),
    Color(0xFFD8E8FF), Color(0xFFF3E8FF), Color(0xFFFFF2CC),
)

private val highlightPaletteDark = listOf(
    Color(0xFF5D4037), Color(0xFF2E7D32), Color(0xFFC62828),
    Color(0xFF1565C0), Color(0xFF6A1B9A), Color(0xFFE65100),
)

private fun currentHighlightPalette(isDarkTheme: Boolean) =
    if (isDarkTheme) highlightPaletteDark else highlightPaletteLight

@Composable
fun StudyModeEditorPanel(
    modifier: Modifier = Modifier,
    editorState: com.cristiancogollo.biblion.feature.studydocs.domain.StudyEditorUiState,
    splitViewModel: com.cristiancogollo.biblion.feature.studydocs.domain.StudyDocSplitViewModel?,
    viewModel: com.cristiancogollo.biblion.feature.studydocs.domain.StudyDocViewModel?,
    onSaveClick: () -> Unit,
    onBack: () -> Unit,
    isFullScreen: Boolean = false,
    onToggleFullScreen: () -> Unit = {},
    isDarkTheme: Boolean = false,
    onToggleDarkTheme: (Boolean) -> Unit = {},
    navController: androidx.navigation.NavController? = null,
    showFullScreenToggle: Boolean = true,
    isCompactLayout: Boolean = false,
    onBibiClick: () -> Unit = {},
) {
    var documentTitle by remember { mutableStateOf(editorState.doc.title) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val editorZoomState = rememberDocumentZoomState()
    var viewMode by remember { mutableStateOf(SheetViewMode.PAGINATED) }
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val hardwareKeyboardPresent =
        LocalConfiguration.current.keyboard != Configuration.KEYBOARD_NOKEYS
    val allowProgrammaticFocus =
        !isCompactLayout || imeVisible || hardwareKeyboardPresent

    val onVerseComparisonSelected: (StudyBlock.Verse, String?) -> Unit = { block, version ->
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
    val onVerseSelected: (StudyBlock.Verse) -> Unit = { block ->
        splitViewModel?.setActiveBlock(block.id) ?: viewModel?.setActiveBlock(block.id)
    }
    val onVerseDelete: (StudyBlock.Verse) -> Unit = { block ->
        splitViewModel?.deleteBlock(block.id) ?: viewModel?.deleteBlock(block.id)
    }

    LaunchedEffect(isFullScreen) {
        editorZoomState.set(if (isFullScreen) 1f else DocumentZoomState.MIN_ZOOM)
    }

    LaunchedEffect(editorState.doc.title) {
        if (editorState.doc.title != documentTitle) {
            documentTitle = editorState.doc.title
        }
    }

    // Solo el editor (como en v3). El split 50/50 y el fullscreen se manejan
    // en el padre (StudyDocEditorScreen) que envuelve este panel en un Row.
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
        if (!isCompactLayout || !imeVisible) {
            StudyModeHeader(
                documentTitle = documentTitle,
                isFullScreen = isFullScreen,
                onToggleFullScreen = onToggleFullScreen,
                viewMode = viewMode,
                onToggleViewMode = { viewMode = if (viewMode == SheetViewMode.PAGINATED) SheetViewMode.PAGELESS else SheetViewMode.PAGINATED },
                onTitleChange = { newTitle ->
                    documentTitle = newTitle
                    splitViewModel?.updateTitle(newTitle) ?: viewModel?.updateTitle(newTitle)
                },
                canUndo = editorState.canUndo,
                canRedo = editorState.canRedo,
                onUndo = { splitViewModel?.undo() ?: viewModel?.undo() },
                onRedo = { splitViewModel?.redo() ?: viewModel?.redo() },
                isSaving = editorState.isSaving,
                hasUnsavedChanges = editorState.hasUnsavedChanges,
                lastError = editorState.lastError,
                onBackClick = onBack,
                onSaveClick = onSaveClick,
                showFullScreenToggle = showFullScreenToggle,
            )
        }

        val activeCapabilities = editorState.doc.blocks
            .firstOrNull { it.id == editorState.activeBlockId }
            ?.capabilities()
        StudyModeToolbar(
            activeFormat = editorState.activeFormat,
            inlineFormattingEnabled =
                activeCapabilities?.supportsInlineFormatting != false,
            blockTypeChangeEnabled =
                activeCapabilities?.supportsBlockTypeChange != false,
            currentFontSize = run {
                val af = editorState.activeFormat.fontSize
                val bf = editorState.doc.blocks.firstOrNull { it.id == editorState.activeBlockId }?.fontSize
                val result = when {
                    af != null && af >= 0 -> af
                    af == -1 -> -1
                    else -> bf ?: DocConfig.DEFAULT_FONT_SIZE
                }
                result
            },
            currentAlignment = run {
                val activeId = editorState.activeBlockId ?: return@run com.cristiancogollo.biblion.feature.studydocs.model.BlockAlignment.Start
                editorState.doc.blocks.firstOrNull { it.id == activeId }?.alignment
                    ?: com.cristiancogollo.biblion.feature.studydocs.model.BlockAlignment.Start
            },
            onToggleStyle = { kind ->
                splitViewModel?.applyStyleToActive(kind) ?: viewModel?.applyStyleToActive(kind)
            },
            onTextColor = { color ->
                splitViewModel?.setActiveTextColor(color) ?: viewModel?.setActiveTextColor(color)
            },
            onBackgroundColor = { color ->
                splitViewModel?.setActiveBackgroundColor(color) ?: viewModel?.setActiveBackgroundColor(color)
            },
            onStepFontSize = { delta ->
                splitViewModel?.stepFontSizeActive(delta) ?: viewModel?.stepFontSizeActive(delta)
            },
            onSetFontSize = { size ->
                splitViewModel?.setFontSizeActive(size) ?: viewModel?.setFontSizeActive(size)
            },
            onSetAlignment = { alignment ->
                editorState.activeBlockId?.let {
                    splitViewModel?.setBlockAlignment(it, alignment) ?: viewModel?.setBlockAlignment(it, alignment)
                }
            },
            onInsertBlock = { type ->
                StudyEditorDebugLog.log(
                    "BLOCK_INSERT_REQUEST",
                    "type=$type active=${editorState.activeBlockId?.value} " +
                        "activeItem=${editorState.activeListItemIndex}",
                )
                splitViewModel?.insertBlock(editorState.activeBlockId, type)
                    ?: viewModel?.insertBlock(editorState.activeBlockId, type)
            },
            onChangeBlockType = { type ->
                StudyEditorDebugLog.log(
                    "BLOCK_TYPE_REQUEST",
                    "type=$type active=${editorState.activeBlockId?.value} " +
                        "activeItem=${editorState.activeListItemIndex}",
                )
                editorState.activeBlockId?.let { blockId ->
                    splitViewModel?.changeBlockType(blockId, type)
                        ?: viewModel?.changeBlockType(blockId, type)
                }
            },
            onClearColor = {
                splitViewModel?.clearActiveColor() ?: viewModel?.clearActiveColor()
            },
            isDarkTheme = isDarkTheme,
            onToggleDarkTheme = onToggleDarkTheme,
            zoomState = editorZoomState,
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), thickness = 1.dp)

        PaginatedSheet(
            blocks = editorState.doc.blocks,
            isEditing = true,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            zoomState = editorZoomState,
            viewMode = viewMode,
            contentFragmentRenderer = { fragment, _ ->
                EditorSheetFragment(
                    fragment = fragment,
                    allBlocks = editorState.doc.blocks,
                    activeIdProvider = { splitViewModel?.editorState?.value?.activeBlockId ?: viewModel?.uiState?.value?.activeBlockId },
                    activeListItemIndexProvider = { splitViewModel?.editorState?.value?.activeListItemIndex ?: viewModel?.uiState?.value?.activeListItemIndex },
                    richStateProvider = { id, itemIndex ->
                        splitViewModel?.richStateFor(id, itemIndex)
                            ?: viewModel?.richStateFor(id, itemIndex)
                    },
                    focusRequest = editorState.focusRequest,
                    splitViewModel = splitViewModel,
                    viewModel = viewModel,
                    allowProgrammaticFocus = allowProgrammaticFocus,
                    onVerseSelected = onVerseSelected,
                    onVerseComparisonSelected = onVerseComparisonSelected,
                    onVerseDelete = onVerseDelete,
                )
            },
            contentBlockRenderer = { block, idx ->
                val richState = splitViewModel?.blockRichStates?.get(block.id)
                    ?: viewModel?.blockRichStates?.get(block.id)
                val isActive = editorState.activeBlockId == block.id &&
                    allowProgrammaticFocus
                if (block is StudyBlock.Verse ||
                    block is StudyBlock.BulletList ||
                    block is StudyBlock.OrderedList ||
                    richState != null
                ) {
                    UnifiedBlockRenderer(
                        block = block,
                        blockIndex = idx,
                        richState = richState,
                        isActive = isActive,
                        isEditing = true,
                        allBlocks = editorState.doc.blocks,
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
                        isActive = allowProgrammaticFocus &&
                            editorState.activeBlockId == unit.key.blockId &&
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

        if (!imeVisible) {
            FloatingActionButton(
                onClick = onBibiClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 24.dp, bottom = 96.dp)
                    .size(56.dp),
            ) {
                Icon(
                    painter = androidx.compose.ui.res.painterResource(
                        com.cristiancogollo.biblion.R.drawable.bibi_logo
                    ),
                    contentDescription = "Abrir Bibi",
                )
            }
        }
    }
}

@Composable
private fun StudyModeHeader(
    documentTitle: String,
    isFullScreen: Boolean,
    onToggleFullScreen: () -> Unit,
    viewMode: SheetViewMode,
    onToggleViewMode: () -> Unit,
    onTitleChange: (String) -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    isSaving: Boolean,
    hasUnsavedChanges: Boolean,
    lastError: String?,
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit,
    showFullScreenToggle: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Botón X para salir
        IconButton(onClick = onBackClick) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Salir del Modo Estudio",
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }

        // Título editable
        BasicTextField(
            value = documentTitle,
            onValueChange = onTitleChange,
            textStyle = TextStyle(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            ),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            singleLine = true,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.Center) {
                    if (documentTitle.isEmpty()) {
                        Text(
                            text = "Documento sin titulo",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    innerTextField()
                }
            },
        )

        // Botón alternar pantalla completa / dividida
        SaveStatus(
            isSaving = isSaving,
            hasUnsavedChanges = hasUnsavedChanges,
            lastError = lastError,
        )
        IconButton(onClick = onUndo, enabled = canUndo) {
            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Deshacer")
        }
        IconButton(onClick = onRedo, enabled = canRedo) {
            Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Rehacer")
        }
        if (showFullScreenToggle) {
            IconButton(onClick = onToggleFullScreen) {
                Icon(
                    imageVector = if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                    contentDescription = if (isFullScreen) "Pantalla dividida" else "Pantalla completa",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        // Botón alternar modo de vista (PAGINATED <-> PAGELESS)
        IconButton(onClick = onToggleViewMode) {
            Icon(
                imageVector = if (viewMode == SheetViewMode.PAGINATED) {
                    Icons.Default.ViewStream
                } else {
                    Icons.Default.Description
                },
                contentDescription = if (viewMode == SheetViewMode.PAGINATED) {
                    androidx.compose.ui.res.stringResource(
                        com.cristiancogollo.biblion.R.string.view_mode_pageless
                    )
                } else {
                    androidx.compose.ui.res.stringResource(
                        com.cristiancogollo.biblion.R.string.view_mode_paginated
                    )
                },
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp),
            )
        }

        // Botón Guardar
        IconButton(onClick = onSaveClick, enabled = !isSaving) {
            Icon(
                Icons.Default.Save,
                contentDescription = "Guardar",
                tint = if (!isSaving) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                },
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun SaveStatus(
    isSaving: Boolean,
    hasUnsavedChanges: Boolean,
    lastError: String?,
) {
    val label = when {
        lastError != null -> "Error al guardar"
        isSaving -> "Guardando..."
        hasUnsavedChanges -> "Cambios sin guardar"
        else -> "Guardado"
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = if (lastError != null) {
            MaterialTheme.colorScheme.error
        } else if (isSaving || hasUnsavedChanges) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
    )
}

/**
 * Panel izquierdo del split: una instancia de la app con su propio NavController
 * local. Usa [addSharedPrimaryDestinations] para registrar las rutas principales
 * (Home, Books, Search, etc.) en el NavGraphBuilder local, de modo que la
 * navegacion del usuario (ir a libros, abrir un libro, buscar) ocurre dentro
 * del panel izquierdo sin cerrar el editor. Inicia en [Screen.Home].
 *
 * Las rutas del lector y Perfil se registran aqui porque addSharedPrimaryDestinations
 * no las incluye. Todos estos destinos permanecen dentro del panel izquierdo.
 */
@Composable
internal fun BibleReaderPane(
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = false,
    onToggleDarkTheme: (Boolean) -> Unit = {},
    themeMode: BiblionThemeMode = if (isDarkTheme) BiblionThemeMode.DARK else BiblionThemeMode.LIGHT,
    onThemeModeChange: (BiblionThemeMode) -> Unit = {},
    onInsertVerseCitation: ((com.cristiancogollo.biblion.CitationVerseGroup, String) -> Unit)? = null,
) {
    val localNavController = rememberNavController()
    val context = LocalContext.current
    val appContext = context.applicationContext
    val scope = rememberCoroutineScope()
    val googleCredentialsAuth = remember(context) { GoogleCredentialsAuth(context) }
    val authViewModel: AuthViewModel = viewModel()
    val profileViewModel: ProfileViewModel = viewModel()
    val authState by authViewModel.state.collectAsState()
    val profileState by profileViewModel.state.collectAsState()

    LaunchedEffect(authState.currentUser?.uid) {
        profileViewModel.setCurrentUser(authState.currentUser)
    }

    val onNavigateToProfile = {
        Log.d("BIBLION_STUDY", "BibleReaderPane navigating to embedded profile")
        if (authState.isAuthenticated) {
            localNavController.navigate(Screen.Profile.route) {
                launchSingleTop = true
            }
        } else {
            localNavController.navigate("auth") {
                launchSingleTop = true
            }
        }
    }
    val onAuthActionClick = {
        Log.d("BIBLION_STUDY", "BibleReaderPane onAuthActionClick (no auth) -> navigate to auth")
        localNavController.navigate("auth")
    }

    NavHost(
        navController = localNavController,
        startDestination = "home",
        modifier = modifier,
    ) {
        addSharedPrimaryDestinations(
            navController = localNavController,
            isDarkTheme = isDarkTheme,
            onToggleDarkTheme = onToggleDarkTheme,
            themeMode = themeMode,
            onThemeModeChange = onThemeModeChange,
            onNavigateToProfile = onNavigateToProfile,
            isAuthenticated = authState.isAuthenticated,
            onAuthActionClick = onAuthActionClick,
        )

        // Ruta auth dentro del NavHost: muestra el AuthDialog real reutilizado de
        // la app principal. Al autenticarse, isAuthenticated = true y navega a
        // "profile". Si el AuthViewModel falla al inicializarse, el dialog puede
        // no mostrarse; en ese caso se puede simplificar a un dialog con inputs.
        composable("auth") {
            Log.d("BIBLION_STUDY", "BibleReaderPane composable(auth) entered")

            androidx.compose.runtime.LaunchedEffect(authState.isAuthenticated) {
                if (authState.isAuthenticated) {
                    Log.d("BIBLION_STUDY", "Auth success -> navigate embedded profile")
                    localNavController.navigate(Screen.Profile.route) {
                        popUpTo("home")
                        launchSingleTop = true
                    }
                }
            }

            AuthDialog(
                mode = AuthDialogMode.LOGIN,
                uiState = authState,
                onIntent = { authViewModel.process(it) },
                onGoogleSignIn = { /* TODO: disparar intent de Google en este scope */ },
                onModeChange = { /* mismo dialog */ },
                onDismiss = { localNavController.popBackStack() },
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                navController = localNavController,
                uiState = profileState,
                onNombresChange = profileViewModel::updateNombres,
                onApellidosChange = profileViewModel::updateApellidos,
                onAliasChange = profileViewModel::updateAlias,
                onBiografiaChange = profileViewModel::updateBiografia,
                onAvatarColorChange = profileViewModel::updateAvatarColor,
                onProfilePhotoSelected = { uri -> profileViewModel.uploadProfilePhoto(appContext, uri) },
                onClearProfilePhoto = profileViewModel::clearProfilePhoto,
                onSave = { profileViewModel.saveProfile() },
                onClearSaveSuccess = profileViewModel::clearSaveSuccess,
                onSignOut = {
                    authViewModel.process(AuthIntent.SignOut)
                    scope.launch {
                        googleCredentialsAuth.clearCredentialState()
                    }
                    localNavController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) {
                            inclusive = false
                        }
                        launchSingleTop = true
                    }
                },
                themeMode = themeMode,
                onThemeModeChange = onThemeModeChange,
                forceCompactLayout = true,
            )
        }

        // Rutas del lector (Reader) que addSharedPrimaryDestinations no registra
        composable(
            route = "reader/{bookName}?studyMode={studyMode}&chapter={chapter}&verse={verse}&studyId={studyId}",
            arguments = listOf(
                androidx.navigation.navArgument("bookName") {
                    type = androidx.navigation.NavType.StringType
                },
            ),
        ) { entry ->
            val bookName = entry.arguments?.getString("bookName")
            com.cristiancogollo.biblion.ReaderScreen(
                navController = localNavController,
                bookName = bookName,
                showBibi = false,
                onInsertVerseCitation = onInsertVerseCitation,
            )
        }
        composable(
            route = "reader?studyMode={studyMode}&chapter={chapter}&verse={verse}&studyId={studyId}",
        ) { entry ->
            com.cristiancogollo.biblion.ReaderScreen(
                navController = localNavController,
                bookName = null,
                showBibi = false,
                onInsertVerseCitation = onInsertVerseCitation,
            )
        }

    }

    // DEBUG eliminado: el if (showAuthDialog) estaba fuera del NavHost y no se
    // recompone. Ahora la navegacion a "auth" ocurre dentro del NavHost.
}

@Composable
private fun StudyModeToolbar(
    activeFormat: com.cristiancogollo.biblion.feature.studydocs.domain.ActiveFormatSnapshot,
    inlineFormattingEnabled: Boolean,
    blockTypeChangeEnabled: Boolean,
    currentFontSize: Int,
    currentAlignment: com.cristiancogollo.biblion.feature.studydocs.model.BlockAlignment,
    onToggleStyle: (TextStyleKind) -> Unit,
    onTextColor: (Int) -> Unit,
    onBackgroundColor: (Int) -> Unit,
    onStepFontSize: (Int) -> Unit,
    onSetFontSize: (Int) -> Unit,
    onSetAlignment: (com.cristiancogollo.biblion.feature.studydocs.model.BlockAlignment) -> Unit,
    onInsertBlock: (String) -> Unit,
    onChangeBlockType: (String) -> Unit,
    onClearColor: () -> Unit,
    isDarkTheme: Boolean,
    onToggleDarkTheme: (Boolean) -> Unit,
    zoomState: DocumentZoomState,
) {
    val scrollState = rememberScrollState()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .horizontalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Negrita (B)
        ToolbarIcon(
            icon = Icons.Default.FormatBold,
            contentDescription = "Negrita",
            isActive = activeFormat.bold,
            enabled = inlineFormattingEnabled,
            onClick = { onToggleStyle(TextStyleKind.Bold) },
        )

        // Cursiva (I)
        ToolbarIcon(
            icon = Icons.Default.FormatItalic,
            contentDescription = "Cursiva",
            isActive = activeFormat.italic,
            enabled = inlineFormattingEnabled,
            onClick = { onToggleStyle(TextStyleKind.Italic) },
        )

        // Subrayado (U)
        ToolbarIcon(
            icon = Icons.Default.FormatUnderlined,
            contentDescription = "Subrayado",
            isActive = activeFormat.underline,
            enabled = inlineFormattingEnabled,
            onClick = { onToggleStyle(TextStyleKind.Underline) },
        )

        // Color de texto (A) — menú flotante
        var showTextColorMenu by remember { mutableStateOf(false) }
        Box {
            ToolbarIcon(
                icon = Icons.Default.FormatColorText,
                contentDescription = "Color de Texto",
                isActive = activeFormat.color != null,
                enabled = inlineFormattingEnabled,
                onClick = { showTextColorMenu = true },
            )
            DropdownMenu(
                expanded = showTextColorMenu && inlineFormattingEnabled,
                onDismissRequest = { showTextColorMenu = false },
            ) {
                Text(
                    "Color de texto",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    textColorPalette.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(color, CircleShape)
                                .clickable {
                                    onTextColor(color.toArgb())
                                    showTextColorMenu = false
                                }
                        )
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                DropdownMenuItem(
                    text = { Text("Quitar color") },
                    leadingIcon = { Icon(Icons.Default.FormatColorReset, null, modifier = Modifier.size(18.dp)) },
                    onClick = {
                        onClearColor()
                        showTextColorMenu = false
                    },
                )
            }
        }

        // Resaltador — menú flotante
        var showHighlightMenu by remember { mutableStateOf(false) }
        Box {
            ToolbarIcon(
                icon = Icons.Default.FormatColorFill,
                contentDescription = "Color de Resaltado",
                isActive = activeFormat.background != null,
                enabled = inlineFormattingEnabled,
                onClick = { showHighlightMenu = true },
            )
            DropdownMenu(
                expanded = showHighlightMenu && inlineFormattingEnabled,
                onDismissRequest = { showHighlightMenu = false },
            ) {
                Text(
                    "Color de resaltado",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    currentHighlightPalette(isDarkTheme).forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(color, CircleShape)
                                .clickable {
                                    onBackgroundColor(color.toArgb())
                                    showHighlightMenu = false
                                }
                        )
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                DropdownMenuItem(
                    text = { Text("Quitar resaltado") },
                    leadingIcon = { Icon(Icons.Default.FormatColorReset, null, modifier = Modifier.size(18.dp)) },
                    onClick = {
                        onClearColor()
                        showHighlightMenu = false
                    },
                )
            }
        }

        ToolbarDivider()

        // Viñetas
        ToolbarIcon(
            icon = Icons.Default.FormatListBulleted,
            contentDescription = "Lista vinetas",
            isActive = false,
            enabled = blockTypeChangeEnabled,
            onClick = { onChangeBlockType("bullet") },
        )

        // Numerada
        ToolbarIcon(
            icon = Icons.Default.FormatListNumbered,
            contentDescription = "Lista numerada",
            isActive = false,
            enabled = blockTypeChangeEnabled,
            onClick = { onChangeBlockType("numbered") },
        )

        ToolbarDivider()

        // Alineación izquierda
        ToolbarIcon(
            icon = Icons.Default.FormatAlignLeft,
            contentDescription = "Alinear izquierda",
            isActive = currentAlignment == com.cristiancogollo.biblion.feature.studydocs.model.BlockAlignment.Start,
            onClick = { onSetAlignment(com.cristiancogollo.biblion.feature.studydocs.model.BlockAlignment.Start) },
        )

        // Alineación centro
        ToolbarIcon(
            icon = Icons.Default.FormatAlignCenter,
            contentDescription = "Alinear centro",
            isActive = currentAlignment == com.cristiancogollo.biblion.feature.studydocs.model.BlockAlignment.Center,
            onClick = { onSetAlignment(com.cristiancogollo.biblion.feature.studydocs.model.BlockAlignment.Center) },
        )

        // Justificado
        ToolbarIcon(
            icon = Icons.Default.FormatAlignJustify,
            contentDescription = "Alinear justificado",
            isActive = currentAlignment == com.cristiancogollo.biblion.feature.studydocs.model.BlockAlignment.Justify,
            onClick = { onSetAlignment(com.cristiancogollo.biblion.feature.studydocs.model.BlockAlignment.Justify) },
        )

        ToolbarDivider()

        // Tamaño de fuente
        FontSizeControl(
            currentFontSize = currentFontSize,
            onStepFontSize = onStepFontSize,
            onSetFontSize = onSetFontSize,
        )

        ToolbarDivider()

        ZoomMenu(zoomState = zoomState, showStepButtons = false)

        ToolbarIcon(
            icon = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
            contentDescription = if (isDarkTheme) "Usar modo claro" else "Usar modo oscuro",
            isActive = isDarkTheme,
            onClick = { onToggleDarkTheme(!isDarkTheme) },
        )
    }
}

@Composable
private fun ToolbarIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    isActive: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val bg = if (isActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val shape = RoundedCornerShape(6.dp)
    IconButton(
        enabled = enabled,
        onClick = {
            StudyEditorDebugLog.log(
                "TOOLBAR_CLICK",
                "action=$contentDescription isActive=$isActive",
            )
            onClick()
        },
        modifier = Modifier
            .size(36.dp)
            .clip(shape)
            .background(bg),
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = when {
                !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                isActive -> MaterialTheme.colorScheme.onPrimaryContainer
                else -> MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun ToolbarDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(18.dp)
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}

/**
 * Renderiza un fragmento de pagina dentro del `PaginatedSheet` del editor.
 * Extraido para no duplicar la logica entre el modo pantalla completa y el split.
 */
@Composable
private fun EditorSheetFragment(
    fragment: com.cristiancogollo.biblion.feature.studydocs.ui.pagination.PageFragment,
    allBlocks: List<com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock>,
    activeIdProvider: () -> com.cristiancogollo.biblion.feature.studydocs.model.BlockId?,
    activeListItemIndexProvider: () -> Int?,
    focusRequest: com.cristiancogollo.biblion.feature.studydocs.domain.EditorFocusRequest?,
    richStateProvider: (com.cristiancogollo.biblion.feature.studydocs.model.BlockId, Int?) -> com.mohamedrejeb.richeditor.model.RichTextState?,
    splitViewModel: com.cristiancogollo.biblion.feature.studydocs.domain.StudyDocSplitViewModel?,
    viewModel: com.cristiancogollo.biblion.feature.studydocs.domain.StudyDocViewModel?,
    allowProgrammaticFocus: Boolean,
    onVerseSelected: (StudyBlock.Verse) -> Unit,
    onVerseComparisonSelected: (StudyBlock.Verse, String?) -> Unit,
    onVerseDelete: (StudyBlock.Verse) -> Unit,
) {
    val activeId = activeIdProvider()
    val activeListItemIndex = activeListItemIndexProvider()
    val itemIndex = when (fragment) {
        is PageFragment.ListItemSlice -> fragment.itemIndex
        is PageFragment.OrderedListItemSlice -> fragment.itemIndex
        else -> null
    }
    val richState = richStateProvider(fragment.originBlockId, itemIndex)
    val isOwner = activeId == fragment.originBlockId &&
        (itemIndex == null || activeListItemIndex == itemIndex) &&
        fragment.isFirstOwnerFragment()
    Log.d(
        "BIBLION_STUDY",
        "SplitEditor fragment blockId=${fragment.originBlockId} richState=${richState != null} isOwner=$isOwner activeBlockId=$activeId",
    )
    StudyEditorDebugLog.log(
        "FRAGMENT_OWNER",
        "fragment=${fragment::class.simpleName} origin=${fragment.originBlockId.value} " +
            "item=$itemIndex active=${activeId?.value} activeItem=$activeListItemIndex " +
            "owner=$isOwner richState=${richState != null}",
    )
    if (richState != null ||
        fragment is com.cristiancogollo.biblion.feature.studydocs.ui.pagination.PageFragment.VerseSlice ||
        fragment is com.cristiancogollo.biblion.feature.studydocs.ui.pagination.PageFragment.ListItemSlice ||
        fragment is com.cristiancogollo.biblion.feature.studydocs.ui.pagination.PageFragment.OrderedListItemSlice
    ) {
        UnifiedBlockRenderer(
            fragment = fragment,
            allBlocks = allBlocks,
            isEditing = true,
            isOwnerFragment = isOwner,
            richState = richState,
            isActive = isOwner && (
                allowProgrammaticFocus ||
                    fragment is com.cristiancogollo.biblion.feature.studydocs.ui.pagination.PageFragment.VerseSlice
                ),
            focusRequest = focusRequest,
            splitViewModel = splitViewModel,
            viewModel = viewModel,
            onVerseClick = onVerseSelected,
            onVerseComparisonSelected = onVerseComparisonSelected,
            onVerseDelete = onVerseDelete,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun com.cristiancogollo.biblion.feature.studydocs.ui.pagination.PageFragment.isFirstOwnerFragment(): Boolean =
    when (this) {
        is com.cristiancogollo.biblion.feature.studydocs.ui.pagination.PageFragment.ParagraphSlice -> charStart == 0
        is com.cristiancogollo.biblion.feature.studydocs.ui.pagination.PageFragment.HeadingSlice -> charStart == 0
        is com.cristiancogollo.biblion.feature.studydocs.ui.pagination.PageFragment.ListItemSlice -> charStart == 0
        is com.cristiancogollo.biblion.feature.studydocs.ui.pagination.PageFragment.OrderedListItemSlice -> charStart == 0
        is com.cristiancogollo.biblion.feature.studydocs.ui.pagination.PageFragment.VerseSlice -> charStart == 0
        is com.cristiancogollo.biblion.feature.studydocs.ui.pagination.PageFragment.QuoteSlice -> charStart == 0
        is com.cristiancogollo.biblion.feature.studydocs.ui.pagination.PageFragment.Whole -> true
    }

package com.cristiancogollo.biblion.feature.studydocs.ui.editor

import android.widget.Toast
import android.util.Log
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.rememberNavController
import com.cristiancogollo.biblion.feature.studydocs.domain.TextStyleKind
import com.cristiancogollo.biblion.feature.studydocs.model.BlockId
import com.cristiancogollo.biblion.feature.studydocs.model.DocConfig
import com.cristiancogollo.biblion.feature.studydocs.model.DocTagGroups
import com.cristiancogollo.biblion.feature.studydocs.ui.pagination.PaginatedSheet

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

@Composable
private fun currentHighlightPalette() = if (isSystemInDarkTheme()) highlightPaletteDark else highlightPaletteLight

@Composable
fun StudyModeEditorPanel(
    editorState: com.cristiancogollo.biblion.feature.studydocs.domain.StudyEditorUiState,
    focusRequesters: androidx.compose.runtime.snapshots.SnapshotStateMap<BlockId, androidx.compose.ui.focus.FocusRequester>,
    splitViewModel: com.cristiancogollo.biblion.feature.studydocs.domain.StudyDocSplitViewModel?,
    viewModel: com.cristiancogollo.biblion.feature.studydocs.domain.StudyDocViewModel?,
    onSaveClick: () -> Unit,
    onBack: () -> Unit,
) {
    var documentTitle by remember { mutableStateOf(editorState.doc.title) }
    val editorZoomState = rememberDocumentZoomState()
    var isFullScreen by remember { mutableStateOf(false) }

    LaunchedEffect(editorState.doc.title) {
        if (editorState.doc.title != documentTitle) {
            documentTitle = editorState.doc.title
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        // 1. Header: X + título + Alternar pantalla + Guardar
        StudyModeHeader(
            documentTitle = documentTitle,
            isFullScreen = isFullScreen,
            onToggleFullScreen = { isFullScreen = !isFullScreen },
            onTitleChange = { newTitle ->
                documentTitle = newTitle
                splitViewModel?.updateTitle(newTitle) ?: viewModel?.updateTitle(newTitle)
            },
            onBackClick = onBack,
            onSaveClick = onSaveClick,
        )

        // 2. Toolbar integrada plana
        StudyModeToolbar(
            activeFormat = editorState.activeFormat,
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
            onSetAlignment = { alignment ->
                editorState.activeBlockId?.let {
                    splitViewModel?.setBlockAlignment(it, alignment) ?: viewModel?.setBlockAlignment(it, alignment)
                }
            },
            onInsertBlock = { type ->
                splitViewModel?.insertBlock(editorState.activeBlockId, type)
                    ?: viewModel?.insertBlock(editorState.activeBlockId, type)
            },
            onChangeBlockType = { type ->
                editorState.activeBlockId?.let { blockId ->
                    splitViewModel?.changeBlockType(blockId, type)
                        ?: viewModel?.changeBlockType(blockId, type)
                }
            },
            onClearColor = {
                splitViewModel?.clearActiveColor() ?: viewModel?.clearActiveColor()
            },
            zoomState = editorZoomState,
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), thickness = 1.dp)

        if (isFullScreen) {
            PaginatedSheet(
                blocks = editorState.doc.blocks,
                isEditing = true,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                zoomState = editorZoomState,
            ) { fragment, _ ->
                val activeId = splitViewModel?.editorState?.value?.activeBlockId
                    ?: viewModel?.uiState?.value?.activeBlockId
                val richState = splitViewModel?.blockRichStates?.get(fragment.originBlockId)
                    ?: viewModel?.blockRichStates?.get(fragment.originBlockId)
                val isOwner = activeId == fragment.originBlockId
                Log.d("BIBLION_STUDY", "SplitEditor fragment blockId=${fragment.originBlockId} richState=${richState != null} isOwner=$isOwner activeBlockId=$activeId")
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
        } else {
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                // Panel izquierdo: lector bíblico
                BibleReaderPane(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surface),
                )
                VerticalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    thickness = 1.dp,
                )
                // Panel derecho: editor
                PaginatedSheet(
                    blocks = editorState.doc.blocks,
                    isEditing = true,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    zoomState = editorZoomState,
                ) { fragment, _ ->
                    val activeId = splitViewModel?.editorState?.value?.activeBlockId
                        ?: viewModel?.uiState?.value?.activeBlockId
                    val richState = splitViewModel?.blockRichStates?.get(fragment.originBlockId)
                        ?: viewModel?.blockRichStates?.get(fragment.originBlockId)
                    val isOwner = activeId == fragment.originBlockId
                    Log.d("BIBLION_STUDY", "SplitEditor fragment blockId=${fragment.originBlockId} richState=${richState != null} isOwner=$isOwner activeBlockId=$activeId")
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

@Composable
private fun StudyModeHeader(
    documentTitle: String,
    isFullScreen: Boolean,
    onToggleFullScreen: () -> Unit,
    onTitleChange: (String) -> Unit,
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit,
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
        IconButton(onClick = onToggleFullScreen) {
            Icon(
                imageVector = if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                contentDescription = if (isFullScreen) "Pantalla dividida" else "Pantalla completa",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp),
            )
        }

        // Botón Guardar
        IconButton(onClick = onSaveClick) {
            Icon(
                Icons.Default.Save,
                contentDescription = "Guardar",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun BibleReaderPane(modifier: Modifier = Modifier) {
    val readerNavController = rememberNavController()
    Box(modifier = modifier) {
        com.cristiancogollo.biblion.ReaderScreen(
            navController = readerNavController,
            bookName = "Genesis",
            initialChapter = 1,
        )
    }
}

@Composable
private fun StudyModeToolbar(
    activeFormat: com.cristiancogollo.biblion.feature.studydocs.domain.ActiveFormatSnapshot,
    currentFontSize: Int,
    currentAlignment: com.cristiancogollo.biblion.feature.studydocs.model.BlockAlignment,
    onToggleStyle: (TextStyleKind) -> Unit,
    onTextColor: (Int) -> Unit,
    onBackgroundColor: (Int) -> Unit,
    onStepFontSize: (Int) -> Unit,
    onSetAlignment: (com.cristiancogollo.biblion.feature.studydocs.model.BlockAlignment) -> Unit,
    onInsertBlock: (String) -> Unit,
    onChangeBlockType: (String) -> Unit,
    onClearColor: () -> Unit,
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
            onClick = { onToggleStyle(TextStyleKind.Bold) },
        )

        // Cursiva (I)
        ToolbarIcon(
            icon = Icons.Default.FormatItalic,
            contentDescription = "Cursiva",
            isActive = activeFormat.italic,
            onClick = { onToggleStyle(TextStyleKind.Italic) },
        )

        // Subrayado (U)
        ToolbarIcon(
            icon = Icons.Default.FormatUnderlined,
            contentDescription = "Subrayado",
            isActive = activeFormat.underline,
            onClick = { onToggleStyle(TextStyleKind.Underline) },
        )

        // Color de texto (A) — menú flotante
        var showTextColorMenu by remember { mutableStateOf(false) }
        Box {
            ToolbarIcon(
                icon = Icons.Default.FormatColorText,
                contentDescription = "Color de Texto",
                isActive = activeFormat.color != null,
                onClick = { showTextColorMenu = true },
            )
            DropdownMenu(
                expanded = showTextColorMenu,
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
                                    onTextColor(color.hashCode())
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
                onClick = { showHighlightMenu = true },
            )
            DropdownMenu(
                expanded = showHighlightMenu,
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
                    currentHighlightPalette().forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(color, CircleShape)
                                .clickable {
                                    onBackgroundColor(color.hashCode())
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
            onClick = { onChangeBlockType("bullet") },
        )

        // Numerada
        ToolbarIcon(
            icon = Icons.Default.FormatListNumbered,
            contentDescription = "Lista numerada",
            isActive = false,
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
        IconButton(onClick = { onStepFontSize(-1) }, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.TextDecrease, "Reducir", modifier = Modifier.size(18.dp))
        }
        Text(
            text = if (currentFontSize == -1) "-" else "${currentFontSize}px",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        IconButton(onClick = { onStepFontSize(1) }, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.TextIncrease, "Aumentar", modifier = Modifier.size(18.dp))
        }

        ToolbarDivider()

        ZoomMenu(zoomState = zoomState, showStepButtons = false)
    }
}

@Composable
private fun ToolbarIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    isActive: Boolean,
    onClick: () -> Unit,
) {
    val bg = if (isActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val shape = RoundedCornerShape(6.dp)
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(36.dp)
            .clip(shape)
            .background(bg),
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
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
            .background(Color.LightGray),
    )
}

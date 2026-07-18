package com.cristiancogollo.biblion.feature.studydocs.ui.editor

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.cristiancogollo.biblion.feature.studydocs.domain.TextStyleKind
import com.cristiancogollo.biblion.feature.studydocs.model.BlockId
import com.cristiancogollo.biblion.feature.studydocs.model.DocConfig
import com.cristiancogollo.biblion.feature.studydocs.model.DocTagGroups

// Paleta de colores del diseño
private val ColorFondoPanel = Color(0xFFF3F4F6)
private val ColorHoja = Color(0xFFFFFFFF)
private val ColorBarraToolbar = Color(0xFFEDF2FA)
private val ColorTextoPrimario = Color(0xFF0F172A)
private val ColorAccionAzul = Color(0xFF1E3A8A)
private val ColorPlaceholder = Color(0xFF94A3B8)
private val ColorDivisor = Color(0xFFE2E8F0)

private val textColorPalette = listOf(
    Color(0xFF0F172A), // Slate 900
    Color(0xFF424242), // Gray 700
    Color(0xFFE53935), // Red
    Color(0xFFFB8C00), // Orange
    Color(0xFFFDD835), // Yellow
    Color(0xFF43A047), // Green
    Color(0xFF1E88E5), // Blue
    Color(0xFF8E24AA), // Purple
)

private val highlightPalette = listOf(
    Color(0xFFFEF3C7), // Yellow pastel
    Color(0xFFD1FAE5), // Green pastel
    Color(0xFFFFD0D0), // Red pastel
    Color(0xFFD8E8FF), // Blue pastel
    Color(0xFFF3E8FF), // Purple pastel
    Color(0xFFFFF2CC), // Orange pastel
)

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

    LaunchedEffect(editorState.doc.title) {
        if (editorState.doc.title != documentTitle) {
            documentTitle = editorState.doc.title
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorFondoPanel)
    ) {
        // 1. Header: X + título + Guardar
        StudyModeHeader(
            documentTitle = documentTitle,
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
            currentFontSize = editorState.doc.blocks
                .firstOrNull { it.id == editorState.activeBlockId }?.fontSize
                ?: DocConfig.DEFAULT_FONT_SIZE,
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
            onCycleAlignment = {
                editorState.activeBlockId?.let {
                    splitViewModel?.cycleBlockAlignment(it) ?: viewModel?.cycleBlockAlignment(it)
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
        )

        HorizontalDivider(color = ColorDivisor, thickness = 1.dp)

        // 3. Lienzo de papel virtual — Card con weight(1f) en Column
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = ColorHoja),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                itemsIndexed(
                    items = editorState.doc.blocks,
                    key = { _, block -> block.id.value },
                ) { index, block ->
                    val richState = splitViewModel?.blockRichStates?.get(block.id)
                        ?: viewModel?.blockRichStates?.get(block.id)
                    if (richState != null) {
                        StudyModeBlockRenderer(
                            block = block,
                            blockIndex = index,
                            richState = richState,
                            isActive = editorState.activeBlockId == block.id,
                            focusRequesters = focusRequesters,
                            splitViewModel = splitViewModel,
                            viewModel = viewModel,
                            allBlocks = editorState.doc.blocks,
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
    onTitleChange: (String) -> Unit,
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Botón X para salir
        IconButton(onClick = onBackClick) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Salir del Modo Estudio",
                tint = ColorTextoPrimario,
            )
        }

        // Título editable
        BasicTextField(
            value = documentTitle,
            onValueChange = onTitleChange,
            textStyle = TextStyle(
                color = ColorTextoPrimario,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            ),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            singleLine = true,
            cursorBrush = SolidColor(ColorAccionAzul),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.Center) {
                    if (documentTitle.isEmpty()) {
                        Text(
                            text = "Documento sin titulo",
                            color = ColorPlaceholder,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    innerTextField()
                }
            },
        )

        // Botón Guardar
        IconButton(onClick = onSaveClick) {
            Icon(
                Icons.Default.Save,
                contentDescription = "Guardar",
                tint = ColorAccionAzul,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun StudyModeToolbar(
    activeFormat: com.cristiancogollo.biblion.feature.studydocs.domain.ActiveFormatSnapshot,
    currentFontSize: Int,
    onToggleStyle: (TextStyleKind) -> Unit,
    onTextColor: (Int) -> Unit,
    onBackgroundColor: (Int) -> Unit,
    onStepFontSize: (Int) -> Unit,
    onCycleAlignment: () -> Unit,
    onInsertBlock: (String) -> Unit,
    onChangeBlockType: (String) -> Unit,
    onClearColor: () -> Unit,
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ColorBarraToolbar)
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
                    highlightPalette.forEach { color ->
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
            isActive = true,
            onClick = { onCycleAlignment() },
        )

        // Alineación centro
        ToolbarIcon(
            icon = Icons.Default.FormatAlignCenter,
            contentDescription = "Alinear centro",
            isActive = false,
            onClick = { onCycleAlignment() },
        )

        // Justificado
        ToolbarIcon(
            icon = Icons.Default.FormatAlignJustify,
            contentDescription = "Alinear justificado",
            isActive = false,
            onClick = { onCycleAlignment() },
        )

        ToolbarDivider()

        // Tamaño de fuente
        IconButton(onClick = { onStepFontSize(-1) }, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.TextDecrease, "Reducir", modifier = Modifier.size(18.dp))
        }
        Text(
            text = if (currentFontSize == -1) "-" else "${currentFontSize}px",
            style = MaterialTheme.typography.labelSmall,
            color = ColorTextoPrimario,
        )
        IconButton(onClick = { onStepFontSize(1) }, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.TextIncrease, "Aumentar", modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun ToolbarIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    isActive: Boolean,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, modifier = Modifier.size(36.dp)) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = if (isActive) ColorAccionAzul else ColorTextoPrimario,
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

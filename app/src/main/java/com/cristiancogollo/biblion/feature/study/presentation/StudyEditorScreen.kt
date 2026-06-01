package com.cristiancogollo.biblion

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cristiancogollo.biblion.ui.theme.BiblionBluePrimary
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditorDefaults
import com.cristiancogollo.biblion.ui.theme.BiblionGoldPrimary
import com.cristiancogollo.biblion.ui.theme.BiblionGoldSoft
import com.cristiancogollo.biblion.ui.theme.BiblionNavy
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyEditorScreen(
    viewModel: StudyViewModel,
    onClose: () -> Unit,
    onFocusModeChanged: (Boolean) -> Unit
) {
    val ui by viewModel.state.collectAsState()
    val richState = rememberRichTextState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var menuOffset by remember { mutableStateOf(IntOffset(0, -120)) }
    var editorContainerWidthPx by remember { mutableStateOf(0) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var saveTitle by remember { mutableStateOf("") }
    var saveTagsInput by remember { mutableStateOf("") }
    var saveError by remember { mutableStateOf<String?>(null) }

    fun openMetadataDialog() {
        saveTitle = ui.title
        saveTagsInput = ui.tags.joinToString(", ")
        saveError = null
        showSaveDialog = true
    }

    fun validateNonEmptyContent(): Boolean {
        val plainContent = ui.richHtml
            .replace(Regex("<[^>]*>"), " ")
            .replace("&nbsp;", " ")
            .trim()
        return plainContent.isNotBlank()
    }

    fun handleSave() {
        if (ui.title.isBlank() && ui.tags.isEmpty()) {
            openMetadataDialog()
            return
        }
        if (!validateNonEmptyContent()) {
            scope.launch { snackbarHostState.showSnackbar("No puedes guardar una enseñanza vacía.") }
            return
        }
        viewModel.process(StudyIntent.SaveStudy)
    }

    fun htmlEscape(value: String): String =
        value.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")

    fun selectedText(): String {
        val range = richState.selection
        val text = richState.annotatedString.text
        return text.substring(range.min, range.max.coerceAtMost(text.length)).trim()
    }

    fun applySelectionStyle(style: SpanStyle) {
        val range = richState.selection
        if (range.collapsed) {
            richState.addSpanStyle(style)
        } else {
            richState.addSpanStyle(style, TextRange(range.min, range.max))
            richState.selection = range
        }
    }

    fun toggleSelectionStyle(style: SpanStyle, isActive: Boolean) {
        val range = richState.selection
        if (range.collapsed) {
            if (isActive) {
                richState.removeSpanStyle(style)
            } else {
                richState.addSpanStyle(style)
            }
        } else {
            if (isActive) {
                richState.removeSpanStyle(style, range)
            } else {
                richState.addSpanStyle(style, range)
            }
            richState.selection = range
        }
    }

    fun keepSelection(action: () -> Unit) {
        val range = richState.selection
        action()
        if (!range.collapsed) {
            richState.selection = range
        }
    }

    fun insertStudyBlock(label: String, body: String = "") {
        val safeBody = body.ifBlank { "Escribe aqui..." }
        richState.insertHtmlAfterSelection(
            """
            <br>
            <blockquote><b>$label</b><br>${htmlEscape(safeBody)}</blockquote>
            <br>
            """.trimIndent()
        )
    }

    fun clearSelectionFormatting() {
        val range = richState.selection
        val style = richState.currentSpanStyle
        val styles = buildList {
            add(SpanStyle(fontWeight = FontWeight.Bold))
            add(SpanStyle(fontStyle = FontStyle.Italic))
            add(SpanStyle(textDecoration = TextDecoration.Underline))
            if (style.color != Color.Unspecified) add(SpanStyle(color = style.color))
            if (style.background != Color.Unspecified) add(SpanStyle(background = style.background))
        }
        styles.forEach { spanStyle ->
            if (range.collapsed) richState.removeSpanStyle(spanStyle) else richState.removeSpanStyle(spanStyle, range)
        }
        if (!range.collapsed) {
            richState.selection = range
        }
    }

    fun clearSelectionTextColor() {
        val range = richState.selection
        val color = richState.currentSpanStyle.color
        if (color != Color.Unspecified) {
            val style = SpanStyle(color = color)
            if (range.collapsed) richState.removeSpanStyle(style) else richState.removeSpanStyle(style, range)
            if (!range.collapsed) richState.selection = range
        }
    }

    fun clearSelectionBackground() {
        val range = richState.selection
        val background = richState.currentSpanStyle.background
        if (background != Color.Unspecified) {
            val style = SpanStyle(background = background)
            if (range.collapsed) richState.removeSpanStyle(style) else richState.removeSpanStyle(style, range)
            if (!range.collapsed) richState.selection = range
        }
    }

    val selection = richState.selection
    val hasSelection = selection.start != selection.end
    val showMenu = hasSelection || ui.pendingCitations.isNotEmpty()

    // Sincronizar estado del ViewModel al Editor
    LaunchedEffect(ui.richHtml) {
        if (richState.toHtml() != ui.richHtml) {
            richState.setHtml(ui.richHtml)
        }
    }

    // Guardar cambios del Editor al ViewModel
    LaunchedEffect(richState) {
        snapshotFlow { richState.toHtml() }
            .debounce(250)
            .distinctUntilChanged()
            .collect { html ->
                viewModel.process(StudyIntent.UpdateRichHtml(html))
            }
    }

    LaunchedEffect(hasSelection) {
        viewModel.process(StudyIntent.SetSelectionActive(hasSelection))
        if (hasSelection) {
            menuOffset = IntOffset(0, -220)
        }
    }

    LaunchedEffect(selection) {
        if (selection.collapsed) {
            val style = richState.currentSpanStyle
            if (style.textDecoration == TextDecoration.Underline) {
                richState.removeSpanStyle(SpanStyle(textDecoration = TextDecoration.Underline))
            }
            if (style.background != Color.Unspecified) {
                richState.removeSpanStyle(SpanStyle(background = style.background))
            }
        }
    }

    LaunchedEffect(ui.pendingCitations.size) {
        if (ui.pendingCitations.isNotEmpty()) {
            snackbarHostState.showSnackbar("${ui.pendingCitations.size} cita(s) lista(s) para insertar")
        }
    }

    LaunchedEffect(ui.focusMode) { onFocusModeChanged(ui.focusMode) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(
                tonalElevation = 2.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar Modo Estudio", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Editor de Estudio",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Botón de Guardar Enseñanza
                        IconButton(onClick = {
                            handleSave()
                        }) {
                            Icon(Icons.Default.Save, contentDescription = "Guardar Enseñanza", tint = MaterialTheme.colorScheme.onSurface)
                        }
                        
                        // Botón de Modo Enfoque (Ampliar)
                        IconButton(onClick = { viewModel.process(StudyIntent.ToggleFocusMode(!ui.focusMode)) }) {
                            Icon(
                                imageVector = if (ui.focusMode) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = "Modo Enfoque",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = if (ui.focusMode) 64.dp else 24.dp)
                .onGloballyPositioned { coordinates ->
                    editorContainerWidthPx = coordinates.size.width
                }
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown || !event.isCtrlPressed) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.B -> {
                            richState.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold)); true
                        }
                        Key.I -> {
                            richState.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic)); true
                        }
                        Key.S -> {
                            handleSave()
                            true
                        }
                        else -> false
                    }
                }
        ) {
            RichTextEditor(
                state = richState,
                modifier = Modifier.fillMaxSize(),
                colors = RichTextEditorDefaults.richTextEditorColors(containerColor = Color.Transparent),
                placeholder = { Text("Comienza a escribir tu enseñanza aquí...") }
            )

            StudyEditorFloatingMenu(
                isVisible = showMenu,
                anchorOffset = menuOffset,
                containerWidthPx = editorContainerWidthPx,
                pendingCitations = ui.pendingCitations.size,
                onDismiss = { viewModel.process(StudyIntent.SetContextMenuVisible(false)) },
                onHeadlineUp = {
                    keepSelection { richState.toggleSpanStyle(SpanStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold)) }
                },
                onHeadlineDown = {
                    keepSelection { richState.toggleSpanStyle(SpanStyle(fontSize = 18.sp, fontWeight = FontWeight.Normal)) }
                },
                onBold = { keepSelection { richState.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold)) } },
                onItalic = { keepSelection { richState.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic)) } },
                onUnderline = {
                    toggleSelectionStyle(
                        style = SpanStyle(textDecoration = TextDecoration.Underline),
                        isActive = richState.currentSpanStyle.textDecoration == TextDecoration.Underline
                    )
                },
                onTextColor = { color -> applySelectionStyle(SpanStyle(color = color)) },
                onBackgroundColor = { color -> applySelectionStyle(SpanStyle(background = color)) },
                onClearTextColor = { clearSelectionTextColor() },
                onClearBackground = { clearSelectionBackground() },
                onClearFormatting = { clearSelectionFormatting() },
                onIncreaseSize = {
                    viewModel.process(StudyIntent.IncreaseSelectionFont)
                    keepSelection { richState.toggleSpanStyle(SpanStyle(fontSize = (ui.selectionFontSizeSp + 2f).sp)) }
                },
                onDecreaseSize = {
                    viewModel.process(StudyIntent.DecreaseSelectionFont)
                    keepSelection { richState.toggleSpanStyle(SpanStyle(fontSize = (ui.selectionFontSizeSp - 2f).coerceAtLeast(12f).sp)) }
                },
                onBulletList = { richState.toggleUnorderedList() },
                onOrderedList = { richState.toggleOrderedList() },
                onInsertPendingCitations = {
                    val currentHtml = richState.toHtml()
                    val newCitationsHtml = StringBuilder()
                    viewModel.consumePendingCitations().forEach { request ->
                        val quoteHtml = if (request.includeFullText) {
                            "<blockquote><i>\"${request.text}\"</i><br>— <b>${request.reference}</b></blockquote>"
                        } else {
                            "<blockquote>— <b>${request.reference}</b></blockquote>"
                        }
                        newCitationsHtml.append("<br>").append(quoteHtml).append("<br>")
                    }
                    richState.setHtml(currentHtml + newCitationsHtml.toString())
                },
                onInsertNote = { insertStudyBlock("Nota", selectedText()) },
                onInsertReflection = { insertStudyBlock("Reflexion", selectedText()) },
                onInsertPrayer = { insertStudyBlock("Oracion") },
                onInsertQuestion = { insertStudyBlock("Pregunta", selectedText()) }
            )
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text("Guardar enseñanza") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = saveTitle,
                        onValueChange = {
                            saveTitle = it
                            saveError = null
                        },
                        singleLine = true,
                        label = { Text("Título") },
                        placeholder = { Text("Ej: La fe en tiempos difíciles") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BiblionNavy,
                            unfocusedBorderColor = BiblionGoldPrimary,
                            focusedLabelColor = BiblionNavy,
                            cursorColor = BiblionNavy
                        )
                    )
                    OutlinedTextField(
                        value = saveTagsInput,
                        onValueChange = {
                            saveTagsInput = it
                            saveError = null
                        },
                        singleLine = true,
                        label = { Text("Etiquetas") },
                        placeholder = { Text("Ej: fe, oración, esperanza") },
                        supportingText = { Text("Separa las etiquetas por comas.") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BiblionNavy,
                            unfocusedBorderColor = BiblionGoldPrimary,
                            focusedLabelColor = BiblionNavy,
                            cursorColor = BiblionNavy
                        )
                    )
                    saveError?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val cleanedTitle = saveTitle.trim()
                    val parsedTags = saveTagsInput.split(",")
                        .map { it.trim().removePrefix("#") }
                        .filter { it.isNotBlank() }
                        .distinct()
                    saveError = when {
                        !validateNonEmptyContent() -> "No puedes guardar una enseñanza vacía."
                        cleanedTitle.isBlank() && parsedTags.isEmpty() ->
                            "Debes agregar al menos un título o una etiqueta para guardar."
                        else -> null
                    }

                    if (saveError == null) {
                        viewModel.process(StudyIntent.SaveStudyWithMetadata(cleanedTitle, parsedTags))
                        showSaveDialog = false
                    }
                }, colors = ButtonDefaults.textButtonColors(contentColor = BiblionGoldPrimary)) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSaveDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = BiblionBluePrimary)
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

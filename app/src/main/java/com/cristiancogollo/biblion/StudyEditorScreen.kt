package com.cristiancogollo.biblion

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditorDefaults
import com.cristiancogollo.biblion.ui.theme.BiblionNavy

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
    LaunchedEffect(richState.toHtml()) {
        viewModel.process(StudyIntent.UpdateRichHtml(richState.toHtml()))
    }

    LaunchedEffect(hasSelection) {
        viewModel.process(StudyIntent.SetSelectionActive(hasSelection))
        if (hasSelection) {
            menuOffset = IntOffset(0, -220)
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
        containerColor = Color(0xFFFDFBF0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(
                tonalElevation = 2.dp,
                color = Color.White
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
                            Icon(Icons.Default.Close, contentDescription = "Cerrar Modo Estudio", tint = Color.Gray)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Editor de Estudio",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = BiblionNavy
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Botón de Guardar Enseñanza
                        IconButton(onClick = { 
                            viewModel.process(StudyIntent.SaveStudy)
                            // Opcional: Mostrar feedback al guardar
                        }) {
                            Icon(Icons.Default.Save, contentDescription = "Guardar Enseñanza", tint = BiblionNavy)
                        }
                        
                        // Botón de Modo Enfoque (Ampliar)
                        IconButton(onClick = { viewModel.process(StudyIntent.ToggleFocusMode(!ui.focusMode)) }) {
                            Icon(
                                imageVector = if (ui.focusMode) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = "Modo Enfoque",
                                tint = BiblionNavy
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
                            viewModel.process(StudyIntent.SaveStudy); true
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
                pendingCitations = ui.pendingCitations.size,
                onDismiss = { viewModel.process(StudyIntent.SetContextMenuVisible(false)) },
                onHeadlineUp = {
                    richState.toggleSpanStyle(SpanStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold))
                },
                onHeadlineDown = {
                    richState.toggleSpanStyle(SpanStyle(fontSize = 18.sp, fontWeight = FontWeight.Normal))
                },
                onBold = { richState.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold)) },
                onItalic = { richState.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic)) },
                onIncreaseSize = {
                    viewModel.process(StudyIntent.IncreaseSelectionFont)
                    richState.toggleSpanStyle(SpanStyle(fontSize = (ui.selectionFontSizeSp + 2f).sp))
                },
                onDecreaseSize = {
                    viewModel.process(StudyIntent.DecreaseSelectionFont)
                    richState.toggleSpanStyle(SpanStyle(fontSize = (ui.selectionFontSizeSp - 2f).coerceAtLeast(12f).sp))
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
                }
            )
        }
    }
}

package com.cristiancogollo.biblion

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cristiancogollo.biblion.ui.theme.BiblionNavy
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditorDefaults
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

    val selection = richState.selection
    val hasSelection = selection.start != selection.end
    val showMenu = hasSelection || ui.pendingCitations.isNotEmpty()

    LaunchedEffect(ui.richHtml) {
        if (richState.toHtml() != ui.richHtml) richState.setHtml(ui.richHtml)
    }

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
            Surface(tonalElevation = 2.dp, color = Color.White) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.Gray)
                        }
                        Text(
                            text = "Editor de Estudio",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = BiblionNavy
                        )
                    }

                    Row {
                        IconButton(onClick = {
                            viewModel.process(StudyIntent.Save)
                            scope.launch {
                                snackbarHostState.showSnackbar("Enseñanza guardada")
                            }
                        }) {
                            Icon(Icons.Default.Save, contentDescription = "Guardar", tint = BiblionNavy)
                        }
                        IconButton(onClick = { viewModel.process(StudyIntent.ToggleFocusMode(!ui.focusMode)) }) {
                            Icon(
                                if (ui.focusMode) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
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
                .padding(horizontal = if (ui.focusMode) 64.dp else 24.dp, vertical = 12.dp)
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown || !event.isCtrlPressed) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.B -> {
                            richState.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold)); true
                        }
                        Key.I -> {
                            richState.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic)); true
                        }
                        else -> false
                    }
                }
        ) {
            RichTextEditor(
                state = richState,
                modifier = Modifier.fillMaxSize(),
                colors = RichTextEditorDefaults.richTextEditorColors(
                    containerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
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

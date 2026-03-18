package com.cristiancogollo.biblion

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.RichTextEditor

@Composable
fun StudyEditorScreen(
    viewModel: StudyViewModel,
    onClose: () -> Unit,
    onFocusModeChanged: (Boolean) -> Unit
) {
    val ui by viewModel.state.collectAsState()
    val richState = rememberRichTextState()
    val snackbarHostState = remember { SnackbarHostState() }
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
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFDFBF0)),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = onClose, containerColor = MaterialTheme.colorScheme.surface) {
                Icon(Icons.Default.Close, contentDescription = "Cerrar")
            }
        },
        topBar = {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
            ) {
                Text("The Minimalist Slate", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = { viewModel.process(StudyIntent.ToggleFocusMode(!ui.focusMode)) }) {
                    Icon(if (ui.focusMode) Icons.Default.FullscreenExit else Icons.Default.Fullscreen, contentDescription = "Focus")
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 48.dp, vertical = 12.dp)
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown || !event.isCtrlPressed) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.B -> {
                            richState.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold)); true
                        }
                        Key.I -> {
                            richState.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic)); true
                        }
                        else -> {
                            val ch = event.nativeKeyEvent.unicodeChar.toChar()
                            if (ch == '[') {
                                viewModel.process(StudyIntent.DecreaseSelectionFont)
                                richState.toggleSpanStyle(SpanStyle(fontSize = ui.selectionFontSizeSp.sp))
                                true
                            } else false
                        }
                    }
                }
        ) {
            RichTextEditor(
                state = richState,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFFDFBF0))
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
                    viewModel.consumePendingCitations().forEach { request ->
                        richState.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic))
                        richState.addText(viewModel.asBlockquoteText(request))
                    }
                }
            )
        }
    }
}

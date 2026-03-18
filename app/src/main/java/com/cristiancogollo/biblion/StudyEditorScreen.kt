package com.cristiancogollo.biblion

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.RichTextEditor
import kotlinx.coroutines.launch

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
        if (hasSelection) menuOffset = IntOffset(0, -220)
    }

    LaunchedEffect(ui.pendingCitations.size) {
        if (ui.pendingCitations.isNotEmpty()) {
            snackbarHostState.showSnackbar("${ui.pendingCitations.size} cita(s) lista(s) para insertar")
        }
    }

    LaunchedEffect(ui.exportMessage) {
        ui.exportMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.process(StudyIntent.ClearExportMessage)
        }
    }

    LaunchedEffect(ui.saveError) {
        ui.saveError?.let { snackbarHostState.showSnackbar(it) }
    }

    LaunchedEffect(ui.focusMode) { onFocusModeChanged(ui.focusMode) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFDFBF0)),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFFFCF3))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = ui.title,
                            onValueChange = { viewModel.process(StudyIntent.UpdateTitle(it)) },
                            label = { Text("Título de la enseñanza") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = viewModel.saveStatusLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (ui.hasUnsavedChanges) MaterialTheme.colorScheme.primary else Color.Gray,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = { viewModel.createStudy() }, modifier = Modifier.padding(end = 8.dp)) {
                            Icon(Icons.Default.NoteAdd, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Nueva")
                        }
                        OutlinedButton(onClick = { viewModel.duplicateCurrentStudy() }, modifier = Modifier.padding(end = 8.dp)) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Duplicar")
                        }
                        Button(
                            onClick = {
                                viewModel.saveStudyNow { saved ->
                                    if (saved) scope.launch { snackbarHostState.showSnackbar("Enseñanza guardada") }
                                }
                            },
                            enabled = !ui.isSaving,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(if (ui.isSaving) "Guardando..." else "Guardar enseñanza")
                        }
                        TextButton(onClick = onClose) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar modo estudio")
                            Text("Cerrar modo estudio")
                        }
                        IconButton(onClick = { viewModel.process(StudyIntent.ToggleFocusMode(!ui.focusMode)) }) {
                            Icon(
                                if (ui.focusMode) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = "Ampliar pantalla"
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1.3f)
                    .fillMaxHeight()
                    .padding(end = 12.dp)
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
                    onHeadlineUp = { richState.toggleSpanStyle(SpanStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold)) },
                    onHeadlineDown = { richState.toggleSpanStyle(SpanStyle(fontSize = 18.sp, fontWeight = FontWeight.Normal)) },
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

            if (!ui.focusMode) {
                StudyWorkspacePanel(
                    ui = ui,
                    onSelectStudy = { viewModel.process(StudyIntent.SelectStudy(it)) },
                    onSelectNotebook = { viewModel.process(StudyIntent.SelectNotebook(it)) },
                    onSearchQueryChange = { viewModel.process(StudyIntent.UpdateSearchQuery(it)) },
                    onOpenSearchResult = { viewModel.selectSearchResult(it) },
                    onDeleteStudy = { viewModel.deleteCurrentStudy() },
                    onExportText = { viewModel.process(StudyIntent.ExportPdf) },
                    onShareStudy = { viewModel.shareStudy() },
                    onCopyStudy = { viewModel.copyStudyToClipboard() }
                )
            }
        }
    }
}

@Composable
private fun StudyWorkspacePanel(
    ui: StudyUiState,
    onSelectStudy: (Long) -> Unit,
    onSelectNotebook: (Long) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onOpenSearchResult: (StudySearchResult) -> Unit,
    onDeleteStudy: () -> Unit,
    onExportText: () -> Unit,
    onShareStudy: () -> Unit,
    onCopyStudy: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(320.dp)
            .fillMaxHeight(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("Panel de trabajo", style = MaterialTheme.typography.titleMedium)
            Text("Tus cuadernos, búsquedas y referencias en un solo lugar.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Spacer(modifier = Modifier.height(12.dp))

            Text("Cuadernos", style = MaterialTheme.typography.labelLarge)
            ui.notebooks.forEach { notebook ->
                AssistChip(
                    onClick = { onSelectNotebook(notebook.id) },
                    label = { Text(notebook.title) },
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Enseñanzas", style = MaterialTheme.typography.labelLarge)
            ui.studies.take(8).forEach { study ->
                TextButton(onClick = { onSelectStudy(study.id) }, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(study.title, fontWeight = if (study.id == ui.selectedStudyId) FontWeight.Bold else FontWeight.Normal)
                        Text("Actualizada: ${study.updatedAt}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = ui.searchQuery,
                onValueChange = onSearchQueryChange,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                label = { Text("Buscar en mis enseñanzas") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            if (ui.searchResults.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.height(180.dp)) {
                    items(ui.searchResults) { result ->
                        TextButton(onClick = { onOpenSearchResult(result) }, modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(result.title, fontWeight = FontWeight.SemiBold)
                                Text(result.snippet.ifBlank { "Sin contenido previo" }, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Resumen del documento", style = MaterialTheme.typography.labelLarge)
            ui.workspaceItems.forEach { item ->
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(item.label)
                    Text(item.detail, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Referencias recientes", style = MaterialTheme.typography.labelLarge)
            if (ui.recentReferences.isEmpty()) {
                Text("Aún no hay referencias detectadas en esta enseñanza.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            } else {
                ui.recentReferences.forEach { reference ->
                    AssistChip(onClick = {}, label = { Text(reference) }, modifier = Modifier.padding(top = 6.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Salida y respaldo", style = MaterialTheme.typography.labelLarge)
            OutlinedButton(onClick = onCopyStudy, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Icon(Icons.Default.ContentCopy, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Copiar enseñanza")
            }
            OutlinedButton(onClick = onShareStudy, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Icon(Icons.Default.ScreenShare, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Compartir texto")
            }
            OutlinedButton(onClick = onExportText, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Icon(Icons.Default.FileUpload, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Exportar TXT")
            }
            OutlinedButton(onClick = onDeleteStudy, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Eliminar enseñanza")
            }
        }
    }
}

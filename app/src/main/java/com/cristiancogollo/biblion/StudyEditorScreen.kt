package com.cristiancogollo.biblion

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cristiancogollo.biblion.ui.theme.BiblionNavy

private fun applyAroundSelection(value: TextFieldValue, prefix: String, suffix: String): TextFieldValue {
    val start = value.selection.min
    val end = value.selection.max
    if (start == end) return value
    val selected = value.text.substring(start, end)
    val replacement = "$prefix$selected$suffix"
    val updated = value.text.replaceRange(start, end, replacement)
    val cursor = start + replacement.length
    return value.copy(text = updated, selection = TextRange(cursor, cursor))
}

private fun applyToSelection(value: TextFieldValue, transform: (String) -> String): TextFieldValue {
    val start = value.selection.min
    val end = value.selection.max
    if (start == end) return value
    val selected = value.text.substring(start, end)
    val replacement = transform(selected)
    val updated = value.text.replaceRange(start, end, replacement)
    val cursor = start + replacement.length
    return value.copy(text = updated, selection = TextRange(cursor, cursor))
}

private fun toBoldUnicode(input: String): String = buildString {
    input.forEach { char ->
        val codePoint = when (char) {
            in 'A'..'Z' -> 0x1D400 + (char.code - 'A'.code)
            in 'a'..'z' -> 0x1D41A + (char.code - 'a'.code)
            in '0'..'9' -> 0x1D7CE + (char.code - '0'.code)
            else -> null
        }
        if (codePoint != null) {
            append(String(Character.toChars(codePoint)))
        } else {
            append(char)
        }
    }
}

private fun toUnderlineUnicode(input: String): String = buildString {
    input.forEach { char ->
        append(char)
        if (char != ' ' && char != '\n') {
            append('\u0332')
        }
    }
}

private fun buildFormattedStudyText(text: String, defaultSize: Float): AnnotatedString {
    val regex = Regex("\\[size=(\\d+(?:\\.\\d+)?)\\](.*?)\\[/size\\]", RegexOption.DOT_MATCHES_ALL)
    val builder = AnnotatedString.Builder()
    var cursor = 0

    regex.findAll(text).forEach { match ->
        val start = match.range.first
        val end = match.range.last + 1
        val rawSize = match.groupValues[1].toFloatOrNull()?.coerceIn(12f, 32f) ?: defaultSize
        val content = match.groupValues[2]

        if (start > cursor) {
            builder.append(text.substring(cursor, start))
        }

        builder.withStyle(
            androidx.compose.ui.text.SpanStyle(
                fontSize = rawSize.sp,
                fontWeight = FontWeight.Medium
            )
        ) {
            append(content)
        }
        cursor = end
    }

    if (cursor < text.length) {
        builder.append(text.substring(cursor))
    }

    return builder.toAnnotatedString()
}

@Composable
fun StudyEditorScreen(
    viewModel: StudyViewModel,
    onClose: () -> Unit
) {
    var selectedCitation by remember { mutableStateOf<StudyCitation?>(null) }
    val clipboard = LocalClipboardManager.current

    var noteField by remember {
        mutableStateOf(TextFieldValue(viewModel.noteContent))
    }

    LaunchedEffect(viewModel.noteContent) {
        if (viewModel.noteContent != noteField.text) {
            noteField = noteField.copy(text = viewModel.noteContent)
        }
    }

    val hasSelection = noteField.selection.start != noteField.selection.end

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CUADERNO DE ESTUDIO",
                    style = MaterialTheme.typography.labelLarge,
                    color = BiblionNavy,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onClose) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar", tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            BasicTextField(
                value = viewModel.noteTitle,
                onValueChange = { viewModel.updateTitle(it) },
                textStyle = TextStyle(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    color = Color.Black
                ),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    if (viewModel.noteTitle.isEmpty()) {
                        Text("Título de la enseñanza...", color = Color.Gray, fontSize = 20.sp, fontFamily = FontFamily.Serif)
                    }
                    innerTextField()
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            BasicTextField(
                value = viewModel.baseReference,
                onValueChange = { viewModel.updateBaseReference(it) },
                textStyle = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Serif,
                    color = Color(0xFF1A3A6E)
                ),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    if (viewModel.baseReference.isEmpty()) {
                        Text("Versículo o texto base...", color = Color.Gray, fontSize = 16.sp, fontFamily = FontFamily.Serif)
                    }
                    innerTextField()
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 1.dp, color = Color.LightGray)

            if (viewModel.citations.isNotEmpty()) {
                Text(
                    text = "Versículos citados",
                    style = MaterialTheme.typography.labelLarge,
                    color = BiblionNavy,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    viewModel.citations.forEach { citation ->
                        AssistChip(
                            onClick = { selectedCitation = citation },
                            label = { Text(citation.reference) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                SelectionContainer {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        viewModel.citations.takeLast(3).forEach { citation ->
                            Text(
                                text = "\"${citation.text}\"",
                                fontStyle = FontStyle.Italic,
                                fontFamily = FontFamily.Serif,
                                color = Color(0xFF303030)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (hasSelection) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, shape = MaterialTheme.shapes.medium)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Copiar", color = BiblionNavy, modifier = Modifier.clickable {
                        val start = noteField.selection.min
                        val end = noteField.selection.max
                        val selected = noteField.text.substring(start, end)
                        if (selected.isNotEmpty()) {
                            clipboard.setText(AnnotatedString(selected))
                        }
                    })
                    Text("Subrayar", color = BiblionNavy, modifier = Modifier.clickable {
                        noteField = applyToSelection(noteField, ::toUnderlineUnicode)
                        viewModel.updateContent(noteField.text)
                    })
                    Text("Negrita", color = BiblionNavy, modifier = Modifier.clickable {
                        noteField = applyToSelection(noteField, ::toBoldUnicode)
                        viewModel.updateContent(noteField.text)
                    })
                    Text("A+", color = BiblionNavy, modifier = Modifier.clickable {
                        noteField = applyAroundSelection(
                            noteField,
                            "[size=${(viewModel.noteFontSize + 3f).coerceIn(12f, 32f)}]",
                            "[/size]"
                        )
                        viewModel.updateContent(noteField.text)
                    })
                    Text("A-", color = BiblionNavy, modifier = Modifier.clickable {
                        noteField = applyAroundSelection(
                            noteField,
                            "[size=${(viewModel.noteFontSize - 2f).coerceIn(12f, 32f)}]",
                            "[/size]"
                        )
                        viewModel.updateContent(noteField.text)
                    })
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = "Vista previa",
                style = MaterialTheme.typography.labelMedium,
                color = BiblionNavy,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = buildFormattedStudyText(noteField.text, viewModel.noteFontSize),
                style = TextStyle(
                    fontSize = viewModel.noteFontSize.sp,
                    fontFamily = FontFamily.Serif,
                    color = Color(0xFF303030),
                    lineHeight = (viewModel.noteFontSize + 8f).sp,
                    fontSynthesis = FontSynthesis.All
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, MaterialTheme.shapes.medium)
                    .padding(10.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))

            BasicTextField(
                value = noteField,
                onValueChange = {
                    noteField = it
                    viewModel.updateContent(it.text)
                },
                textStyle = TextStyle(
                    fontSize = viewModel.noteFontSize.sp,
                    fontFamily = FontFamily.Serif,
                    color = Color.DarkGray,
                    lineHeight = (viewModel.noteFontSize + 8f).sp
                ),
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                decorationBox = { innerTextField ->
                    if (noteField.text.isEmpty()) {
                        Text("Empieza a escribir tus notas aquí...", color = Color.Gray, fontSize = 16.sp)
                    }
                    innerTextField()
                }
            )
        }

        selectedCitation?.let { citation ->
            AlertDialog(
                onDismissRequest = { selectedCitation = null },
                title = { Text(citation.reference) },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        citation.previousText?.let {
                            Text("Anterior: $it", color = Color.Gray)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        Text("\"${citation.text}\"", fontStyle = FontStyle.Italic, fontFamily = FontFamily.Serif)
                        citation.nextText?.let {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Siguiente: $it", color = Color.Gray)
                        }
                    }
                },
                confirmButton = {
                    Text(
                        text = "Cerrar",
                        color = BiblionNavy,
                        modifier = Modifier
                            .clickable { selectedCitation = null }
                            .padding(8.dp)
                    )
                }
            )
        }

        LargeFloatingActionButton(
            onClick = { },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = BiblionNavy,
            contentColor = Color.White,
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Text(text = "AI", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
    }
}

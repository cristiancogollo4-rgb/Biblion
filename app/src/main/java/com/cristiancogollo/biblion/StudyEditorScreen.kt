package com.cristiancogollo.biblion

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cristiancogollo.biblion.ui.theme.BiblionNavy

@Composable
fun StudyEditorScreen(
    viewModel: StudyViewModel,
    onClose: () -> Unit
) {
    var selectedCitation by remember { mutableStateOf<StudyCitation?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)) // Fondo ligeramente distinto para el editor
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

            // Título de la enseñanza
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
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Contenido de la nota
            BasicTextField(
                value = viewModel.noteContent,
                onValueChange = { viewModel.updateContent(it) },
                textStyle = TextStyle(
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Serif,
                    color = Color.DarkGray,
                    lineHeight = 24.sp
                ),
                modifier = Modifier.fillMaxSize().weight(1f),
                decorationBox = { innerTextField ->
                    if (viewModel.noteContent.isEmpty()) {
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
                        Text("Base: ${citation.text}")
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

        // FloatingActionButton "AI"
        LargeFloatingActionButton(
            onClick = { /* Acción AI */ },
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

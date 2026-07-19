package com.cristiancogollo.biblion.feature.studydocs.ui.editor

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatColorReset
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cristiancogollo.biblion.BibleRepository
import com.cristiancogollo.biblion.BibleVersionOption
import com.cristiancogollo.biblion.ui.theme.BiblionGoldPrimary
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import kotlinx.coroutines.launch

private val VerseBgColor = Color(0xFFFEF3C7)
private val VerseBorderColor = Color(0xFFFDE68A)
private val VerseRefColor = Color(0xFF92400E)

@Composable
fun BibleVerseBlock(
    block: StudyBlock.Verse,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(block.showCompare) }
    var showVersionDialog by remember { mutableStateOf(false) }
    var comparedTexts by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var selectedVersions by remember { mutableStateOf<Set<String>>(emptySet()) }
    var availableVersions by remember { mutableStateOf<List<BibleVersionOption>>(emptyList()) }
    val scope = rememberCoroutineScope()

    val currentText = block.contents[block.sourceVersion] ?: ""

    // Texto con números de versículo coloreados
    val annotatedText = remember(currentText) {
        buildAnnotatedString {
            // Si el texto contiene números de versículo al inicio de segmentos
            val segments = currentText.split(Regex("(?=\\d+ )"))
            segments.forEachIndexed { _, segment ->
                val match = Regex("^(\\d+) (.+)").find(segment.trim())
                if (match != null) {
                    val num = match.groupValues[1]
                    val txt = match.groupValues[2]
                    withStyle(SpanStyle(
                        color = BiblionGoldPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )) {
                        append(num)
                    }
                    append(" ")
                    append(txt)
                } else {
                    append(segment)
                }
            }
        }
    }

    LaunchedEffect(showVersionDialog) {
        if (showVersionDialog) {
            availableVersions = BibleRepository.getAvailableVersions(context)
            selectedVersions = block.comparedVersions.toSet()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(VerseBgColor)
            .padding(16.dp),
    ) {
        Text(
            text = "${block.bookId} ${block.chapter}:${block.verseStart}${if (block.verseEnd != block.verseStart) "-${block.verseEnd}" else ""} (${block.sourceVersion})",
            style = MaterialTheme.typography.labelMedium,
            color = VerseRefColor,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
        )
        Spacer(Modifier.height(8.dp))

        Text(
            text = annotatedText,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = FontFamily.Serif,
                lineHeight = 24.sp,
                color = Color(0xFF0F172A),
            ),
        )

        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = { showVersionDialog = true }) {
                Text("Comparar versiones", style = MaterialTheme.typography.labelSmall, color = VerseRefColor)
            }
        }

        // Panel de comparación — solo versiones adicionales, con texto completo del rango
        if (expanded && comparedTexts.isNotEmpty()) {
            HorizontalDivider(color = VerseBorderColor, thickness = 1.dp)
            Spacer(Modifier.height(8.dp))

            val addicionalVersions = comparedTexts.filterKeys { it != block.sourceVersion }
            if (addicionalVersions.isEmpty()) {
                Text(
                    "No hay otras versiones disponibles.",
                    style = MaterialTheme.typography.bodySmall,
                    color = VerseRefColor.copy(alpha = 0.6f),
                )
            } else {
                addicionalVersions.forEach { (version, text) ->
                    val label = availableVersions.firstOrNull { it.key == version }?.label ?: version.uppercase()
                    Column {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = VerseRefColor,
                            ),
                        )
                        Spacer(Modifier.height(2.dp))
                        val comparedAnnotated = remember(text) {
                            buildAnnotatedString {
                                val segments = text.split(Regex("(?=\\d+)"))
                                segments.forEach { seg ->
                                    val match = Regex("^(\\d+) (.+)$").find(seg.trim())
                                    if (match != null) {
                                        withStyle(SpanStyle(color = BiblionGoldPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)) {
                                            append(match.groupValues[1])
                                        }
                                        append(" ${match.groupValues[2]}")
                                    } else {
                                        append(seg)
                                    }
                                }
                            }
                        }
                        Text(
                            text = comparedAnnotated,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Serif,
                                lineHeight = 22.sp,
                                color = Color(0xFF1E293B),
                            ),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }

    // Diálogo de selección de versiones
    if (showVersionDialog) {
        VersionSelectorDialog(
            availableVersions = availableVersions,
            selectedVersions = selectedVersions,
            sourceVersion = block.sourceVersion,
            onDismiss = { showVersionDialog = false },
            onConfirm = { chosen ->
                selectedVersions = chosen
                showVersionDialog = false
                scope.launch {
                    val results = mutableMapOf<String, String>()
                    results[block.sourceVersion] = currentText
                    for (vk in chosen.filter { it != block.sourceVersion }) {
                        val fullText = buildFullVerseRangeText(context, vk, block)
                        if (fullText.isNotBlank()) results[vk] = fullText
                    }
                    comparedTexts = results
                    expanded = true
                }
            },
        )
    }
}

private suspend fun buildFullVerseRangeText(
    context: Context,
    versionKey: String,
    block: StudyBlock.Verse,
): String {
    val parts = mutableListOf<String>()
    for (v in block.verseStart..block.verseEnd) {
        val dv = BibleRepository.getVerseText(
            context, versionKey, block.bookId,
            block.chapter.toString(), v.toString()
        )
        if (dv.text.isNotBlank()) {
            parts.add("$v ${dv.text}")
        }
    }
    return parts.joinToString(" ")
}

@Composable
private fun VersionSelectorDialog(
    availableVersions: List<BibleVersionOption>,
    selectedVersions: Set<String>,
    sourceVersion: String,
    onDismiss: () -> Unit,
    onConfirm: (Set<String>) -> Unit,
) {
    var chosenVersions by remember { mutableStateOf(selectedVersions) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Seleccionar versiones") },
        text = {
            Column {
                Text(
                    "Elige las versiones para comparar:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                availableVersions.forEach { v ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            if (v.key != sourceVersion) {
                                chosenVersions = if (v.key in chosenVersions) {
                                    chosenVersions - v.key
                                } else {
                                    chosenVersions + v.key
                                }
                            }
                        }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = v.key in chosenVersions,
                            onCheckedChange = { checked ->
                                if (v.key != sourceVersion) {
                                    chosenVersions = if (checked) chosenVersions + v.key else chosenVersions - v.key
                                }
                            },
                            enabled = v.key != sourceVersion,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = v.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (v.key == sourceVersion) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onConfirm(chosenVersions.toSet()) }) { Text("Comparar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

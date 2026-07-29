package com.cristiancogollo.biblion.feature.studydocs.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cristiancogollo.biblion.BibleRepository
import com.cristiancogollo.biblion.BibleVersionOption
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import com.cristiancogollo.biblion.feature.studydocs.model.BlockAlignment
import com.cristiancogollo.biblion.feature.studydocs.model.DocConfig

private val LightVerseAccent = Color(0xFF9A6A00)
private val DarkVerseAccent = Color(0xFFE4BD55)

enum class VerseBlockMode {
    Editing,
    Reading,
}

@Composable
fun BibleVerseBlock(
    block: StudyBlock.Verse,
    modifier: Modifier = Modifier,
    mode: VerseBlockMode = VerseBlockMode.Reading,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null,
    onComparisonSelected: ((String?) -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    val accent = verseAccent()
    val comparisonVersion = block.displayedVersions().drop(1).firstOrNull()
    val hasComparison = comparisonVersion != null
    val focusRequester = remember(block.id) { FocusRequester() }
    val bodyClick = onClick?.let { select ->
        {
            if (mode == VerseBlockMode.Editing) {
                runCatching { focusRequester.requestFocus() }
            }
            select()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (mode == VerseBlockMode.Editing && onDelete != null) {
                    Modifier
                        .focusRequester(focusRequester)
                        .focusable()
                        .onPreviewKeyEvent { event ->
                            val deletesBlock =
                                event.type == KeyEventType.KeyDown &&
                                    (event.key == Key.Backspace || event.key == Key.Delete)
                            if (deletesBlock) {
                                onDelete()
                                true
                            } else {
                                false
                            }
                        }
                } else {
                    Modifier
                }
            )
            .then(
                if (mode == VerseBlockMode.Editing && isSelected) {
                    Modifier.background(accent.copy(alpha = 0.08f))
                } else {
                    Modifier
                }
            )
            .drawBehind {
                drawRect(
                    color = accent,
                    size = androidx.compose.ui.geometry.Size(2.dp.toPx(), size.height),
                )
            }
            .padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
    ) {
        VerseReferenceHeader(
            block = block,
            onReferenceClick = bodyClick,
            onComparisonSelected = onComparisonSelected,
            onDelete = onDelete.takeIf { mode == VerseBlockMode.Editing },
        )
        Spacer(Modifier.height(6.dp))
        if (hasComparison) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .then(if (bodyClick != null) Modifier.clickable(onClick = bodyClick) else Modifier),
            ) {
                VerseColumn(
                    block = block,
                    version = block.sourceVersion,
                    text = block.contents[block.sourceVersion].orEmpty(),
                    showVersion = true,
                    modifier = Modifier.weight(1f),
                )
                VerticalVerseDivider()
                VerseColumn(
                    block = block,
                    version = comparisonVersion,
                    text = block.contents[comparisonVersion].orEmpty(),
                    showVersion = true,
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            VerseColumn(
                block = block,
                version = block.sourceVersion,
                text = block.contents[block.sourceVersion].orEmpty(),
                showVersion = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (bodyClick != null) Modifier.clickable(onClick = bodyClick) else Modifier),
            )
        }
    }
}

@Composable
internal fun VerseReferenceHeader(
    block: StudyBlock.Verse,
    onReferenceClick: (() -> Unit)?,
    onComparisonSelected: ((String?) -> Unit)?,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val accent = verseAccent()
    var expanded by remember(block.id) { mutableStateOf(false) }
    var versions by remember { mutableStateOf<List<BibleVersionOption>>(emptyList()) }
    val comparisonVersion = block.displayedVersions().drop(1).firstOrNull()

    LaunchedEffect(expanded) {
        if (expanded && versions.isEmpty()) {
            versions = BibleRepository.getAvailableVersions(context)
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${block.referenceLabel()}  ${block.sourceVersion.uppercase()}",
            style = MaterialTheme.typography.labelMedium,
            color = accent,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .weight(1f)
                .then(
                    if (onReferenceClick != null) {
                        Modifier.clickable(onClick = onReferenceClick)
                    } else {
                        Modifier
                    }
                ),
        )
        if (onComparisonSelected != null) {
            Box {
                IconButton(
                    onClick = { expanded = true },
                    modifier = Modifier.size(34.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.CompareArrows,
                        contentDescription = "Comparar con otra version",
                        tint = accent,
                    )
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    if (comparisonVersion != null) {
                        DropdownMenuItem(
                            text = { Text("Sin comparacion") },
                            leadingIcon = {
                                Icon(Icons.Default.Close, contentDescription = null)
                            },
                            onClick = {
                                expanded = false
                                onComparisonSelected(null)
                            },
                        )
                        HorizontalDivider()
                    }
                    versions
                        .filter { it.key != block.sourceVersion }
                        .forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                leadingIcon = if (option.key == comparisonVersion) {
                                    {
                                        Icon(Icons.Default.Check, contentDescription = null)
                                    }
                                } else {
                                    null
                                },
                                onClick = {
                                    expanded = false
                                    onComparisonSelected(option.key)
                                },
                            )
                        }
                }
            }
        }
        if (onDelete != null) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(34.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Eliminar cita",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
internal fun VerseColumn(
    block: StudyBlock.Verse,
    version: String,
    text: String,
    showVersion: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        if (showVersion) {
            Text(
                text = version.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = verseAccent(),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        Text(
            text = text,
            style = verseBodyTextStyle(
                block = block,
                color = MaterialTheme.colorScheme.onSurface,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

internal fun verseBodyTextStyle(
    block: StudyBlock.Verse,
    color: Color,
): TextStyle {
    val fontSizeSp = block.fontSize.coerceIn(
        DocConfig.MIN_FONT_SIZE,
        DocConfig.MAX_FONT_SIZE,
    )
    return TextStyle(
        color = color,
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontStyle = FontStyle.Normal,
        fontSize = fontSizeSp.sp,
        lineHeight = (fontSizeSp * 1.55f).sp,
        textAlign = block.alignment.toVerseTextAlign(),
    )
}

private fun BlockAlignment.toVerseTextAlign(): TextAlign = when (this) {
    BlockAlignment.Start -> TextAlign.Start
    BlockAlignment.Center -> TextAlign.Center
    BlockAlignment.End -> TextAlign.End
    BlockAlignment.Justify -> TextAlign.Justify
}

@Composable
private fun VerticalVerseDivider() {
    Box(
        modifier = Modifier
            .width(20.dp)
            .fillMaxHeight(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.outlineVariant),
        )
    }
}

@Composable
internal fun verseAccent(): Color {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return if (dark) DarkVerseAccent else LightVerseAccent
}

internal suspend fun loadVerseRangeText(
    context: android.content.Context,
    version: String,
    block: StudyBlock.Verse,
): String {
    val chapter = BibleRepository.getChapter(
        context = context,
        bookName = block.bookId,
        chapterNumber = block.chapter,
        versionKey = version,
    )
    val selectedNumbers = block.selectedVerseNumbers().toSet()
    return chapter.verses
        .filter { (number, _) ->
            (number.toIntOrNull() ?: -1) in selectedNumbers
        }
        .joinToString(" ") { (number, text) -> "$number $text" }
}

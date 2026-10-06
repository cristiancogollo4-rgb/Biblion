package com.cristiancogollo.biblion.feature.reader.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cristiancogollo.biblion.BibleRepository
import com.cristiancogollo.biblion.BibleVersionOption
import com.cristiancogollo.biblion.R
import com.cristiancogollo.biblion.ui.theme.BiblionGoldPrimary
import kotlinx.coroutines.CancellationException
import java.text.Normalizer
import java.util.Locale

@Composable
internal fun PaneNavigationControls(
    paneNumber: Int,
    selection: BiblePaneSelection,
    chapterCount: Int,
    versions: List<BibleVersionOption>,
    onSelectionChange: (BiblePaneSelection) -> Unit,
) {
    var showPassagePicker by remember { mutableStateOf(false) }
    var showVersionPicker by remember { mutableStateOf(false) }
    val accent = if (paneNumber == 1) MaterialTheme.colorScheme.primary else BiblionGoldPrimary
    val versionDescription = stringResource(
        R.string.reader_parallel_version_button, selection.versionKey.uppercase(Locale.ROOT),
    )
    val passageLabel = "${selection.bookName} ${selection.chapter}"
    val passageDescription = stringResource(R.string.reader_parallel_passage_button, passageLabel)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = accent.copy(alpha = 0.13f),
            ) {
                Text(
                    text = stringResource(R.string.reader_parallel_pane, paneNumber),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(Modifier.weight(1f))
            Surface(
                onClick = { showVersionPicker = true },
                enabled = versions.isNotEmpty(),
                shape = RoundedCornerShape(9.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .widthIn(max = 132.dp)
                    .semantics {
                        contentDescription = versionDescription
                    },
            ) {
                Row(
                    modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 5.dp, bottom = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = selection.versionKey.uppercase(Locale.ROOT),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                onClick = { showPassagePicker = true },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .semantics {
                        contentDescription = passageDescription
                    },
                shape = RoundedCornerShape(11.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, accent.copy(alpha = 0.55f)),
            ) {
                Row(
                    modifier = Modifier.padding(start = 12.dp, end = 5.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = passageLabel,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = accent)
                }
            }
            IconButton(
                onClick = { onSelectionChange(selection.copy(chapter = selection.chapter - 1)) },
                enabled = selection.chapter > 1,
                modifier = Modifier.size(44.dp),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.reader_previous_chapter),
                )
            }
            IconButton(
                onClick = { onSelectionChange(selection.copy(chapter = selection.chapter + 1)) },
                enabled = selection.chapter < chapterCount,
                modifier = Modifier.size(44.dp),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = stringResource(R.string.reader_next_chapter),
                )
            }
        }
    }

    if (showPassagePicker) {
        ParallelPassagePicker(
            selection = selection,
            currentChapterCount = chapterCount,
            onDismiss = { showPassagePicker = false },
            onSelected = { book, chapter ->
                showPassagePicker = false
                onSelectionChange(selection.copy(bookName = book, chapter = chapter))
            },
        )
    }
    if (showVersionPicker) {
        ParallelVersionPicker(
            selectedVersion = selection.versionKey,
            versions = versions,
            onDismiss = { showVersionPicker = false },
            onSelected = { version ->
                showVersionPicker = false
                onSelectionChange(selection.copy(versionKey = version))
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ParallelPassagePicker(
    selection: BiblePaneSelection,
    currentChapterCount: Int,
    onDismiss: () -> Unit,
    onSelected: (String, Int) -> Unit,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var choosingBook by remember { mutableStateOf(false) }
    var selectedBook by remember { mutableStateOf(selection.bookName) }
    var query by remember { mutableStateOf("") }
    var chapters by remember(selectedBook) {
        mutableIntStateOf(if (selectedBook == selection.bookName) currentChapterCount else 0)
    }
    var loadFailed by remember(selectedBook) { mutableStateOf(false) }
    var retryRequest by remember(selectedBook) { mutableIntStateOf(0) }

    LaunchedEffect(selectedBook, selection.versionKey, currentChapterCount, retryRequest) {
        if (selectedBook == selection.bookName && currentChapterCount > 0) {
            chapters = currentChapterCount
            return@LaunchedEffect
        }
        chapters = 0
        loadFailed = false
        try {
            chapters = BibleRepository.getChapter(
                context = context,
                bookName = selectedBook,
                chapterNumber = 1,
                versionKey = selection.versionKey,
            ).chapterCount
            loadFailed = chapters <= 0
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            loadFailed = true
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (choosingBook) {
                Text(
                    text = stringResource(R.string.reader_parallel_choose_book),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(R.string.reader_parallel_search_books)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                val matches = remember(query) {
                    val needle = query.normalizedBookSearch()
                    readerCanonicalBooks.filter { it.normalizedBookSearch().contains(needle) }
                }
                if (matches.isEmpty()) {
                    Text(stringResource(R.string.reader_parallel_no_books))
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        itemsIndexed(matches, key = { _, book -> book }) { _, book ->
                            Surface(
                                onClick = {
                                    focusManager.clearFocus()
                                    selectedBook = book
                                    choosingBook = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                                color = if (book == selectedBook) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                            ) {
                                Text(
                                    text = book,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }
                        }
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.reader_parallel_chapters_of, selectedBook),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                    )
                    Surface(
                        onClick = { choosingBook = true },
                        shape = RoundedCornerShape(9.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Text(
                            text = stringResource(R.string.reader_parallel_change_book),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.reader_parallel_choose_chapter),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                when {
                    loadFailed -> {
                        Text(stringResource(R.string.reader_parallel_chapters_error))
                        Surface(
                            onClick = { retryRequest++ },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                        ) {
                            Text(
                                text = stringResource(R.string.action_retry),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            )
                        }
                    }
                    chapters <= 0 -> Box(
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator() }
                    else -> LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 54.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 8.dp),
                    ) {
                        items(chapters) { index ->
                            val chapter = index + 1
                            val isSelected = selectedBook == selection.bookName &&
                                chapter == selection.chapter
                            Surface(
                                onClick = { onSelected(selectedBook, chapter) },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                            ) {
                                Box(
                                    modifier = Modifier.height(48.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = chapter.toString(),
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ParallelVersionPicker(
    selectedVersion: String,
    versions: List<BibleVersionOption>,
    onDismiss: () -> Unit,
    onSelected: (String) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.reader_parallel_choose_version),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                itemsIndexed(versions, key = { _, version -> version.key }) { _, version ->
                    Surface(
                        onClick = { onSelected(version.key) },
                        modifier = Modifier.fillMaxWidth(),
                        color = if (version.key == selectedVersion) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = version.label,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            if (version.key == selectedVersion) {
                                Icon(Icons.Default.Check, contentDescription = null)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun String.normalizedBookSearch(): String = Normalizer
    .normalize(this, Normalizer.Form.NFD)
    .replace(Regex("\\p{M}+"), "")
    .lowercase(Locale.ROOT)

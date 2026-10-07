package com.cristiancogollo.biblion.feature.reader.ui

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
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.text.font.FontFamily
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
    onReadingOptionsClick: () -> Unit,
) {
    var showPassagePicker by remember { mutableStateOf(false) }
    var passagePickerStartsWithBooks by remember { mutableStateOf(false) }
    var showVersionMenu by remember { mutableStateOf(false) }
    val accent = if (paneNumber == 1) MaterialTheme.colorScheme.primary else BiblionGoldPrimary
    val versionDescription = stringResource(
        R.string.reader_parallel_version_button, selection.versionKey.uppercase(Locale.ROOT),
    )
    val bookDescription = stringResource(R.string.reader_parallel_book_button, selection.bookName)
    val chapterDescription = stringResource(R.string.reader_parallel_chapter_button, selection.chapter)

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box {
                Surface(
                    onClick = { showVersionMenu = true },
                    enabled = versions.isNotEmpty(),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .semantics { contentDescription = versionDescription },
                ) {
                    Row(
                        modifier = Modifier.padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = selection.versionKey.uppercase(Locale.ROOT),
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                    }
                }
                DropdownMenu(
                    expanded = showVersionMenu,
                    onDismissRequest = { showVersionMenu = false },
                    modifier = Modifier.widthIn(min = 180.dp, max = 260.dp).heightIn(max = 400.dp),
                ) {
                    versions.forEach { version ->
                        DropdownMenuItem(
                            text = { Text(version.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            leadingIcon = if (version.key == selection.versionKey) {
                                { Icon(Icons.Default.Check, contentDescription = null, tint = accent) }
                            } else {
                                null
                            },
                            onClick = {
                                showVersionMenu = false
                                onSelectionChange(selection.copy(versionKey = version.key))
                            },
                        )
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onReadingOptionsClick, modifier = Modifier.size(48.dp)) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.reader_settings_title),
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                Text(
                    text = stringResource(R.string.reader_parallel_pane, paneNumber),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = selection.bookName,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .clickable {
                                passagePickerStartsWithBooks = true
                                showPassagePicker = true
                            }
                            .semantics { contentDescription = bookDescription }
                            .padding(top = 8.dp, bottom = 8.dp),
                        style = MaterialTheme.typography.headlineSmall,
                        fontFamily = FontFamily.Serif,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = selection.chapter.toString(),
                        modifier = Modifier
                            .widthIn(min = 48.dp)
                            .heightIn(min = 48.dp)
                            .clickable {
                                passagePickerStartsWithBooks = false
                                showPassagePicker = true
                            }
                            .semantics { contentDescription = chapterDescription }
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.headlineSmall,
                        fontFamily = FontFamily.Serif,
                        maxLines = 1,
                    )
                }
            }
            IconButton(
                onClick = { onSelectionChange(selection.copy(chapter = selection.chapter - 1)) },
                enabled = selection.chapter > 1,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    Icons.Default.ChevronLeft,
                    contentDescription = stringResource(R.string.reader_previous_chapter),
                )
            }
            IconButton(
                onClick = { onSelectionChange(selection.copy(chapter = selection.chapter + 1)) },
                enabled = selection.chapter < chapterCount,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = stringResource(R.string.reader_next_chapter),
                )
            }
        }
    }

    if (showPassagePicker) {
        ParallelPassagePicker(
            selection = selection,
            currentChapterCount = chapterCount,
            initiallyChoosingBook = passagePickerStartsWithBooks,
            onDismiss = { showPassagePicker = false },
            onSelected = { book, chapter ->
                showPassagePicker = false
                onSelectionChange(selection.copy(bookName = book, chapter = chapter))
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ParallelPassagePicker(
    selection: BiblePaneSelection,
    currentChapterCount: Int,
    initiallyChoosingBook: Boolean,
    onDismiss: () -> Unit,
    onSelected: (String, Int) -> Unit,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var choosingBook by remember { mutableStateOf(initiallyChoosingBook) }
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

private fun String.normalizedBookSearch(): String = Normalizer
    .normalize(this, Normalizer.Form.NFD)
    .replace(Regex("\\p{M}+"), "")
    .lowercase(Locale.ROOT)

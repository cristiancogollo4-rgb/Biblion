package com.cristiancogollo.biblion.feature.studydocs.ui.read

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cristiancogollo.biblion.BibleRepository
import com.cristiancogollo.biblion.BibleVersionOption
import com.cristiancogollo.biblion.ChapterContent
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import com.cristiancogollo.biblion.feature.studydocs.ui.editor.verseAccent

internal data class StudyBibleTarget(
    val bookName: String,
    val chapter: Int,
    val verseNumbers: Set<Int> = emptySet(),
    val preferredVersion: String? = null,
) {
    fun matches(bookName: String, chapter: Int): Boolean =
        this.chapter == chapter && this.bookName.equals(bookName, ignoreCase = true)
}

internal fun StudyBlock.Verse.toStudyBibleTarget(): StudyBibleTarget = StudyBibleTarget(
    bookName = bookId,
    chapter = chapter,
    verseNumbers = selectedVerseNumbers().toSet(),
    preferredVersion = sourceVersion,
)

internal val studyBibleBooks = listOf(
    "Genesis", "Exodo", "Levitico", "Numeros", "Deuteronomio", "Josue", "Jueces", "Rut",
    "1 Samuel", "2 Samuel", "1 Reyes", "2 Reyes", "1 Cronicas", "2 Cronicas", "Esdras",
    "Nehemias", "Ester", "Job", "Salmos", "Proverbios", "Eclesiastes", "Cantares",
    "Isaias", "Jeremias", "Lamentaciones", "Ezequiel", "Daniel", "Oseas", "Joel", "Amos",
    "Abdias", "Jonas", "Miqueas", "Nahum", "Habacuc", "Sofonias", "Hageo", "Zacarias",
    "Malaquias", "Mateo", "Marcos", "Lucas", "Juan", "Hechos", "Romanos", "1 Corintios",
    "2 Corintios", "Galatas", "Efesios", "Filipenses", "Colosenses", "1 Tesalonicenses",
    "2 Tesalonicenses", "1 Timoteo", "2 Timoteo", "Tito", "Filemon", "Hebreos",
    "Santiago", "1 Pedro", "2 Pedro", "1 Juan", "2 Juan", "3 Juan", "Judas", "Apocalipsis",
)

@Composable
internal fun StudyBibleDialog(
    initialTarget: StudyBibleTarget?,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    var versions by remember { mutableStateOf<List<BibleVersionOption>>(emptyList()) }
    var selectedVersion by remember(initialTarget) {
        mutableStateOf(
            initialTarget?.preferredVersion ?: BibleRepository.getSelectedVersionKey(context),
        )
    }
    var selectedBook by remember(initialTarget) {
        mutableStateOf(initialTarget?.bookName ?: "Genesis")
    }
    var selectedChapter by remember(initialTarget) {
        mutableIntStateOf(initialTarget?.chapter?.coerceAtLeast(1) ?: 1)
    }
    var chapterContent by remember { mutableStateOf<ChapterContent?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        versions = BibleRepository.getAvailableVersions(context)
        selectedVersion = versions
            .firstOrNull { it.key.equals(selectedVersion, ignoreCase = true) }
            ?.key
            ?: versions.firstOrNull()?.key
            ?: selectedVersion
    }

    LaunchedEffect(selectedVersion, selectedBook, selectedChapter) {
        loading = true
        val loaded = BibleRepository.getChapter(
            context = context,
            bookName = selectedBook,
            chapterNumber = selectedChapter,
            versionKey = selectedVersion,
        )
        val validChapter = selectedChapter.coerceIn(1, loaded.chapterCount.coerceAtLeast(1))
        if (validChapter != selectedChapter) {
            selectedChapter = validChapter
            return@LaunchedEffect
        }
        chapterContent = loaded
        loading = false

        val targetVerse = initialTarget
            ?.takeIf { it.matches(selectedBook, selectedChapter) }
            ?.verseNumbers
            ?.minOrNull()
        if (targetVerse != null) {
            val targetIndex = loaded.verses.indexOfFirst { (number, _) ->
                number.toIntOrNull() == targetVerse
            }
            if (targetIndex >= 0) {
                listState.scrollToItem(targetIndex)
            }
        } else {
            listState.scrollToItem(0)
        }
    }

    val highlightedVerses = initialTarget
        ?.takeIf { it.matches(selectedBook, selectedChapter) }
        ?.verseNumbers
        .orEmpty()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.92f)
                .widthIn(max = 820.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            tonalElevation = 6.dp,
        ) {
            Column {
                BibleDialogHeader(
                    bookName = selectedBook,
                    chapter = selectedChapter,
                    onDismiss = onDismiss,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                BibleNavigationControls(
                    books = studyBibleBooks,
                    selectedBook = selectedBook,
                    onBookSelected = {
                        selectedBook = it
                        selectedChapter = 1
                    },
                    chapter = selectedChapter,
                    chapterCount = chapterContent?.chapterCount ?: 1,
                    onChapterSelected = { selectedChapter = it },
                    versions = versions,
                    selectedVersion = selectedVersion,
                    onVersionSelected = { selectedVersion = it },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                if (loading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                    ) {
                        items(
                            items = chapterContent?.verses.orEmpty(),
                            key = { it.first },
                        ) { (number, text) ->
                            val verseNumber = number.toIntOrNull() ?: -1
                            chapterContent?.titlesByVerse?.get(number)?.let { title ->
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(
                                        start = 36.dp,
                                        top = 18.dp,
                                        bottom = 4.dp,
                                    ),
                                )
                            }
                            BibleReaderVerse(
                                number = number,
                                text = text,
                                highlighted = verseNumber in highlightedVerses,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BibleDialogHeader(
    bookName: String,
    chapter: Int,
    onDismiss: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, top = 10.dp, end = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Biblia",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "$bookName $chapter",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
        IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Cerrar Biblia")
        }
    }
}

@Composable
private fun BibleNavigationControls(
    books: List<String>,
    selectedBook: String,
    onBookSelected: (String) -> Unit,
    chapter: Int,
    chapterCount: Int,
    onChapterSelected: (Int) -> Unit,
    versions: List<BibleVersionOption>,
    selectedVersion: String,
    onVersionSelected: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SelectionMenu(
                label = selectedBook,
                options = books,
                selected = selectedBook,
                onSelected = onBookSelected,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = { onChapterSelected(chapter - 1) },
                enabled = chapter > 1,
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Capitulo anterior")
            }
            SelectionMenu(
                label = "Cap. $chapter",
                options = (1..chapterCount.coerceAtLeast(1)).map(Int::toString),
                selected = chapter.toString(),
                onSelected = { onChapterSelected(it.toInt()) },
                modifier = Modifier.width(112.dp),
            )
            IconButton(
                onClick = { onChapterSelected(chapter + 1) },
                enabled = chapter < chapterCount,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Capitulo siguiente",
                )
            }
        }
        SelectionMenu(
            label = versions.firstOrNull { it.key == selectedVersion }?.label
                ?: selectedVersion.uppercase(),
            options = versions.map { it.key },
            optionLabel = { key -> versions.firstOrNull { it.key == key }?.label ?: key.uppercase() },
            selected = selectedVersion,
            onSelected = onVersionSelected,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SelectionMenu(
    label: String,
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    optionLabel: (String) -> String = { it },
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Surface(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(4.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Row(
                modifier = Modifier.padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                Icon(Icons.Default.ExpandMore, contentDescription = null)
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 360.dp),
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    leadingIcon = if (option == selected) {
                        { Icon(Icons.Default.Check, contentDescription = null) }
                    } else {
                        null
                    },
                    onClick = {
                        expanded = false
                        onSelected(option)
                    },
                )
            }
        }
    }
}

@Composable
private fun BibleReaderVerse(
    number: String,
    text: String,
    highlighted: Boolean,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (highlighted) {
            verseAccent().copy(alpha = 0.14f)
        } else {
            Color.Transparent
        },
        shape = RoundedCornerShape(4.dp),
    ) {
        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp)) {
            Text(
                text = number,
                style = MaterialTheme.typography.labelSmall,
                color = verseAccent(),
                modifier = Modifier.width(28.dp),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = FontFamily.Serif,
                    lineHeight = 25.sp,
                ),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

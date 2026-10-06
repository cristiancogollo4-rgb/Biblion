package com.cristiancogollo.biblion.feature.reader.ui

import android.content.ClipData
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.cristiancogollo.biblion.AppPreferencesSyncStore
import com.cristiancogollo.biblion.BibleRepository
import com.cristiancogollo.biblion.BibleVersionOption
import com.cristiancogollo.biblion.ChapterContent
import com.cristiancogollo.biblion.CitationVerseGroup
import com.cristiancogollo.biblion.R
import com.cristiancogollo.biblion.VerseAction
import com.cristiancogollo.biblion.VerseActionsFloatingMenu
import com.cristiancogollo.biblion.VerseItem
import com.cristiancogollo.biblion.buildCitationVerseGroups
import com.cristiancogollo.biblion.buildVerseCopyText
import com.cristiancogollo.biblion.feature.reader.HighlightsCache
import com.cristiancogollo.biblion.feature.bibi.engine.CrossReferenceVoteEngine
import com.cristiancogollo.biblion.feature.reader.ReaderPreferences
import com.cristiancogollo.biblion.feature.reader.ReaderTextLayout
import com.cristiancogollo.biblion.feature.reader.buildBibleTextSections
import com.cristiancogollo.biblion.readerHighlightPalette
import com.cristiancogollo.biblion.verseSelectionRangePosition
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Immutable
data class BiblePaneSelection(
    val bookName: String,
    val chapter: Int,
    val versionKey: String,
    val targetVerse: Int? = null,
    val targetRequest: Int = 0,
)

internal val readerCanonicalBooks = listOf(
    "Genesis", "Exodo", "Levitico", "Numeros", "Deuteronomio", "Josue", "Jueces", "Rut",
    "1 Samuel", "2 Samuel", "1 Reyes", "2 Reyes", "1 Cronicas", "2 Cronicas", "Esdras",
    "Nehemias", "Ester", "Job", "Salmos", "Proverbios", "Eclesiastes", "Cantares",
    "Isaias", "Jeremias", "Lamentaciones", "Ezequiel", "Daniel", "Oseas", "Joel", "Amos",
    "Abdias", "Jonas", "Miqueas", "Nahum", "Habacuc", "Sofonias", "Hageo", "Zacarias",
    "Malaquias", "Mateo", "Marcos", "Lucas", "Juan", "Hechos", "Romanos", "1 Corintios",
    "2 Corintios", "Galatas", "Efesios", "Filipenses", "Colosenses", "1 Tesalonicenses",
    "2 Tesalonicenses", "1 Timoteo", "2 Timoteo", "Tito", "Filemon", "Hebreos", "Santiago",
    "1 Pedro", "2 Pedro", "1 Juan", "2 Juan", "3 Juan", "Judas", "Apocalipsis",
)

@Composable
fun ParallelBibleReader(
    primarySelection: BiblePaneSelection,
    secondarySelection: BiblePaneSelection,
    versions: List<BibleVersionOption>,
    fontSize: TextUnit,
    preferences: ReaderPreferences,
    onPrimarySelectionChange: (BiblePaneSelection) -> Unit,
    onSecondarySelectionChange: (BiblePaneSelection) -> Unit,
    onInsertVerseCitation: ((CitationVerseGroup, String) -> Unit)? = null,
    onOpenCrossReferences: (ReaderVerseReference) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val highlightsCache = remember {
        HighlightsCache(maxVersions = 2, maxChaptersPerVersion = 12)
    }
    var activePane by remember { mutableStateOf<Int?>(null) }
    var selectedActions by remember { mutableStateOf<Map<String, VerseAction>>(emptyMap()) }
    var primaryHighlights by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var secondaryHighlights by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }

    LaunchedEffect(primarySelection, secondarySelection) {
        activePane = null
        selectedActions = emptyMap()
    }

    fun toggleSelection(paneNumber: Int, verseNumber: String, verseText: String) {
        if (activePane != paneNumber) {
            activePane = paneNumber
            selectedActions = mapOf(verseNumber to VerseAction(verseNumber, verseText))
        } else {
            selectedActions = if (selectedActions.containsKey(verseNumber)) {
                selectedActions - verseNumber
            } else {
                selectedActions + (verseNumber to VerseAction(verseNumber, verseText))
            }
            if (selectedActions.isEmpty()) activePane = null
        }
    }

    fun saveHighlights(colorIndex: Int) {
        val pane = activePane ?: return
        val selection = if (pane == 1) primarySelection else secondarySelection
        val currentHighlights = if (pane == 1) primaryHighlights else secondaryHighlights
        val raw = AppPreferencesSyncStore.getRawHighlights(context)
        val keyPrefix = "${selection.bookName}|${selection.chapter}|"
        val result = highlightsCache.saveHighlights(
            versionKey = selection.versionKey,
            rawHighlights = raw,
            bookName = selection.bookName,
            chapter = selection.chapter,
            verseNumbers = selectedActions.keys,
            colorIndex = colorIndex,
            verseKeyProvider = { verse -> "$keyPrefix$verse" },
            currentChapterHighlights = currentHighlights,
        )
        if (pane == 1) {
            primaryHighlights = result.updatedChapterHighlights
        } else {
            secondaryHighlights = result.updatedChapterHighlights
        }
        AppPreferencesSyncStore.updateHighlightChapter(
            context = context,
            book = selection.bookName,
            chapter = selection.chapter,
            verses = result.updatedChapterHighlights,
        )
        activePane = null
        selectedActions = emptyMap()
    }

    Box(modifier = modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize()) {
            IndependentBiblePane(
                paneNumber = 1,
                selection = primarySelection,
                versions = versions,
                fontSize = fontSize,
                preferences = preferences,
                highlightsCache = highlightsCache,
                highlights = primaryHighlights,
                selectedActions = if (activePane == 1) selectedActions else emptyMap(),
                onHighlightsLoaded = { primaryHighlights = it },
                onVerseClick = { number, text -> toggleSelection(1, number, text) },
                onOpenCrossReferences = {
                    activePane = null
                    selectedActions = emptyMap()
                    onOpenCrossReferences(it.copy(pane = 1))
                },
                onSelectionChange = onPrimarySelectionChange,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            IndependentBiblePane(
                paneNumber = 2,
                selection = secondarySelection,
                versions = versions,
                fontSize = fontSize,
                preferences = preferences,
                highlightsCache = highlightsCache,
                highlights = secondaryHighlights,
                selectedActions = if (activePane == 2) selectedActions else emptyMap(),
                onHighlightsLoaded = { secondaryHighlights = it },
                onVerseClick = { number, text -> toggleSelection(2, number, text) },
                onOpenCrossReferences = {
                    activePane = null
                    selectedActions = emptyMap()
                    onOpenCrossReferences(it.copy(pane = 2))
                },
                onSelectionChange = onSecondarySelectionChange,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }

        if (activePane != null && selectedActions.isNotEmpty()) {
            val activeSelection = if (activePane == 1) primarySelection else secondarySelection
            VerseActionsFloatingMenu(
                selectedCount = selectedActions.size,
                anchorOffset = IntOffset.Zero,
                showHighlightOptions = true,
                highlightPalette = readerHighlightPalette,
                onDismiss = {
                    activePane = null
                    selectedActions = emptyMap()
                },
                onClearSelection = {
                    activePane = null
                    selectedActions = emptyMap()
                },
                onCopy = {
                    val content = buildVerseCopyText(
                        bookName = activeSelection.bookName,
                        chapter = activeSelection.chapter,
                        selections = selectedActions.values,
                        bibleVersion = activeSelection.versionKey,
                    )
                    scope.launch {
                        clipboard.setClipEntry(
                            ClipEntry(ClipData.newPlainText("BIBLION", content))
                        )
                    }
                    activePane = null
                    selectedActions = emptyMap()
                },
                onAddCitation = null,
                onHighlight = ::saveHighlights,
                onInsertAsQuote = onInsertVerseCitation?.let { insertCitation ->
                    {
                        buildCitationVerseGroups(
                            bookName = activeSelection.bookName,
                            chapter = activeSelection.chapter,
                            selections = selectedActions.values,
                        ).forEach { group ->
                            insertCitation(group, activeSelection.versionKey)
                        }
                        activePane = null
                        selectedActions = emptyMap()
                    }
                },
            )
        }
    }
}

@Composable
private fun IndependentBiblePane(
    paneNumber: Int,
    selection: BiblePaneSelection,
    versions: List<BibleVersionOption>,
    fontSize: TextUnit,
    preferences: ReaderPreferences,
    highlightsCache: HighlightsCache,
    highlights: Map<String, Int>,
    selectedActions: Map<String, VerseAction>,
    onHighlightsLoaded: (Map<String, Int>) -> Unit,
    onVerseClick: (String, String) -> Unit,
    onOpenCrossReferences: (ReaderVerseReference) -> Unit,
    onSelectionChange: (BiblePaneSelection) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    var content by remember {
        mutableStateOf(
            ChapterContent(
                chapterCount = 0,
                verses = emptyList(),
                titlesByVerse = emptyMap(),
            )
        )
    }
    var loading by remember { mutableStateOf(true) }
    var loadFailed by remember { mutableStateOf(false) }
    var referenceVerses by remember(selection.bookName, selection.chapter) {
        mutableStateOf<Set<Int>>(emptySet())
    }

    LaunchedEffect(selection.bookName, selection.chapter, preferences.showCrossReferences) {
        referenceVerses = emptySet()
        if (!preferences.showCrossReferences) return@LaunchedEffect
        try {
            referenceVerses = CrossReferenceVoteEngine.getSourceVersesForChapter(
                context = context,
                book = selection.bookName,
                chapter = selection.chapter,
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            referenceVerses = emptySet()
        }
    }

    LaunchedEffect(selection) {
        loading = true
        loadFailed = false
        onHighlightsLoaded(emptyMap())
        content = ChapterContent(
            chapterCount = 0,
            verses = emptyList(),
            titlesByVerse = emptyMap(),
        )
        try {
            val loaded = BibleRepository.getChapter(
                context = context,
                bookName = selection.bookName,
                chapterNumber = selection.chapter,
                versionKey = selection.versionKey,
            )
            val validChapter = selection.chapter.coerceIn(
                1,
                loaded.chapterCount.coerceAtLeast(1),
            )
            if (validChapter != selection.chapter) {
                onSelectionChange(selection.copy(chapter = validChapter))
                return@LaunchedEffect
            }
            content = loaded
            val raw = AppPreferencesSyncStore.getRawHighlights(context)
            val keyPrefix = "${selection.bookName}|${selection.chapter}|"
            val loadedHighlights = withContext(Dispatchers.Default) {
                highlightsCache.loadChapterHighlights(
                    versionKey = selection.versionKey,
                    rawHighlights = raw,
                    bookName = selection.bookName,
                    chapter = selection.chapter,
                    verses = loaded.verses,
                    verseKeyProvider = { verse -> "$keyPrefix$verse" },
                    validColorIndices = readerHighlightPalette.indices,
                )
            }
            onHighlightsLoaded(loadedHighlights)
            loading = false
            listState.scrollToItem(0)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            loading = false
            loadFailed = true
        }
    }

    LaunchedEffect(selection, loading, loadFailed, content.verses, preferences.textLayout) {
        val target = selection.targetVerse?.toString() ?: return@LaunchedEffect
        if (loading || loadFailed || content.verses.isEmpty()) return@LaunchedEffect
        val targetIndex = if (preferences.textLayout == ReaderTextLayout.FLOWING) {
            buildBibleTextSections(content.verses, content.titlesByVerse)
                .indexOfFirst { section -> section.verses.any { it.first == target } }
        } else {
            content.verses.indexOfFirst { it.first == target }
        }
        if (targetIndex >= 0) listState.scrollToItem(targetIndex)
    }

    val selectedVerseNumbers = remember(selectedActions) {
        selectedActions.keys.mapNotNull { it.toIntOrNull() }.toSet()
    }
    val highlightColors = remember(highlights) {
        highlights.mapValues { (_, colorIndex) ->
            readerHighlightPalette.getOrElse(colorIndex) { Color.Transparent }
        }
    }
    val textSections = remember(content.verses, content.titlesByVerse) {
        buildBibleTextSections(content.verses, content.titlesByVerse)
    }

    Column(modifier = modifier) {
        PaneNavigationControls(
            paneNumber = paneNumber,
            selection = selection,
            chapterCount = content.chapterCount,
            versions = versions,
            onSelectionChange = { next ->
                val changingOnlyVersion = next.bookName == selection.bookName &&
                    next.chapter == selection.chapter &&
                    next.versionKey != selection.versionKey
                val visibleVerse = if (changingOnlyVersion) {
                    if (preferences.textLayout == ReaderTextLayout.FLOWING) {
                        textSections.getOrNull(listState.firstVisibleItemIndex)?.firstVerseNumber
                    } else {
                        content.verses.getOrNull(listState.firstVisibleItemIndex)?.first
                    }?.toIntOrNull()
                } else {
                    null
                }
                onSelectionChange(
                    next.copy(
                        targetVerse = visibleVerse,
                        targetRequest = selection.targetRequest + 1,
                    )
                )
            },
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            loadFailed -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.reader_parallel_load_error),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(24.dp),
                )
            }

            else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 18.dp,
                    top = 12.dp,
                    end = 18.dp,
                    bottom = 112.dp,
                ),
            ) {
                if (preferences.textLayout == ReaderTextLayout.FLOWING) {
                    itemsIndexed(
                        items = textSections,
                        key = { _, section ->
                            "pane-$paneNumber-${selection.bookName}-${selection.chapter}-section-${section.firstVerseNumber}"
                        },
                        contentType = { _, _ -> "parallel_bible_section" },
                    ) { _, section ->
                        FlowingBibleSection(
                            section = section,
                            fontSize = fontSize,
                            fontFamily = preferences.fontFamily.asComposeFontFamily(),
                            fontWeight = preferences.fontFamily.bodyWeight(),
                            lineSpacingMultiplier = preferences.lineSpacingMultiplier,
                            showVerseNumbers = preferences.showVerseNumbers,
                            showHeading = preferences.showSectionHeadings,
                            highContrast = preferences.highContrast,
                            selectedVerseNumbers = selectedActions.keys,
                            highlightColors = highlightColors,
                            onVerseClick = onVerseClick,
                        )
                    }
                } else {
                    itemsIndexed(
                        items = content.verses,
                        key = { _, verse ->
                            "pane-$paneNumber-${selection.bookName}-${selection.chapter}-${verse.first}"
                        },
                        contentType = { _, _ -> "parallel_bible_verse" },
                    ) { _, (verseNumber, verseText) ->
                        val crossReference = verseNumber.toIntOrNull()
                            ?.takeIf { it in referenceVerses }
                            ?.let { verse ->
                                ReaderVerseReference(
                                    book = selection.bookName,
                                    chapter = selection.chapter,
                                    verse = verse,
                                    text = verseText,
                                    versionKey = selection.versionKey,
                                )
                            }
                        val title = content.titlesByVerse[verseNumber]
                        if (preferences.showSectionHeadings && !title.isNullOrBlank()) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = if (preferences.highContrast) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .semantics { heading() }
                                    .padding(top = 6.dp, bottom = 10.dp),
                            )
                        }
                        VerseItem(
                            verseNumber = verseNumber,
                            verseText = verseText,
                            crossReference = crossReference,
                            onOpenCrossReferences = onOpenCrossReferences,
                            fontSize = fontSize,
                            fontFamily = preferences.fontFamily.asComposeFontFamily(),
                            fontWeight = preferences.fontFamily.bodyWeight(),
                            lineSpacingMultiplier = preferences.lineSpacingMultiplier,
                            showVerseNumber = preferences.showVerseNumbers,
                            highlightColor = readerHighlightPalette[highlights[verseNumber] ?: 0],
                            isSelected = selectedActions.containsKey(verseNumber),
                            selectionRangePosition = verseSelectionRangePosition(
                                verseNumber = verseNumber,
                                selectedVerseNumbers = selectedVerseNumbers,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            anchorSpan = null,
                            onShowActions = { onVerseClick(verseNumber, verseText) },
                            onToggleSelection = { onVerseClick(verseNumber, verseText) },
                        )
                    }
                }
            }
        }
    }
}

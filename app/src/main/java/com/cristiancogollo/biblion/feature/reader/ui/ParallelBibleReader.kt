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
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import com.cristiancogollo.biblion.feature.reader.ContinuousChapterWindow
import com.cristiancogollo.biblion.feature.reader.continuousChapterDirection
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
    onReadingOptionsClick: () -> Unit,
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
    var activeChapter by remember { mutableStateOf<Int?>(null) }
    var selectedActions by remember { mutableStateOf<Map<String, VerseAction>>(emptyMap()) }
    var primaryHighlights by remember { mutableStateOf<Map<Int, Map<String, Int>>>(emptyMap()) }
    var secondaryHighlights by remember { mutableStateOf<Map<Int, Map<String, Int>>>(emptyMap()) }

    LaunchedEffect(
        primarySelection.bookName, primarySelection.versionKey, primarySelection.targetRequest,
        secondarySelection.bookName, secondarySelection.versionKey, secondarySelection.targetRequest,
    ) {
        activePane = null
        activeChapter = null
        selectedActions = emptyMap()
    }

    LaunchedEffect(primarySelection.bookName, primarySelection.versionKey) {
        primaryHighlights = emptyMap()
    }
    LaunchedEffect(secondarySelection.bookName, secondarySelection.versionKey) {
        secondaryHighlights = emptyMap()
    }

    fun toggleSelection(paneNumber: Int, chapter: Int, verseNumber: String, verseText: String) {
        if (activePane != paneNumber || activeChapter != chapter) {
            activePane = paneNumber
            activeChapter = chapter
            selectedActions = mapOf(verseNumber to VerseAction(verseNumber, verseText))
        } else {
            selectedActions = if (selectedActions.containsKey(verseNumber)) {
                selectedActions - verseNumber
            } else {
                selectedActions + (verseNumber to VerseAction(verseNumber, verseText))
            }
            if (selectedActions.isEmpty()) {
                activePane = null
                activeChapter = null
            }
        }
    }

    fun saveHighlights(colorIndex: Int) {
        val pane = activePane ?: return
        val chapter = activeChapter ?: return
        val selection = if (pane == 1) primarySelection else secondarySelection
        val currentHighlights = (if (pane == 1) primaryHighlights else secondaryHighlights)[chapter].orEmpty()
        val raw = AppPreferencesSyncStore.getRawHighlights(context)
        val keyPrefix = "${selection.bookName}|$chapter|"
        val result = highlightsCache.saveHighlights(
            versionKey = selection.versionKey,
            rawHighlights = raw,
            bookName = selection.bookName,
            chapter = chapter,
            verseNumbers = selectedActions.keys,
            colorIndex = colorIndex,
            verseKeyProvider = { verse -> "$keyPrefix$verse" },
            currentChapterHighlights = currentHighlights,
        )
        if (pane == 1) {
            primaryHighlights = primaryHighlights + (chapter to result.updatedChapterHighlights)
        } else {
            secondaryHighlights = secondaryHighlights + (chapter to result.updatedChapterHighlights)
        }
        AppPreferencesSyncStore.updateHighlightChapter(
            context = context,
            book = selection.bookName,
            chapter = chapter,
            verses = result.updatedChapterHighlights,
        )
        activePane = null
        activeChapter = null
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
                selectedChapter = if (activePane == 1) activeChapter else null,
                onHighlightsLoaded = { chapter, values ->
                    primaryHighlights = primaryHighlights.filterKeys { it in (chapter - 2)..(chapter + 2) } +
                        (chapter to values)
                },
                onVerseClick = { chapter, number, text -> toggleSelection(1, chapter, number, text) },
                onOpenCrossReferences = {
                    activePane = null
                    activeChapter = null
                    selectedActions = emptyMap()
                    onOpenCrossReferences(it.copy(pane = 1))
                },
                onSelectionChange = onPrimarySelectionChange,
                onReadingOptionsClick = onReadingOptionsClick,
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
                selectedChapter = if (activePane == 2) activeChapter else null,
                onHighlightsLoaded = { chapter, values ->
                    secondaryHighlights = secondaryHighlights.filterKeys { it in (chapter - 2)..(chapter + 2) } +
                        (chapter to values)
                },
                onVerseClick = { chapter, number, text -> toggleSelection(2, chapter, number, text) },
                onOpenCrossReferences = {
                    activePane = null
                    activeChapter = null
                    selectedActions = emptyMap()
                    onOpenCrossReferences(it.copy(pane = 2))
                },
                onSelectionChange = onSecondarySelectionChange,
                onReadingOptionsClick = onReadingOptionsClick,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }

        if (activePane != null && selectedActions.isNotEmpty()) {
            val activeSelection = (if (activePane == 1) primarySelection else secondarySelection)
                .copy(chapter = activeChapter ?: return@Box)
            VerseActionsFloatingMenu(
                selectedCount = selectedActions.size,
                anchorOffset = IntOffset.Zero,
                showHighlightOptions = true,
                highlightPalette = readerHighlightPalette,
                onDismiss = {
                    activePane = null
                    activeChapter = null
                    selectedActions = emptyMap()
                },
                onClearSelection = {
                    activePane = null
                    activeChapter = null
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
                    activeChapter = null
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
                        activeChapter = null
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
    highlights: Map<Int, Map<String, Int>>,
    selectedActions: Map<String, VerseAction>,
    selectedChapter: Int?,
    onHighlightsLoaded: (Int, Map<String, Int>) -> Unit,
    onVerseClick: (Int, String, String) -> Unit,
    onOpenCrossReferences: (ReaderVerseReference) -> Unit,
    onSelectionChange: (BiblePaneSelection) -> Unit,
    onReadingOptionsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val emptyContent = remember {
        ChapterContent(chapterCount = 0, verses = emptyList(), titlesByVerse = emptyMap())
    }
    var content by remember {
        mutableStateOf(emptyContent)
    }
    var contentChapter by remember { mutableIntStateOf(0) }
    var previousContent by remember { mutableStateOf<ChapterContent?>(null) }
    var nextContent by remember { mutableStateOf<ChapterContent?>(null) }
    var previousLoadFailed by remember { mutableStateOf(false) }
    var nextLoadFailed by remember { mutableStateOf(false) }
    var adjacentRetryRequest by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var loadFailed by remember { mutableStateOf(false) }
    var appliedTargetRequest by remember { mutableIntStateOf(-1) }
    var referenceVersesByChapter by remember(selection.bookName) {
        mutableStateOf<Map<Int, Set<Int>>>(emptyMap())
    }

    LaunchedEffect(selection.bookName, selection.versionKey, selection.targetRequest) {
        loading = true
        loadFailed = false
        contentChapter = 0
        content = emptyContent
        previousContent = null
        nextContent = null
        previousLoadFailed = false
        nextLoadFailed = false
        appliedTargetRequest = -1
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
                onSelectionChange(selection.copy(chapter = validChapter, targetRequest = selection.targetRequest + 1))
                return@LaunchedEffect
            }
            content = loaded
            contentChapter = selection.chapter
            loading = false
            listState.scrollToItem(0)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            loading = false
            loadFailed = true
        }
    }

    LaunchedEffect(
        preferences.continuousScrolling, selection.bookName, selection.versionKey,
        selection.chapter, contentChapter, content.chapterCount, adjacentRetryRequest,
    ) {
        if (!preferences.continuousScrolling) {
            previousContent = null
            nextContent = null
            previousLoadFailed = false
            nextLoadFailed = false
            return@LaunchedEffect
        }
        if (loading || contentChapter != selection.chapter) return@LaunchedEffect
        val book = selection.bookName
        val version = selection.versionKey
        val chapter = selection.chapter
        suspend fun loadAdjacent(number: Int): ChapterContent? = try {
            BibleRepository.getChapter(context, book, number, version)
                .takeIf { it.verses.isNotEmpty() }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
        val loadedNext = if (chapter < content.chapterCount) {
            nextContent ?: loadAdjacent(chapter + 1)
        } else null
        if (selection.chapter != chapter || contentChapter != chapter) return@LaunchedEffect
        nextContent = loadedNext
        nextLoadFailed = chapter < content.chapterCount && loadedNext == null
        val loadedPrevious = if (chapter > 1) {
            previousContent ?: loadAdjacent(chapter - 1)
        } else null
        if (selection.chapter != chapter || contentChapter != chapter) return@LaunchedEffect
        previousContent = loadedPrevious
        previousLoadFailed = chapter > 1 && loadedPrevious == null
    }

    LaunchedEffect(
        preferences.continuousScrolling, selection.chapter,
        contentChapter, previousContent, content, nextContent,
    ) {
        if (!preferences.continuousScrolling || loading || contentChapter != selection.chapter) {
            return@LaunchedEffect
        }
        snapshotFlow {
            val index = listState.firstVisibleItemIndex
            listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }?.key
        }.collect { key ->
            val direction = continuousChapterDirection(
                firstVisibleItemKey = key,
                currentChapter = selection.chapter,
                hasPreviousChapter = previousContent != null,
                hasNextChapter = nextContent != null,
            ) ?: return@collect
            val shifted = ContinuousChapterWindow(
                chapter = selection.chapter,
                current = content,
                previous = previousContent,
                next = nextContent,
            ).shift(direction) ?: return@collect
            content = shifted.current
            contentChapter = shifted.chapter
            previousContent = shifted.previous
            nextContent = shifted.next
            previousLoadFailed = false
            nextLoadFailed = false
            onSelectionChange(selection.copy(chapter = shifted.chapter, targetVerse = null))
        }
    }

    LaunchedEffect(
        selection.bookName, selection.versionKey, selection.chapter,
        contentChapter, content, previousContent, nextContent,
    ) {
        if (loading || contentChapter != selection.chapter) return@LaunchedEffect
        val visible = buildList {
            previousContent?.let { add((selection.chapter - 1) to it) }
            add(selection.chapter to content)
            nextContent?.let { add((selection.chapter + 1) to it) }
        }
        val raw = AppPreferencesSyncStore.getRawHighlights(context)
        for ((chapter, chapterContent) in visible) {
            val prefix = "${selection.bookName}|$chapter|"
            val loaded = withContext(Dispatchers.Default) {
                highlightsCache.loadChapterHighlights(
                    versionKey = selection.versionKey,
                    rawHighlights = raw,
                    bookName = selection.bookName,
                    chapter = chapter,
                    verses = chapterContent.verses,
                    verseKeyProvider = { verse -> "$prefix$verse" },
                    validColorIndices = readerHighlightPalette.indices,
                )
            }
            onHighlightsLoaded(chapter, loaded)
        }
    }

    LaunchedEffect(
        selection.bookName, selection.chapter, contentChapter,
        previousContent, nextContent, preferences.showCrossReferences,
    ) {
        if (!preferences.showCrossReferences) {
            referenceVersesByChapter = emptyMap()
            return@LaunchedEffect
        }
        if (loading || contentChapter != selection.chapter) return@LaunchedEffect
        val chapters = buildList {
            if (previousContent != null) add(selection.chapter - 1)
            add(selection.chapter)
            if (nextContent != null) add(selection.chapter + 1)
        }
        referenceVersesByChapter = referenceVersesByChapter.filterKeys { it in chapters }
        for (chapter in chapters) {
            if (chapter in referenceVersesByChapter) continue
            try {
                val sources = CrossReferenceVoteEngine.getSourceVersesForChapter(
                    context = context,
                    book = selection.bookName,
                    chapter = chapter,
                )
                referenceVersesByChapter = referenceVersesByChapter + (chapter to sources)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                referenceVersesByChapter = referenceVersesByChapter + (chapter to emptySet())
            }
        }
    }

    LaunchedEffect(
        selection.targetRequest, selection.targetVerse, loading,
        contentChapter, previousContent, preferences.textLayout,
    ) {
        if (loading || loadFailed || contentChapter != selection.chapter ||
            appliedTargetRequest == selection.targetRequest
        ) return@LaunchedEffect
        val target = selection.targetVerse?.toString()
        if (target != null) {
            val index = if (preferences.textLayout == ReaderTextLayout.FLOWING) {
                buildBibleTextSections(content.verses, content.titlesByVerse)
                    .indexOfFirst { section -> section.verses.any { it.first == target } }
            } else {
                content.verses.indexOfFirst { it.first == target }
            }
            val preceding = if (preferences.continuousScrolling && previousContent != null) {
                previousContent!!.verses.size + 1
            } else 0
            if (index >= 0) listState.scrollToItem(preceding + index)
        }
        appliedTargetRequest = selection.targetRequest
    }

    val selectedVerseNumbers = remember(selectedActions) {
        selectedActions.keys.mapNotNull { it.toIntOrNull() }.toSet()
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
            onReadingOptionsClick = onReadingOptionsClick,
            onSelectionChange = { next ->
                val changingOnlyVersion = next.bookName == selection.bookName &&
                    next.chapter == selection.chapter &&
                    next.versionKey != selection.versionKey
                val visibleVerse = if (changingOnlyVersion) {
                    listState.layoutInfo.visibleItemsInfo
                        .firstOrNull { item ->
                            val key = item.key as? String
                            key?.startsWith("chapter-${selection.chapter}-verse-") == true ||
                                key?.startsWith("chapter-${selection.chapter}-section-") == true
                        }?.key?.toString()?.substringAfterLast('-')?.toIntOrNull()
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
                    bottom = if (preferences.continuousScrolling &&
                        selection.chapter < content.chapterCount
                    ) 320.dp else 112.dp,
                ),
            ) {
                val visible = buildList {
                    if (preferences.continuousScrolling) {
                        previousContent?.let { add((selection.chapter - 1) to it) }
                    }
                    add(selection.chapter to content)
                    if (preferences.continuousScrolling) {
                        nextContent?.let { add((selection.chapter + 1) to it) }
                    }
                }
                if (preferences.continuousScrolling && previousLoadFailed) {
                    item(key = "chapter-${selection.chapter - 1}-retry") {
                        TextButton(onClick = { adjacentRetryRequest++ }) {
                            Text(stringResource(R.string.action_retry))
                        }
                    }
                }
                visible.forEach { (chapter, chapterContent) ->
                    val chapterHighlights = highlights[chapter].orEmpty()
                    val chapterHighlightColors = chapterHighlights.mapValues { (_, index) ->
                        readerHighlightPalette.getOrElse(index) { Color.Transparent }
                    }
                    if (chapter != selection.chapter) {
                        item(key = "chapter-$chapter-header") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 28.dp, bottom = 18.dp)
                                    .semantics { heading() },
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                HorizontalDivider(modifier = Modifier.padding(bottom = 16.dp))
                                Text(
                                    text = "${selection.bookName} $chapter",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                    if (preferences.textLayout == ReaderTextLayout.FLOWING) {
                        val sections = if (chapter == selection.chapter) textSections else
                            buildBibleTextSections(chapterContent.verses, chapterContent.titlesByVerse)
                        itemsIndexed(
                            items = sections,
                            key = { _, section -> "chapter-$chapter-section-${section.firstVerseNumber}" },
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
                                selectedVerseNumbers = if (selectedChapter == chapter) selectedActions.keys else emptySet(),
                                highlightColors = chapterHighlightColors,
                                onVerseClick = { number, text -> onVerseClick(chapter, number, text) },
                            )
                        }
                    } else {
                        itemsIndexed(
                            items = chapterContent.verses,
                            key = { _, verse -> "chapter-$chapter-verse-${verse.first}" },
                            contentType = { _, _ -> "parallel_bible_verse" },
                        ) { _, (verseNumber, verseText) ->
                            val crossReference = verseNumber.toIntOrNull()
                                ?.takeIf { it in referenceVersesByChapter[chapter].orEmpty() }
                                ?.let { verse ->
                                    ReaderVerseReference(
                                        book = selection.bookName,
                                        chapter = chapter,
                                        verse = verse,
                                        text = verseText,
                                        versionKey = selection.versionKey,
                                    )
                                }
                            val title = chapterContent.titlesByVerse[verseNumber]
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
                                highlightColor = readerHighlightPalette[chapterHighlights[verseNumber] ?: 0],
                                isSelected = selectedChapter == chapter && selectedActions.containsKey(verseNumber),
                                selectionRangePosition = verseSelectionRangePosition(
                                    verseNumber = verseNumber,
                                    selectedVerseNumbers = if (selectedChapter == chapter) selectedVerseNumbers else emptySet(),
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                anchorSpan = null,
                                onShowActions = { onVerseClick(chapter, verseNumber, verseText) },
                                onToggleSelection = { onVerseClick(chapter, verseNumber, verseText) },
                            )
                        }
                    }
                }
                if (preferences.continuousScrolling && selection.chapter < content.chapterCount &&
                    nextContent == null
                ) {
                    item(key = "chapter-${selection.chapter + 1}-loading") {
                        if (nextLoadFailed) {
                            TextButton(onClick = { adjacentRetryRequest++ }) {
                                Text(stringResource(R.string.action_retry))
                            }
                        } else {
                            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }
        }
    }
}

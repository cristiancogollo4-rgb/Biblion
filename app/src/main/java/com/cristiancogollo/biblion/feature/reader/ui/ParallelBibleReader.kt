package com.cristiancogollo.biblion.feature.reader.ui

import android.content.ClipData
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
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
import com.cristiancogollo.biblion.feature.bibi.engine.CrossReferenceVoteEngine
import com.cristiancogollo.biblion.feature.reader.ContinuousChapterScroll
import com.cristiancogollo.biblion.feature.reader.ContinuousChapterWindow
import com.cristiancogollo.biblion.feature.reader.NEIGHBOR_CHAPTER_ALPHA
import kotlinx.coroutines.async
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.layout.Box
import kotlinx.coroutines.coroutineScope
import com.cristiancogollo.biblion.feature.reader.HighlightsCache
import com.cristiancogollo.biblion.feature.reader.ReaderPreferences
import com.cristiancogollo.biblion.feature.reader.ReaderTextLayout
import com.cristiancogollo.biblion.feature.reader.ReaderHighlightColorIndices
import com.cristiancogollo.biblion.feature.reader.readerHighlightPalette
import com.cristiancogollo.biblion.ui.theme.BiblionBluePrimary
import com.cristiancogollo.biblion.ui.theme.BiblionGoldPrimary
import com.cristiancogollo.biblion.ui.theme.BiblionGoldSoft
import com.cristiancogollo.biblion.ui.theme.BiblionThemeMode
import com.cristiancogollo.biblion.ui.theme.LocalBiblionThemeMode
import com.cristiancogollo.biblion.verseSelectionRangePosition
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

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

private const val MIN_SPLIT_FRACTION = 0.3f
private const val MAX_SPLIT_FRACTION = 0.7f
private val SideBySideMinWidth = 600.dp
private val SpineThickness = 14.dp

/**
 * Estado compartido entre los dos paneles. Se lee solo dentro de efectos para que el
 * scroll sincronizado no recomponga el lector completo en cada frame.
 */
@Stable
internal class ParallelScrollSync {
    var leaderPane by mutableIntStateOf(1)
    var anchor by mutableStateOf<ParallelScrollAnchor?>(null)
    val lastVisibleVerse = IntArray(3)
}

/** Colores de identidad de cada panel dentro de la paleta azul/dorado de Biblion. */
@Composable
internal fun parallelPaneAccent(paneNumber: Int): Color {
    val isLight = LocalBiblionThemeMode.current == BiblionThemeMode.LIGHT
    return when {
        paneNumber == 1 && isLight -> BiblionBluePrimary
        paneNumber == 1 -> Color(0xFF9DBCDD)
        isLight -> BiblionGoldPrimary
        else -> BiblionGoldSoft
    }
}

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
    val scrollSync = remember { ParallelScrollSync() }
    var isLinked by rememberSaveable { mutableStateOf(true) }
    var splitFraction by rememberSaveable { mutableFloatStateOf(0.5f) }
    var activePane by remember { mutableStateOf<Int?>(null) }
    var activeChapter by remember { mutableStateOf<Int?>(null) }
    var selectedActions by remember { mutableStateOf<Map<String, VerseAction>>(emptyMap()) }
    var primaryHighlights by remember { mutableStateOf<Map<Int, Map<String, Int>>>(emptyMap()) }
    var secondaryHighlights by remember { mutableStateOf<Map<Int, Map<String, Int>>>(emptyMap()) }

    fun clearSelection() {
        activePane = null
        activeChapter = null
        selectedActions = emptyMap()
    }

    BackHandler(enabled = activePane != null && selectedActions.isNotEmpty()) { clearSelection() }

    LaunchedEffect(
        primarySelection.bookName, primarySelection.versionKey, primarySelection.targetRequest,
        secondarySelection.bookName, secondarySelection.versionKey, secondarySelection.targetRequest,
    ) {
        clearSelection()
    }

    LaunchedEffect(primarySelection.bookName, primarySelection.versionKey) {
        primaryHighlights = emptyMap()
    }
    LaunchedEffect(secondarySelection.bookName, secondarySelection.versionKey) {
        secondaryHighlights = emptyMap()
    }

    // Los callbacks pueden llegar desde efectos lanzados en composiciones previas.
    val latestPrimary by rememberUpdatedState(primarySelection)
    val latestSecondary by rememberUpdatedState(secondarySelection)
    fun selectionOf(pane: Int) = if (pane == 1) latestPrimary else latestSecondary
    fun publish(pane: Int, selection: BiblePaneSelection) {
        if (pane == 1) onPrimarySelectionChange(selection) else onSecondarySelectionChange(selection)
    }

    // Solo los cambios explicitos (selector, flechas) incrementan targetRequest; el desplazamiento
    // continuo no lo hace y cada panel lo resuelve con su propio scroll sincronizado.
    fun changeSelection(pane: Int, next: BiblePaneSelection) {
        val current = selectionOf(pane)
        val otherPane = if (pane == 1) 2 else 1
        val other = selectionOf(otherPane)
        publish(pane, next)
        val explicitChange = next.targetRequest != current.targetRequest
        val passageDiffers = next.bookName != other.bookName || next.chapter != other.chapter
        if (isLinked && explicitChange && passageDiffers) {
            publish(
                otherPane,
                other.copy(
                    bookName = next.bookName,
                    chapter = next.chapter,
                    targetVerse = next.targetVerse,
                    targetRequest = other.targetRequest + 1,
                ),
            )
        }
    }

    fun toggleLink() {
        isLinked = !isLinked
        if (isLinked) {
            scrollSync.leaderPane = 1
            val primary = latestPrimary
            val secondary = latestSecondary
            if (primary.bookName != secondary.bookName || primary.chapter != secondary.chapter) {
                publish(
                    2,
                    secondary.copy(
                        bookName = primary.bookName,
                        chapter = primary.chapter,
                        targetVerse = scrollSync.lastVisibleVerse[1].takeIf { it > 0 },
                        targetRequest = secondary.targetRequest + 1,
                    ),
                )
            }
        }
    }

    fun swapPanes() {
        val primary = latestPrimary
        val secondary = latestSecondary
        val primaryVerse = scrollSync.lastVisibleVerse[1].takeIf { it > 0 }
        val secondaryVerse = scrollSync.lastVisibleVerse[2].takeIf { it > 0 }
        publish(1, secondary.copy(targetVerse = secondaryVerse, targetRequest = primary.targetRequest + 1))
        publish(2, primary.copy(targetVerse = primaryVerse, targetRequest = secondary.targetRequest + 1))
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
        val selection = selectionOf(pane)
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
        clearSelection()
    }

    @Composable
    fun Pane(paneNumber: Int, paneModifier: Modifier) {
        val highlights = if (paneNumber == 1) primaryHighlights else secondaryHighlights
        IndependentBiblePane(
            paneNumber = paneNumber,
            selection = selectionOf(paneNumber),
            versions = versions,
            fontSize = fontSize,
            preferences = preferences,
            highlightsCache = highlightsCache,
            highlights = highlights,
            selectedActions = if (activePane == paneNumber) selectedActions else emptyMap(),
            selectedChapter = if (activePane == paneNumber) activeChapter else null,
            isLinked = isLinked,
            scrollSync = scrollSync,
            onHighlightsLoaded = { chapter, values ->
                // Lee el estado vigente: el efecto entrega varios capitulos seguidos.
                val latest = if (paneNumber == 1) primaryHighlights else secondaryHighlights
                val updated = latest.filterKeys { it in (chapter - 2)..(chapter + 2) } + (chapter to values)
                if (paneNumber == 1) primaryHighlights = updated else secondaryHighlights = updated
            },
            onVerseClick = { chapter, number, text -> toggleSelection(paneNumber, chapter, number, text) },
            onOpenCrossReferences = {
                clearSelection()
                onOpenCrossReferences(it.copy(pane = paneNumber))
            },
            onSelectionChange = { changeSelection(paneNumber, it) },
            modifier = paneModifier,
        )
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        val sideBySide = maxWidth >= SideBySideMinWidth
        val totalSize: Dp = if (sideBySide) maxWidth else maxHeight
        val density = LocalDensity.current
        val totalPx = with(density) { (totalSize - SpineThickness).toPx() }.coerceAtLeast(1f)
        val dragState = rememberDraggableState { delta ->
            splitFraction = (splitFraction + delta / totalPx).coerceIn(MIN_SPLIT_FRACTION, MAX_SPLIT_FRACTION)
        }
        val outerPadding = 8.dp
        val spineModifier = Modifier
            .draggable(
                state = dragState,
                orientation = if (sideBySide) Orientation.Horizontal else Orientation.Vertical,
            )
            .pointerInput(Unit) { detectTapGestures(onDoubleTap = { splitFraction = 0.5f }) }

        if (sideBySide) {
            Row(modifier = Modifier.fillMaxSize().padding(outerPadding)) {
                Pane(1, Modifier.weight(splitFraction).fillMaxHeight())
                Box(
                    modifier = spineModifier.width(SpineThickness).fillMaxHeight(),
                    contentAlignment = Alignment.Center,
                ) { SpineGrip(vertical = true) }
                Pane(2, Modifier.weight(1f - splitFraction).fillMaxHeight())
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(outerPadding)) {
                Pane(1, Modifier.weight(splitFraction).fillMaxWidth())
                Box(
                    modifier = spineModifier.height(SpineThickness).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) { SpineGrip(vertical = false) }
                Pane(2, Modifier.weight(1f - splitFraction).fillMaxWidth())
            }
        }

        // Controles del lomo: flotan centrados sobre la division, siguiendo su posicion.
        val inner = totalSize - outerPadding * 2 - SpineThickness
        val spineCenter = outerPadding + inner * splitFraction + SpineThickness / 2
        SpineControls(
            sideBySide = sideBySide,
            isLinked = isLinked,
            onToggleLink = ::toggleLink,
            onSwap = ::swapPanes,
            modifier = Modifier
                .zIndex(1f)
                .then(
                    if (sideBySide) {
                        Modifier
                            .align(Alignment.CenterStart)
                            .offset { IntOffset((spineCenter - SpineControlsBreadth / 2).roundToPx(), 0) }
                    } else {
                        Modifier
                            .align(Alignment.TopCenter)
                            .offset { IntOffset(0, (spineCenter - SpineControlsBreadth / 2).roundToPx()) }
                    }
                ),
        )

        if (activePane != null && selectedActions.isNotEmpty()) {
            val activeSelection = selectionOf(activePane ?: 1)
                .copy(chapter = activeChapter ?: return@BoxWithConstraints)
            VerseActionsFloatingMenu(
                selectedCount = selectedActions.size,
                anchorOffset = IntOffset.Zero,
                showHighlightOptions = true,
                highlightPalette = readerHighlightPalette,
                onDismiss = ::clearSelection,
                onClearSelection = ::clearSelection,
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
                    clearSelection()
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
                        clearSelection()
                    }
                },
            )
        }
    }
}

private val SpineControlsBreadth = 44.dp

@Composable
private fun SpineGrip(vertical: Boolean) {
    Box(
        modifier = Modifier
            .then(if (vertical) Modifier.size(width = 4.dp, height = 36.dp) else Modifier.size(width = 36.dp, height = 4.dp))
            .background(MaterialTheme.colorScheme.outlineVariant, CircleShape),
    )
}

@Composable
private fun SpineControls(
    sideBySide: Boolean,
    isLinked: Boolean,
    onToggleLink: () -> Unit,
    onSwap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val linkDescription = stringResource(
        if (isLinked) R.string.reader_parallel_unlink else R.string.reader_parallel_link
    )
    val linkState = stringResource(
        if (isLinked) R.string.reader_parallel_linked_state else R.string.reader_parallel_unlinked_state
    )
    val swapDescription = stringResource(R.string.reader_parallel_swap)
    val linkColors = if (isLinked) {
        IconButtonDefaults.iconButtonColors(
            containerColor = BiblionGoldPrimary,
            contentColor = BiblionBluePrimary,
        )
    } else {
        IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(percent = 50),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 4.dp,
    ) {
        val buttons: @Composable () -> Unit = {
            IconButton(
                onClick = onToggleLink,
                colors = linkColors,
                modifier = Modifier
                    .padding(2.dp)
                    .size(40.dp)
                    .semantics {
                        contentDescription = linkDescription
                        stateDescription = linkState
                    },
            ) {
                Icon(
                    imageVector = if (isLinked) Icons.Default.Link else Icons.Default.LinkOff,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            }
            IconButton(
                onClick = onSwap,
                modifier = Modifier.padding(2.dp).size(40.dp),
            ) {
                Icon(
                    imageVector = if (sideBySide) Icons.Default.SwapHoriz else Icons.Default.SwapVert,
                    contentDescription = swapDescription,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        if (sideBySide) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) { buttons() }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) { buttons() }
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
    isLinked: Boolean,
    scrollSync: ParallelScrollSync,
    onHighlightsLoaded: (Int, Map<String, Int>) -> Unit,
    onVerseClick: (Int, String, String) -> Unit,
    onOpenCrossReferences: (ReaderVerseReference) -> Unit,
    onSelectionChange: (BiblePaneSelection) -> Unit,
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
    var contentBook by remember { mutableStateOf("") }
    var previousContent by remember { mutableStateOf<ChapterContent?>(null) }
    var nextContent by remember { mutableStateOf<ChapterContent?>(null) }
    var nextLoadFailed by remember { mutableStateOf(false) }
    var adjacentRetryRequest by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var loadFailed by remember { mutableStateOf(false) }
    var versionPickerRequest by remember { mutableIntStateOf(0) }
    var appliedTargetRequest by remember { mutableIntStateOf(-1) }
    var referenceVersesByChapter by remember(selection.bookName) {
        mutableStateOf<Map<Int, Set<Int>>>(emptyMap())
    }
    val accent = parallelPaneAccent(paneNumber)
    val density = LocalDensity.current
    val slideOffset = remember { Animatable(0f) }
    val latestSelection by rememberUpdatedState(selection)
    val latestOnSelectionChange by rememberUpdatedState(onSelectionChange)
    val continuous = preferences.continuousScrolling

    // Pasar de capitulo es un cambio explicito: si los paneles estan vinculados, ambos avanzan.
    fun requestChapter(chapter: Int) {
        val current = latestSelection
        latestOnSelectionChange(
            current.copy(chapter = chapter, targetVerse = null, targetRequest = current.targetRequest + 1),
        )
    }

    /** Hace activo el capitulo vecino sin recargar; el scroll ya lo muestra. */
    fun shiftWindow(direction: Int): Boolean {
        val shifted = ContinuousChapterWindow(
            chapter = contentChapter,
            current = content,
            previous = previousContent,
            next = nextContent,
        ).shift(direction) ?: return false
        content = shifted.current
        contentChapter = shifted.chapter
        previousContent = shifted.previous
        nextContent = shifted.next
        nextLoadFailed = false
        latestOnSelectionChange(latestSelection.copy(chapter = shifted.chapter, targetVerse = null))
        return true
    }

    val latestNextStartKey by rememberUpdatedState(
        when {
            !continuous || loading || contentChapter == 0 -> null
            nextContent != null -> "chapter-${contentChapter + 1}-header"
            contentChapter < content.chapterCount -> "chapter-${contentChapter + 1}-loading"
            else -> null
        },
    )
    val latestCurrentStartKey by rememberUpdatedState(
        if (continuous && !loading && previousContent != null) "chapter-$contentChapter-header" else null,
    )
    val scope = rememberCoroutineScope()
    val chapterFlingDecay = rememberSplineBasedDecay<Float>()
    val continuousScroll = remember(listState, scope, density, chapterFlingDecay) {
        ContinuousChapterScroll(
            listState = listState,
            scope = scope,
            nextStartKey = { latestNextStartKey },
            currentStartKey = { latestCurrentStartKey },
            canCommit = { direction -> if (direction > 0) nextContent != null else previousContent != null },
            onCommitted = { direction -> shiftWindow(direction) },
            commitDistancePx = with(density) { 44.dp.toPx() },
            commitVelocityPx = with(density) { 600.dp.toPx() },
            minPullPx = with(density) { 12.dp.toPx() },
            flingDecay = chapterFlingDecay,
            reduceMotion = { preferences.reduceMotion },
        )
    }

    LaunchedEffect(selection.bookName, selection.versionKey, selection.targetRequest) {
        // Mismo libro: el texto actual permanece mientras llega el nuevo, sin spinner.
        val keepVisible = contentChapter > 0 && contentBook == selection.bookName
        val direction = if (keepVisible) {
            (selection.chapter - contentChapter).coerceIn(-1, 1)
        } else {
            0
        }
        val distance = with(density) { 48.dp.toPx() }
        val animate = direction != 0 && !preferences.reduceMotion
        loadFailed = false
        appliedTargetRequest = -1
        if (!keepVisible) {
            loading = true
            contentChapter = 0
            content = emptyContent
            previousContent = null
            nextContent = null
        }
        try {
            val loaded = coroutineScope {
                val exit = launch {
                    if (animate) {
                        slideOffset.animateTo(-direction * distance, tween(durationMillis = 140))
                    }
                }
                val chapter = BibleRepository.getChapter(
                    context = context,
                    bookName = selection.bookName,
                    chapterNumber = selection.chapter,
                    versionKey = selection.versionKey,
                )
                exit.join()
                chapter
            }
            val validChapter = selection.chapter.coerceIn(
                1,
                loaded.chapterCount.coerceAtLeast(1),
            )
            if (validChapter != selection.chapter) {
                slideOffset.snapTo(0f)
                onSelectionChange(selection.copy(chapter = validChapter, targetRequest = selection.targetRequest + 1))
                return@LaunchedEffect
            }
            previousContent = null
            nextContent = null
            nextLoadFailed = false
            content = loaded
            contentChapter = selection.chapter
            contentBook = selection.bookName
            loading = false
            listState.scrollToItem(0)
            if (animate) {
                slideOffset.snapTo(direction * distance)
                slideOffset.animateTo(0f, tween(durationMillis = 260, easing = FastOutSlowInEasing))
            } else {
                slideOffset.snapTo(0f)
            }
            // Precarga del capitulo siguiente para que el proximo paso sea inmediato.
            if (selection.chapter < loaded.chapterCount) {
                try {
                    BibleRepository.getChapter(
                        context, selection.bookName, selection.chapter + 1, selection.versionKey,
                    )
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            slideOffset.snapTo(0f)
            loading = false
            loadFailed = true
        }
    }

    LaunchedEffect(
        continuous, selection.bookName, selection.versionKey,
        contentChapter, content.chapterCount, loading, adjacentRetryRequest,
    ) {
        if (!continuous || loading || contentChapter == 0) {
            if (!continuous) {
                previousContent = null
                nextContent = null
                nextLoadFailed = false
            }
            return@LaunchedEffect
        }
        val book = selection.bookName
        val version = selection.versionKey
        val chapter = contentChapter
        val count = content.chapterCount
        suspend fun loadAdjacent(number: Int): ChapterContent? = try {
            BibleRepository.getChapter(context, book, number, version)
                .takeIf { it.verses.isNotEmpty() }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
        coroutineScope {
            val existingNext = nextContent
            val existingPrevious = previousContent
            val nextLoad = async { if (chapter < count) existingNext ?: loadAdjacent(chapter + 1) else null }
            val previousLoad = async { if (chapter > 1) existingPrevious ?: loadAdjacent(chapter - 1) else null }
            val loadedNext = nextLoad.await()
            if (contentChapter != chapter) return@coroutineScope
            nextContent = loadedNext
            nextLoadFailed = chapter < count && loadedNext == null
            val loadedPrevious = previousLoad.await()
            if (contentChapter != chapter) return@coroutineScope
            previousContent = loadedPrevious
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
                    validColorIndices = ReaderHighlightColorIndices,
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

    val slots = remember(
        contentChapter, content, previousContent, nextContent,
        continuous, preferences.textLayout, nextLoadFailed,
    ) {
        buildParallelListSlots(
            currentChapter = contentChapter,
            current = content,
            previous = previousContent,
            next = nextContent,
            continuousScrolling = continuous,
            flowingLayout = preferences.textLayout == ReaderTextLayout.FLOWING,
            nextLoadFailed = nextLoadFailed,
        )
    }

    LaunchedEffect(
        selection.targetRequest, selection.targetVerse, loading,
        contentChapter, slots,
    ) {
        if (loading || loadFailed || contentChapter != selection.chapter ||
            appliedTargetRequest == selection.targetRequest
        ) return@LaunchedEffect
        selection.targetVerse?.let { target ->
            val index = slots.indexOfVerse(selection.chapter, target)
            if (index >= 0) listState.scrollToItem(index)
        }
        appliedTargetRequest = selection.targetRequest
    }

    // Informa el versiculo visible; si este panel lidera, lo publica para el panel vinculado.
    LaunchedEffect(slots, isLinked, loading) {
        if (loading) return@LaunchedEffect
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                val slot = slots.getOrNull(index) ?: return@collect
                val verse = slot.verseNumbers.firstOrNull() ?: return@collect
                scrollSync.lastVisibleVerse[paneNumber] = verse
                if (!isLinked || scrollSync.leaderPane != paneNumber) return@collect
                val size = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }?.size ?: 0
                scrollSync.anchor = ParallelScrollAnchor(
                    pane = paneNumber,
                    chapter = slot.chapter,
                    verse = verse,
                    offsetFraction = if (size > 0) (offset.toFloat() / size).coerceIn(0f, 1f) else 0f,
                )
            }
    }

    // Sigue al panel lider cuando la lectura esta vinculada.
    LaunchedEffect(slots, isLinked, loading) {
        if (!isLinked || loading) return@LaunchedEffect
        snapshotFlow { scrollSync.anchor }
            .filterNotNull()
            .collect { anchor ->
                if (anchor.pane == paneNumber || scrollSync.leaderPane == paneNumber) return@collect
                // El panel lider cambio de capitulo: este panel lo sigue con su propia ventana.
                if (anchor.chapter == contentChapter + 1 || anchor.chapter == contentChapter - 1) {
                    if (shiftWindow(anchor.chapter - contentChapter)) return@collect
                }
                val index = slots.indexOfVerse(anchor.chapter, anchor.verse)
                if (index < 0) return@collect
                val visibleSize = listState.layoutInfo.visibleItemsInfo
                    .firstOrNull { it.index == index }?.size ?: 0
                listState.scrollToItem(index, (anchor.offsetFraction * visibleSize).roundToInt())
            }
    }

    val selectedVerseNumbers = remember(selectedActions) {
        selectedActions.keys.mapNotNull { it.toIntOrNull() }.toSet()
    }
    val isActive = selectedActions.isNotEmpty()

    Surface(
        modifier = modifier.pointerInput(paneNumber) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                scrollSync.leaderPane = paneNumber
            }
        },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (isActive) 1.5.dp else 1.dp,
            color = if (isActive) accent.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
        ),
        shadowElevation = 1.dp,
    ) {
        Column {
            PaneNavigationControls(
                paneNumber = paneNumber,
                selection = selection,
                chapterCount = content.chapterCount,
                versions = versions,
                versionPickerRequest = versionPickerRequest,
                onSelectionChange = { next ->
                    val changingOnlyVersion = next.bookName == selection.bookName &&
                        next.chapter == selection.chapter &&
                        next.versionKey != selection.versionKey
                    val visibleVerse = if (changingOnlyVersion) {
                        listState.layoutInfo.visibleItemsInfo
                            .asSequence()
                            .mapNotNull { slots.getOrNull(it.index) }
                            .firstOrNull { it.chapter == selection.chapter && it.verseNumbers.isNotEmpty() }
                            ?.verseNumbers?.first()
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
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))

            when {
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = accent)
                }

                loadFailed -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.reader_parallel_load_error),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(24.dp),
                    )
                }

                content.chapterCount == 0 -> Box(
                    Modifier.fillMaxSize(), contentAlignment = Alignment.Center,
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            stringResource(
                                R.string.reader_book_unavailable_in_version,
                                selection.bookName,
                                selection.versionKey.uppercase(),
                            ),
                        )
                        TextButton(onClick = { versionPickerRequest += 1 }) {
                            Text(stringResource(R.string.reader_choose_another_version))
                        }
                    }
                }

                else -> Box(Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(continuousScroll.connection)
                        .graphicsLayer {
                            val slide = slideOffset.value
                            translationY = slide - continuousScroll.pull
                            alpha = 1f - (kotlin.math.abs(slide) / 48.dp.toPx()).coerceIn(0f, 1f)
                        },
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        top = 16.dp,
                        end = 20.dp,
                        bottom = 112.dp,
                    ),
                ) {
                    val activeChapterForDimming = contentChapter + continuousScroll.committingDirection
                    slots.forEach { slot ->
                        when (slot) {
                            is ParallelListSlot.ChapterHeader -> item(
                                key = slot.key,
                                contentType = "parallel_chapter_start",
                            ) {
                                val alpha by animateFloatAsState(
                                    if (slot.chapter == activeChapterForDimming) 1f else NEIGHBOR_CHAPTER_ALPHA,
                                    label = "parallelChapterAlpha",
                                )
                                ContinuousChapterStart(
                                    bookName = selection.bookName,
                                    chapter = slot.chapter,
                                    separateFromPrevious = slot.separateFromPrevious,
                                    modifier = Modifier.graphicsLayer { this.alpha = alpha },
                                    fontFamily = preferences.fontFamily.asComposeFontFamily(),
                                )
                            }

                            is ParallelListSlot.NextLoader -> item(key = slot.key) {
                                Box(
                                    Modifier.fillMaxWidth().padding(top = 72.dp, bottom = 24.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (slot.failed) {
                                        TextButton(onClick = { adjacentRetryRequest++ }) {
                                            Text(stringResource(R.string.action_retry))
                                        }
                                    } else {
                                        CircularProgressIndicator(color = accent)
                                    }
                                }
                            }

                            is ParallelListSlot.Section -> item(
                                key = slot.key,
                                contentType = "parallel_bible_section",
                            ) {
                                val alpha by animateFloatAsState(
                                    if (slot.chapter == activeChapterForDimming) 1f else NEIGHBOR_CHAPTER_ALPHA,
                                    label = "parallelChapterAlpha",
                                )
                                val chapterHighlightColors = highlights[slot.chapter].orEmpty()
                                    .mapValues { (_, index) ->
                                        readerHighlightPalette.getOrElse(index) { Color.Transparent }
                                    }
                                FlowingBibleSection(
                                    section = slot.section,
                                    fontSize = fontSize,
                                    fontFamily = preferences.fontFamily.asComposeFontFamily(),
                                    fontWeight = preferences.fontFamily.bodyWeight(),
                                    lineSpacingMultiplier = preferences.lineSpacingMultiplier,
                                    showVerseNumbers = preferences.showVerseNumbers,
                                    showHeading = preferences.showSectionHeadings,
                                    highContrast = preferences.highContrast,
                                    selectedVerseNumbers = if (selectedChapter == slot.chapter) {
                                        selectedActions.keys
                                    } else {
                                        emptySet()
                                    },
                                    highlightColors = chapterHighlightColors,
                                    onVerseClick = { number, text -> onVerseClick(slot.chapter, number, text) },
                                    modifier = Modifier.graphicsLayer { this.alpha = alpha },
                                )
                            }

                            is ParallelListSlot.Verse -> item(
                                key = slot.key,
                                contentType = "parallel_bible_verse",
                            ) {
                                val alpha by animateFloatAsState(
                                    if (slot.chapter == activeChapterForDimming) 1f else NEIGHBOR_CHAPTER_ALPHA,
                                    label = "parallelChapterAlpha",
                                )
                                Box(Modifier.graphicsLayer { this.alpha = alpha }) {
                                ParallelVerseRow(
                                    slot = slot,
                                    selection = selection,
                                    fontSize = fontSize,
                                    preferences = preferences,
                                    chapterHighlights = highlights[slot.chapter].orEmpty(),
                                    hasCrossReference = slot.verseNumbers.firstOrNull()
                                        ?.let { it in referenceVersesByChapter[slot.chapter].orEmpty() } == true,
                                    selectedActions = selectedActions,
                                    selectedVerseNumbers = selectedVerseNumbers,
                                    isSelectionChapter = selectedChapter == slot.chapter,
                                    onVerseClick = onVerseClick,
                                    onOpenCrossReferences = onOpenCrossReferences,
                                )
                                }
                            }

                            is ParallelListSlot.ChapterEnd -> item(
                                key = slot.key,
                                contentType = "parallel_chapter_end",
                            ) {
                                ChapterEndFooter(
                                    bookName = selection.bookName,
                                    chapter = slot.chapter,
                                    chapterCount = content.chapterCount,
                                    onNextChapter = { requestChapter(slot.chapter + 1) },
                                    accent = accent,
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

@Composable
private fun ParallelVerseRow(
    slot: ParallelListSlot.Verse,
    selection: BiblePaneSelection,
    fontSize: TextUnit,
    preferences: ReaderPreferences,
    chapterHighlights: Map<String, Int>,
    hasCrossReference: Boolean,
    selectedActions: Map<String, VerseAction>,
    selectedVerseNumbers: Set<Int>,
    isSelectionChapter: Boolean,
    onVerseClick: (Int, String, String) -> Unit,
    onOpenCrossReferences: (ReaderVerseReference) -> Unit,
) {
    val crossReference = slot.verseNumbers.firstOrNull()
        ?.takeIf { hasCrossReference }
        ?.let { verse ->
            ReaderVerseReference(
                book = selection.bookName,
                chapter = slot.chapter,
                verse = verse,
                text = slot.text,
                versionKey = selection.versionKey,
            )
        }
    Column {
        if (preferences.showSectionHeadings && !slot.title.isNullOrBlank()) {
            Text(
                text = slot.title,
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
            verseNumber = slot.number,
            verseText = slot.text,
            crossReference = crossReference,
            onOpenCrossReferences = onOpenCrossReferences,
            fontSize = fontSize,
            fontFamily = preferences.fontFamily.asComposeFontFamily(),
            fontWeight = preferences.fontFamily.bodyWeight(),
            lineSpacingMultiplier = preferences.lineSpacingMultiplier,
            showVerseNumber = preferences.showVerseNumbers,
            highlightColor = readerHighlightPalette[chapterHighlights[slot.number] ?: 0],
            isSelected = isSelectionChapter && selectedActions.containsKey(slot.number),
            allowTapSelection = isSelectionChapter && selectedActions.isNotEmpty(),
            selectionRangePosition = verseSelectionRangePosition(
                verseNumber = slot.number,
                selectedVerseNumbers = if (isSelectionChapter) selectedVerseNumbers else emptySet(),
            ),
            modifier = Modifier.fillMaxWidth(),
            anchorSpan = null,
            onShowActions = { onVerseClick(slot.chapter, slot.number, slot.text) },
            onToggleSelection = { onVerseClick(slot.chapter, slot.number, slot.text) },
        )
    }
}


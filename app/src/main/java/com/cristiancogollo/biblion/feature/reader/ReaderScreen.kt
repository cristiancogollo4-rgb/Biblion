package com.cristiancogollo.biblion

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.focusable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.cristiancogollo.biblion.feature.reader.HighlightsCache
import com.cristiancogollo.biblion.feature.reader.ReaderPreferences
import com.cristiancogollo.biblion.feature.reader.ReaderPreferencesStore
import com.cristiancogollo.biblion.feature.reader.ReaderTextLayout
import com.cristiancogollo.biblion.feature.reader.buildBibleTextSections
import com.cristiancogollo.biblion.feature.reader.chapterBodyIndexForVerse
import com.cristiancogollo.biblion.feature.reader.firstVerseAtBodyIndex
import com.cristiancogollo.biblion.feature.reader.continuousChapterDirection
import com.cristiancogollo.biblion.feature.reader.ContinuousChapterWindow
import com.cristiancogollo.biblion.feature.reader.readerVerseForeground
import com.cristiancogollo.biblion.feature.reader.ui.BiblePaneSelection
import com.cristiancogollo.biblion.feature.reader.ui.FlowingBibleSection
import com.cristiancogollo.biblion.feature.reader.ui.ParallelBibleReader
import com.cristiancogollo.biblion.feature.reader.ui.ReaderSettingsSheet
import com.cristiancogollo.biblion.feature.reader.ui.ReaderCrossReferenceIcon
import com.cristiancogollo.biblion.feature.reader.ui.ReaderCrossReferencesSheet
import com.cristiancogollo.biblion.feature.reader.ui.ReaderVerseReference
import com.cristiancogollo.biblion.feature.bibi.engine.CrossReferenceVoteEngine
import com.cristiancogollo.biblion.core.ui.StudyModeLandscapeLock
import com.cristiancogollo.biblion.ui.theme.BiblionGoldPrimary
import com.cristiancogollo.biblion.ui.theme.BiblionBluePrimary
import com.cristiancogollo.biblion.ui.theme.BiblionGoldSoft
import com.cristiancogollo.biblion.ui.theme.BiblionNavy
import com.cristiancogollo.biblion.feature.bibi.ui.BibiReaderOverlay
import com.cristiancogollo.biblion.feature.bibi.model.BibiContext
import com.cristiancogollo.biblion.feature.bibi.model.BibiPassage
import com.cristiancogollo.biblion.feature.achievements.domain.AchievementEvent
import com.cristiancogollo.biblion.feature.achievements.tracking.AchievementTracker
import com.cristiancogollo.biblion.feature.bibi.ui.StudyBibiController
import com.cristiancogollo.biblion.feature.studydocs.ui.Screen as StudyDocScreen

internal val readerHighlightPalette = listOf(
    Color(0x00000000),
    Color(0xFFFFF2A8),
    Color(0xFFC8F7C5),
    Color(0xFFFFD0D0),
    Color(0xFFD8E8FF)
)

private val newTestamentBookNames = setOf(
    "mateo", "marcos", "lucas", "juan", "hechos", "romanos",
    "1 corintios", "2 corintios", "galatas", "efesios", "filipenses",
    "colosenses", "1 tesalonicenses", "2 tesalonicenses", "1 timoteo",
    "2 timoteo", "tito", "filemon", "hebreos", "santiago", "1 pedro",
    "2 pedro", "1 juan", "2 juan", "3 juan", "judas", "apocalipsis",
)

private class ReaderJobs {
    var chapterLoad: Job? = null
    var highlightLoad: Job? = null
    var adjacentPrefetch: Job? = null

    fun cancelAll() {
        chapterLoad?.cancel()
        highlightLoad?.cancel()
        adjacentPrefetch?.cancel()
    }
}

/**
 * Utility para obtener el [Activity] desde un [Context] de Compose.
 *
 * Se usa principalmente para cambios de orientación cuando se activa/desactiva modo estudio.
 */
fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

@Composable
/**
 * Pantalla contenedora del lector.
 *
 * Maneja:
 * - Modo lectura normal.
 * - Modo estudio en split (lectura + editor) cuando corresponde.
 *
 * @param navController navegación principal.
 * @param bookName libro inicial a abrir.
 * @param initialStudyMode bandera inicial para abrir directamente en modo estudio.
 */
fun ReaderScreen(
    navController: NavController,
    bookName: String?,
    initialStudyMode: Boolean = false,
    initialChapter: Int = 1,
    targetVerse: String? = null,
    initialStudyId: Long? = null,
    isDarkTheme: Boolean = false,
    onToggleDarkTheme: (Boolean) -> Unit = {},
    currentUserName: String? = null,
    guidedTutorial: GuidedTutorialProgress? = null,
    onGuidedTutorialNext: () -> Unit = {},
    onGuidedTutorialSkip: () -> Unit = {},
    onGuidedTutorialRestart: () -> Unit = {},
    onGuidedTutorialTargetAction: (String) -> Unit = {},
    onTutorialEvent: (String) -> Unit = {},
    onInsertVerseCitation: ((CitationVerseGroup, String) -> Unit)? = null,
    showBibi: Boolean = true,
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val useCompactStudyLayout =
        com.cristiancogollo.biblion.feature.studydocs.ui.editor.shouldUseCompactStudyLayout(
            widthDp = configuration.screenWidthDp,
            heightDp = configuration.screenHeightDp,
        )

    var isStudyModeEnabled by remember { mutableStateOf(initialStudyMode) }
    var isFocusMode by remember { mutableStateOf(false) }
    var compactStudyPane by rememberSaveable { mutableStateOf("document") }
    val studyBibiController = remember { StudyBibiController() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(
        isStudyModeEnabled,
        isLandscape,
        configuration.screenWidthDp,
        configuration.screenHeightDp,
        useCompactStudyLayout,
    ) {
        if (isStudyModeEnabled) {
            Log.d(
                "BIBLION_STUDY_LAYOUT",
                "landscape=$isLandscape widthDp=${configuration.screenWidthDp} " +
                    "heightDp=${configuration.screenHeightDp} compact=$useCompactStudyLayout",
            )
        }
    }

    @Suppress("UNUSED_PARAMETER")
    val deprecatedInitialStudyId = initialStudyId

    StudyModeLandscapeLock(enabled = isStudyModeEnabled)

    if (isStudyModeEnabled && isLandscape) {
        // Nuevo: Usar SplitLayoutController con ViewModel compartido
        val context = LocalContext.current
        val repository = remember {
            com.cristiancogollo.biblion.feature.studydocs.data.StudyDocRepository(
                com.cristiancogollo.biblion.feature.studydocs.data.StudyDocDatabase.getInstance(context).studyDocDao(),
                ownerUidProvider = { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid },
            )
        }
        val splitViewModel: com.cristiancogollo.biblion.feature.studydocs.domain.StudyDocSplitViewModel = viewModel(
            factory = com.cristiancogollo.biblion.feature.studydocs.domain.StudyDocSplitViewModel.Factory(repository)
        )

        DisposableEffect(Unit) {
            onDispose {
                com.cristiancogollo.biblion.feature.studydocs.data.StudyDocDatabase.resetInstance()
            }
        }

        val splitState by splitViewModel.splitState.collectAsState()

        if (useCompactStudyLayout) {
            val activePane = if (compactStudyPane == "bible") {
                com.cristiancogollo.biblion.feature.studydocs.ui.editor.CompactStudyPane.Bible
            } else {
                com.cristiancogollo.biblion.feature.studydocs.ui.editor.CompactStudyPane.Document
            }
            com.cristiancogollo.biblion.feature.studydocs.ui.editor.CompactStudyLayoutController(
                activePane = activePane,
                onPaneSelected = { pane ->
                    focusManager.clearFocus(force = true)
                    compactStudyPane = if (
                        pane == com.cristiancogollo.biblion.feature.studydocs.ui.editor.CompactStudyPane.Bible
                    ) {
                        "bible"
                    } else {
                        "document"
                    }
                },
                biblePane = {
                    androidx.compose.runtime.CompositionLocalProvider(
                        com.cristiancogollo.biblion.feature.studydocs.ui.editor.LocalSplitViewModel provides splitViewModel
                    ) {
                        StudyModeNavigation(
                            initialBook = bookName,
                            isDarkTheme = isDarkTheme,
                            onToggleDarkTheme = onToggleDarkTheme,
                            currentUserName = currentUserName,
                        )
                    }
                },
                documentPane = {
                    com.cristiancogollo.biblion.feature.studydocs.ui.editor.StudyDocEditorScreen(
                        splitViewModel = splitViewModel,
                        onBack = { navController.popBackStackOrNavigateHome() },
                        isSplitMode = false,
                        isDarkTheme = isDarkTheme,
                        onToggleDarkTheme = onToggleDarkTheme,
                        isActive = activePane ==
                            com.cristiancogollo.biblion.feature.studydocs.ui.editor.CompactStudyPane.Document,
                        currentUserName = currentUserName,
                        bibiController = studyBibiController,
                    )
                },
            )
        } else {
            com.cristiancogollo.biblion.feature.studydocs.ui.editor.SplitLayoutController(
                leftPane = {
                    if (!isFocusMode) {
                        androidx.compose.runtime.CompositionLocalProvider(
                            com.cristiancogollo.biblion.feature.studydocs.ui.editor.LocalSplitViewModel provides splitViewModel
                        ) {
                            StudyModeNavigation(
                                initialBook = bookName,
                                isDarkTheme = isDarkTheme,
                                onToggleDarkTheme = onToggleDarkTheme,
                                currentUserName = currentUserName,
                            )
                        }
                    }
                },
                rightPane = {
                    com.cristiancogollo.biblion.feature.studydocs.ui.editor.StudyDocEditorScreen(
                        splitViewModel = splitViewModel,
                        onBack = { navController.popBackStackOrNavigateHome() },
                        isSplitMode = true,
                        onFocusModeChanged = { isFocusMode = !isFocusMode },
                        isDarkTheme = isDarkTheme,
                        onToggleDarkTheme = onToggleDarkTheme,
                        currentUserName = currentUserName,
                        bibiController = studyBibiController,
                    )
                },
            )
        }
    } else {
        ReaderContent(
            navController = navController,
            bookName = bookName,
            initialChapter = initialChapter,
            targetVerse = targetVerse,
            currentUserName = currentUserName,
            guidedTutorial = guidedTutorial,
            onGuidedTutorialNext = onGuidedTutorialNext,
            onGuidedTutorialSkip = onGuidedTutorialSkip,
            onGuidedTutorialRestart = onGuidedTutorialRestart,
            onGuidedTutorialTargetAction = onGuidedTutorialTargetAction,
            onTutorialEvent = onTutorialEvent,
            onInsertVerseCitation = onInsertVerseCitation,
            showBibi = showBibi,
        )
    }
}

@Composable
/**
 * Navegación interna usada solo en el panel izquierdo cuando el modo estudio está activo.
 *
 * Provee un sub-NavHost con Biblion (Home, Books, Reader) para que el usuario
 * pueda leer un capitulo a la izquierda mientras edita una ensenanza a la derecha.
 *
 * @param initialBook libro a abrir automáticamente al iniciar la navegación dividida.
 */
private fun StudyModeNavigation(
    initialBook: String?,
    isDarkTheme: Boolean,
    onToggleDarkTheme: (Boolean) -> Unit,
    currentUserName: String?,
) {
    val splitNavController = rememberNavController()

    NavHost(navController = splitNavController, startDestination = Screen.Home.route) {
        addSharedPrimaryDestinations(
            navController = splitNavController,
            openBooksInStudyMode = true,
            isDarkTheme = isDarkTheme,
            onToggleDarkTheme = onToggleDarkTheme,
            currentUserName = currentUserName,
        )
        composable(
            route = Screen.ReaderWithBook.route,
            arguments = listOf(
                navArgument("bookName") { type = NavType.StringType },
                navArgument("studyMode") { type = NavType.BoolType; defaultValue = true },
                navArgument("chapter") { type = NavType.IntType; defaultValue = 1 },
                navArgument("verse") { type = NavType.StringType; defaultValue = "" },
                navArgument("studyId") { type = NavType.LongType; defaultValue = -1L }
            )
        ) { backStackEntry ->
            val encodedBook = backStackEntry.arguments?.getString("bookName") ?: ""
            val book = decodeArg(encodedBook).ifBlank { null }
            val initialChapter = backStackEntry.arguments?.getInt("chapter") ?: 1
            val targetVerse = decodeArg(backStackEntry.arguments?.getString("verse") ?: "").ifBlank { null }
            ReaderContent(
                navController = splitNavController,
                bookName = book,
                initialChapter = initialChapter,
                targetVerse = targetVerse,
                currentUserName = currentUserName,
                guidedTutorial = null,
                onGuidedTutorialTargetAction = {},
                showBibi = false,
            )
        }
        composable(
            route = Screen.ReaderWithoutBook.route,
            arguments = listOf(
                navArgument("studyMode") { type = NavType.BoolType; defaultValue = true },
                navArgument("chapter") { type = NavType.IntType; defaultValue = 1 },
                navArgument("verse") { type = NavType.StringType; defaultValue = "" },
                navArgument("studyId") { type = NavType.LongType; defaultValue = -1L }
            )
        ) { backStackEntry ->
            val initialChapter = backStackEntry.arguments?.getInt("chapter") ?: 1
            val targetVerse = decodeArg(backStackEntry.arguments?.getString("verse") ?: "").ifBlank { null }
            ReaderContent(
                navController = splitNavController,
                bookName = null,
                initialChapter = initialChapter,
                targetVerse = targetVerse,
                currentUserName = currentUserName,
                guidedTutorial = null,
                onGuidedTutorialTargetAction = {},
                showBibi = false,
            )
        }
    }

    LaunchedEffect(initialBook) {
        if (!initialBook.isNullOrBlank()) {
            splitNavController.navigate(
                Screen.Reader.createRoute(bookName = initialBook, studyMode = true)
            ) {
                popUpTo(Screen.Home.route)
            }
        }
    }
}

@Composable
/**
 * Panel derecho del split-screen: editor v2 con un boton de focus mode.
 *
 * El editor mantiene su propio StudyDocViewModel autocontenido (no comparte estado
 * con el sub-NavHost de la izquierda).
 */
private fun StudyDocEditorSplitContent(
    navController: NavController,
    onFocusModeChanged: () -> Unit,
) {
    val context = LocalContext.current
    val repository = remember {
        com.cristiancogollo.biblion.feature.studydocs.data.StudyDocRepository(
            com.cristiancogollo.biblion.feature.studydocs.data.StudyDocDatabase.getInstance(context).studyDocDao(),
            ownerUidProvider = { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid },
        )
    }
    val viewModel: com.cristiancogollo.biblion.feature.studydocs.domain.StudyDocViewModel = viewModel(
        factory = com.cristiancogollo.biblion.feature.studydocs.domain.StudyDocViewModel.Factory(repository)
    )
    DisposableEffect(Unit) {
        onDispose {
            com.cristiancogollo.biblion.feature.studydocs.data.StudyDocDatabase.resetInstance()
        }
    }

    com.cristiancogollo.biblion.feature.studydocs.ui.editor.StudyDocEditorScreen(
        viewModel = viewModel,
        onBack = { navController.popBackStackOrNavigateHome() },
        isSplitMode = true,
    )
}

data class VerseAction(val number: String, val text: String)

data class CitationVerseGroup(
    val reference: String,
    val text: String,
    val bookName: String,
    val chapter: Int,
    val verseStart: Int,
    val verseEnd: Int,
    val verseNumbers: List<Int> = emptyList(),
)

enum class VerseSelectionRangePosition {
    None,
    Single,
    Start,
    Middle,
    End
}

internal fun verseSelectionRangePosition(
    verseNumber: String,
    selectedVerseNumbers: Set<Int>
): VerseSelectionRangePosition {
    val number = verseNumber.toIntOrNull() ?: return VerseSelectionRangePosition.None
    if (number !in selectedVerseNumbers) return VerseSelectionRangePosition.None

    val hasPrevious = (number - 1) in selectedVerseNumbers
    val hasNext = (number + 1) in selectedVerseNumbers
    return when {
        !hasPrevious && !hasNext -> VerseSelectionRangePosition.Single
        !hasPrevious && hasNext -> VerseSelectionRangePosition.Start
        hasPrevious && hasNext -> VerseSelectionRangePosition.Middle
        else -> VerseSelectionRangePosition.End
    }
}

private fun formatCitationVerseText(number: Int, text: String): String = "$number ${text.trim()}"

private fun formatBibleVersionForCopy(versionKey: String): String =
    when (versionKey.trim().lowercase()) {
        "rv1960", "rvr1960" -> "RVR1960"
        else -> versionKey.trim().uppercase()
    }

internal fun buildVerseCopyText(
    bookName: String,
    chapter: Int,
    selections: Collection<VerseAction>,
    bibleVersion: String,
): String {
    val sortedSelections = selections
        .mapNotNull { selection ->
            val number = selection.number.toIntOrNull() ?: return@mapNotNull null
            number to selection.text.trim()
        }
        .distinctBy { it.first }
        .sortedBy { it.first }
    if (sortedSelections.isEmpty()) return ""

    val reference = "$bookName $chapter:" +
        com.cristiancogollo.biblion.feature.studydocs.model.formatVerseNumberRanges(
            sortedSelections.map { it.first }
        )
    val referenceWithVersion = "$reference · ${formatBibleVersionForCopy(bibleVersion)}"

    return if (sortedSelections.size == 1) {
        val verseText = sortedSelections.single().second
        "“$verseText”\n\n$referenceWithVersion\nCompartido desde BIBLION"
    } else {
        val versesText = sortedSelections.joinToString("\n") { (number, text) ->
            "$number $text"
        }
        "$referenceWithVersion\n\n$versesText\n\nCompartido desde BIBLION"
    }
}

internal fun buildCitationVerseGroups(
    bookName: String,
    chapter: Int,
    selections: Collection<VerseAction>
): List<CitationVerseGroup> {
    val sortedSelections = selections
        .mapNotNull { selection ->
            val number = selection.number.toIntOrNull() ?: return@mapNotNull null
            number to selection
        }
        .sortedBy { it.first }

    if (sortedSelections.isEmpty()) return emptyList()

    val verseNumbers = sortedSelections.map { it.first }.distinct()
    return listOf(
        CitationVerseGroup(
            reference = "$bookName $chapter:" +
                com.cristiancogollo.biblion.feature.studydocs.model
                    .formatVerseNumberRanges(verseNumbers),
            text = sortedSelections.joinToString(" ") { (number, selection) ->
                formatCitationVerseText(number, selection.text)
            },
            bookName = bookName,
            chapter = chapter,
            verseStart = verseNumbers.first(),
            verseEnd = verseNumbers.last(),
            verseNumbers = verseNumbers,
        )
    )
}

@Composable
/**
 * Contenido principal del lector de capítulos y versículos.
 *
 * @param navController controlador para navegación interna/externa.
 * @param bookName libro actual.
 * @param isStudyModeActive indica si está embebido en split de estudio.
 * @param viewModel estado compartido del cuaderno de estudio.
 */
fun ReaderContent(
    navController: NavController,
    bookName: String?,
    initialChapter: Int = 1,
    targetVerse: String? = null,
    currentUserName: String? = null,
    guidedTutorial: GuidedTutorialProgress? = null,
    onGuidedTutorialNext: () -> Unit = {},
    onGuidedTutorialSkip: () -> Unit = {},
    onGuidedTutorialRestart: () -> Unit = {},
    onGuidedTutorialTargetAction: (String) -> Unit = {},
    onTutorialEvent: (String) -> Unit = {},
    onInsertVerseCitation: ((CitationVerseGroup, String) -> Unit)? = null,
    showBibi: Boolean = true,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val prefs = remember {
        context.getSharedPreferences(AppPreferencesSyncStore.PREFS_NAME, Context.MODE_PRIVATE)
    }
    val clipboard = LocalClipboard.current

    var fontSizeValue by remember {
        mutableFloatStateOf(AppPreferencesSyncStore.getReaderFontSizeSp(context).toFloat())
    }
    val fontSize = fontSizeValue.sp
    var readerPreferences by remember {
        mutableStateOf(ReaderPreferencesStore.load(context))
    }
    var showReaderSettings by remember { mutableStateOf(false) }
    val readerConfiguration = LocalConfiguration.current
    val parallelReadingAvailable =
        readerConfiguration.screenWidthDp >= 600 && !bookName.isNullOrBlank()
    var isParallelReading by rememberSaveable { mutableStateOf(false) }
    val effectiveReaderPreferences = readerPreferences

    LaunchedEffect(parallelReadingAvailable) {
        if (!parallelReadingAvailable) isParallelReading = false
    }

    var selectedChapter by remember(bookName) { mutableIntStateOf(initialChapter.coerceAtLeast(1)) }
    var chapterContent by remember(bookName) {
        mutableStateOf(
            ChapterContent(
                chapterCount = 0,
                verses = emptyList(),
                titlesByVerse = emptyMap(),
            )
        )
    }
    var continuousNextChapterContent by remember(bookName) {
        mutableStateOf<ChapterContent?>(null)
    }
    var continuousPreviousChapterContent by remember(bookName) {
        mutableStateOf<ChapterContent?>(null)
    }
    var continuousNextChapterLoadFailed by remember(bookName) { mutableStateOf(false) }
    var continuousReloadRequest by remember(bookName) { mutableIntStateOf(0) }
    val chapterCount = chapterContent.chapterCount
    val verses = chapterContent.verses
    val chapterTitles = chapterContent.titlesByVerse
    val currentTextSections = remember(verses, chapterTitles) {
        buildBibleTextSections(verses, chapterTitles)
    }
    val currentBodyItemCount = verses.size
    val previousContinuousBodyItemCount =
        continuousPreviousChapterContent?.verses?.size ?: 0
    var selectedVersionKey by remember {
        mutableStateOf(BibleRepository.getSelectedVersionKey(context))
    }
    var highlightsByChapter by remember(bookName, selectedVersionKey) {
        mutableStateOf<Map<Int, Map<String, Int>>>(emptyMap())
    }
    val verseHighlights = highlightsByChapter[selectedChapter].orEmpty()
    val currentHighlightColors = remember(verseHighlights) {
        verseHighlights.mapValues { (_, colorIndex) ->
            readerHighlightPalette.getOrElse(colorIndex) { Color.Transparent }
        }
    }
    var showDialog by remember { mutableStateOf(false) }
    var showVersionDialog by remember { mutableStateOf(false) }
    var secondaryVersionKey by rememberSaveable {
        mutableStateOf(readerPreferences.secondaryVersionKey)
    }
    var parallelPrimaryBook by rememberSaveable(bookName) {
        mutableStateOf(bookName.orEmpty())
    }
    var parallelPrimaryChapter by rememberSaveable(bookName) {
        mutableIntStateOf(initialChapter.coerceAtLeast(1))
    }
    var parallelPrimaryVersion by rememberSaveable {
        mutableStateOf(selectedVersionKey)
    }
    var parallelPrimaryTargetVerse by remember { mutableStateOf<Int?>(null) }
    var parallelPrimaryTargetRequest by remember { mutableIntStateOf(0) }
    var parallelSecondaryBook by rememberSaveable {
        mutableStateOf(readerPreferences.secondaryBookName)
    }
    var parallelSecondaryChapter by rememberSaveable {
        mutableIntStateOf(readerPreferences.secondaryChapter)
    }
    var parallelSecondaryTargetVerse by remember { mutableStateOf<Int?>(null) }
    var parallelSecondaryTargetRequest by remember { mutableIntStateOf(0) }
    var availableVersions by remember { mutableStateOf<List<BibleVersionOption>>(emptyList()) }
    var selectedVerseActions by remember { mutableStateOf<Map<String, VerseAction>>(emptyMap()) }
    var showVerseActionsMenu by remember { mutableStateOf(false) }
    var requestedBibiContext by remember { mutableStateOf<BibiContext.Reader?>(null) }
    var bibiOpenRequest by remember { mutableIntStateOf(0) }
    var referenceVersesByChapter by remember(bookName) {
        mutableStateOf<Map<Int, Set<Int>>>(emptyMap())
    }
    var openCrossReference by remember { mutableStateOf<ReaderVerseReference?>(null) }
    var horizontalDrag by remember { mutableFloatStateOf(0f) }
    var pendingTargetVerse by remember(bookName, targetVerse) { mutableStateOf(targetVerse) }
    val lazyListState = rememberLazyListState()
    var pendingScrollRestoration by remember { mutableStateOf<Pair<String, Int>?>(null) }
    var pendingScrollToTop by remember { mutableStateOf(false) }
    val chapterSlideOffset = remember { Animatable(0f) }
    val readerJobs = remember { ReaderJobs() }
    var readerContainerSize by remember { mutableStateOf(IntSize.Zero) }
    val highlightsCache = remember {
        HighlightsCache(
            maxVersions = 2,
            maxChaptersPerVersion = 12,
            entryTtlMillis = 2 * 60 * 1000
        )
    }
    val guidedStep = guidedTutorial
        ?.currentStep()
        ?.takeIf { it.screenTarget == GuidedTutorialScreenTarget.READER }
    val tutorialTargetBounds = remember { mutableStateMapOf<String, Rect>() }

    fun verseKey(verseNumber: String): String = "${bookName ?: ""}|$selectedChapter|$verseNumber"

    fun toggleVerseSelection(verseNumber: String, verseText: String) {
        selectedVerseActions = if (selectedVerseActions.containsKey(verseNumber)) {
            selectedVerseActions - verseNumber
        } else {
            selectedVerseActions + (verseNumber to VerseAction(verseNumber, verseText))
        }
        showVerseActionsMenu = selectedVerseActions.isNotEmpty()
    }

    fun referenceForVerse(chapter: Int, verseNumber: String, verseText: String): ReaderVerseReference? {
        val number = verseNumber.toIntOrNull() ?: return null
        if (number !in referenceVersesByChapter[chapter].orEmpty()) return null
        return ReaderVerseReference(
            book = bookName.orEmpty(),
            chapter = chapter,
            verse = number,
            text = verseText,
            versionKey = selectedVersionKey,
        )
    }

    LaunchedEffect(
        bookName,
        selectedChapter,
        continuousPreviousChapterContent,
        continuousNextChapterContent,
        effectiveReaderPreferences.showCrossReferences,
        effectiveReaderPreferences.continuousScrolling,
        isParallelReading,
    ) {
        if (
            bookName.isNullOrBlank() ||
            !effectiveReaderPreferences.showCrossReferences ||
            isParallelReading
        ) {
            referenceVersesByChapter = emptyMap()
            return@LaunchedEffect
        }
        val chapters = buildList {
            add(selectedChapter)
            if (effectiveReaderPreferences.continuousScrolling) {
                if (continuousPreviousChapterContent != null) add(selectedChapter - 1)
                if (continuousNextChapterContent != null) add(selectedChapter + 1)
            }
        }
        referenceVersesByChapter = referenceVersesByChapter.filterKeys { it in chapters }
        for (chapter in chapters) {
            if (chapter in referenceVersesByChapter) continue
            try {
                val versesWithReferences = CrossReferenceVoteEngine.getSourceVersesForChapter(
                    context = context,
                    book = bookName,
                    chapter = chapter,
                )
                referenceVersesByChapter = referenceVersesByChapter +
                    (chapter to versesWithReferences)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.w("ReaderScreen", "No se cargaron referencias de $bookName $chapter", error)
            }
        }
    }

    fun rememberCurrentVerseScroll() {
        val previousContent = continuousPreviousChapterContent
        val currentStartIndex = if (
            effectiveReaderPreferences.continuousScrolling &&
            previousContent != null
        ) {
            previousContinuousBodyItemCount + 2
        } else {
            0
        }
        val bodyIndex = lazyListState.firstVisibleItemIndex - currentStartIndex
        val visibleVerse = firstVerseAtBodyIndex(
            index = bodyIndex.coerceAtLeast(0),
            verses = verses,
            titlesByVerse = chapterTitles,
            textLayout = effectiveReaderPreferences.textLayout,
        )?.first
        if (!visibleVerse.isNullOrBlank()) {
            val offset = if (bodyIndex < 0) 0 else lazyListState.firstVisibleItemScrollOffset
            pendingScrollRestoration = visibleVerse to offset
        }
    }

    fun loadVisibleHighlights() {
        val targetBook = bookName ?: return
        val targetChapter = selectedChapter
        val targetVersion = selectedVersionKey
        val targetCurrentContent = chapterContent
        if (targetCurrentContent.verses.isEmpty()) return
        val targetPreviousContent = continuousPreviousChapterContent
        val targetNextContent = continuousNextChapterContent
        val chapters = buildList {
            targetPreviousContent?.let { add((targetChapter - 1) to it) }
            add(targetChapter to targetCurrentContent)
            targetNextContent?.let { add((targetChapter + 1) to it) }
        }

        val raw = AppPreferencesSyncStore.getRawHighlights(context)
        readerJobs.highlightLoad?.cancel()
        readerJobs.highlightLoad = scope.launch {
            val loaded = withContext(Dispatchers.Default) {
                chapters.associate { (chapter, content) ->
                    chapter to highlightsCache.loadChapterHighlights(
                        versionKey = targetVersion,
                        rawHighlights = raw,
                        bookName = targetBook,
                        chapter = chapter,
                        verses = content.verses,
                        verseKeyProvider = { verse -> "$targetBook|$chapter|$verse" },
                        validColorIndices = readerHighlightPalette.indices,
                    )
                }
            }
            if (
                bookName == targetBook &&
                selectedVersionKey == targetVersion &&
                selectedChapter == targetChapter &&
                chapterContent === targetCurrentContent &&
                continuousPreviousChapterContent === targetPreviousContent &&
                continuousNextChapterContent === targetNextContent
            ) {
                highlightsByChapter = highlightsByChapter
                    .filterKeys { it in (targetChapter - 1)..(targetChapter + 1) } + loaded
            }
        }
    }

    fun saveHighlights(verseNumbers: Collection<String>, colorIndex: Int) {
        if (verseNumbers.isEmpty()) return
        val targetBook = bookName ?: return
        val targetChapter = selectedChapter
        val targetVersion = selectedVersionKey
        val verseKeyPrefix = "$targetBook|$targetChapter|"
        readerJobs.highlightLoad?.cancel()
        val raw = AppPreferencesSyncStore.getRawHighlights(context)
        val result = highlightsCache.saveHighlights(
            versionKey = targetVersion,
            rawHighlights = raw,
            bookName = targetBook,
            chapter = targetChapter,
            verseNumbers = verseNumbers,
            colorIndex = colorIndex,
            verseKeyProvider = { verse -> "$verseKeyPrefix$verse" },
            currentChapterHighlights = verseHighlights,
        )
        highlightsByChapter = highlightsByChapter +
            (targetChapter to result.updatedChapterHighlights)
        AppPreferencesSyncStore.updateHighlightChapter(
            context = context,
            book = targetBook,
            chapter = targetChapter,
            verses = verseNumbers.associateWith { colorIndex },
        )
        scope.launch {
            verseNumbers.forEach { verseNumber ->
                AchievementTracker.track(
                    context,
                    AchievementEvent.HighlightCreated(
                        verseKey = "$verseKeyPrefix$verseNumber",
                        colorKey = "highlight_$colorIndex",
                    ),
                )
            }
        }
    }

    fun saveHighlight(verseNumber: String, colorIndex: Int) {
        saveHighlights(listOf(verseNumber), colorIndex)
    }

    fun clearVisibleChapterWhileLoading() {
        readerJobs.highlightLoad?.cancel()
        continuousPreviousChapterContent = null
        continuousNextChapterContent = null
        continuousNextChapterLoadFailed = false
        chapterContent = chapterContent.copy(
            verses = emptyList(),
            titlesByVerse = emptyMap(),
        )
    }

    fun loadChapter(
        book: String,
        chapter: Int,
        versionKey: String = selectedVersionKey,
    ) {
        readerJobs.chapterLoad?.cancel()
        readerJobs.adjacentPrefetch?.cancel()
        readerJobs.chapterLoad = scope.launch {
            try {
                val content = BibleRepository.getChapter(
                    context = context,
                    bookName = book,
                    chapterNumber = chapter,
                    versionKey = versionKey,
                )
                if (
                    bookName == book &&
                    selectedChapter == chapter &&
                    selectedVersionKey == versionKey
                ) {
                    chapterContent = content
                    readerJobs.adjacentPrefetch = scope.launch {
                        delay(300)
                        listOf(chapter - 1, chapter + 1)
                            .filter { adjacent -> adjacent in 1..content.chapterCount }
                            .forEach { adjacent ->
                                BibleRepository.getChapter(
                                    context = context,
                                    bookName = book,
                                    chapterNumber = adjacent,
                                    versionKey = versionKey,
                                )
                            }
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                Log.e("READER", "Error loading chapter: ${e.message}")
            }
        }
    }

    fun navigateToChapterWithAnimation(targetChapter: Int, direction: Int) {
        val targetBook = bookName ?: return
        if (targetChapter == selectedChapter) return
        selectedChapter = targetChapter
        clearVisibleChapterWhileLoading()
        pendingTargetVerse = null
        pendingScrollRestoration = null
        pendingScrollToTop = true
        loadChapter(targetBook, targetChapter, selectedVersionKey)
        scope.launch {
            if (effectiveReaderPreferences.reduceMotion) {
                chapterSlideOffset.snapTo(0f)
                return@launch
            }
            val availableWidth = readerContainerSize.width
                .takeIf { it > 0 }
                ?: with(density) { 120.dp.roundToPx() }
            chapterSlideOffset.snapTo(direction * availableWidth * 0.18f)
            chapterSlideOffset.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 220)
            )
        }
    }

    LaunchedEffect(bookName) {
        bookName?.let {
            if (pendingTargetVerse.isNullOrBlank()) {
                pendingScrollToTop = true
            }
            loadChapter(it, selectedChapter, selectedVersionKey)
        }
    }

    LaunchedEffect(Unit) {
        availableVersions = BibleRepository.getAvailableVersions(context)
        selectedVersionKey = BibleRepository.getSelectedVersionKey(context)
        parallelPrimaryVersion = selectedVersionKey
        if (secondaryVersionKey.isBlank()) {
            secondaryVersionKey = (
                availableVersions.firstOrNull { it.key != selectedVersionKey }
                    ?: availableVersions.firstOrNull()
                )?.key.orEmpty()
            readerPreferences = readerPreferences.copy(
                secondaryVersionKey = secondaryVersionKey,
            )
            ReaderPreferencesStore.save(context, readerPreferences)
        }
    }

    LaunchedEffect(
        isParallelReading,
        bookName,
        selectedChapter,
        selectedVersionKey,
    ) {
        if (!isParallelReading) {
            parallelPrimaryBook = bookName.orEmpty()
            parallelPrimaryChapter = selectedChapter
            parallelPrimaryVersion = selectedVersionKey
        }
    }

    LaunchedEffect(
        effectiveReaderPreferences.continuousScrolling,
        bookName,
        selectedChapter,
        selectedVersionKey,
        chapterCount,
        isParallelReading,
        continuousReloadRequest,
    ) {
        if (
            !effectiveReaderPreferences.continuousScrolling ||
            isParallelReading ||
            bookName.isNullOrBlank() ||
            chapterCount <= 0
        ) {
            continuousPreviousChapterContent = null
            continuousNextChapterContent = null
            continuousNextChapterLoadFailed = false
            return@LaunchedEffect
        }

        val targetBook = bookName
        val targetChapter = selectedChapter
        val targetVersion = selectedVersionKey
        continuousNextChapterLoadFailed = false

        suspend fun loadAdjacent(chapter: Int): ChapterContent? = try {
            BibleRepository.getChapter(
                context = context,
                bookName = targetBook,
                chapterNumber = chapter,
                versionKey = targetVersion,
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.e("READER", "Error loading adjacent chapter $chapter", error)
            null
        }

        // Stable item keys retain the visible verse while adjacent chapters are replaced.
        // Load the next chapter first so forward reading is never held up by the previous one.
        val next = if (targetChapter < chapterCount) {
            continuousNextChapterContent ?: loadAdjacent(targetChapter + 1)
        } else {
            null
        }
        if (
            selectedChapter != targetChapter ||
            selectedVersionKey != targetVersion ||
            bookName != targetBook
        ) return@LaunchedEffect
        continuousNextChapterContent = next?.takeIf { it.verses.isNotEmpty() }
        continuousNextChapterLoadFailed =
            targetChapter < chapterCount && continuousNextChapterContent == null
        if (BuildConfig.DEBUG) {
            Log.d(
                "READER_CONTINUOUS",
                "chapter=$targetChapter next=${targetChapter + 1} " +
                    "loaded=${continuousNextChapterContent != null} failed=$continuousNextChapterLoadFailed",
            )
        }

        val previous = if (targetChapter > 1) {
            continuousPreviousChapterContent ?: loadAdjacent(targetChapter - 1)
        } else {
            null
        }
        if (
            selectedChapter != targetChapter ||
            selectedVersionKey != targetVersion ||
            bookName != targetBook
        ) return@LaunchedEffect
        continuousPreviousChapterContent = previous?.takeIf { it.verses.isNotEmpty() }
    }

    LaunchedEffect(
        effectiveReaderPreferences.continuousScrolling,
        continuousPreviousChapterContent,
        verses,
        continuousNextChapterContent,
        selectedChapter,
        isParallelReading,
    ) {
        if (
            !effectiveReaderPreferences.continuousScrolling ||
            isParallelReading
        ) return@LaunchedEffect
        snapshotFlow {
            val firstVisibleIndex = lazyListState.firstVisibleItemIndex
            lazyListState.layoutInfo.visibleItemsInfo
                .firstOrNull { it.index == firstVisibleIndex }
                ?.key
        }.collect { firstVisibleItemKey ->
            val direction = continuousChapterDirection(
                firstVisibleItemKey = firstVisibleItemKey,
                currentChapter = selectedChapter,
                hasPreviousChapter = continuousPreviousChapterContent != null,
                hasNextChapter = continuousNextChapterContent != null,
            )
            val shiftedWindow = ContinuousChapterWindow(
                chapter = selectedChapter,
                current = chapterContent,
                previous = continuousPreviousChapterContent,
                next = continuousNextChapterContent,
            ).shift(direction ?: return@collect) ?: return@collect
            readerJobs.highlightLoad?.cancel()
            selectedVerseActions = emptyMap()
            pendingScrollRestoration = null
            pendingTargetVerse = null
            pendingScrollToTop = false
            selectedChapter = shiftedWindow.chapter
            chapterContent = shiftedWindow.current
            continuousPreviousChapterContent = shiftedWindow.previous
            continuousNextChapterContent = shiftedWindow.next
            continuousNextChapterLoadFailed = false
            if (BuildConfig.DEBUG) {
                Log.d("READER_CONTINUOUS", "visible chapter=${shiftedWindow.chapter}")
            }
        }
    }

    LaunchedEffect(
        bookName,
        selectedVersionKey,
        selectedChapter,
        chapterContent,
        continuousPreviousChapterContent,
        continuousNextChapterContent,
    ) {
        if (bookName != null && verses.isNotEmpty()) {
            loadVisibleHighlights()
        }
    }

    LaunchedEffect(bookName, selectedChapter) {
        bookName?.let { AppPreferencesSyncStore.setLastReading(context, it, selectedChapter, pendingTargetVerse) }
    }

    LaunchedEffect(
        bookName,
        selectedChapter,
        verses.size,
        effectiveReaderPreferences.continuousScrolling,
        continuousPreviousChapterContent,
        effectiveReaderPreferences.textLayout,
        isParallelReading,
    ) {
        if (
            isParallelReading ||
            bookName.isNullOrBlank() ||
            verses.isEmpty()
        ) {
            return@LaunchedEffect
        }
        val currentChapterStartIndex = if (
            effectiveReaderPreferences.continuousScrolling &&
            continuousPreviousChapterContent != null
        ) {
            previousContinuousBodyItemCount + 2
        } else {
            0
        }
        val lastVisibleIndex = snapshotFlow {
            lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
        }.first { index ->
            index >= currentChapterStartIndex +
                ((currentBodyItemCount - 1).coerceAtLeast(0) * 0.7f).toInt()
        }
        if (lastVisibleIndex >= 0) {
            AchievementTracker.track(
                context,
                AchievementEvent.ChapterRead(
                    book = bookName,
                    chapter = selectedChapter,
                    testament = if (bookName.lowercase() in newTestamentBookNames) "NEW" else "OLD",
                ),
            )
        }
    }

    LaunchedEffect(selectedVersionKey) {
        readerJobs.highlightLoad?.cancel()
        highlightsCache.clearAll()
    }

    DisposableEffect(Unit) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                AppPreferencesSyncStore.KEY_FONT_SIZE -> {
                    fontSizeValue = AppPreferencesSyncStore.getReaderFontSizeSp(context).toFloat()
                }

                AppPreferencesSyncStore.KEY_VERSE_HIGHLIGHTS -> {
                    if (!isParallelReading && bookName != null && verses.isNotEmpty()) {
                        loadVisibleHighlights()
                    }
                }

                AppPreferencesSyncStore.KEY_SELECTED_BIBLE_VERSION -> {
                    val updatedVersion = AppPreferencesSyncStore.getSelectedBibleVersion(context)
                    if (updatedVersion != selectedVersionKey) {
                        rememberCurrentVerseScroll()
                        selectedVersionKey = updatedVersion
                        clearVisibleChapterWhileLoading()
                        bookName?.let {
                            loadChapter(it, selectedChapter, updatedVersion)
                        }
                    }
                }
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
            readerJobs.cancelAll()
            highlightsCache.clearAll()
        }
    }

    LaunchedEffect(
        verses,
        selectedChapter,
        bookName,
        continuousPreviousChapterContent,
        effectiveReaderPreferences.continuousScrolling,
        effectiveReaderPreferences.textLayout,
        isParallelReading,
    ) {
        if (
            isParallelReading ||
            bookName.isNullOrBlank() ||
            verses.isEmpty()
        ) {
            return@LaunchedEffect
        }

        val currentStartIndex = if (
            effectiveReaderPreferences.continuousScrolling &&
            continuousPreviousChapterContent != null
        ) {
            previousContinuousBodyItemCount + 2
        } else {
            0
        }

        pendingScrollRestoration?.let { (verse, offset) ->
            val index = chapterBodyIndexForVerse(
                verseNumber = verse,
                verses = verses,
                titlesByVerse = chapterTitles,
                textLayout = effectiveReaderPreferences.textLayout,
            )
            if (index >= 0) {
                lazyListState.scrollToItem(currentStartIndex + index, offset)
            }
            pendingScrollRestoration = null
            pendingScrollToTop = false
            return@LaunchedEffect
        }

        val target = pendingTargetVerse
        if (!target.isNullOrBlank()) {
            val index = chapterBodyIndexForVerse(
                verseNumber = target,
                verses = verses,
                titlesByVerse = chapterTitles,
                textLayout = effectiveReaderPreferences.textLayout,
            )
            if (index >= 0) {
                lazyListState.animateScrollToItem(currentStartIndex + index)
            } else {
                lazyListState.scrollToItem(currentStartIndex)
            }
            pendingTargetVerse = null
        } else if (pendingScrollToTop) {
            lazyListState.scrollToItem(currentStartIndex)
            pendingScrollToTop = false
        }
    }

    // Auto-scroll to verse 1 when highlight tutorial step becomes active
    LaunchedEffect(
        guidedStep?.targetKey,
        verses,
        continuousPreviousChapterContent,
        effectiveReaderPreferences.textLayout,
        isParallelReading,
    ) {
        val isHighlightStep = guidedStep?.targetKey == GuidedTutorialTargets.READER_FIRST_VERSE
        if (!isParallelReading && isHighlightStep && verses.isNotEmpty()) {
            Log.d("GUIDE_DEBUG", "Auto-scrolling to verse 1 for highlight step")
            val currentStartIndex = if (
                effectiveReaderPreferences.continuousScrolling &&
                continuousPreviousChapterContent != null
            ) {
                previousContinuousBodyItemCount + 2
            } else {
                0
            }
            lazyListState.animateScrollToItem(currentStartIndex)
        }
    }

    if (
        showDialog &&
        bookName != null &&
        !isParallelReading
    ) {
        BiblionSelectionDialog(
            title = "Capítulo",
            subtitle = bookName,
            itemCount = chapterCount,
            onDismiss = { showDialog = false },
            onItemSelected = {
                navigateToChapterWithAnimation(
                    targetChapter = it,
                    direction = if (it >= selectedChapter) 1 else -1
                )
                showDialog = false
                onGuidedTutorialTargetAction(
                    GuidedTutorialTargets.READER_CHAPTER_SELECTOR
                )
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                BiblionReaderTopAppBar(
                    bookName = bookName ?: "",
                    chapter = selectedChapter,
                    onNavigationIconClick = {
                        val popped = navController.popBackStackOrNavigateHome()
                        if (!popped) {
                            context.findActivity()?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        }
                    },
                    selectedVersionName = selectedVersionKey.uppercase(),
                    onVersionClick = {
                        showVersionDialog = true
                    },
                    onBookTitleClick = { showDialog = true },
                    onReadingOptionsClick = { showReaderSettings = true },
                    parallelReadingAvailable = parallelReadingAvailable,
                    isParallelReading = isParallelReading,
                    onParallelReadingClick = {
                        selectedVerseActions = emptyMap()
                        showDialog = false
                        showVersionDialog = false
                        if (isParallelReading) {
                            isParallelReading = false
                            selectedVersionKey = parallelPrimaryVersion
                            BibleRepository.setSelectedVersionKey(context, parallelPrimaryVersion)
                            if (parallelPrimaryBook == bookName) {
                                selectedChapter = parallelPrimaryChapter
                                clearVisibleChapterWhileLoading()
                                loadChapter(
                                    parallelPrimaryBook,
                                    parallelPrimaryChapter,
                                    parallelPrimaryVersion,
                                )
                            } else {
                                navController.navigate(
                                    Screen.Reader.createRoute(
                                        bookName = parallelPrimaryBook,
                                        chapter = parallelPrimaryChapter,
                                    )
                                ) {
                                    launchSingleTop = true
                                }
                            }
                        } else {
                            val currentStartIndex = if (
                                effectiveReaderPreferences.continuousScrolling &&
                                continuousPreviousChapterContent != null
                            ) {
                                previousContinuousBodyItemCount + 2
                            } else {
                                0
                            }
                            val visibleVerse = firstVerseAtBodyIndex(
                                index = (lazyListState.firstVisibleItemIndex - currentStartIndex)
                                    .coerceAtLeast(0),
                                verses = verses,
                                titlesByVerse = chapterTitles,
                                textLayout = effectiveReaderPreferences.textLayout,
                            )?.first?.toIntOrNull()
                                ?: pendingTargetVerse?.toIntOrNull()
                                ?: verses.firstOrNull()?.first?.toIntOrNull()
                            parallelPrimaryBook = bookName.orEmpty()
                            parallelPrimaryChapter = selectedChapter
                            parallelPrimaryVersion = selectedVersionKey
                            parallelPrimaryTargetVerse = visibleVerse
                            parallelPrimaryTargetRequest++
                            parallelSecondaryBook = bookName.orEmpty()
                            parallelSecondaryChapter = selectedChapter
                            secondaryVersionKey = selectedVersionKey
                            parallelSecondaryTargetVerse = visibleVerse
                            parallelSecondaryTargetRequest++
                            val updatedPreferences = readerPreferences.copy(
                                secondaryBookName = bookName.orEmpty(),
                                secondaryChapter = selectedChapter,
                                secondaryVersionKey = selectedVersionKey,
                            )
                            readerPreferences = updatedPreferences
                            ReaderPreferencesStore.save(context, updatedPreferences)
                            isParallelReading = true
                        }
                    },
                    titleOverride = if (isParallelReading) {
                        stringResource(R.string.reader_parallel_app_bar_title)
                    } else {
                        null
                    },
                    showPrimarySelectors = !isParallelReading,
                    versionModifier = Modifier.guidedTutorialTarget(
                        GuidedTutorialTargets.READER_VERSION_SELECTOR,
                        tutorialTargetBounds
                    ),
                    bookTitleModifier = Modifier.guidedTutorialTarget(
                        GuidedTutorialTargets.READER_CHAPTER_SELECTOR,
                        tutorialTargetBounds
                    )
                )
            },
            bottomBar = {
                BiblionBottomNavigation(
                    currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route,
                    onHome = { navController.navigateTopLevel(Screen.Home.route) },
                    onBible = { navController.navigateTopLevel(Screen.Books.createRoute(Testament.OLD)) },
                    onSearch = { navController.navigateTopLevel(Screen.Search.createRoute()) },
                    onStudy = { navController.navigateTopLevel(StudyDocScreen.StudyDocsList.route) },
                    onProfile = { navController.navigateTopLevel(Screen.Profile.route) },
                        onGuidedTutorialTargetAction = onGuidedTutorialTargetAction,
                        activeTutorialTargetKey = guidedStep?.targetKey,
                    )
            }
        ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .onGloballyPositioned { coordinates ->
                    readerContainerSize = coordinates.size
                }
        ) {
            val selectedVerseNumbers = remember(selectedVerseActions) {
                selectedVerseActions.keys.mapNotNull { it.toIntOrNull() }.toSet()
            }
            if (isParallelReading) {
                ParallelBibleReader(
                    primarySelection = BiblePaneSelection(
                        bookName = parallelPrimaryBook,
                        chapter = parallelPrimaryChapter,
                        versionKey = parallelPrimaryVersion,
                        targetVerse = parallelPrimaryTargetVerse,
                        targetRequest = parallelPrimaryTargetRequest,
                    ),
                    secondarySelection = BiblePaneSelection(
                        bookName = parallelSecondaryBook,
                        chapter = parallelSecondaryChapter,
                        versionKey = secondaryVersionKey,
                        targetVerse = parallelSecondaryTargetVerse,
                        targetRequest = parallelSecondaryTargetRequest,
                    ),
                    versions = availableVersions,
                    fontSize = fontSize,
                    preferences = effectiveReaderPreferences,
                    onPrimarySelectionChange = { selection ->
                        parallelPrimaryBook = selection.bookName
                        parallelPrimaryChapter = selection.chapter
                        parallelPrimaryVersion = selection.versionKey
                        parallelPrimaryTargetVerse = selection.targetVerse
                        parallelPrimaryTargetRequest = selection.targetRequest
                    },
                    onSecondarySelectionChange = { selection ->
                        parallelSecondaryBook = selection.bookName
                        parallelSecondaryChapter = selection.chapter
                        secondaryVersionKey = selection.versionKey
                        parallelSecondaryTargetVerse = selection.targetVerse
                        parallelSecondaryTargetRequest = selection.targetRequest
                        val updatedPreferences = readerPreferences.copy(
                            secondaryBookName = selection.bookName,
                            secondaryChapter = selection.chapter,
                            secondaryVersionKey = selection.versionKey,
                        )
                        readerPreferences = updatedPreferences
                        ReaderPreferencesStore.save(context, updatedPreferences)
                    },
                    onInsertVerseCitation = onInsertVerseCitation,
                    onOpenCrossReferences = { openCrossReference = it },
                )
            } else {
            Row(modifier = Modifier.fillMaxSize()) {
                val readerBottomPadding = if (
                    effectiveReaderPreferences.continuousScrolling &&
                    selectedChapter < chapterCount
                ) {
                    with(density) { readerContainerSize.height.toDp() }.coerceAtLeast(112.dp)
                } else {
                    112.dp
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter,
                ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxHeight()
                        .widthIn(max = effectiveReaderPreferences.textWidthDp.dp)
                        .fillMaxWidth()
                    .guidedTutorialTarget(
                        GuidedTutorialTargets.READER_TEXT,
                        tutorialTargetBounds
                    )
                    .graphicsLayer {
                        val width = size.width.takeIf { it > 0f } ?: 1f
                        translationX = chapterSlideOffset.value
                        alpha = 1f - (abs(chapterSlideOffset.value) / width * 0.45f)
                            .coerceIn(0f, 0.35f)
                    }
                    .pointerInput(
                        bookName,
                        selectedChapter,
                        chapterCount,
                        effectiveReaderPreferences.continuousScrolling,
                    ) {
                        if (effectiveReaderPreferences.continuousScrolling) {
                            return@pointerInput
                        }
                        detectHorizontalDragGestures(
                            onHorizontalDrag = { _, dragAmount ->
                                horizontalDrag += dragAmount
                            },
                            onDragEnd = {
                                if (bookName.isNullOrBlank() || chapterCount <= 1) {
                                    horizontalDrag = 0f
                                    return@detectHorizontalDragGestures
                                }
                                when {
                                    horizontalDrag <= -40f && selectedChapter < chapterCount -> {
                                        navigateToChapterWithAnimation(
                                            targetChapter = selectedChapter + 1,
                                            direction = 1
                                        )
                                    }

                                    horizontalDrag >= 40f && selectedChapter > 1 -> {
                                        navigateToChapterWithAnimation(
                                            targetChapter = selectedChapter - 1,
                                            direction = -1
                                        )
                                    }
                                }
                                horizontalDrag = 0f
                            }
                        )
                    },
                    state = lazyListState,
                    contentPadding = PaddingValues(
                        start = if (readerConfiguration.screenWidthDp >= 600) 24.dp else 16.dp,
                        top = 16.dp,
                        end = if (readerConfiguration.screenWidthDp >= 600) 24.dp else 16.dp,
                        bottom = readerBottomPadding,
                    ),
                ) {
                val previousContinuousContent = continuousPreviousChapterContent
                if (
                    effectiveReaderPreferences.continuousScrolling &&
                    previousContinuousContent != null
                ) {
                    item(key = "chapter-${selectedChapter - 1}-header") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp, bottom = 18.dp)
                                .semantics { heading() },
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = "${bookName.orEmpty()} ${selectedChapter - 1}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            androidx.compose.material3.HorizontalDivider(
                                modifier = Modifier.padding(top = 20.dp),
                            )
                        }
                    }
                    if (effectiveReaderPreferences.textLayout == ReaderTextLayout.FLOWING) {
                        val previousSections = buildBibleTextSections(
                            previousContinuousContent.verses,
                            previousContinuousContent.titlesByVerse,
                        )
                        itemsIndexed(
                            items = previousSections,
                            key = { _, section ->
                                "chapter-${selectedChapter - 1}-section-${section.firstVerseNumber}"
                            },
                            contentType = { _, _ -> "continuous_bible_section" },
                        ) { _, section ->
                            FlowingBibleSection(
                                section = section,
                                fontSize = fontSize,
                                fontFamily = effectiveReaderPreferences.fontFamily.asComposeFontFamily(),
                                fontWeight = effectiveReaderPreferences.fontFamily.bodyWeight(),
                                lineSpacingMultiplier = effectiveReaderPreferences.lineSpacingMultiplier,
                                showVerseNumbers = effectiveReaderPreferences.showVerseNumbers,
                                showHeading = effectiveReaderPreferences.showSectionHeadings,
                                highContrast = effectiveReaderPreferences.highContrast,
                                selectedVerseNumbers = emptySet(),
                                highlightColors = emptyMap(),
                                onVerseClick = null,
                            )
                        }
                    } else {
                        itemsIndexed(
                            items = previousContinuousContent.verses,
                            key = { _, verse ->
                                "chapter-${selectedChapter - 1}-verse-${verse.first}"
                            },
                            contentType = { _, _ -> "continuous_bible_verse" },
                        ) { _, (verseNumber, verseText) ->
                            val previousTitle =
                                previousContinuousContent.titlesByVerse[verseNumber]
                            if (
                                effectiveReaderPreferences.showSectionHeadings &&
                                !previousTitle.isNullOrBlank()
                            ) {
                                Text(
                                    text = previousTitle,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    color = if (effectiveReaderPreferences.highContrast) {
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
                            ReadOnlyVerseItem(
                                verseNumber = verseNumber,
                                verseText = verseText,
                                crossReference = referenceForVerse(
                                    selectedChapter - 1, verseNumber, verseText,
                                ),
                                onOpenCrossReferences = {
                                    selectedVerseActions = emptyMap()
                                    openCrossReference = it
                                },
                                highlightColor = readerHighlightPalette.getOrElse(
                                    highlightsByChapter[selectedChapter - 1]
                                        ?.get(verseNumber) ?: 0
                                ) { Color.Transparent },
                                fontSize = fontSize,
                                fontFamily = effectiveReaderPreferences.fontFamily.asComposeFontFamily(),
                                fontWeight = effectiveReaderPreferences.fontFamily.bodyWeight(),
                                lineSpacingMultiplier =
                                    effectiveReaderPreferences.lineSpacingMultiplier,
                                showVerseNumber = effectiveReaderPreferences.showVerseNumbers,
                            )
                        }
                    }

                    item(key = "chapter-$selectedChapter-header") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 28.dp, bottom = 18.dp)
                                .semantics { heading() },
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            androidx.compose.material3.HorizontalDivider(
                                modifier = Modifier.padding(bottom = 20.dp),
                            )
                            Text(
                                text = "${bookName.orEmpty()} $selectedChapter",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }

                if (effectiveReaderPreferences.textLayout == ReaderTextLayout.FLOWING) {
                    itemsIndexed(
                        items = currentTextSections,
                        key = { _, section ->
                            "chapter-$selectedChapter-section-${section.firstVerseNumber}"
                        },
                        contentType = { _, _ -> "bible_text_section" },
                    ) { index, section ->
                        FlowingBibleSection(
                            section = section,
                            fontSize = fontSize,
                            fontFamily = effectiveReaderPreferences.fontFamily.asComposeFontFamily(),
                            fontWeight = effectiveReaderPreferences.fontFamily.bodyWeight(),
                            lineSpacingMultiplier = effectiveReaderPreferences.lineSpacingMultiplier,
                            showVerseNumbers = effectiveReaderPreferences.showVerseNumbers,
                            showHeading = effectiveReaderPreferences.showSectionHeadings,
                            highContrast = effectiveReaderPreferences.highContrast,
                            selectedVerseNumbers = selectedVerseActions.keys,
                            highlightColors = currentHighlightColors,
                            onVerseClick = ::toggleVerseSelection,
                            modifier = if (index == 0) {
                                Modifier.guidedTutorialTarget(
                                    GuidedTutorialTargets.READER_FIRST_VERSE,
                                    tutorialTargetBounds,
                                )
                            } else {
                                Modifier
                            },
                        )
                    }
                } else {
                    itemsIndexed(
                        items = verses,
                        key = { _, verse ->
                            "chapter-$selectedChapter-verse-${verse.first}"
                        },
                        contentType = { _, _ -> "bible_verse" },
                    ) { index, (verseNumber, verseText) ->
                        val chapterTitle = chapterTitles[verseNumber]
                        if (
                            effectiveReaderPreferences.showSectionHeadings &&
                            !chapterTitle.isNullOrBlank()
                        ) {
                            Text(
                                text = chapterTitle,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (effectiveReaderPreferences.highContrast) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .semantics {
                                        heading()
                                        contentDescription = context.getString(
                                            R.string.reader_section_heading,
                                            chapterTitle,
                                        )
                                    }
                                    .padding(top = 6.dp, bottom = 10.dp)
                            )
                        }

                        Box(
                            modifier = if (index == 0) {
                                Modifier
                                    .fillMaxWidth()
                                    .guidedTutorialTarget(
                                        GuidedTutorialTargets.READER_FIRST_VERSE,
                                        tutorialTargetBounds
                                    )
                            } else {
                                Modifier.fillMaxWidth()
                            }
                        ) {
                            VerseItem(
                                verseNumber = verseNumber,
                                verseText = verseText,
                                crossReference = referenceForVerse(
                                    selectedChapter, verseNumber, verseText,
                                ),
                                onOpenCrossReferences = {
                                    selectedVerseActions = emptyMap()
                                    openCrossReference = it
                                },
                                fontSize = fontSize,
                                fontFamily = effectiveReaderPreferences.fontFamily.asComposeFontFamily(),
                                fontWeight = effectiveReaderPreferences.fontFamily.bodyWeight(),
                                lineSpacingMultiplier = effectiveReaderPreferences.lineSpacingMultiplier,
                                showVerseNumber = effectiveReaderPreferences.showVerseNumbers,
                                highlightColor = readerHighlightPalette[verseHighlights[verseNumber] ?: 0],
                                isSelected = selectedVerseActions.containsKey(verseNumber),
                                selectionRangePosition = verseSelectionRangePosition(
                                    verseNumber = verseNumber,
                                    selectedVerseNumbers = selectedVerseNumbers
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                anchorSpan = null,
                                onShowActions = {
                                    val currentStep = guidedTutorial?.currentStep()?.takeIf { it.screenTarget == GuidedTutorialScreenTarget.READER }
                                    val isGuideHighlightStep = currentStep?.targetKey == GuidedTutorialTargets.READER_FIRST_VERSE
                                    val isTargetVerse = index == 0
                                    Log.d("GUIDE_DEBUG", "onShowActions verse=$verseNumber index=$index isGuideStep=$isGuideHighlightStep isTarget=$isTargetVerse step=${currentStep?.id}")
                                    if (isGuideHighlightStep && isTargetVerse) {
                                        saveHighlight(verseNumber, 1)
                                        selectedVerseActions = emptyMap()
                                        onGuidedTutorialTargetAction(GuidedTutorialTargets.READER_FIRST_VERSE)
                                    } else {
                                        toggleVerseSelection(verseNumber, verseText)
                                    }
                                },
                                onToggleSelection = {
                                    toggleVerseSelection(verseNumber, verseText)
                                },
                            )
                        }
                    }
                }

                val nextContinuousContent = continuousNextChapterContent
                if (
                    effectiveReaderPreferences.continuousScrolling &&
                    nextContinuousContent != null
                ) {
                    item(key = "chapter-${selectedChapter + 1}-header") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 28.dp, bottom = 18.dp)
                                .semantics { heading() },
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            androidx.compose.material3.HorizontalDivider(
                                modifier = Modifier.padding(bottom = 20.dp),
                            )
                            Text(
                                text = "${bookName.orEmpty()} ${selectedChapter + 1}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                    if (effectiveReaderPreferences.textLayout == ReaderTextLayout.FLOWING) {
                        val nextSections = buildBibleTextSections(
                            nextContinuousContent.verses,
                            nextContinuousContent.titlesByVerse,
                        )
                        itemsIndexed(
                            items = nextSections,
                            key = { _, section ->
                                "chapter-${selectedChapter + 1}-section-${section.firstVerseNumber}"
                            },
                            contentType = { _, _ -> "continuous_bible_section" },
                        ) { _, section ->
                            FlowingBibleSection(
                                section = section,
                                fontSize = fontSize,
                                fontFamily = effectiveReaderPreferences.fontFamily.asComposeFontFamily(),
                                fontWeight = effectiveReaderPreferences.fontFamily.bodyWeight(),
                                lineSpacingMultiplier = effectiveReaderPreferences.lineSpacingMultiplier,
                                showVerseNumbers = effectiveReaderPreferences.showVerseNumbers,
                                showHeading = effectiveReaderPreferences.showSectionHeadings,
                                highContrast = effectiveReaderPreferences.highContrast,
                                selectedVerseNumbers = emptySet(),
                                highlightColors = emptyMap(),
                                onVerseClick = null,
                            )
                        }
                    } else {
                        itemsIndexed(
                            items = nextContinuousContent.verses,
                            key = { _, verse ->
                                "chapter-${selectedChapter + 1}-verse-${verse.first}"
                            },
                            contentType = { _, _ -> "continuous_bible_verse" },
                        ) { _, (verseNumber, verseText) ->
                            val nextTitle = nextContinuousContent.titlesByVerse[verseNumber]
                            if (
                                effectiveReaderPreferences.showSectionHeadings &&
                                !nextTitle.isNullOrBlank()
                            ) {
                                Text(
                                    text = nextTitle,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    color = if (effectiveReaderPreferences.highContrast) {
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
                            ReadOnlyVerseItem(
                                verseNumber = verseNumber,
                                verseText = verseText,
                                crossReference = referenceForVerse(
                                    selectedChapter + 1, verseNumber, verseText,
                                ),
                                onOpenCrossReferences = {
                                    selectedVerseActions = emptyMap()
                                    openCrossReference = it
                                },
                                highlightColor = readerHighlightPalette.getOrElse(
                                    highlightsByChapter[selectedChapter + 1]
                                        ?.get(verseNumber) ?: 0
                                ) { Color.Transparent },
                                fontSize = fontSize,
                                fontFamily =
                                    effectiveReaderPreferences.fontFamily.asComposeFontFamily(),
                                fontWeight = effectiveReaderPreferences.fontFamily.bodyWeight(),
                                lineSpacingMultiplier =
                                    effectiveReaderPreferences.lineSpacingMultiplier,
                                showVerseNumber = effectiveReaderPreferences.showVerseNumbers,
                            )
                        }
                    }
                } else if (
                    effectiveReaderPreferences.continuousScrolling &&
                    selectedChapter < chapterCount
                ) {
                    item(key = "chapter-${selectedChapter + 1}-loading") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            if (continuousNextChapterLoadFailed) {
                                Text(stringResource(R.string.reader_next_chapter_load_error))
                                TextButton(onClick = { continuousReloadRequest += 1 }) {
                                    Text(stringResource(R.string.action_retry))
                                }
                            } else {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                Text(
                                    text = stringResource(R.string.reader_loading_next_chapter),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                            }
                        }
                    }
                }
            }

                }
            }
            }

            val firstVisibleVerse by remember(
                verses,
                chapterTitles,
                continuousPreviousChapterContent,
                effectiveReaderPreferences.continuousScrolling,
                effectiveReaderPreferences.textLayout,
                lazyListState,
            ) {
                derivedStateOf {
                    val currentStartIndex = if (
                        effectiveReaderPreferences.continuousScrolling &&
                        continuousPreviousChapterContent != null
                    ) {
                        previousContinuousBodyItemCount + 2
                    } else {
                        0
                    }
                    firstVerseAtBodyIndex(
                        index = (lazyListState.firstVisibleItemIndex - currentStartIndex)
                            .coerceAtLeast(0),
                        verses = verses,
                        titlesByVerse = chapterTitles,
                        textLayout = effectiveReaderPreferences.textLayout,
                    )
                }
            }
            val bibiPassages = remember(
                selectedVerseActions,
                firstVisibleVerse,
                bookName,
                selectedChapter,
            ) {
                selectedVerseActions.values
                    .sortedBy { it.number.toIntOrNull() ?: Int.MAX_VALUE }
                    .mapNotNull { selected ->
                        selected.number.toIntOrNull()?.let { verse ->
                            com.cristiancogollo.biblion.feature.bibi.model.BibiPassage(
                                book = bookName.orEmpty(),
                                chapter = selectedChapter,
                                verse = verse,
                                text = selected.text,
                            )
                        }
                    }
                    .ifEmpty {
                        firstVisibleVerse?.let { visible ->
                            visible.first.toIntOrNull()?.let { verse ->
                                listOf(
                                    com.cristiancogollo.biblion.feature.bibi.model.BibiPassage(
                                        book = bookName.orEmpty(),
                                        chapter = selectedChapter,
                                        verse = verse,
                                        text = visible.second,
                                    )
                                )
                            }
                        }.orEmpty()
                    }
            }

            if (showBibi && !isParallelReading) {
                BibiReaderOverlay(
                    bookName = bookName,
                    chapter = selectedChapter,
                    passages = bibiPassages,
                    hasExplicitSelection = selectedVerseActions.isNotEmpty(),
                    bibleVersion = selectedVersionKey,
                    openRequest = bibiOpenRequest,
                    requestedContext = requestedBibiContext,
                    currentUserName = currentUserName,
                    tutorialTargetBounds = tutorialTargetBounds,
                    onGuidedTutorialTargetAction = onGuidedTutorialTargetAction,
                    onTutorialEvent = onTutorialEvent,
                    forceOpenForTutorial = guidedStep?.targetKey ==
                        GuidedTutorialTargets.READER_BIBI_CHAT_PANEL,
                    onOpenPassage = { passage ->
                        navController.navigate(
                            Screen.Reader.createRoute(
                                bookName = passage.book,
                                chapter = passage.chapter,
                                verse = passage.verse.toString(),
                            )
                        ) {
                            launchSingleTop = true
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomEnd),
                )
            }
        }

        if (
            selectedVerseActions.isNotEmpty() &&
            showVerseActionsMenu &&
            !isParallelReading
        ) {
            VerseActionsFloatingMenu(
                selectedCount = selectedVerseActions.size,
                anchorOffset = IntOffset.Zero,
                showHighlightOptions = true,
                highlightPalette = readerHighlightPalette,
                onDismiss = { showVerseActionsMenu = false },
                onClearSelection = { selectedVerseActions = emptyMap() },
                onCopy = {
                    val selectedContent = buildVerseCopyText(
                        bookName = bookName.orEmpty(),
                        chapter = selectedChapter,
                        selections = selectedVerseActions.values,
                        bibleVersion = selectedVersionKey,
                    )
                    scope.launch {
                        clipboard.setClipEntry(
                            ClipEntry(ClipData.newPlainText("BIBLION", selectedContent))
                        )
                    }
                    selectedVerseActions = emptyMap()
                },
                onAddCitation = null,
                onHighlight = { colorIndex ->
                    val currentStep = guidedTutorial?.currentStep()?.takeIf { it.screenTarget == GuidedTutorialScreenTarget.READER }
                    val isGuideHighlightStep = currentStep?.targetKey == GuidedTutorialTargets.READER_FIRST_VERSE
                    val hasTargetVerse = selectedVerseActions.keys.isNotEmpty()
                    Log.d("GUIDE_DEBUG", "onHighlight isGuideStep=$isGuideHighlightStep hasTarget=$hasTargetVerse step=${currentStep?.id} selectedVerses=${selectedVerseActions.keys.toList()}")
                    saveHighlights(selectedVerseActions.keys, colorIndex)
                    if (isGuideHighlightStep && hasTargetVerse) {
                        onGuidedTutorialTargetAction(GuidedTutorialTargets.READER_FIRST_VERSE)
                    }
                    selectedVerseActions = emptyMap()
                },
                onInsertAsQuote = run {
                    val splitViewModel =
                        com.cristiancogollo.biblion.feature.studydocs.ui.editor.LocalSplitViewModel.current
                    if (onInsertVerseCitation == null && splitViewModel == null) {
                        null
                    } else {
                        {
                            buildCitationVerseGroups(
                                bookName = bookName ?: "Desconocido",
                                chapter = selectedChapter,
                                selections = selectedVerseActions.values,
                            ).forEach { group ->
                                if (onInsertVerseCitation != null) {
                                    onInsertVerseCitation(group, selectedVersionKey)
                                } else {
                                    splitViewModel?.insertVerseAsQuote(
                                        book = group.bookName,
                                        chapter = group.chapter,
                                        verseStart = group.verseStart,
                                        verseEnd = group.verseEnd,
                                        verseNumbers = group.verseNumbers,
                                        text = group.text,
                                        version = selectedVersionKey,
                                    )
                                }
                            }
                            selectedVerseActions = emptyMap()
                        }
                    }
                },
                onAskBibi = if (showBibi) {
                    {
                        val selectedPassages = selectedVerseActions.values
                            .sortedBy { it.number.toIntOrNull() ?: Int.MAX_VALUE }
                            .mapNotNull { selected ->
                                selected.number.toIntOrNull()?.let { verse ->
                                    BibiPassage(
                                        book = bookName.orEmpty(),
                                        chapter = selectedChapter,
                                        verse = verse,
                                        text = selected.text,
                                    )
                                }
                            }
                        if (selectedPassages.isNotEmpty()) {
                            requestedBibiContext = BibiContext.Reader(
                                book = bookName.orEmpty(),
                                chapter = selectedChapter,
                                passages = selectedPassages,
                                bibleVersion = selectedVersionKey,
                            )
                            bibiOpenRequest++
                        }
                        selectedVerseActions = emptyMap()
                        showVerseActionsMenu = false
                    }
                } else {
                    null
                },
            )
        }

        openCrossReference?.let { source ->
            ReaderCrossReferencesSheet(
                source = source,
                onDismiss = { openCrossReference = null },
                onOpenPassage = { target ->
                    openCrossReference = null
                    selectedVerseActions = emptyMap()
                    when (source.pane) {
                        1 -> {
                            parallelPrimaryBook = target.book
                            parallelPrimaryChapter = target.chapter
                            parallelPrimaryTargetVerse = target.verseStart
                            parallelPrimaryTargetRequest++
                        }
                        2 -> {
                            parallelSecondaryBook = target.book
                            parallelSecondaryChapter = target.chapter
                            parallelSecondaryTargetVerse = target.verseStart
                            parallelSecondaryTargetRequest++
                            val updatedPreferences = readerPreferences.copy(
                                secondaryBookName = target.book,
                                secondaryChapter = target.chapter,
                            )
                            readerPreferences = updatedPreferences
                            ReaderPreferencesStore.save(context, updatedPreferences)
                        }
                        else -> {
                            BibleRepository.setSelectedVersionKey(context, source.versionKey)
                            navController.navigate(
                                Screen.Reader.createRoute(
                                    bookName = target.book,
                                    chapter = target.chapter,
                                    verse = target.verseStart.toString(),
                                )
                            ) {
                                launchSingleTop = true
                            }
                        }
                    }
                },
                onAskBibi = if (showBibi && !isParallelReading) {
                    {
                        openCrossReference = null
                        requestedBibiContext = BibiContext.Reader(
                            book = source.book,
                            chapter = source.chapter,
                            passages = listOf(
                                BibiPassage(
                                    book = source.book,
                                    chapter = source.chapter,
                                    verse = source.verse,
                                    text = source.text,
                                )
                            ),
                            bibleVersion = source.versionKey,
                        )
                        bibiOpenRequest++
                    }
                } else {
                    null
                },
            )
        }

        GuidedTutorialOverlay(
            step = guidedStep,
            targetBounds = tutorialTargetBounds,
            onNext = onGuidedTutorialNext,
            onSkip = onGuidedTutorialSkip,
            onRestart = onGuidedTutorialRestart,
            isRestart = guidedTutorial?.isRestart == true
        )

        if (showReaderSettings) {
            ReaderSettingsSheet(
                fontSizeSp = fontSizeValue,
                preferences = effectiveReaderPreferences,
                onDismiss = { showReaderSettings = false },
                onApply = { updatedFontSize, updatedPreferences ->
                    if (readerPreferences.textLayout != updatedPreferences.textLayout) {
                        rememberCurrentVerseScroll()
                    }
                    fontSizeValue = updatedFontSize
                    readerPreferences = updatedPreferences
                    ReaderPreferencesStore.save(context, updatedPreferences)
                    AppPreferencesSyncStore.setReaderFontSizeSp(
                        context,
                        updatedFontSize.toInt(),
                    )
                    showReaderSettings = false
                },
            )
        }

        if (showVersionDialog) {
            BibleVersionDialog(
                versions = availableVersions,
                selectedVersionKey = selectedVersionKey,
                onVersionSelected = { selected ->
                    rememberCurrentVerseScroll()
                    selectedVersionKey = selected.key
                    clearVisibleChapterWhileLoading()
                    BibleRepository.setSelectedVersionKey(context, selected.key)
                    bookName?.let {
                        loadChapter(it, selectedChapter, selected.key)
                    }
                    showVersionDialog = false
                },
                onDismiss = {
                    showVersionDialog = false
                }
            )
        }
        }
    }
}

@Composable
internal fun ReadOnlyVerseItem(
    verseNumber: String,
    verseText: String,
    highlightColor: Color = Color.Transparent,
    fontSize: TextUnit,
    fontFamily: FontFamily,
    fontWeight: FontWeight,
    lineSpacingMultiplier: Float,
    showVerseNumber: Boolean,
    crossReference: ReaderVerseReference? = null,
    onOpenCrossReferences: ((ReaderVerseReference) -> Unit)? = null,
) {
    val verseTextColor = readerVerseForeground(
        highlightColor = highlightColor,
        normalColor = MaterialTheme.colorScheme.onSurface,
    )
    val primaryColor = readerVerseForeground(
        highlightColor = highlightColor,
        normalColor = MaterialTheme.colorScheme.primary,
    )
    val accessibilityDescription = stringResource(
        R.string.reader_verse_accessibility,
        verseNumber,
        verseText,
    )
    val annotatedVerse = remember(
        verseNumber,
        verseText,
        fontSize,
        showVerseNumber,
        primaryColor,
    ) {
        buildAnnotatedString {
            if (showVerseNumber) {
                withStyle(
                    SpanStyle(
                        fontSize = (fontSize.value * 0.6f).sp,
                        fontWeight = FontWeight.Bold,
                        baselineShift = BaselineShift.Superscript,
                        color = primaryColor,
                    )
                ) {
                    append(verseNumber)
                }
                append("  ")
            }
            append(verseText)
        }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = annotatedVerse,
            style = MaterialTheme.typography.bodyLarge.merge(
                TextStyle(
                    fontFamily = fontFamily,
                    fontWeight = fontWeight,
                    fontSize = fontSize,
                    lineHeight = (fontSize.value * lineSpacingMultiplier).sp,
                    color = verseTextColor,
                )
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = if (crossReference != null) 48.dp else 0.dp)
                .background(highlightColor, RoundedCornerShape(8.dp))
                .semantics(mergeDescendants = true) {
                    contentDescription = accessibilityDescription
                }
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .padding(bottom = 12.dp),
        )
        if (crossReference != null && onOpenCrossReferences != null) {
            Box(modifier = Modifier.align(Alignment.TopEnd)) {
                ReaderCrossReferenceIcon(crossReference) {
                    onOpenCrossReferences(crossReference)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
/**
 * Composable de un versículo individual con soporte de:
 * - selección múltiple,
 * - long-press para iniciar selección,
 * - teclado/mouse para accesibilidad.
 *
 * @param verseNumber número del versículo.
 * @param verseText contenido textual del versículo.
 * @param fontSize tamaño de letra del lector.
 * @param highlightColor color de subrayado persistido del versículo.
 * @param isSelected estado visual de selección múltiple.
 * @param onShowActions callback long-press (inicio de selección/acciones).
 * @param onToggleSelection callback de toggle en selección activa.
 */
fun VerseItem(
    verseNumber: String,
    verseText: String,
    fontSize: TextUnit,
    fontFamily: FontFamily,
    fontWeight: FontWeight,
    lineSpacingMultiplier: Float,
    showVerseNumber: Boolean,
    highlightColor: Color,
    isSelected: Boolean,
    selectionRangePosition: VerseSelectionRangePosition = VerseSelectionRangePosition.None,
    modifier: Modifier = Modifier,
    onShowActions: () -> Unit,
    onToggleSelection: () -> Unit,
    anchorSpan: IntRange? = null,
    crossReference: ReaderVerseReference? = null,
    onOpenCrossReferences: ((ReaderVerseReference) -> Unit)? = null,
) {
    val isRangeSelected = isSelected && selectionRangePosition != VerseSelectionRangePosition.None
    val selectedShape = when (selectionRangePosition) {
        VerseSelectionRangePosition.Start -> RoundedCornerShape(
            topStart = 8.dp,
            topEnd = 8.dp,
            bottomStart = 2.dp,
            bottomEnd = 2.dp
        )
        VerseSelectionRangePosition.Middle -> RoundedCornerShape(2.dp)
        VerseSelectionRangePosition.End -> RoundedCornerShape(
            topStart = 2.dp,
            topEnd = 2.dp,
            bottomStart = 8.dp,
            bottomEnd = 8.dp
        )
        else -> RoundedCornerShape(8.dp)
    }
    val bottomPadding = when (selectionRangePosition) {
        VerseSelectionRangePosition.Start,
        VerseSelectionRangePosition.Middle -> 2.dp
        else -> 12.dp
    }
    val containerColor = if (isRangeSelected) {
        BiblionBluePrimary.copy(alpha = 0.14f)
    } else {
        highlightColor
    }
    val verseTextColor = readerVerseForeground(
        highlightColor = highlightColor,
        normalColor = MaterialTheme.colorScheme.onSurface,
        isRangeSelected = isRangeSelected,
    )
    val sideBarColor = BiblionGoldPrimary
    val primaryColor = readerVerseForeground(
        highlightColor = highlightColor,
        normalColor = MaterialTheme.colorScheme.primary,
        isRangeSelected = isRangeSelected,
    )
    val verseAccessibilityDescription = stringResource(
        R.string.reader_verse_accessibility,
        verseNumber,
        verseText,
    )
    val selectActionLabel = stringResource(R.string.reader_verse_action_select)
    val optionsActionLabel = stringResource(R.string.reader_verse_action_options)
    val verseStateDescription = when {
        isSelected -> stringResource(R.string.reader_verse_selected)
        highlightColor.alpha > 0f -> stringResource(R.string.reader_verse_highlighted)
        else -> null
    }
    val annotatedVerse = remember(
        verseNumber,
        verseText,
        fontSize,
        showVerseNumber,
        anchorSpan,
        primaryColor,
        verseTextColor,
    ) {
        buildAnnotatedString {
            if (showVerseNumber) {
                withStyle(
                    style = SpanStyle(
                        fontSize = (fontSize.value * 0.6).sp,
                        fontWeight = FontWeight.Bold,
                        baselineShift = BaselineShift.Superscript,
                        color = primaryColor,
                    )
                ) {
                    append(verseNumber)
                }
            }
            val textToRender = if (showVerseNumber) "  $verseText" else verseText
            if (anchorSpan != null && anchorSpan.first >= 0 && anchorSpan.last < textToRender.length) {
                append(textToRender.substring(0, anchorSpan.first))
                withStyle(
                    style = SpanStyle(
                        background = BiblionGoldPrimary.copy(alpha = 0.25f),
                        textDecoration = TextDecoration.Underline,
                        color = verseTextColor,
                    )
                ) {
                    append(textToRender.substring(anchorSpan.first, anchorSpan.last + 1))
                }
                append(textToRender.substring(anchorSpan.last + 1))
            } else {
                append(textToRender)
            }
        }
    }
    val bodyLargeStyle = MaterialTheme.typography.bodyLarge
    val verseTextStyle = remember(
        bodyLargeStyle,
        fontSize,
        fontFamily,
        fontWeight,
        lineSpacingMultiplier,
        verseTextColor,
    ) {
        bodyLargeStyle.merge(
            TextStyle(
                fontFamily = fontFamily,
                fontWeight = fontWeight,
                lineHeight = (fontSize.value * lineSpacingMultiplier).sp,
                fontSize = fontSize,
                color = verseTextColor,
            )
        )
    }

    Box(modifier = modifier.fillMaxWidth()) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = if (crossReference != null) 48.dp else 0.dp)
                .padding(bottom = bottomPadding)
                .background(
                    color = containerColor,
                    shape = selectedShape
                )
                .drawBehind {
                    if (isRangeSelected) {
                        val width = 4.dp.toPx()
                        drawRoundRect(
                            color = sideBarColor,
                            topLeft = Offset.Zero,
                            size = Size(width = width, height = size.height),
                            cornerRadius = CornerRadius(width / 2f, width / 2f)
                        )
                    }
                }
                .semantics(mergeDescendants = true) {
                    contentDescription = verseAccessibilityDescription
                    selected = isSelected
                    if (verseStateDescription != null) {
                        stateDescription = verseStateDescription
                    }
                }
                .combinedClickable(
                    onClickLabel = selectActionLabel,
                    onLongClickLabel = optionsActionLabel,
                    onClick = onToggleSelection,
                    onLongClick = onShowActions,
                )
                .onPreviewKeyEvent { keyEvent ->
                    if (
                        keyEvent.type == KeyEventType.KeyUp &&
                        (keyEvent.key == Key.Enter || keyEvent.key == Key.Spacebar)
                    ) {
                        onToggleSelection()
                        true
                    } else {
                        false
                    }
                }
                .focusable()
                .padding(
                    start = if (isRangeSelected) 14.dp else 8.dp,
                    top = 8.dp,
                    end = 8.dp,
                    bottom = 8.dp
                ),
            text = annotatedVerse,
            style = verseTextStyle,
        )
        if (crossReference != null && onOpenCrossReferences != null) {
            Box(modifier = Modifier.align(Alignment.TopEnd)) {
                ReaderCrossReferenceIcon(crossReference) {
                    onOpenCrossReferences(crossReference)
                }
            }
        }
    }
}

package com.cristiancogollo.biblion

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
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
import com.cristiancogollo.biblion.feature.reader.ReaderVerseNumberFont
import com.cristiancogollo.biblion.feature.reader.readerSectionHeadingStyle
import com.cristiancogollo.biblion.feature.reader.buildBibleTextSections
import com.cristiancogollo.biblion.feature.reader.chapterBodyIndexForVerse
import com.cristiancogollo.biblion.feature.reader.firstVerseAtBodyIndex
import com.cristiancogollo.biblion.feature.reader.ContinuousChapterScroll
import com.cristiancogollo.biblion.feature.reader.ContinuousChapterWindow
import com.cristiancogollo.biblion.feature.reader.NEIGHBOR_CHAPTER_ALPHA
import com.cristiancogollo.biblion.feature.reader.currentChapterBodyStartIndex
import com.cristiancogollo.biblion.feature.reader.BibleTextSection
import com.cristiancogollo.biblion.feature.reader.ui.ChapterEndFooter
import com.cristiancogollo.biblion.feature.reader.ui.ContinuousChapterStart
import androidx.compose.foundation.lazy.LazyListScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.animation.rememberSplineBasedDecay
import com.cristiancogollo.biblion.feature.reader.readerVerseForeground
import com.cristiancogollo.biblion.feature.reader.ReaderHighlightColorIndices
import com.cristiancogollo.biblion.feature.reader.readerHighlightPalette
import com.cristiancogollo.biblion.feature.reader.ui.BiblePaneSelection
import com.cristiancogollo.biblion.feature.reader.ui.FlowingBibleSection
import com.cristiancogollo.biblion.feature.reader.ui.ParallelBibleReader
import com.cristiancogollo.biblion.feature.reader.ui.ReaderSettingsSheet
import com.cristiancogollo.biblion.feature.reader.ui.ReaderCrossReferenceIcon
import com.cristiancogollo.biblion.feature.reader.ui.ReaderCrossReferencesSheet
import com.cristiancogollo.biblion.feature.reader.ui.ReaderVerseReference
import com.cristiancogollo.biblion.feature.bibi.engine.CrossReferenceVoteEngine
import com.cristiancogollo.biblion.ui.theme.BiblionGoldPrimary
import com.cristiancogollo.biblion.ui.theme.BiblionGoldSoft
import com.cristiancogollo.biblion.ui.theme.BiblionNavy
import com.cristiancogollo.biblion.ui.theme.LocalBiblionThemeMode
import com.cristiancogollo.biblion.ui.theme.biblionLabelColor
import com.cristiancogollo.biblion.feature.bibi.ui.BibiReaderOverlay
import com.cristiancogollo.biblion.feature.bibi.model.BibiContext
import com.cristiancogollo.biblion.feature.bibi.model.BibiPassage
import com.cristiancogollo.biblion.feature.achievements.domain.AchievementEvent
import com.cristiancogollo.biblion.feature.achievements.tracking.AchievementTracker
import com.cristiancogollo.biblion.feature.bibi.ui.StudyBibiController
import com.cristiancogollo.biblion.feature.studydocs.ui.Screen as StudyDocScreen

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

    if (isStudyModeEnabled) {
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

        val splitState by splitViewModel.splitState.collectAsStateWithLifecycle()

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
                            primaryNavigationVisible = activePane ==
                                com.cristiancogollo.biblion.feature.studydocs.ui.editor.CompactStudyPane.Bible,
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
                    } else {
                        // En modo enfoque no hay panel de Biblia que navegar.
                        RegisterPrimaryNavigationOverride(PrimaryNavigationOverride(
                            currentRoute = null,
                            visible = false,
                            onHome = {}, onBible = {}, onSearch = {}, onStudy = {}, onProfile = {},
                        ))
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
    primaryNavigationVisible: Boolean = true,
) {
    val splitNavController = rememberNavController()
    val splitEntry by splitNavController.currentBackStackEntryAsState()

    // La barra del shell navega este panel; el lector principal no cambia de ruta.
    RegisterPrimaryNavigationOverride(
        PrimaryNavigationOverride(
            currentRoute = splitEntry?.destination?.route,
            visible = primaryNavigationVisible,
            onHome = { splitNavController.navigateTopLevel(Screen.Home.route) },
            onBible = { splitNavController.navigateTopLevel(Screen.Books.createRoute(Testament.OLD)) },
            onSearch = { splitNavController.navigateTopLevel(Screen.Search.createRoute()) },
            onStudy = { splitNavController.navigateTopLevel(StudyDocScreen.StudyDocsList.route) },
            // Este panel no tiene ruta de perfil, igual que antes en Home y Biblia.
            onProfile = {},
        )
    )

    NavHost(navController = splitNavController, startDestination = Screen.Home.route) {
        addSharedPrimaryDestinations(
            navController = splitNavController,
            openBooksInStudyMode = true,
            studyLibraryReadOnly = true,
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
    // En pantallas compactas la lectura paralela se apila verticalmente.
    val parallelReadingAvailable = !bookName.isNullOrBlank()
    var isParallelReading by rememberSaveable { mutableStateOf(false) }
    val showParallelInAppBar = readerConfiguration.screenWidthDp >= 600 || isParallelReading
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
    var isChapterLoading by remember(bookName) { mutableStateOf(!bookName.isNullOrBlank()) }
    var chapterLoadFailed by remember(bookName) { mutableStateOf(false) }
    var isBookUnavailableInVersion by remember(bookName) { mutableStateOf(false) }
    // Lectura continua: capitulos vecinos en la misma lista, atenuados (como YouVersion).
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
    val previousTextSections = remember(continuousPreviousChapterContent) {
        continuousPreviousChapterContent?.let {
            buildBibleTextSections(it.verses, it.titlesByVerse)
        }.orEmpty()
    }
    val nextTextSections = remember(continuousNextChapterContent) {
        continuousNextChapterContent?.let {
            buildBibleTextSections(it.verses, it.titlesByVerse)
        }.orEmpty()
    }
    val isFlowingLayout = readerPreferences.textLayout == ReaderTextLayout.FLOWING
    val isContinuousReading = readerPreferences.continuousScrolling
    // En modo fluido cada item es una seccion, no un versiculo.
    val currentBodyItemCount = if (isFlowingLayout) currentTextSections.size else verses.size
    val latestCurrentBodyItemCount by rememberUpdatedState(currentBodyItemCount)
    val previousContinuousBodyItemCount = when {
        !isContinuousReading || continuousPreviousChapterContent == null -> 0
        isFlowingLayout -> previousTextSections.size
        else -> continuousPreviousChapterContent?.verses?.size ?: 0
    }
    val latestCurrentChapterStartIndex by rememberUpdatedState(
        currentChapterBodyStartIndex(
            continuousScrolling = isContinuousReading && verses.isNotEmpty(),
            hasPreviousChapter = isContinuousReading && continuousPreviousChapterContent != null,
            previousBodyItemCount = previousContinuousBodyItemCount,
        ),
    )
    var selectedVersionKey by remember {
        mutableStateOf(BibleRepository.getSelectedVersionKey(context))
    }
    var highlightsByChapter by remember(bookName, selectedVersionKey) {
        mutableStateOf<Map<Int, Map<String, Int>>>(emptyMap())
    }
    val verseHighlights = highlightsByChapter[selectedChapter].orEmpty()
    val highlightPalette = readerHighlightPalette
    val currentHighlightColors = remember(verseHighlights, highlightPalette) {
        verseHighlights.mapValues { (_, colorIndex) ->
            highlightPalette.getOrElse(colorIndex) { Color.Transparent }
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
    val haptic = LocalHapticFeedback.current
    var pendingTargetVerse by remember(bookName, targetVerse) { mutableStateOf(targetVerse) }
    val lazyListState = rememberLazyListState()
    var pendingScrollRestoration by remember { mutableStateOf<Pair<String, Int>?>(null) }
    var pendingScrollToTop by remember { mutableStateOf(false) }
    val chapterSlideOffset = remember { Animatable(0f) }
    var chapterSlideVertical by remember { mutableStateOf(false) }
    // Limites de la lectura continua; se leen dentro del gesto, no al componer.
    val latestNextChapterStartKey by rememberUpdatedState(
        when {
            !isContinuousReading || isParallelReading || verses.isEmpty() -> null
            continuousNextChapterContent != null -> "chapter-${selectedChapter + 1}-header"
            selectedChapter < chapterCount -> "chapter-${selectedChapter + 1}-loading"
            else -> null
        },
    )
    val latestCurrentChapterStartKey by rememberUpdatedState(
        if (
            isContinuousReading && !isParallelReading &&
            verses.isNotEmpty() && continuousPreviousChapterContent != null
        ) {
            "chapter-$selectedChapter-header"
        } else {
            null
        },
    )
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
    var isBibiChatOpen by remember { mutableStateOf(false) }

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
            if (continuousPreviousChapterContent != null) add(selectedChapter - 1)
            if (continuousNextChapterContent != null) add(selectedChapter + 1)
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
        val bodyIndex = lazyListState.firstVisibleItemIndex - latestCurrentChapterStartIndex
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
                        validColorIndices = ReaderHighlightColorIndices,
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
        isChapterLoading = true
        chapterLoadFailed = false
        isBookUnavailableInVersion = false
        chapterContent = chapterContent.copy(
            verses = emptyList(),
            titlesByVerse = emptyMap(),
        )
    }

    // Deja listos los capitulos vecinos para que el paso entre capitulos no muestre carga.
    fun prefetchAdjacentChapters(book: String, chapter: Int, count: Int, versionKey: String) {
        readerJobs.adjacentPrefetch?.cancel()
        readerJobs.adjacentPrefetch = scope.launch {
            delay(300)
            listOf(chapter + 1, chapter - 1)
                .filter { adjacent -> adjacent in 1..count }
                .forEach { adjacent ->
                    try {
                        BibleRepository.getChapter(
                            context = context,
                            bookName = book,
                            chapterNumber = adjacent,
                            versionKey = versionKey,
                        )
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        Log.w("READER", "Adjacent chapter prefetch failed", error)
                    }
                }
        }
    }

    fun loadChapter(
        book: String,
        chapter: Int,
        versionKey: String = selectedVersionKey,
    ) {
        readerJobs.chapterLoad?.cancel()
        readerJobs.adjacentPrefetch?.cancel()
        isChapterLoading = true
        chapterLoadFailed = false
        isBookUnavailableInVersion = false
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
                    isChapterLoading = false
                    chapterLoadFailed = content.verses.isEmpty()
                    isBookUnavailableInVersion = content.chapterCount == 0
                    prefetchAdjacentChapters(book, chapter, content.chapterCount, versionKey)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                Log.e("READER", "Error loading chapter", e)
                if (
                    bookName == book &&
                    selectedChapter == chapter &&
                    selectedVersionKey == versionKey
                ) {
                    isChapterLoading = false
                    chapterLoadFailed = true
                    isBookUnavailableInVersion = false
                }
            }
        }
    }

    /**
     * Paso entre capitulos con pausa: el capitulo actual sale y el siguiente entra ya
     * precargado, sin pantalla de carga intermedia. [vertical] se usa al tirar desde el borde.
     */
    fun navigateToChapterWithAnimation(
        targetChapter: Int,
        direction: Int,
        vertical: Boolean = false,
    ) {
        val targetBook = bookName ?: return
        if (targetChapter == selectedChapter) return
        if (chapterCount > 0 && targetChapter !in 1..chapterCount) return
        val targetVersion = selectedVersionKey
        val reduceMotion = effectiveReaderPreferences.reduceMotion
        readerJobs.chapterLoad?.cancel()
        readerJobs.adjacentPrefetch?.cancel()
        selectedVerseActions = emptyMap()
        showVerseActionsMenu = false
        chapterSlideVertical = vertical
        readerJobs.chapterLoad = scope.launch {
            val distance = with(density) {
                (if (vertical) 48.dp else 72.dp).toPx()
            }
            val exit = launch {
                if (!reduceMotion) {
                    chapterSlideOffset.animateTo(
                        targetValue = -direction * distance,
                        animationSpec = tween(durationMillis = 140),
                    )
                }
            }
            val content = try {
                BibleRepository.getChapter(
                    context = context,
                    bookName = targetBook,
                    chapterNumber = targetChapter,
                    versionKey = targetVersion,
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.e("READER", "Error loading chapter $targetChapter", error)
                null
            }
            exit.join()
            if (bookName != targetBook || selectedVersionKey != targetVersion) return@launch
            selectedChapter = targetChapter
            pendingTargetVerse = null
            pendingScrollRestoration = null
            if (content == null || content.verses.isEmpty()) {
                chapterSlideOffset.snapTo(0f)
                clearVisibleChapterWhileLoading()
                pendingScrollToTop = true
                loadChapter(targetBook, targetChapter, targetVersion)
                return@launch
            }
            continuousPreviousChapterContent = null
            continuousNextChapterContent = null
            continuousNextChapterLoadFailed = false
            chapterContent = content
            isChapterLoading = false
            chapterLoadFailed = false
            isBookUnavailableInVersion = content.chapterCount == 0
            pendingScrollToTop = false
            lazyListState.scrollToItem(0)
            if (!reduceMotion) {
                chapterSlideOffset.snapTo(direction * distance)
                chapterSlideOffset.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
                )
            } else {
                chapterSlideOffset.snapTo(0f)
            }
            prefetchAdjacentChapters(targetBook, targetChapter, content.chapterCount, targetVersion)
        }
    }

    val chapterFlingDecay = rememberSplineBasedDecay<Float>()
    val continuousScroll = remember(lazyListState, scope, density, chapterFlingDecay) {
        ContinuousChapterScroll(
            listState = lazyListState,
            scope = scope,
            nextStartKey = { latestNextChapterStartKey },
            currentStartKey = { latestCurrentChapterStartKey },
            canCommit = { direction ->
                if (direction > 0) {
                    continuousNextChapterContent != null
                } else {
                    continuousPreviousChapterContent != null
                }
            },
            onCommitted = { direction ->
                val shiftedWindow = ContinuousChapterWindow(
                    chapter = selectedChapter,
                    current = chapterContent,
                    previous = continuousPreviousChapterContent,
                    next = continuousNextChapterContent,
                ).shift(direction)
                if (shiftedWindow != null) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    readerJobs.highlightLoad?.cancel()
                    selectedVerseActions = emptyMap()
                    showVerseActionsMenu = false
                    pendingScrollRestoration = null
                    pendingTargetVerse = null
                    pendingScrollToTop = false
                    selectedChapter = shiftedWindow.chapter
                    chapterContent = shiftedWindow.current
                    isChapterLoading = false
                    chapterLoadFailed = false
                    continuousPreviousChapterContent = shiftedWindow.previous
                    continuousNextChapterContent = shiftedWindow.next
                    continuousNextChapterLoadFailed = false
                }
            },
            commitDistancePx = with(density) { CONTINUOUS_COMMIT_DISTANCE.toPx() },
            commitVelocityPx = with(density) { CONTINUOUS_COMMIT_VELOCITY.toPx() },
            minPullPx = with(density) { 12.dp.toPx() },
            flingDecay = chapterFlingDecay,
            reduceMotion = { readerPreferences.reduceMotion },
        )
    }

    // Carga los capitulos vecinos en paralelo cuando el actual ya esta en pantalla.
    LaunchedEffect(
        isContinuousReading,
        bookName,
        selectedChapter,
        selectedVersionKey,
        chapterCount,
        isParallelReading,
        continuousReloadRequest,
        verses.isNotEmpty(),
    ) {
        if (
            !isContinuousReading ||
            isParallelReading ||
            bookName.isNullOrBlank() ||
            chapterCount <= 0 ||
            verses.isEmpty()
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
            ).takeIf { it.verses.isNotEmpty() }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.e("READER", "Error loading adjacent chapter $chapter", error)
            null
        }

        fun isStale() = selectedChapter != targetChapter ||
            selectedVersionKey != targetVersion ||
            bookName != targetBook

        coroutineScope {
            val existingNext = continuousNextChapterContent
            val existingPrevious = continuousPreviousChapterContent
            val nextLoad = async {
                if (targetChapter < chapterCount) existingNext ?: loadAdjacent(targetChapter + 1) else null
            }
            val previousLoad = async {
                if (targetChapter > 1) existingPrevious ?: loadAdjacent(targetChapter - 1) else null
            }
            val next = nextLoad.await()
            if (isStale()) return@coroutineScope
            continuousNextChapterContent = next
            continuousNextChapterLoadFailed = targetChapter < chapterCount && next == null
            val previous = previousLoad.await()
            if (isStale()) return@coroutineScope
            continuousPreviousChapterContent = previous
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
        val lastVisibleIndex = snapshotFlow {
            lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
        }.first { index ->
            index >= latestCurrentChapterStartIndex + ((latestCurrentBodyItemCount - 1).coerceAtLeast(0) * 0.7f).toInt()
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

        pendingScrollRestoration?.let { (verse, offset) ->
            val index = chapterBodyIndexForVerse(
                verseNumber = verse,
                verses = verses,
                titlesByVerse = chapterTitles,
                textLayout = effectiveReaderPreferences.textLayout,
            )
            if (index >= 0) {
                lazyListState.scrollToItem(latestCurrentChapterStartIndex + index, offset)
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
            val startIndex = latestCurrentChapterStartIndex
            when {
                index < 0 -> lazyListState.scrollToItem((startIndex - 1).coerceAtLeast(0))
                // Los vecinos se insertan mientras se anima: un salto directo no se desfasa.
                isContinuousReading -> lazyListState.scrollToItem(startIndex + index)
                else -> lazyListState.animateScrollToItem(index)
            }
            pendingTargetVerse = null
        } else if (pendingScrollToTop) {
            // En lectura continua el inicio del capitulo es su encabezado.
            lazyListState.scrollToItem((latestCurrentChapterStartIndex - 1).coerceAtLeast(0))
            pendingScrollToTop = false
        }
    }

    // Auto-scroll to verse 1 when highlight tutorial step becomes active
    LaunchedEffect(
        guidedStep?.targetKey,
        verses,
        effectiveReaderPreferences.textLayout,
        isParallelReading,
    ) {
        val isHighlightStep = guidedStep?.targetKey == GuidedTutorialTargets.READER_FIRST_VERSE
        if (!isParallelReading && isHighlightStep && verses.isNotEmpty()) {
            Log.d("GUIDE_DEBUG", "Auto-scrolling to verse 1 for highlight step")
            lazyListState.animateScrollToItem(latestCurrentChapterStartIndex)
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

    fun exitParallelReading() {
        selectedVerseActions = emptyMap()
        showDialog = false
        showVersionDialog = false
        isParallelReading = false
        selectedVersionKey = parallelPrimaryVersion
        BibleRepository.setSelectedVersionKey(context, parallelPrimaryVersion)
        if (parallelPrimaryBook == bookName) {
            selectedChapter = parallelPrimaryChapter
            clearVisibleChapterWhileLoading()
            loadChapter(parallelPrimaryBook, parallelPrimaryChapter, parallelPrimaryVersion)
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
    }

    fun enterParallelReading() {
        selectedVerseActions = emptyMap()
        showDialog = false
        showVersionDialog = false
        val visibleVerse = firstVerseAtBodyIndex(
            index = (lazyListState.firstVisibleItemIndex - latestCurrentChapterStartIndex)
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
        // Abre la segunda lectura en otra version para comparar sin pasos extra.
        secondaryVersionKey = secondaryVersionKey
            .takeIf { key -> key != selectedVersionKey && availableVersions.any { it.key == key } }
            ?: availableVersions.firstOrNull { it.key != selectedVersionKey }?.key
            ?: selectedVersionKey
        parallelSecondaryTargetVerse = visibleVerse
        parallelSecondaryTargetRequest++
        val updatedPreferences = readerPreferences.copy(
            secondaryBookName = bookName.orEmpty(),
            secondaryChapter = selectedChapter,
            secondaryVersionKey = secondaryVersionKey,
        )
        readerPreferences = updatedPreferences
        ReaderPreferencesStore.save(context, updatedPreferences)
        isParallelReading = true
    }

    BackHandler(enabled = isParallelReading) { exitParallelReading() }
    BackHandler(
        enabled = !isParallelReading && selectedVerseActions.isNotEmpty() && showVerseActionsMenu,
    ) {
        selectedVerseActions = emptyMap()
        showVerseActionsMenu = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                BiblionReaderTopAppBar(
                    bookName = bookName ?: "",
                    chapter = selectedChapter,
                    onNavigationIconClick = navigation@{
                        if (isParallelReading) {
                            exitParallelReading()
                            return@navigation
                        }
                        val popped = navController.popBackStackOrNavigateHome()
                        if (!popped) {
                            // Libera cualquier bloqueo heredado sin fijar vertical (rompe tablets).
                            context.findActivity()?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                        }
                    },
                    selectedVersionName = selectedVersionKey.uppercase(),
                    onVersionClick = {
                        showVersionDialog = true
                    },
                    onBookTitleClick = { showDialog = true },
                    onReadingOptionsClick = { showReaderSettings = true },
                    parallelReadingAvailable = parallelReadingAvailable && showParallelInAppBar,
                    isParallelReading = isParallelReading,
                    onParallelReadingClick = {
                        if (isParallelReading) {
                            selectedVerseActions = emptyMap()
                            showDialog = false
                            showVersionDialog = false
                            exitParallelReading()
                        } else {
                            enterParallelReading()
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
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        // El gesto vive en el contenedor completo para que en tablet
                        // tambien funcione sobre los margenes laterales del texto.
                        .pointerInput(
                            bookName,
                            selectedChapter,
                            chapterCount,
                        ) {
                            val swipeThresholdPx = CHAPTER_SWIPE_THRESHOLD.toPx()
                            detectHorizontalDragGestures(
                                onHorizontalDrag = { _, dragAmount ->
                                    horizontalDrag += dragAmount
                                },
                                onDragCancel = { horizontalDrag = 0f },
                                onDragEnd = {
                                    if (bookName.isNullOrBlank() || chapterCount <= 1) {
                                        horizontalDrag = 0f
                                        return@detectHorizontalDragGestures
                                    }
                                    when {
                                        horizontalDrag <= -swipeThresholdPx && selectedChapter < chapterCount -> {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            navigateToChapterWithAnimation(
                                                targetChapter = selectedChapter + 1,
                                                direction = 1
                                            )
                                        }

                                        horizontalDrag >= swipeThresholdPx && selectedChapter > 1 -> {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
                    .nestedScroll(continuousScroll.connection)
                    .graphicsLayer {
                        val slide = chapterSlideOffset.value
                        val slideDistance = (if (chapterSlideVertical) 48.dp else 72.dp).toPx()
                        // El empuje en el limite desplaza levemente el texto como respuesta.
                        if (chapterSlideVertical) {
                            translationY = slide - continuousScroll.pull
                        } else {
                            translationX = slide
                            translationY = -continuousScroll.pull
                        }
                        alpha = 1f - (abs(slide) / slideDistance).coerceIn(0f, 1f)
                    },
                    state = lazyListState,
                    contentPadding = PaddingValues(
                        start = if (readerConfiguration.screenWidthDp >= 600) 24.dp else 16.dp,
                        top = 16.dp,
                        end = if (readerConfiguration.screenWidthDp >= 600) 24.dp else 16.dp,
                        bottom = 112.dp,
                    ),
                ) {
                val activeChapterForDimming = selectedChapter + continuousScroll.committingDirection
                // Un solo renderizador para todos los capitulos: al avanzar, cada item conserva
                // su estado (clave estable) y solo anima su opacidad.
                fun LazyListScope.chapterItems(
                    chapter: Int,
                    content: ChapterContent,
                    sections: List<BibleTextSection>,
                    showStart: Boolean,
                    separateFromPrevious: Boolean,
                ) {
                    val isActive = chapter == selectedChapter
                    val chapterHighlights = highlightsByChapter[chapter].orEmpty()
                    if (showStart) {
                        item(key = "chapter-$chapter-header", contentType = "chapter_start") {
                            val alpha by animateFloatAsState(
                                targetValue = if (chapter == activeChapterForDimming) 1f else NEIGHBOR_CHAPTER_ALPHA,
                                label = "chapterAlpha",
                            )
                            ContinuousChapterStart(
                                bookName = bookName.orEmpty(),
                                chapter = chapter,
                                separateFromPrevious = separateFromPrevious,
                                modifier = Modifier.graphicsLayer { this.alpha = alpha },
                                fontFamily = effectiveReaderPreferences.fontFamily.asComposeFontFamily(),
                            )
                        }
                    }
                    if (effectiveReaderPreferences.textLayout == ReaderTextLayout.FLOWING) {
                        itemsIndexed(
                            items = sections,
                            key = { _, section -> "chapter-$chapter-section-${section.firstVerseNumber}" },
                            contentType = { _, _ -> "bible_text_section" },
                        ) { index, section ->
                            val alpha by animateFloatAsState(
                                targetValue = if (chapter == activeChapterForDimming) 1f else NEIGHBOR_CHAPTER_ALPHA,
                                label = "chapterAlpha",
                            )
                            FlowingBibleSection(
                                section = section,
                                fontSize = fontSize,
                                fontFamily = effectiveReaderPreferences.fontFamily.asComposeFontFamily(),
                                fontWeight = effectiveReaderPreferences.fontFamily.bodyWeight(),
                                lineSpacingMultiplier = effectiveReaderPreferences.lineSpacingMultiplier,
                                showVerseNumbers = effectiveReaderPreferences.showVerseNumbers,
                                showHeading = effectiveReaderPreferences.showSectionHeadings,
                                highContrast = effectiveReaderPreferences.highContrast,
                                selectedVerseNumbers = if (isActive) selectedVerseActions.keys else emptySet(),
                                highlightColors = chapterHighlights.mapValues { (_, colorIndex) ->
                                    readerHighlightPalette.getOrElse(colorIndex) { Color.Transparent }
                                },
                                onVerseClick = if (isActive) ::toggleVerseSelection else null,
                                modifier = Modifier
                                    .graphicsLayer { this.alpha = alpha }
                                    .then(
                                        if (isActive && index == 0) {
                                            Modifier.guidedTutorialTarget(
                                                GuidedTutorialTargets.READER_FIRST_VERSE,
                                                tutorialTargetBounds,
                                            )
                                        } else {
                                            Modifier
                                        },
                                    ),
                            )
                        }
                    } else {
                        itemsIndexed(
                            items = content.verses,
                            key = { _, verse -> "chapter-$chapter-verse-${verse.first}" },
                            contentType = { _, _ -> "bible_verse" },
                        ) { index, (verseNumber, verseText) ->
                            val alpha by animateFloatAsState(
                                targetValue = if (chapter == activeChapterForDimming) 1f else NEIGHBOR_CHAPTER_ALPHA,
                                label = "chapterAlpha",
                            )
                            Column(modifier = Modifier.graphicsLayer { this.alpha = alpha }) {
                                val chapterTitle = content.titlesByVerse[verseNumber]
                                if (
                                    effectiveReaderPreferences.showSectionHeadings &&
                                    !chapterTitle.isNullOrBlank()
                                ) {
                                    Text(
                                        text = chapterTitle,
                                        style = readerSectionHeadingStyle(
                                            effectiveReaderPreferences.fontFamily.asComposeFontFamily(),
                                        ),
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
                                    modifier = if (isActive && index == 0) {
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
                                        crossReference = referenceForVerse(chapter, verseNumber, verseText),
                                        onOpenCrossReferences = {
                                            selectedVerseActions = emptyMap()
                                            openCrossReference = it
                                        },
                                        fontSize = fontSize,
                                        fontFamily = effectiveReaderPreferences.fontFamily.asComposeFontFamily(),
                                        fontWeight = effectiveReaderPreferences.fontFamily.bodyWeight(),
                                        lineSpacingMultiplier = effectiveReaderPreferences.lineSpacingMultiplier,
                                        showVerseNumber = effectiveReaderPreferences.showVerseNumbers,
                                        highlightColor = readerHighlightPalette[chapterHighlights[verseNumber] ?: 0],
                                        isSelected = isActive && selectedVerseActions.containsKey(verseNumber),
                                        allowTapSelection = isActive && selectedVerseActions.isNotEmpty(),
                                        selectionRangePosition = verseSelectionRangePosition(
                                            verseNumber = verseNumber,
                                            selectedVerseNumbers = if (isActive) selectedVerseNumbers else emptySet(),
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                        anchorSpan = null,
                                        onShowActions = {
                                            if (!isActive) return@VerseItem
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
                                            if (isActive) toggleVerseSelection(verseNumber, verseText)
                                        },
                                    )
                                }
                            }
                        }
                    }
                }

                val continuous = isContinuousReading && verses.isNotEmpty()
                val previousContent = continuousPreviousChapterContent.takeIf { continuous }
                val nextContent = continuousNextChapterContent.takeIf { continuous }
                previousContent?.let { content ->
                    chapterItems(
                        chapter = selectedChapter - 1,
                        content = content,
                        sections = previousTextSections,
                        showStart = true,
                        separateFromPrevious = false,
                    )
                }
                chapterItems(
                    chapter = selectedChapter,
                    content = chapterContent,
                    sections = currentTextSections,
                    showStart = continuous,
                    separateFromPrevious = previousContent != null,
                )
                when {
                    nextContent != null -> chapterItems(
                        chapter = selectedChapter + 1,
                        content = nextContent,
                        sections = nextTextSections,
                        showStart = true,
                        separateFromPrevious = true,
                    )

                    continuous && selectedChapter < chapterCount -> item(
                        key = "chapter-${selectedChapter + 1}-loading",
                        contentType = "chapter_loading",
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 72.dp, bottom = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            if (continuousNextChapterLoadFailed) {
                                Text(stringResource(R.string.reader_next_chapter_load_error))
                                TextButton(onClick = { continuousReloadRequest += 1 }) {
                                    Text(stringResource(R.string.action_retry))
                                }
                            } else {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    }

                    verses.isNotEmpty() -> item(
                        key = "chapter-$selectedChapter-end",
                        contentType = "chapter_end",
                    ) {
                        ChapterEndFooter(
                            bookName = bookName.orEmpty(),
                            chapter = selectedChapter,
                            chapterCount = chapterCount,
                            onNextChapter = {
                                navigateToChapterWithAnimation(
                                    targetChapter = selectedChapter + 1,
                                    direction = 1,
                                    vertical = true,
                                )
                            },
                        )
                    }
                }
            }
                if (verses.isEmpty() && (isChapterLoading || chapterLoadFailed)) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (isChapterLoading) {
                            CircularProgressIndicator()
                        } else {
                            if (isBookUnavailableInVersion) {
                                Text(
                                    stringResource(
                                        R.string.reader_book_unavailable_in_version,
                                        bookName.orEmpty(),
                                        selectedVersionKey.uppercase(),
                                    ),
                                )
                                TextButton(onClick = { showVersionDialog = true }) {
                                    Text(stringResource(R.string.reader_choose_another_version))
                                }
                            } else {
                                Text(stringResource(R.string.reader_chapter_load_error))
                                TextButton(onClick = {
                                    bookName?.let { loadChapter(it, selectedChapter, selectedVersionKey) }
                                }) {
                                    Text(stringResource(R.string.action_retry))
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
                effectiveReaderPreferences.textLayout,
                lazyListState,
            ) {
                derivedStateOf {
                    firstVerseAtBodyIndex(
                        index = (lazyListState.firstVisibleItemIndex - latestCurrentChapterStartIndex)
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
                // Se mide en la ventana de la app: el chat de Bibi (Popup) usa la misma senal.
                val isImeVisibleForTutorial = WindowInsets.ime.getBottom(LocalDensity.current) > 0
                BibiReaderOverlay(
                    onOpenChange = { isBibiChatOpen = it },
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
                    forceOpenForTutorial = guidedStepRendersInsideBibiChat(guidedStep),
                    tutorialContent = guidedStep
                        ?.takeIf { guidedStepRendersInsideBibiChat(it) }
                        ?.let { step ->
                            {
                                GuidedTutorialInlineCard(
                                    step = step,
                                    onNext = onGuidedTutorialNext,
                                    onSkip = onGuidedTutorialSkip,
                                    onRestart = onGuidedTutorialRestart,
                                    collapsed = isImeVisibleForTutorial,
                                )
                            }
                        },
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

        if (showReaderSettings) {
            ReaderSettingsSheet(
                fontSizeSp = fontSizeValue,
                preferences = effectiveReaderPreferences,
                onDismiss = { showReaderSettings = false },
                onOpenParallelReading = if (
                    parallelReadingAvailable && !showParallelInAppBar
                ) {
                    {
                        showReaderSettings = false
                        enterParallelReading()
                    }
                } else {
                    null
                },
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

        // Fuera del contenido del Scaffold: si no, la barra superior queda por encima del velo.
        GuidedTutorialOverlay(
            step = guidedStep,
            targetBounds = tutorialTargetBounds,
            onNext = onGuidedTutorialNext,
            onSkip = onGuidedTutorialSkip,
            onRestart = onGuidedTutorialRestart,
            isRestart = guidedTutorial?.isRestart == true,
            // Con el chat abierto la guia va dentro de el; si se cierra, vuelve la burbuja.
            hostedInBibiChat = isBibiChatOpen && guidedStepRendersInsideBibiChat(guidedStep),
        )
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
 * @param allowTapSelection permite agregar o quitar versículos con un toque tras iniciar la selección.
 * @param onShowActions callback long-press (inicio de selección/acciones).
 * @param onToggleSelection callback de teclado para selección accesible.
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
    allowTapSelection: Boolean = false,
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
    val themeMode = LocalBiblionThemeMode.current
    val containerColor = if (isRangeSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
    } else {
        highlightColor
    }
    val verseTextColor = readerVerseForeground(
        highlightColor = highlightColor,
        normalColor = MaterialTheme.colorScheme.onSurface,
        mode = themeMode,
        isRangeSelected = isRangeSelected,
    )
    val sideBarColor = BiblionGoldPrimary
    val primaryColor = readerVerseForeground(
        highlightColor = highlightColor,
        normalColor = biblionLabelColor(themeMode),
        mode = themeMode,
        isRangeSelected = isRangeSelected,
    )
    val verseAccessibilityDescription = stringResource(
        R.string.reader_verse_accessibility,
        verseNumber,
        verseText,
    )
    val selectActionLabel = stringResource(R.string.reader_verse_action_select)
    val currentOnShowActions by rememberUpdatedState(onShowActions)
    val verseHaptic = LocalHapticFeedback.current
    val currentOnToggleSelection by rememberUpdatedState(onToggleSelection)
    val currentAllowTapSelection by rememberUpdatedState(allowTapSelection)
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
                        fontFamily = ReaderVerseNumberFont,
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
                    onLongClick(label = selectActionLabel) {
                        onShowActions()
                        true
                    }
                    if (allowTapSelection) {
                        onClick(label = selectActionLabel) {
                            onToggleSelection()
                            true
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            if (currentAllowTapSelection) currentOnToggleSelection()
                        },
                        onLongPress = {
                            verseHaptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            currentOnShowActions()
                        },
                    )
                }
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

/** Distancia minima para cambiar de capitulo; en dp para ser estable entre densidades. */
private val CHAPTER_SWIPE_THRESHOLD = 88.dp

/** Empuje (con resistencia) en el limite que confirma el paso de capitulo, medido en YouVersion. */
private val CONTINUOUS_COMMIT_DISTANCE = 44.dp

/** Velocidad de lanzamiento en el limite que tambien confirma el paso. */
private val CONTINUOUS_COMMIT_VELOCITY = 600.dp

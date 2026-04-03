package com.cristiancogollo.biblion

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.focusable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.ClipEntry
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
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.core.content.edit
import kotlinx.coroutines.launch
import org.json.JSONObject
import com.cristiancogollo.biblion.ui.theme.BiblionGoldPrimary
import com.cristiancogollo.biblion.ui.theme.BiblionBluePrimary
import com.cristiancogollo.biblion.ui.theme.BiblionGoldSoft
import com.cristiancogollo.biblion.ui.theme.BiblionNavy

private const val PREFS_NAME = "BiblionReaderPrefs"
private const val KEY_FONT_SIZE = "fontSize"
private const val KEY_VERSE_HIGHLIGHTS = "verseHighlights"

private val highlightPalette = listOf(
    Color(0x00000000),
    Color(0xFFFFF2A8),
    Color(0xFFC8F7C5),
    Color(0xFFFFD0D0),
    Color(0xFFD8E8FF)
)

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
    initialStudyId: Long? = null
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val studyViewModel: StudyViewModel = viewModel()

    var isStudyModeEnabled by remember { mutableStateOf(initialStudyMode) }
    val studyUi by studyViewModel.state.collectAsState()

    LaunchedEffect(initialStudyMode, initialStudyId) {
        if (initialStudyMode) {
            if (initialStudyId != null) {
                studyViewModel.process(StudyIntent.SelectStudy(initialStudyId))
            } else {
                studyViewModel.process(StudyIntent.StartNewDraft)
            }
        }
    }

    // EFECTO DE ENTRADA: Forza horizontal solo si el modo estudio está activo
    LaunchedEffect(isStudyModeEnabled, isLandscape) {
        if (isStudyModeEnabled && !isLandscape) {
            context.findActivity()?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
    }

    // EFECTO DE SALIDA: Restaura vertical SIEMPRE que se destruya esta pantalla
    DisposableEffect(Unit) {
        onDispose {
            context.findActivity()?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
        }
    }

    if (isStudyModeEnabled && isLandscape) {
        Row(modifier = Modifier.fillMaxSize()) {
            if (!studyUi.focusMode) {
                Box(modifier = Modifier.weight(1f)) {
                    StudyModeNavigation(initialBook = bookName)
                }
            }
            Box(modifier = Modifier.weight(1f)) {
                StudyEditorScreen(
                    viewModel = studyViewModel,
                    onFocusModeChanged = {},
                    onClose = {
                        // Al hacer popBackStack, el DisposableEffect de arriba se encargará de la orientación
                        navController.popBackStack()
                    }
                )
            }
        }
    } else {
        ReaderContent(
            navController = navController,
            bookName = bookName,
            isStudyModeActive = false,
            viewModel = studyViewModel,
            initialChapter = initialChapter,
            targetVerse = targetVerse
        )
    }
}

@Composable
/**
 * Navegación interna usada solo en el panel izquierdo cuando el modo estudio está activo.
 *
 * @param initialBook libro a abrir automáticamente al iniciar la navegación dividida.
 */
private fun StudyModeNavigation(initialBook: String?) {
    val splitNavController = rememberNavController()
    val studyViewModel: StudyViewModel = viewModel()

    NavHost(navController = splitNavController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) { HomeScreen(splitNavController) }
        composable(Screen.Search.route) { SearchScreen(splitNavController) }
        composable(
            route = Screen.Books.route,
            arguments = listOf(navArgument("testament") { type = NavType.StringType })
        ) { backStackEntry ->
            val testamentStr = backStackEntry.arguments?.getString("testament")
            val testament = Testament.fromRouteArg(testamentStr)
            BooksScreen(navController = splitNavController, selectedTestament = testament, openInStudyMode = true)
        }
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
                isStudyModeActive = true,
                viewModel = studyViewModel,
                initialChapter = initialChapter,
                targetVerse = targetVerse
            )
        }
    }

    LaunchedEffect(initialBook) {
        if (!initialBook.isNullOrBlank()) {
            splitNavController.navigate(Screen.Reader.createRoute(bookName = initialBook, studyMode = true)) {
                popUpTo(Screen.Home.route)
            }
        }
    }
}

data class VerseAction(val number: String, val text: String)

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
    isStudyModeActive: Boolean,
    viewModel: StudyViewModel,
    initialChapter: Int = 1,
    targetVerse: String? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    val clipboard = LocalClipboard.current

    var fontSizeValue by remember { mutableFloatStateOf(prefs.getInt(KEY_FONT_SIZE, 18).toFloat()) }
    val fontSize = fontSizeValue.sp

    var chapterCount by remember { mutableIntStateOf(0) }
    var selectedChapter by remember(bookName) { mutableIntStateOf(initialChapter.coerceAtLeast(1)) }
    var verses by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var chapterTitles by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var verseHighlights by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var showDialog by remember { mutableStateOf(false) }
    var showVersionDialog by remember { mutableStateOf(false) }
    var selectedVersionKey by remember { mutableStateOf(BibleRepository.getSelectedVersionKey(context)) }
    var availableVersions by remember { mutableStateOf<List<BibleVersionOption>>(emptyList()) }
    var showCitationInsertDialog by remember { mutableStateOf(false) }
    var selectedVerseActions by remember { mutableStateOf<Map<String, VerseAction>>(emptyMap()) }
    var horizontalDrag by remember { mutableFloatStateOf(0f) }
    var pendingTargetVerse by remember(bookName, targetVerse) { mutableStateOf(targetVerse) }
    val lazyListState = rememberLazyListState()
    var floatingButtonOffset by remember { mutableStateOf(IntOffset(0, 0)) }

    fun verseKey(verseNumber: String): String = "${bookName ?: ""}|$selectedChapter|$verseNumber"

    fun loadHighlightsForChapter() {
        val raw = prefs.getString(KEY_VERSE_HIGHLIGHTS, "{}") ?: "{}"
        val json = JSONObject(raw)
        val map = mutableMapOf<String, Int>()
        verses.forEach { (verseNumber, _) ->
            val saved = json.optInt(verseKey(verseNumber), 0)
            if (saved in highlightPalette.indices) {
                map[verseNumber] = saved
            }
        }
        verseHighlights = map
    }

    fun saveHighlight(verseNumber: String, colorIndex: Int) {
        val raw = prefs.getString(KEY_VERSE_HIGHLIGHTS, "{}") ?: "{}"
        val json = JSONObject(raw)
        json.put(verseKey(verseNumber), colorIndex)
        prefs.edit { putString(KEY_VERSE_HIGHLIGHTS, json.toString()) }
        verseHighlights = verseHighlights + (verseNumber to colorIndex)
    }

    fun loadChapter(book: String, chapter: Int) {
        scope.launch {
            try {
                val content = BibleRepository.getChapter(context, book, chapter)
                chapterCount = content.chapterCount
                verses = content.verses
                chapterTitles = content.titlesByVerse
            } catch (e: Exception) {
                Log.e("READER", "Error loading chapter: ${e.message}")
            }
        }
    }

    LaunchedEffect(bookName) {
        bookName?.let { loadChapter(it, selectedChapter) }
    }

    LaunchedEffect(Unit) {
        availableVersions = BibleRepository.getAvailableVersions(context)
        selectedVersionKey = BibleRepository.getSelectedVersionKey(context)
    }

    LaunchedEffect(bookName, selectedChapter, verses) {
        if (bookName != null && verses.isNotEmpty()) {
            loadHighlightsForChapter()
        }
    }

    LaunchedEffect(verses, selectedChapter, bookName) {
        if (bookName.isNullOrBlank() || verses.isEmpty()) return@LaunchedEffect

        val target = pendingTargetVerse
        if (!target.isNullOrBlank()) {
            val index = verses.indexOfFirst { it.first == target }
            if (index >= 0) {
                lazyListState.animateScrollToItem(index)
            } else {
                lazyListState.scrollToItem(0)
            }
            pendingTargetVerse = null
        } else {
            lazyListState.scrollToItem(0)
        }
    }

    if (showDialog && bookName != null) {
        BiblionSelectionDialog(
            title = "Capítulo",
            subtitle = bookName,
            itemCount = chapterCount,
            onDismiss = { showDialog = false },
            onItemSelected = {
                selectedChapter = it
                loadChapter(bookName, it)
                showDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            BiblionReaderTopAppBar(
                bookName = bookName ?: "",
                chapters = (1..chapterCount).toList(),
                selectedChapter = selectedChapter,
                fontSize = fontSize,
                onNavigationIconClick = {
                    if (!isStudyModeActive) {
                        navController.popBackStack()
                    } else if (!navController.popBackStack()) {
                        context.findActivity()?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    }
                },
                onChapterClick = {
                    selectedChapter = it
                    bookName?.let { b -> loadChapter(b, it) }
                },
                onSearchIconClick = { navController.navigate(Screen.Search.route) },
                onBookTitleClick = { showDialog = true },
                onIncreaseFontSize = {
                    if (fontSizeValue < 35f) {
                        fontSizeValue++
                        prefs.edit { putInt(KEY_FONT_SIZE, fontSizeValue.toInt()) }
                    }
                },
                onDecreaseFontSize = {
                    if (fontSizeValue > 12f) {
                        fontSizeValue--
                        prefs.edit { putInt(KEY_FONT_SIZE, fontSizeValue.toInt()) }
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(bookName, selectedChapter, chapterCount) {
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
                                        selectedChapter += 1
                                        loadChapter(bookName, selectedChapter)
                                    }

                                    horizontalDrag >= 40f && selectedChapter > 1 -> {
                                        selectedChapter -= 1
                                        loadChapter(bookName, selectedChapter)
                                    }
                                }
                                horizontalDrag = 0f
                            }
                        )
                    },
                state = lazyListState,
                contentPadding = PaddingValues(16.dp)
            ) {
                items(verses) { (verseNumber, verseText) ->
                    val chapterTitle = chapterTitles[verseNumber]
                    if (!chapterTitle.isNullOrBlank()) {
                        Text(
                            text = chapterTitle,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = BiblionGoldPrimary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp, bottom = 10.dp)
                        )
                    }

                    VerseItem(
                        verseNumber = verseNumber,
                        verseText = verseText,
                        fontSize = fontSize,
                        highlightColor = highlightPalette[verseHighlights[verseNumber] ?: 0],
                        isSelected = selectedVerseActions.containsKey(verseNumber),
                        isSelectionMode = selectedVerseActions.isNotEmpty(),
                        onShowActions = {
                            selectedVerseActions = if (selectedVerseActions.containsKey(verseNumber)) {
                                selectedVerseActions - verseNumber
                            } else {
                                selectedVerseActions + (verseNumber to VerseAction(verseNumber, verseText))
                            }
                        },
                        onToggleSelection = {
                            if (selectedVerseActions.isNotEmpty()) {
                                selectedVerseActions = if (selectedVerseActions.containsKey(verseNumber)) {
                                    selectedVerseActions - verseNumber
                                } else {
                                    selectedVerseActions + (verseNumber to VerseAction(verseNumber, verseText))
                                }
                            }
                        }
                    )
                }
            }

            FloatingActionButton(
                onClick = { showVersionDialog = true },
                containerColor = BiblionGoldSoft,
                contentColor = Color.White, // o Color.White si lo prefieres
                modifier = Modifier
                    .offset { floatingButtonOffset }
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            floatingButtonOffset = IntOffset(
                                x = floatingButtonOffset.x + dragAmount.x.roundToInt(),
                                y = floatingButtonOffset.y + dragAmount.y.roundToInt()
                            )
                        }
                    }
            ) {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = "Cambiar versión de Biblia"
                )
            }
        }

        if (selectedVerseActions.isNotEmpty()) {
            VerseActionsFloatingMenu(
                selectedCount = selectedVerseActions.size,
                anchorOffset = IntOffset.Zero,
                showHighlightOptions = true,
                highlightPalette = highlightPalette,
                onDismiss = { selectedVerseActions = emptyMap() },
                onClearSelection = { selectedVerseActions = emptyMap() },
                onCopy = {
                    val selectedContent = selectedVerseActions.values
                        .sortedBy { it.number.toIntOrNull() ?: Int.MAX_VALUE }
                        .joinToString("\n\n") { selected ->
                            val reference = "${bookName ?: ""} $selectedChapter:${selected.number}"
                            "$reference\n${selected.text}"
                        }
                    scope.launch {
                        clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("Biblion", selectedContent)))
                    }
                    selectedVerseActions = emptyMap()
                },
                onAddCitation = if (isStudyModeActive) {
                    {
                        showCitationInsertDialog = true
                    }
                } else {
                    null
                },
                onHighlight = { colorIndex ->
                    selectedVerseActions.keys.forEach { verseNumber ->
                        saveHighlight(verseNumber, colorIndex)
                    }
                    selectedVerseActions = emptyMap()
                }
            )
        }

        if (showCitationInsertDialog) {
            AlertDialog(
                onDismissRequest = { showCitationInsertDialog = false },
                title = { Text("Insertar cita") },
                text = { Text("Elige cómo insertar los versículos seleccionados.") },
                confirmButton = {
                    TextButton(onClick = {
                        selectedVerseActions.values.sortedBy { it.number.toIntOrNull() ?: Int.MAX_VALUE }.forEach { selected ->
                            val reference = "${bookName ?: ""} $selectedChapter:${selected.number}"
                            viewModel.addCitation(reference = reference, text = selected.text, includeFullText = true)
                        }
                        selectedVerseActions = emptyMap()
                        showCitationInsertDialog = false
                    }) { Text("Texto completo") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        selectedVerseActions.values.sortedBy { it.number.toIntOrNull() ?: Int.MAX_VALUE }.forEach { selected ->
                            val reference = "${bookName ?: ""} $selectedChapter:${selected.number}"
                            viewModel.addCitation(reference = reference, text = selected.text, includeFullText = false)
                        }
                        selectedVerseActions = emptyMap()
                        showCitationInsertDialog = false
                    }) { Text("Solo referencia") }
                }
            )
        }

        if (showVersionDialog) {
            BibleVersionDialog(
                versions = availableVersions,
                selectedVersionKey = selectedVersionKey,
                onVersionSelected = { selected ->
                    BibleRepository.setSelectedVersionKey(context, selected.key)
                    selectedVersionKey = selected.key
                    bookName?.let { loadChapter(it, selectedChapter) }
                    showVersionDialog = false
                },
                onDismiss = { showVersionDialog = false }
            )
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
 * @param isSelectionMode indica si hay selección activa global.
 * @param onShowActions callback long-press (inicio de selección/acciones).
 * @param onToggleSelection callback de toggle en selección activa.
 */
fun VerseItem(
    verseNumber: String,
    verseText: String,
    fontSize: TextUnit,
    highlightColor: Color,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onShowActions: () -> Unit,
    onToggleSelection: () -> Unit
) {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else highlightColor,
                RoundedCornerShape(8.dp)
            )
            .combinedClickable(onClick = onToggleSelection, onLongClick = onShowActions)
            .onPreviewKeyEvent { keyEvent ->
                if (
                    keyEvent.type == KeyEventType.KeyUp &&
                    (keyEvent.key == Key.Enter || keyEvent.key == Key.Spacebar)
                ) {
                    if (isSelectionMode) {
                        onToggleSelection()
                    } else {
                        onShowActions()
                    }
                    true
                } else {
                    false
                }
            }
            .focusable()
            .padding(8.dp),
        text = buildAnnotatedString {
            withStyle(
                style = SpanStyle(
                    fontSize = (fontSize.value * 0.6).sp,
                    fontWeight = FontWeight.Bold,
                    baselineShift = BaselineShift.Superscript,
                    color = BiblionGoldPrimary
                )
            ) {
                append(verseNumber)
            }
            append("  $verseText")
        },
        style = MaterialTheme.typography.bodyLarge.merge(
            TextStyle(
                fontFamily = FontFamily.Serif,
                lineHeight = (fontSize.value * 1.5).sp,
                fontSize = fontSize
            )
        )
    )
}

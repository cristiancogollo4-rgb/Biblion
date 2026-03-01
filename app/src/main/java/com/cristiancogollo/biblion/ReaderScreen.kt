package com.cristiancogollo.biblion

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.focusable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

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

fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

@Composable
fun ReaderScreen(
    navController: NavController,
    bookName: String?,
    initialStudyMode: Boolean = false
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val studyViewModel: StudyViewModel = viewModel()

    var isStudyModeEnabled by remember { mutableStateOf(initialStudyMode) }

    LaunchedEffect(isStudyModeEnabled, isLandscape) {
        if (isStudyModeEnabled && !isLandscape) {
            context.findActivity()?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
    }

    if (isStudyModeEnabled && isLandscape) {
        Row(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                StudyModeNavigation(initialBook = bookName)
            }
            Box(modifier = Modifier.weight(1f)) {
                StudyEditorScreen(
                    viewModel = studyViewModel,
                    onClose = {
                        isStudyModeEnabled = false
                        context.findActivity()?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    }
                )
            }
        }
    } else {
        ReaderContent(navController, bookName, isStudyModeActive = false, viewModel = studyViewModel)
    }
}

@Composable
private fun StudyModeNavigation(initialBook: String?) {
    val splitNavController = rememberNavController()
    val charset = StandardCharsets.UTF_8.toString()
    val studyViewModel: StudyViewModel = viewModel()

    NavHost(navController = splitNavController, startDestination = "home") {
        composable("home") { HomeScreen(splitNavController) }
        composable("search") { SearchScreen(splitNavController) }
        composable(
            route = "books/{testament}",
            arguments = listOf(navArgument("testament") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedTestament = backStackEntry.arguments?.getString("testament") ?: ""
            val testament = URLDecoder.decode(encodedTestament, charset)
            BooksScreen(navController = splitNavController, selectedTestament = testament)
        }
        composable(
            route = "reader/{bookName}",
            arguments = listOf(navArgument("bookName") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedBook = backStackEntry.arguments?.getString("bookName") ?: ""
            val book = URLDecoder.decode(encodedBook, charset)
            ReaderContent(
                navController = splitNavController,
                bookName = book,
                isStudyModeActive = true,
                viewModel = studyViewModel
            )
        }
    }

    LaunchedEffect(initialBook) {
        initialBook?.let {
            val encoded = URLEncoder.encode(it, charset)
            splitNavController.navigate("reader/$encoded") {
                popUpTo("home")
            }
        }
    }
}

data class VerseAction(val number: String, val text: String)

@Composable
fun ReaderContent(
    navController: NavController,
    bookName: String?,
    isStudyModeActive: Boolean,
    viewModel: StudyViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    val clipboard = LocalClipboardManager.current

    var fontSizeValue by remember { mutableStateOf(prefs.getInt(KEY_FONT_SIZE, 18).toFloat()) }
    val fontSize = fontSizeValue.sp

    var chapterCount by remember { mutableStateOf(0) }
    var selectedChapter by remember { mutableStateOf(1) }
    var verses by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var verseHighlights by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var showDialog by remember { mutableStateOf(false) }
    var selectedVerseAction by remember { mutableStateOf<VerseAction?>(null) }

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
        prefs.edit().putString(KEY_VERSE_HIGHLIGHTS, json.toString()).apply()
        verseHighlights = verseHighlights + (verseNumber to colorIndex)
    }

    fun copyVerse(reference: String, text: String, clipboardManager: ClipboardManager) {
        clipboardManager.setText(AnnotatedString("$reference\n$text"))
    }

    fun loadChapter(book: String, chapter: Int) {
        scope.launch(Dispatchers.IO) {
            try {
                context.assets.open("rvr1960.json").use { input ->
                    val json = JSONObject(input.bufferedReader().readText())
                    json.optJSONObject(book)?.let { bookJson ->
                        chapterCount = bookJson.length()
                        val chapterJson = bookJson.optJSONObject(chapter.toString())
                        val versesList = mutableListOf<Pair<String, String>>()
                        chapterJson?.keys()?.forEach { key ->
                            versesList.add(key to chapterJson.getString(key))
                        }
                        verses = versesList.sortedBy { it.first.toInt() }
                    }
                }
            } catch (e: Exception) {
                Log.e("READER", "Error loading chapter: ${e.message}")
            }
        }
    }

    LaunchedEffect(bookName) {
        bookName?.let { loadChapter(it, selectedChapter) }
    }

    LaunchedEffect(bookName, selectedChapter, verses) {
        if (bookName != null && verses.isNotEmpty()) {
            loadHighlightsForChapter()
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
                onSearchIconClick = { navController.navigate("search") },
                onBookTitleClick = { showDialog = true },
                onIncreaseFontSize = {
                    if (fontSizeValue < 35f) {
                        fontSizeValue++
                        prefs.edit().putInt(KEY_FONT_SIZE, fontSizeValue.toInt()).apply()
                    }
                },
                onDecreaseFontSize = {
                    if (fontSizeValue > 12f) {
                        fontSizeValue--
                        prefs.edit().putInt(KEY_FONT_SIZE, fontSizeValue.toInt()).apply()
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(verses) { (verseNumber, verseText) ->
                VerseItem(
                    verseNumber = verseNumber,
                    verseText = verseText,
                    fontSize = fontSize,
                    highlightColor = highlightPalette[verseHighlights[verseNumber] ?: 0],
                    onShowActions = {
                        selectedVerseAction = VerseAction(verseNumber, verseText)
                    }
                )
            }
        }

        selectedVerseAction?.let { selected ->
            val currentIndex = verses.indexOfFirst { it.first == selected.number }
            val previousVerseText = verses.getOrNull(currentIndex - 1)?.second
            val nextVerseText = verses.getOrNull(currentIndex + 1)?.second
            val reference = "${bookName ?: ""} $selectedChapter:${selected.number}"

            VerseActionsDialog(
                selectedVerse = selected,
                reference = reference,
                isStudyModeActive = isStudyModeActive,
                previousVerseText = previousVerseText,
                nextVerseText = nextVerseText,
                onSaveHighlight = { colorIndex ->
                    saveHighlight(selected.number, colorIndex)
                },
                onAddCitation = { ref, text, previousText, nextText ->
                    viewModel.addCitation(
                        reference = ref,
                        text = text,
                        previousText = previousText,
                        nextText = nextText
                    )
                },
                onDismiss = { selectedVerseAction = null },
                onCopy = { ref, text ->
                    copyVerse(ref, text, clipboard)
                }
            )
        }
    }
}

@Composable
private fun VerseActionsDialog(
    selectedVerse: VerseAction,
    reference: String,
    isStudyModeActive: Boolean,
    previousVerseText: String?,
    nextVerseText: String?,
    onSaveHighlight: (Int) -> Unit,
    onAddCitation: (reference: String, text: String, previousText: String?, nextText: String?) -> Unit,
    onDismiss: () -> Unit,
    onCopy: (reference: String, text: String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Opciones") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(reference, style = MaterialTheme.typography.labelLarge)
                Text(selectedVerse.text, style = MaterialTheme.typography.bodySmall)
                HorizontalDivider()
                Text("Subrayado / Favorito")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    highlightPalette.forEachIndexed { index, color ->
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(
                                    color = if (index == 0) Color.White else color,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    onSaveHighlight(index)
                                }
                        )
                    }
                }
                Text(
                    text = "Copiar",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable {
                        onCopy(reference, selectedVerse.text)
                        onDismiss()
                    }
                )
                if (isStudyModeActive) {
                    Text(
                        text = "Citar en cuaderno",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            onAddCitation(reference, selectedVerse.text, previousVerseText, nextVerseText)
                            onDismiss()
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VerseItem(
    verseNumber: String,
    verseText: String,
    fontSize: TextUnit,
    highlightColor: Color,
    onShowActions: () -> Unit
) {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .background(highlightColor, RoundedCornerShape(8.dp))
            .combinedClickable(onClick = {}, onLongClick = onShowActions)
            .onPreviewKeyEvent { keyEvent ->
                if (
                    keyEvent.type == KeyEventType.KeyUp &&
                    (keyEvent.key == Key.Enter || keyEvent.key == Key.Spacebar)
                ) {
                    onShowActions()
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
                    color = MaterialTheme.colorScheme.primary
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

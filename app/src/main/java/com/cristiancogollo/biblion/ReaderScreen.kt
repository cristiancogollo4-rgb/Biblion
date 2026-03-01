package com.cristiancogollo.biblion

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
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

    var fontSizeValue by remember { mutableStateOf(prefs.getInt(KEY_FONT_SIZE, 18).toFloat()) }
    val fontSize = fontSizeValue.sp

    var chapterCount by remember { mutableStateOf(0) }
    var selectedChapter by remember { mutableStateOf(1) }
    var verses by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var favoriteVerses by remember { mutableStateOf(setOf<String>()) }
    var verseStyles by remember { mutableStateOf<Map<String, VerseStyle>>(emptyMap()) }
    var showDialog by remember { mutableStateOf(false) }
    var selectedVerseMenu by remember { mutableStateOf<Pair<String, String>?>(null) }

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
        SelectionContainer {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(verses) { verse ->
                    VerseItem(
                        verseNumber = verse.first,
                        verseText = verse.second,
                        fontSize = fontSize,
                        isFavorite = favoriteVerses.contains(verse.first),
                        verseStyle = verseStyles[verse.first] ?: VerseStyle.None,
                        onFavoriteToggle = {
                            favoriteVerses = if (favoriteVerses.contains(verse.first)) {
                                favoriteVerses - verse.first
                            } else {
                                favoriteVerses + verse.first
                            }
                        },
                        onShowStudyMenu = {
                            if (isStudyModeActive) selectedVerseMenu = verse
                        }
                    )
                }
            }
        }

        selectedVerseMenu?.let { (verseNumber, verseText) ->
            val currentIndex = verses.indexOfFirst { it.first == verseNumber }
            val previousVerseText = verses.getOrNull(currentIndex - 1)?.second
            val nextVerseText = verses.getOrNull(currentIndex + 1)?.second

            AlertDialog(
                onDismissRequest = { selectedVerseMenu = null },
                title = { Text("Herramientas de estudio") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("$bookName $selectedChapter:$verseNumber")
                        Text(verseText, style = MaterialTheme.typography.bodySmall)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text(
                            "Resaltar amarillo",
                            modifier = Modifier.clickable {
                                verseStyles = verseStyles + (verseNumber to VerseStyle.HighlightYellow)
                            }
                        )
                        Text(
                            "Resaltar verde",
                            modifier = Modifier.clickable {
                                verseStyles = verseStyles + (verseNumber to VerseStyle.HighlightGreen)
                            }
                        )
                        Text(
                            "Subrayar",
                            modifier = Modifier.clickable {
                                verseStyles = verseStyles + (verseNumber to VerseStyle.Underline)
                            }
                        )
                        Text(
                            "Citar en cuaderno",
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable {
                                viewModel.addCitation(
                                    reference = "$bookName $selectedChapter:$verseNumber",
                                    text = verseText,
                                    previousText = previousVerseText,
                                    nextText = nextVerseText
                                )
                                selectedVerseMenu = null
                            }
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedVerseMenu = null }) {
                        Text("Cerrar")
                    }
                }
            )
        }
    }
}

enum class VerseStyle {
    None,
    HighlightYellow,
    HighlightGreen,
    Underline
}

@Composable
fun VerseItem(
    verseNumber: String,
    verseText: String,
    fontSize: TextUnit,
    isFavorite: Boolean,
    verseStyle: VerseStyle,
    onFavoriteToggle: () -> Unit,
    onShowStudyMenu: () -> Unit
) {
    val textColor = when (verseStyle) {
        VerseStyle.HighlightYellow -> Color(0xFF664A00)
        VerseStyle.HighlightGreen -> Color(0xFF155930)
        else -> MaterialTheme.colorScheme.onBackground
    }
    val textDecoration = if (verseStyle == VerseStyle.Underline) TextDecoration.Underline else null

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconButton(onClick = onFavoriteToggle, modifier = Modifier.size(26.dp)) {
            Icon(
                imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription = "Favorito",
                tint = if (isFavorite) Color(0xFFFFB300) else Color.Gray
            )
        }

        Text(
            modifier = Modifier.weight(1f).clickable { onShowStudyMenu() },
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
                    fontSize = fontSize,
                    color = textColor,
                    textDecoration = textDecoration
                )
            )
        )
    }
}

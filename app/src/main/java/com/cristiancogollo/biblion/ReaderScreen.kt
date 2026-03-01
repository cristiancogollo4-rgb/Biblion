package com.cristiancogollo.biblion

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
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
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private const val PREFS_NAME = "BiblionReaderPrefs"
private const val KEY_FONT_SIZE = "fontSize"

// Helper para encontrar la Activity y manejar la rotación
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

    // Manejo de rotación automática
    LaunchedEffect(isStudyModeEnabled, isLandscape) {
        if (isStudyModeEnabled && !isLandscape) {
            context.findActivity()?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
    }

    if (isStudyModeEnabled && isLandscape) {
        // MODO ESTUDIO: Pantalla Dividida
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
        // MODO LECTURA: Pantalla Completa
        ReaderContent(navController, bookName, isStudyModeActive = false)
    }
}

@Composable
private fun StudyModeNavigation(initialBook: String?) {
    val splitNavController = rememberNavController()
    val charset = StandardCharsets.UTF_8.toString()

    NavHost(navController = splitNavController, startDestination = "home") {
        composable("home") { HomeScreen(splitNavController) }

        composable("search") { SearchScreen(splitNavController) }

        composable(
            route = "books/{testament}",
            arguments = listOf(navArgument("testament") { type = NavType.StringType })
        ) { backStackEntry ->
            val testament = URLDecoder.decode(backStackEntry.arguments?.getString("testament") ?: "", charset)
            BooksScreen(navController = splitNavController, selectedTestament = testament)
        }

        composable(
            route = "reader/{bookName}",
            arguments = listOf(navArgument("bookName") { type = NavType.StringType })
        ) { backStackEntry ->
            val book = URLDecoder.decode(backStackEntry.arguments?.getString("bookName") ?: "", charset)
            ReaderContent(navController = splitNavController, bookName = book, isStudyModeActive = true)
        }
    }

    // Al iniciar, si hay un libro seleccionado, navegamos a él dentro del split
    LaunchedEffect(initialBook) {
        initialBook?.let {
            val encoded = URLEncoder.encode(it, charset)
            splitNavController.navigate("reader/$encoded") {
                popUpTo("home") // Evita acumular historial al iniciar
            }
        }
    }
}

@Composable
fun ReaderContent(
    navController: NavController,
    bookName: String?,
    isStudyModeActive: Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    var fontSizeValue by remember { mutableStateOf(prefs.getInt(KEY_FONT_SIZE, 18).toFloat()) }
    val fontSize = fontSizeValue.sp

    var chapterCount by remember { mutableStateOf(0) }
    var selectedChapter by remember { mutableStateOf(1) }
    var verses by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var showDialog by remember { mutableStateOf(false) }

    // Carga de JSON simplificada
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
                    if (isStudyModeActive) {
                        context.findActivity()?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    } else {
                        navController.popBackStack()
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
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(verses) { verse ->
                VerseItem(verse.first, verse.second, fontSize)
            }
        }
    }
}

@Composable
fun VerseItem(verseNumber: String, verseText: String, fontSize: TextUnit) {
    Text(
        modifier = Modifier.padding(bottom = 12.dp),
        text = buildAnnotatedString {
            withStyle(style = SpanStyle(
                fontSize = (fontSize.value * 0.6).sp,
                fontWeight = FontWeight.Bold,
                baselineShift = BaselineShift.Superscript,
                color = MaterialTheme.colorScheme.primary
            )) {
                append(verseNumber)
            }
            append("  $verseText")
        },
        style = MaterialTheme.typography.bodyLarge.copy(
            fontFamily = FontFamily.Serif,
            lineHeight = (fontSize.value * 1.5).sp,
            fontSize = fontSize
        )
    )
}
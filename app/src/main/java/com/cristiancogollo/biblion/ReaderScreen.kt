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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

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

    // Forzar rotación si viene del menú "Modo Estudio"
    LaunchedEffect(initialStudyMode) {
        if (initialStudyMode && !isLandscape) {
            context.findActivity()?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
    }

    // Estructura de pantalla dividida para Modo Estudio (Horizontal)
    if (isLandscape) {
        Row(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                ReaderContent(navController, bookName, isStudyModeActive = true)
            }
            Box(modifier = Modifier.weight(1f)) {
                // Pasamos el bookName al editor para que sepa de qué libro son las notas
                StudyEditorScreen(
                    viewModel = studyViewModel,
                    onClose = {
                        context.findActivity()?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    }
                )
            }
        }
    } else {
        // Lector normal (Vertical)
        ReaderContent(navController, bookName, isStudyModeActive = false)
    }
}

@Composable
fun ReaderContent(
    navController: NavController,
    bookName: String?,
    isStudyModeActive: Boolean
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Preferencias de fuente
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    var fontSizeValue by remember { mutableStateOf(prefs.getInt(KEY_FONT_SIZE, 18).toFloat()) }
    val fontSize = fontSizeValue.sp

    // Estados de la Biblia
    var chapterCount by remember { mutableStateOf(0) }
    var selectedChapter by remember { mutableStateOf(1) }
    var verses by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var showDialog by remember { mutableStateOf(false) }

    // --- FUNCIONES DE CARGA ---
    fun loadChapter(book: String, chapter: Int) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                context.assets.open("rvr1960.json").use { input ->
                    val json = JSONObject(input.bufferedReader().readText())
                    val bookJson = json.optJSONObject(book)
                    if (bookJson != null) {
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
                Log.e("READER", "Error: ${e.message}")
            }
        }
    }

    // Carga inicial
    LaunchedEffect(bookName) {
        bookName?.let { loadChapter(it, 1) }
    }

    if (showDialog && bookName != null) {
        BiblionSelectionDialog(
            title = "Capítulo",
            subtitle = bookName,
            itemCount = chapterCount,
            onDismiss = { showDialog = false },
            onItemSelected = { chapter ->
                selectedChapter = chapter
                loadChapter(bookName, chapter)
                showDialog = false
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background, // Asegura el fondo oscuro/claro oficial
        contentColor = MaterialTheme.colorScheme.onBackground, // Asegura que el contenido sea visible

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
                onChapterClick = { chapter ->
                    selectedChapter = chapter
                    bookName?.let { loadChapter(it, chapter) }
                },
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
    ) { paddingValues ->
        // LISTA DE VERSÍCULOS
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(verses) { verse ->
                VerseItem(
                    verseNumber = verse.first,
                    verseText = verse.second,
                    fontSize = fontSize
                )
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
            append("  ")
            append(verseText)
        },
        color = MaterialTheme.colorScheme.onBackground,
        style = MaterialTheme.typography.bodyLarge.copy(
            fontFamily = FontFamily.Serif,
            lineHeight = (fontSize.value * 1.5).sp,
            fontSize = fontSize
        )
    )
}
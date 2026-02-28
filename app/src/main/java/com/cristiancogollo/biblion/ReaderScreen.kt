package com.cristiancogollo.biblion

import android.content.Context
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

// Constantes para SharedPreferences
private const val PREFS_NAME = "BiblionReaderPrefs"
private const val KEY_FONT_SIZE = "fontSize"

@Composable
fun ReaderScreen(
    navController: NavController,
    bookName: String?
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Manejo de Preferencias y Fuente
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    val initialFontSize = prefs.getInt(KEY_FONT_SIZE, 18)
    var fontSize by remember { mutableStateOf(initialFontSize.sp) }

    // Estados de la Biblia
    var chapterCount by remember { mutableStateOf(0) }
    var selectedChapter by remember { mutableStateOf(1) }
    var verses by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var showDialog by remember { mutableStateOf(false) }

    // Funciones de ayuda para cargar datos
    fun getChapterCountFromAssets(book: String): Int {
        return try {
            context.assets.open("rvr1960.json").use { inputStream ->
                val json = JSONObject(inputStream.bufferedReader().use { it.readText() })
                val bookJson = json.optJSONObject(book)
                bookJson?.length() ?: 0
            }
        } catch (e: Exception) { 0 }
    }

    // Dentro de ReaderScreen.kt, busca la función loadChapterFromAssets
    fun loadChapterFromAssets(book: String, chapter: Int): List<Pair<String, String>> {
        return try {
            context.assets.open("rvr1960.json").use { inputStream ->
                val json = JSONObject(inputStream.bufferedReader().use { it.readText() })

                // LOG PARA DEPURAR: Abre el Logcat y busca "JSON_CHECK"
                android.util.Log.d("JSON_CHECK", "Buscando libro: '$book' en el JSON")

                val bookJson = json.optJSONObject(book)
                if (bookJson == null) {
                    android.util.Log.e("JSON_CHECK", "¡ERROR! No se encontró el libro '$book' en el JSON")
                    return emptyList()
                }

                val chapterJson = bookJson.optJSONObject(chapter.toString())
                val versesList = mutableListOf<Pair<String, String>>()

                chapterJson?.keys()?.forEach { verseNumber ->
                    versesList.add(Pair(verseNumber, chapterJson.getString(verseNumber)))
                }
                // Importante: Ordenar por número de versículo
                versesList.sortedBy { it.first.toInt() }
            }
        } catch (e: Exception) {
            android.util.Log.e("JSON_CHECK", "Error cargando: ${e.message}")
            emptyList()
        }
    }

    // Lógica para aumentar/disminuir fuente
    val increaseFont = {
        val newSize = (fontSize.value + 1f).coerceAtMost(35f)
        fontSize = newSize.sp
        prefs.edit().putInt(KEY_FONT_SIZE, newSize.toInt()).apply()
    }

    val decreaseFont = {
        val newSize = (fontSize.value - 1f).coerceAtLeast(12f)
        fontSize = newSize.sp
        prefs.edit().putInt(KEY_FONT_SIZE, newSize.toInt()).apply()
    }

    // Carga inicial
    LaunchedEffect(bookName) {
        if (bookName != null) {
            withContext(Dispatchers.IO) {
                chapterCount = getChapterCountFromAssets(bookName)
                verses = loadChapterFromAssets(bookName, 1)
            }
        }
    }

    // Diálogo de selección
    if (showDialog && bookName != null) {
        BiblionSelectionDialog(
            title = "Capítulo",
            subtitle = bookName,
            itemCount = chapterCount,
            onDismiss = { showDialog = false },
            onItemSelected = { chapter ->
                selectedChapter = chapter
                coroutineScope.launch {
                    withContext(Dispatchers.IO) {
                        verses = loadChapterFromAssets(bookName, chapter)
                    }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            if (bookName != null) {
                // USAMOS LA VERSIÓN ESPECIALIZADA QUE CREAMOS ANTES
                BiblionReaderTopAppBar(
                    bookName = bookName,
                    chapters = (1..chapterCount).toList(),
                    selectedChapter = selectedChapter,
                    fontSize = fontSize,
                    onNavigationIconClick = { navController.popBackStack() },
                    onChapterClick = { chapter ->
                        selectedChapter = chapter
                        coroutineScope.launch {
                            withContext(Dispatchers.IO) {
                                verses = loadChapterFromAssets(bookName, chapter)
                            }
                        }
                    },
                    onSearchIconClick = { /* Implementar búsqueda */ },
                    onBookTitleClick = { showDialog = true },
                    onIncreaseFontSize = increaseFont,
                    onDecreaseFontSize = decreaseFont
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
        ) {
            itemsIndexed(verses) { _, verse ->
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
                fontSize = (fontSize.value * 0.65).sp,
                fontWeight = FontWeight.Bold,
                baselineShift = BaselineShift.Superscript,
                color = MaterialTheme.colorScheme.primary
            )) {
                append(verseNumber)
            }
            append("  ")
            append(verseText)
        },
        style = MaterialTheme.typography.bodyLarge.copy(
            fontFamily = FontFamily.Serif,
            lineHeight = (fontSize.value * 1.6).sp,
            fontSize = fontSize
        )
    )
}
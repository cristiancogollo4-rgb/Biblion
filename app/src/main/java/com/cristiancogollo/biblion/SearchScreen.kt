package com.cristiancogollo.biblion

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class SearchResult(val reference: String, val text: String)

/**
 * Pantalla de búsqueda bíblica por texto libre.
 *
 * @param navController controlador de navegación para volver a pantalla anterior.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<SearchResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Buscar en la Biblia") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Introduce una palabra o frase") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    if (searchQuery.isNotBlank()) {
                        scope.launch {
                            isSearching = true
                            searchResults = searchVerses(context, searchQuery)
                            isSearching = false
                        }
                    }
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Buscar")
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (isSearching) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(searchResults) { result ->
                        // Usamos la Card que ya tienes para mostrar cada versículo encontrado
                        DailyVerseCard(verse = result.text, reference = result.reference)
                    }
                }
            }
        }
    }
}

/**
 * Recorre el JSON bíblico local y devuelve coincidencias por texto.
 *
 * @param context contexto Android para leer assets.
 * @param query texto buscado (case-insensitive).
 * @return lista de referencias y textos que contienen la consulta.
 */
private suspend fun searchVerses(context: Context, query: String): List<SearchResult> {
    return withContext(Dispatchers.IO) {
        val results = mutableListOf<SearchResult>()
        try {
            val jsonString = context.assets.open("rvr1960.json").bufferedReader().use { it.readText() }
            val bible = JSONObject(jsonString)

            val books = bible.keys()
            while (books.hasNext()) {
                val bookName = books.next()
                val book = bible.getJSONObject(bookName)

                val chapters = book.keys()
                while (chapters.hasNext()) {
                    val chapterNum = chapters.next()
                    val chapter = book.getJSONObject(chapterNum)

                    val verses = chapter.keys()
                    while (verses.hasNext()) {
                        val verseNum = verses.next()
                        val verseText = chapter.getString(verseNum)

                        if (verseText.contains(query, ignoreCase = true)) {
                            results.add(SearchResult("$bookName $chapterNum:$verseNum", verseText))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Manejo de errores simple, podrías querer algo más sofisticado
            e.printStackTrace()
        }
        results
    }
}

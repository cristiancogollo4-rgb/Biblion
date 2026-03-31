package com.cristiancogollo.biblion

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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.cristiancogollo.biblion.ui.theme.BiblionNavy
import kotlinx.coroutines.launch

data class SearchResult(
    val reference: String,
    val text: String,
    val bookName: String,
    val chapter: Int,
    val verse: String
)

sealed interface SearchUiState {
    data object Idle : SearchUiState
    data object Loading : SearchUiState
    data class Success(val results: List<SearchResult>) : SearchUiState
    data class Empty(val query: String) : SearchUiState
    data class Error(val message: String) : SearchUiState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var uiState by remember { mutableStateOf<SearchUiState>(SearchUiState.Idle) }

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
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Encuentra un versículo",
                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                        color = BiblionNavy,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Introduce una palabra o frase") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null)
                        }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    FilledTonalButton(
                        onClick = {
                            if (searchQuery.isBlank()) return@FilledTonalButton
                            scope.launch {
                                uiState = SearchUiState.Loading
                                runCatching {
                                    BibleRepository.searchVerses(context, searchQuery)
                                }.onSuccess { results ->
                                    uiState = if (results.isEmpty()) {
                                        SearchUiState.Empty(searchQuery)
                                    } else {
                                        SearchUiState.Success(results)
                                    }
                                }.onFailure { error ->
                                    uiState = SearchUiState.Error(error.message ?: "Ocurrió un error inesperado")
                                }
                            }
                        },
                        enabled = uiState !is SearchUiState.Loading,
                        modifier = Modifier.align(Alignment.End),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = BiblionNavy.copy(alpha = 0.12f),
                            contentColor = BiblionNavy
                        )
                    ) {
                        Text("Buscar")
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            when (val state = uiState) {
                SearchUiState.Idle -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Escribe una palabra para comenzar la búsqueda.")
                    }
                }
                SearchUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is SearchUiState.Empty -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Sin resultados para: \"${state.query}\".")
                    }
                }
                is SearchUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Error al buscar: ${state.message}")
                    }
                }
                is SearchUiState.Success -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.results) { result ->
                            DailyVerseCard(
                                verse = result.text,
                                reference = result.reference,
                                onClick = {
                                    navController.navigateSingleTop(
                                        Screen.Reader.createRoute(
                                            bookName = result.bookName,
                                            chapter = result.chapter,
                                            verse = result.verse
                                        )
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

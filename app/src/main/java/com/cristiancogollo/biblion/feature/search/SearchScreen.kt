package com.cristiancogollo.biblion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.cristiancogollo.biblion.ui.theme.BiblionGoldPrimary
import com.cristiancogollo.biblion.ui.theme.BiblionGoldSoft
import kotlinx.coroutines.launch

private val searchOldTestamentBooks = listOf(
    "Genesis", "Exodo", "Levitico", "Numeros", "Deuteronomio", "Josue", "Jueces", "Rut",
    "1 Samuel", "2 Samuel", "1 Reyes", "2 Reyes", "1 Cronicas", "2 Cronicas", "Esdras",
    "Nehemias", "Ester", "Job", "Salmos", "Proverbios", "Eclesiastes", "Cantares",
    "Isaias", "Jeremias", "Lamentaciones", "Ezequiel", "Daniel", "Oseas", "Joel", "Amos",
    "Abdias", "Jonas", "Miqueas", "Nahum", "Habacuc", "Sofonias", "Hageo", "Zacarias", "Malaquias"
)

private val searchNewTestamentBooks = listOf(
    "Mateo", "Marcos", "Lucas", "Juan", "Hechos", "Romanos", "1 Corintios", "2 Corintios",
    "Galatas", "Efesios", "Filipenses", "Colosenses", "1 Tesalonicenses", "2 Tesalonicenses",
    "1 Timoteo", "2 Timoteo", "Tito", "Filemon", "Hebreos", "Santiago", "1 Pedro", "2 Pedro",
    "1 Juan", "2 Juan", "3 Juan", "Judas", "Apocalipsis"
)

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
    var selectedTestament by remember { mutableStateOf(BibleSearchTestament.ALL) }
    var selectedBook by remember { mutableStateOf<String?>(null) }
    var bookMenuExpanded by remember { mutableStateOf(false) }
    val availableBooks = remember(selectedTestament) {
        when (selectedTestament) {
            BibleSearchTestament.ALL -> searchOldTestamentBooks + searchNewTestamentBooks
            BibleSearchTestament.OLD -> searchOldTestamentBooks
            BibleSearchTestament.NEW -> searchNewTestamentBooks
        }
    }

    fun performSearch() {
        val query = searchQuery.trim()
        if (query.isBlank() || uiState is SearchUiState.Loading) return
        scope.launch {
            uiState = SearchUiState.Loading
            runCatching {
                BibleRepository.searchVerses(
                    context = context,
                    query = query,
                    filter = BibleSearchFilter(
                        testament = selectedTestament,
                        bookName = selectedBook
                    )
                )
            }.onSuccess { results ->
                uiState = if (results.isEmpty()) {
                    SearchUiState.Empty(query)
                } else {
                    SearchUiState.Success(results)
                }
            }.onFailure { error ->
                uiState = SearchUiState.Error(
                    error.message ?: context.getString(R.string.search_error_unexpected)
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.search_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStackOrNavigateHome() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
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
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.search_card_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.search_card_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text(stringResource(R.string.search_field_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { performSearch() }),
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        searchQuery = ""
                                        uiState = SearchUiState.Idle
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = stringResource(R.string.search_clear)
                                    )
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BiblionGoldPrimary,
                            unfocusedBorderColor = BiblionGoldSoft,
                            focusedLabelColor = BiblionGoldPrimary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                            cursorColor = BiblionGoldPrimary,
                            focusedLeadingIconColor = BiblionGoldPrimary,
                            unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    SearchFilters(
                        selectedTestament = selectedTestament,
                        selectedBook = selectedBook,
                        availableBooks = availableBooks,
                        bookMenuExpanded = bookMenuExpanded,
                        onBookMenuExpandedChange = { bookMenuExpanded = it },
                        onTestamentSelected = { testament ->
                            selectedTestament = testament
                            selectedBook = selectedBook?.takeIf { it in when (testament) {
                                BibleSearchTestament.ALL -> searchOldTestamentBooks + searchNewTestamentBooks
                                BibleSearchTestament.OLD -> searchOldTestamentBooks
                                BibleSearchTestament.NEW -> searchNewTestamentBooks
                            } }
                        },
                        onBookSelected = { selected ->
                            selectedBook = selected
                            bookMenuExpanded = false
                        },
                        onClearBook = { selectedBook = null }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    FilledTonalButton(
                        onClick = { performSearch() },
                        enabled = searchQuery.isNotBlank() && uiState !is SearchUiState.Loading,
                        modifier = Modifier.align(Alignment.End),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = BiblionGoldSoft.copy(alpha = 0.12f),
                            contentColor = BiblionGoldPrimary
                        )
                    ) {
                        Text(stringResource(R.string.search_button))
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            when (val state = uiState) {
                SearchUiState.Idle -> {
                    SearchStateMessage(text = stringResource(R.string.search_idle_message))
                }

                SearchUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                is SearchUiState.Empty -> {
                    SearchStateMessage(text = stringResource(R.string.search_empty_message, state.query))
                }

                is SearchUiState.Error -> {
                    SearchStateMessage(text = stringResource(R.string.search_error_message, state.message))
                }

                is SearchUiState.Success -> {
                    Text(
                        text = pluralStringResource(
                            R.plurals.search_results_count,
                            state.results.size,
                            state.results.size
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                    )
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchFilters(
    selectedTestament: BibleSearchTestament,
    selectedBook: String?,
    availableBooks: List<String>,
    bookMenuExpanded: Boolean,
    onBookMenuExpandedChange: (Boolean) -> Unit,
    onTestamentSelected: (BibleSearchTestament) -> Unit,
    onBookSelected: (String) -> Unit,
    onClearBook: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.search_filters_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SearchTestamentChip(
                label = stringResource(R.string.search_filter_all),
                selected = selectedTestament == BibleSearchTestament.ALL,
                onClick = { onTestamentSelected(BibleSearchTestament.ALL) }
            )
            SearchTestamentChip(
                label = stringResource(R.string.testament_old_label),
                selected = selectedTestament == BibleSearchTestament.OLD,
                onClick = { onTestamentSelected(BibleSearchTestament.OLD) }
            )
            SearchTestamentChip(
                label = stringResource(R.string.testament_new_label),
                selected = selectedTestament == BibleSearchTestament.NEW,
                onClick = { onTestamentSelected(BibleSearchTestament.NEW) }
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box {
                TextButton(onClick = { onBookMenuExpandedChange(true) }) {
                    Text(selectedBook ?: stringResource(R.string.search_filter_book_all))
                }
                DropdownMenu(
                    expanded = bookMenuExpanded,
                    onDismissRequest = { onBookMenuExpandedChange(false) }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.search_filter_book_all)) },
                        onClick = {
                            onClearBook()
                            onBookMenuExpandedChange(false)
                        }
                    )
                    availableBooks.forEach { book ->
                        DropdownMenuItem(
                            text = { Text(book) },
                            onClick = { onBookSelected(book) }
                        )
                    }
                }
            }
            if (!selectedBook.isNullOrBlank()) {
                TextButton(onClick = onClearBook) {
                    Text(stringResource(R.string.search_filter_clear_book))
                }
            }
        }
    }
}

@Composable
private fun SearchTestamentChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) }
    )
}

@Composable
private fun SearchStateMessage(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

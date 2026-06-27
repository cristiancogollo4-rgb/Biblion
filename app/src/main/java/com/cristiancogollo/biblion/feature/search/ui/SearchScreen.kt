package com.cristiancogollo.biblion.feature.search.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.cristiancogollo.biblion.GuidedTutorialProgress
import com.cristiancogollo.biblion.GuidedTutorialScreenTarget
import com.cristiancogollo.biblion.GuidedTutorialOverlay
import com.cristiancogollo.biblion.currentStep
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.cristiancogollo.biblion.R
import com.cristiancogollo.biblion.BibleSearchTestament
import com.cristiancogollo.biblion.Screen
import com.cristiancogollo.biblion.DailyVerseCard
import com.cristiancogollo.biblion.BibleRepository
import com.cristiancogollo.biblion.BibleSearchFilter
import com.cristiancogollo.biblion.feature.bibi.data.TopicDatabase
import com.cristiancogollo.biblion.feature.bibi.engine.TopicEngine
import com.cristiancogollo.biblion.feature.bibi.model.RelatedVerse
import com.cristiancogollo.biblion.feature.search.components.AutoScrollingTopicCarousel
import com.cristiancogollo.biblion.feature.search.components.CategoryLabels
import com.cristiancogollo.biblion.feature.search.components.PopularTopic
import com.cristiancogollo.biblion.feature.search.components.PopularTopicsData
import com.cristiancogollo.biblion.feature.search.model.SearchScope
import com.cristiancogollo.biblion.navigateSingleTop
import com.cristiancogollo.biblion.popBackStackOrNavigateHome
import com.cristiancogollo.biblion.feature.search.components.TopicHit
import com.cristiancogollo.biblion.feature.search.components.TopicsSection
import com.cristiancogollo.biblion.feature.search.data.SearchHistoryEntry
import com.cristiancogollo.biblion.feature.search.data.SearchHistoryRepository
import com.cristiancogollo.biblion.ui.theme.BiblionGoldPrimary
import com.cristiancogollo.biblion.ui.theme.BiblionGoldSoft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.text.Normalizer

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
    data class Success(
        val topicHits: List<TopicHit> = emptyList(),
        val results: List<SearchResult>
    ) : SearchUiState
    data class Empty(val query: String) : SearchUiState
    data class Error(val message: String) : SearchUiState
}

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun SearchScreen(
    navController: NavController,
    scope: SearchScope = SearchScope.BIBLE,
    guidedTutorial: GuidedTutorialProgress? = null,
    onGuidedTutorialNext: () -> Unit = {},
    onGuidedTutorialSkip: () -> Unit = {},
    onGuidedTutorialRestart: () -> Unit = {},
    onGuidedTutorialTargetAction: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var uiState by remember { mutableStateOf<SearchUiState>(SearchUiState.Idle) }
    var recentSearches by remember { mutableStateOf<List<SearchHistoryEntry>>(emptyList()) }
    var showAllRecent by remember { mutableStateOf(false) }
    var selectedTestament by remember { mutableStateOf(BibleSearchTestament.ALL) }
    var selectedBook by remember { mutableStateOf<String?>(null) }
    var expandedTopicSlug by remember { mutableStateOf<String?>(null) }
    // Indica si el primer render ya termino. Se usa para diferir la carga
    // de secciones pesadas (carrusel, busquedas recientes) y evitar el bloqueo
    // del primer frame que causa "Skipped 89 frames" en el Choreographer.
    var isReady by remember { mutableStateOf(false) }
    var rotationPool by remember { mutableStateOf<List<PopularTopic>>(emptyList()) }

    // Flow para búsqueda en vivo con debounce seguro (collectLatest cancela la corutina anterior).
    val queryFlow = remember { MutableStateFlow("") }

    val availableBooks = remember(selectedTestament) {
        when (selectedTestament) {
            BibleSearchTestament.ALL -> searchOldTestamentBooks + searchNewTestamentBooks
            BibleSearchTestament.OLD -> searchOldTestamentBooks
            BibleSearchTestament.NEW -> searchNewTestamentBooks
        }
    }

    // Diferir la carga del primer frame: primero pintar la UI vacia
    // y despues cargar las secciones pesadas. Evita "Skipped 89 frames"
    // en el Choreographer que congela la app al entrar a Search.
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(50)  // Da tiempo a Compose a pintar el primer frame
        isReady = true
        recentSearches = SearchHistoryRepository.getRecent(context, 10)
    }

    // Pool de temas para el carrusel de "Temas populares".
    // Carga los 424 temas canonicos con versiculos y los baraja
    // aleatoriamente para que las cards iniciales tengan variedad
    // (no solo los top por cantidad de versiculos). El carrusel
    // mantiene la regla de unicidad entre cards.
    LaunchedEffect(Unit) {
        val entities = TopicDatabase.getInstance(context)
            .topicDao()
            .getRotatableTopics(limit = 500)
        rotationPool = entities
            .shuffled()
            .map { e ->
                PopularTopic(
                    slug = e.slug,
                    nameEs = e.nameEs,
                    nameEn = e.nameEn,
                    description = e.description.ifBlank { CategoryLabels.label(e.category) },
                    category = e.category,
                    topicColor = CategoryLabels.color(e.category).toArgb(),
                    verseCount = e.verseCount
                )
            }
    }

    suspend fun executeSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        uiState = SearchUiState.Loading
        SearchHistoryRepository.recordQuery(context, trimmed)
        try {
            // Búsqueda paralela: versículos bíblicos + temas canónicos.
            val results: List<SearchResult>
            val topicHits: List<TopicHit>
            coroutineScope {
                val versesDeferred = async {
                    BibleRepository.searchVerses(
                        context = context,
                        query = trimmed,
                        filter = BibleSearchFilter(
                            testament = selectedTestament,
                            bookName = selectedBook
                        )
                    )
                }
                val topicsDeferred = async {
                    TopicEngine
                        .searchTopics(context, trimmed, maxTotal = 4)
                        .map { entity ->
                            TopicHit(
                                slug = entity.slug,
                                nameEs = entity.nameEs,
                                nameEn = entity.nameEn,
                                description = entity.description,
                                category = entity.category,
                                verseCount = entity.verseCount
                            )
                        }
                }
                results = versesDeferred.await()
                topicHits = topicsDeferred.await()
            }
            // Verificar que la query no ha cambiado durante la ejecucion
            val currentQuery = searchQuery.trim()
            if (currentQuery == trimmed || currentQuery.isEmpty()) {
                val noTopics = topicHits.isEmpty()
                val noVerses = results.isEmpty()
                uiState = when {
                    noTopics && noVerses -> SearchUiState.Empty(trimmed)
                    else -> SearchUiState.Success(topicHits = topicHits, results = results)
                }
            }
        } catch (e: Exception) {
            val currentQuery = searchQuery.trim()
            if (currentQuery == trimmed || currentQuery.isEmpty()) {
                uiState = SearchUiState.Error(
                    e.message ?: context.getString(R.string.search_error_unexpected)
                )
            }
        }
        if (searchQuery.trim() == trimmed) {
            recentSearches = SearchHistoryRepository.getRecent(context, 10)
        }
    }

    fun runSearchImmediate(query: String) {
        queryFlow.value = query
    }

    // Sincronizar el input con el flow cuando el usuario escribe manualmente
    LaunchedEffect(searchQuery) {
        queryFlow.value = searchQuery
    }

    // Búsqueda en vivo con debounce — collectLatest cancela búsquedas anteriores
    LaunchedEffect(Unit) {
        queryFlow
            .debounce(400L)
            .filter { it.trim().length >= 2 }
            .distinctUntilChanged()
            .collectLatest { query ->
                coroutineScope.launch { executeSearch(query) }
            }
    }

    fun openVerse(bookName: String, chapter: Int, verse: String) {
        navController.navigateSingleTop(
            Screen.Reader.createRoute(
                bookName = bookName,
                chapter = chapter,
                verse = verse
            )
        )
    }

    fun clearAllHistory() {
        coroutineScope.launch {
            SearchHistoryRepository.clearAll(context)
            recentSearches = emptyList()
        }
    }

    fun setTestament(testament: BibleSearchTestament) {
        if (selectedTestament != testament) {
            selectedTestament = testament
            selectedBook = null
            // Si hay una búsqueda activa, re-ejecutar con el nuevo filtro
            if (uiState is SearchUiState.Success || uiState is SearchUiState.Empty) {
                runSearchImmediate(searchQuery)
            }
        }
    }

    fun setBook(book: String?) {
        if (selectedBook != book) {
            selectedBook = book
            // Si hay una búsqueda activa, re-ejecutar
            if (uiState is SearchUiState.Success || uiState is SearchUiState.Empty) {
                runSearchImmediate(searchQuery)
            }
        }
    }

    fun clearAllFilters() {
        selectedTestament = BibleSearchTestament.ALL
        selectedBook = null
        if (uiState is SearchUiState.Success || uiState is SearchUiState.Empty) {
            runSearchImmediate(searchQuery)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.search_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStackOrNavigateHome() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO help */ }) {
                        Icon(
                            Icons.Default.HelpOutline,
                            contentDescription = "Help"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            SearchInputCard(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onSubmit = { runSearchImmediate(searchQuery) },
                onClear = {
                    searchQuery = ""
                    uiState = SearchUiState.Idle
                }
            )

            val isTyping = searchQuery.isNotBlank()
            // Solo mostrar el carrusel y busquedas recientes si:
            // 1) el primer frame ya termino (isReady)
            // 2) NO hay query activa
            // 3) estamos en Idle
            val showPopular = isReady && uiState is SearchUiState.Idle && !isTyping

            SearchContent(
                uiState = uiState,
                currentQuery = searchQuery,
                isTyping = isTyping,
                showPopular = showPopular,
                recentSearches = recentSearches,
                showAllRecent = showAllRecent,
                rotationPool = rotationPool,
                onToggleShowAll = { showAllRecent = !showAllRecent },
                onRecentClick = { query ->
                    searchQuery = query
                    runSearchImmediate(query)
                },
                onClearHistory = { clearAllHistory() },
                onPopularClick = { topic ->
                    expandedTopicSlug = null
                    searchQuery = topic.nameEs
                    runSearchImmediate(topic.nameEs)
                },
                onClearSearch = {
                    searchQuery = ""
                    queryFlow.value = ""
                    uiState = SearchUiState.Idle
                    expandedTopicSlug = null
                },
                onResultClick = { result ->
                    openVerse(result.bookName, result.chapter, result.verse)
                },
                onTopicVerseClick = { verse ->
                    openVerse(verse.book, verse.chapter, verse.verseStart.toString())
                },
                onToggleTopicExpansion = { topic ->
                    expandedTopicSlug = if (expandedTopicSlug == topic.slug) null else topic.slug
                },
                expandedTopicSlug = expandedTopicSlug,
                selectedTestament = selectedTestament,
                onTestamentSelected = { setTestament(it) },
                availableBooks = availableBooks,
                selectedBook = selectedBook,
                onBookSelected = { setBook(it) },
                onClearFilters = { clearAllFilters() },
                onExploreTopics = {
                    navController.navigate(Screen.ExploreTopics.route)
                },
                onOpenDictionary = {
                    navController.navigate(Screen.Dictionary.route)
                }
            )
        }
    }

    val guidedStep = guidedTutorial
        ?.currentStep()
        ?.takeIf { it.screenTarget == GuidedTutorialScreenTarget.SEARCH }
    val tutorialTargetBounds = remember { mutableStateMapOf<String, androidx.compose.ui.geometry.Rect>() }

    if (guidedStep != null) {
        GuidedTutorialOverlay(
            step = guidedStep,
            targetBounds = tutorialTargetBounds,
            onNext = onGuidedTutorialNext,
            onSkip = onGuidedTutorialSkip,
            onRestart = onGuidedTutorialRestart,
            isRestart = guidedTutorial?.isRestart == true
        )
    }
}

private val rotatingPlaceholders = listOf(
    "Juan 3:16",
    "amor",
    "Salmo 23",
    "perdón",
    "fe"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchInputCard(
    query: String,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onClear: () -> Unit
) {
    var placeholderIndex by remember { mutableStateOf(0) }

    LaunchedEffect(query) {
        if (query.isEmpty()) {
            while (true) {
                delay(3500)
                placeholderIndex = (placeholderIndex + 1) % rotatingPlaceholders.size
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = {
                Text(
                    text = stringResource(
                        R.string.search_placeholder_template,
                        rotatingPlaceholders[placeholderIndex]
                    )
                )
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = BiblionGoldPrimary)
            },
            trailingIcon = {
                if (query.isNotBlank()) {
                    IconButton(onClick = onClear) {
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
                focusedLeadingIconColor = BiblionGoldPrimary,
                unfocusedLeadingIconColor = BiblionGoldPrimary
            )
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchContent(
    uiState: SearchUiState,
    currentQuery: String,
    isTyping: Boolean,
    showPopular: Boolean,
    recentSearches: List<SearchHistoryEntry>,
    showAllRecent: Boolean,
    rotationPool: List<PopularTopic>,
    onToggleShowAll: () -> Unit,
    onRecentClick: (String) -> Unit,
    onClearHistory: () -> Unit,
    onPopularClick: (PopularTopic) -> Unit,
    onClearSearch: () -> Unit,
    onResultClick: (SearchResult) -> Unit,
    onTopicVerseClick: (RelatedVerse) -> Unit = {},
    onToggleTopicExpansion: (TopicHit) -> Unit = {},
    expandedTopicSlug: String? = null,
    selectedTestament: BibleSearchTestament,
    onTestamentSelected: (BibleSearchTestament) -> Unit,
    availableBooks: List<String>,
    selectedBook: String?,
    onBookSelected: (String?) -> Unit,
    onClearFilters: () -> Unit,
    onExploreTopics: () -> Unit = {},
    onOpenDictionary: () -> Unit = {}
) {
    val displayedRecent = if (showAllRecent) recentSearches else recentSearches.take(5)
    val hasActiveFilters = selectedTestament != BibleSearchTestament.ALL || selectedBook != null

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        item {
            TestamentTabsRow(
                selected = selectedTestament,
                onSelect = onTestamentSelected
            )
        }

        item {
            BookFilterRow(
                availableBooks = availableBooks,
                selectedBook = selectedBook,
                onBookSelected = onBookSelected,
                hasActiveFilters = hasActiveFilters,
                onClearFilters = onClearFilters
            )
        }

        item {
            RecentSearchesSection(
                entries = displayedRecent,
                hasMore = recentSearches.size > 5,
                showAll = showAllRecent,
                onToggleShowAll = onToggleShowAll,
                onRecentClick = onRecentClick,
                onClearHistory = onClearHistory
            )
        }

        when (val state = uiState) {
            SearchUiState.Idle -> {
                if (showPopular) {
                    item {
                        AutoScrollingTopicCarousel(
                            pool = rotationPool,
                            onTopicClick = onPopularClick
                        )
                    }
                    item {
                        ExploreTopicsButton(
                            onClick = onExploreTopics
                        )
                    }
                    item {
                        DictionaryButton(
                            onClick = onOpenDictionary
                        )
                    }
                    item { EmptyStateHint() }
                }
            }

            SearchUiState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 160.dp)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = BiblionGoldPrimary)
                    }
                }
            }

            is SearchUiState.Empty -> {
                item { NoResultsState(query = state.query, onClear = onClearSearch) }
            }

            is SearchUiState.Error -> {
                item { ErrorState(message = state.message) }
            }

            is SearchUiState.Success -> {
                // 1) Seccion de temas relacionados (si hay)
                if (state.topicHits.isNotEmpty()) {
                    item {
                        TopicsSection(
                            topics = state.topicHits,
                            expandedSlug = expandedTopicSlug,
                            onToggleExpansion = onToggleTopicExpansion,
                            onVerseClick = onTopicVerseClick
                        )
                    }
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }
                // 2) Header de versiculos
                if (state.results.isNotEmpty()) {
                    item {
                        ResultsHeader(
                            query = currentQuery,
                            count = state.results.size,
                            filterLabel = buildFilterLabel(
                                testament = selectedTestament,
                                book = selectedBook
                            )
                        )
                    }
                    items(state.results) { result ->
                        DailyVerseCard(
                            verse = result.text,
                            reference = result.reference,
                            onClick = { onResultClick(result) }
                        )
                    }
                } else if (state.topicHits.isNotEmpty()) {
                    // Solo hay temas, no versiculos
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.search_topics_only_topics),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun buildFilterLabel(
    testament: BibleSearchTestament,
    book: String?
): String? {
    if (testament == BibleSearchTestament.ALL && book == null) return null
    val testamentLabel = when (testament) {
        BibleSearchTestament.ALL -> null
        BibleSearchTestament.OLD -> stringResource(R.string.testament_old_short_label)
        BibleSearchTestament.NEW -> stringResource(R.string.testament_new_short_label)
    }
    return listOfNotNull(testamentLabel, book).joinToString(" · ")
}

@Composable
private fun ResultsHeader(query: String, count: Int, filterLabel: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.search_results_header, query),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = pluralStringResource(R.plurals.search_results_count, count, count),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (filterLabel != null) {
                    Text(
                        text = " · $filterLabel",
                        style = MaterialTheme.typography.labelSmall,
                        color = BiblionGoldPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun NoResultsState(query: String, onClear: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(56.dp),
            color = BiblionGoldSoft.copy(alpha = 0.18f),
            shape = CircleShape
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.SearchOff,
                    contentDescription = null,
                    tint = BiblionGoldPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.search_no_results_header, query),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.search_no_results_suggestion),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = onClear) {
            Text(
                text = stringResource(R.string.search_clear_button),
                color = BiblionGoldPrimary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ErrorState(message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.search_error_message, message),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TestamentTabsRow(
    selected: BibleSearchTestament,
    onSelect: (BibleSearchTestament) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TestamentChip(
            label = stringResource(R.string.search_filter_all),
            icon = Icons.Default.AutoStories,
            selected = selected == BibleSearchTestament.ALL,
            onClick = { onSelect(BibleSearchTestament.ALL) },
            modifier = Modifier.weight(1f)
        )
        TestamentChip(
            label = stringResource(R.string.testament_old_label),
            icon = Icons.Default.Star,
            selected = selected == BibleSearchTestament.OLD,
            onClick = { onSelect(BibleSearchTestament.OLD) },
            modifier = Modifier.weight(1f)
        )
        TestamentChip(
            label = stringResource(R.string.testament_new_label),
            icon = Icons.Default.Lightbulb,
            selected = selected == BibleSearchTestament.NEW,
            onClick = { onSelect(BibleSearchTestament.NEW) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TestamentChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, maxLines = 1) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        },
        modifier = modifier
    )
}

@Composable
private fun BookFilterRow(
    availableBooks: List<String>,
    selectedBook: String?,
    onBookSelected: (String?) -> Unit,
    hasActiveFilters: Boolean,
    onClearFilters: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { menuExpanded = true },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.MenuBook,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = BiblionGoldPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = selectedBook ?: stringResource(R.string.search_filter_book_all),
                    modifier = Modifier.weight(1f),
                    color = if (selectedBook != null) BiblionGoldPrimary else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (selectedBook != null) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1
                )
                Icon(
                    Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = BiblionGoldPrimary
                )
            }

            if (hasActiveFilters) {
                IconButton(
                    onClick = onClearFilters,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.search_filter_clear_filters),
                        tint = BiblionGoldPrimary
                    )
                }
            }
        }

        androidx.compose.material3.DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
            modifier = Modifier.heightIn(max = 420.dp)
        ) {
            androidx.compose.material3.DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.search_filter_book_all),
                        fontWeight = if (selectedBook == null) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                onClick = {
                    onBookSelected(null)
                    menuExpanded = false
                },
                leadingIcon = if (selectedBook == null) {
                    { Icon(Icons.Default.Check, contentDescription = null, tint = BiblionGoldPrimary) }
                } else null
            )
            availableBooks.forEach { book ->
                androidx.compose.material3.DropdownMenuItem(
                    text = {
                        Text(
                            text = book,
                            fontWeight = if (selectedBook == book) FontWeight.SemiBold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onBookSelected(book)
                        menuExpanded = false
                    },
                    leadingIcon = if (selectedBook == book) {
                        { Icon(Icons.Default.Check, contentDescription = null, tint = BiblionGoldPrimary) }
                    } else null
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecentSearchesSection(
    entries: List<SearchHistoryEntry>,
    hasMore: Boolean,
    showAll: Boolean,
    onToggleShowAll: () -> Unit,
    onRecentClick: (String) -> Unit,
    onClearHistory: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.History,
                    contentDescription = null,
                    tint = BiblionGoldPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.search_recent_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (entries.isNotEmpty() && hasMore) {
                TextButton(onClick = onToggleShowAll) {
                    Text(
                        text = if (showAll) "Ver menos" else stringResource(R.string.search_recent_see_all),
                        color = BiblionGoldPrimary,
                        style = MaterialTheme.typography.labelMedium
                    )
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = BiblionGoldPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (entries.isEmpty()) {
            Text(
                text = stringResource(R.string.search_recent_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                entries.forEach { entry ->
                    AssistChip(
                        onClick = { onRecentClick(entry.query) },
                        label = { Text(entry.query, maxLines = 1) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.History,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = BiblionGoldPrimary
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            labelColor = MaterialTheme.colorScheme.onSurface
                        ),
                        border = AssistChipDefaults.assistChipBorder(
                            enabled = true,
                            borderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                }
            }
            if (entries.isNotEmpty() && showAll) {
                Spacer(modifier = Modifier.height(4.dp))
                TextButton(
                    onClick = onClearHistory,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = stringResource(R.string.search_recent_clear),
                        color = BiblionGoldPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyStateHint() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 200.dp)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "📖",
            style = MaterialTheme.typography.displayMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.search_empty_state_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.search_empty_state_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ExploreTopicsButton(onClick: () -> Unit) {
    androidx.compose.material3.OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 8.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
    ) {
        Icon(
            Icons.Default.GridView,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = BiblionGoldPrimary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.search_explore_topics),
            color = BiblionGoldPrimary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun DictionaryButton(onClick: () -> Unit) {
    val blueColor = Color(0xFF1976D2)
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(
            Icons.Default.MenuBook,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = blueColor
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.drawer_dictionary),
            color = blueColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}

package com.cristiancogollo.biblion.feature.dictionary.ui

import android.content.Context
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.cristiancogollo.biblion.R
import com.cristiancogollo.biblion.feature.dictionary.data.DictionaryCategory
import com.cristiancogollo.biblion.feature.dictionary.data.DictionaryEntry
import com.cristiancogollo.biblion.feature.dictionary.data.DictionaryRepository
import com.cristiancogollo.biblion.navigateSingleTop
import com.cristiancogollo.biblion.ui.theme.BiblionNavy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Estado UI para la pantalla de detalle de un termino del diccionario.
 */
sealed interface DictionaryEntryDetailUiState {
    data object Loading : DictionaryEntryDetailUiState
    data class Success(val entry: DictionaryEntry) : DictionaryEntryDetailUiState
    data class Error(val message: String) : DictionaryEntryDetailUiState
    data object NotFound : DictionaryEntryDetailUiState
}

/**
 * ViewModel para cargar una entrada del diccionario por id.
 */
class DictionaryEntryDetailViewModel(
    private val entryId: Long,
    private val appContext: Context,
) : ViewModel() {

    private val _state = MutableStateFlow<DictionaryEntryDetailUiState>(DictionaryEntryDetailUiState.Loading)
    val state: StateFlow<DictionaryEntryDetailUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = DictionaryEntryDetailUiState.Loading
        viewModelScope.launch {
            val entry = runCatching {
                DictionaryRepository.getEntryById(appContext, entryId)
            }.getOrNull()
            _state.value = if (entry != null) {
                DictionaryEntryDetailUiState.Success(entry)
            } else {
                DictionaryEntryDetailUiState.NotFound
            }
        }
    }

    class Factory(
        private val entryId: Long,
        private val appContext: Context,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DictionaryEntryDetailViewModel(entryId, appContext) as T
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun DictionaryEntryDetailScreen(
    navController: NavController,
    entryId: Long,
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val viewModel: DictionaryEntryDetailViewModel = viewModel(
        factory = DictionaryEntryDetailViewModel.Factory(entryId = entryId, appContext = appContext)
    )
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = (state as? DictionaryEntryDetailUiState.Success)?.entry?.term
                            ?: stringResource(R.string.search_section_dictionary_title),
                        color = BiblionNavy,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                            tint = BiblionNavy,
                        )
                    }
                },
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (val current = state) {
                DictionaryEntryDetailUiState.Loading -> LoadingState()
                is DictionaryEntryDetailUiState.Error -> ErrorState(current.message)
                DictionaryEntryDetailUiState.NotFound -> NotFoundState()
                is DictionaryEntryDetailUiState.Success -> EntryDetail(
                    entry = current.entry,
                    onOpenReference = { ref ->
                        val route = com.cristiancogollo.biblion.Screen.Reader.createRoute(
                            bookName = ref.book,
                            chapter = ref.chapter,
                            verse = ref.verse?.toString(),
                        )
                        navController.navigateSingleTop(route)
                    },
                )
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(message, color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun NotFoundState() {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text("Termino no encontrado", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EntryDetail(
    entry: DictionaryEntry,
    onOpenReference: (Reference) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Text(
            text = entry.term,
            style = MaterialTheme.typography.headlineMedium,
            color = BiblionNavy,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        AssistChip(
            onClick = {},
            label = { Text(entry.category.displayName) },
            colors = AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = entry.definition,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )

        MetadataSection(entry = entry)

        if (entry.references.isNotEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Referencias",
                style = MaterialTheme.typography.titleMedium,
                color = BiblionNavy,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                entry.references.forEach { refText ->
                    val parsed = parseReference(refText)
                    AssistChip(
                        onClick = { if (parsed != null) onOpenReference(parsed) },
                        label = { Text(refText) },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.AutoStories,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun MetadataSection(entry: DictionaryEntry) {
    val showMetadata = entry.displayTitle != null
        || entry.gender != null
        || entry.birthYear != null
        || entry.deathYear != null
        || entry.latitude != null
        || entry.longitude != null
        || !entry.aliases.isNullOrBlank()
        || entry.featureType != null

    if (!showMetadata) return

    Spacer(modifier = Modifier.height(16.dp))
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    Spacer(modifier = Modifier.height(12.dp))

    val rows = buildList {
        entry.displayTitle?.let { add("Titulo" to it) }
        entry.gender?.let { add("Genero" to it) }
        entry.birthYear?.let { add("Nacimiento" to formatYear(it)) }
        entry.deathYear?.let { add("Fallecimiento" to formatYear(it)) }
        entry.featureType?.let { add("Tipo" to translateFeatureType(it)) }
        if (entry.latitude != null && entry.longitude != null) {
            add("Ubicacion" to "${entry.latitude}, ${entry.longitude}")
        }
        entry.aliases?.takeIf { it.isNotBlank() }?.let { add("Alias" to it) }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            rows.forEach { (label, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                ) {
                    Text(
                        text = "$label:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

private fun formatYear(isoYear: String): String {
    return try {
        val year = isoYear.toInt()
        when {
            year < 0 -> "${-year} a.C."
            year == 0 -> "1 a.C."
            else -> "$year d.C."
        }
    } catch (e: Exception) {
        isoYear
    }
}

private fun translateFeatureType(type: String): String = when (type.lowercase()) {
    "city" -> "Ciudad"
    "region" -> "Region"
    "country" -> "Pais"
    "mountain" -> "Monte"
    "river" -> "Rio"
    "sea" -> "Mar"
    "lake" -> "Lago"
    "desert" -> "Desierto"
    "valley" -> "Valle"
    "island" -> "Isla"
    else -> type.replaceFirstChar { it.uppercase() }
}

data class Reference(val book: String, val chapter: Int, val verse: Int?)

/**
 * Parsea una referencia biblica simple como "Genesis 1:1" o "Mateo 5:3-12".
 * Devuelve null si el texto no encaja con un formato conocido.
 */
private fun parseReference(text: String): Reference? {
    val parts = text.trim().split(" ", limit = 2)
    if (parts.size < 2) return null
    val book = parts[0]
    val chapterVerse = parts[1].split(":")
    if (chapterVerse.size < 2) return null
    val chapter = chapterVerse[0].toIntOrNull() ?: return null
    val verse = chapterVerse[1].split("-").first().toIntOrNull()
    return Reference(book = book, chapter = chapter, verse = verse)
}

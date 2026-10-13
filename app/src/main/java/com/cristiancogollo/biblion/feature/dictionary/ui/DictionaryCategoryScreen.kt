package com.cristiancogollo.biblion.feature.dictionary.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.cristiancogollo.biblion.R
import com.cristiancogollo.biblion.Screen
import com.cristiancogollo.biblion.navigateSingleTop
import com.cristiancogollo.biblion.feature.dictionary.data.DictionaryCategory
import com.cristiancogollo.biblion.feature.dictionary.data.DictionaryRepository
import com.cristiancogollo.biblion.feature.dictionary.data.DictionarySearchResultUi
import com.cristiancogollo.biblion.ui.theme.BiblionBluePrimary
import com.cristiancogollo.biblion.ui.theme.BiblionNavy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

private val categoryColors = mapOf(
    DictionaryCategory.PERSON to Color(0xFF8D6E63),
    DictionaryCategory.PLACE to Color(0xFF4CAF50),
    DictionaryCategory.CONCEPT to Color(0xFF1976D2),
    DictionaryCategory.OBJECT to Color(0xFFFF9800),
    DictionaryCategory.PRACTICE to Color(0xFF7B1FA2),
    DictionaryCategory.EVENT to Color(0xFFD32F2F),
    DictionaryCategory.BOOK to Color(0xFF5D4037),
    DictionaryCategory.OTHER to Color(0xFF607D8B)
)

private val categoryLabelRes = mapOf(
    DictionaryCategory.PERSON to R.string.dictionary_category_person,
    DictionaryCategory.PLACE to R.string.dictionary_category_place,
    DictionaryCategory.CONCEPT to R.string.dictionary_category_concept,
    DictionaryCategory.OBJECT to R.string.dictionary_category_object,
    DictionaryCategory.PRACTICE to R.string.dictionary_category_practice,
    DictionaryCategory.EVENT to R.string.dictionary_category_event,
    DictionaryCategory.BOOK to R.string.dictionary_category_book,
    DictionaryCategory.OTHER to R.string.dictionary_category_other
)

private fun String.normalizeForBookOrder(): String {
    return this.lowercase()
        .replace("á", "a").replace("é", "e").replace("í", "i")
        .replace("ó", "o").replace("ú", "u").replace("ñ", "n")
}

private val canonicalBookOrder = listOf(
    "Genesis", "Exodo", "Levitico", "Numeros", "Deuteronomio",
    "Josue", "Jueces", "Rut", "1 Samuel", "2 Samuel",
    "1 Reyes", "2 Reyes", "1 Cronicas", "2 Cronicas",
    "Esdras", "Nehemias", "Ester", "Job", "Salmos",
    "Proverbios", "Eclesiastes", "Cantares",
    "Isaias", "Jeremias", "Lamentaciones", "Ezequiel", "Daniel",
    "Oseas", "Joel", "Amos", "Abdias", "Jonas", "Miqueas",
    "Nahum", "Habacuc", "Sofonias", "Hageo", "Zacarias", "Malaquias",
    "Mateo", "Marcos", "Lucas", "Juan", "Hechos",
    "Romanos", "1 Corintios", "2 Corintios", "Galatas", "Efesios",
    "Filipenses", "Colosenses", "1 Tesalonicenses", "2 Tesalonicenses",
    "1 Timoteo", "2 Timoteo", "Tito", "Filemon", "Hebreos",
    "Santiago", "1 Pedro", "2 Pedro", "1 Juan", "2 Juan", "3 Juan",
    "Judas", "Apocalipsis"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionaryCategoryScreen(
    navController: NavController,
    category: String
) {
    val context = LocalContext.current
    val catEnum = DictionaryCategory.entries.find {
        it.name.equals(category, ignoreCase = true)
    } ?: DictionaryCategory.OTHER
    val catColor = categoryColors[catEnum] ?: Color(0xFF607D8B)
    val catLabel = stringResource(categoryLabelRes[catEnum] ?: R.string.dictionary_category_other)

    var query by remember { mutableStateOf("") }
    var entries by remember { mutableStateOf<List<DictionarySearchResultUi>>(emptyList()) }
    var allCategoryEntries by remember(catEnum) {
        mutableStateOf<List<DictionarySearchResultUi>?>(null)
    }
    var isLoading by remember { mutableStateOf(true) }
    var loadFailed by remember { mutableStateOf(false) }
    var retryCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(query, catEnum, retryCount) {
        isLoading = true
        loadFailed = false
        if (query.trim().length >= 2) delay(400L)
        try {
            val results = if (query.trim().length >= 2) {
                DictionaryRepository.searchEntriesOrThrow(context, query)
                    .filter { it.category == catEnum }
            } else {
                allCategoryEntries ?: DictionaryRepository
                    .getEntriesByCategoryOrThrow(context, catEnum)
                    .map {
                        DictionarySearchResultUi(
                            id = it.id,
                            term = it.term,
                            definitionPreview = it.definition,
                            category = catEnum,
                            rank = 0.0,
                        )
                    }.also { allCategoryEntries = it }
            }
            entries = if (catEnum == DictionaryCategory.BOOK) {
                results.sortedBy { entry ->
                    canonicalBookOrder.indexOfFirst {
                        it.normalizeForBookOrder() == entry.term.normalizeForBookOrder()
                    }.let { if (it == -1) Int.MAX_VALUE else it }
                }
            } else results
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            entries = emptyList()
            loadFailed = true
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = catLabel,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = catColor.copy(alpha = 0.08f)
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                },
                placeholder = {
                    Text(
                        text = stringResource(R.string.dictionary_screen_search_hint),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                trailingIcon = if (query.isNotEmpty()) {
                    {
                        IconButton(onClick = {
                            query = ""
                        }) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_clear))
                        }
                    }
                } else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = catColor,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                ),
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = catColor)
                }
            } else if (loadFailed) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(stringResource(R.string.dictionary_search_error))
                    TextButton(onClick = {
                        allCategoryEntries = null
                        retryCount++
                    }) {
                        Text(stringResource(R.string.action_retry))
                    }
                }
            } else if (entries.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.SearchOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.dictionary_browse_no_results),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.dictionary_browse_results_count, entries.size),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                )
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(entries, key = { it.id }) { entry ->
                        DictionaryCategoryResultCard(
                            entry = entry,
                            color = catColor,
                            onClick = {
                                navController.navigateSingleTop(
                                    Screen.DictionaryEntry.createRoute(entry.id)
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DictionaryCategoryResultCard(
    entry: DictionarySearchResultUi,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.MenuBook,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.term,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.size(4.dp))
                Text(
                    text = entry.definitionPreview,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 4,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

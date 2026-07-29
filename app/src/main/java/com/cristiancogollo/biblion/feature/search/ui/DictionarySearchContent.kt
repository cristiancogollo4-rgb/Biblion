package com.cristiancogollo.biblion.feature.search.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter


/**
 * Vista de busqueda para el ambito de diccionario biblico.
 *
 * Patron de busqueda en vivo:
 *  - MutableStateFlow(query) + collectLatest + debounce(400ms) + filter(length >= 2) + distinctUntilChanged
 *  - Cancela la corutina anterior cuando llega un nuevo query
 *  - Render reactivo segun resultados de DictionaryRepository.searchEntriesFlow
 */
@OptIn(FlowPreview::class, ExperimentalLayoutApi::class)
@Composable
fun DictionarySearchContent(
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val queryFlow = remember { MutableStateFlow("") }
    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<DictionaryCategory?>(null) }
    var results by remember { mutableStateOf<List<DictionarySearchResultUi>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        queryFlow
            .debounce(400L)
            .filter { it.trim().length >= 2 }
            .distinctUntilChanged()
            .collectLatest { q ->
                isLoading = true
                runCatching {
                    DictionaryRepository.searchEntries(context, q)
                        .let { entries ->
                            val filtered = if (selectedCategory == null) entries
                            else entries.filter { it.category == selectedCategory }
                            results = filtered
                        }
                }.onFailure {
                    results = emptyList()
                }
                isLoading = false
            }
    }

    // Cuando cambia la categoria, refiltrar localmente los resultados actuales
    LaunchedEffect(selectedCategory, results) {
        if (query.trim().length < 2) {
            return@LaunchedEffect
        }
        val baseQuery = query
        if (baseQuery.isNotBlank()) {
            isLoading = true
            runCatching {
                val entries = DictionaryRepository.searchEntries(context, baseQuery)
                results = if (selectedCategory == null) entries
                else entries.filter { it.category == selectedCategory }
            }
            isLoading = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                queryFlow.value = it
            },
            placeholder = {
                Text(
                    text = stringResource(R.string.search_section_dictionary_hint),
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
                        queryFlow.value = ""
                        results = emptyList()
                    }) {
                        Icon(Icons.Filled.Close, contentDescription = "Limpiar")
                    }
                }
            } else null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BiblionBluePrimary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            ),
        )

        Spacer(modifier = Modifier.height(12.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { selectedCategory = null },
                label = { Text(stringResource(R.string.search_dictionary_all_categories)) },
            )
            DictionaryCategory.entries.forEach { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                    label = { Text(cat.displayName) },
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        val letters = ('A'..'Z').toList()
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth().heightIn(min = 36.dp),
        ) {
            items(letters) { letter ->
                AssistChip(
                    onClick = {
                        val newQuery = letter.toString()
                        query = newQuery
                        queryFlow.value = newQuery
                    },
                    label = { Text(letter.toString(), fontSize = 12.sp) },
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else if (query.trim().length < 2) {
            EmptyHint()
        } else if (results.isEmpty()) {
            EmptyState()
        } else {
            DictResultsHeader(count = results.size)
            Spacer(modifier = Modifier.height(8.dp))
            results.forEach { entry ->
                DictionaryResultCard(
                    entry = entry,
                    onClick = {
                        navController.navigateSingleTop(
                            Screen.DictionaryEntry.createRoute(entry.id)
                        )
                    },
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun DictResultsHeader(count: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    ) {
        Text(
            text = stringResource(R.string.search_section_dictionary_title),
            style = MaterialTheme.typography.titleMedium,
            color = BiblionNavy,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = stringResource(R.string.search_dictionary_count, count),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DictionaryResultCard(
    entry: DictionarySearchResultUi,
    onClick: () -> Unit,
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
                tint = BiblionBluePrimary,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entry.term,
                        style = MaterialTheme.typography.titleMedium,
                        color = BiblionNavy,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    ) {
                        Text(
                            entry.category.displayName,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 10.sp,
                        )
                    }
                }
                Spacer(modifier = Modifier.size(4.dp))
                Text(
                    text = entry.definitionPreview,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
            Spacer(modifier = Modifier.size(4.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmptyHint() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp),
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = stringResource(R.string.search_section_dictionary_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.SearchOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp),
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = stringResource(R.string.search_section_dictionary_empty),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}


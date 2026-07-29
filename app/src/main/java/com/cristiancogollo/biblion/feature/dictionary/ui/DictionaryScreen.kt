package com.cristiancogollo.biblion.feature.dictionary.ui

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextOverflow
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

private data class CategoryCardInfo(
    val category: DictionaryCategory,
    val labelRes: Int,
    val descRes: Int,
    val color: Color,
    val count: Int
)

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

private val categoryDescRes = mapOf(
    DictionaryCategory.PERSON to R.string.dictionary_category_person_desc,
    DictionaryCategory.PLACE to R.string.dictionary_category_place_desc,
    DictionaryCategory.CONCEPT to R.string.dictionary_category_concept_desc,
    DictionaryCategory.OBJECT to R.string.dictionary_category_object_desc,
    DictionaryCategory.PRACTICE to R.string.dictionary_category_practice_desc,
    DictionaryCategory.EVENT to R.string.dictionary_category_event_desc,
    DictionaryCategory.BOOK to R.string.dictionary_category_book_desc,
    DictionaryCategory.OTHER to R.string.dictionary_category_other_desc
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionaryScreen(navController: NavController) {
    val context = LocalContext.current
    var categories by remember { mutableStateOf<List<CategoryCardInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val counts = DictionaryRepository.getCategoryCounts(context)
        val countMap = counts.toMap()
        categories = DictionaryCategory.entries.map { cat ->
            CategoryCardInfo(
                category = cat,
                labelRes = categoryLabelRes[cat] ?: R.string.dictionary_category_other,
                descRes = categoryDescRes[cat] ?: R.string.dictionary_category_other_desc,
                color = categoryColors[cat] ?: Color(0xFF607D8B),
                count = countMap[cat] ?: 0
            )
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.dictionary_screen_title),
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
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Text(
                text = stringResource(R.string.dictionary_screen_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )

            DictionarySearchBar(
                onResultClick = { entryId ->
                    navController.navigateSingleTop(
                        Screen.DictionaryEntry.createRoute(entryId)
                    )
                }
            )

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(
                        items = categories,
                        key = { it.category.name }
                    ) { catInfo ->
                        DictionaryCategoryCard(
                            info = catInfo,
                            onClick = {
                                navController.navigate(
                                    Screen.ExploreDictionaryCategory.createRoute(catInfo.category.name)
                                )
                            }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@OptIn(FlowPreview::class, ExperimentalLayoutApi::class)
@Composable
private fun DictionarySearchBar(
    onResultClick: (Long) -> Unit
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
                    DictionaryRepository.searchEntries(context, q).let { entries ->
                        results = if (selectedCategory == null) entries
                        else entries.filter { it.category == selectedCategory }
                    }
                }.onFailure { results = emptyList() }
                isLoading = false
            }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                queryFlow.value = it
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
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            ),
        )

        if (query.trim().length >= 2) {
            Spacer(modifier = Modifier.height(8.dp))

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
                        onClick = {
                            selectedCategory = if (selectedCategory == cat) null else cat
                            queryFlow.value = query
                        },
                        label = { Text(cat.displayName) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    }
                }
                results.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Filled.SearchOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.search_section_dictionary_empty),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                else -> {
                    Text(
                        text = stringResource(R.string.dictionary_browse_results_count, results.size),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                    )
                    results.forEach { entry ->
                        DictionaryResultCard(
                            entry = entry,
                            onClick = { onResultClick(entry.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DictionaryCategoryCard(
    info: CategoryCardInfo,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = info.color.copy(alpha = 0.10f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(14.dp)
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                color = info.color.copy(alpha = 0.20f),
                shape = CircleShape
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(info.labelRes).take(1).uppercase(),
                        color = info.color,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(info.labelRes),
                    color = info.color,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(info.descRes),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.dictionary_category_entries, info.count),
                    color = info.color,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = info.color
            )
        }
    }
}

@Composable
private fun DictionaryResultCard(
    entry: DictionarySearchResultUi,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
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
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entry.term,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primaryContainer,
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

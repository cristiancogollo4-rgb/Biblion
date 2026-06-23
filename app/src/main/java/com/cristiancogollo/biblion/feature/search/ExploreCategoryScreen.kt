package com.cristiancogollo.biblion.feature.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.cristiancogollo.biblion.R
import com.cristiancogollo.biblion.Screen
import com.cristiancogollo.biblion.feature.bibi.TopicDatabase
import com.cristiancogollo.biblion.feature.bibi.TopicEntity
import com.cristiancogollo.biblion.ui.theme.BiblionGoldPrimary

/**
 * Pantalla que muestra los temas de una categoria especifica con un
 * buscador en vivo. Cada tema se muestra como una card expandible que
 * carga y muestra los versiculos relacionados al expandirla, igual
 * que en el buscador principal.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreCategoryScreen(
    navController: NavController,
    category: String
) {
    val context = LocalContext.current
    val label = CategoryLabels.label(category)

    var allTopics by remember { mutableStateOf<List<TopicEntity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var query by remember { mutableStateOf("") }
    var expandedSlug by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(category) {
        allTopics = TopicDatabase.getInstance(context).topicDao()
            .getTopicsByCategory(category)
        isLoading = false
    }

    val filtered by remember(allTopics, query) {
        derivedStateOf {
            if (query.isBlank()) allTopics
            else {
                val q = query.trim().lowercase()
                allTopics.filter { t ->
                    t.nameEs.lowercase().contains(q) ||
                        t.nameEn.lowercase().contains(q) ||
                        t.description.lowercase().contains(q)
                }
            }
        }
    }

    val listState = rememberLazyListState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = label,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${allTopics.size} temas",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(stringResource(R.string.explore_category_search_hint)) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = BiblionGoldPrimary
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = null
                            )
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BiblionGoldPrimary,
                    unfocusedBorderColor = BiblionGoldPrimary.copy(alpha = 0.3f)
                )
            )

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = BiblionGoldPrimary)
                }
            } else if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.explore_category_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(
                        items = filtered,
                        key = { it.slug }
                    ) { topic ->
                        ExpandableTopicCard(
                            topic = TopicHit(
                                slug = topic.slug,
                                nameEs = topic.nameEs,
                                nameEn = topic.nameEn,
                                description = topic.description,
                                category = topic.category,
                                verseCount = topic.verseCount
                            ),
                            expanded = topic.slug == expandedSlug,
                            onToggleExpansion = {
                                expandedSlug = if (expandedSlug == topic.slug) null else topic.slug
                            },
                            onVerseClick = { verse ->
                                navController.navigate(
                                    Screen.Reader.createRoute(
                                        bookName = verse.book,
                                        chapter = verse.chapter,
                                        verse = verse.verseStart.toString()
                                    )
                                )
                            },
                            topicColor = Color(CategoryLabels.color(topic.category).toArgb())
                        )
                    }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }
}

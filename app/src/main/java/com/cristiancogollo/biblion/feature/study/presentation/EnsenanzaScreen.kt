package com.cristiancogollo.biblion

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.cristiancogollo.biblion.ui.theme.BiblionBluePrimary
import com.cristiancogollo.biblion.ui.theme.BiblionGoldPrimary
import com.cristiancogollo.biblion.ui.theme.BiblionGoldSoft
import com.cristiancogollo.biblion.ui.theme.BiblionNavy
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnsenanzaScreen(navController: NavController) {
    val viewModel: StudyViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }
    val json = remember { Json { ignoreUnknownKeys = true; classDiscriminator = "nodeType" } }
    var metadataStudy by remember { mutableStateOf<StudyEntity?>(null) }
    var metadataTitle by remember { mutableStateOf("") }
    var metadataTagsInput by remember { mutableStateOf("") }
    var metadataError by remember { mutableStateOf<String?>(null) }
    var filterInput by remember { mutableStateOf("") }
    val visibleStudies = remember(state.allStudies, filterInput) {
        val query = filterInput.trim().lowercase()
        state.allStudies.map { study ->
            study to buildStudyPreview(study.contentSerialized, json)
        }.filter { (study, preview) ->
            query.isBlank() ||
                study.title.lowercase().contains(query) ||
                preview.tags.any { tag -> tag.lowercase().contains(query) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Enseñanzas", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    navController.navigateSingleTop(Screen.Reader.createRoute(studyMode = true))
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nueva Enseñanza")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (state.allStudies.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No tienes enseñanzas guardadas aún.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = filterInput,
                        onValueChange = { filterInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null)
                        },
                        label = { Text("Filtrar") },
                        placeholder = { Text("Titulo o etiqueta") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BiblionNavy,
                            unfocusedBorderColor = BiblionGoldPrimary,
                            focusedLabelColor = BiblionNavy,
                            cursorColor = BiblionNavy
                        )
                    )
                }
                if (visibleStudies.isEmpty()) {
                    item {
                        Text(
                            text = "No hay ensenanzas que coincidan con el filtro.",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                items(visibleStudies, key = { it.first.id }) { (study, _) ->
                    EnsenanzaCard(
                        study = study,
                        dateText = dateFormat.format(Date(study.updatedAt)),
                        onOpen = {
                            navController.navigateSingleTop(Screen.StudyRead.createRoute(study.id))
                        },
                        onEdit = {
                            val preferredBook = viewModel.preferredBookForStudy(study)
                            navController.navigateSingleTop(
                                Screen.Reader.createRoute(
                                    bookName = preferredBook,
                                    studyMode = true,
                                    studyId = study.id
                                )
                            )
                        },
                        onDelete = {
                            viewModel.process(StudyIntent.DeleteStudy(study.id))
                        },
                        onEditMetadata = {
                            val preview = buildStudyPreview(study.contentSerialized, json)
                            metadataStudy = study
                            metadataTitle = study.title
                            metadataTagsInput = preview.tags.joinToString(", ")
                            metadataError = null
                        }
                    )
                }
            }
        }
    }

    metadataStudy?.let { study ->
        AlertDialog(
            onDismissRequest = { metadataStudy = null },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text("Editar título y etiquetas") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = metadataTitle,
                        onValueChange = {
                            metadataTitle = it
                            metadataError = null
                        },
                        singleLine = true,
                        label = { Text("Título") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BiblionNavy,
                            unfocusedBorderColor = BiblionGoldPrimary,
                            focusedLabelColor = BiblionNavy,
                            cursorColor = BiblionNavy
                        )
                    )
                    StudyTagSelector(
                        value = metadataTagsInput,
                        onValueChange = {
                            metadataTagsInput = it
                            metadataError = null
                        }
                    )
                    metadataError?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                    val cleanTitle = metadataTitle.trim()
                    val cleanTags = parseStudyTags(metadataTagsInput)
                    val tagError = validateRequiredStudyTags(cleanTags)
                    metadataError = when {
                        cleanTitle.isBlank() -> "El título es obligatorio."
                        tagError != null -> tagError
                        else -> null
                    }
                    if (metadataError == null) {
                        viewModel.updateStudyMetadata(study.id, cleanTitle, cleanTags)
                        metadataStudy = null
                    }
                },
                    colors = ButtonDefaults.textButtonColors(contentColor = BiblionGoldPrimary)
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { metadataStudy = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = BiblionBluePrimary)
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun EnsenanzaCard(
    study: StudyEntity,
    dateText: String,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onEditMetadata: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onOpen() }
            ) {
                Text(
                    text = study.title.ifBlank { "Sin título" },
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Última edición: $dateText",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
            }
            Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Más opciones",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    containerColor = MaterialTheme.colorScheme.surface,
                    offset = DpOffset(x = 0.dp, y = 6.dp) // aparece debajo del icono
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(onClick = {
                            menuExpanded = false
                            onEdit()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Editar",
                                tint = BiblionGoldPrimary
                            )
                        }

                        IconButton(onClick = {
                            menuExpanded = false
                            onEditMetadata()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Configurar",
                                tint = BiblionGoldPrimary
                            )
                        }

                        IconButton(onClick = {
                            menuExpanded = false
                            onDelete()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Eliminar",
                                tint = BiblionGoldPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class StudyPreview(
    val content: String,
    val tags: List<String>
)

private fun buildStudyPreview(serialized: String, json: Json): StudyPreview {
    val doc = runCatching { json.decodeFromString<SerializedStudyDocument>(serialized) }.getOrNull()
    val html = doc?.blocks?.filterIsInstance<StudyBlockNode.RichText>()?.firstOrNull()?.html.orEmpty()
    val plainContent = html
        .replace(Regex("<[^>]*>"), " ")
        .replace("&nbsp;", " ")
        .replace(Regex("\\s+"), " ")
        .trim()
    return StudyPreview(
        content = plainContent,
        tags = doc?.tags ?: emptyList()
    )
}

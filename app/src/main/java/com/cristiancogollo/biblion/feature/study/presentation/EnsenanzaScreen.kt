package com.cristiancogollo.biblion

import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilePresent
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
import java.io.File
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnsenanzaScreen(navController: NavController) {
    val context = LocalContext.current
    val viewModel: StudyViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }
    val json = remember { StudyViewModel.sharedJson }
    var metadataStudy by remember { mutableStateOf<StudyEntity?>(null) }
    var metadataTitle by remember { mutableStateOf("") }
    var metadataTagsInput by remember { mutableStateOf("") }
    var metadataError by remember { mutableStateOf<String?>(null) }
    var titleFilterInput by remember { mutableStateOf("") }
    var selectedTagFilters by remember { mutableStateOf<Set<String>>(emptySet()) }
    val studiesWithPreview = remember(state.allStudies) {
        state.allStudies.map { study ->
            study to buildStudyPreview(study.contentSerialized, json)
        }
    }
    val tagFilterGroups = remember(studiesWithPreview) {
        buildTeachingTagFilterGroups(studiesWithPreview.flatMap { (_, preview) -> preview.tags })
    }
    val visibleStudies = remember(studiesWithPreview, titleFilterInput, selectedTagFilters, tagFilterGroups) {
        val titleQuery = titleFilterInput.trim().lowercase()
        studiesWithPreview.filter { (study, preview) ->
            val matchesTitle = titleQuery.isBlank() || study.title.lowercase().contains(titleQuery)
            val matchesTags = matchesSelectedTeachingTags(
                studyTags = preview.tags,
                selectedTags = selectedTagFilters,
                tagGroups = tagFilterGroups
            )
            matchesTitle && matchesTags
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Enseñanzas", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStackOrNavigateHome() }) {
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
        if (state.isStudiesLoading) {
            TeachingListLoadingSkeleton(contentPadding = padding)
        } else if (state.allStudies.isEmpty()) {
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
                    TeachingFilters(
                        titleQuery = titleFilterInput,
                        onTitleQueryChange = { titleFilterInput = it },
                        tagGroups = tagFilterGroups,
                        selectedTags = selectedTagFilters,
                        onTagToggled = { tag ->
                            selectedTagFilters = if (tag in selectedTagFilters) {
                                selectedTagFilters - tag
                            } else {
                                selectedTagFilters + tag
                            }
                        },
                        onClearTags = { selectedTagFilters = emptySet() }
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
                items(visibleStudies, key = { it.first.id }) { (study, preview) ->
                    EnsenanzaCard(
                        study = study,
                        previewText = preview.content,
                        tags = preview.tags,
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
                        },
                        onShareText = {
                            shareStudyText(context, study, json)
                        },
                        onShareBiblion = {
                            shareStudyBiblionFile(context, study)
                        },
                        onSharePdf = {
                            shareStudyPdf(context, study, json)
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
private fun TeachingFilters(
    titleQuery: String,
    onTitleQueryChange: (String) -> Unit,
    tagGroups: List<TeachingTagFilterGroup>,
    selectedTags: Set<String>,
    onTagToggled: (String) -> Unit,
    onClearTags: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, BiblionGoldSoft.copy(alpha = 0.38f)),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = titleQuery,
                onValueChange = onTitleQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                label = { Text("Buscar por titulo") },
                placeholder = { Text("Nombre de la ensenanza") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BiblionNavy,
                    unfocusedBorderColor = BiblionGoldPrimary,
                    focusedLabelColor = BiblionNavy,
                    cursorColor = BiblionNavy
                )
            )

            if (tagGroups.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(end = 8.dp)
                ) {
                    items(tagGroups, key = { it.title }) { group ->
                        TeachingTagFilterMenuButton(
                            group = group,
                            selectedTags = selectedTags,
                            onTagToggled = onTagToggled
                        )
                    }
                    if (selectedTags.isNotEmpty()) {
                        item {
                            TextButton(
                                onClick = onClearTags,
                                colors = ButtonDefaults.textButtonColors(contentColor = BiblionGoldPrimary)
                            ) {
                                Text("Limpiar")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TeachingTagFilterMenuButton(
    group: TeachingTagFilterGroup,
    selectedTags: Set<String>,
    onTagToggled: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedCount = group.tags.count { it in selectedTags }
    val hasSelection = selectedCount > 0

    Box {
        OutlinedButton(
            onClick = { expanded = true },
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(
                width = 1.dp,
                color = if (hasSelection) BiblionGoldPrimary else BiblionGoldSoft.copy(alpha = 0.52f)
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (hasSelection) BiblionBluePrimary else MaterialTheme.colorScheme.surface,
                contentColor = if (hasSelection) MaterialTheme.colorScheme.onPrimary else BiblionBluePrimary
            ),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = if (hasSelection) "${group.title} ($selectedCount)" else group.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription = null
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 320.dp),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            group.tags.forEach { tag ->
                val checked = tag in selectedTags
                DropdownMenuItem(
                    text = {
                        Text(
                            text = tag,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    onClick = { onTagToggled(tag) },
                    leadingIcon = {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = null,
                            colors = CheckboxDefaults.colors(
                                checkedColor = BiblionBluePrimary,
                                uncheckedColor = BiblionGoldPrimary,
                                checkmarkColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                )
            }
        }
    }
}

internal data class TeachingTagFilterGroup(
    val title: String,
    val tags: List<String>
)

internal fun buildTeachingTagFilterGroups(tags: List<String>): List<TeachingTagFilterGroup> {
    val availableTags = tags
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .sorted()
    val standardGroups = suggestedStudyTagGroups.map { group ->
        TeachingTagFilterGroup(group.title, group.tags)
    }
    val customTags = availableTags.filterNot { tag ->
        suggestedStudyTagGroups.any { group -> tag in group.tags }
    }
    return if (customTags.isEmpty()) {
        standardGroups
    } else {
        standardGroups + TeachingTagFilterGroup("Personalizadas", customTags)
    }
}

internal fun matchesSelectedTeachingTags(
    studyTags: List<String>,
    selectedTags: Set<String>,
    tagGroups: List<TeachingTagFilterGroup>
): Boolean {
    if (selectedTags.isEmpty()) return true
    val studyTagSet = studyTags.toSet()
    val selectedByGroup = tagGroups.mapNotNull { group ->
        group.tags.filter { it in selectedTags }.takeIf { it.isNotEmpty() }
    }
    val groupedSelectedTags = selectedByGroup.flatten().toSet()
    val ungroupedSelectedTags = selectedTags - groupedSelectedTags
    return selectedByGroup.all { groupSelectedTags ->
        groupSelectedTags.any { it in studyTagSet }
    } && ungroupedSelectedTags.all { it in studyTagSet }
}

@Composable
private fun TeachingListLoadingSkeleton(contentPadding: PaddingValues) {
    val transition = rememberInfiniteTransition(label = "teachings-shimmer")
    val shimmerAlpha by transition.animateFloat(
        initialValue = 0.28f,
        targetValue = 0.72f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 950),
            repeatMode = RepeatMode.Reverse
        ),
        label = "teachings-shimmer-alpha"
    )
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
            BiblionGoldSoft.copy(alpha = 0.12f + shimmerAlpha * 0.18f),
            BiblionBluePrimary.copy(alpha = 0.06f + shimmerAlpha * 0.12f)
        )
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ShimmerBlock(
                brush = shimmerBrush,
                height = 56.dp,
                modifier = Modifier.fillMaxWidth()
            )
        }
        repeat(4) {
            item {
                TeachingCardSkeleton(brush = shimmerBrush)
            }
        }
    }
}

@Composable
private fun TeachingCardSkeleton(brush: Brush) {
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
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ShimmerBlock(brush = brush, height = 20.dp, modifier = Modifier.fillMaxWidth(0.72f))
                ShimmerBlock(brush = brush, height = 14.dp, modifier = Modifier.fillMaxWidth(0.44f))
            }
            ShimmerBlock(brush = brush, height = 40.dp, modifier = Modifier.width(40.dp))
        }
    }
}

@Composable
private fun ShimmerBlock(
    brush: Brush,
    height: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(8.dp))
            .background(brush)
    )
}

@Composable
fun EnsenanzaCard(
    study: StudyEntity,
    previewText: String = "",
    tags: List<String> = emptyList(),
    dateText: String,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onEditMetadata: () -> Unit,
    onShareText: () -> Unit,
    onShareBiblion: () -> Unit,
    onSharePdf: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpen() }
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxHeight()
                        .width(6.dp)
                        .background(BiblionGoldPrimary)
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 20.dp,
                        top = 16.dp,
                        end = 16.dp,
                        bottom = 16.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = study.title.ifBlank { "Sin titulo" },
                            style = MaterialTheme.typography.titleLarge,
                            color = BiblionBluePrimary,
                            fontWeight = FontWeight.Black,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Ultima edicion: $dateText",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                    }
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Mas opciones",
                                tint = BiblionBluePrimary
                            )
                        }
                        TeachingActionsMenu(
                            expanded = menuExpanded,
                            onDismiss = { menuExpanded = false },
                            onEdit = onEdit,
                            onEditMetadata = onEditMetadata,
                            onShareText = onShareText,
                            onShareBiblion = onShareBiblion,
                            onSharePdf = onSharePdf,
                            onDelete = onDelete
                        )
                    }
                }

                if (previewText.isNotBlank()) {
                    Text(
                        text = previewText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (tags.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(end = 8.dp)
                    ) {
                        items(tags, key = { it }) { tag ->
                            TeachingTagChip(tag)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TeachingActionsMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onEditMetadata: () -> Unit,
    onShareText: () -> Unit,
    onShareBiblion: () -> Unit,
    onSharePdf: () -> Unit,
    onDelete: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        offset = DpOffset(x = 0.dp, y = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(onClick = { onDismiss(); onEdit() }) {
                Icon(Icons.Default.Edit, contentDescription = "Editar", tint = BiblionGoldPrimary)
            }
            IconButton(onClick = { onDismiss(); onEditMetadata() }) {
                Icon(Icons.Default.Settings, contentDescription = "Configurar", tint = BiblionGoldPrimary)
            }
            IconButton(onClick = { onDismiss(); onShareText() }) {
                Icon(Icons.Default.Share, contentDescription = "Compartir texto", tint = BiblionGoldPrimary)
            }
            IconButton(onClick = { onDismiss(); onShareBiblion() }) {
                Icon(Icons.Default.FilePresent, contentDescription = "Compartir archivo Biblion", tint = BiblionGoldPrimary)
            }
            IconButton(onClick = { onDismiss(); onSharePdf() }) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = "Exportar PDF", tint = BiblionGoldPrimary)
            }
            IconButton(onClick = { onDismiss(); onDelete() }) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = BiblionGoldPrimary)
            }
        }
    }
}

@Composable
private fun TeachingTagChip(tag: String) {
    Surface(
        modifier = Modifier.widthIn(max = 120.dp),
        color = BiblionBluePrimary.copy(alpha = 0.08f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, BiblionGoldSoft.copy(alpha = 0.42f))
    ) {
        Text(
            text = tag,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = BiblionBluePrimary,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun shareStudyText(context: android.content.Context, study: StudyEntity, json: Json) {
    val text = buildStudyShareText(study, json)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, study.title.ifBlank { "Ensenanza Biblion" })
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Compartir ensenanza"))
}

private fun shareStudyBiblionFile(context: android.content.Context, study: StudyEntity) {
    val shareDir = File(context.cacheDir, "shared_studies").apply { mkdirs() }
    val safeName = study.title.ifBlank { "ensenanza-biblion" }
        .replace(Regex("[^A-Za-z0-9._-]+"), "-")
        .trim('-')
        .ifBlank { "ensenanza-biblion" }
    val file = File(shareDir, "$safeName.biblion")
    val payload = StudyViewModel.sharedJson.encodeToString(
        BiblionSharedStudyFile(
            title = study.title,
            remoteId = study.remoteId,
            updatedAt = study.updatedAt,
            contentSerialized = study.contentSerialized
        )
    )
    file.writeText(payload, Charsets.UTF_8)
    val uri = androidx.core.content.FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = BIBLION_STUDY_SHARE_MIME
        putExtra(Intent.EXTRA_SUBJECT, study.title.ifBlank { "Ensenanza Biblion" })
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Compartir archivo Biblion"))
}

private fun shareStudyPdf(context: android.content.Context, study: StudyEntity, json: Json) {
    val shareDir = File(context.cacheDir, "shared_studies").apply { mkdirs() }
    val safeName = study.title.ifBlank { "ensenanza-biblion" }
        .replace(Regex("[^A-Za-z0-9._-]+"), "-")
        .trim('-')
        .ifBlank { "ensenanza-biblion" }
    val file = File(shareDir, "$safeName.pdf")
    val text = buildStudyPdfBodyText(study, json)
    createStudyPdf(file, study.title.ifBlank { "Ensenanza Biblion" }, text)
    val uri = androidx.core.content.FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_SUBJECT, study.title.ifBlank { "Ensenanza Biblion" })
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Compartir PDF"))
}

private fun createStudyPdf(file: File, title: String, text: String) {
    val pageWidth = 595
    val pageHeight = 842
    val margin = 48f
    val lineHeight = 18f
    val paragraphGap = 10f
    val document = PdfDocument()
    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(31, 41, 55)
        textSize = 12f
        typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
    }
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(15, 23, 42)
        textSize = 18f
        typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
    }
    var pageNumber = 1
    var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
    var canvas = page.canvas
    var y = margin
    canvas.drawText(title, margin, y, titlePaint)
    y += 30f

    fun finishPage() {
        document.finishPage(page)
        pageNumber += 1
        page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        canvas = page.canvas
        y = margin
    }

    text.lineSequence().forEach { rawLine ->
        val line = rawLine.trimEnd()
        if (line.isBlank()) {
            y += paragraphGap
            if (y > pageHeight - margin) finishPage()
            return@forEach
        }
        wrapPdfLine(line, bodyPaint, pageWidth - (margin * 2)).forEach { wrapped ->
            if (y > pageHeight - margin) finishPage()
            canvas.drawText(wrapped, margin, y, bodyPaint)
            y += lineHeight
        }
    }

    document.finishPage(page)
    file.outputStream().use { output -> document.writeTo(output) }
    document.close()
}

private fun wrapPdfLine(line: String, paint: Paint, maxWidth: Float): List<String> {
    val words = line.split(Regex("\\s+")).filter { it.isNotBlank() }
    if (words.isEmpty()) return listOf("")
    val lines = mutableListOf<String>()
    var current = ""
    words.forEach { word ->
        val candidate = if (current.isBlank()) word else "$current $word"
        if (paint.measureText(candidate) <= maxWidth) {
            current = candidate
        } else {
            if (current.isNotBlank()) lines += current
            current = word
        }
    }
    if (current.isNotBlank()) lines += current
    return lines
}

private fun buildStudyShareText(study: StudyEntity, json: Json): String {
    val doc = runCatching {
        json.decodeFromString<SerializedStudyDocument>(study.contentSerialized)
    }.getOrNull()
    val content = doc?.blocks.orEmpty().toVerticalShareText()
    return buildString {
        appendLine(study.title.ifBlank { "Ensenanza Biblion" })
        doc?.tags?.takeIf { it.isNotEmpty() }?.let { tags ->
            appendLine("Etiquetas: ${tags.joinToString(", ")}")
        }
        appendLine()
        append(content.ifBlank { "Sin contenido." })
        appendLine()
        appendLine()
        append("Compartido desde Biblion")
    }
}

private fun buildStudyPdfBodyText(study: StudyEntity, json: Json): String {
    val doc = runCatching {
        json.decodeFromString<SerializedStudyDocument>(study.contentSerialized)
    }.getOrNull()
    val content = doc?.blocks.orEmpty().toVerticalShareText()
    return buildString {
        doc?.tags?.takeIf { it.isNotEmpty() }?.let { tags ->
            appendLine("Etiquetas: ${tags.joinToString(", ")}")
            appendLine()
        }
        append(content.ifBlank { "Sin contenido." })
        appendLine()
        appendLine()
        append("Compartido desde Biblion")
    }
}

private fun List<StudyBlockNode>.toVerticalShareText(): String {
    val sections = mutableListOf<String>()
    var numberedIndex = 0
    forEach { block ->
        val rendered = when (block) {
            is StudyBlockNode.Paragraph -> {
                if (block.role == "numbered") numberedIndex += 1 else numberedIndex = 0
                block.toVerticalParagraphText(numberedIndex)
            }
            is StudyBlockNode.RichText -> {
                numberedIndex = 0
                block.html.toPlainShareText()
            }
            is StudyBlockNode.Citation -> {
                numberedIndex = 0
                "${block.reference.display}\n${block.text}".trim()
            }
            is StudyBlockNode.Note -> {
                numberedIndex = 0
                "Nota\n${block.text}".trim()
            }
            is StudyBlockNode.Reflection -> {
                numberedIndex = 0
                listOf("Reflexion", block.topic, block.text)
                    .filter { it.isNotBlank() }
                    .joinToString("\n")
            }
            is StudyBlockNode.QuotedVerse -> {
                numberedIndex = 0
                listOf(block.reference, block.primaryText, block.note)
                    .filter { it.isNotBlank() }
                    .joinToString("\n")
            }
            is StudyBlockNode.Question -> {
                numberedIndex = 0
                listOf("Pregunta", block.question, block.answer)
                    .filter { it.isNotBlank() }
                    .joinToString("\n")
            }
            is StudyBlockNode.TwoColumn -> {
                numberedIndex = 0
                listOf(
                    block.leftTitle.ifBlank { "Columna izquierda" },
                    block.leftText,
                    block.rightTitle.ifBlank { "Columna derecha" },
                    block.rightText
                ).filter { it.isNotBlank() }.joinToString("\n")
            }
            is StudyBlockNode.Audio -> {
                numberedIndex = 0
                block.title.ifBlank { "Audio adjunto" }
            }
            is StudyBlockNode.Image -> {
                numberedIndex = 0
                block.caption.ifBlank { "Imagen adjunta" }
            }
        }
        rendered.trim().takeIf { it.isNotBlank() }?.let { sections += it }
    }
    return sections.joinToString("\n\n")
}

private fun StudyBlockNode.Paragraph.toVerticalParagraphText(numberedIndex: Int): String {
    if (role == "columns") {
        return listOf(
            "Columna izquierda",
            formatColumnFlowText(text, embeddedBlocks),
            "Columna derecha",
            formatColumnFlowText(parallelText, parallelEmbeddedBlocks)
        ).filter { it.isNotBlank() }.joinToString("\n")
    }
    val content = text.trim()
    return when (role) {
        "heading" -> content.uppercase()
        "bullet" -> content.lines()
            .filter { it.isNotBlank() }
            .joinToString("\n") { line -> "- ${line.trim().removePrefix("-").trimStart()}" }
        "numbered" -> if (content.isBlank()) "" else "$numberedIndex. $content"
        else -> content
    }
}

private fun formatColumnFlowText(text: String, blocks: List<ColumnEmbeddedBlock>): String {
    return StudyDocumentEngine.buildColumnFlow(text, blocks).flatMap { segment ->
        listOf(segment.text.formatInlineColumnText()) + segment.blocksAfter.mapNotNull { block ->
            block.toVerticalEmbeddedText()
        }
    }.filter { it.isNotBlank() }.joinToString("\n")
}

private fun ColumnEmbeddedBlock.toVerticalEmbeddedText(): String {
    val title = title.ifBlank {
        when (type) {
            "reflection" -> "Reflexion"
            "quote" -> "Cita"
            else -> "Nota"
        }
    }
    val content = text.formatInlineColumnText()
    return listOf(title, if (collapsed) "" else content)
        .filter { it.isNotBlank() }
        .joinToString("\n")
        .takeIf { it.isNotBlank() }
        .orEmpty()
}

private fun String.formatInlineColumnText(): String {
    return lines()
        .map { it.trimEnd() }
        .dropWhile { it.isBlank() }
        .dropLastWhile { it.isBlank() }
        .joinToString("\n")
}

private fun String.toPlainShareText(): String {
    return replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("</p>|</div>|</h[1-6]>", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("<[^>]*>"), " ")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace(Regex("[ \\t]+"), " ")
        .replace(Regex("\\n{3,}"), "\n\n")
        .trim()
}

private data class StudyPreview(
    val content: String,
    val tags: List<String>
)

private fun buildStudyPreview(serialized: String, json: Json): StudyPreview {
    val doc = runCatching { json.decodeFromString<SerializedStudyDocument>(serialized) }.getOrNull()
    val html = doc?.blocks?.filterIsInstance<StudyBlockNode.RichText>()?.firstOrNull()?.html.orEmpty()
    val structuredContent = doc?.blocks.orEmpty().toVerticalShareText()
    val plainContent = structuredContent.ifBlank { html }
        .replace(Regex("<[^>]*>"), " ")
        .replace("&nbsp;", " ")
        .replace(Regex("\\s+"), " ")
        .trim()
    return StudyPreview(
        content = plainContent,
        tags = doc?.tags ?: emptyList()
    )
}

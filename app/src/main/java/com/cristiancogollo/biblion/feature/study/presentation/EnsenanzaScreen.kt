package com.cristiancogollo.biblion

import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
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
import androidx.compose.material.icons.filled.FilePresent
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
import androidx.compose.ui.platform.LocalContext
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
fun EnsenanzaCard(
    study: StudyEntity,
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
                            onShareText()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Compartir texto",
                                tint = BiblionGoldPrimary
                            )
                        }

                        IconButton(onClick = {
                            menuExpanded = false
                            onShareBiblion()
                        }) {
                            Icon(
                                imageVector = Icons.Default.FilePresent,
                                contentDescription = "Compartir archivo Biblion",
                                tint = BiblionGoldPrimary
                            )
                        }

                        IconButton(onClick = {
                            menuExpanded = false
                            onSharePdf()
                        }) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = "Exportar PDF",
                                tint = BiblionGoldPrimary.copy(alpha = 0.55f)
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
    val payload = Json.encodeToString(
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

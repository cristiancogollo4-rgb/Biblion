package com.cristiancogollo.biblion

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.cristiancogollo.biblion.ui.theme.BiblionBluePrimary
import com.cristiancogollo.biblion.ui.theme.BiblionGoldPrimary

private val LocalStudyReadFontSize = compositionLocalOf { 18.sp }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyReadScreen(
    navController: NavController,
    studyId: Long,
    isDarkTheme: Boolean = false,
    onToggleDarkTheme: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val viewModel: StudyViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    val expandedBlocks = remember(studyId) { mutableStateMapOf<String, Boolean>() }
    var availableBibleVersions by remember { mutableStateOf<List<BibleVersionOption>>(emptyList()) }
    val supportsSplitReading = configuration.screenWidthDp >= 840
    var splitReadingEnabled by remember(studyId) { mutableStateOf(false) }
    var readFontSizeSp by remember(studyId) { mutableStateOf(18f) }

    LaunchedEffect(studyId) {
        viewModel.process(StudyIntent.SelectStudy(studyId))
    }

    LaunchedEffect(Unit) {
        availableBibleVersions = BibleRepository.getAvailableVersions(context)
    }

    LaunchedEffect(supportsSplitReading) {
        if (!supportsSplitReading) {
            splitReadingEnabled = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.title.ifBlank { "Sin titulo" }) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atras")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { readFontSizeSp = (readFontSizeSp - 1f).coerceAtLeast(14f) },
                        enabled = readFontSizeSp > 14f
                    ) {
                        Text("A-", style = MaterialTheme.typography.labelLarge)
                    }
                    TextButton(
                        onClick = { readFontSizeSp = (readFontSizeSp + 1f).coerceAtMost(28f) },
                        enabled = readFontSizeSp < 28f
                    ) {
                        Text("A+", style = MaterialTheme.typography.labelLarge)
                    }
                    IconButton(onClick = { onToggleDarkTheme(!isDarkTheme) }) {
                        Icon(
                            Icons.Default.Brightness4,
                            contentDescription = if (isDarkTheme) "Cambiar a modo claro" else "Cambiar a modo oscuro",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (supportsSplitReading && state.blocks.isNotEmpty()) {
                        IconButton(onClick = { splitReadingEnabled = !splitReadingEnabled }) {
                            Icon(
                                Icons.Default.ViewColumn,
                                contentDescription = if (splitReadingEnabled) {
                                    "Leer en una columna"
                                } else {
                                    "Leer en pantalla dividida"
                                },
                                tint = if (splitReadingEnabled) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        val readItems = remember(state.blocks) { state.blocks.toReadItems() }
        val blockActions = StudyReadBlockActions(
            availableVersions = availableBibleVersions,
            expandedBlocks = expandedBlocks,
            onExpandedChange = { key, expanded ->
                expandedBlocks[key] = expanded
            },
            onChangeQuotedVerseVersion = { blockId, version ->
                viewModel.process(StudyIntent.ChangeQuotedVerseVersion(blockId, version))
            },
            onCompareQuotedVerseVersion = { blockId, version ->
                viewModel.process(StudyIntent.CompareQuotedVerseVersion(blockId, version))
            }
        )

        CompositionLocalProvider(LocalStudyReadFontSize provides readFontSizeSp.sp) {
            if (supportsSplitReading && splitReadingEnabled && readItems.isNotEmpty()) {
                StudyReadSplitContent(
                    tags = state.tags,
                    readItems = readItems,
                    blockActions = blockActions,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                )
            } else {
                StudyReadVerticalContent(
                    tags = state.tags,
                    richHtml = state.richHtml,
                    readItems = readItems,
                    blockActions = blockActions,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                )
            }
        }
    }
}

private data class StudyReadItem(
    val block: StudyBlockNode,
    val numberedIndex: Int
) {
    val key: String = block.readBlockKey()
}

private data class StudyReadBlockActions(
    val availableVersions: List<BibleVersionOption>,
    val expandedBlocks: Map<String, Boolean>,
    val onExpandedChange: (String, Boolean) -> Unit,
    val onChangeQuotedVerseVersion: (String, String) -> Unit,
    val onCompareQuotedVerseVersion: (String, String) -> Unit
)

private fun List<StudyBlockNode>.toReadItems(): List<StudyReadItem> {
    var numberedIndex = 0
    return map { block ->
        if (block is StudyBlockNode.Paragraph && block.role == "numbered") {
            numberedIndex += 1
        }
        StudyReadItem(block = block, numberedIndex = numberedIndex)
    }
}

@Composable
private fun StudyReadVerticalContent(
    tags: List<String>,
    richHtml: String,
    readItems: List<StudyReadItem>,
    blockActions: StudyReadBlockActions,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (tags.isNotEmpty()) {
            item {
                StudyReadTags(tags)
            }
        }

        if (readItems.isEmpty() && richHtml.isBlank()) {
            item {
                StudyReadEmptyMessage()
            }
        } else if (readItems.isEmpty()) {
            item {
                Text(
                    text = richHtml.toPlainReadText(),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = LocalStudyReadFontSize.current,
                        lineHeight = (LocalStudyReadFontSize.current.value * 1.45f).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        } else {
            readItems.forEach { readItem ->
                item(key = readItem.key) {
                    StudyReadBlockItem(item = readItem, blockActions = blockActions)
                }
            }
        }
    }
}

@Composable
private fun StudyReadSplitContent(
    tags: List<String>,
    readItems: List<StudyReadItem>,
    blockActions: StudyReadBlockActions,
    modifier: Modifier = Modifier
) {
    val splitIndex = (readItems.size + 1) / 2
    val leftItems = readItems.take(splitIndex)
    val rightItems = readItems.drop(splitIndex)

    Column(
        modifier = modifier.padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (tags.isNotEmpty()) {
            StudyReadTags(tags)
        }
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            StudyReadSplitColumn(
                readItems = leftItems,
                blockActions = blockActions,
                modifier = Modifier
                    .weight(1f)
                    .widthIn(max = 620.dp)
            )
            StudyReadSplitColumn(
                readItems = rightItems,
                blockActions = blockActions,
                modifier = Modifier
                    .weight(1f)
                    .widthIn(max = 620.dp)
            )
        }
    }
}

@Composable
private fun StudyReadSplitColumn(
    readItems: List<StudyReadItem>,
    blockActions: StudyReadBlockActions,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        readItems.forEach { readItem ->
            item(key = readItem.key) {
                StudyReadBlockItem(item = readItem, blockActions = blockActions)
            }
        }
    }
}

@Composable
private fun StudyReadBlockItem(
    item: StudyReadItem,
    blockActions: StudyReadBlockActions
) {
    Spacer(modifier = Modifier.height(4.dp))
    StudyReadBlock(
        block = item.block,
        numberedIndex = item.numberedIndex,
        availableVersions = blockActions.availableVersions,
        expanded = blockActions.expandedBlocks[item.key] ?: !item.block.readCollapsed,
        onExpandedChange = { expanded ->
            blockActions.onExpandedChange(item.key, expanded)
        },
        onChangeQuotedVerseVersion = blockActions.onChangeQuotedVerseVersion,
        onCompareQuotedVerseVersion = blockActions.onCompareQuotedVerseVersion
    )
}

@Composable
private fun StudyReadTags(tags: List<String>) {
    Text(
        text = "Etiquetas: ${tags.joinToString(", ")}",
        color = MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.bodyMedium
    )
}

@Composable
private fun StudyReadEmptyMessage() {
    Text(
        text = "Esta ensenanza no tiene contenido aun.",
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
        style = MaterialTheme.typography.bodyLarge
    )
}

@Composable
private fun StudyReadBlock(
    block: StudyBlockNode,
    numberedIndex: Int,
    availableVersions: List<BibleVersionOption>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onChangeQuotedVerseVersion: (String, String) -> Unit,
    onCompareQuotedVerseVersion: (String, String) -> Unit
) {
    when (block) {
        is StudyBlockNode.Paragraph -> StudyReadParagraph(block, numberedIndex)
        is StudyBlockNode.RichText -> StudyReadLegacyRichText(block)
        is StudyBlockNode.Citation -> StudyReadCard(
            title = block.reference.display,
            accent = Color(0xFFB45309),
            collapsed = false,
            expandable = false,
            expanded = true,
            onExpandedChange = onExpandedChange
        ) {
            ReadSerifText(block.text.ifBlank { "Texto de la cita no disponible." })
            Text(
                text = block.version.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.54f)
            )
        }
        is StudyBlockNode.Note -> StudyReadCard(
            title = "Nota",
            accent = Color(0xFF0F766E),
            collapsed = block.collapsed,
            expanded = expanded,
            onExpandedChange = onExpandedChange
        ) {
            ReadSerifText(block.text)
        }
        is StudyBlockNode.Reflection -> StudyReadCard(
            title = "Reflexion",
            accent = Color(0xFF7C3AED),
            collapsed = block.collapsed,
            expanded = expanded,
            onExpandedChange = onExpandedChange
        ) {
            if (block.topic.isNotBlank()) {
                Text(
                    text = block.topic,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF7C3AED)
                )
            }
            ReadSerifText(block.text)
        }
        is StudyBlockNode.QuotedVerse -> StudyReadCard(
            title = block.reference.ifBlank { "Cita biblica" },
            accent = Color(0xFFB45309),
            collapsed = block.collapsed,
            expanded = expanded,
            onExpandedChange = onExpandedChange
        ) {
            var showCompareTools by remember(block.blockId) { mutableStateOf(block.compareText.isNotBlank()) }
            StudyReadQuotedVerseHeader(
                block = block,
                availableVersions = availableVersions,
                showCompareTools = showCompareTools,
                onVersionSelected = { version ->
                    onChangeQuotedVerseVersion(block.blockId, version)
                },
                onToggleCompare = { showCompareTools = !showCompareTools },
                onCompareVersionSelected = { version ->
                    onCompareQuotedVerseVersion(block.blockId, version)
                }
            )
            if (showCompareTools && block.compareText.isNotBlank()) {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val compact = maxWidth < 520.dp
                    if (compact) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            StudyReadVerseColumn(block.primaryVersion, block.primaryText)
                            StudyReadVerseColumn(block.compareVersion, block.compareText)
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StudyReadVerseColumn(block.primaryVersion, block.primaryText, Modifier.weight(1f))
                            StudyReadVerseColumn(block.compareVersion, block.compareText, Modifier.weight(1f))
                        }
                    }
                }
            } else {
                StudyReadVerseColumn(block.primaryVersion, block.primaryText)
            }
            if (block.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                ReadSerifText(block.note)
            }
        }
        is StudyBlockNode.Question -> StudyReadCard(
            title = "Pregunta",
            accent = Color(0xFF2563EB),
            collapsed = block.collapsed,
            expanded = expanded,
            onExpandedChange = onExpandedChange
        ) {
            Text(
                text = block.question.ifBlank { "Pregunta sin texto." },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (block.answer.isNotBlank()) {
                ReadSerifText(block.answer)
            }
        }
        is StudyBlockNode.TwoColumn -> {
            if (!block.collapsed || expanded) {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val compact = maxWidth < 520.dp
                    if (compact) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            StudyReadColumn(block.leftTitle, block.leftText)
                            StudyReadColumn(block.rightTitle, block.rightText)
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StudyReadColumn(block.leftTitle, block.leftText, Modifier.weight(1f))
                            StudyReadColumn(block.rightTitle, block.rightText, Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        is StudyBlockNode.Audio -> StudyReadCard(
            title = "Audio",
            accent = Color(0xFF0F766E),
            collapsed = false,
            expandable = false,
            expanded = true,
            onExpandedChange = onExpandedChange
        ) {
            Text(text = block.title.ifBlank { block.uri }, style = MaterialTheme.typography.bodyLarge)
        }
        is StudyBlockNode.Image -> StudyReadCard(
            title = "Imagen",
            accent = Color(0xFF475569),
            collapsed = false,
            expandable = false,
            expanded = true,
            onExpandedChange = onExpandedChange
        ) {
            Text(text = block.caption.ifBlank { block.uri }, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun StudyReadParagraph(block: StudyBlockNode.Paragraph, numberedIndex: Int) {
    if (block.role == "columns") {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val compact = maxWidth < 520.dp
            if (compact) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StudyReadColumnText(block.text, block.styles)
                    StudyReadColumnText(block.parallelText, block.parallelStyles)
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StudyReadColumnText(block.text, block.styles, Modifier.weight(1f))
                    StudyReadColumnText(block.parallelText, block.parallelStyles, Modifier.weight(1f))
                }
            }
        }
        return
    }

    val isListItem = block.role == "bullet" || block.role == "numbered"
    val readFontSize = LocalStudyReadFontSize.current
    val textStyle = when (block.role) {
        "heading" -> MaterialTheme.typography.headlineSmall.copy(
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        else -> MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = readFontSize,
            lineHeight = (readFontSize.value * 1.45f).sp
        )
    }
    val prefix = when (block.role) {
        "bullet" -> "* "
        "numbered" -> "$numberedIndex. "
        else -> ""
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (isListItem) 28.dp else 36.dp),
    ) {
        if (prefix.isNotEmpty()) {
            Text(
                text = prefix,
                modifier = Modifier.padding(top = 1.dp, end = 4.dp),
                style = textStyle,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.76f)
            )
        }
        Text(
            text = block.text.toStyledText(block.styles),
            modifier = Modifier.weight(1f),
            style = textStyle
        )
    }
}

@Composable
private fun StudyReadLegacyRichText(block: StudyBlockNode.RichText) {
    val readFontSize = LocalStudyReadFontSize.current
    Text(
        text = block.html.toPlainReadText(),
        style = MaterialTheme.typography.bodyLarge.copy(
            fontSize = readFontSize,
            lineHeight = (readFontSize.value * 1.45f).sp
        ),
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun StudyReadColumnText(
    text: String,
    styles: List<TextStyleRange>,
    modifier: Modifier = Modifier
) {
    val readFontSize = LocalStudyReadFontSize.current
    Text(
        text = text.toStyledText(styles),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        style = MaterialTheme.typography.bodyLarge.copy(
            fontSize = readFontSize,
            lineHeight = (readFontSize.value * 1.45f).sp
        ),
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun StudyReadCard(
    title: String,
    accent: Color,
    collapsed: Boolean,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    expandable: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Italic
                ),
                color = accent
            )
            if (expandable || collapsed) {
                IconButton(onClick = { onExpandedChange(!expanded) }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Contraer bloque" else "Expandir bloque",
                        tint = accent
                    )
                }
            }
        }
        if (expanded) {
            content()
        }
    }
}

@Composable
private fun StudyReadQuotedVerseHeader(
    block: StudyBlockNode.QuotedVerse,
    availableVersions: List<BibleVersionOption>,
    showCompareTools: Boolean,
    onVersionSelected: (String) -> Unit,
    onToggleCompare: () -> Unit,
    onCompareVersionSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StudyReadVersionMenuButton(
            label = block.primaryVersion.uppercase(),
            versions = availableVersions,
            selectedVersion = block.primaryVersion,
            onVersionSelected = onVersionSelected
        )
        TextButton(
            onClick = onToggleCompare,
            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)
        ) {
            Text(
                text = if (showCompareTools) "ocultar" else "comparar",
                style = MaterialTheme.typography.labelSmall
            )
        }
        if (showCompareTools) {
            StudyReadVersionMenuButton(
                label = if (block.compareVersion.isBlank()) "version" else block.compareVersion.uppercase(),
                versions = availableVersions.filter { it.key != block.primaryVersion },
                selectedVersion = block.compareVersion,
                onVersionSelected = onCompareVersionSelected
            )
        }
    }
}

@Composable
private fun StudyReadVersionMenuButton(
    label: String,
    versions: List<BibleVersionOption>,
    selectedVersion: String,
    onVersionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(
            onClick = { expanded = true },
            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)
        ) {
            Text(
                text = label.ifBlank { "version" },
                style = MaterialTheme.typography.labelSmall
            )
            Icon(
                Icons.Default.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.height(16.dp)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            versions.forEach { version ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = version.label,
                            fontWeight = if (version.key == selectedVersion) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        expanded = false
                        onVersionSelected(version.key)
                    }
                )
            }
        }
    }
}

@Composable
private fun StudyReadVerseColumn(
    version: String,
    text: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (version.isNotBlank()) {
            Text(
                text = version.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.52f)
            )
        }
        ReadSerifText(text.ifBlank { "Texto de la cita no disponible." })
    }
}

@Composable
private fun StudyReadColumn(
    title: String,
    text: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (title.isNotBlank()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        ReadSerifText(text)
    }
}

@Composable
private fun ReadSerifText(text: String) {
    val contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f)
    val verseNumberColor = verseNumberAccentColor()
    val readFontSize = LocalStudyReadFontSize.current
    Text(
        text = text.ifBlank { "Sin contenido." }.toVerseNumberStyledText(verseNumberColor),
        style = MaterialTheme.typography.bodyLarge.copy(
            fontFamily = FontFamily.Serif,
            fontStyle = FontStyle.Italic,
            fontSize = readFontSize,
            lineHeight = (readFontSize.value * 1.45f).sp
        ),
        color = contentColor
    )
}

@Composable
private fun verseNumberAccentColor(): Color {
    return if (MaterialTheme.colorScheme.background.luminance() > 0.5f) {
        BiblionBluePrimary
    } else {
        BiblionGoldPrimary
    }
}

private val StudyBlockNode.readCollapsed: Boolean
    get() = when (this) {
        is StudyBlockNode.Note -> collapsed
        is StudyBlockNode.Reflection -> collapsed
        is StudyBlockNode.QuotedVerse -> collapsed
        is StudyBlockNode.Question -> collapsed
        is StudyBlockNode.TwoColumn -> collapsed
        else -> false
    }

private fun StudyBlockNode.readBlockKey(): String = when (this) {
    is StudyBlockNode.Paragraph -> blockId
    is StudyBlockNode.RichText -> blockId
    is StudyBlockNode.Citation -> citationId
    is StudyBlockNode.Note -> blockId
    is StudyBlockNode.Reflection -> blockId
    is StudyBlockNode.QuotedVerse -> blockId
    is StudyBlockNode.Question -> blockId
    is StudyBlockNode.TwoColumn -> blockId
    is StudyBlockNode.Audio -> uri
    is StudyBlockNode.Image -> uri
}

private fun String.toStyledText(styles: List<TextStyleRange>): AnnotatedString {
    if (isBlank() || styles.isEmpty()) return AnnotatedString(this)

    return AnnotatedString.Builder(this).apply {
        styles.forEach { range ->
            val start = range.start.coerceIn(0, length)
            val end = range.end.coerceIn(start, length)
            if (start == end) return@forEach
            addStyle(
                SpanStyle(
                    color = range.color?.toStudyColorOrUnspecified() ?: Color.Unspecified,
                    background = range.background?.toStudyColorOrUnspecified() ?: Color.Unspecified,
                    fontWeight = if (range.bold) FontWeight.Bold else null,
                    fontStyle = if (range.italic) FontStyle.Italic else null,
                    textDecoration = if (range.underline) TextDecoration.Underline else null,
                    fontSize = range.fontSizeSp?.sp ?: TextUnit.Unspecified
                ),
                start,
                end
            )
        }
    }.toAnnotatedString()
}

@Composable
private fun String.toVerseNumberStyledText(verseNumberColor: Color): AnnotatedString {
    val builder = AnnotatedString.Builder(this)
    val numberFontSize = (LocalStudyReadFontSize.current.value * 0.72f).sp
    Regex("""(^|\s)(\d{1,3})(?=\s)""").findAll(this).forEach { match ->
        val numberStart = match.range.first + match.groupValues[1].length
        val numberEnd = numberStart + match.groupValues[2].length
        builder.addStyle(
            SpanStyle(
                color = verseNumberColor,
                fontWeight = FontWeight.Bold,
                fontSize = numberFontSize
            ),
            numberStart,
            numberEnd
        )
    }
    return builder.toAnnotatedString()
}

private fun String.toPlainReadText(): String {
    return replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("</p>|</div>|</h[1-6]>", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("<[^>]*>"), " ")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace(Regex("[ \\t]+"), " ")
        .replace(Regex("\\n{3,}"), "\n\n")
        .trim()
}

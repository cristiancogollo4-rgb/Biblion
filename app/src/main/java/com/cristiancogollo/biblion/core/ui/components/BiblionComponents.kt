package com.cristiancogollo.biblion

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatClear
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.HighlightOff
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.TextIncrease
import androidx.compose.material.icons.filled.TextDecrease
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlin.math.roundToInt
import com.cristiancogollo.biblion.ui.theme.BiblionBluePrimary
import com.cristiancogollo.biblion.ui.theme.BiblionGoldPrimary
import com.cristiancogollo.biblion.ui.theme.BiblionGoldSoft
import com.cristiancogollo.biblion.ui.theme.BiblionNavy

private enum class AppDrawerOption(val labelRes: Int) {
    HOME(R.string.drawer_home),
    PROFILE(R.string.drawer_profile),
    PICK_VERSION(R.string.drawer_pick_version),
    MY_TEACHINGS(R.string.drawer_my_teachings),
    DOCTRINES(R.string.drawer_doctrines),
    BIBLION(R.string.drawer_biblion),
    STUDY_MODE(R.string.drawer_study_mode),
    ABOUT_US(R.string.drawer_about_us)
}

// 1. BiblionTopAppBar: Usado en HomeScreen y BooksScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BiblionTopAppBar(
    onNavigationIconClick: () -> Unit = {},
    onSearchIconClick: () -> Unit = {},
    logoResId: Int? = null,
    logoContentDescription: String = ""
) {
    val resolvedLogoContentDescription =
        logoContentDescription.ifBlank { stringResource(R.string.cd_app_logo) }
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    CenterAlignedTopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (logoResId != null) {
                    Image(
                        painter = painterResource(id = logoResId),
                        contentDescription = resolvedLogoContentDescription,
                        modifier = Modifier
                            .size(80.dp)
                            .padding(end = 10.dp),
                        contentScale = ContentScale.Fit
                    )
                }

                Text(
                    text = stringResource(R.string.biblion_wordmark),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold
                    ),
                    color = onSurfaceColor
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onNavigationIconClick) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = stringResource(R.string.cd_open_menu),
                    tint = onSurfaceColor
                )
            }
        },
        actions = {
            IconButton(onClick = onSearchIconClick) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(R.string.cd_search),
                    tint = onSurfaceColor
                )
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
fun BiblionAppDrawer(
    drawerState: DrawerState,
    isDarkTheme: Boolean,
    onToggleDarkTheme: (Boolean) -> Unit,
    currentUserName: String? = null,
    currentUserEmail: String? = null,
    isAuthenticated: Boolean = false,
    onClose: () -> Unit,
    onNavigateHome: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToTeachings: () -> Unit,
    onNavigateToStudyMode: () -> Unit,
    onPickVersion: () -> Unit,
    onShowComingSoon: () -> Unit,
    onShowAbout: () -> Unit,
    onAuthActionClick: () -> Unit
) {
    val menuOptions = listOf(
        AppDrawerOption.HOME,
        AppDrawerOption.PROFILE,
        AppDrawerOption.PICK_VERSION,
        AppDrawerOption.MY_TEACHINGS,
        AppDrawerOption.DOCTRINES,
        AppDrawerOption.BIBLION,
        AppDrawerOption.STUDY_MODE,
        AppDrawerOption.ABOUT_US
    )

    ModalDrawerSheet(
        modifier = Modifier
            .fillMaxHeight()
            .width(300.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerShape = RectangleShape
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        menuOptions.forEach { option ->
            if (option == AppDrawerOption.ABOUT_US) {
                DarkModeMenuItem(
                    checked = isDarkTheme,
                    onCheckedChange = onToggleDarkTheme
                )
            }

            BiblionMenuItem(
                text = stringResource(option.labelRes),
                onClick = {
                    if (option == AppDrawerOption.PROFILE && !isAuthenticated) {
                        onClose()
                        onAuthActionClick()
                        return@BiblionMenuItem
                    }
                    if (!drawerState.isOpen) return@BiblionMenuItem
                    onClose()
                    when (option) {
                        AppDrawerOption.HOME -> onNavigateHome()
                        AppDrawerOption.PROFILE -> onNavigateToProfile()
                        AppDrawerOption.PICK_VERSION -> onPickVersion()
                        AppDrawerOption.MY_TEACHINGS -> onNavigateToTeachings()
                        AppDrawerOption.DOCTRINES,
                        AppDrawerOption.BIBLION -> onShowComingSoon()
                        AppDrawerOption.STUDY_MODE -> onNavigateToStudyMode()
                        AppDrawerOption.ABOUT_US -> onShowAbout()
                    }
                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(80.dp))
            Text(
                text = if (isAuthenticated) {
                    currentUserName?.takeIf { it.isNotBlank() }
                        ?: stringResource(R.string.auth_account_title)
                } else {
                    stringResource(R.string.reader_label)
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            if (!currentUserEmail.isNullOrBlank()) {
                Text(
                    text = currentUserEmail,
                    modifier = Modifier.padding(horizontal = 24.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = {
                    onClose()
                    onAuthActionClick()
                },
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface)
            ) {
                Text(
                    text = stringResource(
                        if (isAuthenticated) R.string.sign_out else R.string.sign_in
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun BiblionMenuItem(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 24.dp),
        style = MaterialTheme.typography.bodyLarge
    )
}

@Composable
private fun DarkModeMenuItem(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.drawer_dark_mode),
            style = MaterialTheme.typography.bodyLarge
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

// 2. TestamentSelector: Selector tipo pestañas (Antiguo / Nuevo)
@Composable
fun TestamentSelector(
    selectedTab: Testament?,
    onTabSelected: (Testament) -> Unit
) {
    val tabs = listOf(Testament.OLD, Testament.NEW)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .height(50.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEach { testament ->
            val selected = selectedTab == testament
            Button(
                onClick = {
                    onTabSelected(testament)
                },
                modifier = Modifier.weight(1f).fillMaxHeight(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    contentColor = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = stringResource(testament.shortLabelRes),
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 13.sp
                )
            }
        }
    }
}

// 3. DailyVerseCard: Muestra el versículo del día
@Composable
fun DailyVerseCard(
    verse: String,
    reference: String,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = verse,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = FontFamily.Serif
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = reference,
                style = MaterialTheme.typography.labelLarge,
                color = BiblionGoldPrimary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// 4. BookCard: Representa un libro en la cuadrícula
@Composable
fun BookCard(bookName: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val spacious = maxWidth >= 120.dp
            val bookFontSize = when {
                spacious && bookName.length >= 17 -> 12.sp
                spacious && bookName.length >= 14 -> 13.sp
                spacious -> 14.sp
                bookName.length >= 17 -> 9.sp
                bookName.length >= 14 -> 10.sp
                bookName.length >= 11 -> 11.sp
                else -> 12.sp
            }

            Text(
                text = bookName,
                textAlign = TextAlign.Center,
                fontSize = bookFontSize,
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                softWrap = false,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

// 5. BiblionSelectionDialog: Diálogo para elegir capítulos
@Composable
fun BiblionSelectionDialog(
    title: String,
    subtitle: String,
    itemCount: Int,
    onDismiss: () -> Unit,
    onItemSelected: (Int) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(text = title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                    Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
                }

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items((1..itemCount).toList()) { itemNumber ->
                        Text(
                            text = "$title $itemNumber",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onItemSelected(itemNumber)
                                    onDismiss()
                                }
                                .padding(horizontal = 24.dp, vertical = 16.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel), color = BiblionGoldPrimary)
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BiblionReaderTopAppBar(
    bookName: String,
    chapters: List<Int>,
    selectedChapter: Int,
    fontSize: TextUnit,
    onNavigationIconClick: () -> Unit,
    onChapterClick: (Int) -> Unit,
    onSearchIconClick: () -> Unit,
    onBookTitleClick: () -> Unit,
    onIncreaseFontSize: () -> Unit,
    onDecreaseFontSize: () -> Unit,
    modifier: Modifier = Modifier
) {
    val chapterListState = rememberLazyListState()

    LaunchedEffect(selectedChapter, chapters) {
        val selectedIndex = chapters.indexOf(selectedChapter)
        if (selectedIndex >= 0) {
            chapterListState.animateScrollToItem(
                index = (selectedIndex - 2).coerceAtLeast(0)
            )
        }
    }

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding() // Evita que se solape con la barra de estado
    ) {
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = bookName,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.clickable(onClick = onBookTitleClick)
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigationIconClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            actions = {
                IconButton(onClick = onSearchIconClick) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(R.string.cd_search),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                // Botón Disminuir Fuente
                IconButton(onClick = onDecreaseFontSize) {
                    Icon(
                        imageVector = Icons.Default.HorizontalRule,
                        contentDescription = stringResource(R.string.cd_decrease_font_size),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                // Botón Aumentar Fuente
                IconButton(onClick = onIncreaseFontSize) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.cd_increase_font_size),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = Color.Transparent
            )
        )

        // Fila horizontal de capítulos
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            state = chapterListState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(chapters) { chapter ->
                val isSelected = chapter == selectedChapter
                Text(
                    text = "CAP $chapter",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        letterSpacing = 1.sp
                    ),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    modifier = Modifier
                        .clickable { onChapterClick(chapter) }
                        .padding(vertical = 4.dp)
                )
            }
        }

        // Línea divisoria sutil
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
    }
}

/**
 * Burbuja contextual flotante optimizada para acciones sobre versículos.
 * Ubicada en la parte inferior-media para mejor ergonomía.
 */
@Composable
fun VerseActionsFloatingMenu(
    selectedCount: Int,
    anchorOffset: IntOffset,
    showHighlightOptions: Boolean,
    highlightPalette: List<Color>,
    onDismiss: () -> Unit,
    onClearSelection: () -> Unit,
    onCopy: () -> Unit,
    onAddCitation: (() -> Unit)?,
    onHighlight: (Int) -> Unit
) {
    Popup(
        alignment = Alignment.BottomCenter,
        offset = IntOffset(0, -180), // Posición interactiva en la zona inferior-media
        onDismissRequest = onDismiss,
        properties = PopupProperties(
            focusable = false,
            dismissOnClickOutside = false
        )
    ) {
        ElevatedCard(
            shape = RoundedCornerShape(28.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 12.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Contador Estilizado
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = "$selectedCount",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.surface,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    ActionPill(icon = Icons.Default.CopyAll, label = "Copiar", onClick = onCopy)
                    
                    if (onAddCitation != null) {
                        ActionPill(icon = Icons.Default.FormatQuote, label = "Citar", onClick = onAddCitation)
                    }

                    IconButton(onClick = onClearSelection, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.HighlightOff,
                            contentDescription = "Limpiar",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                    }
                }

                if (showHighlightOptions) {
                    HorizontalDivider(
                        modifier = Modifier.width(60.dp).padding(vertical = 4.dp),
                        color = BiblionGoldSoft.copy(alpha = 0.2f)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        highlightPalette.forEachIndexed { index, color ->
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .background(
                                        color = if (index == 0) MaterialTheme.colorScheme.surface else color,
                                        shape = RoundedCornerShape(13.dp)
                                    )
                                    .border(1.dp, BiblionGoldSoft.copy(alpha = 0.5f), RoundedCornerShape(13.dp))
                                    .clickable { onHighlight(index) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Menú contextual flotante del editor de estudio (estética píldora).
 */
@Composable
fun StudyEditorFloatingMenu(
    isVisible: Boolean,
    anchorOffset: IntOffset,
    containerWidthPx: Int,
    hasSelection: Boolean,
    isParallelTextMode: Boolean = false,
    isBulletMode: Boolean = false,
    isNumberedMode: Boolean = false,
    pendingCitations: Int,
    onDismiss: () -> Unit,
    onHeadlineUp: () -> Unit,
    onHeadlineDown: () -> Unit,
    onBold: () -> Unit,
    onItalic: () -> Unit,
    onUnderline: () -> Unit,
    onUppercase: () -> Unit,
    onLowercase: () -> Unit,
    onAlignStart: () -> Unit,
    onAlignCenter: () -> Unit,
    onAlignEnd: () -> Unit,
    onTextColor: (Color) -> Unit,
    onBackgroundColor: (Color) -> Unit,
    onClearTextColor: () -> Unit,
    onClearBackground: () -> Unit,
    onClearFormatting: () -> Unit,
    onIncreaseSize: () -> Unit,
    onDecreaseSize: () -> Unit,
    onBulletList: () -> Unit,
    onOrderedList: () -> Unit,
    onInsertPendingCitations: () -> Unit,
    onInsertNote: () -> Unit,
    onInsertReflection: () -> Unit,
    onInsertTwoColumn: () -> Unit
) {
    if (!isVisible) return
    var expandedTool by remember { mutableStateOf<String?>(null) }
    val textPalette = remember {
        mutableStateListOf(
        Color(0xFF1F2937),
        Color(0xFFFFFFFF),
        Color(0xFF111827),
        Color(0xFF6B7280),
        Color(0xFF0F766E),
        Color(0xFF16A34A),
        Color(0xFF2563EB),
        Color(0xFF0891B2),
        Color(0xFF7C3AED),
        Color(0xFFDB2777),
        Color(0xFFB42318),
        Color(0xFFD97706)
        )
    }
    val backgroundPalette = remember {
        mutableStateListOf(
        Color(0xFFFFFFFF),
        Color(0xFFFFF3B0),
        Color(0xFFFFE082),
        Color(0xFFD7F9E9),
        Color(0xFFA7F3D0),
        Color(0xFFDDEBFF),
        Color(0xFFBFDBFE),
        Color(0xFFF2E3FF),
        Color(0xFFE9D5FF),
        Color(0xFFFFD9D6),
        Color(0xFFFECACA),
        Color(0xFFE5E7EB)
        )
    }

    fun addCustomColor(palette: MutableList<Color>, color: Color) {
        if (palette.none { it.value == color.value }) {
            palette.add(color)
        }
    }

    StudyEditorFloatingBubble(
        anchorOffset = anchorOffset,
        containerWidthPx = containerWidthPx,
        hasSelection = hasSelection,
        isParallelTextMode = isParallelTextMode,
        isBulletMode = isBulletMode,
        isNumberedMode = isNumberedMode,
        pendingCitations = pendingCitations,
        expandedTool = expandedTool,
        onExpandedToolChange = { expandedTool = it },
        textPalette = textPalette,
        backgroundPalette = backgroundPalette,
        onDismiss = onDismiss,
        onHeadlineUp = onHeadlineUp,
        onHeadlineDown = onHeadlineDown,
        onBold = onBold,
        onItalic = onItalic,
        onUnderline = onUnderline,
        onUppercase = onUppercase,
        onLowercase = onLowercase,
        onAlignStart = onAlignStart,
        onAlignCenter = onAlignCenter,
        onAlignEnd = onAlignEnd,
        onTextColor = onTextColor,
        onBackgroundColor = onBackgroundColor,
        onCustomTextColor = { color ->
            addCustomColor(textPalette, color)
            onTextColor(color)
        },
        onCustomBackgroundColor = { color ->
            addCustomColor(backgroundPalette, color)
            onBackgroundColor(color)
        },
        onClearTextColor = onClearTextColor,
        onClearBackground = onClearBackground,
        onClearFormatting = onClearFormatting,
        onIncreaseSize = onIncreaseSize,
        onDecreaseSize = onDecreaseSize,
        onBulletList = onBulletList,
        onOrderedList = onOrderedList,
        onInsertPendingCitations = onInsertPendingCitations,
        onInsertNote = onInsertNote,
        onInsertReflection = onInsertReflection,
        onInsertTwoColumn = onInsertTwoColumn
    )
    return

    Popup(
        alignment = Alignment.BottomCenter,
        offset = IntOffset(anchorOffset.x, anchorOffset.y - 180),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = false)
    ) {
        ElevatedCard(
            shape = RoundedCornerShape(28.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 10.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(onClick = onHeadlineUp) { Icon(Icons.Default.Title, contentDescription = "H+") }
                IconButton(onClick = onHeadlineDown) { Icon(Icons.Default.HorizontalRule, contentDescription = "H-") }
                IconButton(onClick = onBold) { Icon(Icons.Default.FormatBold, contentDescription = "Negrita") }
                IconButton(onClick = onItalic) { Icon(Icons.Default.FormatItalic, contentDescription = "Cursiva") }
                IconButton(onClick = onIncreaseSize) { Icon(Icons.Default.TextIncrease, contentDescription = "A+") }
                IconButton(onClick = onDecreaseSize) { Icon(Icons.Default.TextDecrease, contentDescription = "A-") }
                IconButton(onClick = onBulletList) { Icon(Icons.Default.FormatListBulleted, contentDescription = "Viñetas") }
                IconButton(onClick = onOrderedList) { Icon(Icons.Default.FormatListNumbered, contentDescription = "Numerada") }

                AssistChip(
                    onClick = onInsertPendingCitations,
                    label = {
                        Text(if (pendingCitations > 0) "Citar $pendingCitations" else "Citar")
                    },
                    leadingIcon = { Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }
        }
    }
}

@Composable
private fun StudyEditorFloatingBubble(
    anchorOffset: IntOffset,
    containerWidthPx: Int,
    hasSelection: Boolean,
    isParallelTextMode: Boolean,
    isBulletMode: Boolean,
    isNumberedMode: Boolean,
    pendingCitations: Int,
    expandedTool: String?,
    onExpandedToolChange: (String?) -> Unit,
    textPalette: List<Color>,
    backgroundPalette: List<Color>,
    onDismiss: () -> Unit,
    onHeadlineUp: () -> Unit,
    onHeadlineDown: () -> Unit,
    onBold: () -> Unit,
    onItalic: () -> Unit,
    onUnderline: () -> Unit,
    onUppercase: () -> Unit,
    onLowercase: () -> Unit,
    onAlignStart: () -> Unit,
    onAlignCenter: () -> Unit,
    onAlignEnd: () -> Unit,
    onTextColor: (Color) -> Unit,
    onBackgroundColor: (Color) -> Unit,
    onCustomTextColor: (Color) -> Unit,
    onCustomBackgroundColor: (Color) -> Unit,
    onClearTextColor: () -> Unit,
    onClearBackground: () -> Unit,
    onClearFormatting: () -> Unit,
    onIncreaseSize: () -> Unit,
    onDecreaseSize: () -> Unit,
    onBulletList: () -> Unit,
    onOrderedList: () -> Unit,
    onInsertPendingCitations: () -> Unit,
    onInsertNote: () -> Unit,
    onInsertReflection: () -> Unit,
    onInsertTwoColumn: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val availableWidthPx = containerWidthPx.takeIf { it > 0 }
        ?: with(density) { configuration.screenWidthDp.dp.roundToPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.roundToPx() }
    var popupSize by remember { mutableStateOf(IntSize.Zero) }
    var popupOffset by remember(anchorOffset) { mutableStateOf(IntOffset(anchorOffset.x, anchorOffset.y - 180)) }
    var customRed by remember { mutableStateOf(31f) }
    var customGreen by remember { mutableStateOf(41f) }
    var customBlue by remember { mutableStateOf(55f) }
    val customColor = Color(
        red = customRed.toInt().coerceIn(0, 255),
        green = customGreen.toInt().coerceIn(0, 255),
        blue = customBlue.toInt().coerceIn(0, 255)
    )

    fun boundedOffset(offset: IntOffset, size: IntSize = popupSize): IntOffset {
        if (size == IntSize.Zero) return offset
        val maxHorizontal = ((availableWidthPx - size.width) / 2).coerceAtLeast(0)
        val minVertical = -(screenHeightPx - size.height).coerceAtMost(screenHeightPx).coerceAtLeast(0)
        return IntOffset(
            x = offset.x.coerceIn(-maxHorizontal, maxHorizontal),
            y = offset.y.coerceIn(minVertical, 0)
        )
    }

    Popup(
        alignment = Alignment.BottomCenter,
        offset = boundedOffset(popupOffset),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = false)
    ) {
        ElevatedCard(
            modifier = Modifier
                .onGloballyPositioned { coordinates ->
                    popupSize = coordinates.size
                    popupOffset = boundedOffset(popupOffset, coordinates.size)
                }
                .pointerInput(availableWidthPx, screenHeightPx, popupSize) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        popupOffset = boundedOffset(
                            IntOffset(
                                x = popupOffset.x + dragAmount.x.roundToInt(),
                                y = popupOffset.y + dragAmount.y.roundToInt()
                            )
                        )
                    }
                },
            shape = RoundedCornerShape(28.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 10.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 372.dp)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    TextEditorGlyphButton("H1", "Titulo", hasSelection, fontWeight = FontWeight.Bold, onClick = onHeadlineUp)
                    TextEditorGlyphButton("P", "Texto normal", hasSelection, onClick = onHeadlineDown)
                    TextEditorGlyphButton("B", "Negrita", hasSelection, fontWeight = FontWeight.Black, onClick = onBold)
                    TextEditorGlyphButton("I", "Cursiva", hasSelection, fontStyle = FontStyle.Italic, onClick = onItalic)
                    TextEditorGlyphButton("U", "Subrayado", hasSelection, textDecoration = TextDecoration.Underline, onClick = onUnderline)
                    TextEditorGlyphButton("AA", "Mayusculas", hasSelection, fontWeight = FontWeight.Bold, onClick = onUppercase)
                    TextEditorGlyphButton("aa", "Minusculas", hasSelection, onClick = onLowercase)
                    TextEditorGlyphButton("|<", "Alinear izquierda", hasSelection, onClick = onAlignStart)
                    TextEditorGlyphButton("||", "Centrar texto", hasSelection, onClick = onAlignCenter)
                    TextEditorGlyphButton(">|", "Alinear derecha", hasSelection, onClick = onAlignEnd)
                    TextEditorGlyphButton("A+", "Aumentar texto", hasSelection, fontWeight = FontWeight.Bold, onClick = onIncreaseSize)
                    TextEditorGlyphButton("A-", "Reducir texto", hasSelection, fontWeight = FontWeight.Bold, onClick = onDecreaseSize)
                    TextEditorGlyphButton("Tx", "Limpiar formato", hasSelection, textDecoration = TextDecoration.LineThrough, onClick = onClearFormatting)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    EditorToolChip(Icons.Default.FormatColorText, "Color", expandedTool == "text", hasSelection) {
                        onExpandedToolChange(if (expandedTool == "text") null else "text")
                    }
                    EditorToolChip(Icons.Default.FormatColorFill, "Resaltar", expandedTool == "background", hasSelection) {
                        onExpandedToolChange(if (expandedTool == "background") null else "background")
                    }
                    EditorToolChip(
                        Icons.AutoMirrored.Filled.FormatListBulleted,
                        if (isBulletMode) "Quitar vinetas" else "Vinetas",
                        isBulletMode,
                        hasSelection,
                        onBulletList
                    )
                    EditorToolChip(
                        Icons.Default.FormatListNumbered,
                        if (isNumberedMode) "Quitar numeracion" else "Numerada",
                        isNumberedMode,
                        hasSelection,
                        onOrderedList
                    )
                    EditorToolChip(
                        Icons.Default.ViewColumn,
                        if (isParallelTextMode) "Una columna" else "Columnas",
                        isParallelTextMode,
                        hasSelection,
                        onInsertTwoColumn
                    )
                }

                when (expandedTool.takeIf { hasSelection }) {
                    "text" -> ColorTools(
                        colors = textPalette,
                        customColor = customColor,
                        onRedChange = { customRed = it },
                        onGreenChange = { customGreen = it },
                        onBlueChange = { customBlue = it },
                        onColorClick = onTextColor,
                        onApplyCustom = { onCustomTextColor(customColor) },
                        onClear = onClearTextColor,
                        clearLabel = "Auto"
                    )
                    "background" -> ColorTools(
                        colors = backgroundPalette,
                        customColor = customColor,
                        onRedChange = { customRed = it },
                        onGreenChange = { customGreen = it },
                        onBlueChange = { customBlue = it },
                        onColorClick = onBackgroundColor,
                        onApplyCustom = { onCustomBackgroundColor(customColor) },
                        onClear = onClearBackground,
                        clearLabel = "Sin fondo"
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EditorToolChip(Icons.Default.EditNote, "Nota", false, true, onInsertNote)
                    EditorToolChip(Icons.Default.Lightbulb, "Reflexion", false, true, onInsertReflection)
                    EditorToolChip(
                        Icons.Default.FormatQuote,
                        if (pendingCitations > 0) "Citar $pendingCitations" else "Citar",
                        pendingCitations > 0,
                        pendingCitations > 0,
                        onInsertPendingCitations
                    )
                }
            }
        }
    }
}

@Composable
private fun TextEditorGlyphButton(
    glyph: String,
    description: String,
    enabled: Boolean = true,
    fontWeight: FontWeight = FontWeight.SemiBold,
    fontStyle: FontStyle? = null,
    textDecoration: TextDecoration? = null,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(36.dp)
    ) {
        Text(
            text = glyph,
            modifier = Modifier.semantics { contentDescription = description },
            fontSize = if (glyph.length > 1) 13.sp else 16.sp,
            fontWeight = fontWeight,
            fontStyle = fontStyle,
            textDecoration = textDecoration,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 0.86f else 0.28f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun CompactEditorButton(
    icon: ImageVector,
    description: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(36.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 0.82f else 0.28f)
        )
    }
}

@Composable
private fun TextToolChip(
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    AssistChip(
        onClick = onClick,
        enabled = enabled,
        label = {
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        },
        shape = RoundedCornerShape(14.dp),
        border = null,
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
            labelColor = MaterialTheme.colorScheme.onSurface,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f),
            disabledLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.32f)
        ),
        modifier = Modifier.height(32.dp)
    )
}

@Composable
private fun EditorToolChip(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    AssistChip(
        onClick = onClick,
        enabled = enabled,
        label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp)) },
        shape = RoundedCornerShape(14.dp),
        border = null,
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (selected) BiblionGoldSoft.copy(alpha = 0.38f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
            labelColor = MaterialTheme.colorScheme.onSurface,
            leadingIconContentColor = if (selected) BiblionGoldPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f),
            disabledLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.32f),
            disabledLeadingIconContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.32f)
        ),
        modifier = Modifier.height(32.dp)
    )
}

@Composable
private fun ColorTools(
    colors: List<Color>,
    customColor: Color,
    onRedChange: (Float) -> Unit,
    onGreenChange: (Float) -> Unit,
    onBlueChange: (Float) -> Unit,
    onColorClick: (Color) -> Unit,
    onApplyCustom: () -> Unit,
    onClear: () -> Unit,
    clearLabel: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LazyRow(
            modifier = Modifier.widthIn(max = 340.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item {
                AssistChip(
                    onClick = onClear,
                    label = { Text(clearLabel, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            }
            items(colors) { color ->
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(color = color, shape = RoundedCornerShape(14.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                        .clickable { onColorClick(color) }
                )
            }
        }

        Row(
            modifier = Modifier.widthIn(max = 340.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(customColor, RoundedCornerShape(17.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(17.dp))
            )
            Column(modifier = Modifier.weight(1f)) {
                ColorChannelSlider("R", customColor.red * 255f, onRedChange)
                ColorChannelSlider("G", customColor.green * 255f, onGreenChange)
                ColorChannelSlider("B", customColor.blue * 255f, onBlueChange)
            }
            Button(
                onClick = onApplyCustom,
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Text("Aplicar", fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun ColorChannelSlider(label: String, value: Float, onValueChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            modifier = Modifier.width(14.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..255f,
            modifier = Modifier.height(22.dp)
        )
    }
}

@Composable
private fun ActionPill(icon: ImageVector, label: String, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp)) },
        shape = RoundedCornerShape(12.dp),
        border = null,
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        ),
        modifier = Modifier.height(30.dp)
    )
}

package com.cristiancogollo.biblion

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.HighlightOff
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.TextIncrease
import androidx.compose.material.icons.filled.TextDecrease
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.cristiancogollo.biblion.ui.theme.BiblionBluePrimary
import com.cristiancogollo.biblion.ui.theme.BiblionGoldPrimary
import com.cristiancogollo.biblion.ui.theme.BiblionGoldSoft
import com.cristiancogollo.biblion.ui.theme.BiblionNavy

private enum class AppDrawerOption(val labelRes: Int) {
    HOME(R.string.drawer_home),
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
    currentUserEmail: String? = null,
    isAuthenticated: Boolean = false,
    onClose: () -> Unit,
    onNavigateHome: () -> Unit,
    onNavigateToTeachings: () -> Unit,
    onNavigateToStudyMode: () -> Unit,
    onPickVersion: () -> Unit,
    onShowComingSoon: () -> Unit,
    onShowAbout: () -> Unit,
    onAuthActionClick: () -> Unit
) {
    val menuOptions = listOf(
        AppDrawerOption.HOME,
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
                    if (!drawerState.isOpen) return@BiblionMenuItem
                    onClose()
                    when (option) {
                        AppDrawerOption.HOME -> onNavigateHome()
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
                    stringResource(R.string.auth_account_title)
                } else {
                    stringResource(R.string.reader_label)
                }
            )
            if (!currentUserEmail.isNullOrBlank()) {
                Text(
                    text = currentUserEmail,
                    modifier = Modifier.padding(horizontal = 24.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
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
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = bookName,
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(4.dp)
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
                        ActionPill(icon = Icons.Default.EditNote, label = "Citar", onClick = onAddCitation)
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
    pendingCitations: Int,
    onDismiss: () -> Unit,
    onHeadlineUp: () -> Unit,
    onHeadlineDown: () -> Unit,
    onBold: () -> Unit,
    onItalic: () -> Unit,
    onIncreaseSize: () -> Unit,
    onDecreaseSize: () -> Unit,
    onBulletList: () -> Unit,
    onOrderedList: () -> Unit,
    onInsertPendingCitations: () -> Unit
) {
    if (!isVisible) return

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

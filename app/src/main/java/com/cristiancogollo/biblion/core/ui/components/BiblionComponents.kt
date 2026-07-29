package com.cristiancogollo.biblion

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.automirrored.filled.MenuBook
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
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

// Encabezado de marca compartido por las pantallas principales.
@Composable
fun BiblionTopAppBar(
    onNavigationIconClick: () -> Unit = {},
    onSearchIconClick: () -> Unit = {},
    logoResId: Int? = null,
    logoContentDescription: String = "",
    showSearchIcon: Boolean = true,
    showNavigationIcon: Boolean = true
) {
    val resolvedLogoContentDescription =
        logoContentDescription.ifBlank { stringResource(R.string.cd_app_logo) }
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    Surface(
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(76.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                if (logoResId != null) {
                    Image(
                        painter = painterResource(id = logoResId),
                        contentDescription = resolvedLogoContentDescription,
                        modifier = Modifier.size(42.dp),
                        contentScale = ContentScale.Fit,
                    )
                    Spacer(Modifier.width(8.dp))
                }

                Text(
                    text = stringResource(R.string.biblion_wordmark),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = onSurfaceColor,
                )
            }

            if (showNavigationIcon) {
                IconButton(
                    onClick = onNavigationIconClick,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = stringResource(R.string.cd_open_menu),
                        tint = onSurfaceColor,
                    )
                }
            }

            if (showSearchIcon) {
                IconButton(
                    onClick = onSearchIconClick,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(R.string.cd_search),
                        tint = onSurfaceColor,
                    )
                }
            }
        }
    }
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
    onNavigateToBiblion: () -> Unit,
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
                        AppDrawerOption.DOCTRINES -> onShowComingSoon()
                        AppDrawerOption.BIBLION -> onNavigateToBiblion()
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

// 2. TestamentSelector: Selector tipo pestaÃ±as (Antiguo / Nuevo)
@Composable
fun TestamentSelector(
    selectedTab: Testament?,
    onTabSelected: (Testament) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf(Testament.OLD, Testament.NEW)

    Row(
        modifier = modifier
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

// 3. DailyVerseCard: Muestra el versÃ­culo del dÃ­a
@Composable
fun DailyVerseCard(
    verse: String,
    reference: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
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

// 4. BookCard: Representa un libro en la cuadrÃ­cula
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookCard(
    bookName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colorSchema: com.cristiancogollo.biblion.feature.books.CategoryColorSchema? = null,
    onLongClick: (() -> Unit)? = null
) {
    val bg = colorSchema?.bg ?: MaterialTheme.colorScheme.surfaceVariant
    val textColor = colorSchema?.text ?: MaterialTheme.colorScheme.onSurface
    val borderStroke = colorSchema?.border?.let { BorderStroke(1.dp, it) }

    val cardModifier = if (onLongClick != null) {
        modifier
            .aspectRatio(1f)
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    } else {
        modifier
            .aspectRatio(1f)
            .fillMaxWidth()
            .clickable(onClick = onClick)
    }

    Card(
        modifier = cardModifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = borderStroke
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
                color = textColor,
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

// 5. BiblionSelectionDialog: DiÃ¡logo para elegir capÃ­tulos
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
    chapter: Int,
    fontSize: TextUnit,
    onNavigationIconClick: () -> Unit,
    selectedVersionName: String,
    onVersionClick: () -> Unit,
    onBookTitleClick: () -> Unit,
    onFontSizeChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    versionModifier: Modifier = Modifier,
    bookTitleModifier: Modifier = Modifier,
) {
    var showReadingOptions by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding() // Evita que se solape con la barra de estado
    ) {
        CenterAlignedTopAppBar(
            title = {
                Row(
                    modifier = bookTitleModifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onBookTitleClick)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = "$bookName $chapter",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Seleccionar capítulo",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
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
                TextButton(onClick = onVersionClick, modifier = versionModifier) {
                    Text(
                        text = selectedVersionName,
                        style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Serif),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Box {
                    IconButton(onClick = { showReadingOptions = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Opciones de lectura",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    DropdownMenu(
                        expanded = showReadingOptions,
                        onDismissRequest = { showReadingOptions = false },
                        modifier = Modifier.width(340.dp),
                        shape = RoundedCornerShape(24.dp),
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp,
                        shadowElevation = 16.dp,
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.TextIncrease,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(10.dp),
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Tamaño de lectura",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontFamily = FontFamily.Serif,
                                            fontWeight = FontWeight.Bold,
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = "Ajusta el texto a tu comodidad",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.primary,
                                ) {
                                    Text(
                                        text = "${fontSize.value.toInt()} sp",
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TextDecrease,
                                    contentDescription = "Texto más pequeño",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Slider(
                                    value = fontSize.value.coerceIn(12f, 35f),
                                    onValueChange = onFontSizeChange,
                                    valueRange = 12f..35f,
                                    steps = 22,
                                    modifier = Modifier.weight(1f),
                                    colors = SliderDefaults.colors(
                                        thumbColor = MaterialTheme.colorScheme.primary,
                                        activeTrackColor = MaterialTheme.colorScheme.primary,
                                        inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant,
                                    ),
                                )
                                Icon(
                                    imageVector = Icons.Default.TextIncrease,
                                    contentDescription = "Texto más grande",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                listOf(
                                    "Pequeño" to 16f,
                                    "Mediano" to 20f,
                                    "Grande" to 26f,
                                ).forEach { (label, value) ->
                                    FilterChip(
                                        selected = fontSize.value.toInt() == value.toInt(),
                                        onClick = { onFontSizeChange(value) },
                                        label = { Text(label) },
                                        modifier = Modifier.weight(1f),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                            selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = fontSize.value.toInt() == value.toInt(),
                                            borderColor = MaterialTheme.colorScheme.outlineVariant,
                                            selectedBorderColor = MaterialTheme.colorScheme.primary,
                                        ),
                                    )
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                            TextButton(
                                onClick = { showReadingOptions = false },
                                modifier = Modifier.align(Alignment.End),
                            ) {
                                Text("Listo", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = Color.Transparent
            )
        )

        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
    }
}

/**
 * Burbuja contextual flotante optimizada para acciones sobre versÃ­culos.
 * Ubicada en la parte inferior-media para mejor ergonomÃ­a.
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
    onHighlight: (Int) -> Unit,
    onInsertAsQuote: (() -> Unit)? = null,
) {
    val popupOffset = if (anchorOffset == IntOffset.Zero) IntOffset(0, -32) else anchorOffset

    Popup(
        alignment = Alignment.BottomCenter,
        offset = popupOffset,
        onDismissRequest = onDismiss,
        properties = PopupProperties(
            focusable = false,
            dismissOnClickOutside = false
        )
    ) {
        AnimatedVisibility(
            visible = true,
            enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut()
        ) {
            ElevatedCard(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .widthIn(max = 420.dp),
                shape = RoundedCornerShape(24.dp),
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

                        if (onInsertAsQuote != null) {
                            ActionPill(icon = Icons.Default.Add, label = "Insertar cita", onClick = onInsertAsQuote)
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
}

@Composable
private fun LegacyVerseActionsFloatingMenu(
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
        offset = IntOffset(0, -180), // PosiciÃ³n interactiva en la zona inferior-media
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

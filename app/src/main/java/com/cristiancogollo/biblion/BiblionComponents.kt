package com.cristiancogollo.biblion

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.cristiancogollo.biblion.ui.theme.BiblionNavy

// 1. BiblionTopAppBar: Usado en HomeScreen y BooksScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BiblionTopAppBar(
    onNavigationIconClick: () -> Unit = {},
    onSearchIconClick: () -> Unit = {}
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = "Biblion",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold
                ),
                color = BiblionNavy
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigationIconClick) {
                Icon(imageVector = Icons.Default.Menu, contentDescription = "Menú", tint = BiblionNavy)
            }
        },
        actions = {
            IconButton(onClick = onSearchIconClick) {
                Icon(imageVector = Icons.Default.Search, contentDescription = "Buscar", tint = BiblionNavy)
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color.White
        )
    )
}

// 2. TestamentSelector: Selector tipo pestañas (Antiguo / Nuevo)
@Composable
fun TestamentSelector(
    selectedTab: Testament,
    onTabSelected: (Testament) -> Unit
) {
    val tabs = listOf(Testament.OLD, Testament.NEW)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .height(50.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF0F0F0)),
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
                    containerColor = if (selected) BiblionNavy else Color.Transparent,
                    contentColor = if (selected) Color.White else Color.Gray
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = testament.shortLabel,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 13.sp
                )
            }
        }
    }
}

// 3. DailyVerseCard: Muestra el versículo del día
@Composable
fun DailyVerseCard(verse: String, reference: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
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
                color = Color.Black,
                fontFamily = FontFamily.Serif
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = reference,
                style = MaterialTheme.typography.labelLarge,
                color = BiblionNavy,
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
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDFBF0)),
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
                color = BiblionNavy,
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
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(text = title, style = MaterialTheme.typography.headlineSmall, color = BiblionNavy)
                    Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
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
                        Text("CANCELAR", color = BiblionNavy)
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
            .background(Color.White)
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
                    color = BiblionNavy,
                    modifier = Modifier.clickable(onClick = onBookTitleClick)
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigationIconClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = BiblionNavy
                    )
                }
            },
            actions = {
                IconButton(onClick = onSearchIconClick) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Buscar",
                        tint = BiblionNavy
                    )
                }
                // Botón Disminuir Fuente
                IconButton(onClick = onDecreaseFontSize) {
                    Icon(
                        imageVector = Icons.Default.HorizontalRule,
                        contentDescription = "Menos letra",
                        tint = BiblionNavy
                    )
                }
                // Botón Aumentar Fuente
                IconButton(onClick = onIncreaseFontSize) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Más letra",
                        tint = BiblionNavy
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
                    color = if (isSelected) BiblionNavy else Color.Gray,
                    modifier = Modifier
                        .clickable { onChapterClick(chapter) }
                        .padding(vertical = 4.dp)
                )
            }
        }

        // Línea divisoria sutil
        HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
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
                        color = BiblionNavy,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = "$selectedCount",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
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
                            tint = Color.Gray
                        )
                    }
                }

                if (showHighlightOptions) {
                    HorizontalDivider(
                        modifier = Modifier.width(60.dp).padding(vertical = 4.dp),
                        color = Color.LightGray.copy(alpha = 0.2f)
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
                                        color = if (index == 0) Color.White else color,
                                        shape = RoundedCornerShape(13.dp)
                                    )
                                    .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(13.dp))
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

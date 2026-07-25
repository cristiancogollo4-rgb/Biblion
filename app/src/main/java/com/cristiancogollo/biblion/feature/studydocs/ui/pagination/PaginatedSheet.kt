package com.cristiancogollo.biblion.feature.studydocs.ui.pagination

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.produceState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.toSize
import com.cristiancogollo.biblion.feature.studydocs.model.PageDimensions
import com.cristiancogollo.biblion.feature.studydocs.model.StudyBlock
import com.cristiancogollo.biblion.feature.studydocs.debug.StudyEditorDebugLog
import com.cristiancogollo.biblion.feature.studydocs.ui.editor.calibratePageGapHeights
import com.cristiancogollo.biblion.feature.studydocs.ui.editor.styledText
import com.cristiancogollo.biblion.feature.studydocs.ui.editor.DocumentZoomState
import com.cristiancogollo.biblion.feature.studydocs.ui.editor.rememberDocumentZoomState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private const val EDITING_PAGINATION_DEBOUNCE_MS = 64L

private val DarkStudyWorkspace = Color(0xFF050608)
private val DarkStudyPage = Color(0xFF181A20)
private val DarkStudyPageBorder = Color(0xFF343740)
private val LightStudyWorkspace = Color(0xFFE8EAED)

/**
 * Lienzo del editor con vista paginada o continua.
 *
 * La paginacion usa la densidad normal y el zoom se aplica al medir el lienzo
 * mediante una densidad virtual. Los ScrollState nativos siguen siendo la
 * unica autoridad del paneo, igual que en un documento largo convencional.
 */
@Composable
fun PaginatedSheet(
    blocks: List<StudyBlock>,
    isEditing: Boolean,
    modifier: Modifier = Modifier,
    zoomState: DocumentZoomState = rememberDocumentZoomState(),
    viewMode: SheetViewMode = SheetViewMode.PAGINATED,
    contentFragmentRenderer: @Composable (PageFragment, Int) -> Unit = { _, _ -> },
    contentBlockRenderer: @Composable (StudyBlock, Int) -> Unit = { _, _ -> },
    contentPagedEditorRenderer: @Composable (PagedEditorUnit, Density) -> Unit = { _, _ -> },
) {
    if (viewMode == SheetViewMode.PAGELESS) {
        PagelessSheet(
            blocks = blocks,
            isEditing = isEditing,
            modifier = modifier,
            zoomState = zoomState,
            contentBlockRenderer = contentBlockRenderer,
        )
    } else {
        PaginatedSheetImpl(
            blocks = blocks,
            isEditing = isEditing,
            modifier = modifier,
            zoomState = zoomState,
            contentFragmentRenderer = contentFragmentRenderer,
            contentPagedEditorRenderer = contentPagedEditorRenderer,
        )
    }
}

@Composable
private fun PaginatedSheetImpl(
    blocks: List<StudyBlock>,
    isEditing: Boolean,
    modifier: Modifier,
    zoomState: DocumentZoomState,
    contentFragmentRenderer: @Composable (PageFragment, Int) -> Unit,
    contentPagedEditorRenderer: @Composable (PagedEditorUnit, Density) -> Unit,
) {
    val density = LocalDensity.current
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val workspaceColor = if (isDarkTheme) DarkStudyWorkspace else LightStudyWorkspace
    val textMeasurer = rememberTextMeasurer()
    val pageWidthPx = with(density) {
        (PageDimensions.LETTER_WIDTH - PageDimensions.PAGE_MARGIN * 2).toPx()
    }
    val pageHeightPx = with(density) {
        (
            PageDimensions.LETTER_HEIGHT -
                PageDimensions.PAGE_MARGIN * 2 -
                PageDimensions.FOOTER_HEIGHT
            ).toPx()
    }

    // The editor itself already renders the live RichTextState. Pagination can
    // settle a few frames later without restarting the active text field for
    // every character in a continuous typing burst.
    val paginationBlocks by produceState(
        initialValue = blocks,
        key1 = blocks,
        key2 = isEditing,
    ) {
        if (isEditing) delay(EDITING_PAGINATION_DEBOUNCE_MS)
        value = blocks
    }

    val pages by produceState(
        initialValue = emptyList<Page>(),
        key1 = paginationBlocks,
        key2 = pageWidthPx,
        key3 = pageHeightPx,
    ) {
        StudyEditorDebugLog.log(
            "PAGINATE_START",
            "mode=paginated blocks=${StudyEditorDebugLog.blocksSummary(paginationBlocks)} " +
                "widthPx=$pageWidthPx heightPx=$pageHeightPx",
        )
        value = withContext(Dispatchers.Default) {
            PaginationEngine.paginate(
                blocks = paginationBlocks,
                pageWidthPx = pageWidthPx,
                pageHeightPx = pageHeightPx,
                textMeasurer = textMeasurer,
                density = density,
            )
        }
        StudyEditorDebugLog.log(
            "PAGINATE_DONE",
            "mode=paginated pages=${value.size} fragments=${value.sumOf { it.fragments.size }} " +
                "blocks=${StudyEditorDebugLog.blocksSummary(paginationBlocks)}",
        )
    }

    val animatedZoom by animateFloatAsState(
        targetValue = zoomState.zoom,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "documentZoom",
    )
    val virtualDensity = Density(
        density = density.density * animatedZoom,
        fontScale = density.fontScale,
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .clipToBounds()
            .background(workspaceColor),
    ) {
        val horizontalPadding = 48.dp
        val baseCanvasWidth = maxOf(maxWidth, PageDimensions.LETTER_WIDTH + horizontalPadding)
        // Keep the scaled canvas at least as wide as the viewport. Without this
        // lower bound, zoom values below 1x anchor the page to the left because
        // horizontalScroll cannot represent the negative offset needed to center it.
        val canvasWidth = maxOf(baseCanvasWidth, maxWidth / animatedZoom)
        val canvasHeight = 48.dp +
            (PageDimensions.LETTER_HEIGHT * pages.size) +
            (20.dp * (pages.size - 1).coerceAtLeast(0))
        val pageGapPx = with(density) { 20.dp.toPx() }
        val pageMarginPx = with(density) { PageDimensions.PAGE_MARGIN.toPx() }
        val canvasVerticalPaddingPx = with(density) { 24.dp.toPx() }
        val rawEditorUnits = remember(
            pages,
            pageHeightPx,
            pageGapPx,
            pageMarginPx,
            canvasVerticalPaddingPx,
        ) {
            buildPagedEditorUnits(
                pages = pages,
                pageHeightPx = with(density) { PageDimensions.LETTER_HEIGHT.toPx() },
                pageGapPx = pageGapPx,
                pageMarginPx = pageMarginPx,
                canvasVerticalPaddingPx = canvasVerticalPaddingPx,
            )
        }
        val editorUnits = remember(rawEditorUnits, pageWidthPx, density) {
            rawEditorUnits.map { unit ->
                val unitWidthPx = when (unit.block) {
                    is StudyBlock.BulletList,
                    is StudyBlock.OrderedList,
                    -> pageWidthPx - with(density) { 40.dp.toPx() }
                    is StudyBlock.Quote -> pageWidthPx - with(density) {
                        PaginationEngine.QUOTE_INDENT.toPx()
                    }
                    else -> pageWidthPx
                }.coerceAtLeast(1f)
                calibratePageGapHeights(
                    unit = unit,
                    styledText = unit.styledText(),
                    textStyle = PaginationEngine.textStyleFor(unit.block, density),
                    widthPx = unitWidthPx,
                    density = density,
                    textMeasurer = textMeasurer,
                )
            }
        }
        val baseContentSize = with(density) {
            Size(baseCanvasWidth.toPx(), canvasHeight.toPx())
        }
        val horizontalScrollState = rememberScrollState()
        val verticalScrollState = rememberScrollState()

        LaunchedEffect(baseContentSize) {
            zoomState.updateContent(baseContentSize)
        }
        val viewportWidthPx = with(density) { maxWidth.toPx() }
        val horizontalScrollMax = horizontalScrollState.maxValue
        LaunchedEffect(baseContentSize.width, viewportWidthPx, animatedZoom, horizontalScrollMax) {
            val scaledWidth = baseContentSize.width * animatedZoom
            val requestedOffset = ((scaledWidth - viewportWidthPx).coerceAtLeast(0f) / 2f)
                .roundToInt()
            val centeredOffset = requestedOffset.coerceIn(0, horizontalScrollMax)
            StudyEditorDebugLog.log(
                "SHEET_CENTER_REQUEST",
                "mode=paginated canvasPx=${baseContentSize.width} viewportPx=$viewportWidthPx " +
                    "zoom=$animatedZoom scaledPx=$scaledWidth requested=$requestedOffset " +
                    "max=$horizontalScrollMax applied=$centeredOffset current=${horizontalScrollState.value}",
            )
            horizontalScrollState.scrollTo(centeredOffset)
            StudyEditorDebugLog.log(
                "SHEET_CENTER_APPLIED",
                "mode=paginated value=${horizontalScrollState.value} max=${horizontalScrollState.maxValue}",
            )
        }
        LaunchedEffect(horizontalScrollState) {
            snapshotFlow { horizontalScrollState.value to horizontalScrollState.maxValue }
                .collect { (value, max) ->
                    StudyEditorDebugLog.log(
                        "SHEET_SCROLL",
                        "mode=paginated value=$value max=$max zoom=${zoomState.zoom}",
                    )
                }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged {
                    StudyEditorDebugLog.log(
                        "SHEET_VIEWPORT",
                        "mode=paginated widthPx=${it.width} heightPx=${it.height}",
                    )
                    zoomState.updateViewport(it.toSize())
                }
                .documentTransformGestures(zoomState)
                .pointerInput(isEditing) {
                    if (!isEditing) {
                        detectTapGestures(onDoubleTap = { zoomState.reset() })
                    }
                },
        ) {
            if (pages.isEmpty()) {
                Text(
                    text = "Documento sin contenido paginable",
                    modifier = Modifier.padding(16.dp),
                )
            } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .horizontalScroll(horizontalScrollState)
                            .verticalScroll(verticalScrollState),
                    ) {
                        CompositionLocalProvider(LocalDensity provides virtualDensity) {
                            Box(
                                modifier = Modifier
                                    .width(canvasWidth)
                                    .height(canvasHeight),
                            ) {
                                Column(
                                    modifier = Modifier
                                        .width(canvasWidth)
                                        .padding(vertical = 24.dp),
                                    verticalArrangement = Arrangement.spacedBy(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    pages.forEach { page ->
                                        key(page.index) {
                                            PageCard(
                                                page = page,
                                                contentFragmentRenderer = contentFragmentRenderer,
                                                editorOverlay = isEditing,
                                            )
                                        }
                                    }
                                }
                                if (isEditing) {
                                    val contentX = (canvasWidth - PageDimensions.LETTER_WIDTH) / 2 +
                                        PageDimensions.PAGE_MARGIN
                                    val contentWidth = PageDimensions.LETTER_WIDTH -
                                        PageDimensions.PAGE_MARGIN * 2
                                    editorUnits.forEach { unit ->
                                        key(unit.key) {
                                            Box(
                                                propagateMinConstraints = true,
                                                modifier = Modifier
                                                    .offset(
                                                        x = contentX,
                                                        y = with(density) { unit.topPx.toDp() },
                                                    )
                                                    .width(contentWidth)
                                                    .height(with(density) { unit.heightPx.toDp() })
                                                    .clipToBounds(),
                                            ) {
                                                contentPagedEditorRenderer(unit, density)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
    }
}

@Composable
private fun PagelessSheet(
    blocks: List<StudyBlock>,
    isEditing: Boolean,
    modifier: Modifier,
    zoomState: DocumentZoomState,
    contentBlockRenderer: @Composable (StudyBlock, Int) -> Unit,
) {
    val density = LocalDensity.current
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val pageColor = if (isDarkTheme) DarkStudyPage else MaterialTheme.colorScheme.surface
    val horizontalScrollState = rememberScrollState()
    val verticalScrollState = rememberScrollState()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .clipToBounds()
            .background(pageColor),
    ) {
        LaunchedEffect(blocks) {
            StudyEditorDebugLog.log(
                "PAGINATE_CONTENT_CHANGE",
                "mode=pageless blocks=${StudyEditorDebugLog.blocksSummary(blocks)}",
            )
        }
        val animatedZoom by animateFloatAsState(
            targetValue = zoomState.zoom,
            animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
            label = "documentZoom",
        )
        val baseCanvasWidth = maxWidth.coerceAtLeast(320.dp)
        // Preserve a viewport-sized scaled canvas at low zoom so the page stays
        // centered instead of becoming left-anchored.
        val canvasWidth = maxOf(baseCanvasWidth, maxWidth / animatedZoom)
        val baseContentSize = with(density) {
            Size(baseCanvasWidth.toPx(), 1f)
        }
        LaunchedEffect(baseContentSize) {
            zoomState.updateContent(baseContentSize)
        }
        val virtualDensity = Density(
            density = density.density * animatedZoom,
            fontScale = density.fontScale,
        )
        val viewportWidthPx = with(density) { maxWidth.toPx() }
        val horizontalScrollMax = horizontalScrollState.maxValue
        LaunchedEffect(baseContentSize.width, viewportWidthPx, animatedZoom, horizontalScrollMax) {
            val scaledWidth = baseContentSize.width * animatedZoom
            val requestedOffset = ((scaledWidth - viewportWidthPx).coerceAtLeast(0f) / 2f)
                .roundToInt()
            val centeredOffset = requestedOffset.coerceIn(0, horizontalScrollMax)
            StudyEditorDebugLog.log(
                "SHEET_CENTER_REQUEST",
                "mode=pageless canvasPx=${baseContentSize.width} viewportPx=$viewportWidthPx " +
                    "zoom=$animatedZoom scaledPx=$scaledWidth requested=$requestedOffset " +
                    "max=$horizontalScrollMax applied=$centeredOffset current=${horizontalScrollState.value}",
            )
            horizontalScrollState.scrollTo(centeredOffset)
            StudyEditorDebugLog.log(
                "SHEET_CENTER_APPLIED",
                "mode=pageless value=${horizontalScrollState.value} max=${horizontalScrollState.maxValue}",
            )
        }
        LaunchedEffect(horizontalScrollState) {
            snapshotFlow { horizontalScrollState.value to horizontalScrollState.maxValue }
                .collect { (value, max) ->
                    StudyEditorDebugLog.log(
                        "SHEET_SCROLL",
                        "mode=pageless value=$value max=$max zoom=${zoomState.zoom}",
                    )
                }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged {
                    StudyEditorDebugLog.log(
                        "SHEET_VIEWPORT",
                        "mode=pageless widthPx=${it.width} heightPx=${it.height}",
                    )
                    zoomState.updateViewport(it.toSize())
                }
                .documentTransformGestures(zoomState)
                .pointerInput(isEditing) {
                    if (!isEditing) {
                        detectTapGestures(onDoubleTap = { zoomState.reset() })
                    }
                },
        ) {
            if (blocks.isEmpty()) {
                Text("Documento sin contenido", modifier = Modifier.padding(16.dp))
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .horizontalScroll(horizontalScrollState)
                        .verticalScroll(verticalScrollState),
                ) {
                    CompositionLocalProvider(LocalDensity provides virtualDensity) {
                        Column(
                            modifier = Modifier
                                .width(canvasWidth)
                                .padding(vertical = 24.dp, horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            blocks.forEachIndexed { idx, block ->
                                key(block.id) {
                                    contentBlockRenderer(block, idx)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun Modifier.documentTransformGestures(
    zoomState: DocumentZoomState,
): Modifier = pointerInput(zoomState) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        StudyEditorDebugLog.log(
            "SHEET_GESTURE_START",
            "zoom=${zoomState.zoom}",
        )
        var gestureZoom = zoomState.zoom
        var event = awaitPointerEvent(PointerEventPass.Initial)
        while (event.changes.any { it.pressed }) {
            val pressedCount = event.changes.count { it.pressed }
            if (pressedCount >= 2) {
                val zoomChange = event.calculateZoom()
                if (zoomChange.isFinite() && zoomChange != 1f) {
                    gestureZoom = (gestureZoom * zoomChange).coerceIn(
                        DocumentZoomState.MIN_ZOOM,
                        DocumentZoomState.MAX_ZOOM,
                    )
                    zoomState.set(gestureZoom)
                    StudyEditorDebugLog.log(
                        "SHEET_PINCH",
                        "zoomChange=$zoomChange zoom=$gestureZoom pointers=$pressedCount",
                    )
                }
                event.changes.forEach { it.consume() }
            } else if (pressedCount == 1) {
                // A second finger may be added after a one-finger scroll.
                // Start the next pinch from the current committed zoom.
                gestureZoom = zoomState.zoom
            }
            event = awaitPointerEvent(PointerEventPass.Initial)
        }
        StudyEditorDebugLog.log(
            "SHEET_GESTURE_END",
            "zoom=${zoomState.zoom}",
        )
    }
}

@Composable
private fun PageCard(
    page: Page,
    contentFragmentRenderer: @Composable (PageFragment, Int) -> Unit,
    editorOverlay: Boolean = false,
) {
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val pageColor = if (isDarkTheme) DarkStudyPage else MaterialTheme.colorScheme.surface
    Card(
        modifier = Modifier
            .width(PageDimensions.LETTER_WIDTH)
            .height(PageDimensions.LETTER_HEIGHT),
        colors = CardDefaults.cardColors(containerColor = pageColor),
        border = BorderStroke(
            width = 1.dp,
            color = if (isDarkTheme) DarkStudyPageBorder else MaterialTheme.colorScheme.outlineVariant,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PageDimensions.PAGE_MARGIN),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                page.fragments.forEachIndexed { idx, fragment ->
                    if (editorOverlay &&
                        fragment !is PageFragment.VerseSlice &&
                        fragment !is PageFragment.Whole
                    ) {
                        return@forEachIndexed
                    }
                    key(fragment.compositionKey()) {
                        contentFragmentRenderer(fragment, idx)
                    }
                }
            }
            Text(
                text = "${page.index + 1}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PageDimensions.FOOTER_HEIGHT),
            )
        }
    }
}

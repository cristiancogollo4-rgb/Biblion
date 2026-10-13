package com.cristiancogollo.biblion.feature.studydocs.ui.pagination

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
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
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private const val EDITING_PAGINATION_DEBOUNCE_MS = 64L
private val paginationDispatcher = Dispatchers.Default.limitedParallelism(1)

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
    activeEditorKey: PagedEditorUnitKey? = null,
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
            activeEditorKey = activeEditorKey,
            contentBlockRenderer = contentBlockRenderer,
        )
    } else {
        PaginatedSheetImpl(
            blocks = blocks,
            isEditing = isEditing,
            modifier = modifier,
            zoomState = zoomState,
            activeEditorKey = activeEditorKey,
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
    activeEditorKey: PagedEditorUnitKey?,
    contentFragmentRenderer: @Composable (PageFragment, Int) -> Unit,
    contentPagedEditorRenderer: @Composable (PagedEditorUnit, Density) -> Unit,
) {
    val density = LocalDensity.current
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val workspaceColor = if (isDarkTheme) DarkStudyWorkspace else LightStudyWorkspace
    // Pagination runs off the main thread while gap calibration runs during
    // composition. Separate measurers prevent their internal caches from
    // being accessed concurrently.
    val paginationTextMeasurer = rememberTextMeasurer()
    val editorTextMeasurer = rememberTextMeasurer()
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
        StudyEditorDebugLog.log("PAGINATE_START") {
            "mode=paginated blockCount=${paginationBlocks.size} " +
                "widthPx=$pageWidthPx heightPx=$pageHeightPx"
        }
        value = withContext(paginationDispatcher) {
            val paginationContext = currentCoroutineContext()
            PaginationEngine.paginate(
                blocks = paginationBlocks,
                pageWidthPx = pageWidthPx,
                pageHeightPx = pageHeightPx,
                textMeasurer = paginationTextMeasurer,
                density = density,
                cancellationCheck = { paginationContext.ensureActive() },
            )
        }
        StudyEditorDebugLog.log("PAGINATE_DONE") {
            "mode=paginated pages=${value.size} fragments=${value.sumOf { it.fragments.size }} " +
                "blockCount=${paginationBlocks.size}"
        }
    }

    // Only the committed zoom affects measurement. The pinch preview is a
    // graphics-layer transform, so text is laid out once after fingers lift.
    val renderedZoom = zoomState.zoom
    val virtualDensity = Density(
        density = density.density * renderedZoom,
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
        val canvasWidth = maxOf(baseCanvasWidth, maxWidth / renderedZoom)
        val canvasHeight = 48.dp +
            (PageDimensions.LETTER_HEIGHT * pages.size) +
            (20.dp * (pages.size - 1).coerceAtLeast(0))
        val pageGapPx = with(density) { 20.dp.toPx() }
        val pageMarginPx = with(density) { PageDimensions.PAGE_MARGIN.toPx() }
        val canvasVerticalPaddingPx = with(density) { 24.dp.toPx() }
        val horizontalScrollState = rememberScrollState()
        val verticalScrollState = rememberScrollState()
        var viewportHeightPx by remember { mutableIntStateOf(0) }
        var previousRenderedZoom by remember { mutableFloatStateOf(renderedZoom) }
        LaunchedEffect(renderedZoom, viewportHeightPx) {
            val oldZoom = previousRenderedZoom
            if (oldZoom != renderedZoom && oldZoom > 0f) {
                previousRenderedZoom = renderedZoom
                val target = anchoredScrollOffset(
                    scrollOffsetPx = verticalScrollState.value,
                    viewportPx = viewportHeightPx,
                    anchorFraction = zoomState.gestureAnchorFraction.y,
                    zoomRatio = renderedZoom / oldZoom,
                    maxOffsetPx = verticalScrollState.maxValue,
                )
                verticalScrollState.scrollTo(target)
            }
        }
        val visiblePages = visiblePageRange(
            scrollOffsetPx = verticalScrollState.value,
            viewportHeightPx = viewportHeightPx,
            pageHeightPx = with(density) { PageDimensions.LETTER_HEIGHT.toPx() } * renderedZoom,
            pageGapPx = pageGapPx * renderedZoom,
            topPaddingPx = canvasVerticalPaddingPx * renderedZoom,
            pageCount = pages.size,
        )
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
        val editorUnits = remember(rawEditorUnits, visiblePages, activeEditorKey, pageWidthPx, density) {
            val firstVisiblePx = canvasVerticalPaddingPx +
                visiblePages.first * (with(density) { PageDimensions.LETTER_HEIGHT.toPx() } + pageGapPx)
            val lastVisiblePx = canvasVerticalPaddingPx +
                (visiblePages.last + 1) * (with(density) { PageDimensions.LETTER_HEIGHT.toPx() } + pageGapPx)
            rawEditorUnits.asSequence().filter { unit ->
                unit.key == activeEditorKey ||
                    (unit.topPx <= lastVisiblePx && unit.topPx + unit.heightPx >= firstVisiblePx)
            }.map { unit ->
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
                    textMeasurer = editorTextMeasurer,
                )
            }.toList()
        }
        val baseContentSize = with(density) {
            Size(baseCanvasWidth.toPx(), canvasHeight.toPx())
        }

        LaunchedEffect(baseContentSize) {
            zoomState.updateContent(baseContentSize)
        }
        val viewportWidthPx = with(density) { maxWidth.toPx() }
        val horizontalScrollMax = horizontalScrollState.maxValue
        LaunchedEffect(baseContentSize.width, viewportWidthPx, renderedZoom, horizontalScrollMax, zoomState.isGesturing) {
            if (zoomState.isGesturing) return@LaunchedEffect
            val scaledWidth = baseContentSize.width * renderedZoom
            val requestedOffset = ((scaledWidth - viewportWidthPx).coerceAtLeast(0f) / 2f)
                .roundToInt()
            val centeredOffset = requestedOffset.coerceIn(0, horizontalScrollMax)
            StudyEditorDebugLog.log("SHEET_CENTER_REQUEST") {
                "mode=paginated canvasPx=${baseContentSize.width} viewportPx=$viewportWidthPx " +
                    "zoom=$renderedZoom scaledPx=$scaledWidth requested=$requestedOffset " +
                    "max=$horizontalScrollMax applied=$centeredOffset current=${horizontalScrollState.value}"
            }
            horizontalScrollState.scrollTo(centeredOffset)
            StudyEditorDebugLog.log("SHEET_CENTER_APPLIED") {
                "mode=paginated value=${horizontalScrollState.value} max=${horizontalScrollState.maxValue}"
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged {
                    StudyEditorDebugLog.log("SHEET_VIEWPORT") {
                        "mode=paginated widthPx=${it.width} heightPx=${it.height}"
                    }
                    zoomState.updateViewport(it.toSize())
                    viewportHeightPx = it.height
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
                            .zoomGesturePreview(zoomState)
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
                                    val pagesBefore = visiblePages.first
                                    if (pagesBefore > 0) {
                                        Spacer(
                                            Modifier.height(
                                                PageDimensions.LETTER_HEIGHT * pagesBefore.toFloat() +
                                                    20.dp * (pagesBefore - 1).toFloat()
                                            )
                                        )
                                    }
                                    visiblePages.forEach { pageIndex ->
                                        key(pageIndex) {
                                            PageCard(
                                                page = pages[pageIndex],
                                                contentFragmentRenderer = contentFragmentRenderer,
                                                editorOverlay = isEditing,
                                                baseDensity = density,
                                            )
                                        }
                                    }
                                    val pagesAfter = pages.size - visiblePages.last - 1
                                    if (pagesAfter > 0) {
                                        Spacer(
                                            Modifier.height(
                                                PageDimensions.LETTER_HEIGHT * pagesAfter.toFloat() +
                                                    20.dp * (pagesAfter - 1).toFloat()
                                            )
                                        )
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
    activeEditorKey: PagedEditorUnitKey?,
    contentBlockRenderer: @Composable (StudyBlock, Int) -> Unit,
) {
    val density = LocalDensity.current
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val pageColor = if (isDarkTheme) DarkStudyPage else MaterialTheme.colorScheme.surface
    val horizontalScrollState = rememberScrollState()
    val blockListState = rememberLazyListState()

    LaunchedEffect(activeEditorKey?.blockId, blocks.size) {
        if (!isEditing) return@LaunchedEffect
        val activeIndex = blocks.indexOfFirst { it.id == activeEditorKey?.blockId }
        if (activeIndex >= 0 && blockListState.layoutInfo.visibleItemsInfo.none { it.index == activeIndex }) {
            blockListState.scrollToItem(activeIndex)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .clipToBounds()
            .background(pageColor),
    ) {
        val renderedZoom = zoomState.zoom
        val baseCanvasWidth = maxWidth.coerceAtLeast(320.dp)
        // Preserve a viewport-sized scaled canvas at low zoom so the page stays
        // centered instead of becoming left-anchored.
        val canvasWidth = maxOf(baseCanvasWidth, maxWidth / renderedZoom)
        val baseContentSize = with(density) {
            Size(baseCanvasWidth.toPx(), 1f)
        }
        LaunchedEffect(baseContentSize) {
            zoomState.updateContent(baseContentSize)
        }
        val virtualDensity = Density(
            density = density.density * renderedZoom,
            fontScale = density.fontScale,
        )
        val viewportWidthPx = with(density) { maxWidth.toPx() }
        val horizontalScrollMax = horizontalScrollState.maxValue
        LaunchedEffect(baseContentSize.width, viewportWidthPx, renderedZoom, horizontalScrollMax, zoomState.isGesturing) {
            if (zoomState.isGesturing) return@LaunchedEffect
            val scaledWidth = baseContentSize.width * renderedZoom
            val requestedOffset = ((scaledWidth - viewportWidthPx).coerceAtLeast(0f) / 2f)
                .roundToInt()
            val centeredOffset = requestedOffset.coerceIn(0, horizontalScrollMax)
            StudyEditorDebugLog.log("SHEET_CENTER_REQUEST") {
                "mode=pageless canvasPx=${baseContentSize.width} viewportPx=$viewportWidthPx " +
                    "zoom=$renderedZoom scaledPx=$scaledWidth requested=$requestedOffset " +
                    "max=$horizontalScrollMax applied=$centeredOffset current=${horizontalScrollState.value}"
            }
            horizontalScrollState.scrollTo(centeredOffset)
            StudyEditorDebugLog.log("SHEET_CENTER_APPLIED") {
                "mode=pageless value=${horizontalScrollState.value} max=${horizontalScrollState.maxValue}"
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged {
                    StudyEditorDebugLog.log("SHEET_VIEWPORT") {
                        "mode=pageless widthPx=${it.width} heightPx=${it.height}"
                    }
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
                        .zoomGesturePreview(zoomState)
                        .horizontalScroll(horizontalScrollState),
                ) {
                    CompositionLocalProvider(LocalDensity provides virtualDensity) {
                        LazyColumn(
                            state = blockListState,
                            modifier = Modifier
                                .width(canvasWidth)
                                .fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 24.dp, horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            itemsIndexed(
                                items = blocks,
                                key = { index, block -> "${block.id.value}:$index" },
                            ) { idx, block ->
                                contentBlockRenderer(block, idx)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun Modifier.zoomGesturePreview(zoomState: DocumentZoomState): Modifier =
    graphicsLayer {
        val scale = zoomState.visualScale
        scaleX = scale
        scaleY = scale
        val anchor = zoomState.gestureAnchorFraction
        transformOrigin = TransformOrigin(anchor.x, anchor.y)
    }

private fun Modifier.documentTransformGestures(
    zoomState: DocumentZoomState,
): Modifier = pointerInput(zoomState) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        StudyEditorDebugLog.log("SHEET_GESTURE_START") {
            "zoom=${zoomState.zoom}"
        }
        var didPinch = false
        var event = awaitPointerEvent(PointerEventPass.Initial)
        try {
            while (event.changes.any { it.pressed }) {
                val pressedCount = event.changes.count { it.pressed }
                if (pressedCount >= 2) {
                    if (!didPinch) {
                        val centroid = event.calculateCentroid()
                        zoomState.beginGesture(
                            Offset(
                                if (centroid.x.isFinite() && size.width > 0) centroid.x / size.width else 0.5f,
                                if (centroid.y.isFinite() && size.height > 0) centroid.y / size.height else 0.5f,
                            ),
                        )
                        didPinch = true
                    }
                    zoomState.updateGesture(event.calculateZoom())
                    event.changes.forEach { it.consume() }
                }
                event = awaitPointerEvent(PointerEventPass.Initial)
            }
        } finally {
            if (didPinch) zoomState.endGesture()
        }
        StudyEditorDebugLog.log("SHEET_GESTURE_END") {
            "zoom=${zoomState.zoom}"
        }
    }
}

/** Inclusive page window, including a small prefetch margin above and below. */
internal fun visiblePageRange(
    scrollOffsetPx: Int,
    viewportHeightPx: Int,
    pageHeightPx: Float,
    pageGapPx: Float,
    topPaddingPx: Float,
    pageCount: Int,
    overscanPages: Int = 2,
): IntRange {
    if (pageCount == 0) return IntRange.EMPTY
    val stride = (pageHeightPx + pageGapPx).coerceAtLeast(1f)
    val first = (((scrollOffsetPx - topPaddingPx) / stride).toInt() - overscanPages)
        .coerceIn(0, pageCount - 1)
    val last = (((scrollOffsetPx + viewportHeightPx - topPaddingPx) / stride).toInt() + overscanPages)
        .coerceIn(first, pageCount - 1)
    return first..last
}

internal fun anchoredScrollOffset(
    scrollOffsetPx: Int,
    viewportPx: Int,
    anchorFraction: Float,
    zoomRatio: Float,
    maxOffsetPx: Int,
): Int {
    val anchorPx = viewportPx * anchorFraction.coerceIn(0f, 1f)
    return ((scrollOffsetPx + anchorPx) * zoomRatio - anchorPx)
        .roundToInt()
        .coerceIn(0, maxOffsetPx.coerceAtLeast(0))
}

@Composable
private fun PageCard(
    page: Page,
    contentFragmentRenderer: @Composable (PageFragment, Int) -> Unit,
    editorOverlay: Boolean = false,
    baseDensity: Density = LocalDensity.current,
) {
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val pageColor = if (isDarkTheme) DarkStudyPage else MaterialTheme.colorScheme.surface
    Card(
        modifier = Modifier
            .width(PageDimensions.LETTER_WIDTH)
            .height(PageDimensions.LETTER_HEIGHT),
        colors = CardDefaults.cardColors(containerColor = pageColor),
        shape = RoundedCornerShape(2.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (isDarkTheme) DarkStudyPageBorder else MaterialTheme.colorScheme.outlineVariant,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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
                        Spacer(
                            modifier = Modifier.height(
                                with(baseDensity) { fragment.heightPx.toDp() },
                            ),
                        )
                    } else {
                        key(fragment.compositionKey()) {
                            contentFragmentRenderer(fragment, idx)
                        }
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

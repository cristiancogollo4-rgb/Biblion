package com.cristiancogollo.biblion

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateValueAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import com.cristiancogollo.biblion.core.ui.motion.BiblionMotion
import com.cristiancogollo.biblion.core.ui.motion.rememberBiblionMotionEnabled
import com.cristiancogollo.biblion.ui.theme.BiblionBlueNavigation
import com.cristiancogollo.biblion.ui.theme.BiblionGoldPrimary
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

enum class GuidedTutorialId(val routeArg: String) {
    READING("reading"),
    STUDY("study"),
    EXPLORE("explore");

    companion object {
        fun fromRouteArg(value: String?): GuidedTutorialId? {
            return entries.firstOrNull { it.routeArg == value }
        }
    }
}

enum class GuidedTutorialScreenTarget {
    HOME,
    BOOKS,
    READER,
    SEARCH
}

enum class GuidedTutorialSecondaryAction {
    SKIP,
    RESTART
}

data class GuidedTutorialProgress(
    val guideId: GuidedTutorialId,
    val stepIndex: Int,
    val isRestart: Boolean = false
)

data class GuidedTutorialStep(
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    val targetKey: String?,
    val actionRequired: Boolean,
    val screenTarget: GuidedTutorialScreenTarget,
    @StringRes val primaryLabelRes: Int = R.string.guide_next,
    @StringRes val secondaryLabelRes: Int = R.string.guide_skip_tour,
    private val secondaryAction: GuidedTutorialSecondaryAction = GuidedTutorialSecondaryAction.SKIP,
    val advanceOnEvent: String? = null
) {
    val restartsGuide: Boolean
        get() = secondaryAction == GuidedTutorialSecondaryAction.RESTART

    /** El paso espera algo del usuario (tocar un objetivo o completar un evento) en vez de un boton. */
    val waitsForUser: Boolean
        get() = actionRequired || advanceOnEvent != null
}

object GuidedTutorialTargets {
    const val NAV_BIBLE = "navigation_bible"
    const val NAV_SEARCH = "navigation_search"
    const val NAV_STUDY = "navigation_study"
    const val HOME_DAILY_VERSE = "home_daily_verse"
    const val BOOKS_FIRST_BOOK = "books_first_book"
    const val BOOKS_LONG_PRESS = "books_long_press"
    const val READER_CHAPTER_SELECTOR = "reader_chapter_selector"
    const val READER_TEXT = "reader_text"
    const val READER_FIRST_VERSE = "reader_first_verse"
    const val READER_VERSION_SELECTOR = "reader_version_selector"
    const val READER_BIBI_BUTTON = "reader_bibi_button"
    const val READER_BIBI_CHAT_PANEL = "reader_bibi_chat_panel"
}

private val navigationTargetKeys = setOf(
    GuidedTutorialTargets.NAV_BIBLE,
    GuidedTutorialTargets.NAV_SEARCH,
    GuidedTutorialTargets.NAV_STUDY,
)

/** Si un objetivo existe pero el usuario no actua, se ofrece "Saltar este paso" tras este tiempo. */
internal const val GUIDE_FALLBACK_DELAY_MS = 8_000L

/** Si el objetivo no aparece en pantalla, el respaldo se ofrece casi de inmediato. */
internal const val GUIDE_MISSING_TARGET_DELAY_MS = 1_200L

fun GuidedTutorialProgress.currentStep(): GuidedTutorialStep? {
    return guidedTutorialSteps(guideId).getOrNull(stepIndex)
}

fun guidedTutorialSteps(guideId: GuidedTutorialId): List<GuidedTutorialStep> {
    val readingSteps = listOf(
        GuidedTutorialStep(
            id = "reading-welcome",
            titleRes = R.string.guide_reading_welcome_title,
            descriptionRes = R.string.guide_reading_welcome_body,
            targetKey = null,
            actionRequired = false,
            screenTarget = GuidedTutorialScreenTarget.HOME,
            primaryLabelRes = R.string.guide_start_tour
        ),
        GuidedTutorialStep(
            id = "reading-testament",
            titleRes = R.string.guide_reading_testament_title,
            descriptionRes = R.string.guide_reading_testament_body,
            targetKey = GuidedTutorialTargets.NAV_BIBLE,
            actionRequired = true,
            screenTarget = GuidedTutorialScreenTarget.HOME
        ),
        GuidedTutorialStep(
            id = "reading-book",
            titleRes = R.string.guide_reading_book_title,
            descriptionRes = R.string.guide_reading_book_body,
            targetKey = GuidedTutorialTargets.BOOKS_FIRST_BOOK,
            actionRequired = true,
            screenTarget = GuidedTutorialScreenTarget.BOOKS
        ),
        GuidedTutorialStep(
            id = "reading-chapter",
            titleRes = R.string.guide_reading_chapter_title,
            descriptionRes = R.string.guide_reading_chapter_body,
            targetKey = GuidedTutorialTargets.READER_CHAPTER_SELECTOR,
            actionRequired = true,
            screenTarget = GuidedTutorialScreenTarget.READER
        ),
        GuidedTutorialStep(
            id = "reader-text",
            titleRes = R.string.guide_reader_text_title,
            descriptionRes = R.string.guide_reader_text_body,
            targetKey = GuidedTutorialTargets.READER_TEXT,
            actionRequired = false,
            screenTarget = GuidedTutorialScreenTarget.READER,
            primaryLabelRes = R.string.guide_continue
        ),
        GuidedTutorialStep(
            id = "reader-highlight",
            titleRes = R.string.guide_reader_highlight_title,
            descriptionRes = R.string.guide_reader_highlight_body,
            targetKey = GuidedTutorialTargets.READER_FIRST_VERSE,
            actionRequired = true,
            screenTarget = GuidedTutorialScreenTarget.READER
        ),
        GuidedTutorialStep(
            id = "reader-version",
            titleRes = R.string.guide_reader_version_title,
            descriptionRes = R.string.guide_reader_version_body,
            targetKey = GuidedTutorialTargets.READER_VERSION_SELECTOR,
            actionRequired = false,
            screenTarget = GuidedTutorialScreenTarget.READER,
            primaryLabelRes = R.string.guide_understood
        ),
        GuidedTutorialStep(
            id = "reader-bibi-chat",
            titleRes = R.string.guide_reader_bibi_title,
            descriptionRes = R.string.guide_reader_bibi_body,
            targetKey = GuidedTutorialTargets.READER_BIBI_BUTTON,
            actionRequired = true,
            screenTarget = GuidedTutorialScreenTarget.READER
        ),
        GuidedTutorialStep(
            id = "reader-bibi",
            titleRes = R.string.guide_reader_bibi_chat_title,
            descriptionRes = R.string.guide_reader_bibi_chat_body,
            targetKey = GuidedTutorialTargets.READER_BIBI_CHAT_PANEL,
            actionRequired = false,
            screenTarget = GuidedTutorialScreenTarget.READER,
            advanceOnEvent = "chat_exchange_completed"
        ),
        GuidedTutorialStep(
            id = "reader-deeper-path",
            titleRes = R.string.guide_reader_deeper_title,
            descriptionRes = R.string.guide_reader_deeper_body,
            targetKey = GuidedTutorialTargets.READER_BIBI_CHAT_PANEL,
            actionRequired = false,
            screenTarget = GuidedTutorialScreenTarget.READER,
            primaryLabelRes = R.string.guide_continue
        ),
        GuidedTutorialStep(
            id = "reader-dictionary",
            titleRes = R.string.guide_dictionary_title,
            descriptionRes = R.string.guide_dictionary_body,
            targetKey = GuidedTutorialTargets.NAV_SEARCH,
            actionRequired = true,
            screenTarget = GuidedTutorialScreenTarget.READER
        ),
        GuidedTutorialStep(
            id = "search-components",
            titleRes = R.string.guide_search_components_title,
            descriptionRes = R.string.guide_search_components_body,
            targetKey = null,
            actionRequired = false,
            screenTarget = GuidedTutorialScreenTarget.SEARCH,
            primaryLabelRes = R.string.guide_continue
        ),
        GuidedTutorialStep(
            id = "reading-finish",
            titleRes = R.string.guide_reading_finish_title,
            descriptionRes = R.string.guide_reading_finish_body,
            targetKey = null,
            actionRequired = false,
            screenTarget = GuidedTutorialScreenTarget.SEARCH,
            primaryLabelRes = R.string.guide_start_reading,
            secondaryLabelRes = R.string.guide_restart_tour,
            secondaryAction = GuidedTutorialSecondaryAction.RESTART
        )
    )
    val studySteps = listOf(
        GuidedTutorialStep(
            id = "study-entry",
            titleRes = R.string.guide_study_entry_title,
            descriptionRes = R.string.guide_study_entry_body,
            targetKey = GuidedTutorialTargets.NAV_STUDY,
            actionRequired = true,
            screenTarget = GuidedTutorialScreenTarget.HOME
        )
    )
    val exploreSteps = listOf(
        GuidedTutorialStep(
            id = "explore-reading",
            titleRes = R.string.guide_explore_reading_title,
            descriptionRes = R.string.guide_explore_reading_body,
            targetKey = GuidedTutorialTargets.NAV_BIBLE,
            actionRequired = false,
            screenTarget = GuidedTutorialScreenTarget.HOME
        ),
        GuidedTutorialStep(
            id = "explore-daily-verse",
            titleRes = R.string.guide_explore_daily_title,
            descriptionRes = R.string.guide_explore_daily_body,
            targetKey = GuidedTutorialTargets.HOME_DAILY_VERSE,
            actionRequired = false,
            screenTarget = GuidedTutorialScreenTarget.HOME
        ),
        GuidedTutorialStep(
            id = "explore-study",
            titleRes = R.string.guide_explore_study_title,
            descriptionRes = R.string.guide_explore_study_body,
            targetKey = GuidedTutorialTargets.NAV_STUDY,
            actionRequired = false,
            screenTarget = GuidedTutorialScreenTarget.HOME
        )
    )
    return when (guideId) {
        GuidedTutorialId.READING -> readingSteps
        GuidedTutorialId.STUDY -> studySteps
        GuidedTutorialId.EXPLORE -> exploreSteps
    }
}

/** Posicion 1-based del paso y total de pasos de su recorrido, para el indicador "Paso N de M". */
internal fun guidedTutorialStepPosition(stepId: String): Pair<Int, Int>? {
    GuidedTutorialId.entries.forEach { guideId ->
        val steps = guidedTutorialSteps(guideId)
        val index = steps.indexOfFirst { it.id == stepId }
        if (index >= 0) return (index + 1) to steps.size
    }
    return null
}

/**
 * Cuanto esperar antes de ofrecer "Saltar este paso". `null` si el paso ya tiene boton principal.
 * Garantiza que ningun paso deje al usuario sin salida.
 */
internal fun guidedTutorialFallbackDelayMs(step: GuidedTutorialStep, targetFound: Boolean): Long? {
    if (!step.waitsForUser) return null
    return if (step.targetKey != null && !targetFound) {
        GUIDE_MISSING_TARGET_DELAY_MS
    } else {
        GUIDE_FALLBACK_DELAY_MS
    }
}

/** Pista bajo el texto del paso: en el chat no hay elemento resaltado que tocar. */
internal fun guidedTutorialActionHintRes(step: GuidedTutorialStep): Int =
    if (step.advanceOnEvent == "chat_exchange_completed") {
        R.string.guide_action_chat
    } else {
        R.string.guide_action_required
    }

/** Pasos cuya guia se muestra dentro del chat de Bibi (ventana propia) y no en la burbuja. */
fun guidedStepRendersInsideBibiChat(step: GuidedTutorialStep?): Boolean =
    step?.targetKey == GuidedTutorialTargets.READER_BIBI_CHAT_PANEL

/** Pantalla del tutorial que corresponde a una ruta del NavHost principal. */
internal fun guidedTutorialScreenForRoute(route: String?): GuidedTutorialScreenTarget? = when {
    route == null -> null
    route == Screen.Home.route -> GuidedTutorialScreenTarget.HOME
    route == Screen.Books.route -> GuidedTutorialScreenTarget.BOOKS
    route.startsWith("reader") -> GuidedTutorialScreenTarget.READER
    route == Screen.Search.route -> GuidedTutorialScreenTarget.SEARCH
    else -> null
}

/**
 * La burbuja va arriba cuando el objetivo esta en la mitad inferior o es tan alto que la
 * burbuja inferior lo taparia (por ejemplo, el panel de chat de Bibi con su campo de texto).
 */
internal fun shouldPlaceGuideBubbleAtTop(target: Rect?, screenHeightPx: Float): Boolean {
    if (target == null || screenHeightPx <= 0f) return false
    return target.center.y > screenHeightPx * 0.5f || target.height > screenHeightPx * 0.55f
}

@Stable
fun Modifier.guidedTutorialTarget(
    targetKey: String,
    targetBounds: MutableMap<String, Rect>
): Modifier = composed {
    val holder = remember { arrayOfNulls<Rect>(1) }
    DisposableEffect(targetKey, targetBounds) {
        onDispose {
            // Evita que un objetivo que ya no esta en pantalla siga recortando el velo.
            if (holder[0] != null && targetBounds[targetKey] == holder[0]) {
                targetBounds.remove(targetKey)
            }
        }
    }
    onGloballyPositioned { coordinates ->
        val bounds = coordinates.boundsInRoot()
        holder[0] = bounds
        targetBounds[targetKey] = bounds
    }
}

internal fun guidedNavigationTargetRect(
    targetKey: String?,
    screenWidthPx: Float,
    screenHeightPx: Float,
    density: Float,
): Rect? {
    val itemIndex = when (targetKey) {
        GuidedTutorialTargets.NAV_BIBLE -> 1
        GuidedTutorialTargets.NAV_SEARCH -> 2
        GuidedTutorialTargets.NAV_STUDY -> 3
        else -> return null
    }
    val horizontalMarginPx = 16f * density
    val navigationWidthPx = (screenWidthPx - horizontalMarginPx * 2f)
        .coerceAtMost(680f * density)
    val itemWidthPx = navigationWidthPx / 5f
    val navigationLeftPx = (screenWidthPx - navigationWidthPx) / 2f
    return Rect(
        left = navigationLeftPx + itemWidthPx * itemIndex,
        top = screenHeightPx - 96f * density,
        right = navigationLeftPx + itemWidthPx * (itemIndex + 1),
        bottom = screenHeightPx - 12f * density,
    )
}

private val GuideEaseOut = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
private val GuideScrimColor = BiblionBlueNavigation.copy(alpha = 0.66f)

@Composable
fun GuidedTutorialOverlay(
    step: GuidedTutorialStep?,
    targetBounds: Map<String, Rect>,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    onRestart: () -> Unit,
    isRestart: Boolean = false,
    hostedInBibiChat: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (step == null) return
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val motionEnabled = rememberBiblionMotionEnabled()
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val isNavigationTarget = step.targetKey in navigationTargetKeys
    val measuredTarget = step.targetKey?.let { targetBounds[it] }
    val target = measuredTarget
        ?: guidedNavigationTargetRect(
            targetKey = step.targetKey,
            screenWidthPx = screenWidthPx,
            screenHeightPx = screenHeightPx,
            density = density.density,
        )

    var showStepFallback by remember(step.id) { mutableStateOf(false) }
    LaunchedEffect(step.id, target != null) {
        showStepFallback = false
        val delayMs = guidedTutorialFallbackDelayMs(step, targetFound = target != null)
            ?: return@LaunchedEffect
        delay(delayMs)
        showStepFallback = true
    }

    val holePaddingPx = with(density) {
        if (step.targetKey == GuidedTutorialTargets.READER_FIRST_VERSE) 16.dp.toPx() else 8.dp.toPx()
    }
    val hole = target
        ?.takeUnless { isNavigationTarget || hostedInBibiChat }
        ?.let {
            Rect(
                left = (it.left - holePaddingPx).coerceAtLeast(0f),
                top = (it.top - holePaddingPx).coerceAtLeast(0f),
                right = (it.right + holePaddingPx).coerceAtMost(screenWidthPx),
                bottom = (it.bottom + holePaddingPx).coerceAtMost(screenHeightPx),
            )
        }

    Box(modifier = modifier.fillMaxSize()) {
        GuideSpotlight(hole = hole, motionEnabled = motionEnabled)
        GuideTouchShield(hole = hole, screenWidthPx = screenWidthPx, screenHeightPx = screenHeightPx)

        if (hostedInBibiChat) return@Box
        val placeAtTop = shouldPlaceGuideBubbleAtTop(target, screenHeightPx)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 12.dp,
                    // Deja libre la capsula de navegacion flotante.
                    bottom = 104.dp,
                ),
            contentAlignment = if (placeAtTop) Alignment.TopCenter else Alignment.BottomCenter,
        ) {
            GuideBubble(
                step = step,
                onNext = onNext,
                onSecondary = if (step.restartsGuide) onRestart else onSkip,
                showStepFallback = showStepFallback,
                motionEnabled = motionEnabled,
                maxHeight = with(density) { (screenHeightPx * 0.46f).toDp() },
            )
        }
    }
}

@Composable
private fun GuideSpotlight(hole: Rect?, motionEnabled: Boolean) {
    val animatedHole by animateValueAsState(
        targetValue = hole ?: Rect.Zero,
        typeConverter = Rect.VectorConverter,
        animationSpec = if (motionEnabled) {
            tween(BiblionMotion.EMPHASIS_MS, easing = GuideEaseOut)
        } else {
            snap()
        },
        label = "guide-spotlight-hole",
    )
    val pulse = rememberInfiniteTransition(label = "guide-pulse")
    val ringAlpha by pulse.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_100, easing = GuideEaseOut),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "guide-ring-alpha",
    )
    val hasHole = hole != null && animatedHole.width > 0f && animatedHole.height > 0f

    Canvas(modifier = Modifier.fillMaxSize()) {
        if (!hasHole) {
            drawRect(GuideScrimColor)
            return@Canvas
        }
        val corner = CornerRadius(16.dp.toPx())
        val roundHole = RoundRect(animatedHole, corner)
        val veil = Path().apply {
            fillType = PathFillType.EvenOdd
            addRect(Rect(Offset.Zero, size))
            addRoundRect(roundHole)
        }
        drawPath(veil, GuideScrimColor)
        drawRoundRect(
            color = BiblionGoldPrimary.copy(alpha = 0.14f),
            topLeft = animatedHole.topLeft,
            size = Size(animatedHole.width, animatedHole.height),
            cornerRadius = corner,
        )
        drawRoundRect(
            color = BiblionGoldPrimary.copy(alpha = if (motionEnabled) ringAlpha else 1f),
            topLeft = animatedHole.topLeft,
            size = Size(animatedHole.width, animatedHole.height),
            cornerRadius = corner,
            style = Stroke(width = 2.5.dp.toPx()),
        )
    }
}

/**
 * Bloquea los toques fuera del objetivo para que el usuario no se pierda a mitad del recorrido.
 * El recorte queda libre: solo el elemento resaltado (y la capsula de navegacion, que esta por
 * encima de cada pantalla) recibe toques.
 */
@Composable
private fun GuideTouchShield(hole: Rect?, screenWidthPx: Float, screenHeightPx: Float) {
    if (hole == null) {
        GuideShieldSegment(0f, 0f, screenWidthPx, screenHeightPx)
        return
    }
    GuideShieldSegment(0f, 0f, screenWidthPx, hole.top)
    GuideShieldSegment(0f, hole.bottom, screenWidthPx, screenHeightPx - hole.bottom)
    GuideShieldSegment(0f, hole.top, hole.left, hole.height)
    GuideShieldSegment(hole.right, hole.top, screenWidthPx - hole.right, hole.height)
}

@Composable
private fun GuideShieldSegment(x: Float, y: Float, width: Float, height: Float) {
    if (width <= 0f || height <= 0f) return
    val density = LocalDensity.current
    Box(
        modifier = Modifier
            .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
            .size(
                width = with(density) { width.toDp() },
                height = with(density) { height.toDp() }
            )
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent().changes.forEach { it.consume() }
                    }
                }
            }
    )
}

@Composable
private fun GuideBubble(
    step: GuidedTutorialStep,
    onNext: () -> Unit,
    onSecondary: () -> Unit,
    showStepFallback: Boolean,
    motionEnabled: Boolean,
    maxHeight: Dp,
) {
    val isCompact = LocalConfiguration.current.screenWidthDp < 360
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 6.dp,
        shadowElevation = 10.dp,
        modifier = Modifier
            .widthIn(max = 400.dp)
            .fillMaxWidth()
            .heightIn(max = maxHeight.coerceAtLeast(220.dp))
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        GuideCardContent(
            step = step,
            onNext = onNext,
            onSecondary = onSecondary,
            showStepFallback = showStepFallback,
            motionEnabled = motionEnabled,
            compact = isCompact,
            showAvatar = true,
            modifier = Modifier.padding(if (isCompact) 14.dp else 18.dp),
        )
    }
}

/**
 * Tarjeta del recorrido que vive dentro del chat de Bibi. El chat se abre en una ventana propia
 * (Popup) por encima de la app, asi que la burbuja normal quedaria tapada y sin toques.
 */
@Composable
fun GuidedTutorialInlineCard(
    step: GuidedTutorialStep,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    onRestart: () -> Unit,
    collapsed: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val motionEnabled = rememberBiblionMotionEnabled()
    var showStepFallback by remember(step.id) { mutableStateOf(false) }
    LaunchedEffect(step.id) {
        showStepFallback = false
        val delayMs = guidedTutorialFallbackDelayMs(step, targetFound = true) ?: return@LaunchedEffect
        delay(delayMs)
        showStepFallback = true
    }
    val onSecondary = if (step.restartsGuide) onRestart else onSkip
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, BiblionGoldPrimary.copy(alpha = 0.55f)),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 300.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        if (collapsed) {
            // Con el teclado abierto el chat se encoge: una franja deja visibles mensajes y campo.
            GuideCollapsedStrip(
                step = step,
                onNext = onNext,
                onSecondary = onSecondary,
                showStepFallback = showStepFallback,
            )
        } else {
            GuideCardContent(
                step = step,
                onNext = onNext,
                onSecondary = onSecondary,
                showStepFallback = showStepFallback,
                motionEnabled = motionEnabled,
                compact = true,
                showAvatar = false,
                modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 6.dp),
            )
        }
    }
}

@Composable
private fun GuideCollapsedStrip(
    step: GuidedTutorialStep,
    onNext: () -> Unit,
    onSecondary: () -> Unit,
    showStepFallback: Boolean,
) {
    val position = remember(step.id) { guidedTutorialStepPosition(step.id) }
    Column(modifier = Modifier.padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                if (position != null) {
                    Text(
                        text = stringResource(R.string.guide_step_progress, position.first, position.second),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    text = stringResource(step.titleRes),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (!step.waitsForUser) {
                TextButton(onClick = onNext) {
                    Text(stringResource(step.primaryLabelRes), style = MaterialTheme.typography.labelLarge)
                }
            } else if (showStepFallback) {
                TextButton(onClick = onNext) {
                    Text(stringResource(R.string.guide_skip_step), style = MaterialTheme.typography.labelLarge)
                }
            }
            TextButton(onClick = onSecondary) {
                Text(
                    text = stringResource(step.secondaryLabelRes),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (position != null) {
            Box(modifier = Modifier.padding(end = 10.dp, bottom = 6.dp)) {
                GuideStepTrack(current = position.first, total = position.second)
            }
        }
    }
}

@Composable
private fun GuideCardContent(
    step: GuidedTutorialStep,
    onNext: () -> Unit,
    onSecondary: () -> Unit,
    showStepFallback: Boolean,
    motionEnabled: Boolean,
    compact: Boolean,
    showAvatar: Boolean,
    modifier: Modifier = Modifier,
) {
    val position = remember(step.id) { guidedTutorialStepPosition(step.id) }
    val showPrimary = !step.waitsForUser

    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showAvatar) {
                BibiAvatar(size = if (compact) 36.dp else 40.dp)
                Spacer(Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                if (position != null) {
                    Text(
                        text = stringResource(
                            R.string.guide_step_progress,
                            position.first,
                            position.second
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(6.dp))
                    GuideStepTrack(current = position.first, total = position.second)
                }
            }
        }

        Spacer(Modifier.height(if (compact) 10.dp else 14.dp))

        AnimatedContent(
            targetState = step,
            contentKey = { it.id },
            transitionSpec = {
                if (motionEnabled) {
                    (fadeIn(tween(BiblionMotion.STANDARD_MS, easing = GuideEaseOut)) +
                        slideInVertically(tween(BiblionMotion.STANDARD_MS, easing = GuideEaseOut)) { it / 8 })
                        .togetherWith(
                            fadeOut(tween(BiblionMotion.QUICK_MS)) +
                                slideOutVertically(tween(BiblionMotion.QUICK_MS)) { -it / 12 }
                        )
                } else {
                    fadeIn(snap()).togetherWith(fadeOut(snap()))
                }
            },
            modifier = Modifier.weight(1f, fill = false),
            label = "guide-step-content",
        ) { shownStep ->
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = stringResource(shownStep.titleRes),
                    style = (if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge)
                        .copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(shownStep.descriptionRes),
                    style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        AnimatedVisibility(visible = step.waitsForUser) {
            GuideActionHint(textRes = guidedTutorialActionHintRes(step), motionEnabled = motionEnabled)
        }

        Spacer(Modifier.height(if (compact) 8.dp else 14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onSecondary) {
                Text(
                    text = stringResource(step.secondaryLabelRes),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            when {
                showPrimary -> Button(
                    onClick = onNext,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Text(
                        text = stringResource(step.primaryLabelRes),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                else -> AnimatedVisibility(
                    visible = showStepFallback,
                    enter = fadeIn(tween(BiblionMotion.STANDARD_MS)),
                    exit = fadeOut(tween(BiblionMotion.QUICK_MS)),
                ) {
                    FilledTonalButton(
                        onClick = onNext,
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.guide_skip_step),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BibiAvatar(size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(BiblionBlueNavigation),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(id = R.drawable.bibi_logo),
            contentDescription = stringResource(R.string.guide_bibi_avatar_cd),
            modifier = Modifier
                .fillMaxSize()
                .padding(5.dp),
            contentScale = ContentScale.Fit,
        )
    }
}

/** Camino de pasos: segmentos dorados para lo recorrido, neutros para lo que falta. */
@Composable
private fun GuideStepTrack(current: Int, total: Int) {
    val remaining = MaterialTheme.colorScheme.outlineVariant
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        repeat(total) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (index < current) BiblionGoldPrimary else remaining)
            )
        }
    }
}

@Composable
private fun GuideActionHint(@StringRes textRes: Int, motionEnabled: Boolean) {
    val pulse = rememberInfiniteTransition(label = "guide-hint")
    val dotAlpha by pulse.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = GuideEaseOut),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "guide-hint-dot",
    )
    Row(
        modifier = Modifier.padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(BiblionGoldPrimary.copy(alpha = if (motionEnabled) dotAlpha else 1f))
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(textRes),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

/**
 * Aviso flotante cuando el usuario sale de la pantalla del paso actual. Evita que el recorrido
 * quede activo pero invisible.
 */
@Composable
fun GuidedTutorialPausedBanner(
    visible: Boolean,
    onResume: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val motionEnabled = rememberBiblionMotionEnabled()
    AnimatedVisibility(
        visible = visible,
        enter = if (motionEnabled) {
            fadeIn(tween(BiblionMotion.STANDARD_MS, easing = GuideEaseOut)) +
                slideInVertically(tween(BiblionMotion.STANDARD_MS, easing = GuideEaseOut)) { -it / 2 }
        } else {
            fadeIn(snap())
        },
        exit = fadeOut(tween(BiblionMotion.QUICK_MS)),
        modifier = modifier
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
            modifier = Modifier
                .widthIn(max = 420.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
        ) {
            Row(
                modifier = Modifier.padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BibiAvatar(size = 32.dp)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.guide_paused_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(4.dp))
                TextButton(onClick = onSkip) {
                    Text(
                        text = stringResource(R.string.guide_skip_tour),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Button(
                    onClick = onResume,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.guide_resume),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

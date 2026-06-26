package com.cristiancogollo.biblion

import androidx.annotation.StringRes
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.cristiancogollo.biblion.ui.theme.BiblionBluePrimary
import com.cristiancogollo.biblion.ui.theme.BiblionGoldPrimary
import com.cristiancogollo.biblion.ui.theme.BiblionGoldSoft
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
    READER
}

enum class GuidedTutorialSecondaryAction {
    SKIP,
    RESTART
}

data class GuidedTutorialProgress(
    val guideId: GuidedTutorialId,
    val stepIndex: Int
)

data class GuidedTutorialStep(
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    val targetKey: String?,
    val actionRequired: Boolean,
    val screenTarget: GuidedTutorialScreenTarget,
    @StringRes val primaryLabelRes: Int = R.string.guide_next,
    @StringRes val secondaryLabelRes: Int = R.string.guide_skip,
    private val secondaryAction: GuidedTutorialSecondaryAction = GuidedTutorialSecondaryAction.SKIP
) {
    val restartsGuide: Boolean
        get() = secondaryAction == GuidedTutorialSecondaryAction.RESTART
}

object GuidedTutorialTargets {
    const val HOME_TESTAMENT_SELECTOR = "home_testament_selector"
    const val HOME_DAILY_VERSE = "home_daily_verse"
    const val HOME_STUDY_ENTRY = "home_study_entry"
    const val BOOKS_FIRST_BOOK = "books_first_book"
    const val READER_CHAPTER_SELECTOR = "reader_chapter_selector"
    const val READER_TEXT = "reader_text"
    const val READER_FIRST_VERSE = "reader_first_verse"
    const val READER_VERSION_SELECTOR = "reader_version_selector"
    const val READER_BIBI_BUTTON = "reader_bibi_button"
}

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
            targetKey = GuidedTutorialTargets.HOME_TESTAMENT_SELECTOR,
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
            titleRes = R.string.guide_reader_bibi_chat_title,
            descriptionRes = R.string.guide_reader_bibi_chat_body,
            targetKey = null,
            actionRequired = false,
            screenTarget = GuidedTutorialScreenTarget.READER,
            primaryLabelRes = R.string.guide_continue
        ),
        GuidedTutorialStep(
            id = "reader-bibi",
            titleRes = R.string.guide_reader_bibi_title,
            descriptionRes = R.string.guide_reader_bibi_body,
            targetKey = GuidedTutorialTargets.READER_BIBI_BUTTON,
            actionRequired = true,
            screenTarget = GuidedTutorialScreenTarget.READER
        ),
        GuidedTutorialStep(
            id = "reader-dictionary",
            titleRes = R.string.guide_dictionary_title,
            descriptionRes = R.string.guide_dictionary_body,
            targetKey = null,
            actionRequired = false,
            screenTarget = GuidedTutorialScreenTarget.READER,
            primaryLabelRes = R.string.guide_continue
        ),
        GuidedTutorialStep(
            id = "reader-deeper-path",
            titleRes = R.string.guide_reader_deeper_title,
            descriptionRes = R.string.guide_reader_deeper_body,
            targetKey = null,
            actionRequired = false,
            screenTarget = GuidedTutorialScreenTarget.READER,
            primaryLabelRes = R.string.guide_continue
        ),
        GuidedTutorialStep(
            id = "reading-finish",
            titleRes = R.string.guide_reading_finish_title,
            descriptionRes = R.string.guide_reading_finish_body,
            targetKey = null,
            actionRequired = false,
            screenTarget = GuidedTutorialScreenTarget.READER,
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
            targetKey = GuidedTutorialTargets.HOME_STUDY_ENTRY,
            actionRequired = true,
            screenTarget = GuidedTutorialScreenTarget.HOME
        )
    )
    val exploreSteps = listOf(
        GuidedTutorialStep(
            id = "explore-reading",
            titleRes = R.string.guide_explore_reading_title,
            descriptionRes = R.string.guide_explore_reading_body,
            targetKey = GuidedTutorialTargets.HOME_TESTAMENT_SELECTOR,
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
            targetKey = GuidedTutorialTargets.HOME_STUDY_ENTRY,
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

@Stable
fun Modifier.guidedTutorialTarget(
    targetKey: String,
    targetBounds: MutableMap<String, Rect>
): Modifier {
    return onGloballyPositioned { coordinates ->
        targetBounds[targetKey] = coordinates.boundsInRoot()
    }
}

@Composable
fun GuidedTutorialOverlay(
    step: GuidedTutorialStep?,
    targetBounds: Map<String, Rect>,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (step == null) return
    val target = step.targetKey?.let { targetBounds[it] }
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val pulseTransition = rememberInfiniteTransition(label = "guide-pulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 950),
            repeatMode = RepeatMode.Reverse
        ),
        label = "guide-pulse-alpha"
    )

    Box(modifier = modifier.fillMaxSize()) {
        if (target == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.48f))
            )
        } else {
            val basePaddingPx = with(density) { 10.dp.toPx() }
            val isVerseHighlightStep = step.targetKey == GuidedTutorialTargets.READER_FIRST_VERSE
            val paddingPx = if (isVerseHighlightStep) {
                with(density) { 24.dp.toPx() }
            } else {
                basePaddingPx
            }

            val clampedLeft = (target.left - paddingPx).coerceAtLeast(0f)
            val clampedTop = (target.top - paddingPx).coerceAtLeast(0f)
            val clampedRight = (target.right + paddingPx).coerceAtMost(screenWidthPx)
            val clampedBottom = (target.bottom + paddingPx).coerceAtMost(screenHeightPx)

            val highlightWidth = (clampedRight - clampedLeft).coerceAtLeast(with(density) { 80.dp.toPx() })
            val highlightHeight = (clampedBottom - clampedTop).coerceAtLeast(with(density) { 80.dp.toPx() })

            val highlightLeft = clampedLeft
            val highlightTop = clampedTop
            val highlightRight = (highlightLeft + highlightWidth).coerceAtMost(screenWidthPx)
            val highlightBottom = (highlightTop + highlightHeight).coerceAtMost(screenHeightPx)

            GuidedScrimAroundTarget(
                target = target,
                screenWidthPx = screenWidthPx,
                screenHeightPx = screenHeightPx
            )
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = highlightLeft.roundToInt(),
                            y = highlightTop.roundToInt()
                        )
                    }
                    .size(
                        width = with(density) { (highlightRight - highlightLeft).toDp() },
                        height = with(density) { (highlightBottom - highlightTop).toDp() }
                    )
                    .border(
                        width = 3.dp,
                        color = BiblionGoldPrimary.copy(alpha = pulseAlpha),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .background(BiblionGoldPrimary.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
            )
        }

        GuideBubble(
            step = step,
            target = target,
            screenHeightPx = screenHeightPx,
            onNext = onNext,
            onSecondary = if (step.restartsGuide) onRestart else onSkip,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(18.dp)
        )
    }
}

@Composable
private fun GuidedScrimAroundTarget(
    target: Rect,
    screenWidthPx: Float,
    screenHeightPx: Float
) {
    val density = LocalDensity.current
    val paddingPx = with(density) { 10.dp.toPx() }
    val left = (target.left - paddingPx).coerceAtLeast(0f)
    val top = (target.top - paddingPx).coerceAtLeast(0f)
    val right = (target.right + paddingPx).coerceAtMost(screenWidthPx)
    val bottom = (target.bottom + paddingPx).coerceAtMost(screenHeightPx)

    GuidedScrimSegment(0f, 0f, screenWidthPx, top)
    GuidedScrimSegment(0f, bottom, screenWidthPx, screenHeightPx - bottom)
    GuidedScrimSegment(0f, top, left, bottom - top)
    GuidedScrimSegment(right, top, screenWidthPx - right, bottom - top)
}

@Composable
private fun GuidedScrimSegment(
    x: Float,
    y: Float,
    width: Float,
    height: Float
) {
    if (width <= 0f || height <= 0f) return
    val density = LocalDensity.current
    Box(
        modifier = Modifier
            .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
            .size(
                width = with(density) { width.toDp() },
                height = with(density) { height.toDp() }
            )
            .background(Color.Black.copy(alpha = 0.48f))
    )
}

@Composable
private fun GuideBubble(
    step: GuidedTutorialStep,
    target: Rect?,
    screenHeightPx: Float,
    onNext: () -> Unit,
    onSecondary: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenWidthDp = with(density) { configuration.screenWidthDp.dp }
    val placeAboveTarget = target != null && target.center.y > screenHeightPx * 0.58f
    val verticalOffset = if (placeAboveTarget) {
        with(density) { (-190).dp }
    } else {
        0.dp
    }
    
    val isSmallScreen = screenWidthDp < 360.dp
    val bubblePadding = if (isSmallScreen) 12.dp else 16.dp
    val logoSize = if (isSmallScreen) 40.dp else 48.dp
    val titleStyle = if (isSmallScreen) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium
    val bodyStyle = if (isSmallScreen) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge
    val labelStyle = if (isSmallScreen) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge
    val buttonHeight = if (isSmallScreen) 36.dp else 40.dp
    val maxBubbleWidth = if (screenWidthDp < 340.dp) screenWidthDp else 340.dp
    
    Box(
        modifier = modifier
            .padding(16.dp)
            .offset(y = verticalOffset)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF10263B),
            border = BorderStroke(1.dp, BiblionGoldSoft.copy(alpha = 0.6f)),
            tonalElevation = 8.dp,
            modifier = Modifier
                .widthIn(min = 280.dp, max = maxBubbleWidth)
                .heightIn(max = 500.dp)
                .align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .padding(bubblePadding)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(if (isSmallScreen) 10.dp else 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(if (isSmallScreen) 8.dp else 10.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(logoSize),
                        shape = RoundedCornerShape(8.dp),
                        color = BiblionBluePrimary,
                        border = BorderStroke(1.dp, BiblionGoldSoft.copy(alpha = 0.6f))
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.bibi_logo),
                            contentDescription = stringResource(R.string.auth_logo_cd),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                    Text(
                        text = stringResource(step.titleRes),
                        style = titleStyle.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = stringResource(step.descriptionRes),
                    style = bodyStyle,
                    color = Color.White.copy(alpha = 0.9f),
                    overflow = TextOverflow.Ellipsis
                )
                if (step.actionRequired) {
                    Text(
                        text = stringResource(R.string.guide_action_required),
                        style = labelStyle.copy(fontWeight = FontWeight.Bold),
                        color = BiblionGoldSoft
                    )
                }
                Spacer(modifier = Modifier.height(if (isSmallScreen) 4.dp else 6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onSecondary) {
                        Text(
                            text = stringResource(step.secondaryLabelRes),
                            color = Color.White.copy(alpha = 0.85f),
                            style = labelStyle
                        )
                    }
                    if (!step.actionRequired) {
                        Button(
                            onClick = onNext,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BiblionGoldPrimary,
                                contentColor = BiblionBluePrimary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(buttonHeight)
                        ) {
                            Text(
                                text = stringResource(step.primaryLabelRes),
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                style = labelStyle
                            )
                        }
                    }
                }
            }
        }
    }
}

package com.cristiancogollo.biblion.ui.brand

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.cristiancogollo.biblion.R
import com.cristiancogollo.biblion.core.ui.motion.rememberBiblionMotionEnabled
import com.cristiancogollo.biblion.ui.theme.BiblionThemeMode
import kotlin.math.min
import kotlinx.coroutines.launch

/** Total length of the reveal before the closing fade, in milliseconds. */
private const val INTRO_MS = 3_050
private const val EXIT_MS = 380

private val EaseInOut = CubicBezierEasing(0.45f, 0f, 0.25f, 1f)

/**
 * Cold-start brand intro drawn over the app while it finishes loading.
 *
 * The logo is never rotated or deformed: light reveals it in place. Tapping skips straight
 * to the closing fade, and with system animations off the intro is skipped entirely.
 */
@Composable
fun BiblionIntro(
    themeMode: BiblionThemeMode,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val finish by rememberUpdatedState(onFinished)
    if (!rememberBiblionMotionEnabled()) {
        LaunchedEffect(Unit) { finish() }
        return
    }

    val style = remember(themeMode) { IntroStyle.forMode(themeMode) }
    val logo = remember { IntroLogo() }
    val clock = remember { Animatable(0f) }
    val exit = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        awaitSteadyFrames()
        clock.animateTo(INTRO_MS.toFloat(), tween(INTRO_MS, easing = LinearEasing))
        exit.animateTo(0f, tween(EXIT_MS, easing = EaseInOut))
        finish()
    }

    val description = stringResource(R.string.app_name)
    val maxLogo = with(LocalDensity.current) { 380.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { alpha = exit.value }
            .semantics { contentDescription = description }
            .pointerInput(Unit) {
                detectTapGestures {
                    scope.launch {
                        clock.snapTo(INTRO_MS.toFloat())
                        exit.animateTo(0f, tween(EXIT_MS, easing = EaseInOut))
                        finish()
                    }
                }
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val t = clock.value
            val box = min(min(size.width, size.height) * 0.74f, maxLogo)
            val reach = maxOf(size.width, size.height) * 0.75f
            drawRect(Brush.radialGradient(style.background, center = center, radius = reach))
            drawGlow(style, t)
            translate(
                left = (size.width - box) / 2f,
                top = (size.height - box) / 2f,
            ) {
                scale(box / BiblionLogoPaths.VIEWPORT, pivot = Offset.Zero) {
                    translate(BiblionLogoPaths.CONTENT_OFFSET_X, BiblionLogoPaths.CONTENT_OFFSET_Y) {
                        drawLogo(logo, style, t)
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawGlow(style: IntroStyle, t: Float) {
    val rise = progress(t, 0f, 1_000f, EaseInOut)
    val flare = bell(progress(t, 2_050f, 650f, LinearEasing)) * 0.35f
    val alpha = style.glowAlpha * (rise + flare)
    if (alpha <= 0f) return
    val radius = min(size.width, size.height) * 0.62f
    drawCircle(
        brush = Brush.radialGradient(
            0f to style.glow.copy(alpha = alpha),
            0.4f to style.glow.copy(alpha = alpha * 0.55f),
            0.7f to style.glow.copy(alpha = alpha * 0.18f),
            1f to Color.Transparent,
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}

private fun DrawScope.drawLogo(logo: IntroLogo, style: IntroStyle, t: Float) {
    drawFeather(logo, logo.featherMain, style, t, traceStart = 250f, fillStart = 550f, fillTop = 80f, fillBottom = 315f)
    drawFeather(logo, logo.featherInner, style, t, traceStart = 380f, fillStart = 680f, fillTop = 168f, fillBottom = 242f)
    drawLetter(logo, style, t)
}

private fun DrawScope.drawFeather(
    logo: IntroLogo,
    feather: TracedShape,
    style: IntroStyle,
    t: Float,
    traceStart: Float,
    fillStart: Float,
    fillTop: Float,
    fillBottom: Float,
) {
    val trace = progress(t, traceStart, 800f, EaseInOut)
    val fill = progress(t, fillStart, 850f, EaseInOut)
    if (trace <= 0f) return

    if (fill > 0f) {
        withLayer {
            drawPath(feather.path, logo.goldBrush)
            clipPath(feather.path) {
                drawPath(logo.goldShade, Color(0xFFB97C2C), alpha = 0.55f)
                drawPath(logo.goldLight, Color(0xFFF3D16A), alpha = 0.55f)
                drawPath(logo.goldHighlight, Color(0xFFFBE79A), alpha = 0.5f)
            }
            // Rising gold reflection at the reveal front, then the shared closing sweep.
            val from = fillBottom + 20f
            val front = from + (fillTop - 40f - from) * fill
            val sheen = (1f - fill) * style.reflection
            if (sheen > 0f) {
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.5f to Color(0xFFFFF1C2).copy(alpha = 0.75f * sheen),
                        1f to Color.Transparent,
                        startY = front - 26f,
                        endY = front + 26f,
                    ),
                    topLeft = Offset(LOGO_LEFT, LOGO_TOP),
                    size = LOGO_SIZE,
                    blendMode = BlendMode.SrcAtop,
                )
            }
            drawShimmer(style, t, Color(0xFFFFF4D6))
            drawRect(
                brush = Brush.verticalGradient(
                    0f to Color.Transparent,
                    1f to Color.Black,
                    startY = front - 18f,
                    endY = front + 18f,
                ),
                topLeft = Offset(LOGO_LEFT, LOGO_TOP),
                size = LOGO_SIZE,
                blendMode = BlendMode.DstIn,
            )
        }
    }

    // Thin line of light running along the silhouette; it dissolves once the fill lands.
    val lineAlpha = if (fill < 0.6f) 1f else 1f - progress(fill, 0.6f, 0.4f, LinearEasing)
    if (lineAlpha > 0f) {
        val partial = feather.partial(trace)
        drawPath(
            partial,
            style.traceGlow.copy(alpha = 0.28f * lineAlpha),
            style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        drawPath(
            partial,
            style.trace.copy(alpha = 0.95f * lineAlpha),
            style = Stroke(width = 1.6f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

private fun DrawScope.drawLetter(logo: IntroLogo, style: IntroStyle, t: Float) {
    val reveal = progress(t, 800f, 1_000f, EaseInOut)
    if (reveal <= 0f) return
    withLayer {
        drawPath(logo.letter.path, style.letterBrush)
        // Diagonal axis from the foot of the B to its upper bowl.
        val position = -0.12f + 1.24f * reveal
        val light = (1f - reveal).coerceAtLeast(0f)
        if (light > 0f) {
            drawRect(
                brush = bandAlong(position, 0.09f, style.letterLight.copy(alpha = 0.7f * light * style.reflection)),
                topLeft = Offset(LOGO_LEFT, LOGO_TOP),
                size = LOGO_SIZE,
                blendMode = BlendMode.SrcAtop,
            )
        }
        drawShimmer(style, t, style.shimmerBlue)
        drawRect(
            brush = Brush.linearGradient(
                0f to Color.Black,
                1f to Color.Transparent,
                start = lerp(SWEEP_START, SWEEP_END, position - 0.06f),
                end = lerp(SWEEP_START, SWEEP_END, position + 0.06f),
            ),
            topLeft = Offset(LOGO_LEFT, LOGO_TOP),
            size = LOGO_SIZE,
            blendMode = BlendMode.DstIn,
        )
    }
}

/** Short diagonal flash shared by every part of the logo once it is fully defined. */
private fun DrawScope.drawShimmer(style: IntroStyle, t: Float, tint: Color) {
    val p = progress(t, 2_050f, 650f, EaseInOut)
    if (p <= 0f || p >= 1f) return
    drawRect(
        brush = bandAlong(-0.15f + 1.3f * p, 0.07f, tint.copy(alpha = 0.55f * style.reflection)),
        topLeft = Offset(LOGO_LEFT, LOGO_TOP),
        size = LOGO_SIZE,
        blendMode = BlendMode.SrcAtop,
    )
}

private fun bandAlong(position: Float, halfWidth: Float, color: Color): Brush =
    Brush.linearGradient(
        0f to Color.Transparent,
        0.5f to color,
        1f to Color.Transparent,
        start = lerp(SWEEP_START, SWEEP_END, position - halfWidth),
        end = lerp(SWEEP_START, SWEEP_END, position + halfWidth),
    )

private inline fun DrawScope.withLayer(block: DrawScope.() -> Unit) {
    val canvas = drawContext.canvas
    canvas.saveLayer(Rect(LOGO_LEFT, LOGO_TOP, LOGO_LEFT + LOGO_SIZE.width, LOGO_TOP + LOGO_SIZE.height), Paint())
    block()
    canvas.restore()
}

private fun progress(t: Float, start: Float, duration: Float, easing: androidx.compose.animation.core.Easing): Float =
    easing.transform(((t - start) / duration).coerceIn(0f, 1f))

/**
 * Cold start keeps the main thread busy for a while; starting the clock then would skip
 * most of the reveal. Wait (at most ~1 s) until a few consecutive frames arrive on time.
 */
private suspend fun awaitSteadyFrames() {
    val first = withFrameNanos { it }
    var last = first
    var steady = 0
    while (steady < 4 && last - first < 1_000_000_000L) {
        val now = withFrameNanos { it }
        steady = if (now - last < 25_000_000L) steady + 1 else 0
        last = now
    }
}

private fun bell(x: Float): Float = if (x <= 0f || x >= 1f) 0f else 4f * x * (1f - x)

private const val LOGO_LEFT = 140f
private const val LOGO_TOP = 60f
private val LOGO_SIZE = androidx.compose.ui.geometry.Size(260f, 310f)
private val SWEEP_START = Offset(170f, 360f)
private val SWEEP_END = Offset(380f, 110f)

private class TracedShape(pathData: String) {
    val path: Path = PathParser().parsePathString(pathData).toPath()
    val bounds: Rect = path.getBounds()

    /** Leading [fraction] of every contour, for the line of light. */
    fun partial(fraction: Float): Path {
        val out = android.graphics.Path()
        val measure = android.graphics.PathMeasure(path.asAndroidPath(), false)
        do {
            measure.getSegment(0f, measure.length * fraction, out, true)
        } while (measure.nextContour())
        return out.asComposePath()
    }
}

private class IntroLogo {
    val letter = TracedShape(BiblionLogoPaths.LETTER)
    val featherMain = TracedShape(BiblionLogoPaths.FEATHER_MAIN)
    val featherInner = TracedShape(BiblionLogoPaths.FEATHER_INNER)
    val goldShade = PathParser().parsePathString(BiblionLogoPaths.GOLD_SHADE).toPath()
    val goldLight = PathParser().parsePathString(BiblionLogoPaths.GOLD_LIGHT).toPath()
    val goldHighlight = PathParser().parsePathString(BiblionLogoPaths.GOLD_HIGHLIGHT).toPath()
    val goldBrush = Brush.linearGradient(
        0f to Color(0xFFBF8130),
        0.45f to Color(0xFFD8A64A),
        1f to Color(0xFFF0CF66),
        start = Offset(215f, 300f),
        end = Offset(250f, 95f),
    )
}

private class IntroStyle(
    val background: List<Color>,
    val glow: Color,
    val glowAlpha: Float,
    val letterBrush: Brush,
    val letterLight: Color,
    val shimmerBlue: Color,
    val trace: Color,
    val traceGlow: Color,
    /** Strength of metallic reflections; slightly higher on dark backgrounds. */
    val reflection: Float,
) {
    companion object {
        private fun letter(from: Color, to: Color) =
            Brush.linearGradient(listOf(from, to), start = Offset(215f, 306f), end = Offset(368f, 216f))

        private val LightLetter = letter(Color(0xFF061E3D), Color(0xFF13315F))
        private val CreamLetter = letter(Color(0xFFE6E2D8), Color(0xFFFDFDFB))

        fun forMode(mode: BiblionThemeMode): IntroStyle = when (mode) {
            BiblionThemeMode.LIGHT -> IntroStyle(
                background = listOf(Color(0xFFFFFFFF), Color(0xFFFAF7F0)),
                glow = Color(0xFF2F6FD0),
                glowAlpha = 0.10f,
                letterBrush = LightLetter,
                letterLight = Color(0xFF6E9EE8),
                shimmerBlue = Color(0xFF9CC2FF),
                trace = Color(0xFFD9A845),
                traceGlow = Color(0xFFE8C467),
                reflection = 0.8f,
            )
            BiblionThemeMode.DARK -> IntroStyle(
                background = listOf(Color(0xFF05080D), Color(0xFF000000)),
                glow = Color(0xFF1E5BB8),
                glowAlpha = 0.18f,
                letterBrush = CreamLetter,
                letterLight = Color(0xFF8DB8FF),
                shimmerBlue = Color(0xFFBFD7FF),
                trace = Color(0xFFF6DC8A),
                traceGlow = Color(0xFFE8B84A),
                reflection = 1f,
            )
            BiblionThemeMode.BLUE -> IntroStyle(
                background = listOf(Color(0xFF12406B), Color(0xFF0B2A4A), Color(0xFF071E37)),
                glow = Color(0xFF4D8FE8),
                glowAlpha = 0.15f,
                letterBrush = CreamLetter,
                letterLight = Color(0xFFA9CBFF),
                shimmerBlue = Color(0xFFCFE2FF),
                trace = Color(0xFFF6DC8A),
                traceGlow = Color(0xFFE8B84A),
                reflection = 0.9f,
            )
        }
    }
}

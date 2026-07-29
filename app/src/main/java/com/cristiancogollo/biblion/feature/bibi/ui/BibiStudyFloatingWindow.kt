package com.cristiancogollo.biblion.feature.bibi.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.cristiancogollo.biblion.feature.studydocs.domain.StudyEditorUiState
import com.cristiancogollo.biblion.feature.bibi.model.BibiPassage
import com.cristiancogollo.biblion.core.ui.motion.BiblionMotion
import com.cristiancogollo.biblion.core.ui.motion.rememberBiblionMotionEnabled

@Composable
fun BibiStudyFloatingWindow(
    editorState: StudyEditorUiState,
    currentUserName: String?,
    onClose: () -> Unit,
    onOpenPassage: (BibiPassage) -> Unit,
) {
    BibiFloatingWindow(onClose = onClose) {
        BibiStudyPanel(
            editorState = editorState,
            currentUserName = currentUserName,
            onClose = onClose,
            onOpenPassage = onOpenPassage,
        )
    }
}

@Composable
fun BibiFloatingWindow(
    onClose: () -> Unit,
    content: @Composable () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val motionEnabled = rememberBiblionMotionEnabled()
    val entrance = remember { Animatable(if (motionEnabled) 0f else 1f) }
    LaunchedEffect(motionEnabled) {
        if (motionEnabled) {
            entrance.animateTo(
                1f,
                tween(BiblionMotion.STANDARD_MS, easing = FastOutSlowInEasing),
            )
        } else {
            entrance.snapTo(1f)
        }
    }
    val isCompactWidth = configuration.screenWidthDp < 600
    val isCompactHeight = configuration.screenHeightDp < 520
    val horizontalMargin = if (isCompactWidth) 8 else 20
    val verticalMargin = if (isCompactHeight) 8 else 20
    val availableWidth = (configuration.screenWidthDp - horizontalMargin * 2).coerceAtLeast(1)
    val availableHeight = (configuration.screenHeightDp - verticalMargin * 2).coerceAtLeast(1)
    val preferredWidth = when {
        configuration.screenWidthDp < 360 -> availableWidth
        isCompactWidth -> minOf(availableWidth, 400)
        else -> minOf(availableWidth, 480)
    }
    val preferredHeight = when {
        isCompactHeight -> availableHeight
        isCompactWidth -> minOf(availableHeight, 680)
        else -> minOf(availableHeight, 720)
    }
    val popupAlignment = if (isCompactWidth) Alignment.BottomCenter else Alignment.CenterEnd

    Popup(
        alignment = popupAlignment,
        onDismissRequest = onClose,
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            clippingEnabled = true,
        ),
    ) {
        Box(
            modifier = Modifier.padding(
                end = horizontalMargin.dp,
                start = if (isCompactWidth) horizontalMargin.dp else 0.dp,
                top = verticalMargin.dp,
                bottom = verticalMargin.dp,
            ),
        ) {
            Surface(
                modifier = Modifier
                    .width(preferredWidth.dp)
                    .height(preferredHeight.dp)
                    .graphicsLayer {
                        alpha = entrance.value
                        scaleX = 0.92f + entrance.value * 0.08f
                        scaleY = 0.92f + entrance.value * 0.08f
                        translationY = (1f - entrance.value) * 28f
                    },
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f),
                ),
                tonalElevation = 8.dp,
                shadowElevation = 22.dp,
            ) {
                content()
            }
        }
    }
}

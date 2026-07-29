package com.cristiancogollo.biblion.feature.achievements.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.cristiancogollo.biblion.feature.achievements.domain.AchievementDefinition
import com.cristiancogollo.biblion.feature.achievements.data.AchievementRepository
import com.cristiancogollo.biblion.feature.achievements.tracking.AchievementTracker
import com.cristiancogollo.biblion.core.ui.motion.BiblionMotion
import com.cristiancogollo.biblion.core.ui.motion.rememberBiblionMotionEnabled
import kotlinx.coroutines.delay

@Composable
fun AchievementUnlockedHost() {
    val context = LocalContext.current
    val repository = remember(context) { AchievementRepository(context) }
    val motionEnabled = rememberBiblionMotionEnabled()
    var current by remember { mutableStateOf<AchievementDefinition?>(null) }

    LaunchedEffect(Unit) {
        suspend fun show(achievement: AchievementDefinition) {
            current = achievement
            delay(3_500)
            if (current?.id == achievement.id) current = null
            repository.markNotificationSeen(achievement.id)
        }
        repository.pendingNotifications().forEach { pending ->
            show(pending.definition)
        }
        AchievementTracker.unlockedEvents.collect { achievement ->
            show(achievement)
        }
    }

    current?.let { achievement ->
        val entrance = remember(achievement.id) { Animatable(if (motionEnabled) 0f else 1f) }
        LaunchedEffect(achievement.id, motionEnabled) {
            if (motionEnabled) {
                entrance.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = BiblionMotion.EMPHASIS_MS,
                        easing = FastOutSlowInEasing,
                    ),
                )
            } else {
                entrance.snapTo(1f)
            }
        }
        Popup(
            alignment = Alignment.TopCenter,
            properties = PopupProperties(focusable = false),
        ) {
            Surface(
                modifier = Modifier
                    .padding(top = 20.dp, start = 16.dp, end = 16.dp)
                    .widthIn(max = 420.dp)
                    .graphicsLayer {
                        alpha = entrance.value
                        translationY = (1f - entrance.value) * -32f
                        scaleX = 0.94f + entrance.value * 0.06f
                        scaleY = 0.94f + entrance.value * 0.06f
                    },
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                tonalElevation = 8.dp,
                shadowElevation = 14.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val iconScale = 0.75f + entrance.value * 0.25f
                    Text(
                        achievement.icon,
                        fontSize = 28.sp,
                        modifier = Modifier.graphicsLayer {
                            scaleX = iconScale
                            scaleY = iconScale
                        },
                    )
                    androidx.compose.foundation.layout.Column {
                        Text("Logro desbloqueado", style = MaterialTheme.typography.labelMedium)
                        Text(achievement.title, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

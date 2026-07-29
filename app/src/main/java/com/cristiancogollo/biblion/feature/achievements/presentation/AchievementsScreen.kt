package com.cristiancogollo.biblion.feature.achievements.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cristiancogollo.biblion.feature.achievements.data.AchievementRepository
import com.cristiancogollo.biblion.feature.achievements.domain.AchievementCategory
import com.cristiancogollo.biblion.feature.achievements.domain.AchievementProgress
import com.cristiancogollo.biblion.core.ui.motion.BiblionMotion
import com.cristiancogollo.biblion.core.ui.motion.rememberBiblionMotionEnabled

const val ACHIEVEMENTS_ROUTE = "achievements"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember(context) { AchievementRepository(context) }
    val achievements by repository.observeProgress().collectAsState(initial = emptyList())
    val unlocked = achievements.count(AchievementProgress::isUnlocked)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Logros", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(
                        "$unlocked de ${achievements.size.coerceAtLeast(24)} desbloqueados",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { unlocked / achievements.size.coerceAtLeast(1).toFloat() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            AchievementCategory.entries.forEach { category ->
                val categoryItems = achievements.filter { it.definition.category == category }
                if (categoryItems.isNotEmpty()) {
                    item {
                        Text(
                            categoryLabel(category),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    items(categoryItems, key = { it.definition.id }) { progress ->
                        AchievementProgressCard(
                            progress,
                            Modifier.padding(horizontal = 16.dp),
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(100.dp)) }
        }
    }
}

@Composable
private fun AchievementProgressCard(progress: AchievementProgress, modifier: Modifier = Modifier) {
    val motionEnabled = rememberBiblionMotionEnabled()
    val animatedProgress by animateFloatAsState(
        targetValue = progress.normalizedProgress,
        animationSpec = tween(if (motionEnabled) BiblionMotion.EMPHASIS_MS else 0),
        label = "achievementProgress",
    )
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (progress.isUnlocked) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(progress.definition.icon, fontSize = 28.sp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(progress.definition.title, fontWeight = FontWeight.Bold)
                Text(
                    progress.definition.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    if (progress.isUnlocked) "Desbloqueado" else
                        "${progress.currentValue} / ${progress.definition.target}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

private fun categoryLabel(category: AchievementCategory) = when (category) {
    AchievementCategory.READING -> "Lectura bíblica"
    AchievementCategory.SEARCH -> "Búsqueda y descubrimiento"
    AchievementCategory.TOPICS -> "Temas y conexiones"
    AchievementCategory.BIBI -> "Bibi"
    AchievementCategory.STUDY -> "Modo estudio"
    AchievementCategory.GROWTH -> "Diccionario, perfil y constancia"
}

package com.cristiancogollo.biblion

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.cristiancogollo.biblion.ui.theme.BiblionBlueNavigation
import com.cristiancogollo.biblion.ui.theme.BiblionDarkNavigation
import com.cristiancogollo.biblion.ui.theme.BiblionGoldSoft
import com.cristiancogollo.biblion.ui.theme.BiblionNavy
import com.cristiancogollo.biblion.ui.theme.BiblionThemeMode
import com.cristiancogollo.biblion.ui.theme.LocalBiblionThemeMode
import com.cristiancogollo.biblion.feature.studydocs.ui.Screen as StudyDocScreen
import com.cristiancogollo.biblion.core.ui.motion.BiblionMotion
import com.cristiancogollo.biblion.core.ui.motion.rememberBiblionMotionEnabled

@Composable
fun BiblionBottomNavigation(
    currentRoute: String?,
    onHome: () -> Unit,
    onBible: () -> Unit,
    onSearch: () -> Unit,
    onStudy: () -> Unit,
    onProfile: () -> Unit,
    onGuidedTutorialTargetAction: (String) -> Unit = {},
    activeTutorialTargetKey: String? = null,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val viewportWidth = configuration.screenWidthDp.dp
    val capsuleWidth = (configuration.screenWidthDp - 32)
        .coerceIn(0, 680)
        .dp
    val capsuleColor = when (LocalBiblionThemeMode.current) {
        BiblionThemeMode.LIGHT -> BiblionNavy
        BiblionThemeMode.BLUE -> BiblionBlueNavigation
        BiblionThemeMode.DARK -> BiblionDarkNavigation
    }

    Popup(
        alignment = Alignment.BottomCenter,
        properties = PopupProperties(focusable = false)
    ) {
        Box(
            modifier = modifier
                .width(viewportWidth)
                .navigationBarsPadding()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .width(capsuleWidth),
                shape = RoundedCornerShape(32.dp),
                color = capsuleColor,
                shadowElevation = 18.dp,
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BiblionBottomNavigationItem("Home", Icons.Default.Home, currentRoute == Screen.Home.route, onHome)
                    BiblionBottomNavigationItem(
                        label = "Biblia",
                        icon = Icons.Default.MenuBook,
                        selected = currentRoute?.startsWith(Screen.Books.route.substringBefore("/{")) == true ||
                            currentRoute?.startsWith("reader") == true,
                        onClick = {
                            onGuidedTutorialTargetAction(GuidedTutorialTargets.NAV_BIBLE)
                            onBible()
                        },
                        tutorialHighlighted =
                            activeTutorialTargetKey == GuidedTutorialTargets.NAV_BIBLE,
                    )
                    BiblionBottomNavigationItem(
                        label = "Buscar",
                        icon = Icons.Default.Search,
                        selected = currentRoute?.startsWith(Screen.Search.route) == true,
                        onClick = {
                            onGuidedTutorialTargetAction(GuidedTutorialTargets.NAV_SEARCH)
                            onSearch()
                        },
                        tutorialHighlighted =
                            activeTutorialTargetKey == GuidedTutorialTargets.NAV_SEARCH,
                    )
                    BiblionBottomNavigationItem(
                        label = "Estudio",
                        icon = Icons.Default.EditNote,
                        selected = currentRoute?.startsWith(StudyDocScreen.StudyDocsList.route) == true,
                        onClick = {
                            onGuidedTutorialTargetAction(GuidedTutorialTargets.NAV_STUDY)
                            onStudy()
                        },
                        tutorialHighlighted =
                            activeTutorialTargetKey == GuidedTutorialTargets.NAV_STUDY,
                    )
                    BiblionBottomNavigationItem("Perfil", Icons.Default.AccountCircle, currentRoute?.startsWith(Screen.Profile.route) == true, onProfile)
                }
            }
        }
    }
}

@Composable
private fun BiblionBottomNavigationItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tutorialHighlighted: Boolean = false,
) {
    val itemShape = RoundedCornerShape(18.dp)
    val motionEnabled = rememberBiblionMotionEnabled()
    val iconColor by animateColorAsState(
        targetValue = if (selected) BiblionGoldSoft else Color.White.copy(alpha = 0.9f),
        animationSpec = tween(if (motionEnabled) BiblionMotion.QUICK_MS else 0),
        label = "navigationIconColor",
    )
    TextButton(
        onClick = { if (!selected) onClick() },
        modifier = modifier.then(
            if (tutorialHighlighted) {
                Modifier
                    .background(BiblionGoldSoft.copy(alpha = 0.18f), itemShape)
                    .border(2.dp, BiblionGoldSoft, itemShape)
            } else {
                Modifier
            }
        ),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Icon(icon, contentDescription = label, tint = iconColor)
            Text(label, color = iconColor, fontSize = 11.sp)
            Box(
                modifier = Modifier
                    .padding(top = 1.dp)
                    .size(4.dp)
                    .background(
                        color = if (selected) BiblionGoldSoft else Color.Transparent,
                        shape = RoundedCornerShape(50),
                    ),
            )
        }
    }
}

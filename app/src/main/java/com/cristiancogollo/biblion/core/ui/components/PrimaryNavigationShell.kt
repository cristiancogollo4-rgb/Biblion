package com.cristiancogollo.biblion

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.cristiancogollo.biblion.core.ui.motion.BiblionMotion
import com.cristiancogollo.biblion.core.ui.motion.rememberBiblionMotionEnabled
import com.cristiancogollo.biblion.feature.studydocs.ui.Screen as StudyDocScreen

/**
 * Comportamiento propio de la barra para una pantalla que no navega el NavHost principal
 * (por ejemplo, el editor de estudio, cuyas pestañas mueven solo el panel de la Biblia).
 */
data class PrimaryNavigationOverride(
    val currentRoute: String?,
    val onHome: () -> Unit,
    val onBible: () -> Unit,
    val onSearch: () -> Unit,
    val onStudy: () -> Unit,
    val onProfile: () -> Unit,
    val visible: Boolean = true,
    val allowReselectStudy: Boolean = false,
)

/** Overrides registrados, indexados por la entrada del NavHost principal que los publica. */
@Stable
class PrimaryNavigationRegistry {
    internal val overrides = mutableStateMapOf<String, PrimaryNavigationOverride>()
}

val LocalPrimaryNavigationRegistry = staticCompositionLocalOf<PrimaryNavigationRegistry?> { null }

/**
 * Publica un [PrimaryNavigationOverride] mientras la pantalla esté compuesta. Solo aplica
 * cuando la pantalla es la entrada actual del NavHost principal.
 */
@Composable
fun RegisterPrimaryNavigationOverride(override: PrimaryNavigationOverride) {
    val registry = LocalPrimaryNavigationRegistry.current ?: return
    val ownerEntryId = (LocalLifecycleOwner.current as? NavBackStackEntry)?.id ?: return
    val latest by rememberUpdatedState(override)
    // Los callbacks leen `latest` para no quedar obsoletos sin republicar en cada recomposición.
    val published = remember(override.currentRoute, override.visible, override.allowReselectStudy) {
        PrimaryNavigationOverride(
            currentRoute = override.currentRoute,
            onHome = { latest.onHome() },
            onBible = { latest.onBible() },
            onSearch = { latest.onSearch() },
            onStudy = { latest.onStudy() },
            onProfile = { latest.onProfile() },
            visible = override.visible,
            allowReselectStudy = override.allowReselectStudy,
        )
    }
    SideEffect {
        if (registry.overrides[ownerEntryId] !== published) registry.overrides[ownerEntryId] = published
    }
    DisposableEffect(registry, ownerEntryId) {
        onDispose { registry.overrides.remove(ownerEntryId) }
    }
}

/** Rutas del grafo principal que muestran la barra sin necesidad de override. */
internal fun routeShowsPrimaryNavigation(route: String?): Boolean {
    if (route == null) return false
    return route == Screen.Home.route ||
        route == Screen.Books.route ||
        route == Screen.Search.route ||
        route == Screen.Profile.route ||
        route == Screen.ReaderWithBook.route ||
        route == Screen.ReaderWithoutBook.route ||
        route == StudyDocScreen.StudyDocsList.route
}

private fun tutorialScreenFor(route: String?): GuidedTutorialScreenTarget? = when {
    route == null -> null
    route == Screen.Home.route -> GuidedTutorialScreenTarget.HOME
    route == Screen.Books.route -> GuidedTutorialScreenTarget.BOOKS
    route.startsWith("reader") -> GuidedTutorialScreenTarget.READER
    route == Screen.Search.route -> GuidedTutorialScreenTarget.SEARCH
    else -> null
}

private val navigationTutorialTargets = setOf(
    GuidedTutorialTargets.NAV_BIBLE,
    GuidedTutorialTargets.NAV_SEARCH,
    GuidedTutorialTargets.NAV_STUDY,
)

/**
 * Única barra de navegación de la app. Se dibuja una vez por encima del NavHost principal,
 * así que no se duplica ni desaparece durante las transiciones entre pantallas.
 */
@Composable
fun PrimaryNavigationBar(
    navController: NavController,
    registry: PrimaryNavigationRegistry,
    onProfile: () -> Unit,
    guidedTutorial: GuidedTutorialProgress?,
    onGuidedTutorialTargetAction: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentEntry by navController.currentBackStackEntryAsState()
    val route = currentEntry?.destination?.route
    val override = currentEntry?.id?.let { registry.overrides[it] }
    val isKeyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val visible = !isKeyboardVisible &&
        (override?.visible ?: routeShowsPrimaryNavigation(route))

    val tutorialTargetKey = guidedTutorial
        ?.currentStep()
        ?.takeIf { it.screenTarget == tutorialScreenFor(route) }
        ?.targetKey
        ?.takeIf { it in navigationTutorialTargets }

    val motionEnabled = rememberBiblionMotionEnabled()
    val fadeMillis = if (motionEnabled) BiblionMotion.QUICK_MS else 0
    val defaultOverride = remember(navController, onProfile) {
        PrimaryNavigationOverride(
            currentRoute = null,
            onHome = { navController.navigateTopLevel(Screen.Home.route) },
            onBible = { navController.navigateTopLevel(Screen.Books.createRoute(Testament.OLD)) },
            onSearch = { navController.navigateTopLevel(Screen.Search.createRoute()) },
            onStudy = { navController.navigateTopLevel(StudyDocScreen.StudyDocsList.route) },
            onProfile = onProfile,
        )
    }
    val active = override ?: defaultOverride

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(fadeMillis)),
        exit = fadeOut(tween(fadeMillis)),
    ) {
        BiblionBottomNavigation(
            currentRoute = override?.currentRoute ?: route,
            onHome = active.onHome,
            onBible = active.onBible,
            onSearch = active.onSearch,
            onStudy = active.onStudy,
            onProfile = active.onProfile,
            onGuidedTutorialTargetAction = onGuidedTutorialTargetAction,
            activeTutorialTargetKey = tutorialTargetKey,
            allowReselectStudy = active.allowReselectStudy,
        )
    }
}

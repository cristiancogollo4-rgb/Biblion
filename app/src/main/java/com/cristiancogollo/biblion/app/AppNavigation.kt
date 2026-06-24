package com.cristiancogollo.biblion

import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cristiancogollo.biblion.feature.auth.data.GoogleCredentialsAuth
import com.cristiancogollo.biblion.feature.auth.data.GoogleCredentialsResult
import com.cristiancogollo.biblion.feature.studydocs.ui.Screen as StudyDocScreen
import kotlinx.coroutines.launch

@Composable
fun AppNavigation(
    isDarkTheme: Boolean,
    onToggleDarkTheme: (Boolean) -> Unit
) {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val profileViewModel: ProfileViewModel = viewModel()
    val authState by authViewModel.state.collectAsState()
    val profileState by profileViewModel.state.collectAsState()
    val context = LocalContext.current
    val appContext = context.applicationContext
    val googleCredentialsAuth = remember(context) { GoogleCredentialsAuth(context) }
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = context.findActivity()
    var showAuthDialog by remember { mutableStateOf(false) }
    var authDialogMode by remember { mutableStateOf(AuthDialogMode.LOGIN) }
    var activeGuidedTutorial by remember { mutableStateOf<GuidedTutorialProgress?>(null) }
    val currentUserName = preferredUserName(profileState, authState.currentUser)

    LaunchedEffect(Unit) {
        val saved = AppPreferencesSyncStore.getActiveGuidedTutorial(appContext)
        Log.d("GUIDE_DEBUG", "LaunchedEffect(Unit) init - saved=$saved hasCompleted=${AppPreferencesSyncStore.hasCompletedReadingGuide(appContext)}")
        activeGuidedTutorial = saved
        if (saved == null && !AppPreferencesSyncStore.hasCompletedReadingGuide(appContext)) {
            Log.d("GUIDE_DEBUG", "Starting reading guide on first install")
            AppPreferencesSyncStore.startGuidedTutorial(appContext, GuidedTutorialId.READING)
            activeGuidedTutorial = GuidedTutorialProgress(
                guideId = GuidedTutorialId.READING,
                stepIndex = 0
            )
        }
    }

    fun openAuthDialog(mode: AuthDialogMode = AuthDialogMode.LOGIN) {
        authDialogMode = mode
        authViewModel.process(AuthIntent.ClearError)
        showAuthDialog = true
    }

    fun navigateToProfile() {
        if (authState.isAuthenticated) {
            navController.navigateSingleTop(Screen.Profile.route)
        } else {
            openAuthDialog(AuthDialogMode.LOGIN)
        }
    }

    fun startGoogleSignIn() {
        if (activity == null) {
            authViewModel.onGoogleSignInUnavailable()
            return
        }

        authViewModel.beginGoogleSignIn()
        scope.launch {
            try {
                when (val result = googleCredentialsAuth.requestIdToken(activity)) {
                    is GoogleCredentialsResult.Success -> {
                        authViewModel.signInWithGoogleIdToken(result.idToken)
                    }

                    GoogleCredentialsResult.Cancelled -> {
                        authViewModel.onGoogleSignInCancelled()
                    }

                    is GoogleCredentialsResult.Failure -> {
                        Log.w(
                            "BiblionAuth",
                            "Google sign-in credential request failed. Check Firebase OAuth config and SHA-1/SHA-256 fingerprints for the active signing key.",
                            result.throwable
                        )
                        authViewModel.onGoogleSignInConfigurationError()
                    }
                }
            } catch (exception: Throwable) {
                Log.e(
                    "BiblionAuth",
                    "Unexpected Google sign-in crash avoided",
                    exception
                )
                authViewModel.onGoogleSignInUnavailable()
            }
        }
    }

    LaunchedEffect(appContext) {
        FirestoreSyncManager.initialize(appContext)
    }

    LaunchedEffect(appContext) {
        FirestoreSyncManager.syncErrors.collect {
            Toast.makeText(
                appContext,
                appContext.getString(R.string.sync_cloud_error),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    LaunchedEffect(authState.currentUser?.uid) {
        val user = authState.currentUser
        profileViewModel.setCurrentUser(user)
        if (user != null) {
            Log.d("FirestoreSync", "AppNavigation detected authenticated user uid=${user.uid}")
            FirestoreSyncManager.start(user)
        } else {
            Log.d("FirestoreSync", "AppNavigation detected signed-out state")
            FirestoreSyncManager.stop()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                FirestoreSyncManager.refreshNow()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(authViewModel, navController) {
        authViewModel.effects.collect { effect ->
            when (effect) {
                AuthEffect.NavigateHome -> {
                    showAuthDialog = false
                    val returnedToExistingHome = navController.popBackStack(
                        Screen.Home.route,
                        inclusive = false
                    )
                    if (!returnedToExistingHome) {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                inclusive = false
                            }
                            launchSingleTop = true
                        }
                    }
                }

                AuthEffect.NavigateLogin -> {
                    openAuthDialog(AuthDialogMode.LOGIN)
                }
            }
        }
    }

    fun startGuidedTutorial(guideId: GuidedTutorialId) {
        AppPreferencesSyncStore.startGuidedTutorial(appContext, guideId)
        activeGuidedTutorial = GuidedTutorialProgress(guideId = guideId, stepIndex = 0)
    }

    fun restartGuidedTutorial() {
        val guideId = activeGuidedTutorial?.guideId ?: GuidedTutorialId.READING
        startGuidedTutorial(guideId)
        navController.navigate(Screen.Home.route) {
            launchSingleTop = true
        }
    }

    fun advanceGuidedTutorial() {
        val current = activeGuidedTutorial ?: return
        val nextIndex = current.stepIndex + 1
        val steps = guidedTutorialSteps(current.guideId)
        Log.d("GUIDE_DEBUG", "advanceGuidedTutorial currentStep=${current.stepIndex} (${steps.getOrNull(current.stepIndex)?.id}) nextIndex=$nextIndex totalSteps=${steps.size}")
        if (nextIndex >= steps.size) {
            Log.d("GUIDE_DEBUG", "Completing tutorial ${current.guideId}")
            AppPreferencesSyncStore.completeGuidedTutorial(appContext, current.guideId)
            activeGuidedTutorial = null
        } else {
            val next = current.copy(stepIndex = nextIndex)
            Log.d("GUIDE_DEBUG", "Advancing to step $nextIndex (${steps.getOrNull(nextIndex)?.id})")
            AppPreferencesSyncStore.updateGuidedTutorialStep(appContext, next.guideId, next.stepIndex)
            activeGuidedTutorial = next
        }
    }

    fun skipGuidedTutorial() {
        val current = activeGuidedTutorial
        if (current?.guideId == GuidedTutorialId.READING) {
            AppPreferencesSyncStore.completeGuidedTutorial(appContext, current.guideId)
        } else {
            AppPreferencesSyncStore.clearActiveGuidedTutorial(appContext)
        }
        activeGuidedTutorial = null
    }

    fun handleGuidedTutorialTargetAction(targetKey: String) {
        val currentStep = activeGuidedTutorial?.currentStep() ?: return
        Log.d("GUIDE_DEBUG", "handleTargetAction targetKey=$targetKey currentStep=${currentStep.id} actionRequired=${currentStep.actionRequired} stepTargetKey=${currentStep.targetKey}")
        if (currentStep.actionRequired && currentStep.targetKey == targetKey) {
            Log.d("GUIDE_DEBUG", "Advancing tutorial from step ${currentStep.id}")
            advanceGuidedTutorial()
        } else {
            Log.d("GUIDE_DEBUG", "NOT advancing - actionRequired=${currentStep.actionRequired} targetMatch=${currentStep.targetKey == targetKey}")
        }
    }

    var didResumeGuidedTutorial by remember { mutableStateOf(false) }

    LaunchedEffect(activeGuidedTutorial?.guideId, activeGuidedTutorial?.stepIndex) {
        val current = activeGuidedTutorial ?: return@LaunchedEffect
        Log.d("GUIDE_DEBUG", "LaunchedEffect resume: guideId=${current.guideId} stepIndex=${current.stepIndex} didResume=$didResumeGuidedTutorial")
        if (!didResumeGuidedTutorial) {
            didResumeGuidedTutorial = true
            current.resumeRoute()?.let { route ->
                Log.d("GUIDE_DEBUG", "Navigating to resume route: $route")
                if (route != Screen.Home.route) {
                    navController.navigate(route) {
                        launchSingleTop = true
                    }
                }
            }
        }
    }

    NavHost(navController = navController, startDestination = Screen.Home.route) {

        composable(Screen.Home.route) {
            HomeScreen(
                navController = navController,
                isDarkTheme = isDarkTheme,
                onToggleDarkTheme = onToggleDarkTheme,
                currentUserName = currentUserName,
                currentUserEmail = authState.currentUser?.email,
                isAuthenticated = authState.isAuthenticated,
                showSignedOutDialog = authState.showSignedOutDialog,
                onDismissSignedOutDialog = {
                    authViewModel.process(AuthIntent.DismissSignedOutDialog)
                },
                onAuthActionClick = {
                    if (authState.isAuthenticated) {
                        authViewModel.process(AuthIntent.SignOut)
                        scope.launch {
                            googleCredentialsAuth.clearCredentialState()
                        }
                    } else {
                        openAuthDialog(AuthDialogMode.LOGIN)
                    }
                },
                onNavigateToProfile = ::navigateToProfile,
                guidedTutorial = activeGuidedTutorial,
                onGuidedTutorialNext = ::advanceGuidedTutorial,
                onGuidedTutorialSkip = ::skipGuidedTutorial,
                onGuidedTutorialRestart = ::restartGuidedTutorial,
                onGuidedTutorialTargetAction = ::handleGuidedTutorialTargetAction,
                onStartGuidedTutorial = ::startGuidedTutorial
            )
        }

        addSharedPrimaryDestinations(
            navController = navController,
            includeHome = false,
            includeBooks = false,
            isDarkTheme = isDarkTheme,
            onToggleDarkTheme = onToggleDarkTheme,
            currentUserName = currentUserName,
            currentUserEmail = authState.currentUser?.email,
            isAuthenticated = authState.isAuthenticated,
            showSignedOutDialog = authState.showSignedOutDialog,
            onDismissSignedOutDialog = {
                authViewModel.process(AuthIntent.DismissSignedOutDialog)
            },
            onAuthActionClick = {
                if (authState.isAuthenticated) {
                    authViewModel.process(AuthIntent.SignOut)
                    scope.launch {
                        googleCredentialsAuth.clearCredentialState()
                    }
                } else {
                    openAuthDialog(AuthDialogMode.LOGIN)
                }
            },
            onNavigateToProfile = ::navigateToProfile
        )

        composable(Screen.Profile.route) {
            if (!authState.isAuthenticated) {
                LaunchedEffect(Unit) {
                    openAuthDialog(AuthDialogMode.LOGIN)
                    navController.popBackStackOrNavigateHome()
                }
            } else {
                ProfileScreen(
                    navController = navController,
                    uiState = profileState,
                    onNombresChange = profileViewModel::updateNombres,
                    onApellidosChange = profileViewModel::updateApellidos,
                    onAliasChange = profileViewModel::updateAlias,
                    onBiografiaChange = profileViewModel::updateBiografia,
                    onAvatarColorChange = profileViewModel::updateAvatarColor,
                    onProfilePhotoSelected = { uri -> profileViewModel.uploadProfilePhoto(appContext, uri) },
                    onClearProfilePhoto = profileViewModel::clearProfilePhoto,
                    onSave = { profileViewModel.saveProfile() },
                    onClearSaveSuccess = profileViewModel::clearSaveSuccess,
                    onRestartTutorial = ::restartGuidedTutorial
                )
            }
        }

        composable(
            route = Screen.Books.route,
            arguments = listOf(navArgument("testament") { type = NavType.StringType })
        ) { backStackEntry ->
            val testament = Testament.fromRouteArg(backStackEntry.arguments?.getString("testament"))
            BooksScreen(
                navController = navController,
                selectedTestament = testament,
                isDarkTheme = isDarkTheme,
                onToggleDarkTheme = onToggleDarkTheme,
                currentUserName = currentUserName,
                currentUserEmail = authState.currentUser?.email,
                isAuthenticated = authState.isAuthenticated,
                showSignedOutDialog = authState.showSignedOutDialog,
                onDismissSignedOutDialog = {
                    authViewModel.process(AuthIntent.DismissSignedOutDialog)
                },
                onAuthActionClick = {
                    if (authState.isAuthenticated) {
                        authViewModel.process(AuthIntent.SignOut)
                        scope.launch {
                            googleCredentialsAuth.clearCredentialState()
                        }
                    } else {
                        openAuthDialog(AuthDialogMode.LOGIN)
                    }
                },
                onNavigateToProfile = ::navigateToProfile,
                guidedTutorial = activeGuidedTutorial,
                onGuidedTutorialNext = ::advanceGuidedTutorial,
                onGuidedTutorialSkip = ::skipGuidedTutorial,
                onGuidedTutorialRestart = ::restartGuidedTutorial,
                onGuidedTutorialTargetAction = ::handleGuidedTutorialTargetAction
            )
        }

        composable(Screen.Login.route) {
            LaunchedEffect(Unit) {
                openAuthDialog(AuthDialogMode.LOGIN)
                navController.popBackStackOrNavigateHome()
            }
        }

        composable(Screen.Register.route) {
            LaunchedEffect(Unit) {
                openAuthDialog(AuthDialogMode.REGISTER)
                navController.popBackStackOrNavigateHome()
            }
        }

        composable(
            route = Screen.ReaderWithBook.route,
            arguments = listOf(
                navArgument("bookName") { type = NavType.StringType },
                navArgument("studyMode") {
                    type = NavType.BoolType
                    defaultValue = false
                },
                navArgument("chapter") {
                    type = NavType.IntType
                    defaultValue = 1
                },
                navArgument("verse") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("studyId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val encodedBookName = backStackEntry.arguments?.getString("bookName") ?: ""
            val studyMode = backStackEntry.arguments?.getBoolean("studyMode") ?: false
            val initialChapter = backStackEntry.arguments?.getInt("chapter") ?: 1
            val initialVerse = decodeArg(backStackEntry.arguments?.getString("verse") ?: "").ifBlank { null }
            val studyId = backStackEntry.arguments?.getLong("studyId")?.takeIf { it > 0 }
            val decodedBookName = decodeArg(encodedBookName).ifBlank { null }

            if (studyMode) {
                LaunchedEffect(Unit) {
                    navController.navigate(StudyDocScreen.StudyDocEditor.newRoute()) {
                        popUpTo(backStackEntry.destination.route ?: Screen.ReaderWithBook.route) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            } else {
                ReaderScreen(
                    navController = navController,
                    bookName = decodedBookName,
                    initialStudyMode = false,
                    initialChapter = initialChapter,
                    targetVerse = initialVerse,
                    initialStudyId = studyId,
                    isDarkTheme = isDarkTheme,
                    onToggleDarkTheme = onToggleDarkTheme,
                    currentUserName = currentUserName,
                    guidedTutorial = activeGuidedTutorial,
                    onGuidedTutorialNext = ::advanceGuidedTutorial,
                    onGuidedTutorialSkip = ::skipGuidedTutorial,
                    onGuidedTutorialRestart = ::restartGuidedTutorial,
                    onGuidedTutorialTargetAction = ::handleGuidedTutorialTargetAction
                )
            }
        }

        composable(
            route = Screen.ReaderWithoutBook.route,
            arguments = listOf(
                navArgument("studyMode") {
                    type = NavType.BoolType
                    defaultValue = true
                },
                navArgument("chapter") {
                    type = NavType.IntType
                    defaultValue = 1
                },
                navArgument("verse") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("studyId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val studyMode = backStackEntry.arguments?.getBoolean("studyMode") ?: true
            val initialChapter = backStackEntry.arguments?.getInt("chapter") ?: 1
            val initialVerse = decodeArg(backStackEntry.arguments?.getString("verse") ?: "").ifBlank { null }
            val studyId = backStackEntry.arguments?.getLong("studyId")?.takeIf { it > 0 }
            if (studyMode) {
                LaunchedEffect(Unit) {
                    navController.navigate(StudyDocScreen.StudyDocEditor.newRoute()) {
                        popUpTo(backStackEntry.destination.route ?: Screen.ReaderWithoutBook.route) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            } else {
                ReaderScreen(
                    navController = navController,
                    bookName = null,
                    initialStudyMode = false,
                    initialChapter = initialChapter,
                    targetVerse = initialVerse,
                    initialStudyId = studyId,
                    isDarkTheme = isDarkTheme,
                    onToggleDarkTheme = onToggleDarkTheme,
                    currentUserName = currentUserName,
                    guidedTutorial = activeGuidedTutorial,
                    onGuidedTutorialNext = ::advanceGuidedTutorial,
                    onGuidedTutorialSkip = ::skipGuidedTutorial,
                    onGuidedTutorialRestart = ::restartGuidedTutorial,
                    onGuidedTutorialTargetAction = ::handleGuidedTutorialTargetAction
                )
            }
        }

        composable(
            route = Screen.Study.route,
            arguments = listOf(
                navArgument("bookName") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { entry ->
            val encodedName = entry.arguments?.getString("bookName") ?: ""
            val bookName = decodeArg(encodedName).ifBlank { null }
            LaunchedEffect(Unit) {
                navController.navigate(StudyDocScreen.StudyDocEditor.newRoute()) {
                    popUpTo(entry.destination.route ?: Screen.Study.route) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            }
        }
    }

    if (authState.showSignedOutDialog) {
        SignedOutDialog(
            onDismiss = {
                authViewModel.process(AuthIntent.DismissSignedOutDialog)
            }
        )
    }

    if (showAuthDialog && !authState.isAuthenticated) {
        AuthDialog(
            mode = authDialogMode,
            uiState = authState,
            onIntent = authViewModel::process,
            onGoogleSignIn = ::startGoogleSignIn,
            onModeChange = { mode -> authDialogMode = mode },
            onDismiss = {
                if (!authState.isLoading) {
                    showAuthDialog = false
                    authViewModel.process(AuthIntent.ClearError)
                }
            }
        )
    }

    if (profileState.requiresCompletion) {
        CompleteProfileDialog(
            uiState = profileState,
            onNombresChange = profileViewModel::updateNombres,
            onApellidosChange = profileViewModel::updateApellidos,
            onAliasChange = profileViewModel::updateAlias,
            onSave = { profileViewModel.saveProfile() },
            onDismiss = profileViewModel::dismissCompletionPrompt
        )
    }
}

private fun GuidedTutorialProgress.resumeRoute(): String? {
    return when (currentStep()?.screenTarget) {
        GuidedTutorialScreenTarget.HOME -> Screen.Home.route
        GuidedTutorialScreenTarget.BOOKS -> Screen.Books.createRoute(Testament.OLD)
        GuidedTutorialScreenTarget.READER -> Screen.Reader.createRoute(bookName = "Genesis")
        null -> null
    }
}

private fun preferredUserName(profileState: ProfileUiState, currentUser: AuthUser?): String? {
    val fullName = listOf(profileState.nombres, profileState.apellidos)
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .joinToString(" ")
    val candidates = listOf(
        fullName,
        currentUser?.displayName?.trim().orEmpty(),
        profileState.alias.trim(),
        currentUser?.email?.substringBefore("@")?.trim().orEmpty()
    )
    return candidates.firstOrNull { it.isNotBlank() }
}

package com.cristiancogollo.biblion

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

@Composable
fun AppNavigation(
    isDarkTheme: Boolean,
    onToggleDarkTheme: (Boolean) -> Unit
) {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()

    LaunchedEffect(authViewModel, navController) {
        authViewModel.effects.collect { effect ->
            when (effect) {
                AuthEffect.NavigateHome -> {
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
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
        }
    }

    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            val authState by authViewModel.state.collectAsState()
            val context = LocalContext.current
            val googleCredentialsAuth = remember(context) { GoogleCredentialsAuth(context) }
            val scope = rememberCoroutineScope()

            HomeScreen(
                navController = navController,
                isDarkTheme = isDarkTheme,
                onToggleDarkTheme = onToggleDarkTheme,
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
                        navController.navigateSingleTop(Screen.Login.route)
                    }
                }
            )
        }

        addSharedPrimaryDestinations(
            navController = navController,
            includeHome = false,
            isDarkTheme = isDarkTheme,
            onToggleDarkTheme = onToggleDarkTheme,
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
                    navController.navigateSingleTop(Screen.Login.route)
                }
            }
        )

        composable(Screen.Login.route) {
            val authState by authViewModel.state.collectAsState()
            val context = LocalContext.current
            val activity = context.findActivity()
            val googleCredentialsAuth = remember(context) { GoogleCredentialsAuth(context) }
            val scope = rememberCoroutineScope()

            LoginScreen(
                navController = navController,
                uiState = authState,
                onIntent = authViewModel::process,
                onGoogleSignIn = {
                    if (activity == null) {
                        authViewModel.onGoogleSignInUnavailable()
                        return@LoginScreen
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
                                        "Google sign-in credential request failed",
                                        result.throwable
                                    )
                                    authViewModel.onGoogleSignInUnavailable()
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
            )
        }

        composable(Screen.Register.route) {
            val authState by authViewModel.state.collectAsState()

            RegisterScreen(
                navController = navController,
                uiState = authState,
                onIntent = authViewModel::process
            )
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

            ReaderScreen(
                navController = navController,
                bookName = decodedBookName,
                initialStudyMode = studyMode,
                initialChapter = initialChapter,
                targetVerse = initialVerse,
                initialStudyId = studyId
            )
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
            ReaderScreen(
                navController = navController,
                bookName = null,
                initialStudyMode = studyMode,
                initialChapter = initialChapter,
                targetVerse = initialVerse,
                initialStudyId = studyId
            )
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
            ReaderScreen(navController, bookName, initialStudyMode = true)
        }
    }
}

package com.cristiancogollo.biblion.feature.studydocs.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.cristiancogollo.biblion.feature.studydocs.data.StudyDocDatabase
import com.cristiancogollo.biblion.feature.studydocs.data.StudyDocRepository
import com.cristiancogollo.biblion.feature.studydocs.domain.StudyDocSplitViewModel
import com.cristiancogollo.biblion.feature.studydocs.domain.StudyDocViewModel
import com.cristiancogollo.biblion.feature.studydocs.ui.editor.StudyDocEditorScreen
import com.cristiancogollo.biblion.feature.studydocs.ui.list.StudyDocsListScreen
import com.cristiancogollo.biblion.feature.studydocs.ui.list.StudyDocsListViewModel
import com.cristiancogollo.biblion.feature.studydocs.ui.read.StudyDocReadScreen
import com.cristiancogollo.biblion.feature.studydocs.data.StudyShareManager
import com.cristiancogollo.biblion.feature.studydocs.data.FirestorePublicTeachingRepository
import com.cristiancogollo.biblion.feature.studydocs.data.StudyPublicationRequestRepository
import com.cristiancogollo.biblion.feature.studydocs.ui.repository.PublicTeachingScreen
import com.cristiancogollo.biblion.feature.studydocs.ui.repository.PublicTeachingViewModel
import com.cristiancogollo.biblion.ui.theme.BiblionThemeMode
import com.google.firebase.auth.FirebaseAuth
import com.cristiancogollo.biblion.FirestoreUserProfileRepository
import android.widget.Toast
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun StudyDocsListRoute(navController: NavController) {
    val context = LocalContext.current
    val repository = remember { StudyDocRepository(StudyDocDatabase.getInstance(context).studyDocDao()) }
    val publicationRepository = remember { StudyPublicationRequestRepository() }
    val profileRepository = remember { FirestoreUserProfileRepository() }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val viewModel: StudyDocsListViewModel = viewModel(factory = StudyDocsListViewModel.Factory(repository))
    StudyDocsListScreen(
        navController = navController,
        viewModel = viewModel,
        onBack = { navController.popBackStack() },
        onOpenDoc = { doc -> navController.navigate(Screen.StudyDocRead.createRoute(doc.remoteId ?: doc.id.value)) },
        onEditDoc = { doc -> navController.navigate(Screen.StudyDocEditor.createRoute(doc.remoteId ?: doc.id.value)) },
        onNewDoc = { navController.navigate(Screen.StudyDocEditor.newRoute()) },
        onShare = { doc, format -> StudyShareManager.share(context, doc, format) },
        onRequestPublication = { doc ->
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid == null) {
                Toast.makeText(context, "Inicia sesion para solicitar publicacion", Toast.LENGTH_LONG).show()
            } else {
                scope.launch {
                    try {
                        val profile = profileRepository.observeProfile(uid).first()
                        requireNotNull(profile) { "No se encontro tu perfil" }
                        publicationRepository.submit(doc, profile)
                        Toast.makeText(context, "Solicitud enviada para revision", Toast.LENGTH_LONG).show()
                    } catch (error: Throwable) {
                        Toast.makeText(
                            context,
                            error.message ?: "No se pudo enviar la solicitud",
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                }
            }
        },
    )
}

@Composable
fun StudyDocEditorRoute(
    navController: NavController,
    remoteId: String?,
    isDarkTheme: Boolean = false,
    onToggleDarkTheme: (Boolean) -> Unit = {},
    themeMode: BiblionThemeMode = if (isDarkTheme) BiblionThemeMode.DARK else BiblionThemeMode.LIGHT,
    onThemeModeChange: (BiblionThemeMode) -> Unit = {},
) {
    val context = LocalContext.current
    val repository = remember { StudyDocRepository(StudyDocDatabase.getInstance(context).studyDocDao()) }
    val splitViewModel: StudyDocSplitViewModel = viewModel(factory = StudyDocSplitViewModel.Factory(repository))
    StudyDocEditorScreen(
        splitViewModel = splitViewModel,
        remoteId = remoteId,
        isSplitMode = true,
        onBack = { navController.popBackStack() },
        navController = navController,
        isDarkTheme = isDarkTheme,
        onToggleDarkTheme = onToggleDarkTheme,
        themeMode = themeMode,
        onThemeModeChange = onThemeModeChange,
    )
}

@Composable
fun PublicTeachingRoute(navController: NavController) {
    val context = LocalContext.current
    val localRepository = remember {
        StudyDocRepository(StudyDocDatabase.getInstance(context).studyDocDao())
    }
    val publicRepository = remember { FirestorePublicTeachingRepository() }
    val viewModel: PublicTeachingViewModel = viewModel(
        factory = PublicTeachingViewModel.Factory(publicRepository, localRepository),
    )
    PublicTeachingScreen(
        viewModel = viewModel,
        ownerUid = FirebaseAuth.getInstance().currentUser?.uid,
        onBack = { navController.popBackStack() },
        onOpenDownloaded = { remoteId ->
            navController.navigate(Screen.StudyDocEditor.createRoute(remoteId))
        },
    )
}

@Composable
fun StudyDocReadRoute(
    navController: NavController,
    remoteId: String,
) {
    val context = LocalContext.current
    val repository = remember { StudyDocRepository(StudyDocDatabase.getInstance(context).studyDocDao()) }
    val viewModel: StudyDocViewModel = viewModel(factory = StudyDocViewModel.Factory(repository))
    StudyDocReadScreen(
        viewModel = viewModel,
        remoteId = remoteId,
        onBack = { navController.popBackStack() },
        onEdit = { navController.navigate(Screen.StudyDocEditor.createRoute(remoteId)) },
    )
}

object Screen {
    object StudyDocsList {
        const val route: String = "study_docs_list"
        fun createRoute(): String = route
    }
    object StudyDocEditor {
        const val route: String = "study_doc_editor/{remoteId}"
        fun createRoute(remoteId: String): String = "study_doc_editor/$remoteId"
        fun newRoute(): String = "study_doc_editor_new"
        const val ARG_REMOTE_ID: String = "remoteId"
    }
    object StudyDocRead {
        const val route: String = "study_doc_read/{remoteId}"
        fun createRoute(remoteId: String): String = "study_doc_read/$remoteId"
        const val ARG_REMOTE_ID: String = "remoteId"
    }
}

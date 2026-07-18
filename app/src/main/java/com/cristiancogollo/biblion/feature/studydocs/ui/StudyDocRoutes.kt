package com.cristiancogollo.biblion.feature.studydocs.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.cristiancogollo.biblion.feature.studydocs.data.StudyDocDatabase
import com.cristiancogollo.biblion.feature.studydocs.data.StudyDocRepository
import com.cristiancogollo.biblion.feature.studydocs.domain.StudyDocViewModel
import com.cristiancogollo.biblion.feature.studydocs.ui.editor.StudyDocEditorScreen
import com.cristiancogollo.biblion.feature.studydocs.ui.list.StudyDocsListScreen
import com.cristiancogollo.biblion.feature.studydocs.ui.list.StudyDocsListViewModel
import com.cristiancogollo.biblion.feature.studydocs.ui.read.StudyDocReadScreen

@Composable
fun StudyDocsListRoute(navController: NavController) {
    val context = LocalContext.current
    val repository = remember { StudyDocRepository(StudyDocDatabase.getInstance(context).studyDocDao()) }
    val viewModel: StudyDocsListViewModel = viewModel(factory = StudyDocsListViewModel.Factory(repository))
    StudyDocsListScreen(
        viewModel = viewModel,
        onBack = { navController.popBackStack() },
        onOpenDoc = { doc -> navController.navigate(Screen.StudyDocRead.createRoute(doc.remoteId ?: doc.id.value)) },
        onEditDoc = { doc -> navController.navigate(Screen.StudyDocEditor.createRoute(doc.remoteId ?: doc.id.value)) },
        onNewDoc = { navController.navigate(Screen.StudyDocEditor.newRoute()) },
        onShareText = { doc ->
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                putExtra(android.content.Intent.EXTRA_TEXT, doc.plainText())
                type = "text/plain"
            }
            context.startActivity(android.content.Intent.createChooser(intent, "Compartir"))
        },
    )
}

@Composable
fun StudyDocEditorRoute(
    navController: NavController,
    remoteId: String?,
) {
    val context = LocalContext.current
    val repository = remember { StudyDocRepository(StudyDocDatabase.getInstance(context).studyDocDao()) }
    val viewModel: StudyDocViewModel = viewModel(factory = StudyDocViewModel.Factory(repository))
    StudyDocEditorScreen(
        viewModel = viewModel,
        remoteId = remoteId,
        onBack = { navController.popBackStack() },
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
        fun newRoute(): String = "study_doc_editor/new"
        const val ARG_REMOTE_ID: String = "remoteId"
    }
    object StudyDocRead {
        const val route: String = "study_doc_read/{remoteId}"
        fun createRoute(remoteId: String): String = "study_doc_read/$remoteId"
        const val ARG_REMOTE_ID: String = "remoteId"
    }
}

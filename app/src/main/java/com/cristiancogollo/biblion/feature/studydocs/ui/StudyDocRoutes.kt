package com.cristiancogollo.biblion.feature.studydocs.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.cristiancogollo.biblion.feature.studydocs.data.DocVersionRepository
import com.cristiancogollo.biblion.feature.studydocs.data.StudyDocDatabase
import com.cristiancogollo.biblion.feature.studydocs.data.StudyDocRepository
import com.cristiancogollo.biblion.feature.studydocs.domain.StudyDocViewModel
import com.cristiancogollo.biblion.feature.studydocs.model.StudyDoc
import com.cristiancogollo.biblion.feature.studydocs.ui.history.VersionHistoryScreen
import com.cristiancogollo.biblion.feature.studydocs.ui.editor.StudyDocEditorScreen
import com.cristiancogollo.biblion.feature.studydocs.ui.editor.StudyEditorLayout
import com.cristiancogollo.biblion.feature.studydocs.ui.list.StudyDocsListScreen
import com.cristiancogollo.biblion.feature.studydocs.ui.list.StudyDocsListViewModel
import com.cristiancogollo.biblion.feature.studydocs.ui.read.StudyDocReadScreen
import com.cristiancogollo.biblion.feature.studydocs.ui.templates.StudyTemplatePickerScreen
import kotlinx.coroutines.launch

@Composable
fun StudyDocsListRoute(navController: NavController) {
    val context = LocalContext.current
    val repository = remember { StudyDocRepository(StudyDocDatabase.getInstance(context).studyDocDao()) }
    val viewModel: StudyDocsListViewModel = viewModel(factory = StudyDocsListViewModel.Factory(repository))
    StudyDocsListScreen(
        viewModel = viewModel,
        onBack = { navController.popBackStack() },
        onOpenDoc = { doc -> navController.navigate(Screen.StudyDocRead.createRoute(doc.id.value)) },
        onEditDoc = { doc -> navController.navigate(Screen.StudyDocEditor.createRoute(doc.id.value)) },
        onNewDoc = { navController.navigate(Screen.StudyTemplatePicker.createRoute()) },
        onImportComplete = { doc -> navController.navigate(Screen.StudyDocEditor.createRoute(doc.id.value)) },
    )
}

@Composable
fun StudyDocEditorRoute(
    navController: NavController,
    remoteId: String?,
) {
    val context = LocalContext.current
    val database = remember { StudyDocDatabase.getInstance(context) }
    val repository = remember { StudyDocRepository(database.studyDocDao()) }
    val versionRepository = remember { DocVersionRepository(database.docVersionDao()) }
    val viewModel: StudyDocViewModel = viewModel(factory = StudyDocViewModel.Factory(repository, versionRepository))
    val listViewModel: StudyDocsListViewModel = viewModel(factory = StudyDocsListViewModel.Factory(repository))
    
    // Forzar orientación landscape al entrar al editor
    androidx.compose.runtime.LaunchedEffect(Unit) {
        context.findActivity()?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }
    
    // Restaurar orientación al salir del editor usando onBackPressedCallback
    val activity = context.findActivity() as? androidx.activity.ComponentActivity
    androidx.compose.runtime.DisposableEffect(activity) {
        if (activity != null) {
            val callback = object : androidx.activity.OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    context.findActivity()?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
                    isEnabled = false
                    activity.onBackPressedDispatcher.onBackPressed()
                }
            }
            activity.onBackPressedDispatcher.addCallback(callback)
            
            onDispose {
                callback.remove()
                context.findActivity()?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
            }
        } else {
            onDispose {
                context.findActivity()?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
            }
        }
    }
    
    LaunchedEffect(remoteId) {
        android.util.Log.d("StudyDocEditorRoute", "remoteId=$remoteId")
        if (remoteId == null) {
            viewModel.newDraft()
            android.util.Log.d("StudyDocEditorRoute", "newDraft() llamado")
        } else {
            viewModel.loadByRemoteId(remoteId)
            android.util.Log.d("StudyDocEditorRoute", "loadByRemoteId($remoteId) llamado")
        }
    }
    StudyEditorLayout(
        editorContent = { isExpandable, isExpanded, onToggleExpand, isMultiColumnEnabled, onToggleMultiColumn ->
            StudyDocEditorScreen(
                viewModel = viewModel,
                onBack = {
                    android.util.Log.d("StudyDocEditorRoute", "onBack() llamado, restaurando orientación")
                    val activity = context.findActivity()
                    android.util.Log.d("StudyDocEditorRoute", "Activity encontrada: ${activity != null}")
                    activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
                    android.util.Log.d("StudyDocEditorRoute", "Orientación restaurada a portrait")
                    navController.popBackStack()
                },
                onNavigateToVersionHistory = {
                    navController.navigate(Screen.VersionHistory.createRoute(viewModel.uiState.value.doc.id.value))
                },
                isExpandable = isExpandable,
                isExpanded = isExpanded,
                onToggleExpand = onToggleExpand,
                isMultiColumnEnabled = isMultiColumnEnabled,
                onToggleMultiColumn = onToggleMultiColumn,
            )
        },
        sideContent = {
            StudyDocsListScreen(
                viewModel = listViewModel,
                onBack = { navController.popBackStack() },
                onOpenDoc = { doc -> navController.navigate(Screen.StudyDocRead.createRoute(doc.id.value)) },
                onEditDoc = { doc -> navController.navigate(Screen.StudyDocEditor.createRoute(doc.id.value)) },
                onNewDoc = { navController.navigate(Screen.StudyTemplatePicker.createRoute()) },
                onImportComplete = { doc -> navController.navigate(Screen.StudyDocEditor.createRoute(doc.id.value)) },
            )
        },
    )
}

@Composable
fun StudyDocReadRoute(
    navController: NavController,
    remoteId: String,
) {
    val context = LocalContext.current
    val database = remember { StudyDocDatabase.getInstance(context) }
    val repository = remember { StudyDocRepository(database.studyDocDao()) }
    val versionRepository = remember { DocVersionRepository(database.docVersionDao()) }
    val viewModel: StudyDocViewModel = viewModel(factory = StudyDocViewModel.Factory(repository, versionRepository))
    LaunchedEffect(remoteId) { viewModel.loadByRemoteId(remoteId) }
    StudyDocReadScreen(
        viewModel = viewModel,
        onBack = { navController.popBackStack() },
        onEdit = { navController.navigate(Screen.StudyDocEditor.createRoute(remoteId)) },
    )
}

@Composable
fun StudyTemplatePickerRoute(
    navController: NavController,
) {
    val context = LocalContext.current
    val repository = remember { StudyDocRepository(StudyDocDatabase.getInstance(context).studyDocDao()) }
    val viewModel: StudyDocViewModel = viewModel(factory = StudyDocViewModel.Factory(repository))
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    
    StudyTemplatePickerScreen(
        onTemplateSelected = { template ->
            scope.launch {
                viewModel.newFromTemplate(template)
                val doc = viewModel.uiState.value.doc
                android.util.Log.d("StudyTemplatePicker", "Documento creado: id=${doc.id.value}, title=${doc.title}, blocks=${doc.blocks.size}")
                repository.save(doc)
                android.util.Log.d("StudyTemplatePicker", "Documento guardado en repositorio")
                navController.navigate(Screen.StudyDocEditor.createRoute(doc.id.value)) {
                    popUpTo(Screen.StudyTemplatePicker.route) { inclusive = true }
                }
            }
        },
        onBack = { navController.popBackStack() },
    )
}

@Composable
fun VersionHistoryRoute(
    navController: NavController,
    docRemoteId: String,
) {
    val context = LocalContext.current
    val database = remember { StudyDocDatabase.getInstance(context) }
    val repository = remember { StudyDocRepository(database.studyDocDao()) }
    val versionRepository = remember { DocVersionRepository(database.docVersionDao()) }
    val viewModel: StudyDocViewModel = viewModel(factory = StudyDocViewModel.Factory(repository, versionRepository))
    
    val versions by viewModel.versionHistory.collectAsState()
    
    LaunchedEffect(docRemoteId) {
        viewModel.loadVersionHistory(docRemoteId)
    }
    
    VersionHistoryScreen(
        versions = versions,
        onVersionClick = { version ->
            navController.navigate(Screen.VersionDiff.createRoute(docRemoteId, version.id))
        },
        onRestoreVersion = { version ->
            viewModel.restoreVersion(version.id)
            navController.popBackStack()
        },
        onDeleteVersion = { version ->
            viewModel.deleteVersion(version.id)
        },
        onBack = { navController.popBackStack() },
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
    object StudyTemplatePicker {
        const val route: String = "study_template_picker"
        fun createRoute(): String = route
    }
    object VersionHistory {
        const val route: String = "version_history/{docRemoteId}"
        fun createRoute(docRemoteId: String): String = "version_history/$docRemoteId"
        const val ARG_DOC_REMOTE_ID: String = "docRemoteId"
    }
    object VersionDiff {
        const val route: String = "version_diff/{docRemoteId}/{versionId}"
        fun createRoute(docRemoteId: String, versionId: Long): String = "version_diff/$docRemoteId/$versionId"
        const val ARG_DOC_REMOTE_ID: String = "docRemoteId"
        const val ARG_VERSION_ID: String = "versionId"
    }
}

private fun android.content.Context.findActivity(): android.app.Activity? {
    var context = this
    while (context is android.content.ContextWrapper) {
        if (context is android.app.Activity) return context
        context = context.baseContext
    }
    return null
}

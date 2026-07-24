package com.cristiancogollo.biblion.feature.studydocs.ui.read

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cristiancogollo.biblion.feature.studydocs.domain.StudyDocViewModel
import com.cristiancogollo.biblion.feature.studydocs.ui.editor.UnifiedBlockRenderer
import com.cristiancogollo.biblion.feature.studydocs.ui.editor.ZoomMenu
import com.cristiancogollo.biblion.feature.studydocs.ui.editor.rememberDocumentZoomState
import com.cristiancogollo.biblion.feature.studydocs.ui.pagination.PageFragment
import com.cristiancogollo.biblion.feature.studydocs.ui.pagination.PaginatedSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyDocReadScreen(
    viewModel: StudyDocViewModel,
    remoteId: String? = null,
    onBack: () -> Unit,
    onEdit: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val readZoomState = rememberDocumentZoomState()

    LaunchedEffect(remoteId) {
        Log.d("BIBLION_STUDY", "StudyDocReadScreen LaunchedEffect remoteId=$remoteId")
    }

    LaunchedEffect(uiState.isLoading, uiState.doc.blocks.size) {
        Log.d("BIBLION_STUDY", "StudyDocReadScreen state isLoading=${uiState.isLoading} blocks=${uiState.doc.blocks.size} lastError=${uiState.lastError}")
    }

    LaunchedEffect(remoteId) {
        if (remoteId != null) viewModel.loadByRemoteId(remoteId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(uiState.doc.title.ifBlank { "Sin titulo" }, fontWeight = FontWeight.SemiBold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    ZoomMenu(zoomState = readZoomState, showStepButtons = true)
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = "Editar")
                    }
                },
            )
        },
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.doc.blocks.all { it.plainText().isBlank() }) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    "Documento vacio",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            PaginatedSheet(
                blocks = uiState.doc.blocks,
                isEditing = false,
                modifier = Modifier.fillMaxSize().padding(padding),
                zoomState = readZoomState,
                contentFragmentRenderer = { fragment, _ ->
                    UnifiedBlockRenderer(
                        fragment = fragment,
                        allBlocks = uiState.doc.blocks,
                        isEditing = false,
                        isOwnerFragment = false,
                        richState = null,
                        isActive = false,
                        splitViewModel = null,
                        viewModel = null,
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                contentBlockRenderer = { _, _ -> },
            )
        }
    }
}

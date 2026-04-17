package com.cristiancogollo.biblion

import android.widget.TextView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.core.text.HtmlCompat
import com.cristiancogollo.biblion.ui.theme.BiblionNavy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyReadScreen(
    navController: NavController,
    studyId: Long
) {
    val viewModel: StudyViewModel = viewModel()
    val state by viewModel.state.collectAsState()

    LaunchedEffect(studyId) {
        viewModel.process(StudyIntent.SelectStudy(studyId))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.title.ifBlank { "Sin título" }) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.tags.isNotEmpty()) {
                item {
                    Text(
                        text = "Etiquetas: ${state.tags.joinToString(", ")}",
                        color = BiblionNavy,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(4.dp))
                AndroidView(
                    modifier = Modifier.fillMaxWidth(),
                    factory = { context ->
                        TextView(context).apply {
                            textSize = 18f
                            setLineSpacing(0f, 1.2f)
                            setTextColor(android.graphics.Color.BLACK)
                        }
                    },
                    update = { view ->
                        val html = state.richHtml.ifBlank { "<p>Esta enseñanza no tiene contenido aún.</p>" }
                        view.text = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT)
                    }
                )
            }
        }
    }
}

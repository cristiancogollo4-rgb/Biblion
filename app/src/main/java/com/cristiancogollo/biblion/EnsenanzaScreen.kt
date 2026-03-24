package com.cristiancogollo.biblion

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.cristiancogollo.biblion.ui.theme.BiblionNavy
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnsenanzaScreen(navController: NavController) {
    val viewModel: StudyViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Enseñanzas", color = BiblionNavy, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás", tint = BiblionNavy)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    viewModel.process(StudyIntent.CreateNewStudy)
                    navController.navigate(Screen.Reader.createRoute(studyMode = true))
                },
                containerColor = BiblionNavy,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nueva Enseñanza")
            }
        },
        containerColor = Color(0xFFFDFBF0)
    ) { padding ->
        if (state.allStudies.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No tienes enseñanzas guardadas aún.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.allStudies, key = { it.id }) { study ->
                    EnsenanzaCard(
                        study = study,
                        dateText = dateFormat.format(Date(study.updatedAt)),
                        onOpen = {
                            viewModel.process(StudyIntent.SelectStudy(study.id))
                            val preferredBook = viewModel.preferredBookForStudy(study)
                            navController.navigate(Screen.Reader.createRoute(bookName = preferredBook, studyMode = true))
                        },
                        onDelete = {
                            viewModel.process(StudyIntent.DeleteStudy(study.id))
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun EnsenanzaCard(
    study: StudyEntity,
    dateText: String,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onOpen() }
            ) {
                Text(
                    text = study.title.ifBlank { "Sin título" },
                    style = MaterialTheme.typography.titleLarge,
                    color = BiblionNavy,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Última edición: $dateText",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Eliminar",
                    tint = Color.Red.copy(alpha = 0.6f)
                )
            }
            
            IconButton(onClick = onOpen) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Editar",
                    tint = BiblionNavy
                )
            }
        }
    }
}

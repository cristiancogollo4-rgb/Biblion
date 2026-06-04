package com.cristiancogollo.biblion

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.cristiancogollo.biblion.ui.theme.BiblionBluePrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    uiState: ProfileUiState,
    onNombresChange: (String) -> Unit,
    onApellidosChange: (String) -> Unit,
    onAliasChange: (String) -> Unit,
    onBiografiaChange: (String) -> Unit,
    onAvatarColorChange: (Long) -> Unit,
    onProfilePhotoSelected: (Uri) -> Unit,
    onClearProfilePhoto: () -> Unit,
    onSave: () -> Unit,
    onClearSaveSuccess: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val savedMessage = stringResource(R.string.profile_saved)

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            snackbarHostState.showSnackbar(savedMessage)
            onClearSaveSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        val isWide = LocalConfiguration.current.screenWidthDp >= 720
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isWide) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 980.dp),
                    horizontalArrangement = Arrangement.spacedBy(48.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProfileIdentityPanel(
                        uiState = uiState,
                        onAvatarColorChange = onAvatarColorChange,
                        onProfilePhotoSelected = onProfilePhotoSelected,
                        onClearProfilePhoto = onClearProfilePhoto,
                        modifier = Modifier.weight(1f)
                    )
                    ProfileForm(
                        uiState = uiState,
                        onNombresChange = onNombresChange,
                        onApellidosChange = onApellidosChange,
                        onAliasChange = onAliasChange,
                        onBiografiaChange = onBiografiaChange,
                        onSave = onSave,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                ProfileIdentityPanel(
                    uiState = uiState,
                    onAvatarColorChange = onAvatarColorChange,
                    onProfilePhotoSelected = onProfilePhotoSelected,
                    onClearProfilePhoto = onClearProfilePhoto
                )
                ProfileForm(
                    uiState = uiState,
                    onNombresChange = onNombresChange,
                    onApellidosChange = onApellidosChange,
                    onAliasChange = onAliasChange,
                    onBiografiaChange = onBiografiaChange,
                    onSave = onSave
                )
            }

            ProfileMetricsPanel(
                profile = uiState.profile,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 980.dp)
            )
        }
    }
}

@Composable
fun CompleteProfileDialog(
    uiState: ProfileUiState,
    onNombresChange: (String) -> Unit,
    onApellidosChange: (String) -> Unit,
    onAliasChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.profile_complete_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.profile_complete_message),
                    style = MaterialTheme.typography.bodyMedium
                )
                RequiredProfileFields(
                    uiState = uiState,
                    onNombresChange = onNombresChange,
                    onApellidosChange = onApellidosChange,
                    onAliasChange = onAliasChange
                )
                ProfileError(uiState.errorMessage)
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                enabled = !uiState.isSaving
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(stringResource(R.string.profile_save))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !uiState.isSaving
            ) {
                Text(stringResource(R.string.profile_later))
            }
        },
        shape = RoundedCornerShape(8.dp)
    )
}

@Composable
private fun ProfileIdentityPanel(
    uiState: ProfileUiState,
    onAvatarColorChange: (Long) -> Unit,
    onProfilePhotoSelected: (Uri) -> Unit,
    onClearProfilePhoto: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fullName = listOf(uiState.nombres, uiState.apellidos)
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .joinToString(" ")
        .ifBlank { uiState.alias.ifBlank { stringResource(R.string.profile_alias_placeholder) } }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) onProfilePhotoSelected(uri)
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ProfileAvatar(
            photoUrl = uiState.profile?.fotoPerfil,
            avatarColor = Color(uiState.avatarColor.toInt()),
            isUploading = uiState.isUploadingAvatar
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { photoPicker.launch("image/*") },
                    enabled = !uiState.isUploadingAvatar,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(2.dp, BiblionBluePrimary)
                ) {
                    Text(stringResource(R.string.profile_choose_photo))
                }
                if (!uiState.profile?.fotoPerfil.isNullOrBlank()) {
                    TextButton(
                        onClick = onClearProfilePhoto,
                        enabled = !uiState.isUploadingAvatar
                    ) {
                        Text(stringResource(R.string.profile_use_color))
                    }
                }
            }
            AvatarColorSelector(
                selectedColor = uiState.avatarColor,
                enabled = uiState.profile?.fotoPerfil.isNullOrBlank(),
                onAvatarColorChange = onAvatarColorChange
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = fullName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = uiState.profile?.correo ?: uiState.currentUser?.email.orEmpty(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProfileStatusChip(stringResource(R.string.profile_role), uiState.profile?.rol ?: "LECTOR")
            ProfileStatusChip(stringResource(R.string.profile_plan), uiState.profile?.plan ?: "FREE")
        }
    }
}

@Composable
private fun ProfileForm(
    uiState: ProfileUiState,
    onNombresChange: (String) -> Unit,
    onApellidosChange: (String) -> Unit,
    onAliasChange: (String) -> Unit,
    onBiografiaChange: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.widthIn(max = 420.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        RequiredProfileFields(
            uiState = uiState,
            onNombresChange = onNombresChange,
            onApellidosChange = onApellidosChange,
            onAliasChange = onAliasChange
        )
        OutlinedTextField(
            value = uiState.biografia,
            onValueChange = onBiografiaChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.profile_biography)) },
            minLines = 5,
            maxLines = 5,
            colors = profileTextFieldColors()
        )

        ProfileError(uiState.errorMessage)
        Button(
            onClick = onSave,
            enabled = !uiState.isSaving,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            if (uiState.isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = stringResource(R.string.profile_save).uppercase(),
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
private fun ProfileAvatar(
    photoUrl: String?,
    avatarColor: Color,
    isUploading: Boolean
) {
    Surface(
        modifier = Modifier
            .size(184.dp)
            .clip(CircleShape),
        shape = CircleShape,
        color = avatarColor
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (!photoUrl.isNullOrBlank()) {
                RemoteProfileImage(
                    url = photoUrl,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier.size(188.dp),
                    tint = Color.White
                )
            }
            if (isUploading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.32f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun RemoteProfileImage(
    url: String,
    modifier: Modifier = Modifier
) {
    var imageBitmap by remember(url) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }

    LaunchedEffect(url) {
        imageBitmap = withContext(Dispatchers.IO) {
            runCatching {
                URL(url).openStream().use { stream ->
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()
                }
            }.getOrNull()
        }
    }

    val bitmap = imageBitmap
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        Icon(
            imageVector = Icons.Default.AccountCircle,
            contentDescription = null,
            modifier = Modifier.size(188.dp),
            tint = Color.White
        )
    }
}

@Composable
private fun AvatarColorSelector(
    selectedColor: Long,
    enabled: Boolean,
    onAvatarColorChange: (Long) -> Unit
) {
    val colors = listOf(
        0xFF0B6E9FUL.toLong(),
        0xFF184E77UL.toLong(),
        0xFF7B2CBFUL.toLong(),
        0xFF2D6A4FUL.toLong(),
        0xFFB08900UL.toLong(),
        0xFF9D0208UL.toLong()
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        colors.forEach { colorLong ->
            val color = Color(colorLong.toInt())
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = if (enabled) 1f else 0.34f))
                    .border(
                        width = if (selectedColor == colorLong) 3.dp else 1.dp,
                        color = if (selectedColor == colorLong) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant,
                        shape = CircleShape
                    )
                    .clickable(enabled = enabled) { onAvatarColorChange(colorLong) }
            )
        }
    }
}

@Composable
private fun RequiredProfileFields(
    uiState: ProfileUiState,
    onNombresChange: (String) -> Unit,
    onApellidosChange: (String) -> Unit,
    onAliasChange: (String) -> Unit
) {
    val errorNames = stringResource(R.string.profile_error_names)
    val errorLastNames = stringResource(R.string.profile_error_last_names)
    val errorAlias = stringResource(R.string.profile_error_alias)

    OutlinedTextField(
        value = uiState.nombres,
        onValueChange = onNombresChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.profile_names)) },
        singleLine = true,
        isError = uiState.errorMessage == errorNames,
        colors = profileTextFieldColors()
    )
    OutlinedTextField(
        value = uiState.apellidos,
        onValueChange = onApellidosChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.profile_last_names)) },
        singleLine = true,
        isError = uiState.errorMessage == errorLastNames,
        colors = profileTextFieldColors()
    )
    OutlinedTextField(
        value = uiState.alias,
        onValueChange = onAliasChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.profile_alias)) },
        singleLine = true,
        isError = uiState.errorMessage == errorAlias,
        colors = profileTextFieldColors()
    )
}

@Composable
private fun ProfileStatusChip(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(2.dp, BiblionBluePrimary),
        color = MaterialTheme.colorScheme.surface
    ) {
        Text(
            text = "$label ${value.lowercase()}",
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ProfileMetricsPanel(
    profile: BiblionUserProfile?,
    modifier: Modifier = Modifier
) {
    val metrics = profileMetrics(profile)
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.profile_metrics_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            metrics.take(2).forEach { metric ->
                ProfileMetricCard(
                    label = metric.label,
                    value = metric.value,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            metrics.drop(2).take(2).forEach { metric ->
                ProfileMetricCard(
                    label = metric.label,
                    value = metric.value,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ProfileMetricCard(
    label: String,
    value: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.defaultMinSize(minHeight = 82.dp),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(2.dp, BiblionBluePrimary),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = BiblionBluePrimary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ProfileError(message: String?) {
    if (message.isNullOrBlank()) return
    Text(
        text = message,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium
    )
}

@Composable
private fun profileTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = BiblionBluePrimary,
    unfocusedBorderColor = BiblionBluePrimary,
    focusedLabelColor = BiblionBluePrimary,
    cursorColor = BiblionBluePrimary
)

private data class ProfileMetric(val label: String, val value: Int)

@Composable
private fun profileMetrics(profile: BiblionUserProfile?): List<ProfileMetric> {
    val role = profile?.rol.orEmpty().uppercase()
    val publisherApproved = profile?.estadoPublicador.orEmpty().uppercase() == "APROBADO"
    return if (role == "PUBLICADOR" || role == "ADMIN" || publisherApproved) {
        listOf(
            ProfileMetric(stringResource(R.string.profile_metric_created), profile?.totalEnsenanzasCreadas ?: 0),
            ProfileMetric(stringResource(R.string.profile_metric_published), profile?.totalEnsenanzasPublicadas ?: 0),
            ProfileMetric(stringResource(R.string.profile_metric_likes), profile?.totalLikes ?: 0),
            ProfileMetric(stringResource(R.string.profile_metric_downloads), profile?.totalDescargas ?: 0)
        )
    } else {
        listOf(
            ProfileMetric(stringResource(R.string.profile_metric_created), profile?.totalEnsenanzasCreadas ?: 0),
            ProfileMetric(stringResource(R.string.profile_metric_downloaded), profile?.totalDescargas ?: 0),
            ProfileMetric(stringResource(R.string.profile_metric_saved), profile?.totalGuardados ?: 0),
            ProfileMetric(stringResource(R.string.profile_metric_comments), profile?.totalComentarios ?: 0)
        )
    }
}

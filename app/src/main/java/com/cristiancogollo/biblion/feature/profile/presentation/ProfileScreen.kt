package com.cristiancogollo.biblion

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Brush
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
import com.cristiancogollo.biblion.ui.theme.BiblionGoldPrimary
import com.cristiancogollo.biblion.ui.theme.BiblionGoldSoft
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
    var showEditDialog by remember { mutableStateOf(false) }

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
                    IconButton(onClick = { navController.popBackStackOrNavigateHome() }) {
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
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (uiState.isLoading && uiState.profile == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (isWide) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 980.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    ProfileIdentityPanel(
                        uiState = uiState,
                        onAvatarColorChange = onAvatarColorChange,
                        onProfilePhotoSelected = onProfilePhotoSelected,
                        onClearProfilePhoto = onClearProfilePhoto,
                        modifier = Modifier.weight(1f)
                    )
                    ProfileNetworkPanel(
                        uiState = uiState,
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
                ProfileNetworkPanel(
                    uiState = uiState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 980.dp)
                )
            }

            ProfileMetricsPanel(
                profile = uiState.profile,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 980.dp)
            )

            ProfileActionPanel(
                onUpdateProfile = { showEditDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 980.dp)
            )
        }
    }

    if (showEditDialog) {
        ProfileEditDialog(
            uiState = uiState,
            onNombresChange = onNombresChange,
            onApellidosChange = onApellidosChange,
            onAliasChange = onAliasChange,
            onBiografiaChange = onBiografiaChange,
            onAvatarColorChange = onAvatarColorChange,
            onProfilePhotoSelected = onProfilePhotoSelected,
            onClearProfilePhoto = onClearProfilePhoto,
            onSave = onSave,
            onDismiss = { showEditDialog = false }
        )
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
private fun ProfileEditDialog(
    uiState: ProfileUiState,
    onNombresChange: (String) -> Unit,
    onApellidosChange: (String) -> Unit,
    onAliasChange: (String) -> Unit,
    onBiografiaChange: (String) -> Unit,
    onAvatarColorChange: (Long) -> Unit,
    onProfilePhotoSelected: (Uri) -> Unit,
    onClearProfilePhoto: () -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.profile_update_title)) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 620.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                ProfileIdentityPanel(
                    uiState = uiState,
                    onAvatarColorChange = onAvatarColorChange,
                    onProfilePhotoSelected = onProfilePhotoSelected,
                    onClearProfilePhoto = onClearProfilePhoto,
                    showProfileActions = true
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
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        },
        shape = RoundedCornerShape(8.dp)
    )
}

@Composable
private fun ProfileActionPanel(
    onUpdateProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onUpdateProfile,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(stringResource(R.string.profile_update_profile))
            }
            OutlinedButton(
                onClick = {},
                enabled = false,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(stringResource(R.string.profile_update_plan_future))
            }
        }
    }
}

@Composable
private fun ProfileIdentityPanel(
    uiState: ProfileUiState,
    onAvatarColorChange: (Long) -> Unit,
    onProfilePhotoSelected: (Uri) -> Unit,
    onClearProfilePhoto: () -> Unit,
    modifier: Modifier = Modifier,
    showProfileActions: Boolean = false
) {
    val fullName = listOf(uiState.nombres, uiState.apellidos)
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .joinToString(" ")
        .ifBlank { uiState.alias.ifBlank { stringResource(R.string.profile_alias_placeholder) } }
    val identityTextColor = Color.White
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) onProfilePhotoSelected(uri)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 980.dp),
        shape = RoundedCornerShape(8.dp),
        color = BiblionBluePrimary,
        border = BorderStroke(1.dp, BiblionGoldSoft.copy(alpha = 0.62f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.profile_public_identity),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = BiblionGoldSoft
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProfileAvatar(
                    photoUrl = uiState.profile?.fotoPerfil,
                    avatarColor = Color(uiState.avatarColor.toInt()),
                    isUploading = uiState.isUploadingAvatar
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = fullName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = identityTextColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "@${uiState.alias.ifBlank { stringResource(R.string.profile_alias_placeholder) }}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = BiblionGoldSoft,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = uiState.profile?.correo ?: uiState.currentUser?.email.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = identityTextColor.copy(alpha = 0.72f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = uiState.biografia.ifBlank { stringResource(R.string.profile_biography_empty) },
                        style = MaterialTheme.typography.bodyMedium,
                        color = identityTextColor.copy(alpha = 0.86f),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            ProfileCompletionMeter(uiState, onDarkSurface = true)

            if (showProfileActions) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { photoPicker.launch("image/*") },
                            enabled = !uiState.isUploadingAvatar,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, BiblionBluePrimary)
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
            }
        }
    }
}

@Composable
private fun ProfileCompletionMeter(
    uiState: ProfileUiState,
    onDarkSurface: Boolean = false
) {
    val completed = listOf(
        uiState.nombres.isNotBlank(),
        uiState.apellidos.isNotBlank(),
        uiState.alias.isNotBlank(),
        uiState.biografia.isNotBlank()
    ).count { it }
    val progress = completed / 4f
    val percent = (progress * 100).toInt()
    val textColor = if (onDarkSurface) Color.White else MaterialTheme.colorScheme.onSurface
    val accentColor = if (onDarkSurface) BiblionGoldSoft else BiblionBluePrimary
    val trackColor = if (onDarkSurface) Color.White.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.profile_completion_title),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Text(
                text = stringResource(R.string.profile_completion_percent, percent),
                style = MaterialTheme.typography.labelLarge,
                color = accentColor,
                fontWeight = FontWeight.Bold
            )
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(50)),
            color = accentColor,
            trackColor = trackColor
        )
    }
}

@Composable
private fun ProfileNetworkPanel(
    uiState: ProfileUiState,
    modifier: Modifier = Modifier
) {
    var detailsExpanded by remember { mutableStateOf(true) }
    val profile = uiState.profile
    val role = profile?.rol ?: "LECTOR"
    val publisherStatus = profile?.estadoPublicador ?: "NO_APROBADO"
    val plan = profile?.plan ?: "FREE"
    val statusMessage = when (publisherStatus.uppercase()) {
        "APROBADO" -> stringResource(R.string.profile_status_approved_message)
        "PENDIENTE" -> stringResource(R.string.profile_status_pending_message)
        "SUSPENDIDO" -> stringResource(R.string.profile_status_suspended_message)
        else -> stringResource(R.string.profile_status_reader_message)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 980.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.profile_network_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = statusMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.76f)
                    )
                }
                IconButton(onClick = { detailsExpanded = !detailsExpanded }) {
                    Icon(
                        imageVector = if (detailsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = stringResource(R.string.profile_toggle_network_details)
                    )
                }
            }

            AnimatedVisibility(
                visible = detailsExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ProfileStatusChip(
                            label = stringResource(R.string.profile_role),
                            value = role,
                            modifier = Modifier.weight(1f)
                        )
                        ProfileStatusChip(
                            label = stringResource(R.string.profile_plan),
                            value = plan,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    ProfileStatusChip(
                        label = stringResource(R.string.profile_publisher_status),
                        value = publisherStatus,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
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
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 520.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.profile_edit_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.profile_edit_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                )
            }
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
}

@Composable
private fun ProfileAvatar(
    photoUrl: String?,
    avatarColor: Color,
    isUploading: Boolean
) {
    val avatarGradient = Brush.linearGradient(
        colors = listOf(
            avatarColor,
            avatarColor.copy(alpha = 0.72f),
            MaterialTheme.colorScheme.primary.copy(alpha = 0.34f)
        )
    )
    Surface(
        modifier = Modifier
            .size(112.dp)
            .clip(CircleShape)
            .border(
                width = 2.dp,
                color = BiblionGoldSoft.copy(alpha = 0.86f),
                shape = CircleShape
            ),
        shape = CircleShape,
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(avatarGradient),
            contentAlignment = Alignment.Center
        ) {
            if (!photoUrl.isNullOrBlank()) {
                RemoteProfileImage(
                    url = photoUrl,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier.size(118.dp),
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
            modifier = Modifier.size(112.dp),
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
private fun ProfileStatusChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.defaultMinSize(minHeight = 62.dp),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value.toDisplayStatus(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ProfileMetricsPanel(
    profile: BiblionUserProfile?,
    modifier: Modifier = Modifier
) {
    val metrics = profileMetrics(profile)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.profile_metrics_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.profile_metrics_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                )
            }
            val columns = 2
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                metrics.chunked(columns).forEach { rowMetrics ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowMetrics.forEach { metric ->
                            ProfileMetricCard(
                                label = metric.label,
                                value = metric.value,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        repeat(columns - rowMetrics.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
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
    ElevatedCard(
        modifier = modifier.defaultMinSize(minHeight = 82.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = BiblionBluePrimary.copy(alpha = 0.06f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = BiblionGoldPrimary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f)
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
            ProfileMetric(stringResource(R.string.profile_metric_downloads), profile?.totalDescargas ?: 0),
            ProfileMetric(stringResource(R.string.profile_metric_likes), profile?.totalLikes ?: 0),
            ProfileMetric(stringResource(R.string.profile_metric_followers), profile?.totalSeguidores ?: 0),
            ProfileMetric(stringResource(R.string.profile_metric_comments), profile?.totalComentarios ?: 0)
        )
    } else {
        listOf(
            ProfileMetric(stringResource(R.string.profile_metric_created), profile?.totalEnsenanzasCreadas ?: 0),
            ProfileMetric(stringResource(R.string.profile_metric_saved), profile?.totalGuardados ?: 0),
            ProfileMetric(stringResource(R.string.profile_metric_comments), profile?.totalComentarios ?: 0),
            ProfileMetric(stringResource(R.string.profile_metric_following), profile?.totalSiguiendo ?: 0),
            ProfileMetric(stringResource(R.string.profile_metric_downloaded), profile?.totalDescargas ?: 0),
            ProfileMetric(stringResource(R.string.profile_metric_followers), profile?.totalSeguidores ?: 0)
        )
    }
}

private fun String.toDisplayStatus(): String {
    return lowercase()
        .replace("_", " ")
        .split(" ")
        .filter { it.isNotBlank() }
        .joinToString(" ") { part ->
            part.replaceFirstChar { char -> char.uppercase() }
        }
}

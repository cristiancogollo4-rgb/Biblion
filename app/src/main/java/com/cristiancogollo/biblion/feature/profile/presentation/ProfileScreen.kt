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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.cristiancogollo.biblion.BiblionBottomNavigation
import com.cristiancogollo.biblion.BibleRepository
import com.cristiancogollo.biblion.BibleVersionDialog
import com.cristiancogollo.biblion.Screen
import com.cristiancogollo.biblion.Testament
import com.cristiancogollo.biblion.feature.studydocs.ui.Screen as StudyDocScreen
import com.cristiancogollo.biblion.AppPreferencesSyncStore
import com.cristiancogollo.biblion.ui.theme.BiblionBluePrimary
import com.cristiancogollo.biblion.ui.theme.BiblionGoldPrimary
import com.cristiancogollo.biblion.ui.theme.BiblionGoldSoft
import com.cristiancogollo.biblion.ui.theme.BiblionThemeMode
import com.cristiancogollo.biblion.feature.achievements.domain.AchievementEvent
import com.cristiancogollo.biblion.feature.achievements.tracking.AchievementTracker
import com.cristiancogollo.biblion.feature.achievements.data.AchievementRepository
import com.cristiancogollo.biblion.feature.achievements.presentation.ACHIEVEMENTS_ROUTE
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
    onClearSaveSuccess: () -> Unit,
    onSignOut: () -> Unit,
    onRestartTutorial: () -> Unit = {},
    themeMode: BiblionThemeMode = BiblionThemeMode.LIGHT,
    onThemeModeChange: (BiblionThemeMode) -> Unit = {},
    forceCompactLayout: Boolean = false,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val savedMessage = stringResource(R.string.profile_saved)
    val context = androidx.compose.ui.platform.LocalContext.current
    var showEditDialog by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showAboutBiblion by remember { mutableStateOf(false) }
    var selectedVersionKey by remember { mutableStateOf(BibleRepository.getSelectedVersionKey(context)) }
    var availableVersions by remember { mutableStateOf<List<BibleVersionOption>>(emptyList()) }
    val currentRoute by navController.currentBackStackEntryAsState()

    LaunchedEffect(Unit) {
        availableVersions = BibleRepository.getAvailableVersions(context)
        selectedVersionKey = BibleRepository.getSelectedVersionKey(context)
    }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            AchievementTracker.track(
                context,
                AchievementEvent.ProfileSaved(
                    hasName = uiState.nombres.isNotBlank(),
                    hasLastName = uiState.apellidos.isNotBlank(),
                    hasAlias = uiState.alias.isNotBlank(),
                ),
            )
            snackbarHostState.showSnackbar(savedMessage)
            onClearSaveSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Perfil", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
                navigationIcon = {},
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configuración",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            BiblionBottomNavigation(
                currentRoute = currentRoute?.destination?.route,
                onHome = { navController.navigateSingleTop(Screen.Home.route) },
                onBible = { navController.navigateSingleTop(Screen.Books.createRoute(Testament.OLD)) },
                onSearch = { navController.navigateSingleTop(Screen.Search.route) },
                onStudy = { navController.navigateSingleTop(StudyDocScreen.StudyDocsList.route) },
                onProfile = { navController.navigateSingleTop(Screen.Profile.route) }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 136.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (uiState.isLoading && uiState.profile == null) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(260.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                val isWide = !forceCompactLayout && LocalConfiguration.current.screenWidthDp >= 800
                if (isWide) {
                    Row(
                        modifier = Modifier.fillMaxWidth().widthIn(max = 1280.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier.weight(1.4f),
                            verticalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            ProfileIdentityPanel(
                                uiState = uiState,
                                onAvatarColorChange = onAvatarColorChange,
                                onProfilePhotoSelected = onProfilePhotoSelected,
                                onClearProfilePhoto = onClearProfilePhoto,
                                onEditClick = { showEditDialog = true },
                                onSignOut = onSignOut,
                                onRestartTutorial = onRestartTutorial
                            )

                            ProfileNetworkPanel(uiState = uiState)

                            ProfileActivityAndMetricsPanel(
                                profile = uiState.profile,
                                totalLocalEnsenanzas = uiState.totalLocalEnsenanzas
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            ProfileCompletionCard(uiState = uiState, onCompleteNow = { showEditDialog = true })

                            ProfileLogrosCard(
                                onSeeAll = { navController.navigateSingleTop(ACHIEVEMENTS_ROUTE) }
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth().widthIn(max = 640.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ProfileIdentityPanel(
                            uiState = uiState,
                            onAvatarColorChange = onAvatarColorChange,
                            onProfilePhotoSelected = onProfilePhotoSelected,
                            onClearProfilePhoto = onClearProfilePhoto,
                            onEditClick = { showEditDialog = true },
                            onSignOut = onSignOut,
                            onRestartTutorial = onRestartTutorial
                        )

                        ProfileCompletionCard(uiState = uiState, onCompleteNow = { showEditDialog = true })

                        ProfileNetworkPanel(uiState = uiState)

                        ProfileActivityAndMetricsPanel(
                            profile = uiState.profile,
                            totalLocalEnsenanzas = uiState.totalLocalEnsenanzas
                        )

                        ProfileLogrosCard(
                            onSeeAll = { navController.navigateSingleTop(ACHIEVEMENTS_ROUTE) }
                        )
                    }
                }

                Text(
                    text = "Conoce más sobre BIBLION",
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showAboutBiblion = true }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                    textDecoration = TextDecoration.Underline,
                    textAlign = TextAlign.Center
                )
            }
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

    LaunchedEffect(uiState.profile) {
        if (uiState.profile != null) {
            AchievementTracker.track(
                context,
                AchievementEvent.ProfileSaved(
                    hasName = uiState.nombres.isNotBlank(),
                    hasLastName = uiState.apellidos.isNotBlank(),
                    hasAlias = uiState.alias.isNotBlank(),
                ),
                notify = false,
                countAsMeaningfulUse = false,
            )
        }
    }

    if (showSettings) {
        ProfileSettingsSheet(
            selectedVersionKey = selectedVersionKey,
            availableVersions = availableVersions,
            fontSizeSp = AppPreferencesSyncStore.getReaderFontSizeSp(context).toFloat(),
            themeMode = themeMode,
            onDismiss = { showSettings = false },
            onVersionSelected = { selected ->
                BibleRepository.setSelectedVersionKey(context, selected.key)
                selectedVersionKey = selected.key
            },
            onFontSizeChange = { value -> AppPreferencesSyncStore.setReaderFontSizeSp(context, value.toInt()) },
            onThemeModeChange = onThemeModeChange
        )
    }

    if (showAboutBiblion) {
        AboutBiblionDialog(onDismiss = { showAboutBiblion = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileSettingsSheet(
    selectedVersionKey: String,
    availableVersions: List<BibleVersionOption>,
    fontSizeSp: Float,
    themeMode: BiblionThemeMode,
    onDismiss: () -> Unit,
    onVersionSelected: (BibleVersionOption) -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onThemeModeChange: (BiblionThemeMode) -> Unit
) {
    var showVersionDialog by remember { mutableStateOf(false) }
    var sliderValue by remember(fontSizeSp) { mutableStateOf(fontSizeSp.coerceIn(12f, 35f)) }
    val selectedVersion = availableVersions.firstOrNull { it.key == selectedVersionKey }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Configuración", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            ProfileSettingRow(
                icon = Icons.Default.MenuBook,
                title = "Versión de la Biblia",
                value = selectedVersion?.label ?: selectedVersionKey.uppercase(),
                onClick = {
                    showVersionDialog = true
                }
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Tamaño de texto", fontWeight = FontWeight.SemiBold)
                Text("${sliderValue.toInt()} sp", color = MaterialTheme.colorScheme.primary)
                Slider(
                    value = sliderValue,
                    onValueChange = {
                        sliderValue = it
                        onFontSizeChange(it)
                    },
                    valueRange = 12f..35f,
                    steps = 22
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("12 sp", style = MaterialTheme.typography.labelSmall)
                    Text("35 sp", style = MaterialTheme.typography.labelSmall)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Tema de la aplicación", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BiblionThemeMode.entries.forEach { mode ->
                        OutlinedButton(
                            onClick = { onThemeModeChange(mode) },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (themeMode == mode) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (themeMode == mode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(mode.displayName())
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }

    if (showVersionDialog) {
        BibleVersionDialog(
            versions = availableVersions,
            selectedVersionKey = selectedVersionKey,
            onVersionSelected = {
                onVersionSelected(it)
                showVersionDialog = false
            },
            onDismiss = { showVersionDialog = false }
        )
    }
}

@Composable
private fun ProfileSettingRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(40.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
            }
        }
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(">", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 20.sp)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

private fun BiblionThemeMode.displayName(): String = when (this) {
    BiblionThemeMode.LIGHT -> "Claro"
    BiblionThemeMode.BLUE -> "Azul"
    BiblionThemeMode.DARK -> "Oscuro"
}

@Composable
private fun ProfileIdentityPanel(
    uiState: ProfileUiState,
    onAvatarColorChange: (Long) -> Unit,
    onProfilePhotoSelected: (Uri) -> Unit,
    onClearProfilePhoto: () -> Unit,
    onEditClick: () -> Unit,
    onSignOut: () -> Unit,
    onRestartTutorial: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCompact = LocalConfiguration.current.screenWidthDp < 600
    val fullName = listOf(uiState.nombres, uiState.apellidos)
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .joinToString(" ")
        .ifBlank { uiState.alias.ifBlank { "Cristian Cogollo" } }

    val identityGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF0F2A4A), Color(0xFF1E40AF))
    )

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) onProfilePhotoSelected(uri)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(brush = identityGradient, shape = RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier.padding(
                start = if (isCompact) 16.dp else 24.dp,
                top = if (isCompact) 18.dp else 24.dp,
                end = if (isCompact) 16.dp else 24.dp,
                bottom = 36.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(if (isCompact) 12.dp else 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                ProfileAvatar(
                    photoUrl = uiState.profile?.fotoPerfil,
                    avatarColor = Color(uiState.avatarColor.toInt()),
                    isUploading = uiState.isUploadingAvatar
                )
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                    modifier = Modifier.size(32.dp).clickable { photoPicker.launch("image/*") }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AddAPhoto,
                            contentDescription = "Cambiar Foto",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = fullName,
                    fontSize = if (isCompact) 20.sp else 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "@${uiState.alias.ifBlank { "cristian_cogollo" }}",
                    fontSize = 14.sp,
                    color = Color(0xFF93C5FD),
                    fontWeight = FontWeight.Medium
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(14.dp))
                    Text(text = "Bucaramanga, Colombia", fontSize = 13.sp, color = Color(0xFF94A3B8))
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = uiState.biografia.ifBlank { "Apasionado por la Palabra de Dios y el discipulado. Me dedico al estudio biblico y la ensenanza." },
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                ProfileIdentityActions(
                    isCompact = isCompact,
                    onEditClick = onEditClick,
                    onSignOut = onSignOut
                )
            } 
        }

        Text(
            text = "Reiniciar tutorial de lectura",
            fontSize = 12.sp,
            color = BiblionGoldSoft,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 12.dp, end = 16.dp)
                .clickable { onRestartTutorial() }
        )
    }
}

@Composable
private fun ProfileIdentityActions(
    isCompact: Boolean,
    onEditClick: () -> Unit,
    onSignOut: () -> Unit
) {
    val actionModifier = if (isCompact) Modifier.fillMaxWidth() else Modifier
    val contentPadding = PaddingValues(
        horizontal = if (isCompact) 10.dp else 16.dp,
        vertical = 6.dp
    )

    @Composable
    fun EditButton() {
        OutlinedButton(
            onClick = onEditClick,
            modifier = actionModifier,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            contentPadding = contentPadding
        ) {
            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Editar perfil",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }

    @Composable
    fun SignOutButton() {
        OutlinedButton(
            onClick = onSignOut,
            modifier = actionModifier,
            border = BorderStroke(1.dp, BiblionGoldSoft.copy(alpha = 0.8f)),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = BiblionGoldSoft),
            contentPadding = contentPadding
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Logout,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.sign_out),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }

    if (isCompact) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EditButton()
            SignOutButton()
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EditButton()
            SignOutButton()
        }
    }
}

@Composable
private fun ProfileNetworkPanel(
    uiState: ProfileUiState,
    modifier: Modifier = Modifier
) {
    val profile = uiState.profile
    val role = profile?.rol ?: "Lector"
    val plan = profile?.plan ?: "Free"
    val isWide = LocalConfiguration.current.screenWidthDp >= 800

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        if (isWide) {
            Row(
                modifier = Modifier.padding(20.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = Color(0xFFEFF6FF), modifier = Modifier.size(40.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Badge, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Column {
                        Text("Rol", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                        Text(role.toDisplayStatus(), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                        Text("Explora y estudia la Biblia", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = Color(0xFFFFFBEB), modifier = Modifier.size(40.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFD97706))
                        }
                    }
                    Column {
                        Text("Plan", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                        Text(plan, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                        Text("Plan gratuito", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = Color(0xFFF1F5F9), modifier = Modifier.size(40.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Explore, contentDescription = null, tint = Color(0xFF475569))
                        }
                    }
                    Column {
                        Text("Perfil publico", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                        Text("Privado", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                        Text("Solo tu puedes ver tu perfil", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(shape = CircleShape, color = Color(0xFFEFF6FF), modifier = Modifier.size(40.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Badge, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Column {
                        Text("Rol", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                        Text(role.toDisplayStatus(), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                        Text("Explora y estudia la Biblia", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(shape = CircleShape, color = Color(0xFFFFFBEB), modifier = Modifier.size(40.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFD97706))
                        }
                    }
                    Column {
                        Text("Plan", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                        Text(plan, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                        Text("Plan gratuito", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(shape = CircleShape, color = Color(0xFFF1F5F9), modifier = Modifier.size(40.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Explore, contentDescription = null, tint = Color(0xFF475569))
                        }
                    }
                    Column {
                        Text("Perfil publico", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                        Text("Privado", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                        Text("Solo tu puedes ver tu perfil", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileActivityAndMetricsPanel(
    profile: BiblionUserProfile?,
    totalLocalEnsenanzas: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text("Tu actividad", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)

            val metricList = if (profile != null) {
                val role = profile.rol.uppercase()
                val approved = profile.estadoPublicador.uppercase() == "APROBADO"
                val creadasCount = maxOf(profile.totalEnsenanzasCreadas, totalLocalEnsenanzas)
                if (role == "PUBLICADOR" || role == "ADMIN" || approved) {
                    listOf(
                        Triple(creadasCount, "Creadas", Color(0xFFEFF6FF)),
                        Triple(profile.totalEnsenanzasPublicadas, "Publicadas", Color(0xFFF0FDF4)),
                        Triple(profile.totalDescargas, "Descargas", Color(0xFFF5F3FF)),
                        Triple(profile.totalLikes, "Likes", Color(0xFFFFF7ED)),
                        Triple(profile.totalComentarios, "Comentarios", Color(0xFFECFDF5)),
                        Triple(profile.totalSeguidores, "Seguidores", Color(0xFFFDF2F8))
                    )
                } else {
                    listOf(
                        Triple(creadasCount, "Creadas", Color(0xFFEFF6FF)),
                        Triple(profile.totalGuardados, "Guardados", Color(0xFFF0FDF4)),
                        Triple(profile.totalComentarios, "Comentarios", Color(0xFFF5F3FF)),
                        Triple(profile.totalSiguiendo, "Siguiendo", Color(0xFFFFF7ED)),
                        Triple(profile.totalDescargas, "Descargas", Color(0xFFECFDF5)),
                        Triple(profile.totalSeguidores, "Seguidores", Color(0xFFFDF2F8))
                    )
                }
            } else {
                listOf(
                    Triple(totalLocalEnsenanzas, "Creadas", Color(0xFFEFF6FF)),
                    Triple(0, "Guardados", Color(0xFFF0FDF4)),
                    Triple(0, "Comentarios", Color(0xFFF5F3FF)),
                    Triple(0, "Siguiendo", Color(0xFFFFF7ED)),
                    Triple(0, "Descargas", Color(0xFFECFDF5)),
                    Triple(0, "Seguidores", Color(0xFFFDF2F8))
                )
            }

            val columns = 2
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                metricList.chunked(columns).forEach { rowMetrics ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowMetrics.forEach { (value, label, bgColor) ->
                            Surface(
                                modifier = Modifier.weight(1f).height(64.dp),
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(bgColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val emoji = when (label.lowercase()) {
                                            "creadas" -> "✍️"
                                            "publicadas" -> "📢"
                                            "descargas" -> "📥"
                                            "likes" -> "❤️"
                                            "comentarios" -> "💬"
                                            "seguidores" -> "👥"
                                            "guardados" -> "💾"
                                            "siguiendo" -> "👤"
                                            else -> "📊"
                                        }
                                        Text(text = emoji, fontSize = 16.sp)
                                    }
                                    Column {
                                        Text(text = value.toString(), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
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
private fun ProfileCompletionCard(uiState: ProfileUiState, onCompleteNow: () -> Unit) {
    val completed = listOf(
        uiState.nombres.isNotBlank(),
        uiState.apellidos.isNotBlank(),
        uiState.alias.isNotBlank(),
        uiState.biografia.isNotBlank(),
        uiState.profile?.fotoPerfil != null
    )
    val completedCount = completed.count { it }
    val percent = (completedCount * 100) / 5
    val remainingSteps = 5 - completedCount

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(64.dp)) {
                CircularProgressIndicator(
                    progress = { percent / 100f },
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFB45309),
                    strokeWidth = 6.dp,
                    trackColor = Color(0xFFFEF3C7)
                )
                Text("$percent%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Completa tu perfil", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    if (remainingSteps > 0) "Te faltan $remainingSteps pasos para completar tu perfil y desbloquear todas las funciones."
                    else "!Perfil completo! Disfruta de todas las funciones.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 4.dp)) {
                    val steps = listOf(
                        "Nombres" to completed[0],
                        "Apellidos" to completed[1],
                        "Alias" to completed[2],
                        "Biografia" to completed[3],
                        "Foto de perfil" to completed[4]
                    )
                    steps.forEach { (label, done) ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                imageVector = if (done) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (done) Color(0xFF16A34A) else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(label, fontSize = 12.sp, color = if (done) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                if (remainingSteps > 0) {
                    Button(
                        onClick = onCompleteNow,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEF3C7), contentColor = Color(0xFFB45309)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Completar ahora", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileLogrosCard(onSeeAll: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repository = remember(context) { AchievementRepository(context) }
    val achievements by repository.observeProgress().collectAsState(initial = emptyList())
    val featured = achievements
        .sortedWith(
            compareByDescending<com.cristiancogollo.biblion.feature.achievements.domain.AchievementProgress> {
                it.isUnlocked
            }.thenByDescending { it.normalizedProgress }
        )
        .take(3)
    val unlockedCount = achievements.count { it.isUnlocked }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Logros", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text("$unlockedCount de 24", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    "Ver todos",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable(onClick = onSeeAll),
                )
            }

            featured.forEach { progress ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(progress.definition.icon, fontSize = 24.sp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(progress.definition.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            if (progress.isUnlocked) "Desbloqueado" else "${progress.currentValue} de ${progress.definition.target}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
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
    Surface(
        modifier = Modifier
            .size(96.dp)
            .clip(CircleShape)
            .border(width = 2.dp, color = Color(0xFFFEF3C7), shape = CircleShape),
        shape = CircleShape,
        color = avatarColor
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (!photoUrl.isNullOrBlank()) {
                RemoteProfileImage(url = photoUrl, modifier = Modifier.fillMaxSize())
            } else {
                Text("C", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
private fun RemoteProfileImage(url: String, modifier: Modifier = Modifier) {
    var imageBitmap by remember(url) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    LaunchedEffect(url) {
        imageBitmap = withContext(Dispatchers.IO) {
            runCatching {
                URL(url).openStream().use { stream -> BitmapFactory.decodeStream(stream)?.asImageBitmap() }
            }.getOrNull()
        }
    }
    imageBitmap?.let {
        Image(bitmap = it, contentDescription = null, modifier = modifier, contentScale = ContentScale.Crop)
    }
}

// ---- Complete Profile Dialog (used from AppNavigation) ----

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

// ---- Edit Dialog (preserved from previous implementation) ----

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
                ProfileEditIdentityPanel(
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
private fun ProfileEditIdentityPanel(
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
                ProfileEditAvatar(
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
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
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
    val accentColor = if (onDarkSurface) BiblionGoldSoft else MaterialTheme.colorScheme.primary
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
private fun ProfileEditAvatar(
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
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    cursorColor = MaterialTheme.colorScheme.primary
)

private fun String.toDisplayStatus(): String {
    return lowercase()
        .replace("_", " ")
        .split(" ")
        .filter { it.isNotBlank() }
        .joinToString(" ") { part ->
            part.replaceFirstChar { char -> char.uppercase() }
        }
}

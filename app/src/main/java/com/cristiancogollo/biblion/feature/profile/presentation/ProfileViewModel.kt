package com.cristiancogollo.biblion

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val currentUser: AuthUser? = null,
    val profile: BiblionUserProfile? = null,
    val nombres: String = "",
    val apellidos: String = "",
    val alias: String = "",
    val biografia: String = "",
    val avatarColor: Long = 0xFF0B6E9FUL.toLong(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isUploadingAvatar: Boolean = false,
    val errorMessage: String? = null,
    val saveSuccess: Boolean = false,
    val completionDismissedForUid: String? = null
) {
    val isAuthenticated: Boolean
        get() = currentUser != null

    val requiresCompletion: Boolean
        get() = currentUser != null &&
            profile != null &&
            !profile.isComplete &&
            completionDismissedForUid != currentUser.uid
}

class ProfileViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: UserProfileRepository = FirestoreUserProfileRepository()
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    private var profileJob: Job? = null

    fun setCurrentUser(user: AuthUser?) {
        val currentUid = _state.value.currentUser?.uid
        if (currentUid == user?.uid) return

        profileJob?.cancel()
        _state.value = ProfileUiState(
            currentUser = user,
            nombres = user?.displayName?.substringBefore(" ").orEmpty(),
            apellidos = user?.displayName?.substringAfter(" ", "").orEmpty(),
            alias = user?.displayName?.takeIf { it.isNotBlank() }
                ?: user?.email?.substringBefore("@").orEmpty()
        )

        if (user == null) return

        profileJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            repository.observeProfile(user.uid).collect { profile ->
                _state.update { current ->
                    val resolvedProfile = profile ?: BiblionUserProfile(
                        uid = user.uid,
                        correo = user.email.orEmpty(),
                        alias = user.displayName?.takeIf { it.isNotBlank() }
                            ?: user.email?.substringBefore("@").orEmpty(),
                        fotoPerfil = user.photoUrl
                    )
                    current.copy(
                        profile = resolvedProfile,
                        nombres = resolvedProfile.nombres.ifBlank { current.nombres },
                        apellidos = resolvedProfile.apellidos.ifBlank { current.apellidos },
                        alias = resolvedProfile.alias.ifBlank { current.alias },
                        biografia = resolvedProfile.biografia,
                        avatarColor = resolvedProfile.avatarColor,
                        isLoading = false,
                        errorMessage = null
                    )
                }
            }
        }
    }

    fun updateNombres(value: String) {
        _state.update { it.copy(nombres = value, errorMessage = null, saveSuccess = false) }
    }

    fun updateApellidos(value: String) {
        _state.update { it.copy(apellidos = value, errorMessage = null, saveSuccess = false) }
    }

    fun updateAlias(value: String) {
        _state.update { it.copy(alias = value, errorMessage = null, saveSuccess = false) }
    }

    fun updateBiografia(value: String) {
        _state.update { it.copy(biografia = value, errorMessage = null, saveSuccess = false) }
    }

    fun updateAvatarColor(value: Long) {
        val uid = _state.value.currentUser?.uid ?: return
        _state.update { it.copy(avatarColor = value, errorMessage = null, saveSuccess = false) }
        viewModelScope.launch {
            runCatching { repository.saveAvatarColor(uid, value) }
                .onFailure { throwable ->
                    _state.update {
                        it.copy(errorMessage = throwable.message ?: "No se pudo guardar el color del avatar.")
                    }
                }
        }
    }

    fun uploadProfilePhoto(context: Context, imageUri: Uri) {
        val uid = _state.value.currentUser?.uid ?: return
        viewModelScope.launch {
            _state.update { it.copy(isUploadingAvatar = true, errorMessage = null, saveSuccess = false) }
            runCatching {
                repository.uploadProfilePhoto(context.applicationContext, uid, imageUri)
            }.onSuccess { photoUrl ->
                _state.update { current ->
                    val baseProfile = current.profile ?: BiblionUserProfile(
                        uid = uid,
                        correo = current.currentUser?.email.orEmpty(),
                        nombres = current.nombres,
                        apellidos = current.apellidos,
                        alias = current.alias,
                        avatarColor = current.avatarColor
                    )
                    current.copy(
                        isUploadingAvatar = false,
                        profile = baseProfile.copy(fotoPerfil = photoUrl)
                    )
                }
            }.onFailure { throwable ->
                _state.update {
                    it.copy(
                        isUploadingAvatar = false,
                        errorMessage = throwable.localizedMessage ?: "No se pudo subir la foto de perfil."
                    )
                }
            }
        }
    }

    fun clearProfilePhoto() {
        val uid = _state.value.currentUser?.uid ?: return
        viewModelScope.launch {
            _state.update { it.copy(isUploadingAvatar = true, errorMessage = null, saveSuccess = false) }
            runCatching { repository.clearProfilePhoto(uid) }
                .onSuccess {
                    _state.update { current ->
                        val baseProfile = current.profile ?: BiblionUserProfile(
                            uid = uid,
                            correo = current.currentUser?.email.orEmpty(),
                            nombres = current.nombres,
                            apellidos = current.apellidos,
                            alias = current.alias,
                            avatarColor = current.avatarColor
                        )
                        current.copy(
                            isUploadingAvatar = false,
                            profile = baseProfile.copy(fotoPerfil = null)
                        )
                    }
                }
                .onFailure { throwable ->
                    _state.update {
                        it.copy(
                            isUploadingAvatar = false,
                            errorMessage = throwable.message ?: "No se pudo quitar la foto de perfil."
                        )
                    }
                }
        }
    }

    fun dismissCompletionPrompt() {
        val uid = _state.value.currentUser?.uid ?: return
        _state.update { it.copy(completionDismissedForUid = uid) }
    }

    fun saveProfile(onSaved: () -> Unit = {}) {
        val current = _state.value
        val uid = current.currentUser?.uid ?: return
        val validationError = validateProfile(current)
        if (validationError != null) {
            _state.update { it.copy(errorMessage = validationError) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, errorMessage = null, saveSuccess = false) }
            runCatching {
                repository.saveProfile(
                    uid = uid,
                    nombres = current.nombres,
                    apellidos = current.apellidos,
                    alias = current.alias,
                    biografia = current.biografia
                )
            }.onSuccess {
                _state.update {
                    it.copy(
                        isSaving = false,
                        saveSuccess = true,
                        completionDismissedForUid = uid
                    )
                }
                onSaved()
            }.onFailure { throwable ->
                _state.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = throwable.message ?: "No se pudo guardar el perfil."
                    )
                }
            }
        }
    }

    fun clearSaveSuccess() {
        _state.update { it.copy(saveSuccess = false) }
    }

    private fun validateProfile(state: ProfileUiState): String? {
        val res = getApplication<Application>().resources
        return when {
            state.nombres.isBlank() -> res.getString(R.string.profile_error_names)
            state.apellidos.isBlank() -> res.getString(R.string.profile_error_last_names)
            state.alias.isBlank() -> res.getString(R.string.profile_error_alias)
            else -> null
        }
    }
}

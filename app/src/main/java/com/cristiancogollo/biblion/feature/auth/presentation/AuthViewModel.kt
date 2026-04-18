package com.cristiancogollo.biblion

import android.app.Application
import android.util.Patterns
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    @StringRes val errorMessageRes: Int? = null,
    val currentUser: AuthUser? = null,
    val showSignedOutDialog: Boolean = false
) {
    val isAuthenticated: Boolean
        get() = currentUser != null
}

sealed interface AuthIntent {
    data class UpdateEmail(val value: String) : AuthIntent
    data class UpdatePassword(val value: String) : AuthIntent
    data class UpdateConfirmPassword(val value: String) : AuthIntent
    data object SignIn : AuthIntent
    data object Register : AuthIntent
    data object SignOut : AuthIntent
    data object ClearError : AuthIntent
    data object DismissSignedOutDialog : AuthIntent
}

sealed interface AuthEffect {
    data object NavigateHome : AuthEffect
    data object NavigateLogin : AuthEffect
}

class AuthViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: AuthRepository = FirebaseAuthRepository()
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(AuthUiState(currentUser = repository.currentUser()))
    val state: StateFlow<AuthUiState> = _state.asStateFlow()
    private val _effects = MutableSharedFlow<AuthEffect>()
    val effects: SharedFlow<AuthEffect> = _effects.asSharedFlow()

    fun process(intent: AuthIntent) {
        when (intent) {
            is AuthIntent.UpdateEmail -> {
                _state.update {
                    it.copy(
                        email = intent.value,
                        errorMessageRes = null
                    )
                }
            }

            is AuthIntent.UpdatePassword -> {
                _state.update {
                    it.copy(
                        password = intent.value,
                        errorMessageRes = null
                    )
                }
            }

            is AuthIntent.UpdateConfirmPassword -> {
                _state.update {
                    it.copy(
                        confirmPassword = intent.value,
                        errorMessageRes = null
                    )
                }
            }

            AuthIntent.SignIn -> signIn()
            AuthIntent.Register -> register()
            AuthIntent.SignOut -> {
                repository.signOut()
                _state.update {
                    it.copy(
                        currentUser = null,
                        password = "",
                        confirmPassword = "",
                        errorMessageRes = null,
                        isLoading = false,
                        showSignedOutDialog = true
                    )
                }
            }

            AuthIntent.ClearError -> {
                _state.update { it.copy(errorMessageRes = null) }
            }

            AuthIntent.DismissSignedOutDialog -> {
                _state.update { it.copy(showSignedOutDialog = false) }
            }
        }
    }

    private fun signIn() {
        val current = _state.value
        if (current.isLoading) return

        val email = current.email.trim()
        val password = current.password

        val validationError = validateEmail(email) ?: validatePassword(password)
        if (validationError != null) {
            _state.update { it.copy(errorMessageRes = validationError) }
            return
        }

        _state.update { it.copy(isLoading = true, errorMessageRes = null, email = email) }

        viewModelScope.launch {
            repository.signIn(email, password)
                .onSuccess { user ->
                    _state.update {
                        it.copy(
                            currentUser = user,
                            isLoading = false,
                            errorMessageRes = null,
                            password = "",
                            confirmPassword = "",
                            showSignedOutDialog = false
                        )
                    }
                    _effects.emit(AuthEffect.NavigateHome)
                }
                .onFailure { throwable ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessageRes = mapFirebaseError(throwable)
                        )
                    }
                }
        }
    }

    private fun register() {
        val current = _state.value
        if (current.isLoading) return

        val email = current.email.trim()
        val password = current.password
        val confirmPassword = current.confirmPassword

        val validationError = validateEmail(email)
            ?: validatePassword(password)
            ?: validatePasswordConfirmation(password, confirmPassword)

        if (validationError != null) {
            _state.update { it.copy(errorMessageRes = validationError) }
            return
        }

        _state.update { it.copy(isLoading = true, errorMessageRes = null, email = email) }

        viewModelScope.launch {
            repository.register(email, password)
                .onSuccess {
                    repository.signOut()
                    _state.update {
                        it.copy(
                            currentUser = null,
                            password = "",
                            confirmPassword = "",
                            isLoading = false,
                            errorMessageRes = null,
                            showSignedOutDialog = false
                        )
                    }
                    _effects.emit(AuthEffect.NavigateLogin)
                }
                .onFailure { throwable ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessageRes = mapFirebaseError(throwable)
                        )
            }
        }
    }
    }

    @StringRes
    private fun validateEmail(email: String): Int? {
        return when {
            email.isBlank() -> R.string.auth_error_empty_email
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> R.string.auth_error_invalid_email
            else -> null
        }
    }

    @StringRes
    private fun validatePassword(password: String): Int? {
        return when {
            password.isBlank() -> R.string.auth_error_empty_password
            password.length < 6 -> R.string.auth_error_short_password
            else -> null
        }
    }

    @StringRes
    private fun validatePasswordConfirmation(password: String, confirmPassword: String): Int? {
        return when {
            confirmPassword.isBlank() -> R.string.auth_error_empty_password
            password != confirmPassword -> R.string.auth_error_password_mismatch
            else -> null
        }
    }

    @StringRes
    private fun mapFirebaseError(throwable: Throwable): Int {
        return when (throwable) {
            is FirebaseAuthUserCollisionException -> R.string.auth_error_account_exists
            is FirebaseAuthWeakPasswordException -> R.string.auth_error_short_password
            is FirebaseAuthInvalidCredentialsException -> R.string.auth_error_sign_in_failed
            is FirebaseAuthInvalidUserException -> R.string.auth_error_sign_in_failed
            is FirebaseNetworkException -> R.string.auth_error_network
            else -> R.string.auth_error_generic
        }
    }
}

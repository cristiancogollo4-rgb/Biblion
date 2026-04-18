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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    @StringRes val errorMessageRes: Int? = null,
    val currentUser: AuthUser? = null
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
}

class AuthViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: AuthRepository = FirebaseAuthRepository()
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(AuthUiState(currentUser = repository.currentUser()))
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    init {
        observeAuthState()
    }

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
                        password = "",
                        confirmPassword = "",
                        errorMessageRes = null,
                        isLoading = false
                    )
                }
            }

            AuthIntent.ClearError -> {
                _state.update { it.copy(errorMessageRes = null) }
            }
        }
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            repository.authState.collect { user ->
                _state.update { current ->
                    val authUserChanged = current.currentUser?.uid != user?.uid
                    current.copy(
                        currentUser = user,
                        isLoading = false,
                        errorMessageRes = if (authUserChanged) null else current.errorMessageRes,
                        password = if (authUserChanged) "" else current.password,
                        confirmPassword = if (authUserChanged) "" else current.confirmPassword
                    )
                }
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

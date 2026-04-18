package com.cristiancogollo.biblion

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AuthViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun sign_in_with_blank_email_sets_validation_error() = runTest {
        val viewModel = buildViewModel()

        viewModel.process(AuthIntent.UpdatePassword("secret123"))
        viewModel.process(AuthIntent.SignIn)
        advanceUntilIdle()

        assertEquals(R.string.auth_error_empty_email, viewModel.state.value.errorMessageRes)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun register_with_mismatched_passwords_sets_validation_error() = runTest {
        val viewModel = buildViewModel()

        viewModel.process(AuthIntent.UpdateEmail("reader@biblion.app"))
        viewModel.process(AuthIntent.UpdatePassword("secret123"))
        viewModel.process(AuthIntent.UpdateConfirmPassword("different123"))
        viewModel.process(AuthIntent.Register)
        advanceUntilIdle()

        assertEquals(R.string.auth_error_password_mismatch, viewModel.state.value.errorMessageRes)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun successful_sign_in_updates_authenticated_user() = runTest {
        val fakeRepository = FakeAuthRepository()
        val viewModel = buildViewModel(fakeRepository)

        viewModel.process(AuthIntent.UpdateEmail("reader@biblion.app"))
        viewModel.process(AuthIntent.UpdatePassword("secret123"))
        viewModel.process(AuthIntent.SignIn)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.isAuthenticated)
        assertEquals("reader@biblion.app", viewModel.state.value.currentUser?.email)
        assertEquals("", viewModel.state.value.password)
        assertEquals(null, viewModel.state.value.errorMessageRes)
    }

    @Test
    fun sign_out_clears_authenticated_user_immediately() = runTest {
        val fakeRepository = FakeAuthRepository(
            initialUser = AuthUser(
                uid = "uid-reader",
                email = "reader@biblion.app"
            )
        )
        val viewModel = buildViewModel(fakeRepository)

        advanceUntilIdle()
        viewModel.process(AuthIntent.SignOut)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isAuthenticated)
        assertEquals(null, viewModel.state.value.currentUser)
    }

    @Test
    fun google_sign_in_success_updates_authenticated_user() = runTest {
        val fakeRepository = FakeAuthRepository()
        val viewModel = buildViewModel(fakeRepository)

        viewModel.beginGoogleSignIn()
        viewModel.signInWithGoogleIdToken("google-token")
        advanceUntilIdle()

        assertTrue(viewModel.state.value.isAuthenticated)
        assertEquals("google.user@biblion.app", viewModel.state.value.currentUser?.email)
        assertFalse(viewModel.state.value.isLoading)
        assertEquals(null, viewModel.state.value.errorMessageRes)
    }

    private fun buildViewModel(repository: AuthRepository = FakeAuthRepository()): AuthViewModel {
        val application = ApplicationProvider.getApplicationContext<Application>()
        return AuthViewModel(
            application = application,
            repository = repository
        )
    }
}

private class FakeAuthRepository(
    initialUser: AuthUser? = null
) : AuthRepository {
    private val authStateFlow = MutableStateFlow(initialUser)

    override val authState: Flow<AuthUser?> = authStateFlow

    override fun currentUser(): AuthUser? = authStateFlow.value

    override suspend fun signIn(email: String, password: String): Result<AuthUser> {
        val user = AuthUser(
            uid = "uid-$email",
            email = email
        )
        authStateFlow.value = user
        return Result.success(user)
    }

    override suspend fun signInWithGoogle(idToken: String): Result<AuthUser> {
        val user = AuthUser(
            uid = "uid-$idToken",
            email = "google.user@biblion.app"
        )
        authStateFlow.value = user
        return Result.success(user)
    }

    override suspend fun register(email: String, password: String): Result<AuthUser> {
        val user = AuthUser(
            uid = "uid-$email",
            email = email
        )
        authStateFlow.value = user
        return Result.success(user)
    }

    override fun signOut() {
        authStateFlow.value = null
    }
}

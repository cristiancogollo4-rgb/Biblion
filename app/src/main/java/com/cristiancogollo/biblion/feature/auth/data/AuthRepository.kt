package com.cristiancogollo.biblion

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class AuthUser(
    val uid: String,
    val email: String?
)

interface AuthRepository {
    val authState: Flow<AuthUser?>

    fun currentUser(): AuthUser?

    suspend fun signIn(email: String, password: String): Result<AuthUser>

    suspend fun signInWithGoogle(idToken: String): Result<AuthUser>

    suspend fun register(email: String, password: String): Result<AuthUser>

    fun signOut()
}

class FirebaseAuthRepository(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) : AuthRepository {

    override val authState: Flow<AuthUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser?.toAuthUser())
        }

        firebaseAuth.addAuthStateListener(listener)
        trySend(firebaseAuth.currentUser?.toAuthUser())

        awaitClose {
            firebaseAuth.removeAuthStateListener(listener)
        }
    }

    override fun currentUser(): AuthUser? = firebaseAuth.currentUser?.toAuthUser()

    override suspend fun signIn(email: String, password: String): Result<AuthUser> {
        return runCatching {
            val result = firebaseAuth
                .signInWithEmailAndPassword(email, password)
                .awaitValue()

            result.user?.toAuthUser()
                ?: error("Firebase sign-in completed without a user.")
        }
    }

    override suspend fun signInWithGoogle(idToken: String): Result<AuthUser> {
        return runCatching {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = firebaseAuth
                .signInWithCredential(credential)
                .awaitValue()

            result.user?.toAuthUser()
                ?: error("Firebase Google sign-in completed without a user.")
        }
    }

    override suspend fun register(email: String, password: String): Result<AuthUser> {
        return runCatching {
            val result = firebaseAuth
                .createUserWithEmailAndPassword(email, password)
                .awaitValue()

            result.user?.toAuthUser()
                ?: error("Firebase registration completed without a user.")
        }
    }

    override fun signOut() {
        firebaseAuth.signOut()
    }
}

private fun FirebaseUser.toAuthUser(): AuthUser {
    return AuthUser(
        uid = uid,
        email = email
    )
}

private suspend fun Task<AuthResult>.awaitValue(): AuthResult {
    return suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val result = task.result
                if (result != null) {
                    continuation.resume(result)
                } else {
                    continuation.resumeWithException(
                        IllegalStateException("Firebase task completed without a result.")
                    )
                }
            } else {
                continuation.resumeWithException(
                    task.exception ?: IllegalStateException("Firebase task failed.")
                )
            }
        }
    }
}

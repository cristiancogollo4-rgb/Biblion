package com.cristiancogollo.biblion.feature.auth.data

import android.app.Activity
import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.cristiancogollo.biblion.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

sealed interface GoogleCredentialsResult {
    data class Success(val idToken: String) : GoogleCredentialsResult
    data object Cancelled : GoogleCredentialsResult
    data class Failure(val throwable: Throwable) : GoogleCredentialsResult
}

class GoogleCredentialsAuth(
    private val context: Context,
    private val credentialManager: CredentialManager = CredentialManager.create(context)
) {

    suspend fun requestIdToken(activity: Activity): GoogleCredentialsResult {
        val webClientId = context.getString(R.string.default_web_client_id)
        if (webClientId.isBlank()) {
            return GoogleCredentialsResult.Failure(
                IllegalStateException("default_web_client_id is missing. Check google-services.json")
            )
        }

        return try {
            requestGoogleCredential(activity, webClientId, filterAuthorizedAccounts = true)
        } catch (e: NoCredentialException) {
            try {
                requestGoogleCredential(activity, webClientId, filterAuthorizedAccounts = false)
            } catch (e: GetCredentialCancellationException) {
                GoogleCredentialsResult.Cancelled
            } catch (e: Exception) {
                GoogleCredentialsResult.Failure(e)
            }
        } catch (e: GetCredentialCancellationException) {
            GoogleCredentialsResult.Cancelled
        } catch (e: Exception) {
            GoogleCredentialsResult.Failure(e)
        }
    }

    suspend fun clearCredentialState() {
        runCatching {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        }
    }

    private suspend fun requestGoogleCredential(
        activity: Activity,
        webClientId: String,
        filterAuthorizedAccounts: Boolean
    ): GoogleCredentialsResult {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(webClientId)
            .setFilterByAuthorizedAccounts(filterAuthorizedAccounts)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val result = credentialManager.getCredential(
            context = activity,
            request = request
        )

        val credential = result.credential
        return if (
            credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
            GoogleCredentialsResult.Success(googleCredential.idToken)
        } else {
            GoogleCredentialsResult.Failure(
                IllegalStateException("Unsupported Google credential type.")
            )
        }
    }
}

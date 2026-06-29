package com.bakeflow.app.auth.util

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

data class GoogleSignInResult(
    val idToken: String,
    val displayName: String?
)

class GoogleSignInCancelledException : Exception("Sign in cancelled.")

object GoogleAuthHelper {

    suspend fun signIn(context: Context, serverClientId: String): Result<GoogleSignInResult> {
        val credentialManager = CredentialManager.create(context)
        return try {
            parseGoogleIdToken(
                requestGoogleIdCredential(
                    credentialManager = credentialManager,
                    context = context,
                    serverClientId = serverClientId,
                    filterAuthorizedAccounts = true
                )
            )
        } catch (e: NoCredentialException) {
            try {
                parseGoogleIdToken(
                    requestGoogleIdCredential(
                        credentialManager = credentialManager,
                        context = context,
                        serverClientId = serverClientId,
                        filterAuthorizedAccounts = false
                    )
                )
            } catch (retry: Exception) {
                Result.failure(mapException(retry))
            }
        } catch (e: Exception) {
            Result.failure(mapException(e))
        }
    }

    private suspend fun requestGoogleIdCredential(
        credentialManager: CredentialManager,
        context: Context,
        serverClientId: String,
        filterAuthorizedAccounts: Boolean
    ): GetCredentialResponse {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(serverClientId)
            .setFilterByAuthorizedAccounts(filterAuthorizedAccounts)
            .setAutoSelectEnabled(true)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
        return credentialManager.getCredential(context, request)
    }

    private fun parseGoogleIdToken(response: GetCredentialResponse): Result<GoogleSignInResult> {
        val credential = response.credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
            return Result.success(
                GoogleSignInResult(
                    idToken = googleCredential.idToken,
                    displayName = googleCredential.displayName
                )
            )
        }
        return Result.failure(IllegalStateException("Unexpected Google credential type."))
    }

    private fun mapException(throwable: Throwable): Exception = when (throwable) {
        is GoogleSignInCancelledException -> throwable
        is GetCredentialCancellationException -> GoogleSignInCancelledException()
        is GetCredentialException -> Exception(
            throwable.localizedMessage ?: "Google sign in failed. Please try again.",
            throwable
        )
        else -> throwable as? Exception ?: Exception(
            throwable.localizedMessage ?: "Google sign in failed. Please try again.",
            throwable
        )
    }
}

package com.dv.moneym.core.oauth

import android.app.Activity
import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.dv.moneym.core.common.DispatcherProvider
import com.dv.moneym.core.datastore.AppSettings
import com.dv.moneym.core.datastore.PrefKeys
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AndroidGoogleAuthManager(
    private val context: Context,
    private val config: GoogleOAuthConfig,
    private val appSettings: AppSettings,
    private val dispatchers: DispatcherProvider,
) : GoogleAuthManager {

    private val _state = MutableStateFlow<AuthState>(
        appSettings.getString(PrefKeys.REMOTE_BACKUP_ACCOUNT_EMAIL)
            ?.takeIf { it.isNotBlank() }
            ?.let { AuthState.SignedIn(it) }
            ?: AuthState.SignedOut,
    )

    override val isConfigured: Boolean = config.isConfigured
    override val state: StateFlow<AuthState> = _state.asStateFlow()

    private val authorizationRequest: AuthorizationRequest by lazy {
        AuthorizationRequest.builder()
            .setRequestedScopes(config.scopes.map { Scope(it) })
            .build()
    }

    override suspend fun signIn(): Result<AuthState.SignedIn> = runCatching {
        if (!isConfigured) throw GoogleAuthError.NotConfigured()
        val serverClientId = config.serverClientId
            ?: throw GoogleAuthError.NotConfigured()
        val activity = GoogleAuthActivityBridge.activity
            ?: throw GoogleAuthError.Platform("No foreground activity for sign-in")

        val email = credentialSignIn(activity, serverClientId)
        authorize(activity)
        appSettings.putString(PrefKeys.REMOTE_BACKUP_ACCOUNT_EMAIL, email ?: "")
        val signedIn = AuthState.SignedIn(email)
        _state.value = signedIn
        signedIn
    }

    override suspend fun signOut() = withContext(dispatchers.io) {
        runCatching {
            CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
        }
        runCatching {
            GoogleSignIn.getClient(context, GoogleSignInOptions.DEFAULT_SIGN_IN).signOut().await()
        }
        appSettings.remove(PrefKeys.REMOTE_BACKUP_ACCOUNT_EMAIL)
        _state.value = AuthState.SignedOut
    }

    override suspend fun accessToken(): String? = withContext(dispatchers.io) {
        if (!isConfigured) return@withContext null
        if (_state.value is AuthState.SignedOut) return@withContext null
        runCatching { authorize(GoogleAuthActivityBridge.activity) }.getOrElse {
            null
        }
    }

    private suspend fun credentialSignIn(activity: Activity, serverClientId: String): String? {
        val option = GetSignInWithGoogleOption.Builder(serverClientId).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val response = CredentialManager.create(activity).getCredential(activity, request)
        val credential = response.credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            return GoogleIdTokenCredential.createFrom(credential.data).id
        }
        throw GoogleAuthError.Platform("Unexpected credential type: ${credential.type}")
    }

    private suspend fun authorize(activity: Activity?): String {
        val client = Identity.getAuthorizationClient(activity ?: context)
        val result = client.authorize(authorizationRequest).await()
        if (result.hasResolution()) {
            val pendingIntent = result.pendingIntent
                ?: throw GoogleAuthError.Platform("Authorization requires resolution but no PendingIntent")
            if (activity == null) throw GoogleAuthError.Platform("Authorization requires user consent")
            val activityResult = GoogleAuthActivityBridge.resolve(pendingIntent)
            if (activityResult.resultCode != android.app.Activity.RESULT_OK) {
                throw GoogleAuthError.UserCancelled()
            }
            val resolved = client.getAuthorizationResultFromIntent(activityResult.data)
            return resolved.accessToken
                ?.takeIf { it.isNotBlank() }
                ?: throw GoogleAuthError.Platform("No access token after consent")
        }
        return result.accessToken
            ?.takeIf { it.isNotBlank() }
            ?: throw GoogleAuthError.Platform("No access token returned")
    }
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) }
    addOnFailureListener { cont.resumeWithException(it) }
    addOnCanceledListener { cont.cancel() }
}

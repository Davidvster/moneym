package com.dv.moneym.core.oauth

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@OptIn(ExperimentalCoroutinesApi::class)
class AndroidAuthorizationLauncher(
    private val context: Context,
) : AuthorizationLauncher {

    override suspend fun launch(request: AuthorizationRequest, redirectUri: String): AuthorizationResponse {
        return inFlightLock.withLock {
            val deferred = CompletableDeferred<AuthorizationResponse>()
            pending = Pending(state = request.state, deferred = deferred)
            try {
                val intent = CustomTabsIntent.Builder().build()
                intent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.launchUrl(context, Uri.parse(request.url))
                suspendCancellableCoroutine { cont ->
                    deferred.invokeOnCompletion { cause ->
                        if (cause == null) cont.resume(deferred.getCompleted())
                        else cont.resumeWithException(cause)
                    }
                    cont.invokeOnCancellation {
                        if (!deferred.isCompleted) {
                            deferred.completeExceptionally(GoogleAuthError.UserCancelled())
                            pending = null
                        }
                    }
                }
            } finally {
                if (pending?.deferred == deferred) pending = null
            }
        }
    }

    companion object {
        private val inFlightLock = Mutex()
        private var pending: Pending? = null

        fun deliverRedirect(uri: Uri) {
            val current = pending ?: return
            val error = uri.getQueryParameter("error")
            if (error != null) {
                current.deferred.completeExceptionally(
                    GoogleAuthError.Platform("OAuth provider returned error: $error"),
                )
                pending = null
                return
            }
            val code = uri.getQueryParameter("code")
            val state = uri.getQueryParameter("state")
            if (code == null || state == null) {
                current.deferred.completeExceptionally(
                    GoogleAuthError.Platform("Redirect missing code or state"),
                )
                pending = null
                return
            }
            current.deferred.complete(AuthorizationResponse(code = code, state = state))
            pending = null
        }

        fun cancelInFlight() {
            val current = pending ?: return
            current.deferred.completeExceptionally(GoogleAuthError.UserCancelled())
            pending = null
        }

        private data class Pending(
            val state: String,
            val deferred: CompletableDeferred<AuthorizationResponse>,
        )
    }
}

package com.dv.moneym.core.oauth

import android.app.PendingIntent
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.CompletableDeferred

object GoogleAuthActivityBridge {

    @Volatile
    var activity: ComponentActivity? = null
        private set

    private var launcher: ActivityResultLauncher<IntentSenderRequest>? = null
    private var pending: CompletableDeferred<ActivityResult>? = null

    fun register(activity: ComponentActivity) {
        this.activity = activity
        launcher = activity.registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult(),
        ) { result ->
            pending?.complete(result)
            pending = null
        }
    }

    fun unregister() {
        activity = null
        launcher = null
        pending?.completeExceptionally(GoogleAuthError.UserCancelled())
        pending = null
    }

    suspend fun resolve(pendingIntent: PendingIntent): ActivityResult {
        val l = launcher ?: throw GoogleAuthError.Platform("No activity result launcher registered")
        val deferred = CompletableDeferred<ActivityResult>()
        pending = deferred
        l.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
        return deferred.await()
    }
}

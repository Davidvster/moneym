package com.dv.moneym

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.dv.moneym.data.remotebackup.RemoteBackupManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppLifecycleObserver(
    private val lockController: AppLockController,
    private val remoteBackupManager: RemoteBackupManager? = null,
) : LifecycleEventObserver {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_PAUSE -> {
                lockController.onBackground()
                remoteBackupManager?.let { manager ->
                    scope.launch { manager.flushNow() }
                }
            }
            Lifecycle.Event.ON_RESUME -> lockController.onForeground()
            else -> {}
        }
    }
}

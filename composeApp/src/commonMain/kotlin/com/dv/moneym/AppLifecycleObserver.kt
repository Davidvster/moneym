package com.dv.moneym

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner

class AppLifecycleObserver(private val lockController: AppLockController) : LifecycleEventObserver {
    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_PAUSE -> lockController.onBackground()
            Lifecycle.Event.ON_RESUME -> lockController.onForeground()
            else -> {}
        }
    }
}

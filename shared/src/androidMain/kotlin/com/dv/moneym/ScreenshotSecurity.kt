package com.dv.moneym

import android.app.Activity
import android.view.WindowManager
import com.dv.moneym.core.datastore.AppSettings
import com.dv.moneym.core.security.SecurityPrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.core.context.GlobalContext

/**
 * Drives the activity window's [WindowManager.LayoutParams.FLAG_SECURE] from the
 * persisted "allow screenshots" setting. Default (false) keeps screenshots/recording blocked.
 */
object ScreenshotSecurity {

    private var scope: CoroutineScope? = null

    fun bind(activity: Activity) {
        unbind()
        val settings = GlobalContext.get().get<AppSettings>()
        val newScope = CoroutineScope(Dispatchers.Main.immediate)
        scope = newScope
        settings.observeBoolean(SecurityPrefs.ALLOW_SCREENSHOTS, false)
            .distinctUntilChanged()
            .onEach { allowScreenshots ->
                if (allowScreenshots) {
                    activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }
            .launchIn(newScope)
    }

    fun unbind() {
        scope?.cancel()
        scope = null
    }
}

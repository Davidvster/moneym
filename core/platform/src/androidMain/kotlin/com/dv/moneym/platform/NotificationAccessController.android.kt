package com.dv.moneym.platform

import android.content.Context
import android.content.Intent
import android.provider.Settings

class AndroidNotificationAccessController(
    private val context: Context,
) : NotificationAccessController {

    override fun isAccessGranted(): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners",
        ) ?: return false
        return enabled.split(":").any { it.substringBefore("/") == context.packageName }
    }

    override fun openAccessSettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}

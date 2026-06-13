package com.dv.moneym.platform

/**
 * Controls the OS-level "notification access" grant that a [NotificationListenerService] needs.
 * Android-backed; on iOS the implementation is a no-op (the feature is Android-only).
 */
interface NotificationAccessController {
    fun isAccessGranted(): Boolean
    fun openAccessSettings()
}

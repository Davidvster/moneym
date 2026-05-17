package com.dv.moneym

import com.dv.moneym.core.datastore.AppSettings
import com.dv.moneym.core.security.SecurityPrefs
import kotlin.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppLockController(private val settings: AppSettings) {

    private val _isLocked: MutableStateFlow<Boolean?> = MutableStateFlow(null)
    val isLocked: StateFlow<Boolean?> = _isLocked.asStateFlow()

    fun init() {
        _isLocked.value = settings.getBoolean(SecurityPrefs.PIN_ENABLED)
    }

    fun unlock() {
        _isLocked.value = false
    }

    fun lockNow() {
        if (settings.getBoolean(SecurityPrefs.PIN_ENABLED)) {
            _isLocked.value = true
        }
    }

    fun onBackground() {
        settings.putLong(SecurityPrefs.LAST_BACKGROUND_AT, Clock.System.now().toEpochMilliseconds())
    }

    fun onForeground() {
        if (!settings.getBoolean(SecurityPrefs.PIN_ENABLED)) return
        val lastBackground = settings.getLong(SecurityPrefs.LAST_BACKGROUND_AT)
        if (lastBackground == 0L) return
        val elapsed = Clock.System.now().toEpochMilliseconds() - lastBackground
        val lockAfterMs = settings.getInt(SecurityPrefs.BACKGROUND_LOCK_SECONDS,
            SecurityPrefs.DEFAULT_LOCK_SECONDS).toLong() * 1000L
        if (elapsed >= lockAfterMs) {
            _isLocked.value = true
        }
    }
}

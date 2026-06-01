package com.dv.moneym.data.sync

/**
 * Minimal "kick a sync now" surface for UI. [SyncEngine] implements it. Keeping this an interface
 * lets the settings ViewModel be tested without constructing the full engine graph.
 */
interface SyncPuller {
    suspend fun pullNow(): Result<Unit>
}

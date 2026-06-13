package com.dv.moneym.data.walletsync

import com.dv.moneym.core.datastore.AppSettings
import com.dv.moneym.core.datastore.PrefKeys
import kotlinx.coroutines.flow.Flow

/**
 * Minimal wallet-sync status surface for the transaction-list sync sheet. Capture is passive
 * (notifications push in real time), so there is no "sync now" — only enabled state + pending count.
 */
interface WalletSyncStatusProvider {
    val isEnabled: Flow<Boolean>
    val pendingCount: Flow<Int>
}

class DefaultWalletSyncStatusProvider(
    private val appSettings: AppSettings,
    private val repository: WalletSyncRepository,
) : WalletSyncStatusProvider {
    override val isEnabled: Flow<Boolean>
        get() = appSettings.observeBoolean(PrefKeys.WALLET_SYNC_ENABLED, defaultValue = false)
    override val pendingCount: Flow<Int>
        get() = repository.observePendingCount()
}

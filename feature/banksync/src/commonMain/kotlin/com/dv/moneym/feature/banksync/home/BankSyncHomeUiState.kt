package com.dv.moneym.feature.banksync.home

import kotlinx.serialization.Serializable

@Serializable
data class BankAccountRow(
    val uid: String,
    val bankName: String,
    val displayName: String?,
    val iban: String?,
    val currency: String,
    val localAccountId: Long?,
    val enabled: Boolean,
)

@Serializable
data class LocalAccountOption(
    val id: Long,
    val name: String,
)

@Serializable
data class BankSyncHomeUiState(
    val isLoading: Boolean = true,
    val configured: Boolean = false,
    val connected: Boolean = false,
    val sessionValidUntilMs: Long? = null,
    val accounts: List<BankAccountRow> = emptyList(),
    val localAccounts: List<LocalAccountOption> = emptyList(),
    val accountPickerForUid: String? = null,
    val autoSyncEnabled: Boolean = false,
    val isSyncing: Boolean = false,
    val syncError: String? = null,
    val reconnectRequired: Boolean = false,
    val lastSyncMs: Long = 0L,
    val pendingCount: Int = 0,
)

sealed interface BankSyncHomeIntent {
    data class ShowAccountPicker(val uid: String?) : BankSyncHomeIntent
    data class SetLocalAccountMapping(val uid: String, val localAccountId: Long?) : BankSyncHomeIntent
    data class SetAccountEnabled(val uid: String, val enabled: Boolean) : BankSyncHomeIntent
    data object ToggleAutoSync : BankSyncHomeIntent
    data object SyncNow : BankSyncHomeIntent
    data object Disconnect : BankSyncHomeIntent
}

package com.dv.moneym.feature.banksync

import kotlinx.serialization.Serializable

@Serializable
data class BankRow(
    val name: String,
    val country: String,
)

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
data class BankSyncSettingsUiState(
    val isLoading: Boolean = true,
    val configured: Boolean = false,
    val connected: Boolean = false,

    // Credentials form (shown until configured)
    val appIdDraft: String = "",
    val pemDraft: String = "",
    val isValidatingCredentials: Boolean = false,
    val credentialsError: String? = null,

    // Bank picker / consent flow
    val countryDraft: String = "",
    val isLoadingBanks: Boolean = false,
    val banks: List<BankRow> = emptyList(),
    val bankSearch: String = "",
    val authUrlToOpen: String? = null,
    val awaitingAuth: Boolean = false,
    val connectingBankName: String? = null,
    val redirectDraft: String = "",
    val redirectInvalid: Boolean = false,
    val isCompletingConnection: Boolean = false,
    val connectError: String? = null,

    // Connected state
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
) {
    val filteredBanks: List<BankRow>
        get() = if (bankSearch.isBlank()) banks
        else banks.filter { it.name.contains(bankSearch, ignoreCase = true) }
}

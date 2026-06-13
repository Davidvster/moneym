package com.dv.moneym.feature.banksync.credentials

import kotlinx.serialization.Serializable

@Serializable
data class BankSyncCredentialsUiState(
    val appIdDraft: String = "",
    val pemDraft: String = "",
    val isValidatingCredentials: Boolean = false,
    val credentialsError: String? = null,
) {
    val canSave: Boolean
        get() = !isValidatingCredentials && appIdDraft.isNotBlank() && pemDraft.isNotBlank()
}

sealed interface BankSyncCredentialsIntent {
    data class AppIdChanged(val value: String) : BankSyncCredentialsIntent
    data class PemChanged(val value: String) : BankSyncCredentialsIntent
    data object SaveCredentials : BankSyncCredentialsIntent
}

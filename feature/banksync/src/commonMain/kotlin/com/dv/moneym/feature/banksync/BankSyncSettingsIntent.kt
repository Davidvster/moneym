package com.dv.moneym.feature.banksync

sealed interface BankSyncSettingsIntent {
    data class AppIdChanged(val value: String) : BankSyncSettingsIntent
    data class PemChanged(val value: String) : BankSyncSettingsIntent
    data object SaveCredentials : BankSyncSettingsIntent

    data class CountryChanged(val value: String) : BankSyncSettingsIntent
    data object LoadBanks : BankSyncSettingsIntent
    data class BankSearchChanged(val value: String) : BankSyncSettingsIntent
    data class ConnectBank(val name: String, val country: String) : BankSyncSettingsIntent
    data object AuthUrlOpened : BankSyncSettingsIntent
    data class RedirectChanged(val value: String) : BankSyncSettingsIntent
    data object SubmitRedirect : BankSyncSettingsIntent
    data class RedirectReceived(val url: String) : BankSyncSettingsIntent
    data object CancelAuth : BankSyncSettingsIntent

    data class ShowAccountPicker(val uid: String?) : BankSyncSettingsIntent
    data class SetLocalAccountMapping(val uid: String, val localAccountId: Long?) : BankSyncSettingsIntent
    data class SetAccountEnabled(val uid: String, val enabled: Boolean) : BankSyncSettingsIntent

    data object ToggleAutoSync : BankSyncSettingsIntent
    data object SyncNow : BankSyncSettingsIntent
    data object Disconnect : BankSyncSettingsIntent
}

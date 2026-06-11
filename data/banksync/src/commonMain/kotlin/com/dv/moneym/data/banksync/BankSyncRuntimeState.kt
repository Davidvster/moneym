package com.dv.moneym.data.banksync

sealed interface BankSyncRuntimeState {
    data object Idle : BankSyncRuntimeState
    data object Running : BankSyncRuntimeState
    data class Error(val message: String, val reconnectRequired: Boolean = false) : BankSyncRuntimeState
}

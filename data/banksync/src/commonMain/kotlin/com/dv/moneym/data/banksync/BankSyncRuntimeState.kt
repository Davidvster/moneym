package com.dv.moneym.data.banksync

import kotlinx.serialization.Serializable

sealed interface BankSyncRuntimeState {
    data object Idle : BankSyncRuntimeState
    data object Running : BankSyncRuntimeState
    data class Error(
        val message: String,
        val reason: BankSyncFailureReason = BankSyncFailureReason.Unknown,
        val reconnectRequired: Boolean = false,
    ) : BankSyncRuntimeState
}

@Serializable
data class BankSyncFailure(
    val reason: BankSyncFailureReason,
    val reconnectRequired: Boolean = false,
)

@Serializable
enum class BankSyncFailureReason {
    Network,
    Auth,
    Unknown,
}

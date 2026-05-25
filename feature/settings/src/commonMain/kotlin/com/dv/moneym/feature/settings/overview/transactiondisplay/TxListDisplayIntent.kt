package com.dv.moneym.feature.settings.overview.transactiondisplay

import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.model.TxDisplayPrefs

sealed interface TxListDisplayIntent {
    data class SetTxDisplayPrefs(val prefs: TxDisplayPrefs) : TxListDisplayIntent
    data class SetDefaultTransactionType(val type: TransactionType) : TxListDisplayIntent
    data class SetShowPendingRecurring(val enabled: Boolean) : TxListDisplayIntent
}

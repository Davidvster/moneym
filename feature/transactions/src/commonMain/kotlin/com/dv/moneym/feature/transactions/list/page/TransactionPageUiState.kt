package com.dv.moneym.feature.transactions.list.page

import com.dv.moneym.core.model.TxDisplayPrefs

internal data class TransactionPageUiState(
    val isLoading: Boolean = true,
    val dayGroups: List<com.dv.moneym.feature.transactions.list.DayGroup> = emptyList(),
    val isEmpty: Boolean = false,
    val txDisplayPrefs: TxDisplayPrefs = TxDisplayPrefs(),
)
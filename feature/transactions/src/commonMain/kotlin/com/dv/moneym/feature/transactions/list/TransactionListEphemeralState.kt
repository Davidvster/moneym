package com.dv.moneym.feature.transactions.list

import kotlinx.coroutines.flow.MutableStateFlow

class TransactionListEphemeralState {
    val searchQuery = MutableStateFlow("")
}

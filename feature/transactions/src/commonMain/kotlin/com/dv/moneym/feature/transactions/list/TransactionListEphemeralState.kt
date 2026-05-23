package com.dv.moneym.feature.transactions.list

import com.dv.moneym.core.model.CategoryId
import kotlinx.coroutines.flow.MutableStateFlow

class TransactionListEphemeralState {
    val searchQuery = MutableStateFlow("")
    val selectedCategoryIds = MutableStateFlow(emptySet<CategoryId>())
}

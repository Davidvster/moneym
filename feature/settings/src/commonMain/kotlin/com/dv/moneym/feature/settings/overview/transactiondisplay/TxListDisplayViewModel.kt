package com.dv.moneym.feature.settings.overview.transactiondisplay

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dv.moneym.core.datastore.AppSettingsRepository
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.model.TxDisplayPrefs
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TxListDisplayViewModel(
    private val appSettingsRepository: AppSettingsRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val txDisplayPrefs: StateFlow<TxDisplayPrefs> = appSettingsRepository.observeTxDisplayPrefs()
        .stateIn(viewModelScope, SharingStarted.Lazily, TxDisplayPrefs())

    val defaultTransactionType: StateFlow<TransactionType> =
        appSettingsRepository.observeDefaultTransactionType()
            .stateIn(viewModelScope, SharingStarted.Lazily, TransactionType.EXPENSE)

    fun setTxDisplayPrefs(prefs: TxDisplayPrefs) {
        viewModelScope.launch { appSettingsRepository.setTxDisplayPrefs(prefs) }
    }

    fun setDefaultTransactionType(type: TransactionType) {
        viewModelScope.launch { appSettingsRepository.setDefaultTransactionType(type) }
    }
}

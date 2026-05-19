package com.dv.moneym.feature.settings.overview.currencypicker

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dv.moneym.core.datastore.AppSettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CurrencyPickerViewModel(
    private val appSettingsRepository: AppSettingsRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val selectedCurrency: StateFlow<String> = appSettingsRepository.observeDefaultCurrency()
        .stateIn(viewModelScope, SharingStarted.Lazily, "")

    fun setDefaultCurrency(code: String) {
        viewModelScope.launch { appSettingsRepository.setDefaultCurrency(code) }
    }
}

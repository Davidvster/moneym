package com.dv.moneym.feature.settings.overview.currencypicker

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.dv.moneym.core.datastore.AppSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class CurrencyPickerViewModel(
    private val appSettingsRepository: AppSettingsRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val selectedCurrency: StateFlow<String> = MutableStateFlow("")
    fun setDefaultCurrency(code: String) {}
}

package com.dv.moneym.feature.settings.overview.currencypicker

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.serialization.saved
import com.dv.moneym.core.datastore.AppSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class CurrencyPickerViewModel(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val selectedCurrency: StateFlow<String> by savedStateHandle.saved { MutableStateFlow("") }

    fun onIntent(intent: CurrencyPickerIntent) {
        when (intent) {
            is CurrencyPickerIntent.SetDefaultCurrency -> setDefaultCurrency(intent.code)
        }
    }

    private fun setDefaultCurrency(code: String) {}
}

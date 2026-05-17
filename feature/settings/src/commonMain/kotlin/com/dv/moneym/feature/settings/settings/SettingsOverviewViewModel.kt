package com.dv.moneym.feature.settings.settings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dv.moneym.core.datastore.AppSettingsRepository
import com.dv.moneym.core.model.ThemeMode
import com.dv.moneym.core.model.TxDisplayPrefs
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsOverviewViewModel(
    private val appSettingsRepository: AppSettingsRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = appSettingsRepository.observeThemeMode()
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.Auto)

    val txDisplayPrefs: StateFlow<TxDisplayPrefs> = appSettingsRepository.observeTxDisplayPrefs()
        .stateIn(viewModelScope, SharingStarted.Eagerly, TxDisplayPrefs())

    val defaultCurrency: StateFlow<String> = appSettingsRepository.observeDefaultCurrency()
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val language: StateFlow<String> = appSettingsRepository.observeLanguage()
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { appSettingsRepository.setThemeMode(mode) }
    }
}

package com.dv.moneym.feature.settings.overview.locale

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dv.moneym.core.common.LocaleController
import com.dv.moneym.core.datastore.AppSettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LanguagePickerViewModel(
    private val appSettingsRepository: AppSettingsRepository,
    private val localeController: LocaleController,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val selectedLanguage: StateFlow<String> = appSettingsRepository.observeLanguage()
        .stateIn(viewModelScope, SharingStarted.Lazily, "")

    fun setLanguage(tag: String) {
        viewModelScope.launch {
            appSettingsRepository.setLanguage(tag)
            localeController.applyLocale(tag)
        }
    }
}

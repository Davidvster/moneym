package com.dv.moneym.feature.onboarding.currency

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.serialization.saved
import androidx.lifecycle.viewModelScope
import com.dv.moneym.core.common.DispatcherProvider
import com.dv.moneym.core.datastore.AppSettings
import com.dv.moneym.core.datastore.PrefKeys
import com.dv.moneym.core.model.Account
import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.AccountType
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.data.accounts.AccountRepository
import com.dv.moneym.data.backup.DbBackupManager
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Clock

class OnboardingCurrencyViewModel(
    private val accountRepository: AccountRepository,
    private val dbBackupManager: DbBackupManager,
    private val appSettings: AppSettings,
    private val dispatchers: DispatcherProvider,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _state by savedStateHandle.saved { MutableStateFlow(OnboardingCurrencyUiState()) }
    internal val state: StateFlow<OnboardingCurrencyUiState> = _state.asStateFlow()

    private val _effects = Channel<OnboardingCurrencyEffect>(Channel.BUFFERED)
    internal val effects = _effects.receiveAsFlow()

    private var pendingRestoreContent: ByteArray? = null

    fun onRestoreFileSelected(content: ByteArray) {
        onIntent(OnboardingCurrencyIntent.RestoreFileSelected(content))
    }

    internal fun onIntent(intent: OnboardingCurrencyIntent) {
        when (intent) {
            is OnboardingCurrencyIntent.CurrencySelected ->
                _state.update { it.copy(selectedCurrency = intent.code) }

            is OnboardingCurrencyIntent.SearchQueryChanged ->
                _state.update { it.copy(searchQuery = intent.query) }

            OnboardingCurrencyIntent.Continue -> {
                viewModelScope.launch {
                    if (accountRepository.count() == 0L) {
                        accountRepository.insert(
                            Account(
                                id = AccountId(0),
                                name = "Main",
                                type = AccountType.CASH,
                                currency = CurrencyCode(_state.value.selectedCurrency),
                                isDefault = true,
                                archived = false,
                                createdAt = Clock.System.now(),
                                updatedAt = Clock.System.now(),
                            )
                        )
                    }
                    _effects.send(OnboardingCurrencyEffect.NavigateToSecurity)
                }
            }

            is OnboardingCurrencyIntent.RestoreFileSelected -> {
                pendingRestoreContent = intent.content
                _state.update { it.copy(showRestoreWarning = true) }
            }

            OnboardingCurrencyIntent.RestoreConfirmed -> {
                val content = pendingRestoreContent ?: return
                pendingRestoreContent = null
                _state.update { it.copy(showRestoreWarning = false) }
                viewModelScope.launch {
                    appSettings.putBoolean(PrefKeys.ONBOARDING_COMPLETED, true)
                    withContext(dispatchers.io) { dbBackupManager.restore(content) }
                }
            }

            OnboardingCurrencyIntent.RestoreDismissed -> {
                pendingRestoreContent = null
                _state.update { it.copy(showRestoreWarning = false) }
            }

            OnboardingCurrencyIntent.ImportCsvTapped -> {
                viewModelScope.launch { _effects.send(OnboardingCurrencyEffect.OpenCsvFilePicker) }
            }
        }
    }
}

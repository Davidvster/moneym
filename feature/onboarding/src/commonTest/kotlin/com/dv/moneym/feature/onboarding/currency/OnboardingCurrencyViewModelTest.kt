package com.dv.moneym.feature.onboarding.currency

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.dv.moneym.core.datastore.PrefKeys
import com.dv.moneym.core.model.Account
import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.AccountType
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.testing.FakeAccountRepository
import com.dv.moneym.core.testing.FakeAppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class OnboardingCurrencyViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() { Dispatchers.setMain(testDispatcher) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    private fun vm(
        accounts: FakeAccountRepository = FakeAccountRepository(),
        settings: FakeAppSettings = FakeAppSettings(),
    ) = OnboardingCurrencyViewModel(accounts, settings, SavedStateHandle())

    private fun existingAccount() = Account(
        id = AccountId(0),
        name = "Existing",
        type = AccountType.CASH,
        currency = CurrencyCode("USD"),
        isDefault = true,
        archived = false,
        createdAt = Clock.System.now(),
        updatedAt = Clock.System.now(),
    )

    @Test
    fun currencySelectedUpdatesState() = runTest(testDispatcher) {
        val vm = vm()
        vm.state.test {
            assertEquals("", awaitItem().selectedCurrency)
            vm.onIntent(OnboardingCurrencyIntent.CurrencySelected("EUR"))
            assertEquals("EUR", awaitItem().selectedCurrency)
        }
    }

    @Test
    fun searchQueryChangedUpdatesState() = runTest(testDispatcher) {
        val vm = vm()
        vm.state.test {
            assertEquals("", awaitItem().searchQuery)
            vm.onIntent(OnboardingCurrencyIntent.SearchQueryChanged("eu"))
            assertEquals("eu", awaitItem().searchQuery)
        }
    }

    @Test
    fun continueCompletesOnboardingWhenAccountAlreadyExists() = runTest(testDispatcher) {
        val accounts = FakeAccountRepository()
        accounts.addAll(listOf(existingAccount()))
        val settings = FakeAppSettings()
        val vm = vm(accounts = accounts, settings = settings)

        vm.onIntent(OnboardingCurrencyIntent.CurrencySelected("EUR"))
        vm.effects.test {
            vm.onIntent(OnboardingCurrencyIntent.Continue)
            assertEquals(OnboardingCurrencyEffect.NavigateComplete, awaitItem())
        }

        assertEquals(1L, accounts.count())
        assertEquals(CurrencyCode("USD"), accounts.accounts.single().currency)
        assertTrue(settings.getBoolean(PrefKeys.ONBOARDING_COMPLETED))
    }

    @Test
    fun importCsvTappedEmitsOpenPickerEffect() = runTest(testDispatcher) {
        val vm = vm()
        vm.effects.test {
            vm.onIntent(OnboardingCurrencyIntent.ImportCsvTapped)
            assertEquals(OnboardingCurrencyEffect.OpenCsvFilePicker, awaitItem())
        }
    }
}

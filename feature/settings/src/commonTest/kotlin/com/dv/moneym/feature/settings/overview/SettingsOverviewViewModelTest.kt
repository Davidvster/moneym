package com.dv.moneym.feature.settings.overview

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.dv.moneym.core.model.Account
import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.AccountType
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.ThemeMode
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.testing.FakeAccountRepository
import com.dv.moneym.core.testing.FakeAppSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.time.Instant
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SettingsOverviewViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() { Dispatchers.setMain(testDispatcher) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    private fun account(
        id: Long,
        currency: String,
        isDefault: Boolean = false,
    ) = Account(
        id = AccountId(id),
        name = "Acc$id",
        type = AccountType.CASH,
        currency = CurrencyCode(currency),
        isDefault = isDefault,
        archived = false,
        createdAt = Instant.DISTANT_PAST,
        updatedAt = Instant.DISTANT_PAST,
    )

    private fun vm(
        settings: FakeAppSettingsRepository = FakeAppSettingsRepository(),
        accounts: FakeAccountRepository = FakeAccountRepository(),
    ) = SettingsOverviewViewModel(settings, accounts, SavedStateHandle())

    @Test
    fun setThemeModePersists() = runTest(testDispatcher) {
        val settings = FakeAppSettingsRepository()
        val vm = vm(settings = settings)

        vm.themeMode.test {
            assertEquals(ThemeMode.Auto, awaitItem())
            vm.onIntent(SettingsOverviewIntent.SetThemeMode(ThemeMode.Dark))
            assertEquals(ThemeMode.Dark, awaitItem())
        }
    }

    @Test
    fun setDefaultTransactionTypePersists() = runTest(testDispatcher) {
        val vm = vm()
        vm.defaultTransactionType.test {
            assertEquals(TransactionType.EXPENSE, awaitItem())
            vm.onIntent(SettingsOverviewIntent.SetDefaultTransactionType(TransactionType.INCOME))
            assertEquals(TransactionType.INCOME, awaitItem())
        }
    }

    @Test
    fun setPaymentModeEnabledPersists() = runTest(testDispatcher) {
        val vm = vm()
        vm.paymentModeEnabled.test {
            assertFalse(awaitItem())
            vm.onIntent(SettingsOverviewIntent.SetPaymentModeEnabled(true))
            assertTrue(awaitItem())
        }
    }

    @Test
    fun setUseCurrencySymbolPersists() = runTest(testDispatcher) {
        val vm = vm()
        vm.useCurrencySymbol.test {
            assertFalse(awaitItem())
            vm.onIntent(SettingsOverviewIntent.SetUseCurrencySymbol(true))
            assertTrue(awaitItem())
        }
    }

    @Test
    fun showLockPickerTogglesState() = runTest(testDispatcher) {
        val vm = vm()
        vm.showLockPicker.test {
            assertFalse(awaitItem())
            vm.onIntent(SettingsOverviewIntent.ShowLockPicker(true))
            assertTrue(awaitItem())
            vm.onIntent(SettingsOverviewIntent.ShowLockPicker(false))
            assertFalse(awaitItem())
        }
    }

    @Test
    fun walletCurrencyUsesSelectedAccountWhenSelectedIdSet() = runTest(testDispatcher) {
        val settings = FakeAppSettingsRepository()
        val accounts = FakeAccountRepository()
        accounts.addAll(listOf(account(1, "EUR", isDefault = true), account(2, "USD")))
        settings.setSelectedAccountId(2)
        val vm = vm(settings = settings, accounts = accounts)

        vm.walletCurrency.test {
            var v = awaitItem()
            while (v != "USD") v = awaitItem()
            assertEquals("USD", v)
        }
    }

    @Test
    fun walletCurrencyFallsBackToDefaultAccountWhenNoneSelected() = runTest(testDispatcher) {
        val accounts = FakeAccountRepository()
        accounts.addAll(listOf(account(1, "GBP", isDefault = true), account(2, "USD")))
        val vm = vm(accounts = accounts)

        vm.walletCurrency.test {
            var v = awaitItem()
            while (v != "GBP") v = awaitItem()
            assertEquals("GBP", v)
        }
    }

    @Test
    fun walletCurrencyDefaultsToEurWhenNoAccounts() = runTest(testDispatcher) {
        val vm = vm()
        vm.walletCurrency.test {
            assertEquals("EUR", awaitItem())
        }
    }
}

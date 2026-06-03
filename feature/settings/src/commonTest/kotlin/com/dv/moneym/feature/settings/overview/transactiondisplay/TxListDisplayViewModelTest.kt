package com.dv.moneym.feature.settings.overview.transactiondisplay

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.dv.moneym.core.model.Density
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.model.TxDisplayPrefs
import com.dv.moneym.core.testing.FakeAppSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class TxListDisplayViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() { Dispatchers.setMain(testDispatcher) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    private fun vm(settings: FakeAppSettingsRepository = FakeAppSettingsRepository()) =
        TxListDisplayViewModel(settings, SavedStateHandle())

    @Test
    fun setTxDisplayPrefsPersists() = runTest(testDispatcher) {
        val vm = vm()
        val updated = TxDisplayPrefs(density = Density.Compact, showNote = false)
        vm.txDisplayPrefs.test {
            assertEquals(TxDisplayPrefs(), awaitItem())
            vm.onIntent(TxListDisplayIntent.SetTxDisplayPrefs(updated))
            assertEquals(updated, awaitItem())
        }
    }

    @Test
    fun setDefaultTransactionTypePersists() = runTest(testDispatcher) {
        val vm = vm()
        vm.defaultTransactionType.test {
            assertEquals(TransactionType.EXPENSE, awaitItem())
            vm.onIntent(TxListDisplayIntent.SetDefaultTransactionType(TransactionType.INCOME))
            assertEquals(TransactionType.INCOME, awaitItem())
        }
    }

    @Test
    fun setShowPendingRecurringPersists() = runTest(testDispatcher) {
        val vm = vm()
        vm.showPendingRecurring.test {
            assertEquals(true, awaitItem())
            vm.onIntent(TxListDisplayIntent.SetShowPendingRecurring(false))
            assertFalse(awaitItem())
        }
    }
}

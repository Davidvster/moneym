package com.dv.moneym.feature.settings.overview.currencypicker

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class CurrencyPickerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() { Dispatchers.setMain(testDispatcher) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun selectedCurrencyStartsEmpty() = runTest(testDispatcher) {
        val vm = CurrencyPickerViewModel(SavedStateHandle())
        vm.selectedCurrency.test {
            assertEquals("", awaitItem())
        }
    }

    @Test
    fun setDefaultCurrencyIntentDoesNotMutateState() = runTest(testDispatcher) {
        val vm = CurrencyPickerViewModel(SavedStateHandle())
        vm.selectedCurrency.test {
            assertEquals("", awaitItem())
            vm.onIntent(CurrencyPickerIntent.SetDefaultCurrency("USD"))
            expectNoEvents()
            assertEquals("", vm.selectedCurrency.value)
        }
    }
}

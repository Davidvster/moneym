package com.dv.moneym.feature.onboarding

import app.cash.turbine.test
import com.dv.moneym.core.testing.FakeAppSettings
import com.dv.moneym.core.testing.runTestWithDispatchers
import com.dv.moneym.feature.onboarding.presentation.OnboardingCurrencyEffect
import com.dv.moneym.feature.onboarding.presentation.OnboardingCurrencyIntent
import com.dv.moneym.feature.onboarding.presentation.OnboardingCurrencyViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class OnboardingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() { Dispatchers.setMain(testDispatcher) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    private fun makeVm(): OnboardingCurrencyViewModel {
        val settings = FakeAppSettings()
        return OnboardingCurrencyViewModel(settings)
    }

    @Test
    fun currencySelectionUpdatesState() = runTestWithDispatchers(testDispatcher) {
        val vm = makeVm()
        vm.onIntent(OnboardingCurrencyIntent.CurrencySelected("USD"))
        assertEquals("USD", vm.state.value.selectedCurrency)
    }

    @Test
    fun continueEmitsNavigateToSecurityEffect() = runTestWithDispatchers(testDispatcher) {
        val vm = makeVm()
        vm.effects.test {
            vm.onIntent(OnboardingCurrencyIntent.Continue)
            assertEquals(OnboardingCurrencyEffect.NavigateToSecurity, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}

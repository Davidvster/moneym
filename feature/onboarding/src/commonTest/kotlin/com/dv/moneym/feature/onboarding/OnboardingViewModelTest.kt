package com.dv.moneym.feature.onboarding

import app.cash.turbine.test
import com.dv.moneym.core.security.PinHasher
import com.dv.moneym.core.security.PinManager
import com.dv.moneym.core.testing.FakeAppSettings
import com.dv.moneym.core.testing.InMemorySecureStore
import com.dv.moneym.core.testing.runTestWithDispatchers
import com.dv.moneym.feature.onboarding.presentation.OnboardingEffect
import com.dv.moneym.feature.onboarding.presentation.OnboardingIntent
import com.dv.moneym.feature.onboarding.presentation.OnboardingViewModel
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

    private fun makeVm(): OnboardingViewModel {
        val settings = FakeAppSettings()
        val pinManager = PinManager(InMemorySecureStore(), PinHasher(), settings)
        return OnboardingViewModel(settings, pinManager)
    }

    @Test
    fun currencySelectionUpdatesState() = runTestWithDispatchers(testDispatcher) {
        val vm = makeVm()
        vm.onIntent(OnboardingIntent.CurrencySelected("USD"))
        assertEquals("USD", vm.state.value.selectedCurrency)
    }

    @Test
    fun finishEmitsCompleteEffect() = runTestWithDispatchers(testDispatcher) {
        val vm = makeVm()
        vm.effects.test {
            vm.onIntent(OnboardingIntent.Finish)
            assertEquals(OnboardingEffect.Complete, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}

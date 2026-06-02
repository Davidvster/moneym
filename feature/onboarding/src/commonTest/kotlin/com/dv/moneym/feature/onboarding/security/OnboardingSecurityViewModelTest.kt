package com.dv.moneym.feature.onboarding.security

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.dv.moneym.core.security.PinHasher
import com.dv.moneym.core.security.PinManager
import com.dv.moneym.core.security.SecurityPrefs
import com.dv.moneym.core.testing.FakeAppSettings
import com.dv.moneym.core.testing.InMemorySecureStore
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
import kotlin.test.assertTrue

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class OnboardingSecurityViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() { Dispatchers.setMain(testDispatcher) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    private fun pinManager(settings: FakeAppSettings, store: InMemorySecureStore) =
        PinManager(store, PinHasher(), settings)

    private fun vm(
        settings: FakeAppSettings = FakeAppSettings(),
        store: InMemorySecureStore = InMemorySecureStore(),
        biometric: FakeBiometricAuthenticator = FakeBiometricAuthenticator(),
    ) = OnboardingSecurityViewModel(settings, pinManager(settings, store), biometric, SavedStateHandle())

    @Test
    fun biometricAvailabilityReflectedInInitialState() = runTest(testDispatcher) {
        val vm = vm(biometric = FakeBiometricAuthenticator(isAvailable = true))
        vm.state.test {
            assertTrue(awaitItem().biometricAvailable)
        }
    }

    @Test
    fun initMarksPinEnabledWhenPinAlreadySet() = runTest(testDispatcher) {
        val settings = FakeAppSettings()
        val store = InMemorySecureStore()
        pinManager(settings, store).setPin("1234")
        val vm = vm(settings = settings, store = store)

        vm.state.test {
            skipItems(1)
            assertTrue(awaitItem().pinEnabled)
        }
    }

    @Test
    fun setupPinRequestedEmitsNavigateToPinSetup() = runTest(testDispatcher) {
        val vm = vm()
        vm.effects.test {
            vm.onIntent(OnboardingSecurityIntent.SetupPinRequested)
            assertEquals(OnboardingSecurityEffect.NavigateToPinSetup, awaitItem())
        }
    }

    @Test
    fun biometricToggledPersistsAndUpdatesState() = runTest(testDispatcher) {
        val settings = FakeAppSettings()
        val vm = vm(settings = settings)
        vm.state.test {
            skipItems(1)
            vm.onIntent(OnboardingSecurityIntent.BiometricToggled(true))
            assertTrue(awaitItem().biometricEnabled)
        }
        assertTrue(settings.getBoolean(SecurityPrefs.BIOMETRIC_ENABLED))

        vm.onIntent(OnboardingSecurityIntent.BiometricToggled(false))
        assertFalse(settings.getBoolean(SecurityPrefs.BIOMETRIC_ENABLED))
    }

    @Test
    fun finishPersistsPinEnabledAndNavigatesToCurrency() = runTest(testDispatcher) {
        val settings = FakeAppSettings()
        val store = InMemorySecureStore()
        pinManager(settings, store).setPin("1234")
        val vm = vm(settings = settings, store = store)

        vm.state.test {
            skipItems(1)
            assertTrue(awaitItem().pinEnabled)
        }
        vm.effects.test {
            vm.onIntent(OnboardingSecurityIntent.Finish)
            assertEquals(OnboardingSecurityEffect.NavigateToCurrency, awaitItem())
        }
        assertTrue(settings.getBoolean(SecurityPrefs.PIN_ENABLED))
    }

    @Test
    fun returnFromPinSetupRefreshesPinAndBiometricState() = runTest(testDispatcher) {
        val settings = FakeAppSettings()
        val store = InMemorySecureStore()
        val vm = vm(settings = settings, store = store)

        vm.state.test {
            assertFalse(awaitItem().pinEnabled)

            pinManager(settings, store).setPin("4321")
            settings.putBoolean(SecurityPrefs.BIOMETRIC_ENABLED, true)
            vm.onIntent(OnboardingSecurityIntent.ReturnFromPinSetup)

            var s = awaitItem()
            while (!s.pinEnabled || !s.biometricEnabled) s = awaitItem()
            assertTrue(s.pinEnabled)
            assertTrue(s.biometricEnabled)
        }
    }
}

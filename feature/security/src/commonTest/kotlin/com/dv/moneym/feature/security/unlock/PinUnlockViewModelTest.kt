package com.dv.moneym.feature.security.unlock

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.dv.moneym.core.security.BiometricResult
import com.dv.moneym.core.security.BiometryType
import com.dv.moneym.core.security.PinHasher
import com.dv.moneym.core.security.PinManager
import com.dv.moneym.core.security.SecurityPrefs
import com.dv.moneym.core.testing.FakeAppSettings
import com.dv.moneym.core.testing.InMemorySecureStore
import com.dv.moneym.core.testing.TestDispatcherProvider
import com.dv.moneym.feature.security.FakeBiometricAuthenticator
import com.dv.moneym.feature.security.PinError
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
class PinUnlockViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() { Dispatchers.setMain(testDispatcher) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    private fun pinManager(settings: FakeAppSettings, store: InMemorySecureStore) =
        PinManager(store, PinHasher(), settings)

    private suspend fun seedPin(settings: FakeAppSettings, store: InMemorySecureStore, pin: String) {
        pinManager(settings, store).setPin(pin)
    }

    private fun vm(
        settings: FakeAppSettings,
        store: InMemorySecureStore,
        biometric: FakeBiometricAuthenticator = FakeBiometricAuthenticator(),
    ) = PinUnlockViewModel(
        pinManager(settings, store),
        biometric,
        settings,
        TestDispatcherProvider(testDispatcher),
        SavedStateHandle(),
    )

    private fun PinUnlockViewModel.enter(vararg digits: Int) =
        digits.forEach { onIntent(PinUnlockIntent.DigitPressed(it)) }

    @Test
    fun correctPinEmitsUnlocked() = runTest(testDispatcher) {
        val settings = FakeAppSettings()
        val store = InMemorySecureStore()
        seedPin(settings, store, "1234")
        val vm = vm(settings, store)

        vm.effects.test {
            vm.enter(1, 2, 3, 4)
            assertEquals(PinUnlockEffect.Unlocked, awaitItem())
        }
        assertEquals(0, pinManager(settings, store).failedAttempts())
    }

    @Test
    fun wrongPinRecordsFailedAttemptAndSetsError() = runTest(testDispatcher) {
        val settings = FakeAppSettings()
        val store = InMemorySecureStore()
        seedPin(settings, store, "1234")
        val vm = vm(settings, store)

        vm.state.test {
            skipItems(1)
            vm.enter(9, 9, 9, 9)
            var s = awaitItem()
            while (s.error == null) s = awaitItem()
            assertEquals(PinError.IncorrectPin, s.error)
            assertEquals(1, s.failedAttempts)
            assertEquals("", s.pin)
            assertFalse(s.isVerifying)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(1, pinManager(settings, store).failedAttempts())
    }

    @Test
    fun deleteRemovesLastDigit() = runTest(testDispatcher) {
        val settings = FakeAppSettings()
        val store = InMemorySecureStore()
        seedPin(settings, store, "1234")
        val vm = vm(settings, store)

        vm.state.test {
            skipItems(1)
            vm.enter(1, 2)
            var s = awaitItem()
            while (s.pin != "12") s = awaitItem()
            vm.onIntent(PinUnlockIntent.DeletePressed)
            assertEquals("1", awaitItem().pin)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun biometricUnlockOnRequestEmitsUnlocked() = runTest(testDispatcher) {
        val settings = FakeAppSettings()
        val store = InMemorySecureStore()
        seedPin(settings, store, "1234")
        val biometric = FakeBiometricAuthenticator(isAvailable = true, result = BiometricResult.Success)
        val vm = vm(settings, store, biometric)

        vm.effects.test {
            vm.onIntent(PinUnlockIntent.BiometricRequested("Unlock"))
            assertEquals(PinUnlockEffect.Unlocked, awaitItem())
        }
    }

    @Test
    fun autoTriggersBiometricOnInitWhenEnabledAndAvailable() = runTest(testDispatcher) {
        val settings = FakeAppSettings()
        val store = InMemorySecureStore()
        seedPin(settings, store, "1234")
        settings.putBoolean(SecurityPrefs.BIOMETRIC_ENABLED, true)
        val biometric = FakeBiometricAuthenticator(isAvailable = true, result = BiometricResult.Success)
        val vm = vm(settings, store, biometric)

        vm.effects.test {
            vm.state.test {
                var s = awaitItem()
                while (!s.biometricAvailable) s = awaitItem()
                assertTrue(s.biometricAvailable)
                cancelAndIgnoreRemainingEvents()
            }
            assertEquals(PinUnlockEffect.Unlocked, awaitItem())
        }
        assertTrue(biometric.authenticateCount >= 1)
    }

    @Test
    fun keyInvalidatedDisablesBiometric() = runTest(testDispatcher) {
        val settings = FakeAppSettings()
        val store = InMemorySecureStore()
        seedPin(settings, store, "1234")
        settings.putBoolean(SecurityPrefs.BIOMETRIC_ENABLED, true)
        val biometric = FakeBiometricAuthenticator(isAvailable = true, result = BiometricResult.KeyInvalidated)
        val vm = vm(settings, store, biometric)

        vm.state.test {
            var s = awaitItem()
            while (s.biometryType != BiometryType.None) s = awaitItem()
            assertFalse(s.biometricAvailable)
            assertEquals(BiometryType.None, s.biometryType)
            cancelAndIgnoreRemainingEvents()
        }
        assertFalse(settings.getBoolean(SecurityPrefs.BIOMETRIC_ENABLED))
    }

}

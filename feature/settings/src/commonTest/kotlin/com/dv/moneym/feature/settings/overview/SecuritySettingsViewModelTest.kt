package com.dv.moneym.feature.settings.overview

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.dv.moneym.core.security.PinHasher
import com.dv.moneym.core.security.PinManager
import com.dv.moneym.core.security.SecurityPrefs
import com.dv.moneym.core.testing.FakeAppSettings
import com.dv.moneym.core.testing.InMemorySecureStore
import com.dv.moneym.core.testing.TestDispatcherProvider
import com.dv.moneym.feature.settings.FakeBiometricAuthenticator
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
class SecuritySettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() { Dispatchers.setMain(testDispatcher) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    private fun pinManager(settings: FakeAppSettings) =
        PinManager(InMemorySecureStore(), PinHasher(), settings)

    private fun vm(
        settings: FakeAppSettings = FakeAppSettings(),
        pinManager: PinManager = pinManager(settings),
        biometric: FakeBiometricAuthenticator = FakeBiometricAuthenticator(),
    ) = SecuritySettingsViewModel(
        settings,
        pinManager,
        biometric,
        TestDispatcherProvider(testDispatcher),
        SavedStateHandle(),
    )

    @Test
    fun initialStateReflectsSettingsAndBiometricAvailability() = runTest(testDispatcher) {
        val settings = FakeAppSettings()
        settings.putBoolean(SecurityPrefs.PIN_ENABLED, true)
        settings.putBoolean(SecurityPrefs.BIOMETRIC_ENABLED, true)
        settings.putInt(SecurityPrefs.BACKGROUND_LOCK_SECONDS, 60)
        val vm = vm(settings = settings, biometric = FakeBiometricAuthenticator(isAvailable = false))

        val s = vm.state.value
        assertTrue(s.pinEnabled)
        assertTrue(s.biometricEnabled)
        assertFalse(s.biometricAvailable)
        assertEquals(60, s.backgroundLockSeconds)
    }

    @Test
    fun enablingPinSetsFlagAndEmitsNavigateEffect() = runTest(testDispatcher) {
        val vm = vm()
        vm.effects.test {
            vm.state.test {
                skipItems(1)
                vm.onIntent(SecuritySettingsIntent.PinToggled(enable = true))
                assertTrue(awaitItem().pinEnabled)
            }
            assertEquals(SecuritySettingsEffect.NavigateToPinSetup, awaitItem())
        }
    }

    @Test
    fun disablingPinClearsPinAndBiometric() = runTest(testDispatcher) {
        val settings = FakeAppSettings()
        val pm = pinManager(settings)
        pm.setPin("1234")
        settings.putBoolean(SecurityPrefs.BIOMETRIC_ENABLED, true)
        val vm = vm(settings = settings, pinManager = pm)

        vm.state.test {
            skipItems(1)
            vm.onIntent(SecuritySettingsIntent.PinToggled(enable = false))
            var s = awaitItem()
            while (s.pinEnabled || s.biometricEnabled) s = awaitItem()
            assertFalse(s.pinEnabled)
            assertFalse(s.biometricEnabled)
        }
        assertFalse(settings.getBoolean(SecurityPrefs.PIN_ENABLED))
        assertFalse(pm.isPinSet())
    }

    @Test
    fun biometricToggledPersistsAndUpdatesState() = runTest(testDispatcher) {
        val settings = FakeAppSettings()
        val vm = vm(settings = settings)

        vm.state.test {
            skipItems(1)
            vm.onIntent(SecuritySettingsIntent.BiometricToggled(enable = true))
            assertTrue(awaitItem().biometricEnabled)
            assertTrue(settings.getBoolean(SecurityPrefs.BIOMETRIC_ENABLED))
            vm.onIntent(SecuritySettingsIntent.BiometricToggled(enable = false))
            assertFalse(awaitItem().biometricEnabled)
            assertFalse(settings.getBoolean(SecurityPrefs.BIOMETRIC_ENABLED))
        }
    }

    @Test
    fun lockTimeoutChangedPersistsAndUpdatesState() = runTest(testDispatcher) {
        val settings = FakeAppSettings()
        val vm = vm(settings = settings)

        vm.state.test {
            skipItems(1)
            vm.onIntent(SecuritySettingsIntent.LockTimeoutChanged(seconds = 120))
            assertEquals(120, awaitItem().backgroundLockSeconds)
        }
        assertEquals(120, settings.getInt(SecurityPrefs.BACKGROUND_LOCK_SECONDS, -1))
    }

    @Test
    fun changePinRequestedEmitsNavigateEffect() = runTest(testDispatcher) {
        val vm = vm()
        vm.effects.test {
            vm.onIntent(SecuritySettingsIntent.ChangePinRequested)
            assertEquals(SecuritySettingsEffect.NavigateToPinSetup, awaitItem())
        }
    }

    @Test
    fun refreshPinStateReReadsSettings() = runTest(testDispatcher) {
        val settings = FakeAppSettings()
        val vm = vm(settings = settings)

        vm.state.test {
            assertFalse(awaitItem().pinEnabled)
            settings.putBoolean(SecurityPrefs.PIN_ENABLED, true)
            settings.putBoolean(SecurityPrefs.BIOMETRIC_ENABLED, true)
            vm.onIntent(SecuritySettingsIntent.RefreshPinState)
            val s = awaitItem()
            assertTrue(s.pinEnabled)
            assertTrue(s.biometricEnabled)
        }
    }
}

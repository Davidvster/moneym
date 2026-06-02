package com.dv.moneym.feature.security.setup

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class PinSetupViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() { Dispatchers.setMain(testDispatcher) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    private fun pinManager(settings: FakeAppSettings, store: InMemorySecureStore) =
        PinManager(store, PinHasher(), settings)

    private fun vm(
        settings: FakeAppSettings = FakeAppSettings(),
        store: InMemorySecureStore = InMemorySecureStore(),
        biometric: FakeBiometricAuthenticator = FakeBiometricAuthenticator(),
    ) = PinSetupViewModel(
        pinManager(settings, store),
        TestDispatcherProvider(testDispatcher),
        biometric,
        settings,
        SavedStateHandle(),
    )

    private fun PinSetupViewModel.enter(vararg digits: Int) =
        digits.forEach { onIntent(PinSetupIntent.DigitPressed(it)) }

    @Test
    fun firstFourDigitsAdvanceToConfirmStep() = runTest(testDispatcher) {
        val vm = vm()
        vm.state.test {
            assertEquals(PinSetupStep.ENTER_FIRST, awaitItem().step)
            vm.onIntent(PinSetupIntent.DigitPressed(1))
            assertEquals("1", awaitItem().firstPin)
            vm.onIntent(PinSetupIntent.DigitPressed(2))
            assertEquals("12", awaitItem().firstPin)
            vm.onIntent(PinSetupIntent.DigitPressed(3))
            assertEquals("123", awaitItem().firstPin)
            vm.onIntent(PinSetupIntent.DigitPressed(4))
            val confirm = awaitItem()
            assertEquals(PinSetupStep.CONFIRM, confirm.step)
            assertEquals("1234", confirm.firstPin)
        }
    }

    @Test
    fun matchingConfirmSavesPinAndEmitsDone() = runTest(testDispatcher) {
        val settings = FakeAppSettings()
        val store = InMemorySecureStore()
        val vm = vm(settings = settings, store = store)

        vm.effects.test {
            vm.enter(1, 2, 3, 4)
            vm.enter(1, 2, 3, 4)
            assertEquals(PinSetupEffect.Done, awaitItem())
        }
        assertTrue(settings.getBoolean(SecurityPrefs.PIN_ENABLED))
        assertTrue(pinManager(settings, store).verifyPin("1234"))
    }

    @Test
    fun mismatchedConfirmResetsToFirstWithError() = runTest(testDispatcher) {
        val vm = vm()
        vm.state.test {
            skipItems(1)
            vm.enter(1, 2, 3, 4)
            var s = awaitItem()
            while (s.step != PinSetupStep.CONFIRM) s = awaitItem()
            vm.enter(5, 6, 7, 8)
            var after = awaitItem()
            while (after.error == null) after = awaitItem()
            assertEquals(PinSetupStep.ENTER_FIRST, after.step)
            assertEquals("", after.firstPin)
            assertEquals("", after.secondPin)
            assertEquals(PinError.PinsMismatch, after.error)
        }
    }

    @Test
    fun deleteRemovesLastDigitInFirstStep() = runTest(testDispatcher) {
        val vm = vm()
        vm.state.test {
            skipItems(1)
            vm.enter(1, 2)
            var s = awaitItem()
            while (s.firstPin != "12") s = awaitItem()
            vm.onIntent(PinSetupIntent.DeletePressed)
            assertEquals("1", awaitItem().firstPin)
        }
    }

    @Test
    fun deleteOnEmptyConfirmGoesBackToFirstStep() = runTest(testDispatcher) {
        val vm = vm()
        vm.state.test {
            skipItems(1)
            vm.enter(1, 2, 3, 4)
            var s = awaitItem()
            while (s.step != PinSetupStep.CONFIRM) s = awaitItem()
            vm.onIntent(PinSetupIntent.DeletePressed)
            assertEquals(PinSetupStep.ENTER_FIRST, awaitItem().step)
        }
    }

    @Test
    fun biometricAvailableOffersBiometricsOnMatch() = runTest(testDispatcher) {
        val vm = vm(biometric = FakeBiometricAuthenticator(isAvailable = true))
        vm.effects.test {
            vm.enter(1, 2, 3, 4)
            vm.enter(1, 2, 3, 4)
            assertEquals(PinSetupEffect.OfferBiometrics, awaitItem())
        }
    }

    @Test
    fun biometricOfferAcceptedEnablesBiometricsAndCompletes() = runTest(testDispatcher) {
        val settings = FakeAppSettings()
        val vm = vm(settings = settings)
        vm.effects.test {
            vm.onIntent(PinSetupIntent.BiometricOfferAccepted)
            assertEquals(PinSetupEffect.Done, awaitItem())
        }
        assertTrue(settings.getBoolean(SecurityPrefs.BIOMETRIC_ENABLED))
    }

    @Test
    fun biometricOfferDeclinedCompletesWithoutEnabling() = runTest(testDispatcher) {
        val settings = FakeAppSettings()
        val vm = vm(settings = settings)
        vm.effects.test {
            vm.onIntent(PinSetupIntent.BiometricOfferDeclined)
            assertEquals(PinSetupEffect.Done, awaitItem())
        }
        assertTrue(!settings.getBoolean(SecurityPrefs.BIOMETRIC_ENABLED))
    }

    @Test
    fun resetClearsStateBackToInitial() = runTest(testDispatcher) {
        val vm = vm(biometric = FakeBiometricAuthenticator(isAvailable = true, biometryType = BiometryType.FaceId))
        vm.state.test {
            skipItems(1)
            vm.enter(1, 2)
            var s = awaitItem()
            while (s.firstPin != "12") s = awaitItem()
            vm.onIntent(PinSetupIntent.Reset)
            val reset = awaitItem()
            assertEquals(PinSetupStep.ENTER_FIRST, reset.step)
            assertEquals("", reset.firstPin)
            assertNull(reset.error)
            assertEquals(BiometryType.FaceId, reset.biometryType)
        }
    }
}

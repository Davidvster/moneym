package com.dv.moneym.feature.settings.paymentmodes

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.dv.moneym.core.model.PaymentMode
import com.dv.moneym.core.model.PaymentModeId
import com.dv.moneym.feature.settings.FakePaymentModeRepository
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
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class PaymentModeListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() { Dispatchers.setMain(testDispatcher) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    private fun mode(id: Long, name: String) =
        PaymentMode(PaymentModeId(id), name, Instant.DISTANT_PAST, Instant.DISTANT_PAST)

    private fun vm(repo: FakePaymentModeRepository = FakePaymentModeRepository()) =
        PaymentModeListViewModel(repo, SavedStateHandle())

    @Test
    fun stateRendersModesAndClearsLoading() = runTest(testDispatcher) {
        val repo = FakePaymentModeRepository(listOf(mode(1, "Cash")))
        val vm = vm(repo)
        vm.state.test {
            skipItems(1)
            val s = awaitItem()
            assertFalse(s.isLoading)
            assertEquals(1, s.modes.size)
            assertEquals("Cash", s.modes.first().name)
            assertIs<PaymentModeDialogState.None>(s.dialogState)
        }
    }

    @Test
    fun showAddSetsAddDialog() = runTest(testDispatcher) {
        val vm = vm()
        vm.state.test {
            skipItems(1)
            awaitItem()
            vm.onIntent(PaymentModeListIntent.ShowAdd)
            assertIs<PaymentModeDialogState.Add>(awaitItem().dialogState)
        }
    }

    @Test
    fun showRenameSetsRenameDialog() = runTest(testDispatcher) {
        val repo = FakePaymentModeRepository(listOf(mode(1, "Cash")))
        val vm = vm(repo)
        vm.state.test {
            skipItems(1)
            awaitItem()
            vm.onIntent(PaymentModeListIntent.ShowRename(PaymentModeId(1), "Cash"))
            val dialog = awaitItem().dialogState
            assertIs<PaymentModeDialogState.Rename>(dialog)
            assertEquals(PaymentModeId(1), dialog.id)
            assertEquals("Cash", dialog.currentName)
        }
    }

    @Test
    fun showDeleteSetsDeleteConfirmDialog() = runTest(testDispatcher) {
        val repo = FakePaymentModeRepository(listOf(mode(1, "Cash")))
        val vm = vm(repo)
        vm.state.test {
            skipItems(1)
            awaitItem()
            vm.onIntent(PaymentModeListIntent.ShowDelete(PaymentModeId(1), "Cash"))
            val dialog = awaitItem().dialogState
            assertIs<PaymentModeDialogState.DeleteConfirm>(dialog)
            assertEquals("Cash", dialog.name)
        }
    }

    @Test
    fun dismissResetsDialog() = runTest(testDispatcher) {
        val vm = vm()
        vm.state.test {
            skipItems(1)
            awaitItem()
            vm.onIntent(PaymentModeListIntent.ShowAdd)
            assertIs<PaymentModeDialogState.Add>(awaitItem().dialogState)
            vm.onIntent(PaymentModeListIntent.Dismiss)
            assertIs<PaymentModeDialogState.None>(awaitItem().dialogState)
        }
    }

    @Test
    fun createAddsModeTrimmedAndClosesDialog() = runTest(testDispatcher) {
        val repo = FakePaymentModeRepository()
        val vm = vm(repo)
        vm.state.test {
            skipItems(1)
            awaitItem()
            vm.onIntent(PaymentModeListIntent.Create("  Card  "))
            var s = awaitItem()
            while (s.modes.isEmpty()) s = awaitItem()
            assertEquals("Card", s.modes.first().name)
            assertIs<PaymentModeDialogState.None>(s.dialogState)
        }
    }

    @Test
    fun createBlankNameIsIgnored() = runTest(testDispatcher) {
        val repo = FakePaymentModeRepository()
        val vm = vm(repo)
        vm.state.test {
            skipItems(1)
            val initial = awaitItem()
            assertTrue(initial.modes.isEmpty())
            vm.onIntent(PaymentModeListIntent.Create("   "))
            expectNoEvents()
        }
        assertTrue(repo.modes.isEmpty())
    }

    @Test
    fun renameUpdatesModeAndClosesDialog() = runTest(testDispatcher) {
        val repo = FakePaymentModeRepository(listOf(mode(1, "Cash")))
        val vm = vm(repo)
        vm.state.test {
            skipItems(1)
            awaitItem()
            vm.onIntent(PaymentModeListIntent.Rename(PaymentModeId(1), "Wallet"))
            var s = awaitItem()
            while (s.modes.first().name != "Wallet") s = awaitItem()
            assertEquals("Wallet", s.modes.first().name)
        }
    }

    @Test
    fun renameBlankIsIgnored() = runTest(testDispatcher) {
        val repo = FakePaymentModeRepository(listOf(mode(1, "Cash")))
        val vm = vm(repo)
        vm.state.test {
            skipItems(1)
            awaitItem()
            vm.onIntent(PaymentModeListIntent.Rename(PaymentModeId(1), ""))
            expectNoEvents()
        }
        assertEquals("Cash", repo.modes.first().name)
    }

    @Test
    fun deleteRemovesMode() = runTest(testDispatcher) {
        val repo = FakePaymentModeRepository(listOf(mode(1, "Cash"), mode(2, "Card")))
        val vm = vm(repo)
        vm.state.test {
            skipItems(1)
            awaitItem()
            vm.onIntent(PaymentModeListIntent.Delete(PaymentModeId(1)))
            var s = awaitItem()
            while (s.modes.size != 1) s = awaitItem()
            assertEquals("Card", s.modes.single().name)
        }
    }
}

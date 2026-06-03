package com.dv.moneym.feature.settings.wallet

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.dv.moneym.core.model.Account
import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.AccountType
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.testing.FakeAccountRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Instant

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class EditWalletViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() { Dispatchers.setMain(testDispatcher) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    private fun account(id: Long, name: String = "Wallet", color: String? = "#AABBCC") = Account(
        id = AccountId(id),
        name = name,
        type = AccountType.CASH,
        currency = CurrencyCode("EUR"),
        isDefault = false,
        archived = false,
        createdAt = Instant.fromEpochMilliseconds(0),
        updatedAt = Instant.fromEpochMilliseconds(0),
        colorHex = color,
    )

    private fun repoWith(account: Account): FakeAccountRepository =
        FakeAccountRepository().apply { addAll(listOf(account)) }

    private fun vm(id: Long, repo: FakeAccountRepository) =
        EditWalletViewModel(id, repo, SavedStateHandle())

    @Test
    fun loadsExistingAccountIntoState() = runTest(testDispatcher) {
        val repo = repoWith(account(1, name = "Savings", color = "#112233"))
        val vm = vm(1, repo)
        vm.state.test {
            var s = awaitItem()
            while (!s.loaded) s = awaitItem()
            assertEquals("Savings", s.name)
            assertEquals("#112233", s.colorHex)
            assertTrue(s.loaded)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun nameAndColorIntentsUpdateState() = runTest(testDispatcher) {
        val repo = repoWith(account(1))
        val vm = vm(1, repo)
        vm.state.test {
            var s = awaitItem()
            while (!s.loaded) s = awaitItem()
            vm.onIntent(EditWalletIntent.NameChanged("Renamed"))
            assertEquals("Renamed", awaitItem().name)
            vm.onIntent(EditWalletIntent.ColorChanged("#000000"))
            assertEquals("#000000", awaitItem().colorHex)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun saveTrimsNamePersistsAndEmitsDone() = runTest(testDispatcher) {
        val repo = repoWith(account(1))
        val vm = vm(1, repo)
        vm.state.test {
            var s = awaitItem()
            while (!s.loaded) s = awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
        vm.onIntent(EditWalletIntent.NameChanged("  Trimmed  "))
        vm.onIntent(EditWalletIntent.ColorChanged("#FF00FF"))
        vm.effects.test {
            vm.onIntent(EditWalletIntent.Save)
            assertIs<EditWalletEffect.Done>(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        val updated = repo.getById(AccountId(1))!!
        assertEquals("Trimmed", updated.name)
        assertEquals("#FF00FF", updated.colorHex)
    }

    @Test
    fun saveWithBlankNameDoesNothing() = runTest(testDispatcher) {
        val repo = repoWith(account(1, name = "Original"))
        val vm = vm(1, repo)
        vm.state.test {
            var s = awaitItem()
            while (!s.loaded) s = awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
        vm.onIntent(EditWalletIntent.NameChanged("   "))
        vm.effects.test {
            vm.onIntent(EditWalletIntent.Save)
            expectNoEvents()
        }
        assertEquals("Original", repo.getById(AccountId(1))!!.name)
    }
}

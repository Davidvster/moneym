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
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AddWalletViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() { Dispatchers.setMain(testDispatcher) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    private fun account(id: Long, isDefault: Boolean = false) = Account(
        id = AccountId(id),
        name = "A$id",
        type = AccountType.CASH,
        currency = CurrencyCode("EUR"),
        isDefault = isDefault,
        archived = false,
        createdAt = Instant.fromEpochMilliseconds(0),
        updatedAt = Instant.fromEpochMilliseconds(0),
    )

    private fun vm(repo: FakeAccountRepository = FakeAccountRepository()) =
        AddWalletViewModel(repo, SavedStateHandle())

    @Test
    fun inputSettersUpdateStateFlows() = runTest(testDispatcher) {
        val vm = vm()
        vm.setName("Cash")
        vm.setCurrency("USD")
        vm.setColor("#FF0000")
        assertEquals("Cash", vm.name.value)
        assertEquals("USD", vm.selectedCurrency.value)
        assertEquals("#FF0000", vm.colorHex.value)
    }

    @Test
    fun searchFiltersByCodeOrName() = runTest(testDispatcher) {
        val vm = vm()
        vm.filteredCurrencies.test {
            skipItems(1)
            vm.setSearchQuery("usd")
            val filtered = awaitItem()
            assertTrue(filtered.isNotEmpty())
            assertTrue(filtered.all { it.code.lowercase().contains("usd") || it.name.lowercase().contains("usd") })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun blankSearchYieldsAllCurrencies() = runTest(testDispatcher) {
        val vm = vm()
        assertTrue(vm.filteredCurrencies.value.isNotEmpty())
        assertEquals(vm.filteredCurrencies.value.size, vm.filteredCurrencies.value.distinctBy { it.code }.size)
    }

    @Test
    fun addWalletInsertsAndClearsInput() = runTest(testDispatcher) {
        val repo = FakeAccountRepository()
        val vm = vm(repo)
        vm.setColor("#123456")
        vm.setName("Cash")
        vm.setCurrency("EUR")
        vm.addWallet("Cash", "EUR")
        advanceUntilIdle()

        assertEquals(1, repo.accounts.size)
        val inserted = repo.accounts.single()
        assertEquals("Cash", inserted.name)
        assertEquals(CurrencyCode("EUR"), inserted.currency)
        assertEquals(AccountType.CASH, inserted.type)
        assertEquals("#123456", inserted.colorHex)
        assertTrue(inserted.isDefault) // first account becomes default
        assertEquals("", vm.name.value)
        assertEquals("", vm.selectedCurrency.value)
        assertNull(vm.colorHex.value)
    }

    @Test
    fun addWalletNotDefaultWhenAccountsExist() = runTest(testDispatcher) {
        val repo = FakeAccountRepository()
        repo.addAll(listOf(account(1, isDefault = true)))
        val vm = vm(repo)
        vm.addWallet("Second", "USD")
        advanceUntilIdle()

        val added = repo.accounts.first { it.name == "Second" }
        assertTrue(!added.isDefault)
    }
}

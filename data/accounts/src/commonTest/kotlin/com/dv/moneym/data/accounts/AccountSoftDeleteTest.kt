package com.dv.moneym.data.accounts

import app.cash.turbine.test
import com.dv.moneym.core.model.Account
import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.AccountType
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.testing.FakeAccountRepository
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.time.Instant

class AccountSoftDeleteTest {

    private fun account(id: Long) = Account(
        id = AccountId(id),
        name = "Acc $id",
        type = AccountType.CASH,
        currency = CurrencyCode("EUR"),
        isDefault = false,
        archived = false,
        createdAt = Instant.fromEpochMilliseconds(0),
        updatedAt = Instant.fromEpochMilliseconds(0),
    )

    @Test
    fun deleteHidesFromObserveAllButExportYieldsTombstone() = runTest {
        val repo = FakeAccountRepository().apply { addAll(listOf(account(1), account(2))) }

        repo.delete(AccountId(1))

        repo.observeAll().test {
            awaitItem().map { it.id.value } shouldBe listOf(2L)
            cancelAndIgnoreRemainingEvents()
        }
        repo.count() shouldBe 1L

        val rows = repo.exportForSync()
        rows.size shouldBe 2
        rows.single { it.id == 1L }.deleted shouldBe true
        rows.single { it.id == 2L }.deleted shouldBe false
    }
}

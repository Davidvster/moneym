package com.dv.moneym.feature.aianalysis.usecase

import com.dv.moneym.core.model.Account
import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.AccountType
import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.Money
import com.dv.moneym.core.model.Transaction
import com.dv.moneym.core.model.TransactionId
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.testing.FakeAccountRepository
import com.dv.moneym.core.testing.FakeBudgetRepository
import com.dv.moneym.core.testing.FakeCategoryRepository
import com.dv.moneym.core.testing.FakeTransactionRepository
import com.dv.moneym.core.testing.FixedClock
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Instant

class BuildFinanceSnapshotUseCaseTest {

    private val epoch = Instant.fromEpochMilliseconds(0)
    private val clock = FixedClock(Instant.parse("2026-05-15T12:00:00Z"))
    private val txnRepo = FakeTransactionRepository()
    private val accountRepo = FakeAccountRepository()
    private val catRepo = FakeCategoryRepository()
    private val budgetRepo = FakeBudgetRepository()

    private val useCase = BuildFinanceSnapshotUseCase(
        transactionRepository = txnRepo,
        accountRepository = accountRepo,
        categoryRepository = catRepo,
        budgetRepository = budgetRepo,
        clock = clock,
    )

    private fun account() = Account(
        id = AccountId(1),
        name = "Main",
        type = AccountType.CASH,
        currency = CurrencyCode("EUR"),
        isDefault = true,
        archived = false,
        createdAt = epoch,
        updatedAt = epoch,
    )

    private fun category(id: Long, name: String) = Category(
        id = CategoryId(id),
        name = name,
        iconKey = "icon",
        colorHex = "#000000",
        isUserCreated = false,
        archived = false,
        createdAt = epoch,
        updatedAt = epoch,
        type = TransactionType.EXPENSE,
    )

    private fun txn(
        type: TransactionType,
        amount: Long,
        date: LocalDate,
        categoryId: Long,
    ) = Transaction(
        id = TransactionId(0),
        type = type,
        amount = Money(amount, CurrencyCode("EUR")),
        occurredOn = date,
        note = null,
        categoryId = CategoryId(categoryId),
        accountId = AccountId(1),
        createdAt = epoch,
        updatedAt = epoch,
    )

    @Test
    fun snapshotContainsTotalsTopCategoriesAndHistory() = runTest {
        accountRepo.addAll(listOf(account()))
        catRepo.addAll(listOf(category(1, "Groceries"), category(2, "Transport")))

        // selected month: May 2026
        txnRepo.upsert(txn(TransactionType.INCOME, 200000, LocalDate(2026, 5, 1), 1))
        txnRepo.upsert(txn(TransactionType.EXPENSE, 60000, LocalDate(2026, 5, 2), 1))
        txnRepo.upsert(txn(TransactionType.EXPENSE, 30000, LocalDate(2026, 5, 3), 1))
        txnRepo.upsert(txn(TransactionType.EXPENSE, 10000, LocalDate(2026, 5, 4), 2))

        // previous months
        txnRepo.upsert(txn(TransactionType.EXPENSE, 50000, LocalDate(2026, 4, 10), 1))
        txnRepo.upsert(txn(TransactionType.EXPENSE, 20000, LocalDate(2026, 3, 10), 1))

        val snapshot = useCase(2026, 5)

        assertTrue(snapshot.contains("EUR"), snapshot)
        assertTrue(snapshot.contains("Income: 2000.00 EUR"), snapshot)
        // expense total 60000+30000+10000 = 100000 minor => 1000.00
        assertTrue(snapshot.contains("Expense: 1000.00 EUR"), snapshot)
        // Groceries (900.00) ranks above Transport (100.00)
        val groceriesIndex = snapshot.indexOf("Groceries")
        val transportIndex = snapshot.indexOf("Transport")
        assertTrue(groceriesIndex in 0 until transportIndex, snapshot)
        assertTrue(snapshot.contains("Groceries: 900.00 EUR"), snapshot)
        // recent history lines for the prior 3 months
        assertTrue(snapshot.contains("2026-04: 500.00 EUR"), snapshot)
        assertTrue(snapshot.contains("2026-03: 200.00 EUR"), snapshot)
        assertTrue(snapshot.contains("2026-02: 0.00 EUR"), snapshot)
    }

    @Test
    fun snapshotIncludesActiveBudgetStatus() = runTest {
        accountRepo.addAll(listOf(account()))
        catRepo.addAll(listOf(category(1, "Groceries")))
        budgetRepo.addAll(
            listOf(
                com.dv.moneym.core.model.Budget(
                    id = com.dv.moneym.core.model.BudgetId(1),
                    name = "Food",
                    amount = Money(100000, CurrencyCode("EUR")),
                    categoryId = CategoryId(1),
                    accountId = AccountId(1),
                    periodType = com.dv.moneym.core.model.BudgetPeriodType.MONTHLY,
                    startYearMonth = com.dv.moneym.core.model.YearMonth(2026, 5),
                    recurringMonths = com.dv.moneym.core.model.Budget.UNLIMITED,
                    createdAt = epoch,
                    updatedAt = epoch,
                ),
            ),
        )
        txnRepo.upsert(txn(TransactionType.EXPENSE, 40000, LocalDate(2026, 5, 2), 1))

        val snapshot = useCase(2026, 5)

        assertTrue(snapshot.contains("Food"), snapshot)
        assertTrue(snapshot.contains("400.00 EUR / 1000.00 EUR"), snapshot)
    }
}

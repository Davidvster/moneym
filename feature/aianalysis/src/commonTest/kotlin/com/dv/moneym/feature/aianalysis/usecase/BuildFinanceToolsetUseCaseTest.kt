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
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class BuildFinanceToolsetUseCaseTest {

    private val epoch = Instant.fromEpochMilliseconds(0)
    private val clock = FixedClock(Instant.parse("2026-05-15T12:00:00Z"))
    private val txnRepo = FakeTransactionRepository()
    private val accountRepo = FakeAccountRepository()
    private val catRepo = FakeCategoryRepository()
    private val budgetRepo = FakeBudgetRepository()

    private val useCase = BuildFinanceToolsetUseCase(
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

    private fun txn(type: TransactionType, amount: Long, date: LocalDate, categoryId: Long) = Transaction(
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

    private fun toolset() = useCase(2026, 5).associateBy { it.name }

    @Test
    fun builds_expected_tool_definitions() {
        val tools = useCase(2026, 5)
        assertEquals(
            listOf("totals", "spendingByCategory", "topExpenses", "comparePreviousMonths"),
            tools.map { it.name },
        )
        assertEquals("""{"n":"integer"}""", tools.first { it.name == "topExpenses" }.paramsSchema)
        assertTrue(tools.all { it.description.isNotBlank() })
    }

    @Test
    fun totals_reports_income_expense_net() = runTest {
        accountRepo.addAll(listOf(account()))
        catRepo.addAll(listOf(category(1, "Food")))
        txnRepo.upsert(txn(TransactionType.INCOME, 200000, LocalDate(2026, 5, 1), 1))
        txnRepo.upsert(txn(TransactionType.EXPENSE, 60000, LocalDate(2026, 5, 2), 1))

        val out = toolset().getValue("totals").invoke(emptyMap())
        assertEquals("Income 2000.00 EUR, Expense 600.00 EUR, Net 1400.00 EUR", out)
    }

    @Test
    fun totals_uses_default_currency_when_no_accounts() = runTest {
        txnRepo.upsert(txn(TransactionType.EXPENSE, 100, LocalDate(2026, 5, 2), 1))
        val out = toolset().getValue("totals").invoke(emptyMap())
        assertTrue(out.contains("EUR"), out)
    }

    @Test
    fun spending_by_category_groups_and_sorts() = runTest {
        accountRepo.addAll(listOf(account()))
        catRepo.addAll(listOf(category(1, "Food"), category(2, "Travel")))
        txnRepo.upsert(txn(TransactionType.EXPENSE, 30000, LocalDate(2026, 5, 2), 1))
        txnRepo.upsert(txn(TransactionType.EXPENSE, 70000, LocalDate(2026, 5, 3), 2))

        val out = toolset().getValue("spendingByCategory").invoke(emptyMap())
        assertEquals("Travel 700.00 EUR; Food 300.00 EUR", out)
    }

    @Test
    fun spending_by_category_unknown_label_and_no_data() = runTest {
        accountRepo.addAll(listOf(account()))
        val empty = toolset().getValue("spendingByCategory").invoke(emptyMap())
        assertEquals("No data.", empty)

        txnRepo.upsert(txn(TransactionType.EXPENSE, 5000, LocalDate(2026, 5, 2), 99))
        val unknown = toolset().getValue("spendingByCategory").invoke(emptyMap())
        assertTrue(unknown.contains("Other 50.00 EUR"), unknown)
    }

    @Test
    fun top_expenses_respects_n_param_and_default() = runTest {
        accountRepo.addAll(listOf(account()))
        catRepo.addAll(listOf(category(1, "Food")))
        (1..6).forEach { i ->
            txnRepo.upsert(txn(TransactionType.EXPENSE, (i * 1000).toLong(), LocalDate(2026, 5, i), 1))
        }
        val defaultTop = toolset().getValue("topExpenses").invoke(emptyMap())
        assertEquals(5, defaultTop.split(";").size)

        val limited = toolset().getValue("topExpenses").invoke(mapOf("n" to "2"))
        val parts = limited.split(";")
        assertEquals(2, parts.size)
        assertTrue(parts.first().contains("60.00 EUR"), limited)
    }

    @Test
    fun top_expenses_no_data() = runTest {
        accountRepo.addAll(listOf(account()))
        val out = toolset().getValue("topExpenses").invoke(emptyMap())
        assertEquals("No data.", out)
    }

    @Test
    fun compare_previous_months_lists_three_prior_months() = runTest {
        accountRepo.addAll(listOf(account()))
        catRepo.addAll(listOf(category(1, "Food")))
        txnRepo.upsert(txn(TransactionType.EXPENSE, 40000, LocalDate(2026, 4, 10), 1))
        txnRepo.upsert(txn(TransactionType.EXPENSE, 20000, LocalDate(2026, 3, 10), 1))

        val out = toolset().getValue("comparePreviousMonths").invoke(emptyMap())
        val lines = out.split("; ")
        assertEquals(3, lines.size)
        assertTrue(out.contains("2026-04 400.00 EUR"), out)
        assertTrue(out.contains("2026-03 200.00 EUR"), out)
        assertTrue(out.contains("2026-02 0.00 EUR"), out)
    }
}

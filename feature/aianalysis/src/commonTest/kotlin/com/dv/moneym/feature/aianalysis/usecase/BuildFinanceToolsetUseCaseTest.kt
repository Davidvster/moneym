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

    private fun txn(
        type: TransactionType,
        amount: Long,
        date: LocalDate,
        categoryId: Long,
        note: String? = null,
    ) = Transaction(
        id = TransactionId(0),
        type = type,
        amount = Money(amount, CurrencyCode("EUR")),
        occurredOn = date,
        note = note,
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
            listOf(
                "totals",
                "spendingByCategory",
                "topExpenses",
                "comparePreviousMonths",
                "transactions",
                "dataRange",
                "categories",
                "accounts",
                "searchTransactions",
                "totalsForRange",
                "budgets",
            ),
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
    fun top_expenses_includes_note() = runTest {
        accountRepo.addAll(listOf(account()))
        catRepo.addAll(listOf(category(1, "Food")))
        txnRepo.upsert(txn(TransactionType.EXPENSE, 5000, LocalDate(2026, 5, 2), 1, note = "team lunch"))

        val out = toolset().getValue("topExpenses").invoke(emptyMap())
        assertTrue(out.contains("note: team lunch"), out)
    }

    @Test
    fun transactions_lists_notes_and_filters_by_query() = runTest {
        accountRepo.addAll(listOf(account()))
        catRepo.addAll(listOf(category(1, "Food"), category(2, "Travel")))
        txnRepo.upsert(txn(TransactionType.EXPENSE, 5000, LocalDate(2026, 5, 2), 1, note = "team lunch"))
        txnRepo.upsert(txn(TransactionType.EXPENSE, 8000, LocalDate(2026, 5, 3), 2, note = "flight to Berlin"))

        val all = toolset().getValue("transactions").invoke(emptyMap())
        assertTrue(all.contains("note: team lunch"), all)
        assertTrue(all.contains("note: flight to Berlin"), all)

        val filtered = toolset().getValue("transactions").invoke(mapOf("q" to "berlin"))
        assertTrue(filtered.contains("flight to Berlin"), filtered)
        assertTrue(!filtered.contains("team lunch"), filtered)
    }

    @Test
    fun transactions_no_data() = runTest {
        accountRepo.addAll(listOf(account()))
        val out = toolset().getValue("transactions").invoke(emptyMap())
        assertEquals("No data.", out)
    }

    @Test
    fun search_transactions_spans_all_history_and_filters() = runTest {
        accountRepo.addAll(listOf(account()))
        catRepo.addAll(listOf(category(1, "Food"), category(2, "Travel")))
        // outside the selected month (May 2026) on purpose
        txnRepo.upsert(txn(TransactionType.EXPENSE, 5000, LocalDate(2025, 1, 2), 1, note = "team lunch"))
        txnRepo.upsert(txn(TransactionType.EXPENSE, 8000, LocalDate(2024, 7, 3), 2, note = "flight to Berlin"))

        val all = toolset().getValue("searchTransactions").invoke(emptyMap())
        assertTrue(all.contains("team lunch"), all)
        assertTrue(all.contains("flight to Berlin"), all)

        val byText = toolset().getValue("searchTransactions").invoke(mapOf("q" to "berlin"))
        assertTrue(byText.contains("flight to Berlin"), byText)
        assertTrue(!byText.contains("team lunch"), byText)

        val byRange = toolset().getValue("searchTransactions")
            .invoke(mapOf("from" to "2025-01-01", "to" to "2025-12-31"))
        assertTrue(byRange.contains("team lunch"), byRange)
        assertTrue(!byRange.contains("flight to Berlin"), byRange)

        val byType = toolset().getValue("searchTransactions").invoke(mapOf("type" to "income"))
        assertEquals("No data.", byType)
    }

    @Test
    fun totals_for_range_sums_across_history() = runTest {
        accountRepo.addAll(listOf(account()))
        catRepo.addAll(listOf(category(1, "Food")))
        txnRepo.upsert(txn(TransactionType.INCOME, 100000, LocalDate(2025, 1, 2), 1))
        txnRepo.upsert(txn(TransactionType.EXPENSE, 40000, LocalDate(2025, 6, 3), 1))

        val out = toolset().getValue("totalsForRange")
            .invoke(mapOf("from" to "2025-01-01", "to" to "2025-12-31"))
        assertTrue(out.contains("Income 1000.00 EUR"), out)
        assertTrue(out.contains("Expense 400.00 EUR"), out)
        assertTrue(out.contains("Net 600.00 EUR"), out)
    }

    @Test
    fun data_range_reports_span_and_count() = runTest {
        accountRepo.addAll(listOf(account()))
        catRepo.addAll(listOf(category(1, "Food")))
        txnRepo.upsert(txn(TransactionType.EXPENSE, 5000, LocalDate(2024, 7, 3), 1))
        txnRepo.upsert(txn(TransactionType.EXPENSE, 8000, LocalDate(2025, 1, 2), 1))

        val out = toolset().getValue("dataRange").invoke(emptyMap())
        assertTrue(out.contains("2024-07-03"), out)
        assertTrue(out.contains("2025-01-02"), out)
        assertTrue(out.contains("2 total"), out)
    }

    @Test
    fun categories_and_accounts_list_metadata() = runTest {
        accountRepo.addAll(listOf(account()))
        catRepo.addAll(listOf(category(1, "Food")))

        val cats = toolset().getValue("categories").invoke(emptyMap())
        assertTrue(cats.contains("Food (expense)"), cats)

        val accs = toolset().getValue("accounts").invoke(emptyMap())
        assertTrue(accs.contains("Main"), accs)
        assertTrue(accs.contains("EUR"), accs)
        assertTrue(accs.contains("default"), accs)
    }

    @Test
    fun budgets_reports_active_budget_spent_and_limit() = runTest {
        accountRepo.addAll(listOf(account()))
        catRepo.addAll(listOf(category(1, "Food")))
        budgetRepo.addAll(
            listOf(
                com.dv.moneym.core.model.Budget(
                    id = com.dv.moneym.core.model.BudgetId(1),
                    name = "Groceries",
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

        val out = toolset().getValue("budgets").invoke(emptyMap())
        assertTrue(out.contains("Groceries (Food): 400.00 EUR / 1000.00 EUR"), out)
    }

    @Test
    fun budgets_no_data_when_none_active() = runTest {
        accountRepo.addAll(listOf(account()))
        val out = toolset().getValue("budgets").invoke(emptyMap())
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

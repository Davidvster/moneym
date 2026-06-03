package com.dv.moneym.feature.overview.usecase

import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.Icon
import com.dv.moneym.core.model.Money
import com.dv.moneym.core.model.Transaction
import com.dv.moneym.core.model.TransactionId
import com.dv.moneym.core.model.TransactionType
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class BuildCategoryBreakdownUseCaseTest {

    private val epoch = Instant.fromEpochMilliseconds(0)
    private val useCase = BuildCategoryBreakdownUseCase()

    private fun category(id: Long, name: String, color: String = "#112233") = Category(
        id = CategoryId(id),
        name = name,
        iconKey = Icon.Basket.key,
        colorHex = color,
        isUserCreated = true,
        archived = false,
        createdAt = epoch,
        updatedAt = epoch,
    )

    private fun txn(
        id: Long,
        type: TransactionType,
        amountMinor: Long,
        categoryId: Long,
    ) = Transaction(
        id = TransactionId(id),
        type = type,
        amount = Money(amountMinor, CurrencyCode("EUR")),
        occurredOn = LocalDate(2026, 5, 10),
        note = null,
        categoryId = CategoryId(categoryId),
        accountId = AccountId(1),
        createdAt = epoch,
        updatedAt = epoch,
    )

    @Test
    fun empty_input_returns_empty() {
        val result = useCase(emptyList(), emptyMap(), TransactionType.EXPENSE, 10, 1, true)
        assertTrue(result.isEmpty())
    }

    @Test
    fun filters_by_type_and_groups_by_category() {
        val cat1 = category(1, "Food")
        val cat2 = category(2, "Travel")
        val txns = listOf(
            txn(1, TransactionType.EXPENSE, 6000, 1),
            txn(2, TransactionType.EXPENSE, 4000, 1),
            txn(3, TransactionType.EXPENSE, 5000, 2),
            txn(4, TransactionType.INCOME, 99999, 1),
        )
        val result = useCase(
            txns,
            mapOf(cat1.id to cat1, cat2.id to cat2),
            TransactionType.EXPENSE,
            elapsedDays = 10,
            elapsedMonths = 1,
            isMonthMode = true,
        )
        assertEquals(2, result.size)
        val food = result.first { it.categoryName == "Food" }
        val travel = result.first { it.categoryName == "Travel" }
        assertEquals(100.0, food.amount)
        assertEquals(50.0, travel.amount)
    }

    @Test
    fun sorted_by_amount_descending() {
        val cat1 = category(1, "Small")
        val cat2 = category(2, "Big")
        val txns = listOf(
            txn(1, TransactionType.EXPENSE, 1000, 1),
            txn(2, TransactionType.EXPENSE, 9000, 2),
        )
        val result = useCase(
            txns,
            mapOf(cat1.id to cat1, cat2.id to cat2),
            TransactionType.EXPENSE,
            10, 1, true,
        )
        assertEquals("Big", result.first().categoryName)
        assertEquals("Small", result.last().categoryName)
    }

    @Test
    fun percent_computed_from_total_and_zero_when_total_zero() {
        val cat1 = category(1, "Food")
        val txns = listOf(
            txn(1, TransactionType.EXPENSE, 7500, 1),
            txn(2, TransactionType.EXPENSE, 2500, 1),
        )
        val withTotal = useCase(txns, mapOf(cat1.id to cat1), TransactionType.EXPENSE, 10, 1, true)
        assertEquals(100, withTotal.first().percent)

        val zeroAmount = listOf(txn(3, TransactionType.EXPENSE, 0, 1))
        val zeroTotal = useCase(zeroAmount, mapOf(cat1.id to cat1), TransactionType.EXPENSE, 10, 1, true)
        assertEquals(0, zeroTotal.first().percent)
    }

    @Test
    fun missing_category_uses_placeholder_name_and_default_color() {
        val txns = listOf(txn(1, TransactionType.EXPENSE, 1000, 99))
        val result = useCase(txns, emptyMap(), TransactionType.EXPENSE, 10, 1, true)
        assertEquals("—", result.first().categoryName)
        assertEquals(colorHexToLong("#8A8A8A"), result.first().categoryColor)
        assertEquals(Icon.Dots, result.first().categoryIcon)
    }

    @Test
    fun income_default_color_differs_from_expense() {
        val txns = listOf(txn(1, TransactionType.INCOME, 1000, 99))
        val result = useCase(txns, emptyMap(), TransactionType.INCOME, 10, 1, true)
        assertEquals(colorHexToLong("#4A7A56"), result.first().categoryColor)
    }

    @Test
    fun avg_per_day_uses_elapsed_days_and_per_month_only_in_year_mode() {
        val cat1 = category(1, "Food")
        val txns = listOf(txn(1, TransactionType.EXPENSE, 30000, 1))

        val monthMode = useCase(txns, mapOf(cat1.id to cat1), TransactionType.EXPENSE, 10, 6, true)
        assertEquals(30.0, monthMode.first().avgPerDay)
        assertEquals(0.0, monthMode.first().avgPerMonth)

        val yearMode = useCase(txns, mapOf(cat1.id to cat1), TransactionType.EXPENSE, 10, 6, false)
        assertEquals(50.0, yearMode.first().avgPerMonth)
    }
}

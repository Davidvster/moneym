package com.dv.moneym.core.model

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class Budget(
    val id: BudgetId,
    val name: String,
    val amount: Money,
    val categoryId: CategoryId?,
    val accountId: AccountId,
    val periodType: BudgetPeriodType,
    val startYearMonth: YearMonth,
    val recurringMonths: Int?,
    @Serializable(with = InstantSerializer::class) val createdAt: Instant,
    @Serializable(with = InstantSerializer::class) val updatedAt: Instant,
) {
    fun isActiveIn(ym: YearMonth): Boolean {
        if (ym < startYearMonth) return false
        return when {
            recurringMonths == null -> ym == startYearMonth
            recurringMonths == UNLIMITED -> true
            recurringMonths > 0 -> monthsSinceStart(ym) < recurringMonths
            else -> false
        }
    }

    private fun monthsSinceStart(ym: YearMonth): Int =
        (ym.year - startYearMonth.year) * 12 + (ym.monthNumber - startYearMonth.monthNumber)

    companion object {
        const val UNLIMITED: Int = -1
    }
}

enum class BudgetPeriodType { MONTHLY }

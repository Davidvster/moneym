package com.dv.moneym.feature.overview.usecase

import com.dv.moneym.core.model.Transaction
import com.dv.moneym.core.model.TransactionType
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number

internal data class CumulativeSeries(
    val dailyTotals: List<Double>,
    val cumulativeTotals: List<Double>,
    val todayIndex: Int,
)

class BuildCumulativeSeriesUseCase {
    internal operator fun invoke(
        periodTxns: List<Transaction>,
        year: Int,
        month: Int,
        today: LocalDate,
    ): CumulativeSeries {
        val days = daysInMonth(year, month)
        val isCurrentMonth = year == today.year && month == today.month.number

        val rawDaily = (1..days).map { day ->
            periodTxns
                .filter { it.type == TransactionType.EXPENSE && it.occurredOn.day == day }
                .sumOf { it.amount.minorUnits }
                .toDouble() / 100.0
        }

        var running = 0.0
        val cumulative = rawDaily.map { v -> running += v; running }

        val todayIndex = if (isCurrentMonth) {
            (today.day - 1).coerceIn(0, days - 1)
        } else {
            days - 1
        }

        return CumulativeSeries(
            dailyTotals = rawDaily,
            cumulativeTotals = cumulative,
            todayIndex = todayIndex,
        )
    }

    internal fun monthlyTotals(
        accountFilteredTransactions: List<Transaction>,
        year: Int,
        type: TransactionType = TransactionType.EXPENSE,
    ): List<Double> = (1..12).map { m ->
        accountFilteredTransactions
            .filter {
                it.type == type &&
                        it.occurredOn.year == year &&
                        it.occurredOn.month.number == m
            }
            .sumOf { it.amount.minorUnits }
            .toDouble() / 100.0
    }

    internal fun monthlyNetTotals(
        accountFilteredTransactions: List<Transaction>,
        year: Int,
    ): List<Double> {
        val income = monthlyTotals(accountFilteredTransactions, year, TransactionType.INCOME)
        val expense = monthlyTotals(accountFilteredTransactions, year, TransactionType.EXPENSE)
        return income.zip(expense) { i, e -> i - e }
    }
}

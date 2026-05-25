package com.dv.moneym.feature.overview.usecase

import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.Icon
import com.dv.moneym.core.model.Transaction
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.feature.overview.CategoryTrend
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number

class BuildCategoryTrendsUseCase {

    internal fun daily(
        periodTxns: List<Transaction>,
        catMap: Map<CategoryId, Category>,
        days: Int,
        elapsedDays: Int = 1,
    ): List<CategoryTrend> {
        val expenseTxns = periodTxns.filter { it.type == TransactionType.EXPENSE }
        return expenseTxns
            .groupBy { it.categoryId }
            .map { (catId, txns) ->
                val cat = catMap[catId]
                val series = (1..days).map { day ->
                    txns.filter { it.occurredOn.day == day }
                        .sumOf { it.amount.minorUnits }
                        .toDouble() / 100.0
                }
                val totalAmount = txns.sumOf { it.amount.minorUnits }.toDouble() / 100.0
                CategoryTrend(
                    categoryName = cat?.name ?: "—",
                    categoryColor = colorHexToLong(cat?.colorHex ?: "#8A8A8A"),
                    categoryIcon = Icon.fromKeyOrDefault(cat?.iconKey ?: Icon.Dots.key),
                    totalAmount = totalAmount,
                    txCount = txns.size,
                    series = series,
                    avgPerDay = totalAmount / elapsedDays,
                )
            }
            .sortedByDescending { it.totalAmount }
    }

    internal fun monthly(
        allTransactions: List<Transaction>,
        catMap: Map<CategoryId, Category>,
        year: Int,
        elapsedMonths: Int = 1,
        elapsedDays: Int = 1,
    ): List<CategoryTrend> {
        val yearExpenses = allTransactions.filter {
            it.type == TransactionType.EXPENSE && it.occurredOn.year == year
        }
        return yearExpenses
            .groupBy { it.categoryId }
            .map { (catId, txns) ->
                val cat = catMap[catId]
                val series = (1..12).map { m ->
                    txns.filter { it.occurredOn.month.number == m }
                        .sumOf { it.amount.minorUnits }
                        .toDouble() / 100.0
                }
                val totalAmount = txns.sumOf { it.amount.minorUnits }.toDouble() / 100.0
                CategoryTrend(
                    categoryName = cat?.name ?: "—",
                    categoryColor = colorHexToLong(cat?.colorHex ?: "#8A8A8A"),
                    categoryIcon = Icon.fromKeyOrDefault(cat?.iconKey ?: Icon.Dots.key),
                    totalAmount = totalAmount,
                    txCount = txns.size,
                    series = series,
                    avgPerDay = totalAmount / elapsedDays,
                    avgPerMonth = totalAmount / elapsedMonths,
                )
            }
            .sortedByDescending { it.totalAmount }
    }

    internal fun range(
        periodTxns: List<Transaction>,
        catMap: Map<CategoryId, Category>,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<CategoryTrend> {
        val expenseTxns = periodTxns.filter { it.type == TransactionType.EXPENSE }
        val totalDays =
            ((endDate.toEpochDays() - startDate.toEpochDays()).toInt() + 1).coerceAtLeast(1)
        val useDayBuckets = totalDays <= 31

        return expenseTxns
            .groupBy { it.categoryId }
            .map { (catId, txns) ->
                buildRangeTrend(
                    catId,
                    txns,
                    catMap,
                    startDate,
                    totalDays,
                    useDayBuckets,
                    endDate
                )
            }
            .sortedByDescending { it.totalAmount }
    }

    private fun buildRangeTrend(
        catId: CategoryId,
        txns: List<Transaction>,
        catMap: Map<CategoryId, Category>,
        startDate: LocalDate,
        totalDays: Int,
        useDayBuckets: Boolean,
        endDate: LocalDate,
    ): CategoryTrend {
        val cat = catMap[catId]
        val series = if (useDayBuckets) {
            (0 until totalDays).map { offset ->
                val date = LocalDate.fromEpochDays(startDate.toEpochDays() + offset)
                txns.filter { it.occurredOn == date }
                    .sumOf { it.amount.minorUnits }
                    .toDouble() / 100.0
            }
        } else {
            val startEpochMonth = startDate.year * 12 + (startDate.month.number - 1)
            val endEpochMonth = endDate.year * 12 + (endDate.month.number - 1)
            (startEpochMonth..endEpochMonth).map { epochMonth ->
                val y = epochMonth / 12
                val m = epochMonth % 12 + 1
                txns.filter { it.occurredOn.year == y && it.occurredOn.month.number == m }
                    .sumOf { it.amount.minorUnits }
                    .toDouble() / 100.0
            }
        }
        return CategoryTrend(
            categoryName = cat?.name ?: "—",
            categoryColor = colorHexToLong(cat?.colorHex ?: "#8A8A8A"),
            categoryIcon = Icon.fromKeyOrDefault(cat?.iconKey ?: Icon.Dots.key),
            totalAmount = txns.sumOf { it.amount.minorUnits }.toDouble() / 100.0,
            txCount = txns.size,
            series = series,
        )
    }
}

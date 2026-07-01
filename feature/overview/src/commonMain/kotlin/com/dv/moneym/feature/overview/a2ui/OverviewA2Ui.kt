package com.dv.moneym.feature.overview.a2ui

import com.dv.moneym.core.model.Icon
import com.dv.moneym.feature.overview.CategorySpend
import com.dv.moneym.feature.overview.OverviewPeriod
import com.dv.moneym.feature.overview.page.OverviewPageUiState
import com.dv.moneym.feature.overview.usecase.BudgetProgress
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull

internal data class OverviewA2UiNode(
    val type: String,
    val title: String? = null,
    val text: String? = null,
    val label: String? = null,
    val binding: String? = null,
    val value: Double? = null,
    val limit: Int? = null,
    val children: List<OverviewA2UiNode> = emptyList(),
)

internal data class OverviewWidgetContext(
    val currencyCode: String,
    val labels: Map<String, String>,
    val moneyValues: Map<String, Double>,
    val numberValues: Map<String, Double>,
    val listValues: Map<String, List<OverviewWidgetListItem>>,
) {
    val allowedBindings: Set<String> =
        labels.keys + moneyValues.keys + numberValues.keys + listValues.keys
}

internal data class OverviewWidgetListItem(
    val label: String,
    val value: Double,
    val percent: Int,
    val colorHex: Long? = null,
    val icon: Icon? = null,
)

internal sealed interface OverviewA2UiValidationResult {
    data class Valid(val node: OverviewA2UiNode) : OverviewA2UiValidationResult
    data class Invalid(val reason: OverviewA2UiValidationError) : OverviewA2UiValidationResult
}

internal enum class OverviewA2UiValidationError {
    InvalidJson,
    UnknownComponent,
    UnknownBinding,
    UnsafeField,
    TooDeep,
    TooManyChildren,
    TooManyNodes,
    TextTooLong,
}

internal class OverviewA2UiValidator(
    private val allowedBindings: Set<String> = OverviewWidgetCatalog.allowedBindingKeys,
) {
    fun validate(rawJson: String): OverviewA2UiValidationResult {
        val element = runCatching { json.parseToJsonElement(stripMarkdownFence(rawJson)) }.getOrNull()
            ?: return OverviewA2UiValidationResult.Invalid(OverviewA2UiValidationError.InvalidJson)
        val root = element as? JsonObject
            ?: return OverviewA2UiValidationResult.Invalid(OverviewA2UiValidationError.InvalidJson)
        var totalNodes = 0

        fun parseNode(obj: JsonObject, depth: Int): Result<OverviewA2UiNode> {
            if (depth > MAX_DEPTH) return Result.failure(ValidationException(OverviewA2UiValidationError.TooDeep))
            totalNodes += 1
            if (totalNodes > MAX_TOTAL_NODES) {
                return Result.failure(ValidationException(OverviewA2UiValidationError.TooManyNodes))
            }
            if (obj.keys.any { it in unsafeKeys }) {
                return Result.failure(ValidationException(OverviewA2UiValidationError.UnsafeField))
            }

            val type = obj.string("type")
                ?: return Result.failure(ValidationException(OverviewA2UiValidationError.UnknownComponent))
            if (type !in OverviewWidgetCatalog.allowedComponentTypes) {
                return Result.failure(ValidationException(OverviewA2UiValidationError.UnknownComponent))
            }
            val binding = obj.string("binding")
            if (binding != null && binding !in allowedBindings) {
                return Result.failure(ValidationException(OverviewA2UiValidationError.UnknownBinding))
            }
            val childrenElement = obj["children"] as? JsonArray
            if ((childrenElement?.size ?: 0) > MAX_CHILDREN) {
                return Result.failure(ValidationException(OverviewA2UiValidationError.TooManyChildren))
            }
            val children = childrenElement.orEmpty().map { child ->
                parseNode(
                    child as? JsonObject
                        ?: return Result.failure(ValidationException(OverviewA2UiValidationError.InvalidJson)),
                    depth + 1,
                ).getOrElse { return Result.failure(it) }
            }

            val title = obj.string("title")
            val text = obj.string("text")
            val label = obj.string("label")
            if (listOfNotNull(title, text, label).any { it.length > MAX_TEXT_LENGTH }) {
                return Result.failure(ValidationException(OverviewA2UiValidationError.TextTooLong))
            }
            return Result.success(
                OverviewA2UiNode(
                    type = type,
                    title = title,
                    text = text,
                    label = label,
                    binding = binding,
                    value = obj.double("value"),
                    limit = obj.int("limit")?.coerceIn(1, MAX_LIST_ITEMS),
                    children = children,
                ),
            )
        }

        return parseNode(root, depth = 1)
            .fold(
                onSuccess = { OverviewA2UiValidationResult.Valid(it) },
                onFailure = {
                    OverviewA2UiValidationResult.Invalid(
                        (it as? ValidationException)?.error ?: OverviewA2UiValidationError.InvalidJson,
                    )
                },
            )
    }

    companion object {
        const val MAX_DEPTH = 5
        const val MAX_CHILDREN = 8
        const val MAX_TOTAL_NODES = 40
        const val MAX_TEXT_LENGTH = 120
        const val MAX_LIST_ITEMS = 8

        fun stripMarkdownFence(text: String): String {
            val trimmed = text.trim()
            if (!trimmed.startsWith("```")) return trimmed
            return trimmed
                .lines()
                .drop(1)
                .dropLastWhile { it.trim().startsWith("```") }
                .joinToString("\n")
                .trim()
        }

        private val json = Json { ignoreUnknownKeys = false }
        private val unsafeKeys = setOf(
            "code",
            "script",
            "expression",
            "eval",
            "url",
            "network",
            "onClick",
            "action",
            "href",
        )
    }
}

private class ValidationException(val error: OverviewA2UiValidationError) : IllegalArgumentException()

private fun JsonObject.string(key: String): String? =
    (this[key] as? JsonPrimitive)?.contentOrNull

private fun JsonObject.double(key: String): Double? =
    (this[key] as? JsonPrimitive)?.doubleOrNull

private fun JsonObject.int(key: String): Int? =
    (this[key] as? JsonPrimitive)?.intOrNull

internal object OverviewWidgetCatalog {
    val allowedComponentTypes = setOf(
        "card",
        "column",
        "row",
        "text",
        "moneyMetric",
        "percentage",
        "progress",
        "barList",
        "categoryList",
        "spacer",
        "divider",
    )

    val allowedBindingKeys = setOf(
        "period.label",
        "period.type",
        "wallet.label",
        "wallet.currency",
        "filter.type",
        "income",
        "expenses",
        "net",
        "avg.dailyExpense",
        "avg.monthlyExpense",
        "avg.dailyIncome",
        "avg.monthlyIncome",
        "avg.dailyNet",
        "avg.monthlyNet",
        "budget.progressAverage",
        "category.expenseBreakdown",
        "category.incomeBreakdown",
        "series.monthlySpend",
        "series.monthlyIncome",
        "series.monthlyNet",
        "series.cumulativeSpend",
    )

    fun promptCatalog(): String = buildString {
        appendLine("Allowed component types: ${allowedComponentTypes.joinToString()}.")
        appendLine("Allowed bindings: ${allowedBindingKeys.joinToString()}.")
        appendLine("Limits: max depth ${OverviewA2UiValidator.MAX_DEPTH}, max children ${OverviewA2UiValidator.MAX_CHILDREN}, max total nodes ${OverviewA2UiValidator.MAX_TOTAL_NODES}.")
    }
}

internal class BuildOverviewWidgetContextUseCase {
    operator fun invoke(state: OverviewPageUiState, currencyCode: String): OverviewWidgetContext {
        val period = state.period
        return OverviewWidgetContext(
            currencyCode = currencyCode,
            labels = mapOf(
                "period.label" to period.label(),
                "period.type" to period.typeLabel(),
                "wallet.label" to "Selected wallet",
                "wallet.currency" to currencyCode,
                "filter.type" to "Overview filter",
            ),
            moneyValues = mapOf(
                "income" to state.income,
                "expenses" to state.expenses,
                "net" to (state.income - state.expenses),
                "avg.dailyExpense" to state.avgDailyExpense,
                "avg.monthlyExpense" to state.avgMonthlyExpense,
                "avg.dailyIncome" to state.avgDailyIncome,
                "avg.monthlyIncome" to state.avgMonthlyIncome,
                "avg.dailyNet" to state.avgDailyNet,
                "avg.monthlyNet" to state.avgMonthlyNet,
            ),
            numberValues = mapOf(
                "budget.progressAverage" to state.budgetProgress.averageProgress(),
            ),
            listValues = mapOf(
                "category.expenseBreakdown" to state.categoryBreakdown.toItems(),
                "category.incomeBreakdown" to state.categoryIncomeBreakdown.toItems(),
                "series.monthlySpend" to state.monthlyTotals.toSeriesItems(),
                "series.monthlyIncome" to state.monthlyIncomeTotals.toSeriesItems(),
                "series.monthlyNet" to state.monthlyNetTotals.toSeriesItems(),
                "series.cumulativeSpend" to state.cumulativeTotals.toSeriesItems(),
            ),
        )
    }

    companion object {
        fun sample(currencyCode: String = "EUR"): OverviewWidgetContext = OverviewWidgetContext(
            currencyCode = currencyCode,
            labels = mapOf(
                "period.label" to "May 2026",
                "period.type" to "month",
                "wallet.label" to "All wallets",
                "wallet.currency" to currencyCode,
                "filter.type" to "expenses",
            ),
            moneyValues = mapOf(
                "income" to 3200.0,
                "expenses" to 1840.0,
                "net" to 1360.0,
                "avg.dailyExpense" to 59.35,
                "avg.monthlyExpense" to 1840.0,
                "avg.dailyIncome" to 103.22,
                "avg.monthlyIncome" to 3200.0,
                "avg.dailyNet" to 43.87,
                "avg.monthlyNet" to 1360.0,
            ),
            numberValues = mapOf("budget.progressAverage" to 0.62),
            listValues = mapOf(
                "category.expenseBreakdown" to listOf(
                    OverviewWidgetListItem("Groceries", 520.0, 28),
                    OverviewWidgetListItem("Transport", 210.0, 11),
                    OverviewWidgetListItem("Home", 430.0, 23),
                ),
                "category.incomeBreakdown" to listOf(
                    OverviewWidgetListItem("Salary", 3000.0, 94),
                    OverviewWidgetListItem("Other", 200.0, 6),
                ),
                "series.monthlySpend" to listOf(1200.0, 1350.0, 1840.0).toSeriesItems(),
                "series.monthlyIncome" to listOf(3000.0, 3000.0, 3200.0).toSeriesItems(),
                "series.monthlyNet" to listOf(1800.0, 1650.0, 1360.0).toSeriesItems(),
                "series.cumulativeSpend" to listOf(120.0, 480.0, 940.0, 1840.0).toSeriesItems(),
            ),
        )
    }
}

class BuildOverviewWidgetPromptUseCase {
    operator fun invoke(userPrompt: String, title: String): String = buildString {
        appendLine("Create one MoneyM overview widget as constrained A2UI JSON only.")
        appendLine("Do not include markdown, code fences, Kotlin, scripts, network calls, URLs, expressions, or explanations.")
        appendLine("Widget title: ${title.take(80)}")
        appendLine("User request: ${userPrompt.take(1200)}")
        appendLine(OverviewWidgetCatalog.promptCatalog())
        appendLine("Use this JSON shape: {\"type\":\"card\",\"title\":\"Title\",\"children\":[...]}.")
        appendLine("Use bindings for dynamic values, for example {\"type\":\"moneyMetric\",\"label\":\"Net\",\"binding\":\"net\"}.")
        appendLine("For percentage/progress, bind to a number between 0 and 1 when possible.")
        appendLine("Return only valid JSON.")
    }
}

private fun OverviewPeriod?.label(): String = when (this) {
    is OverviewPeriod.Month -> "${yearMonth.year}-${yearMonth.monthNumber.toString().padStart(2, '0')}"
    is OverviewPeriod.Year -> year.toString()
    is OverviewPeriod.DateRange -> "$startYear-$startMonth-$startDay to $endYear-$endMonth-$endDay"
    null -> "Current period"
}

private fun OverviewPeriod?.typeLabel(): String = when (this) {
    is OverviewPeriod.Month -> "month"
    is OverviewPeriod.Year -> "year"
    is OverviewPeriod.DateRange -> "custom range"
    null -> "period"
}

private fun List<CategorySpend>.toItems(): List<OverviewWidgetListItem> = map {
    OverviewWidgetListItem(
        label = it.categoryName,
        value = it.amount,
        percent = it.percent,
        colorHex = it.categoryColor,
        icon = it.categoryIcon,
    )
}

private fun List<Double>.toSeriesItems(): List<OverviewWidgetListItem> = mapIndexed { index, value ->
    OverviewWidgetListItem(label = (index + 1).toString(), value = value, percent = 0)
}

private fun List<BudgetProgress>.averageProgress(): Double =
    if (isEmpty()) 0.0 else map { it.fraction.coerceIn(0f, 1f).toDouble() }.average()

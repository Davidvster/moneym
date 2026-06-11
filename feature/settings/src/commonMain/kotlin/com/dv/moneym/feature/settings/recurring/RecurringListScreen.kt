package com.dv.moneym.feature.settings.recurring

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.model.EndCondition
import com.dv.moneym.core.model.Icon
import com.dv.moneym.core.model.MonthlyDayKind
import com.dv.moneym.core.model.RecurrenceRule
import com.dv.moneym.core.model.RecurringTransaction
import com.dv.moneym.core.model.RecurringTransactionId
import com.dv.moneym.core.model.format
import com.dv.moneym.core.navigation.ModalKey
import com.dv.moneym.core.ui.LocalUseCurrencySymbol
import com.dv.moneym.core.ui.MmButton
import com.dv.moneym.core.ui.MmButtonVariant
import com.dv.moneym.core.ui.MmCard
import com.dv.moneym.core.ui.MmRow
import com.dv.moneym.core.ui.ScreenHeader
import com.dv.moneym.core.ui.imageVector
import kotlinx.serialization.Serializable
import moneym.feature.settings.generated.resources.Res
import moneym.feature.settings.generated.resources.settings_recurring_empty
import moneym.feature.settings.generated.resources.settings_recurring_new
import moneym.feature.settings.generated.resources.settings_recurring_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Serializable
data object RecurringListKey : ModalKey

fun EntryProviderScope<NavKey>.recurringListEntry(
    onBack: () -> Unit,
    onEdit: (RecurringTransactionId) -> Unit,
    onCreateNew: () -> Unit,
    metadata: Map<String, Any> = emptyMap(),
) = entry<RecurringListKey>(metadata = metadata) {
    RecurringListScreen(onBack = onBack, onEdit = onEdit, onCreateNew = onCreateNew)
}

@Composable
private fun RecurringListScreen(
    onBack: () -> Unit,
    onEdit: (RecurringTransactionId) -> Unit,
    onCreateNew: () -> Unit,
    viewModel: RecurringListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    RecurringListContent(state = state, onBack = onBack, onEdit = onEdit, onCreateNew = onCreateNew)
}

@Composable
private fun RecurringListContent(
    state: RecurringListUiState,
    onBack: () -> Unit,
    onEdit: (RecurringTransactionId) -> Unit,
    onCreateNew: () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(MM.colors.bg)) {
        ScreenHeader(stringResource(Res.string.settings_recurring_title), onBack = onBack)
        Box(Modifier.weight(1f)) {
            when {
                state.isLoading -> Unit
                state.rules.isEmpty() -> EmptyView()
                else -> LazyColumn(modifier = Modifier.padding(MM.dimen.padding_2x)) {
                    items(state.rules, key = { it.id.value }) { rule ->
                        MmCard(modifier = Modifier.padding(bottom = MM.dimen.padding_1x)) {
                            MmRow(onClick = { onEdit(rule.id) }) {
                                RuleSummary(
                                    rule = rule,
                                    categoryName = state.categories[rule.categoryId]?.name ?: "—",
                                )
                            }
                        }
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .padding(
                    horizontal = MM.dimen.padding_2_5x,
                    vertical = MM.dimen.padding_2x,
                )
                .navigationBarsPadding(),
        ) {
            MmButton(
                text = stringResource(Res.string.settings_recurring_new),
                onClick = onCreateNew,
                variant = MmButtonVariant.Primary,
                fullWidth = true,
                leadingIcon = Icon.Plus.imageVector,
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun RecurringListContentPreview() {
    val epoch = kotlin.time.Instant.fromEpochSeconds(0)
    val eur = com.dv.moneym.core.model.CurrencyCode("EUR")
    val rentCategory = com.dv.moneym.core.model.Category(
        id = com.dv.moneym.core.model.CategoryId(1),
        name = "Housing",
        iconKey = "home",
        colorHex = "#3B82F6",
        isUserCreated = false,
        archived = false,
        createdAt = epoch,
        updatedAt = epoch,
    )
    val salaryCategory = com.dv.moneym.core.model.Category(
        id = com.dv.moneym.core.model.CategoryId(2),
        name = "Salary",
        iconKey = "wallet",
        colorHex = "#22C55E",
        isUserCreated = false,
        archived = false,
        createdAt = epoch,
        updatedAt = epoch,
        type = com.dv.moneym.core.model.TransactionType.INCOME,
    )
    val rules = listOf(
        RecurringTransaction(
            id = RecurringTransactionId(1),
            type = com.dv.moneym.core.model.TransactionType.EXPENSE,
            amount = com.dv.moneym.core.model.Money(85000, eur),
            note = "Rent",
            categoryId = rentCategory.id,
            accountId = com.dv.moneym.core.model.AccountId(1),
            paymentModeId = null,
            startDate = kotlinx.datetime.LocalDate(2026, 6, 1),
            rule = RecurrenceRule.Monthly(interval = 1, dayKind = MonthlyDayKind.OnDay(1)),
            endCondition = EndCondition.Unlimited,
            lastMaterializedDate = null,
            createdAt = epoch,
            updatedAt = epoch,
        ),
        RecurringTransaction(
            id = RecurringTransactionId(2),
            type = com.dv.moneym.core.model.TransactionType.INCOME,
            amount = com.dv.moneym.core.model.Money(250000, eur),
            note = "Salary",
            categoryId = salaryCategory.id,
            accountId = com.dv.moneym.core.model.AccountId(1),
            paymentModeId = null,
            startDate = kotlinx.datetime.LocalDate(2026, 6, 10),
            rule = RecurrenceRule.Monthly(interval = 1, dayKind = MonthlyDayKind.LastDay),
            endCondition = EndCondition.Count(occurrences = 12),
            lastMaterializedDate = null,
            createdAt = epoch,
            updatedAt = epoch,
        ),
    )
    com.dv.moneym.core.designsystem.MoneyMTheme {
        RecurringListContent(
            state = RecurringListUiState(
                isLoading = false,
                rules = rules,
                categories = mapOf(rentCategory.id to rentCategory, salaryCategory.id to salaryCategory),
            ),
            onBack = {},
            onEdit = {},
            onCreateNew = {},
        )
    }
}

@Composable
private fun EmptyView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(Res.string.settings_recurring_empty),
            style = MM.type.body,
            color = MM.colors.text3,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = MM.dimen.padding_4x),
        )
    }
}

@Composable
private fun RuleSummary(rule: RecurringTransaction, categoryName: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp()),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = categoryName,
                style = MM.type.body,
                color = MM.colors.text,
            )
            Text(
                text = rule.amount.format(LocalUseCurrencySymbol.current),
                style = MM.type.body,
                color = if (rule.type == com.dv.moneym.core.model.TransactionType.EXPENSE)
                    MM.colors.text else MM.colors.accent,
            )
        }
        Text(
            text = ruleDescription(rule),
            style = MM.type.caption,
            color = MM.colors.text2,
        )
        Text(
            text = "Starts ${rule.startDate} · ${endDescription(rule.endCondition)}",
            style = MM.type.micro,
            color = MM.colors.text3,
        )
    }
}

private fun ruleDescription(rule: RecurringTransaction): String =
    when (val r = rule.rule) {
        is RecurrenceRule.Daily -> "Every ${r.interval} day(s)"
        is RecurrenceRule.Weekly -> "Every ${r.interval} week(s) on day ${r.dayOfWeek}"
        is RecurrenceRule.Monthly -> {
            val on = when (val d = r.dayKind) {
                is MonthlyDayKind.OnDay -> "day ${d.day}"
                MonthlyDayKind.LastDay -> "last day"
            }
            "Every ${r.interval} month(s) on $on"
        }
    }

private fun endDescription(end: EndCondition): String = when (end) {
    EndCondition.Unlimited -> "Unlimited"
    is EndCondition.Count -> "${end.occurrences} times"
    is EndCondition.Until -> "Until ${end.date}"
}

private fun Int.dp(): androidx.compose.ui.unit.Dp = androidx.compose.ui.unit.Dp(this.toFloat())

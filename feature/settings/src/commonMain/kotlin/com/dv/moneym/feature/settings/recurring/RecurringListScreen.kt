package com.dv.moneym.feature.settings.recurring

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.model.EndCondition
import com.dv.moneym.core.model.MonthlyDayKind
import com.dv.moneym.core.model.RecurrenceRule
import com.dv.moneym.core.model.RecurringTransaction
import com.dv.moneym.core.model.RecurringTransactionId
import com.dv.moneym.core.model.format
import com.dv.moneym.core.navigation.ModalKey
import com.dv.moneym.core.ui.MmCard
import com.dv.moneym.core.ui.MmRow
import com.dv.moneym.core.ui.ScreenHeader
import kotlinx.serialization.Serializable
import moneym.feature.settings.generated.resources.Res
import moneym.feature.settings.generated.resources.settings_recurring_empty
import moneym.feature.settings.generated.resources.settings_recurring_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Serializable
data object RecurringListKey : ModalKey

fun EntryProviderScope<NavKey>.recurringListEntry(
    onBack: () -> Unit,
    onEdit: (RecurringTransactionId) -> Unit,
) = entry<RecurringListKey> {
    RecurringListScreen(onBack = onBack, onEdit = onEdit)
}

@Composable
private fun RecurringListScreen(
    onBack: () -> Unit,
    onEdit: (RecurringTransactionId) -> Unit,
    viewModel: RecurringListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().background(MM.colors.bg)) {
        ScreenHeader(stringResource(Res.string.settings_recurring_title), onBack = onBack)
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
                text = rule.amount.format(),
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

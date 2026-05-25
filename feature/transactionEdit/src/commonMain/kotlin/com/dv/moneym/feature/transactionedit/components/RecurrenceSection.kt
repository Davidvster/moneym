package com.dv.moneym.feature.transactionedit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.model.MonthlyDayKind
import com.dv.moneym.core.ui.MmButton
import com.dv.moneym.core.ui.MmButtonSize
import com.dv.moneym.core.ui.MmButtonVariant
import com.dv.moneym.core.ui.MmChip
import com.dv.moneym.core.ui.MmField
import com.dv.moneym.core.ui.MmSegmented
import com.dv.moneym.core.ui.MmToggle
import com.dv.moneym.feature.transactionedit.EndKind
import com.dv.moneym.feature.transactionedit.FreqUnit
import com.dv.moneym.feature.transactionedit.TransactionEditIntent
import com.dv.moneym.feature.transactionedit.TransactionEditUiState
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant
import moneym.feature.transactionedit.generated.resources.Res
import moneym.feature.transactionedit.generated.resources.edit_date_ok
import moneym.feature.transactionedit.generated.resources.tx_edit_make_recurring
import moneym.feature.transactionedit.generated.resources.tx_edit_recurring_days
import moneym.feature.transactionedit.generated.resources.tx_edit_recurring_end_count
import moneym.feature.transactionedit.generated.resources.tx_edit_recurring_end_count_label
import moneym.feature.transactionedit.generated.resources.tx_edit_recurring_end_label
import moneym.feature.transactionedit.generated.resources.tx_edit_recurring_end_unlimited
import moneym.feature.transactionedit.generated.resources.tx_edit_recurring_end_until
import moneym.feature.transactionedit.generated.resources.tx_edit_recurring_end_until_pick
import moneym.feature.transactionedit.generated.resources.tx_edit_recurring_error
import moneym.feature.transactionedit.generated.resources.tx_edit_recurring_every
import moneym.feature.transactionedit.generated.resources.tx_edit_recurring_last_day
import moneym.feature.transactionedit.generated.resources.tx_edit_recurring_months
import moneym.feature.transactionedit.generated.resources.tx_edit_recurring_on_day_label
import moneym.feature.transactionedit.generated.resources.tx_edit_recurring_on_dow_label
import moneym.feature.transactionedit.generated.resources.tx_edit_recurring_weekday_friday
import moneym.feature.transactionedit.generated.resources.tx_edit_recurring_weekday_monday
import moneym.feature.transactionedit.generated.resources.tx_edit_recurring_weekday_saturday
import moneym.feature.transactionedit.generated.resources.tx_edit_recurring_weekday_sunday
import moneym.feature.transactionedit.generated.resources.tx_edit_recurring_weekday_thursday
import moneym.feature.transactionedit.generated.resources.tx_edit_recurring_weekday_tuesday
import moneym.feature.transactionedit.generated.resources.tx_edit_recurring_weekday_wednesday
import moneym.feature.transactionedit.generated.resources.tx_edit_recurring_weeks
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun RecurrenceSection(
    state: TransactionEditUiState,
    onIntent: (TransactionEditIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.tx_edit_make_recurring),
                style = MM.type.body,
                color = MM.colors.text,
                modifier = Modifier.weight(1f),
            )
            MmToggle(
                checked = state.isRecurring,
                onCheckedChange = { onIntent(TransactionEditIntent.RecurringToggled(it)) },
            )
        }

        if (state.isRecurring) {
            Spacer(Modifier.height(MM.dimen.padding_2x))
            RecurrenceControls(state = state, onIntent = onIntent)
            if (state.recurrenceError) {
                Spacer(Modifier.height(MM.dimen.padding_1x))
                Text(
                    text = stringResource(Res.string.tx_edit_recurring_error),
                    style = MM.type.micro,
                    color = MM.colors.danger,
                )
            }
        }
    }
}

@Composable
private fun RecurrenceControls(
    state: TransactionEditUiState,
    onIntent: (TransactionEditIntent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(MM.dimen.padding_2x)) {
        IntervalRow(
            interval = state.freqInterval,
            unit = state.freqUnit,
            onIntervalChanged = { onIntent(TransactionEditIntent.FreqIntervalChanged(it)) },
            onUnitChanged = { onIntent(TransactionEditIntent.FreqUnitChanged(it)) },
        )

        when (state.freqUnit) {
            FreqUnit.DAYS -> Unit
            FreqUnit.WEEKS -> WeekdayPicker(
                selected = state.weekDay,
                onSelected = { onIntent(TransactionEditIntent.WeekDayChanged(it)) },
            )
            FreqUnit.MONTHS -> MonthDayPicker(
                kind = state.monthDayKind,
                onChanged = { onIntent(TransactionEditIntent.MonthDayChanged(it)) },
            )
        }

        EndConditionRow(
            state = state,
            onIntent = onIntent,
        )
    }
}

@Composable
private fun IntervalRow(
    interval: Int,
    unit: FreqUnit,
    onIntervalChanged: (Int) -> Unit,
    onUnitChanged: (FreqUnit) -> Unit,
) {
    val daysLabel = stringResource(Res.string.tx_edit_recurring_days)
    val weeksLabel = stringResource(Res.string.tx_edit_recurring_weeks)
    val monthsLabel = stringResource(Res.string.tx_edit_recurring_months)
    val unitOptions = listOf(daysLabel, weeksLabel, monthsLabel)
    val selectedIndex = when (unit) {
        FreqUnit.DAYS -> 0
        FreqUnit.WEEKS -> 1
        FreqUnit.MONTHS -> 2
    }
    Column {
        Text(
            text = stringResource(Res.string.tx_edit_recurring_every),
            style = MM.type.micro,
            color = MM.colors.text3,
        )
        Spacer(Modifier.height(MM.dimen.padding_1x))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MM.dimen.padding_1x),
        ) {
            Stepper(
                value = interval,
                onValueChange = onIntervalChanged,
                modifier = Modifier.weight(0.45f),
            )
            MmSegmented(
                options = unitOptions,
                selectedIndex = selectedIndex,
                onOptionSelected = { idx ->
                    val next = when (idx) {
                        0 -> FreqUnit.DAYS
                        1 -> FreqUnit.WEEKS
                        else -> FreqUnit.MONTHS
                    }
                    onUnitChanged(next)
                },
                modifier = Modifier.weight(0.55f),
                fillWidth = true,
            )
        }
    }
}

@Composable
private fun Stepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.height(36.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MM.dimen.padding_0_5x),
    ) {
        StepperButton(label = "−", enabled = value > 1, onClick = { onValueChange(value - 1) })
        Box(
            modifier = Modifier
                .weight(1f)
                .height(36.dp)
                .clip(MM.dimen.pill)
                .background(MM.colors.surface2),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = value.toString(),
                style = MM.type.body,
                color = MM.colors.text,
                textAlign = TextAlign.Center,
            )
        }
        StepperButton(label = "+", enabled = value < 30, onClick = { onValueChange(value + 1) })
    }
}

@Composable
private fun StepperButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = MM.colors
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(colors.surface2)
            .border(1.dp, colors.borderStrong, CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MM.type.body,
            color = if (enabled) colors.text else colors.text3,
        )
    }
}

@Composable
private fun WeekdayPicker(
    selected: Int,
    onSelected: (Int) -> Unit,
) {
    val days = listOf(
        1 to stringResource(Res.string.tx_edit_recurring_weekday_monday),
        2 to stringResource(Res.string.tx_edit_recurring_weekday_tuesday),
        3 to stringResource(Res.string.tx_edit_recurring_weekday_wednesday),
        4 to stringResource(Res.string.tx_edit_recurring_weekday_thursday),
        5 to stringResource(Res.string.tx_edit_recurring_weekday_friday),
        6 to stringResource(Res.string.tx_edit_recurring_weekday_saturday),
        7 to stringResource(Res.string.tx_edit_recurring_weekday_sunday),
    )
    Column {
        Text(
            text = stringResource(Res.string.tx_edit_recurring_on_dow_label),
            style = MM.type.micro,
            color = MM.colors.text3,
        )
        Spacer(Modifier.height(MM.dimen.padding_1x))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(MM.dimen.padding_0_5x),
        ) {
            days.forEach { (num, label) ->
                MmChip(
                    selected = num == selected,
                    onClick = { onSelected(num) },
                ) {
                    Text(
                        text = label,
                        style = MM.type.caption,
                        color = if (num == selected) MM.colors.bg else MM.colors.text,
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthDayPicker(
    kind: MonthlyDayKind,
    onChanged: (MonthlyDayKind) -> Unit,
) {
    val isLast = kind is MonthlyDayKind.LastDay
    val day = (kind as? MonthlyDayKind.OnDay)?.day ?: 1
    Column {
        Text(
            text = stringResource(Res.string.tx_edit_recurring_on_day_label),
            style = MM.type.micro,
            color = MM.colors.text3,
        )
        Spacer(Modifier.height(MM.dimen.padding_1x))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MM.dimen.padding_1x),
        ) {
            Stepper(
                value = day,
                onValueChange = { v ->
                    onChanged(MonthlyDayKind.OnDay(v.coerceIn(1, 28)))
                },
                modifier = Modifier.weight(0.45f),
            )
            MmChip(
                selected = isLast,
                onClick = {
                    onChanged(if (isLast) MonthlyDayKind.OnDay(day) else MonthlyDayKind.LastDay)
                },
                modifier = Modifier.weight(0.55f),
            ) {
                Text(
                    text = stringResource(Res.string.tx_edit_recurring_last_day),
                    style = MM.type.caption,
                    color = if (isLast) MM.colors.bg else MM.colors.text,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EndConditionRow(
    state: TransactionEditUiState,
    onIntent: (TransactionEditIntent) -> Unit,
) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    Column {
        Text(
            text = stringResource(Res.string.tx_edit_recurring_end_label),
            style = MM.type.micro,
            color = MM.colors.text3,
        )
        Spacer(Modifier.height(MM.dimen.padding_1x))
        Column(verticalArrangement = Arrangement.spacedBy(MM.dimen.padding_1x)) {
            RadioOption(
                label = stringResource(Res.string.tx_edit_recurring_end_unlimited),
                selected = state.endKind == EndKind.UNLIMITED,
                onSelect = { onIntent(TransactionEditIntent.EndKindChanged(EndKind.UNLIMITED)) },
            )
            RadioOption(
                label = stringResource(Res.string.tx_edit_recurring_end_count),
                selected = state.endKind == EndKind.COUNT,
                onSelect = { onIntent(TransactionEditIntent.EndKindChanged(EndKind.COUNT)) },
                trailing = {
                    if (state.endKind == EndKind.COUNT) {
                        MmField(
                            value = state.endCount.toString(),
                            onValueChange = { txt ->
                                val n = txt.filter { it.isDigit() }.toIntOrNull() ?: 1
                                onIntent(TransactionEditIntent.EndCountChanged(n))
                            },
                            modifier = Modifier.width(64.dp),
                            placeholder = stringResource(Res.string.tx_edit_recurring_end_count_label),
                        )
                    }
                },
            )
            RadioOption(
                label = stringResource(Res.string.tx_edit_recurring_end_until),
                selected = state.endKind == EndKind.UNTIL,
                onSelect = { onIntent(TransactionEditIntent.EndKindChanged(EndKind.UNTIL)) },
                trailing = {
                    if (state.endKind == EndKind.UNTIL) {
                        MmButton(
                            text = state.endDate?.toString()
                                ?: stringResource(Res.string.tx_edit_recurring_end_until_pick),
                            onClick = { showDatePicker = true },
                            size = MmButtonSize.Sm,
                            variant = MmButtonVariant.Secondary,
                        )
                    }
                },
            )
        }
    }
    if (showDatePicker) {
        val initialMillis = (state.endDate ?: state.date)
            ?.atStartOfDayIn(TimeZone.UTC)?.toEpochMilliseconds()
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        val colors = MM.colors
        val themed = DatePickerDefaults.colors(
            containerColor = colors.bg,
            titleContentColor = colors.text,
            headlineContentColor = colors.text,
            weekdayContentColor = colors.text2,
            subheadContentColor = colors.text2,
            yearContentColor = colors.text,
            currentYearContentColor = colors.accent,
            selectedYearContentColor = colors.bg,
            selectedYearContainerColor = colors.accent,
            selectedDayContentColor = colors.bg,
            selectedDayContainerColor = colors.accent,
            todayContentColor = colors.accent,
            todayDateBorderColor = colors.accent,
            dayContentColor = colors.text,
            navigationContentColor = colors.text,
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { ms ->
                        val ld: LocalDate = Instant.fromEpochMilliseconds(ms)
                            .toLocalDateTime(TimeZone.UTC).date
                        onIntent(TransactionEditIntent.EndDateChanged(ld))
                    }
                    showDatePicker = false
                }) { Text(stringResource(Res.string.edit_date_ok), color = colors.accent) }
            },
            colors = themed,
        ) {
            DatePicker(state = pickerState, colors = themed)
        }
    }
}

@Composable
private fun RadioOption(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = MM.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MM.dimen.padding_1_5x),
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .border(2.dp, if (selected) colors.accent else colors.borderStrong, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(colors.accent),
                )
            }
        }
        Text(
            text = label,
            style = MM.type.body,
            color = colors.text,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) trailing()
    }
}

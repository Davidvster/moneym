package com.dv.moneym.feature.overview.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.dv.moneym.core.designsystem.MM
import kotlinx.datetime.LocalDate
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import moneym.feature.overview.generated.resources.Res
import moneym.feature.overview.generated.resources.overview_cancel
import moneym.feature.overview.generated.resources.overview_date_range_from
import moneym.feature.overview.generated.resources.overview_date_range_title
import moneym.feature.overview.generated.resources.overview_date_range_to
import moneym.feature.overview.generated.resources.overview_ok
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DateRangePickerDialog(
    initStartYear: Int,
    initStartMonth: Int,
    initStartDay: Int,
    initEndYear: Int,
    initEndMonth: Int,
    initEndDay: Int,
    onDismiss: () -> Unit,
    onConfirm: (startYear: Int, startMonth: Int, startDay: Int, endYear: Int, endMonth: Int, endDay: Int) -> Unit,
) {
    val colors = MM.colors

    // Convert year/month/day to epoch millis (UTC midnight) for initial state
    val initStartMillis = remember(initStartYear, initStartMonth, initStartDay) {
        LocalDate(initStartYear, initStartMonth, initStartDay)
            .atStartOfDayIn(kotlinx.datetime.TimeZone.UTC)
            .toEpochMilliseconds()
    }
    val initEndMillis = remember(initEndYear, initEndMonth, initEndDay) {
        LocalDate(initEndYear, initEndMonth, initEndDay)
            .atStartOfDayIn(kotlinx.datetime.TimeZone.UTC)
            .toEpochMilliseconds()
    }

    val dateRangeState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initStartMillis,
        initialSelectedEndDateMillis = initEndMillis,
    )

    val themedColors = DatePickerDefaults.colors(
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
        dayInSelectionRangeContentColor = colors.text,
        dayInSelectionRangeContainerColor = colors.accent.copy(alpha = 0.15f),
        todayContentColor = colors.accent,
        todayDateBorderColor = colors.accent,
        dayContentColor = colors.text,
        navigationContentColor = colors.text,
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val startMs = dateRangeState.selectedStartDateMillis
                val endMs = dateRangeState.selectedEndDateMillis
                if (startMs != null && endMs != null) {
                    val start = kotlin.time.Instant.fromEpochMilliseconds(startMs)
                        .toLocalDateTime(kotlinx.datetime.TimeZone.UTC).date
                    val end = kotlin.time.Instant.fromEpochMilliseconds(endMs)
                        .toLocalDateTime(kotlinx.datetime.TimeZone.UTC).date
                    onConfirm(
                        start.year, start.monthNumber, start.dayOfMonth,
                        end.year, end.monthNumber, end.dayOfMonth,
                    )
                } else {
                    onDismiss()
                }
            }) {
                Text(stringResource(Res.string.overview_ok), color = colors.accent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.overview_cancel), color = colors.text2)
            }
        },
        colors = themedColors,
    ) {
        DateRangePicker(
            state = dateRangeState,
            colors = themedColors,
            title = {
                Text(
                    text = stringResource(Res.string.overview_date_range_title),
                    style = MM.type.title3,
                    color = colors.text,
                    modifier = Modifier.padding(
                        start = MM.dimen.padding_3x,
                        end = MM.dimen.padding_1_5x,
                        top = MM.dimen.padding_2x
                    ),
                )
            },
            headline = {
                val start = dateRangeState.selectedStartDateMillis?.let {
                    kotlin.time.Instant.fromEpochMilliseconds(it)
                        .toLocalDateTime(kotlinx.datetime.TimeZone.UTC).date
                }
                val end = dateRangeState.selectedEndDateMillis?.let {
                    kotlin.time.Instant.fromEpochMilliseconds(it)
                        .toLocalDateTime(kotlinx.datetime.TimeZone.UTC).date
                }
                val fromLabel = stringResource(Res.string.overview_date_range_from)
                val toLabel = stringResource(Res.string.overview_date_range_to)
                Row(
                    modifier = Modifier.padding(
                        start = MM.dimen.padding_3x,
                        end = MM.dimen.padding_1_5x,
                        bottom = MM.dimen.padding_1_5x
                    ),
                    horizontalArrangement = Arrangement.spacedBy(MM.dimen.padding_1x),
                ) {
                    Text(
                        text = if (start != null) "${start.dayOfMonth}.${start.monthNumber}.${start.year}" else fromLabel,
                        style = MM.type.body,
                        color = if (start != null) colors.text else colors.text3,
                    )
                    Text(" – ", style = MM.type.body, color = colors.text2)
                    Text(
                        text = if (end != null) "${end.dayOfMonth}.${end.monthNumber}.${end.year}" else toLabel,
                        style = MM.type.body,
                        color = if (end != null) colors.text else colors.text3,
                    )
                }
            },
            showModeToggle = false,
        )
    }
}

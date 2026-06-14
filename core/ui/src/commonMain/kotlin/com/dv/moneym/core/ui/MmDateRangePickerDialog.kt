package com.dv.moneym.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.dv.moneym.core.designsystem.MM
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MmDateRangePickerDialog(
    initStartYear: Int,
    initStartMonth: Int,
    initStartDay: Int,
    initEndYear: Int,
    initEndMonth: Int,
    initEndDay: Int,
    title: String,
    fromLabel: String,
    toLabel: String,
    okLabel: String,
    cancelLabel: String,
    minSelectableDateIso: String? = null,
    maxSelectableDateIso: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (startYear: Int, startMonth: Int, startDay: Int, endYear: Int, endMonth: Int, endDay: Int) -> Unit,
) {
    val colors = MM.colors

    // Convert year/month/day to epoch millis (UTC midnight) for initial state
    val initStartMillis = remember(initStartYear, initStartMonth, initStartDay) {
        LocalDate(initStartYear, initStartMonth, initStartDay)
            .atStartOfDayIn(TimeZone.UTC)
            .toEpochMilliseconds()
    }
    val initEndMillis = remember(initEndYear, initEndMonth, initEndDay) {
        LocalDate(initEndYear, initEndMonth, initEndDay)
            .atStartOfDayIn(TimeZone.UTC)
            .toEpochMilliseconds()
    }

    val minDate = remember(minSelectableDateIso) {
        minSelectableDateIso?.let { LocalDate.parse(it) }
    }
    val maxDate = remember(maxSelectableDateIso) {
        maxSelectableDateIso?.let { LocalDate.parse(it) }
    }

    val selectableDatesImpl = remember(minDate, maxDate) {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val date = Instant.fromEpochMilliseconds(utcTimeMillis)
                    .toLocalDateTime(TimeZone.UTC).date
                return (minDate == null || date >= minDate) && (maxDate == null || date <= maxDate)
            }
        }
    }

    val dateRangeState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initStartMillis,
        initialSelectedEndDateMillis = initEndMillis,
        selectableDates = selectableDatesImpl,
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
        disabledDayContentColor = colors.text.copy(alpha = 0.3f),
        navigationContentColor = colors.text,
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val startMs = dateRangeState.selectedStartDateMillis
                val endMs = dateRangeState.selectedEndDateMillis
                if (startMs != null && endMs != null) {
                    val start = Instant.fromEpochMilliseconds(startMs)
                        .toLocalDateTime(TimeZone.UTC).date
                    val end = Instant.fromEpochMilliseconds(endMs)
                        .toLocalDateTime(TimeZone.UTC).date
                    onConfirm(
                        start.year, start.month.number, start.day,
                        end.year, end.month.number, end.day,
                    )
                } else {
                    onDismiss()
                }
            }) {
                Text(okLabel, color = colors.accent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(cancelLabel, color = colors.text2)
            }
        },
        colors = themedColors,
    ) {
        DateRangePicker(
            state = dateRangeState,
            colors = themedColors,
            title = {
                Text(
                    text = title,
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
                    Instant.fromEpochMilliseconds(it)
                        .toLocalDateTime(TimeZone.UTC).date
                }
                val end = dateRangeState.selectedEndDateMillis?.let {
                    Instant.fromEpochMilliseconds(it)
                        .toLocalDateTime(TimeZone.UTC).date
                }
                Row(
                    modifier = Modifier.padding(
                        start = MM.dimen.padding_3x,
                        end = MM.dimen.padding_1_5x,
                        bottom = MM.dimen.padding_1_5x
                    ),
                    horizontalArrangement = Arrangement.spacedBy(MM.dimen.padding_1x),
                ) {
                    Text(
                        text = if (start != null) "${start.day}.${start.month.number}.${start.year}" else fromLabel,
                        style = MM.type.body,
                        color = if (start != null) colors.text else colors.text3,
                    )
                    Text(" – ", style = MM.type.body, color = colors.text2)
                    Text(
                        text = if (end != null) "${end.day}.${end.month.number}.${end.year}" else toLabel,
                        style = MM.type.body,
                        color = if (end != null) colors.text else colors.text3,
                    )
                }
            },
            showModeToggle = false,
        )
    }
}

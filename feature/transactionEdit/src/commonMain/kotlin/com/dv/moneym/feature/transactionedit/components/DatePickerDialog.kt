package com.dv.moneym.feature.transactionedit.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.feature.transactionedit.TransactionEditIntent
import com.dv.moneym.feature.transactionedit.TransactionEditUiState
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import moneym.feature.transactionedit.generated.resources.Res
import moneym.feature.transactionedit.generated.resources.edit_date_ok
import moneym.feature.transactionedit.generated.resources.edit_date_today
import moneym.feature.transactionedit.generated.resources.edit_date_yesterday
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TransactionDatePickerDialog(
    state: TransactionEditUiState,
    todayDate: LocalDate,
    yesterdayDate: LocalDate,
    onIntent: (TransactionEditIntent) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MM.colors
    val initialMillis = state.date
        ?.atStartOfDayIn(TimeZone.UTC)
        ?.toEpochMilliseconds()
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis,
    )

    // Apply design system colors to the date picker
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
        todayContentColor = colors.accent,
        todayDateBorderColor = colors.accent,
        dayContentColor = colors.text,
        navigationContentColor = colors.text,
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // Yesterday quick button
                TextButton(onClick = {
                    onIntent(TransactionEditIntent.DateChanged(yesterdayDate))
                    onDismiss()
                }) {
                    Text(stringResource(Res.string.edit_date_yesterday), color = colors.text2)
                }
                // Today quick button
                TextButton(onClick = {
                    onIntent(TransactionEditIntent.DateChanged(todayDate))
                    onDismiss()
                }) {
                    Text(stringResource(Res.string.edit_date_today), color = colors.accent)
                }
                // OK button
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val instant = Instant.fromEpochMilliseconds(millis)
                        val localDate = instant.toLocalDateTime(TimeZone.UTC).date
                        onIntent(TransactionEditIntent.DateChanged(localDate))
                    }
                    onDismiss()
                }) {
                    Text(stringResource(Res.string.edit_date_ok), color = colors.accent)
                }
            }
        },
        colors = themedColors,
    ) {
        DatePicker(state = datePickerState, colors = themedColors)
    }
}

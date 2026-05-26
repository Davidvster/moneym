package com.dv.moneym.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.model.Icon

@Composable
fun MmMonthPickerDialog(
    currentYear: Int,
    currentMonth: Int,
    nowYear: Int,
    nowMonth: Int,
    title: String,
    nowLabel: String,
    okLabel: String,
    cancelLabel: String,
    prevYearContentDescription: String,
    nextYearContentDescription: String,
    minYear: Int? = null,
    minMonth: Int? = null,
    onDismiss: () -> Unit,
    onConfirm: (year: Int, month: Int) -> Unit,
) {
    val colors = MM.colors
    val type = MM.type

    var selectedYear by remember { mutableIntStateOf(currentYear) }
    var selectedMonth by remember { mutableIntStateOf(currentMonth) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = type.title3,
                color = colors.text,
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(MM.dimen.padding_2x),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (minYear == null || selectedYear > minYear) {
                        MmIconButton(
                            icon = Icon.ChevronLeft.imageVector,
                            onClick = { selectedYear-- },
                            size = MM.dimen.padding_4x,
                            contentDescription = prevYearContentDescription,
                        )
                    } else {
                        Spacer(Modifier.width(MM.dimen.padding_4x))
                    }
                    Text(
                        text = selectedYear.toString(),
                        style = type.body,
                        color = if (selectedYear == nowYear) colors.accent else colors.text,
                        modifier = Modifier.widthIn(min = MM.dimen.padding_8x),
                        textAlign = TextAlign.Center,
                    )
                    MmIconButton(
                        icon = Icon.ChevronRight.imageVector,
                        onClick = { selectedYear++ },
                        size = MM.dimen.padding_4x,
                        contentDescription = nextYearContentDescription,
                    )
                }

                MonthGrid(
                    selectedMonth = selectedMonth,
                    selectedYear = selectedYear,
                    nowMonth = nowMonth,
                    nowYear = nowYear,
                    minYear = minYear,
                    minMonth = minMonth,
                    onMonthSelected = { selectedMonth = it },
                )
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(MM.dimen.padding_0_5x),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { onConfirm(nowYear, nowMonth) }) {
                    Text(nowLabel, color = colors.text2)
                }
                TextButton(onClick = { onConfirm(selectedYear, selectedMonth) }) {
                    Text(okLabel, color = colors.accent)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(cancelLabel, color = colors.text2)
            }
        },
        containerColor = colors.surface,
        titleContentColor = colors.text,
    )
}

@Composable
private fun MonthGrid(
    selectedMonth: Int,
    selectedYear: Int,
    nowMonth: Int,
    nowYear: Int,
    minYear: Int? = null,
    minMonth: Int? = null,
    onMonthSelected: (Int) -> Unit,
) {
    val colors = MM.colors
    val type = MM.type
    val monthNames = localizedMonthAbbreviations()

    Column(verticalArrangement = Arrangement.spacedBy(MM.dimen.padding_1x)) {
        for (row in 0..3) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                for (col in 0..2) {
                    val m = row * 3 + col + 1
                    val isSelected = m == selectedMonth
                    val isNow = m == nowMonth && selectedYear == nowYear
                    val isDisabled = minYear != null && selectedYear == minYear && minMonth != null && m < minMonth
                    Box(
                        modifier = Modifier
                            .clip(MM.dimen.radius_1x)
                            .background(if (isSelected && !isDisabled) colors.accent else Color.Transparent)
                            .then(
                                if (isNow && !isSelected && !isDisabled) {
                                    Modifier.border(
                                        MM.dimen.strokeHairline,
                                        colors.accent.copy(alpha = 0.5f),
                                        MM.dimen.radius_1x,
                                    )
                                } else Modifier
                            )
                            .clickable(
                                enabled = !isDisabled,
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                            ) { onMonthSelected(m) }
                            .padding(
                                horizontal = MM.dimen.padding_1_5x,
                                vertical = MM.dimen.padding_1x,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = monthNames[m - 1],
                            style = type.body,
                            color = when {
                                isSelected && !isDisabled -> colors.bg
                                isDisabled -> colors.text.copy(alpha = 0.3f)
                                isNow -> colors.accent
                                else -> colors.text
                            },
                        )
                    }
                }
            }
        }
    }
}

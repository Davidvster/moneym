package com.dv.moneym.feature.overview.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.ui.MmIconButton
import com.dv.moneym.core.ui.MmIcons
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import moneym.feature.overview.generated.resources.Res
import moneym.feature.overview.generated.resources.overview_cancel
import moneym.feature.overview.generated.resources.overview_dialog_select_month
import moneym.feature.overview.generated.resources.overview_next_year_cd
import moneym.feature.overview.generated.resources.overview_now
import moneym.feature.overview.generated.resources.overview_ok
import moneym.feature.overview.generated.resources.overview_prev_year_cd
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock

@Composable
internal fun OverviewMonthPickerDialog(
    currentYear: Int,
    currentMonth: Int,
    onDismiss: () -> Unit,
    onConfirm: (year: Int, month: Int) -> Unit,
) {
    val colors = MM.colors
    val type = MM.type
    val space = MM.space

    var selectedYear by remember { mutableIntStateOf(currentYear) }
    var selectedMonth by remember { mutableIntStateOf(currentMonth) }

    val todayDate = remember {
        Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault()).date
    }
    val nowYear = todayDate.year
    val nowMonth = todayDate.monthNumber

    val monthNames = localizedMonthNames().map { it.take(3) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(Res.string.overview_dialog_select_month), style = type.title3, color = colors.text)
        },
        text = {
            OverviewMonthPickerContent(
                selectedYear = selectedYear,
                selectedMonth = selectedMonth,
                nowYear = nowYear,
                nowMonth = nowMonth,
                monthNames = monthNames,
                onYearDecrement = { selectedYear-- },
                onYearIncrement = { selectedYear++ },
                onMonthSelected = { selectedMonth = it },
            )
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(space.padding_0_5x),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { onConfirm(nowYear, nowMonth) }) {
                    Text(stringResource(Res.string.overview_now), color = colors.text2)
                }
                TextButton(onClick = { onConfirm(selectedYear, selectedMonth) }) {
                    Text(stringResource(Res.string.overview_ok), color = colors.accent)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.overview_cancel), color = colors.text2)
            }
        },
        containerColor = colors.surface,
        titleContentColor = colors.text,
    )
}

@Composable
private fun OverviewMonthPickerContent(
    selectedYear: Int,
    selectedMonth: Int,
    nowYear: Int,
    nowMonth: Int,
    monthNames: List<String>,
    onYearDecrement: () -> Unit,
    onYearIncrement: () -> Unit,
    onMonthSelected: (Int) -> Unit,
) {
    val colors = MM.colors
    val type = MM.type
    val space = MM.space
    val radius = MM.radius
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space.padding_2x),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            MmIconButton(
                icon = MmIcons.chevronLeft,
                onClick = onYearDecrement,
                size = 32.dp,
                contentDescription = stringResource(Res.string.overview_prev_year_cd),
            )
            Text(
                text = selectedYear.toString(),
                style = type.body,
                color = if (selectedYear == nowYear) colors.accent else colors.text,
                modifier = Modifier.widthIn(min = 64.dp),
                textAlign = TextAlign.Center,
            )
            MmIconButton(
                icon = MmIcons.chevronRight,
                onClick = onYearIncrement,
                size = 32.dp,
                contentDescription = stringResource(Res.string.overview_next_year_cd),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(space.padding_1x)) {
            for (row in 0..3) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    for (col in 0..2) {
                        val m = row * 3 + col + 1
                        val isSelected = m == selectedMonth
                        val isNow = m == nowMonth && selectedYear == nowYear
                        Box(
                            modifier = Modifier
                                .clip(radius.radius_1x)
                                .background(
                                    if (isSelected) colors.accent else Color.Transparent,
                                )
                                .then(
                                    if (isNow && !isSelected) {
                                        Modifier.border(1.dp, colors.accent.copy(alpha = 0.5f), radius.radius_1x)
                                    } else Modifier
                                )
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() },
                                ) { onMonthSelected(m) }
                                .padding(horizontal = space.padding_1_5x, vertical = space.padding_1x),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = monthNames[m - 1],
                                style = type.body,
                                color = when {
                                    isSelected -> colors.bg
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
}
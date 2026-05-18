package com.dv.moneym.feature.transactions.list.components

import com.dv.moneym.core.ui.imageVector
import com.dv.moneym.core.model.Icon
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import moneym.feature.transactions.generated.resources.Res
import moneym.feature.transactions.generated.resources.transactions_cancel
import moneym.feature.transactions.generated.resources.transactions_dialog_select_month
import moneym.feature.transactions.generated.resources.transactions_next_year_cd
import moneym.feature.transactions.generated.resources.transactions_now
import moneym.feature.transactions.generated.resources.transactions_ok
import moneym.feature.transactions.generated.resources.transactions_prev_year_cd
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock

@Composable
internal fun MonthPickerDialog(
    currentYear: Int,
    currentMonth: Int,
    onDismiss: () -> Unit,
    onConfirm: (year: Int, month: Int) -> Unit,
) {
    val colors = MM.colors
    val type = MM.type

    var selectedYear by remember { mutableIntStateOf(currentYear) }
    var selectedMonth by remember { mutableIntStateOf(currentMonth) }

    val todayDate = remember {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    }
    val nowYear = todayDate.year
    val nowMonth = todayDate.monthNumber

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(Res.string.transactions_dialog_select_month),
                style = type.title3,
                color = colors.text,
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(MM.dimen.padding_2x),
            ) {
                // Year selection row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    MmIconButton(
                        icon = Icon.ChevronLeft.imageVector,
                        onClick = { selectedYear-- },
                        size = MM.dimen.padding_4x,
                        contentDescription = stringResource(Res.string.transactions_prev_year_cd),
                    )
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
                        contentDescription = stringResource(Res.string.transactions_next_year_cd),
                    )
                }

                MonthGrid(
                    selectedMonth = selectedMonth,
                    selectedYear = selectedYear,
                    nowMonth = nowMonth,
                    nowYear = nowYear,
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
                    Text(stringResource(Res.string.transactions_now), color = colors.text2)
                }
                TextButton(onClick = { onConfirm(selectedYear, selectedMonth) }) {
                    Text(stringResource(Res.string.transactions_ok), color = colors.accent)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.transactions_cancel), color = colors.text2)
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
    onMonthSelected: (Int) -> Unit,
) {
    val colors = MM.colors
    val type = MM.type
    val monthNames = localizedMonthAbbreviations()

    // Month grid — 4 rows × 3 columns
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
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(MM.dimen.padding_1x))
                            .background(if (isSelected) colors.accent else Color.Transparent)
                            .then(
                                if (isNow && !isSelected) {
                                    Modifier.border(
                                        1.dp,
                                        colors.accent.copy(alpha = 0.5f),
                                        RoundedCornerShape(MM.dimen.padding_1x)
                                    )
                                } else Modifier
                            )
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                            ) { onMonthSelected(m) }
                            .padding(
                                horizontal = MM.dimen.padding_1_5x,
                                vertical = MM.dimen.padding_1x
                            ),
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

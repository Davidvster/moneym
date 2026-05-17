package com.dv.moneym.feature.overview.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.ui.MmIconButton
import com.dv.moneym.core.ui.MmIcons
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import moneym.feature.overview.generated.resources.Res
import moneym.feature.overview.generated.resources.overview_cancel
import moneym.feature.overview.generated.resources.overview_dialog_select_year
import moneym.feature.overview.generated.resources.overview_next_year_cd
import moneym.feature.overview.generated.resources.overview_now
import moneym.feature.overview.generated.resources.overview_ok
import moneym.feature.overview.generated.resources.overview_prev_year_cd
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock

@Composable
internal fun OverviewYearPickerDialog(
    currentYear: Int,
    onDismiss: () -> Unit,
    onConfirm: (year: Int) -> Unit,
) {
    val colors = MM.colors
    val type = MM.type
    val space = MM.space

    var selectedYear by remember { mutableIntStateOf(currentYear) }
    val nowYear = remember {
        Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault()).date.year
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(Res.string.overview_dialog_select_year), style = type.title3, color = colors.text)
        },
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth(),
            ) {
                MmIconButton(
                    icon = MmIcons.chevronLeft,
                    onClick = { selectedYear-- },
                    size = 32.dp,
                    contentDescription = stringResource(Res.string.overview_prev_year_cd),
                )
                Text(
                    text = selectedYear.toString(),
                    style = type.body,
                    color = if (selectedYear == nowYear) colors.accent else colors.text,
                    modifier = Modifier.widthIn(min = 80.dp),
                    textAlign = TextAlign.Center,
                )
                MmIconButton(
                    icon = MmIcons.chevronRight,
                    onClick = { selectedYear++ },
                    size = 32.dp,
                    contentDescription = stringResource(Res.string.overview_next_year_cd),
                )
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(space.padding_0_5x),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { onConfirm(nowYear) }) {
                    Text(stringResource(Res.string.overview_now), color = colors.text2)
                }
                TextButton(onClick = { onConfirm(selectedYear) }) {
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
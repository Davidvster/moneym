package com.dv.moneym.feature.settings.overview.export

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Icon as Material3Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.model.Icon
import com.dv.moneym.core.ui.MmButton
import com.dv.moneym.core.ui.MmButtonVariant
import com.dv.moneym.core.ui.MmCard
import com.dv.moneym.core.ui.MmDateRangePickerDialog
import com.dv.moneym.core.ui.MmSegmented
import com.dv.moneym.core.ui.MmSegmentedSize
import com.dv.moneym.core.ui.MmSheetHeader
import com.dv.moneym.core.ui.ScreenHeader
import com.dv.moneym.core.ui.SectionLabel
import com.dv.moneym.core.ui.imageVector
import com.dv.moneym.feature.settings.overview.importdata.CsvSourceFormat
import com.dv.moneym.feature.settings.overview.importdata.ImportSourceSheet
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import moneym.feature.settings.generated.resources.Res
import moneym.feature.settings.generated.resources.settings_export_date_all
import moneym.feature.settings.generated.resources.settings_export_date_clear
import moneym.feature.settings.generated.resources.settings_export_date_from
import moneym.feature.settings.generated.resources.settings_export_date_ok
import moneym.feature.settings.generated.resources.settings_export_date_range
import moneym.feature.settings.generated.resources.settings_export_date_to
import moneym.feature.settings.generated.resources.settings_export_desc
import moneym.feature.settings.generated.resources.settings_export_format
import moneym.feature.settings.generated.resources.settings_export_import_data_title
import moneym.feature.settings.generated.resources.settings_export_start
import moneym.feature.settings.generated.resources.settings_import
import moneym.feature.settings.generated.resources.settings_import_desc
import moneym.feature.settings.generated.resources.settings_payment_mode_cancel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock

@Serializable
data object ExportDataKey : NavKey

fun EntryProviderScope<NavKey>.exportDataEntry(
    onBack: () -> Unit,
    onExportReady: (suspend (String, String, String) -> Unit)? = null,
    onImportSourceSelected: (CsvSourceFormat) -> Unit = {},
    metadata: Map<String, Any> = emptyMap(),
) = entry<ExportDataKey>(metadata = metadata) {
    ExportScreen(
        onBack = onBack,
        onExportReady = onExportReady,
        onImportSourceSelected = onImportSourceSelected,
    )
}

@Composable
private fun ExportScreen(
    onBack: () -> Unit,
    onExportReady: (suspend (String, String, String) -> Unit)? = null,
    onImportSourceSelected: (CsvSourceFormat) -> Unit = {},
    viewModel: ExportViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ExportEffect.ExportReady -> {
                    onExportReady?.invoke(effect.fileName, effect.content, effect.mimeType)
                }

                ExportEffect.ImportRequested -> Unit
            }
        }
    }

    ExportContent(
        state = state,
        onIntent = viewModel::onIntent,
        onImportSourceSelected = onImportSourceSelected,
        onBack = onBack,
    )
}

@Composable
private fun ExportContent(
    state: ExportUiState,
    onIntent: (ExportIntent) -> Unit,
    onImportSourceSelected: (CsvSourceFormat) -> Unit,
    onBack: () -> Unit,
) {
    val colors = MM.colors
    val type = MM.type
    val space = MM.dimen

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        ScreenHeader(
            title = stringResource(Res.string.settings_export_import_data_title),
            onBack = onBack,
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(
                horizontal = space.padding_2x,
                vertical = space.padding_2x,
            ),
            verticalArrangement = Arrangement.spacedBy(space.padding_3x),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(space.padding_1x)) {
                    SectionLabel(text = stringResource(Res.string.settings_export_start))
                    MmCard(padded = true, shape = MM.dimen.radius_1_5x) {
                        Column(verticalArrangement = Arrangement.spacedBy(space.padding_2x)) {
                            Text(
                                text = stringResource(Res.string.settings_export_desc),
                                style = type.body,
                                color = colors.text2,
                            )
                            MmButton(
                                text = stringResource(Res.string.settings_export_start),
                                onClick = { onIntent(ExportIntent.ShowExportSheet(true)) },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !state.isExporting,
                            )
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(space.padding_1x)) {
                    SectionLabel(text = stringResource(Res.string.settings_import))
                    MmCard(padded = true, shape = MM.dimen.radius_1_5x) {
                        Column(verticalArrangement = Arrangement.spacedBy(space.padding_2x)) {
                            Text(
                                text = stringResource(Res.string.settings_import_desc),
                                style = type.body,
                                color = colors.text2,
                            )
                            MmButton(
                                text = stringResource(Res.string.settings_import),
                                onClick = { onIntent(ExportIntent.ShowImportSheet(true)) },
                                modifier = Modifier.fillMaxWidth(),
                                variant = MmButtonVariant.Secondary,
                            )
                        }
                    }
                }
            }
        }
    }

    if (state.showExportSheet) {
        ExportOptionsSheet(
            state = state,
            onIntent = onIntent,
        )
    }

    if (state.showImportSheet) {
        ImportSourceSheet(
            onMoneyMSelected = {
                onIntent(ExportIntent.ShowImportSheet(false))
                onImportSourceSelected(CsvSourceFormat.MONEYM)
            },
            onEasyHomeFinanceSelected = {
                onIntent(ExportIntent.ShowImportSheet(false))
                onImportSourceSelected(CsvSourceFormat.EASY_HOME_FINANCE)
            },
            onDismiss = { onIntent(ExportIntent.ShowImportSheet(false)) },
        )
    }

    if (state.showExportDateDialog) {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val start = state.exportStartDate ?: today
        val end = state.exportEndDate ?: today
        MmDateRangePickerDialog(
            initStartYear = start.year,
            initStartMonth = start.month.number,
            initStartDay = start.day,
            initEndYear = end.year,
            initEndMonth = end.month.number,
            initEndDay = end.day,
            title = stringResource(Res.string.settings_export_date_range),
            fromLabel = stringResource(Res.string.settings_export_date_from),
            toLabel = stringResource(Res.string.settings_export_date_to),
            okLabel = stringResource(Res.string.settings_export_date_ok),
            cancelLabel = stringResource(Res.string.settings_payment_mode_cancel),
            onDismiss = { onIntent(ExportIntent.ShowExportDateDialog(false)) },
            onConfirm = { sy, sm, sd, ey, em, ed ->
                onIntent(
                    ExportIntent.SetExportDateRange(
                        LocalDate(sy, sm, sd),
                        LocalDate(ey, em, ed),
                    )
                )
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExportOptionsSheet(
    state: ExportUiState,
    onIntent: (ExportIntent) -> Unit,
) {
    val colors = MM.colors
    val type = MM.type
    val space = MM.dimen
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = { onIntent(ExportIntent.ShowExportSheet(false)) },
        sheetState = sheetState,
        shape = RoundedCornerShape(
            topStart = space.padding_2_5x,
            topEnd = space.padding_2_5x,
        ),
        containerColor = colors.bg,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = space.padding_2_5x,
                vertical = space.padding_3x,
            ),
            verticalArrangement = Arrangement.spacedBy(space.padding_2x),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 36.dp, height = space.padding_0_5x)
                        .clip(RoundedCornerShape(50))
                        .background(colors.borderStrong),
                )
            }

            MmSheetHeader(
                title = stringResource(Res.string.settings_export_start),
                onClose = { onIntent(ExportIntent.ShowExportSheet(false)) },
            )

            Column(verticalArrangement = Arrangement.spacedBy(space.padding_1x)) {
                SectionLabel(text = stringResource(Res.string.settings_export_format))
                MmSegmented(
                    options = listOf("JSON", "CSV"),
                    selectedIndex = if (state.exportFormatCsv) 1 else 0,
                    onOptionSelected = { onIntent(ExportIntent.SetExportFormat(it == 1)) },
                    size = MmSegmentedSize.Sm,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(space.padding_1x)) {
                SectionLabel(text = stringResource(Res.string.settings_export_date_range))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(space.radius_1_5x)
                        .clickable { onIntent(ExportIntent.ShowExportDateDialog(true)) }
                        .background(colors.surface, space.radius_1_5x)
                        .padding(space.padding_2x),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(space.padding_1x),
                ) {
                    val rangeText = if (state.exportStartDate == null && state.exportEndDate == null) {
                        stringResource(Res.string.settings_export_date_all)
                    } else {
                        "${formatDay(state.exportStartDate)} – ${formatDay(state.exportEndDate)}"
                    }
                    Text(
                        text = rangeText,
                        style = type.body,
                        color = colors.text,
                        modifier = Modifier.weight(1f),
                    )
                    Material3Icon(
                        imageVector = Icon.Calendar.imageVector,
                        contentDescription = null,
                        tint = colors.text2,
                        modifier = Modifier.size(space.icon_1x),
                    )
                }
                if (state.exportStartDate != null || state.exportEndDate != null) {
                    Text(
                        text = stringResource(Res.string.settings_export_date_clear),
                        style = type.body,
                        color = colors.accent,
                        modifier = Modifier
                            .clip(space.radius_1x)
                            .clickable { onIntent(ExportIntent.ClearExportDateRange) }
                            .padding(
                                horizontal = space.padding_1x,
                                vertical = space.padding_0_5x,
                            ),
                    )
                }
            }

            MmButton(
                text = stringResource(Res.string.settings_export_start),
                onClick = { onIntent(ExportIntent.ExportRequested) },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(space.padding_1x))
        }
    }
}

private fun formatDay(date: LocalDate?): String =
    if (date == null) "" else "${date.day}.${date.month.number}.${date.year}"

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun ExportContentPreview() {
    com.dv.moneym.core.designsystem.MoneyMTheme {
        ExportContent(
            state = ExportUiState(),
            onIntent = {},
            onImportSourceSelected = {},
            onBack = {},
        )
    }
}

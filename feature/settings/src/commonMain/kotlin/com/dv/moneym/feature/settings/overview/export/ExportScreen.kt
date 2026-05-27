package com.dv.moneym.feature.settings.overview.export

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.ui.MmButton
import com.dv.moneym.core.ui.MmButtonSize
import com.dv.moneym.core.ui.MmButtonVariant
import com.dv.moneym.core.ui.MmCard
import com.dv.moneym.core.ui.MmSegmented
import com.dv.moneym.core.ui.MmSegmentedSize
import com.dv.moneym.core.ui.ScreenHeader
import com.dv.moneym.core.ui.SectionLabel
import com.dv.moneym.feature.settings.overview.importdata.CsvSourceFormat
import com.dv.moneym.feature.settings.overview.importdata.ImportSourceSheet
import kotlinx.serialization.Serializable
import moneym.feature.settings.generated.resources.Res
import moneym.feature.settings.generated.resources.settings_export_as_csv
import moneym.feature.settings.generated.resources.settings_export_as_json
import moneym.feature.settings.generated.resources.settings_export_format
import moneym.feature.settings.generated.resources.settings_export_import_data_title
import moneym.feature.settings.generated.resources.settings_export_start
import moneym.feature.settings.generated.resources.settings_import_csv
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

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
    var showImportSheet by remember { mutableStateOf(false) }

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
        isExporting = state.isExporting,
        onExportJson = { viewModel.onIntent(ExportIntent.ExportJsonRequested) },
        onExportCsv = { viewModel.onIntent(ExportIntent.ExportCsvRequested) },
        onImport = { showImportSheet = true },
        onBack = onBack,
    )

    if (showImportSheet) {
        ImportSourceSheet(
            onMoneyMSelected = {
                showImportSheet = false
                onImportSourceSelected(CsvSourceFormat.MONEYM)
            },
            onEasyHomeFinanceSelected = {
                showImportSheet = false
                onImportSourceSelected(CsvSourceFormat.EASY_HOME_FINANCE)
            },
            onDismiss = { showImportSheet = false },
        )
    }
}

@Composable
private fun ExportContent(
    isExporting: Boolean,
    onExportJson: () -> Unit,
    onExportCsv: () -> Unit,
    onImport: () -> Unit,
    onBack: () -> Unit,
) {
    val colors = MM.colors
    val type = MM.type
    val space = MM.dimen

    // 0 = JSON, 1 = CSV
    var formatIndex by remember { mutableStateOf(0) }

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
            verticalArrangement = Arrangement.spacedBy(space.padding_2x),
        ) {
            item {
                SectionLabel(
                    text = stringResource(Res.string.settings_export_format),
                    modifier = Modifier.padding(bottom = space.padding_0_5x),
                )
                MmCard(padded = true, shape = MM.dimen.radius_1_5x) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (formatIndex == 0)
                                stringResource(Res.string.settings_export_as_json)
                            else
                                stringResource(Res.string.settings_export_as_csv),
                            style = type.body,
                            color = colors.text,
                            modifier = Modifier.weight(1f),
                        )
                        MmSegmented(
                            options = listOf("JSON", "CSV"),
                            selectedIndex = formatIndex,
                            onOptionSelected = { formatIndex = it },
                            size = MmSegmentedSize.Sm,
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(space.padding_1x))
                MmButton(
                    text = stringResource(Res.string.settings_export_start),
                    onClick = {
                        if (formatIndex == 0) onExportJson() else onExportCsv()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    size = MmButtonSize.Lg,
                    enabled = !isExporting,
                )
            }

            item {
                MmButton(
                    text = stringResource(Res.string.settings_import_csv),
                    onClick = onImport,
                    modifier = Modifier.fillMaxWidth(),
                    size = MmButtonSize.Lg,
                    variant = MmButtonVariant.Secondary,
                )
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun ExportContentPreview() {
    com.dv.moneym.core.designsystem.MoneyMTheme {
        ExportContent(
            isExporting = false,
            onExportJson = {},
            onExportCsv = {},
            onImport = {},
            onBack = {},
        )
    }
}

package com.dv.moneym.feature.settings.overview.transactiondisplay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.model.Density
import com.dv.moneym.core.model.Icon
import com.dv.moneym.core.model.IndicatorStyle
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.model.TxDisplayPrefs
import com.dv.moneym.core.ui.CategoryIconTile
import com.dv.moneym.core.ui.MmCard
import com.dv.moneym.core.ui.MmRow
import com.dv.moneym.core.ui.MmToggle
import com.dv.moneym.core.ui.ScreenHeader
import com.dv.moneym.core.ui.SectionLabel
import com.dv.moneym.core.ui.TxRow
import com.dv.moneym.core.ui.imageVector
import com.dv.moneym.feature.settings.overview.TxListDisplayKey
import moneym.feature.settings.generated.resources.Res
import moneym.feature.settings.generated.resources.settings_default_tx_expense
import moneym.feature.settings.generated.resources.settings_default_tx_income
import moneym.feature.settings.generated.resources.settings_default_tx_type
import moneym.feature.settings.generated.resources.settings_tx_list_display_title
import moneym.feature.settings.generated.resources.settings_txdisplay_category_name
import moneym.feature.settings.generated.resources.settings_txdisplay_color_indicator
import moneym.feature.settings.generated.resources.settings_txdisplay_comfortable
import moneym.feature.settings.generated.resources.settings_txdisplay_compact
import moneym.feature.settings.generated.resources.settings_txdisplay_daily_sums
import moneym.feature.settings.generated.resources.settings_txdisplay_density
import moneym.feature.settings.generated.resources.settings_txdisplay_normal
import moneym.feature.settings.generated.resources.settings_txdisplay_pending_recurring
import moneym.feature.settings.generated.resources.settings_txdisplay_note
import moneym.feature.settings.generated.resources.settings_txdisplay_preview
import moneym.feature.settings.generated.resources.settings_txdisplay_show
import moneym.feature.settings.generated.resources.settings_txdisplay_style_bar
import moneym.feature.settings.generated.resources.settings_txdisplay_style_dot
import moneym.feature.settings.generated.resources.settings_txdisplay_style_minimal
import moneym.feature.settings.generated.resources.settings_txdisplay_style_soft
import moneym.feature.settings.generated.resources.settings_txdisplay_style_tile
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

fun EntryProviderScope<NavKey>.txListDisplayEntry(
    onBack: () -> Unit,
    metadata: Map<String, Any> = emptyMap(),
) = entry<TxListDisplayKey>(metadata = metadata) {
    TxListDisplayScreen(onBack = onBack)
}

// Sample transaction data for preview
private data class SampleTx(
    val categoryName: String,
    val categoryColor: Color,
    val note: String?,
    val isExpense: Boolean,
    val amount: Double,
    val currency: String = "EUR",
)

private val sampleColor = Color(0xFF7A9572) // catGroceries
private val sampleIncomeColor = Color(0xFF4A8E5C) // catSalary
private val sampleEatingColor = Color(0xFFC97A4F) // catEatingOut

private val sampleTransactions = listOf(
    SampleTx("Groceries", sampleColor, "Weekly shop at market", isExpense = true, amount = 48.50),
    SampleTx("Salary", sampleIncomeColor, "June salary", isExpense = false, amount = 2800.00),
    SampleTx("Eating out", sampleEatingColor, null, isExpense = true, amount = 22.90),
)

@Composable
private fun indicatorDescription(style: IndicatorStyle): String = when (style) {
    IndicatorStyle.IconTile -> stringResource(Res.string.settings_txdisplay_style_tile)
    IndicatorStyle.SoftIcon -> stringResource(Res.string.settings_txdisplay_style_soft)
    IndicatorStyle.Bar -> stringResource(Res.string.settings_txdisplay_style_bar)
    IndicatorStyle.Dot -> stringResource(Res.string.settings_txdisplay_style_dot)
    IndicatorStyle.Minimal -> stringResource(Res.string.settings_txdisplay_style_minimal)
}

@Composable
private fun TxListDisplayScreen(
    onBack: () -> Unit,
    viewModel: TxListDisplayViewModel = koinViewModel(),
) {
    val currentPrefs by viewModel.txDisplayPrefs.collectAsStateWithLifecycle()
    val defaultTransactionType by viewModel.defaultTransactionType.collectAsStateWithLifecycle()
    val showPendingRecurring by viewModel.showPendingRecurring.collectAsStateWithLifecycle()

    TxListDisplayContent(
        currentPrefs = currentPrefs,
        onPrefsChanged = { viewModel.onIntent(TxListDisplayIntent.SetTxDisplayPrefs(it)) },
        defaultTransactionType = defaultTransactionType,
        onDefaultTransactionTypeChanged = { viewModel.onIntent(TxListDisplayIntent.SetDefaultTransactionType(it)) },
        showPendingRecurring = showPendingRecurring,
        onShowPendingRecurringChanged = { viewModel.onIntent(TxListDisplayIntent.SetShowPendingRecurring(it)) },
        onBack = onBack,
    )
}

@Composable
private fun TxListDisplayContent(
    currentPrefs: TxDisplayPrefs,
    onPrefsChanged: (TxDisplayPrefs) -> Unit,
    defaultTransactionType: TransactionType,
    onDefaultTransactionTypeChanged: (TransactionType) -> Unit,
    showPendingRecurring: Boolean,
    onShowPendingRecurringChanged: (Boolean) -> Unit,
    onBack: () -> Unit,
) {

    Column(Modifier.fillMaxSize().background(MM.colors.bg)) {
        ScreenHeader(stringResource(Res.string.settings_tx_list_display_title), onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            ItemPreviewPanel(
                currentPrefs = currentPrefs
            )

            ItemIndicatorSection(
                currentPrefs = currentPrefs,
                onPrefsChanged = onPrefsChanged
            )

            ItemDetailsShowSection(
                currentPrefs = currentPrefs,
                onPrefsChanged = onPrefsChanged
            )

            ItemDensitySection(
                currentPrefs = currentPrefs,
                onPrefsChanged = onPrefsChanged
            )

            ExtraShowOptions(
                currentPrefs = currentPrefs,
                onPrefsChanged = onPrefsChanged,
                showPendingRecurring = showPendingRecurring,
                onShowPendingRecurringChanged = onShowPendingRecurringChanged,
            )

            DefaultTransactionType(
                defaultTransactionType = defaultTransactionType,
                onDefaultTransactionTypeChanged = onDefaultTransactionTypeChanged
            )

            Box(Modifier.padding(bottom = MM.dimen.padding_3x))
        }
    }
}

@Composable
private fun ItemPreviewPanel(
    currentPrefs: TxDisplayPrefs,
) {
    val dividerColor = MM.colors.divider
    Box(
        Modifier
            .fillMaxWidth()
            .background(MM.colors.surface2)
            .drawBehind {
                val strokeWidth = 1.dp.toPx()
                drawLine(
                    color = dividerColor,
                    start = Offset(0f, size.height - strokeWidth / 2),
                    end = Offset(size.width, size.height - strokeWidth / 2),
                    strokeWidth = strokeWidth,
                )
            }
            .padding(horizontal = MM.dimen.padding_2_5x, vertical = MM.dimen.padding_3x),
    ) {
        Column {
            SectionLabel(
                text = stringResource(Res.string.settings_txdisplay_preview),
                modifier = Modifier.padding(bottom = MM.dimen.padding_0_5x)
            )
            MmCard {
                sampleTransactions.forEachIndexed { i, tx ->
                    TxRow(
                        categoryName = tx.categoryName,
                        categoryColor = tx.categoryColor,
                        categoryIcon = Icon.Basket.imageVector,
                        note = tx.note,
                        isExpense = tx.isExpense,
                        amountValue = tx.amount,
                        currency = tx.currency,
                        prefs = currentPrefs,
                        divider = i < sampleTransactions.size - 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun ItemIndicatorSection(
    currentPrefs: TxDisplayPrefs,
    onPrefsChanged: (TxDisplayPrefs) -> Unit,
) {
    SectionLabel(
        text = stringResource(Res.string.settings_txdisplay_color_indicator),
        modifier = Modifier.padding(
            start = MM.dimen.padding_2_5x,
            end = MM.dimen.padding_2_5x,
            top = MM.dimen.padding_2x,
            bottom = MM.dimen.padding_0_5x
        ),
    )
    MmCard(Modifier.padding(horizontal = MM.dimen.padding_2x), shape = MM.dimen.radius_1_5x) {
        val styles = IndicatorStyle.entries
        styles.forEachIndexed { i, opt ->
            val isLast = i == styles.size - 1
            val isSelected = currentPrefs.indicatorStyle == opt

            MmRow(
                onClick = { onPrefsChanged(currentPrefs.copy(indicatorStyle = opt)) },
                divider = !isLast,
            ) {
                // Mini preview
                Box(Modifier.size(38.dp), contentAlignment = Alignment.Center) {
                    when (opt) {
                        IndicatorStyle.IconTile ->
                            CategoryIconTile(
                                categoryName = "Groceries",
                                categoryColor = sampleColor,
                                categoryIcon = Icon.Basket.imageVector,
                                size = MM.dimen.padding_4x,
                                variant = IndicatorStyle.IconTile,
                            )

                        IndicatorStyle.SoftIcon ->
                            CategoryIconTile(
                                categoryName = "Groceries",
                                categoryColor = sampleColor,
                                categoryIcon = Icon.Basket.imageVector,
                                size = MM.dimen.padding_4x,
                                variant = IndicatorStyle.SoftIcon,
                            )

                        IndicatorStyle.Bar ->
                            CategoryIconTile(
                                categoryName = "Groceries",
                                categoryColor = sampleColor,
                                categoryIcon = Icon.Basket.imageVector,
                                size = MM.dimen.padding_4x,
                                variant = IndicatorStyle.Bar,
                            )

                        IndicatorStyle.Dot ->
                            CategoryIconTile(
                                categoryName = "Groceries",
                                categoryColor = sampleColor,
                                categoryIcon = Icon.Basket.imageVector,
                                size = 10.dp,
                                variant = IndicatorStyle.Dot,
                            )

                        IndicatorStyle.Minimal ->
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .size(width = MM.dimen.padding_3x, height = MM.dimen.strokeHairline)
                                    .background(MM.colors.border),
                            )
                    }
                }

                Column(Modifier.weight(1f)) {
                    Text(
                        text = opt.name.replace(Regex("([A-Z])"), " $1").trim(),
                        style = MM.type.body,
                        color = MM.colors.text,
                    )
                    Text(
                        text = indicatorDescription(opt),
                        style = MM.type.caption.copy(color = MM.colors.text2),
                    )
                }

                // Custom radio circle — not M3 RadioButton
                Box(
                    modifier = Modifier
                        .size(MM.dimen.iconLg)
                        .clip(CircleShape)
                        .border(
                            1.5.dp,
                            if (isSelected) MM.colors.accent else MM.colors.borderStrong,
                            CircleShape,
                        )
                        .background(
                            if (isSelected) MM.colors.accent else Color.Transparent,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icon.Check.imageVector,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(MM.dimen.padding_1_5x),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemDetailsShowSection(
    currentPrefs: TxDisplayPrefs,
    onPrefsChanged: (TxDisplayPrefs) -> Unit,
) {
    SectionLabel(
        text = stringResource(Res.string.settings_txdisplay_show),
        modifier = Modifier.padding(
            start = MM.dimen.padding_2_5x,
            end = MM.dimen.padding_2_5x,
            top = MM.dimen.padding_2x,
            bottom = MM.dimen.padding_0_5x
        ),
    )
    MmCard(Modifier.padding(horizontal = MM.dimen.padding_2x), shape = MM.dimen.radius_1_5x) {
        // Category name row — row click is the single source of truth; toggle is display-only
        MmRow(onClick = { onPrefsChanged(currentPrefs.copy(showCategoryName = !currentPrefs.showCategoryName)) }) {
            Text(
                stringResource(Res.string.settings_txdisplay_category_name),
                style = MM.type.body,
                color = MM.colors.text,
                modifier = Modifier.weight(1f),
            )
            MmToggle(
                checked = currentPrefs.showCategoryName,
                onCheckedChange = { onPrefsChanged(currentPrefs.copy(showCategoryName = !currentPrefs.showCategoryName)) },
            )
        }
        MmRow(
            divider = true,
            onClick = { onPrefsChanged(currentPrefs.copy(showNote = !currentPrefs.showNote)) },
        ) {
            Text(
                stringResource(Res.string.settings_txdisplay_note),
                style = MM.type.body,
                color = MM.colors.text,
                modifier = Modifier.weight(1f),
            )
            MmToggle(
                checked = currentPrefs.showNote,
                onCheckedChange = { onPrefsChanged(currentPrefs.copy(showNote = !currentPrefs.showNote)) },
            )
        }
        MmRow(
            divider = false,
            onClick = { onPrefsChanged(currentPrefs.copy(showDailySums = !currentPrefs.showDailySums)) },
        ) {
            Text(
                stringResource(Res.string.settings_txdisplay_daily_sums),
                style = MM.type.body,
                color = MM.colors.text,
                modifier = Modifier.weight(1f),
            )
            MmToggle(
                checked = currentPrefs.showDailySums,
                onCheckedChange = { onPrefsChanged(currentPrefs.copy(showDailySums = !currentPrefs.showDailySums)) },
            )
        }
    }
}

@Composable
private fun ItemDensitySection(
    currentPrefs: TxDisplayPrefs,
    onPrefsChanged: (TxDisplayPrefs) -> Unit,
) {
    SectionLabel(
        text = stringResource(Res.string.settings_txdisplay_density),
        modifier = Modifier.padding(
            start = MM.dimen.padding_2_5x,
            end = MM.dimen.padding_2_5x,
            top = MM.dimen.padding_2x,
            bottom = MM.dimen.padding_0_5x
        ),
    )
    MmCard(Modifier.padding(horizontal = MM.dimen.padding_2x), shape = MM.dimen.radius_1_5x) {
        val densityOptions = listOf(Density.Compact, Density.Normal, Density.Comfortable)
        val densityLabels = mapOf(
            Density.Compact to stringResource(Res.string.settings_txdisplay_compact),
            Density.Normal to stringResource(Res.string.settings_txdisplay_normal),
            Density.Comfortable to stringResource(Res.string.settings_txdisplay_comfortable),
        )
        densityOptions.forEachIndexed { i, opt ->
            val isLast = i == densityOptions.size - 1
            val isSelected = currentPrefs.density == opt
            MmRow(
                onClick = { onPrefsChanged(currentPrefs.copy(density = opt)) },
                divider = !isLast,
            ) {
                Text(
                    text = densityLabels[opt] ?: opt.name,
                    style = MM.type.body,
                    color = MM.colors.text,
                    modifier = Modifier.weight(1f),
                )
                // Custom radio circle
                Box(
                    modifier = Modifier
                        .size(MM.dimen.iconLg)
                        .clip(CircleShape)
                        .border(
                            1.5.dp,
                            if (isSelected) MM.colors.accent else MM.colors.borderStrong,
                            CircleShape,
                        )
                        .background(
                            if (isSelected) MM.colors.accent else Color.Transparent,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icon.Check.imageVector,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(MM.dimen.padding_1_5x),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExtraShowOptions(
    currentPrefs: TxDisplayPrefs,
    onPrefsChanged: (TxDisplayPrefs) -> Unit,
    showPendingRecurring: Boolean,
    onShowPendingRecurringChanged: (Boolean) -> Unit,
) {
    SectionLabel(
        text = stringResource(Res.string.settings_txdisplay_show),
        modifier = Modifier.padding(
            start = MM.dimen.padding_2_5x,
            end = MM.dimen.padding_2_5x,
            top = MM.dimen.padding_2x,
            bottom = MM.dimen.padding_0_5x
        ),
    )
    MmCard(Modifier.padding(horizontal = MM.dimen.padding_2x), shape = MM.dimen.radius_1_5x) {
        MmRow(
            divider = true,
            onClick = { onPrefsChanged(currentPrefs.copy(showDailySums = !currentPrefs.showDailySums)) },
        ) {
            Text(
                stringResource(Res.string.settings_txdisplay_daily_sums),
                style = MM.type.body,
                color = MM.colors.text,
                modifier = Modifier.weight(1f),
            )
            MmToggle(
                checked = currentPrefs.showDailySums,
                onCheckedChange = { onPrefsChanged(currentPrefs.copy(showDailySums = !currentPrefs.showDailySums)) },
            )
        }
        MmRow(
            divider = false,
            onClick = { onShowPendingRecurringChanged(!showPendingRecurring) },
        ) {
            Text(
                stringResource(Res.string.settings_txdisplay_pending_recurring),
                style = MM.type.body,
                color = MM.colors.text,
                modifier = Modifier.weight(1f),
            )
            MmToggle(
                checked = showPendingRecurring,
                onCheckedChange = onShowPendingRecurringChanged,
            )
        }
    }
}

@Composable
private fun DefaultTransactionType(
    defaultTransactionType: TransactionType,
    onDefaultTransactionTypeChanged: (TransactionType) -> Unit,
) {
    SectionLabel(
        text = stringResource(Res.string.settings_default_tx_type),
        modifier = Modifier.padding(
            start = MM.dimen.padding_2_5x,
            end = MM.dimen.padding_2_5x,
            top = MM.dimen.padding_2x,
            bottom = MM.dimen.padding_0_5x
        ),
    )
    MmCard(Modifier.padding(horizontal = MM.dimen.padding_2x), shape = MM.dimen.radius_1_5x) {
        val txTypes = listOf(TransactionType.EXPENSE, TransactionType.INCOME)
        val txTypesLabels = mapOf(
            TransactionType.EXPENSE to stringResource(Res.string.settings_default_tx_expense),
            TransactionType.INCOME to stringResource(Res.string.settings_default_tx_income),
        )
        txTypes.forEachIndexed { i, type ->
            val isLast = i == txTypes.lastIndex
            val isSelected = defaultTransactionType == type
            MmRow(
                onClick = { onDefaultTransactionTypeChanged(type) },
                divider = !isLast,
            ) {
                Text(
                    text = txTypesLabels[type] ?: type.name,
                    style = MM.type.body,
                    color = MM.colors.text,
                    modifier = Modifier.weight(1f),
                )
                // Custom radio circle
                Box(
                    modifier = Modifier
                        .size(MM.dimen.iconLg)
                        .clip(CircleShape)
                        .border(
                            1.5.dp,
                            if (isSelected) MM.colors.accent else MM.colors.borderStrong,
                            CircleShape,
                        )
                        .background(
                            if (isSelected) MM.colors.accent else Color.Transparent,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icon.Check.imageVector,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(MM.dimen.padding_1_5x),
                        )
                    }
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@androidx.compose.runtime.Composable
private fun TxListDisplayContentPreview() {
    com.dv.moneym.core.designsystem.MoneyMTheme {
        TxListDisplayContent(
            currentPrefs = com.dv.moneym.core.model.TxDisplayPrefs(),
            onPrefsChanged = {},
            defaultTransactionType = com.dv.moneym.core.model.TransactionType.EXPENSE,
            onDefaultTransactionTypeChanged = {},
            showPendingRecurring = true,
            onShowPendingRecurringChanged = {},
            onBack = {},
        )
    }
}

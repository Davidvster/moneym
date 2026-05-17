package com.dv.moneym.feature.settings.settings.transactiondisplay

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
import com.dv.moneym.core.model.IndicatorStyle
import com.dv.moneym.core.model.TxDisplayPrefs
import com.dv.moneym.core.ui.CategoryIconTile
import com.dv.moneym.core.ui.MmCard
import com.dv.moneym.core.ui.MmIcons
import com.dv.moneym.core.ui.MmRow
import com.dv.moneym.core.ui.MmToggle
import com.dv.moneym.core.ui.ScreenHeader
import com.dv.moneym.core.ui.SectionLabel
import com.dv.moneym.core.ui.TxRow
import com.dv.moneym.feature.settings.settings.SettingsViewModel
import com.dv.moneym.feature.settings.settings.TxListDisplayKey
import moneym.feature.settings.generated.resources.Res
import moneym.feature.settings.generated.resources.settings_tx_list_display_title
import moneym.feature.settings.generated.resources.settings_txdisplay_category_name
import moneym.feature.settings.generated.resources.settings_txdisplay_color_indicator
import moneym.feature.settings.generated.resources.settings_txdisplay_comfortable
import moneym.feature.settings.generated.resources.settings_txdisplay_compact
import moneym.feature.settings.generated.resources.settings_txdisplay_density
import moneym.feature.settings.generated.resources.settings_txdisplay_normal
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
) = entry<TxListDisplayKey> {
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
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentPrefs = state.txDisplayPrefs

    TxListDisplayContent(
        currentPrefs = currentPrefs,
        onPrefsChanged = { viewModel.setTxDisplayPrefs(it) },
        onBack = onBack,
    )
}

@Composable
private fun TxListDisplayContent(
    currentPrefs: TxDisplayPrefs,
    onPrefsChanged: (TxDisplayPrefs) -> Unit,
    onBack: () -> Unit,
) {
    val colors = MM.colors
    val type = MM.type
    val space = MM.dimen

    val dividerColor = colors.divider

    Column(Modifier.fillMaxSize().background(colors.bg)) {
        ScreenHeader(stringResource(Res.string.settings_tx_list_display_title), onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            // Live preview panel
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(colors.surface2)
                    .drawBehind {
                        val strokeWidth = 1.dp.toPx()
                        drawLine(
                            color = dividerColor,
                            start = Offset(0f, size.height - strokeWidth / 2),
                            end = Offset(size.width, size.height - strokeWidth / 2),
                            strokeWidth = strokeWidth,
                        )
                    }
                    .padding(horizontal = MM.dimen.padding_2_5x, vertical = space.padding_3x),
            ) {
                Column {
                    SectionLabel(
                        stringResource(Res.string.settings_txdisplay_preview),
                        Modifier.padding(bottom = space.padding_0_5x)
                    )
                    MmCard {
                        sampleTransactions.forEachIndexed { i, tx ->
                            TxRow(
                                categoryName = tx.categoryName,
                                categoryColor = tx.categoryColor,
                                categoryIcon = MmIcons.basket,
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

            // COLOR INDICATOR section
            SectionLabel(
                stringResource(Res.string.settings_txdisplay_color_indicator),
                Modifier.padding(
                    start = MM.dimen.padding_2_5x,
                    end = MM.dimen.padding_2_5x,
                    top = space.padding_2x,
                    bottom = space.padding_0_5x
                ),
            )
            MmCard(Modifier.padding(horizontal = space.padding_2x), shape = MM.dimen.radius_1_5x) {
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
                                        categoryIcon = MmIcons.basket,
                                        size = MM.dimen.padding_4x,
                                        variant = IndicatorStyle.IconTile,
                                    )

                                IndicatorStyle.SoftIcon ->
                                    CategoryIconTile(
                                        categoryName = "Groceries",
                                        categoryColor = sampleColor,
                                        categoryIcon = MmIcons.basket,
                                        size = MM.dimen.padding_4x,
                                        variant = IndicatorStyle.SoftIcon,
                                    )

                                IndicatorStyle.Bar ->
                                    CategoryIconTile(
                                        categoryName = "Groceries",
                                        categoryColor = sampleColor,
                                        categoryIcon = MmIcons.basket,
                                        size = MM.dimen.padding_4x,
                                        variant = IndicatorStyle.Bar,
                                    )

                                IndicatorStyle.Dot ->
                                    CategoryIconTile(
                                        categoryName = "Groceries",
                                        categoryColor = sampleColor,
                                        categoryIcon = MmIcons.basket,
                                        size = 10.dp,
                                        variant = IndicatorStyle.Dot,
                                    )

                                IndicatorStyle.Minimal ->
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .size(width = MM.dimen.padding_3x, height = 1.dp)
                                            .background(colors.border),
                                    )
                            }
                        }

                        Column(Modifier.weight(1f)) {
                            Text(
                                text = opt.name.replace(Regex("([A-Z])"), " $1").trim(),
                                style = type.body,
                                color = colors.text,
                            )
                            Text(
                                text = indicatorDescription(opt),
                                style = type.caption.copy(color = colors.text2),
                            )
                        }

                        // Custom radio circle — not M3 RadioButton
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .border(
                                    1.5.dp,
                                    if (isSelected) colors.accent else colors.borderStrong,
                                    CircleShape,
                                )
                                .background(
                                    if (isSelected) colors.accent else Color.Transparent,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = MmIcons.check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(MM.dimen.padding_1_5x),
                                )
                            }
                        }
                    }
                }
            }

            // SHOW section
            SectionLabel(
                stringResource(Res.string.settings_txdisplay_show),
                Modifier.padding(
                    start = MM.dimen.padding_2_5x,
                    end = MM.dimen.padding_2_5x,
                    top = space.padding_2x,
                    bottom = space.padding_0_5x
                ),
            )
            MmCard(Modifier.padding(horizontal = space.padding_2x), shape = MM.dimen.radius_1_5x) {
                // Category name row — row click is the single source of truth; toggle is display-only
                MmRow {
                    Text(
                        stringResource(Res.string.settings_txdisplay_category_name),
                        style = type.body,
                        color = colors.text,
                        modifier = Modifier.weight(1f),
                    )
                    MmToggle(
                        checked = currentPrefs.showCategoryName,
                        onCheckedChange = { onPrefsChanged(currentPrefs.copy(showCategoryName = !currentPrefs.showCategoryName)) },
                    )
                }
                MmRow(
                    divider = false,
                ) {
                    Text(
                        stringResource(Res.string.settings_txdisplay_note),
                        style = type.body,
                        color = colors.text,
                        modifier = Modifier.weight(1f),
                    )
                    MmToggle(
                        checked = currentPrefs.showNote,
                        onCheckedChange = { onPrefsChanged(currentPrefs.copy(showNote = !currentPrefs.showNote)) },
                    )
                }
            }

            // DENSITY section — 3 radio button rows (Compact / Normal / Comfortable)
            SectionLabel(
                stringResource(Res.string.settings_txdisplay_density),
                Modifier.padding(
                    start = MM.dimen.padding_2_5x,
                    end = MM.dimen.padding_2_5x,
                    top = space.padding_2x,
                    bottom = space.padding_0_5x
                ),
            )
            MmCard(Modifier.padding(horizontal = space.padding_2x), shape = MM.dimen.radius_1_5x) {
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
                            style = type.body,
                            color = colors.text,
                            modifier = Modifier.weight(1f),
                        )
                        // Custom radio circle
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .border(
                                    1.5.dp,
                                    if (isSelected) colors.accent else colors.borderStrong,
                                    CircleShape,
                                )
                                .background(
                                    if (isSelected) colors.accent else Color.Transparent,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = MmIcons.check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(MM.dimen.padding_1_5x),
                                )
                            }
                        }
                    }
                }
            }

            // Bottom padding
            Box(Modifier.padding(bottom = MM.dimen.padding_3x))
        }
    }
}

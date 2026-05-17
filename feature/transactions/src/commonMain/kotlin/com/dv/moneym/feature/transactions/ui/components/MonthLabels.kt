package com.dv.moneym.feature.transactions.ui.components

import androidx.compose.runtime.Composable
import moneym.feature.transactions.generated.resources.Res
import moneym.feature.transactions.generated.resources.transactions_month_apr
import moneym.feature.transactions.generated.resources.transactions_month_aug
import moneym.feature.transactions.generated.resources.transactions_month_dec
import moneym.feature.transactions.generated.resources.transactions_month_feb
import moneym.feature.transactions.generated.resources.transactions_month_jan
import moneym.feature.transactions.generated.resources.transactions_month_jul
import moneym.feature.transactions.generated.resources.transactions_month_jun
import moneym.feature.transactions.generated.resources.transactions_month_mar
import moneym.feature.transactions.generated.resources.transactions_month_may
import moneym.feature.transactions.generated.resources.transactions_month_nov
import moneym.feature.transactions.generated.resources.transactions_month_oct
import moneym.feature.transactions.generated.resources.transactions_month_sep
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun localizedMonthAbbreviations(): List<String> = listOf(
    stringResource(Res.string.transactions_month_jan),
    stringResource(Res.string.transactions_month_feb),
    stringResource(Res.string.transactions_month_mar),
    stringResource(Res.string.transactions_month_apr),
    stringResource(Res.string.transactions_month_may),
    stringResource(Res.string.transactions_month_jun),
    stringResource(Res.string.transactions_month_jul),
    stringResource(Res.string.transactions_month_aug),
    stringResource(Res.string.transactions_month_sep),
    stringResource(Res.string.transactions_month_oct),
    stringResource(Res.string.transactions_month_nov),
    stringResource(Res.string.transactions_month_dec),
)

@Composable
internal fun monthLabel(year: Int, month: Int): String {
    val names = listOf(
        stringResource(Res.string.transactions_month_jan),
        stringResource(Res.string.transactions_month_feb),
        stringResource(Res.string.transactions_month_mar),
        stringResource(Res.string.transactions_month_apr),
        stringResource(Res.string.transactions_month_may),
        stringResource(Res.string.transactions_month_jun),
        stringResource(Res.string.transactions_month_jul),
        stringResource(Res.string.transactions_month_aug),
        stringResource(Res.string.transactions_month_sep),
        stringResource(Res.string.transactions_month_oct),
        stringResource(Res.string.transactions_month_nov),
        stringResource(Res.string.transactions_month_dec),
    )
    return "${names[month - 1]} $year"
}

package com.dv.moneym.core.ui

import androidx.compose.runtime.Composable
import com.dv.moneym.core.model.YearMonth
import moneym.core.ui.generated.resources.Res
import moneym.core.ui.generated.resources.month_apr
import moneym.core.ui.generated.resources.month_aug
import moneym.core.ui.generated.resources.month_dec
import moneym.core.ui.generated.resources.month_feb
import moneym.core.ui.generated.resources.month_jan
import moneym.core.ui.generated.resources.month_jul
import moneym.core.ui.generated.resources.month_jun
import moneym.core.ui.generated.resources.month_mar
import moneym.core.ui.generated.resources.month_may
import moneym.core.ui.generated.resources.month_nov
import moneym.core.ui.generated.resources.month_oct
import moneym.core.ui.generated.resources.month_sep
import org.jetbrains.compose.resources.stringResource

@Composable
fun localizedMonthNames(): List<String> = listOf(
    stringResource(Res.string.month_jan),
    stringResource(Res.string.month_feb),
    stringResource(Res.string.month_mar),
    stringResource(Res.string.month_apr),
    stringResource(Res.string.month_may),
    stringResource(Res.string.month_jun),
    stringResource(Res.string.month_jul),
    stringResource(Res.string.month_aug),
    stringResource(Res.string.month_sep),
    stringResource(Res.string.month_oct),
    stringResource(Res.string.month_nov),
    stringResource(Res.string.month_dec),
)

@Composable
fun localizedMonthAbbreviations(): List<String> = localizedMonthNames().map { it.take(3) }

@Suppress("DEPRECATION")
@Composable
fun monthLabel(ym: YearMonth): String = "${localizedMonthNames()[ym.monthNumber - 1]} ${ym.year}"

@Composable
fun monthLabel(year: Int, month: Int): String = "${localizedMonthNames()[month - 1]} $year"

package com.dv.moneym.feature.overview.components

import androidx.compose.runtime.Composable
import moneym.feature.overview.generated.resources.Res
import moneym.feature.overview.generated.resources.overview_month_apr
import moneym.feature.overview.generated.resources.overview_month_aug
import moneym.feature.overview.generated.resources.overview_month_dec
import moneym.feature.overview.generated.resources.overview_month_feb
import moneym.feature.overview.generated.resources.overview_month_jan
import moneym.feature.overview.generated.resources.overview_month_jul
import moneym.feature.overview.generated.resources.overview_month_jun
import moneym.feature.overview.generated.resources.overview_month_mar
import moneym.feature.overview.generated.resources.overview_month_may
import moneym.feature.overview.generated.resources.overview_month_nov
import moneym.feature.overview.generated.resources.overview_month_oct
import moneym.feature.overview.generated.resources.overview_month_sep
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun localizedMonthNames(): List<String> = listOf(
    stringResource(Res.string.overview_month_jan),
    stringResource(Res.string.overview_month_feb),
    stringResource(Res.string.overview_month_mar),
    stringResource(Res.string.overview_month_apr),
    stringResource(Res.string.overview_month_may),
    stringResource(Res.string.overview_month_jun),
    stringResource(Res.string.overview_month_jul),
    stringResource(Res.string.overview_month_aug),
    stringResource(Res.string.overview_month_sep),
    stringResource(Res.string.overview_month_oct),
    stringResource(Res.string.overview_month_nov),
    stringResource(Res.string.overview_month_dec),
)

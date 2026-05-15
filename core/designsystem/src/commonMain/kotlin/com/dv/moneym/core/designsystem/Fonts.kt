package com.dv.moneym.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import moneym.core.designsystem.generated.resources.Res
import moneym.core.designsystem.generated.resources.geist_bold
import moneym.core.designsystem.generated.resources.geist_medium
import moneym.core.designsystem.generated.resources.geist_mono_medium
import moneym.core.designsystem.generated.resources.geist_mono_regular
import moneym.core.designsystem.generated.resources.geist_mono_semibold
import moneym.core.designsystem.generated.resources.geist_regular
import moneym.core.designsystem.generated.resources.geist_semibold
import org.jetbrains.compose.resources.Font

@Composable
fun rememberGeist(): FontFamily = FontFamily(
    Font(Res.font.geist_regular,  weight = FontWeight.Normal),
    Font(Res.font.geist_medium,   weight = FontWeight.Medium),
    Font(Res.font.geist_semibold, weight = FontWeight.SemiBold),
    Font(Res.font.geist_bold,     weight = FontWeight.Bold),
)

@Composable
fun rememberGeistMono(): FontFamily = FontFamily(
    Font(Res.font.geist_mono_regular,  weight = FontWeight.Normal),
    Font(Res.font.geist_mono_medium,   weight = FontWeight.Medium),
    Font(Res.font.geist_mono_semibold, weight = FontWeight.SemiBold),
)

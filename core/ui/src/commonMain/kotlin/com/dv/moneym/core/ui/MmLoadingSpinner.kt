package com.dv.moneym.core.ui

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dv.moneym.core.designsystem.MM

@Composable
fun MmLoadingSpinner(modifier: Modifier = Modifier) {
    CircularProgressIndicator(color = MM.colors.accent, modifier = modifier)
}

package com.dv.moneym.core.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.MoneyMTheme

@Composable
fun MmLoadingSpinner(modifier: Modifier = Modifier) {
    CircularProgressIndicator(color = MM.colors.accent, modifier = modifier)
}

@Preview
@Composable
private fun MmLoadingSpinnerPreview() {
    MoneyMTheme {
        MmLoadingSpinner(Modifier.padding(MM.dimen.padding_2x))
    }
}

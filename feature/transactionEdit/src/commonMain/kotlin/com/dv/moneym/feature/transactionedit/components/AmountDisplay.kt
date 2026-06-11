package com.dv.moneym.feature.transactionedit.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.tooling.preview.Preview
import com.dv.moneym.core.designsystem.MoneyMTheme
import com.dv.moneym.core.ui.MmAmountInput

@Composable
internal fun AmountDisplay(
    amountText: String,
    currencyCode: String,
    focusRequester: FocusRequester,
    onAmountChanged: (String) -> Unit,
    onCalculatorClick: () -> Unit,
    notesFocusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier,
) {
    MmAmountInput(
        amountText = amountText,
        currencyCode = currencyCode,
        focusRequester = focusRequester,
        onAmountChanged = onAmountChanged,
        onCalculatorClick = onCalculatorClick,
        notesFocusRequester = notesFocusRequester,
        modifier = modifier,
    )
}

@Preview
@Composable
private fun AmountDisplayPreview() {
    MoneyMTheme {
        AmountDisplay(
            amountText = "42.50",
            currencyCode = "EUR",
            focusRequester = remember { FocusRequester() },
            onAmountChanged = {},
            onCalculatorClick = {},
        )
    }
}

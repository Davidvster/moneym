package com.dv.moneym.feature.transactionedit.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
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

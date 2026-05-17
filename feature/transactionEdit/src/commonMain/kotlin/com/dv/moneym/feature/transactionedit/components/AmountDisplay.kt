package com.dv.moneym.feature.transactionedit.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.ui.MmIconButton
import com.dv.moneym.core.ui.MmIcons
import com.dv.moneym.core.ui.calculator
import moneym.feature.transactionedit.generated.resources.Res
import moneym.feature.transactionedit.generated.resources.edit_amount_label
import moneym.feature.transactionedit.generated.resources.edit_calculator_title
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun AmountDisplay(
    amountValue: Double,
    formattedAmount: String,
    currencyCode: String,
    amountText: String,
    focusRequester: FocusRequester,
    onAmountChanged: (String) -> Unit,
    onCalculatorClick: () -> Unit,
) {
    val colors = MM.colors
    val type = MM.type

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MM.dimen.padding_1_5x),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(Res.string.edit_amount_label).uppercase(),
            style = type.micro,
            color = colors.text3,
        )
        Spacer(Modifier.height(MM.dimen.padding_1x))

        val interactionSource = remember { MutableInteractionSource() }
        Box(
            modifier = Modifier.clickable(
                indication = null,
                interactionSource = interactionSource,
            ) {
                focusRequester.requestFocus()
            },
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = currencyCode,
                    style = type.bodyMono,
                    color = colors.text3,
                )
                Text(
                    text = formattedAmount,
                    style = type.display.copy(
                        fontSize = 52.sp,
                        color = if (amountValue == 0.0) colors.text3 else colors.text,
                    ),
                )
            }
        }

        // Hidden BasicTextField for numeric input (cursor brush set to accent)
        BasicTextField(
            value = amountText,
            onValueChange = onAmountChanged,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            cursorBrush = SolidColor(colors.accent),
            modifier = Modifier
                .size(1.dp)
                .focusRequester(focusRequester),
            decorationBox = { },
        )

        Spacer(Modifier.height(MM.dimen.padding_1x))

        // Calculator button
        MmIconButton(
            icon = MmIcons.calculator,
            onClick = onCalculatorClick,
            contentDescription = stringResource(Res.string.edit_calculator_title),
        )
    }
}

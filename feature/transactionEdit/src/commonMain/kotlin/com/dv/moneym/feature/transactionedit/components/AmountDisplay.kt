package com.dv.moneym.feature.transactionedit.components

import com.dv.moneym.core.ui.imageVector
import com.dv.moneym.core.model.Icon
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.ui.MmIconButton
import moneym.feature.transactionedit.generated.resources.Res
import moneym.feature.transactionedit.generated.resources.edit_amount_label
import moneym.feature.transactionedit.generated.resources.edit_calculator_title
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun AmountDisplay(
    amountText: String,
    currencyCode: String,
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

        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = currencyCode,
                style = type.bodyMono,
                color = colors.text3,
            )
            Box(contentAlignment = Alignment.CenterStart) {
                // Placeholder shown when field is empty
                if (amountText.isEmpty()) {
                    Text(
                        text = "0.00",
                        style = type.display.copy(
                            fontSize = 52.sp,
                            color = colors.text3,
                        ),
                    )
                }
                BasicTextField(
                    value = TextFieldValue(
                        text = amountText,
                        selection = TextRange(amountText.length),
                    ),
                    onValueChange = { onAmountChanged(it.text) },
                    textStyle = type.display.copy(
                        fontSize = 52.sp,
                        color = colors.text,
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    cursorBrush = SolidColor(colors.accent),
                    singleLine = true,
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .defaultMinSize(minWidth = 1.dp),
                )
            }
        }

        Spacer(Modifier.height(MM.dimen.padding_1x))

        MmIconButton(
            icon = Icon.Calculator.imageVector,
            onClick = onCalculatorClick,
            contentDescription = stringResource(Res.string.edit_calculator_title),
        )
    }
}

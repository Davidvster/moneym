package com.dv.moneym.core.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.dv.moneym.core.common.formatNumber
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.MoneyMTheme
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun MmMoney(
    value: Double,
    modifier: Modifier = Modifier,
    sign: String = "",
    size: TextUnit = 15.sp,
    weight: FontWeight = FontWeight.Medium,
    color: Color = Color.Unspecified,
    currency: String = "",
    style: TextStyle? = null,
) {
    val colors = MM.colors
    val type = MM.type

    val absValue = kotlin.math.abs(value)
    val isNegative = value < 0

    // Format with thousand separator and 2 decimal places
    val formatted = formatMoneyValue(absValue)

    // U+2212 is the mathematical minus sign (−), not ASCII hyphen (-)
    val prefix = when {
        isNegative && sign.isEmpty() -> "−"
        sign.isNotEmpty() -> sign
        else -> ""
    }

    val displayText = if (prefix.isNotEmpty()) {
        "$prefix $currency $formatted"
    } else {
        "$currency $formatted"
    }

    val resolvedColor = if (color == Color.Unspecified) colors.text else color

    val finalStyle = if (style != null) {
        style.copy(color = resolvedColor)
    } else {
        type.bodyMono.copy(
            fontSize = size,
            fontWeight = weight,
            color = resolvedColor,
        )
    }

    Text(
        text = displayText,
        style = finalStyle,
        softWrap = false,
        modifier = modifier,
    )
}

private fun formatMoneyValue(value: Double): String = formatNumber(value, 2)

@Preview
@Composable
private fun MmMoneyPreview() {
    MoneyMTheme {
        Column(
            Modifier.padding(MM.dimen.padding_2x),
            verticalArrangement = Arrangement.spacedBy(MM.dimen.padding_1x),
        ) {
            MmMoney(value = 1234.56, currency = "€")
            MmMoney(value = -78.90, currency = "€")
            MmMoney(value = 999.0, currency = "$", style = MM.type.amountLarge, color = MM.colors.accent)
            MmMoney(value = 12.34, currency = "€", style = MM.type.amountMedium)
        }
    }
}

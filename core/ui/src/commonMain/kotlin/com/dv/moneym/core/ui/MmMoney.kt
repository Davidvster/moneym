package com.dv.moneym.core.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.dv.moneym.core.common.formatNumber
import com.dv.moneym.core.designsystem.MM

@Composable
fun MmMoney(
    value: Double,
    modifier: Modifier = Modifier,
    sign: String = "",
    size: TextUnit = 15.sp,
    weight: FontWeight = FontWeight.Medium,
    color: Color = Color.Unspecified,
    currency: String = "",
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

    Text(
        text = displayText,
        style = type.bodyMono.copy(
            fontSize = size,
            fontWeight = weight,
            color = resolvedColor,
        ),
        softWrap = false,
        modifier = modifier,
    )
}

private fun formatMoneyValue(value: Double): String = formatNumber(value, 2)

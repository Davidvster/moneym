package com.dv.moneym.core.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.dv.moneym.core.common.formatNumber
import com.dv.moneym.core.model.currencyDisplay
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.MoneyMTheme
import androidx.compose.ui.tooling.preview.Preview

val LocalUseCurrencySymbol = staticCompositionLocalOf { false }

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
    animate: Boolean = false,
) {
    val colors = MM.colors
    val type = MM.type

    val displayCurrency = currencyDisplay(currency, LocalUseCurrencySymbol.current)

    val displayValue = if (animate && !LocalInspectionMode.current) {
        val animatable = remember {
            Animatable(0.0, DoubleVectorConverter)
        }
        LaunchedEffect(value) {
            animatable.animateTo(value, tween(durationMillis = 600, easing = CountUpEase))
        }
        animatable.value
    } else {
        value
    }

    val absValue = kotlin.math.abs(displayValue)
    val isNegative = displayValue < 0

    // Format with thousand separator and 2 decimal places
    val formatted = formatMoneyValue(absValue)

    // U+2212 is the mathematical minus sign (−), not ASCII hyphen (-)
    val prefix = when {
        isNegative && sign.isEmpty() -> "−"
        sign.isNotEmpty() -> sign
        else -> ""
    }

    val displayText = if (prefix.isNotEmpty()) {
        "$prefix $displayCurrency $formatted"
    } else {
        "$displayCurrency $formatted"
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

private val CountUpEase = CubicBezierEasing(0.33f, 0f, 0f, 1f)

private val DoubleVectorConverter = TwoWayConverter<Double, AnimationVector1D>(
    convertToVector = { AnimationVector1D(it.toFloat()) },
    convertFromVector = { it.value.toDouble() },
)

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

package com.dv.moneym.feature.security.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.MoneyMColors
import com.dv.moneym.core.designsystem.MoneyMType

/**
 * Shared composable for the MoneyM app lockup (icon box + brand text).
 * Used by both PinSetupScreen and PinUnlockScreen.
 */
@Composable
internal fun AppLockup(
    colors: MoneyMColors,
    type: MoneyMType,
) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(MM.space.padding_2x))
            .background(colors.text),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "M",
            style = type.title2.copy(
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = colors.bg,
            ),
        )
    }
}

/**
 * Shared composable for the 4-dot PIN indicator with shake animation.
 * Used by both PinSetupScreen and PinUnlockScreen.
 */
@Composable
internal fun PinDots(
    filledCount: Int,
    shakeOffsetPx: Int,
    colors: MoneyMColors,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier.offset { IntOffset(shakeOffsetPx, 0) },
    ) {
        repeat(4) { i ->
            val filled = i < filledCount
            Box(
                modifier = Modifier
                    .size(MM.space.padding_1_5x)
                    .clip(CircleShape)
                    .background(
                        if (filled) colors.text else Color.Transparent,
                        CircleShape,
                    )
                    .border(
                        width = 1.5.dp,
                        color = if (filled) colors.text else colors.borderStrong,
                        shape = CircleShape,
                    ),
            )
        }
    }
}

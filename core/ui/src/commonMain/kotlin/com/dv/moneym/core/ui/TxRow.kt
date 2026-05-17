package com.dv.moneym.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.model.Density
import com.dv.moneym.core.model.IndicatorStyle
import com.dv.moneym.core.model.TxDisplayPrefs

/**
 * A single transaction row.
 *
 * Callers supply pre-resolved display data (category color, icon, name, note, amount).
 * This composable only handles rendering — no data joining.
 *
 * The feature screen is responsible for resolving category data and calling this.
 */
@Composable
fun TxRow(
    categoryName: String,
    categoryColor: Color,
    categoryIcon: ImageVector,
    note: String?,
    isExpense: Boolean,
    amountValue: Double,
    currency: String,
    prefs: TxDisplayPrefs,
    onClick: (() -> Unit)? = null,
    divider: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val colors = MM.colors
    val type = MM.type

    val verticalPadding = when (prefs.density) {
        Density.Comfortable -> 14.dp
        Density.Normal -> 12.dp
        Density.Compact -> 10.dp
    }

    val hasNote = !note.isNullOrBlank()
    val showNoteAsPrimary = hasNote && prefs.showNote
    val primaryText = if (showNoteAsPrimary) note.orEmpty() else categoryName
    val secondaryText = if (showNoteAsPrimary && prefs.showCategoryName) categoryName else null

    val dividerColor = colors.divider

    var pressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.bg)
            .alpha(if (pressed) 0.75f else 1f)
            .then(
                if (onClick != null) {
                    Modifier.pointerInput(onClick) {
                        detectTapGestures(
                            onPress = {
                                pressed = true
                                tryAwaitRelease()
                                pressed = false
                            },
                            onTap = { onClick() },
                        )
                    }
                } else Modifier,
            )
            .then(
                if (divider) {
                    Modifier.drawBehind {
                        val strokeWidth = 1.dp.toPx()
                        drawLine(
                            color = dividerColor,
                            start = Offset(0f, size.height - strokeWidth / 2),
                            end = Offset(size.width, size.height - strokeWidth / 2),
                            strokeWidth = strokeWidth,
                        )
                    }
                } else Modifier,
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = verticalPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Leading: category indicator
            CategoryIconTile(
                categoryName = categoryName,
                categoryColor = categoryColor,
                categoryIcon = categoryIcon,
                size = 38.dp,
                variant = prefs.indicatorStyle,
            )

            // Middle: primary and secondary text
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = primaryText,
                    style = type.body,
                    color = colors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (secondaryText != null) {
                    Text(
                        text = secondaryText,
                        style = type.caption,
                        color = colors.text2,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // Trailing: amount
            val amountColor = if (isExpense) colors.text else colors.accent
            val amountWeight = if (isExpense) FontWeight.Medium else FontWeight.SemiBold
            val sign = if (isExpense) "−" else "+"

            MmMoney(
                value = amountValue,
                sign = sign,
                size = 15.sp,
                weight = amountWeight,
                color = amountColor,
                currency = currency,
            )
        }
    }
}

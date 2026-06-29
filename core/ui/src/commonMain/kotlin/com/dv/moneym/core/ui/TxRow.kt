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
import androidx.compose.ui.tooling.preview.Preview
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.MoneyMTheme
import com.dv.moneym.core.model.Density
import com.dv.moneym.core.model.Icon
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
    paymentModeName: String? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    divider: Boolean = true,
    isPending: Boolean = false,
    selected: Boolean = false,
    selectionMode: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val colors = MM.colors
    val type = MM.type

    val verticalPadding = when (prefs.density) {
        Density.Comfortable -> MM.dimen.padding_2x
        Density.Normal -> MM.dimen.padding_1_5x
        Density.Compact -> 10.dp
    }

    val hasNote = !note.isNullOrBlank()
    val showNoteAsPrimary = hasNote && prefs.showNote
    val primaryText = if (showNoteAsPrimary) note else categoryName
    val secondaryText = if (showNoteAsPrimary && prefs.showCategoryName) categoryName else null

    val dividerColor = colors.divider

    val baseAlpha = if (isPending) 0.5f else 1f
    val rowBackground = when {
        selected -> colors.accent.copy(alpha = 0.12f)
        selectionMode -> colors.surface2.copy(alpha = 0.6f)
        else -> colors.bg
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(rowBackground)
            .alpha(baseAlpha)
            .then(
                if (onClick != null || onLongClick != null) {
                    Modifier.pointerInput(onClick, onLongClick) {
                        detectTapGestures(
                            onTap = { onClick?.invoke() },
                            onLongPress = { onLongClick?.invoke() },
                        )
                    }
                } else {
                    Modifier
                },
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
                .padding(horizontal = MM.dimen.padding_2_5x, vertical = verticalPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MM.dimen.padding_1_5x),
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

            // Trailing: amount + optional payment mode
            val amountColor = if (isExpense) colors.text else colors.accent
            val amountWeight = if (isExpense) FontWeight.Medium else FontWeight.SemiBold
            val sign = if (isExpense) "−" else "+"

            Column(horizontalAlignment = Alignment.End) {
                MmMoney(
                    value = amountValue,
                    sign = sign,
                    size = 15.sp,
                    weight = amountWeight,
                    color = amountColor,
                    currency = currency,
                )
                if (paymentModeName != null) {
                    Text(
                        text = paymentModeName,
                        style = type.caption,
                        color = colors.text3,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun TxRowPreview() {
    MoneyMTheme {
        Column {
            TxRow(
                categoryName = "Groceries",
                categoryColor = MM.colors.accent,
                categoryIcon = Icon.List.imageVector,
                note = "Whole Foods",
                isExpense = true,
                amountValue = 45.67,
                currency = "€",
                prefs = TxDisplayPrefs(),
            )
            TxRow(
                categoryName = "Salary",
                categoryColor = MM.colors.text2,
                categoryIcon = Icon.ArrowDown.imageVector,
                note = null,
                isExpense = false,
                amountValue = 2500.0,
                currency = "€",
                prefs = TxDisplayPrefs(),
                divider = false,
            )
        }
    }
}

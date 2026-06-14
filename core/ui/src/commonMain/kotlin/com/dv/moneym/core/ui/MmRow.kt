package com.dv.moneym.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.MoneyMTheme
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun MmRow(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    divider: Boolean = true,
    padding: PaddingValues = PaddingValues(
        horizontal = MM.dimen.padding_2_5x,
        vertical = MM.dimen.padding_2x
    ),
    content: @Composable RowScope.() -> Unit,
) {
    val colors = MM.colors
    val dividerColor = colors.divider

    val clickModifier = if (onClick != null) {
        Modifier.mmClickable(onClick = onClick)
    } else Modifier

    Box(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = MM.dimen.padding_7x)
            .then(clickModifier)
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
                } else Modifier
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = MM.dimen.padding_7x)
                .padding(padding),
            horizontalArrangement = Arrangement.spacedBy(MM.dimen.padding_1_5x),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

@Preview
@Composable
private fun MmRowPreview() {
    MoneyMTheme {
        androidx.compose.foundation.layout.Column {
            MmRow(onClick = {}) {
                Text("Clickable row with divider", color = MM.colors.text)
            }
            MmRow(divider = false) {
                Text("Plain row, no divider", color = MM.colors.text)
            }
        }
    }
}

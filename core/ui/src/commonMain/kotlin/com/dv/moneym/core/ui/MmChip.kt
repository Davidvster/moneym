package com.dv.moneym.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.MoneyMTheme
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun MmChip(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingContent: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val colors = MM.colors
    val radius = MM.dimen

    val bgColor = if (selected) colors.text else androidx.compose.ui.graphics.Color.Transparent
    val borderColor = if (selected) colors.text else colors.border

    Row(
        modifier = modifier
            .height(34.dp)
            .clip(radius.pill)
            .background(bgColor, radius.pill)
            .border(1.dp, borderColor, radius.pill)
            .mmClickable(
                rippleColor = if (selected) colors.bg else androidx.compose.ui.graphics.Color.Unspecified,
                onClick = onClick,
            )
            .padding(horizontal = MM.dimen.padding_1_5x),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingContent != null) {
            leadingContent()
        }
        content()
    }
}

@Preview
@Composable
private fun MmChipPreview() {
    MoneyMTheme {
        Row(
            modifier = Modifier.padding(MM.dimen.padding_2x),
            horizontalArrangement = Arrangement.spacedBy(MM.dimen.padding_1x),
        ) {
            MmChip(selected = false, onClick = {}) { Text("Unselected", color = MM.colors.text) }
            MmChip(selected = true, onClick = {}) { Text("Selected", color = MM.colors.bg) }
        }
    }
}

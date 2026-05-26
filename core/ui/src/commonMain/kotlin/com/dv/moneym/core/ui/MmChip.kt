package com.dv.moneym.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
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

    var pressed by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .height(34.dp)
            .clip(radius.pill)
            .background(bgColor, radius.pill)
            .border(1.dp, borderColor, radius.pill)
            .alpha(if (pressed) 0.7f else 1f)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                    },
                    onTap = { onClick() },
                )
            }
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

package com.dv.moneym.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.MoneyMTheme
import com.dv.moneym.core.model.Icon as ModelIcon

/**
 * App radio indicator — accent-filled circle with a white check when selected.
 * Used in place of M3 RadioButton across selection lists.
 * Pass [onClick] = null when the row already handles the click.
 */
@Composable
fun MmRadio(
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    size: Dp = MM.dimen.iconLg,
) {
    val colors = MM.colors
    val clickModifier = if (onClick != null) {
        Modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
    } else {
        Modifier
    }
    Box(
        modifier = modifier
            .then(clickModifier)
            .size(size)
            .clip(CircleShape)
            .border(
                1.5.dp,
                if (selected) colors.accent else colors.borderStrong,
                CircleShape,
            )
            .background(if (selected) colors.accent else Color.Transparent),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                imageVector = ModelIcon.Check.imageVector,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(MM.dimen.padding_1_5x),
            )
        }
    }
}

@Preview
@Composable
private fun MmRadioPreview() {
    MoneyMTheme {
        MmRadio(selected = true)
    }
}

@Preview
@Composable
private fun MmRadioUnselectedPreview() {
    MoneyMTheme {
        MmRadio(selected = false)
    }
}

package com.dv.moneym.feature.transactionedit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.dv.moneym.core.designsystem.MM

@Composable
internal fun DateDisplayField(
    dateText: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MM.colors
    val type = MM.type
    val radius = MM.radius

    Column(modifier = modifier) {
        Text(
            text = label.uppercase(),
            style = type.micro,
            color = colors.text2,
        )
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(radius.radius_1_5x)
                .background(colors.surface, radius.radius_1_5x)
                .border(1.dp, colors.border, radius.radius_1_5x)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) { onClick() }
                .padding(horizontal = MM.space.padding_2x, vertical = MM.space.padding_2x),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = dateText,
                style = type.body,
                color = colors.text,
            )
        }
    }
}

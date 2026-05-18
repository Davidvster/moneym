package com.dv.moneym.feature.transactionedit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.model.Icon as MmIcon
import com.dv.moneym.core.ui.imageVector

@Composable
internal fun DateDisplayField(
    dateText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    val colors = MM.colors
    val type = MM.type
    val radius = MM.dimen

    Column(modifier = modifier) {
        if (label != null) {
            Text(
                text = label.uppercase(),
                style = type.micro,
                color = colors.text2,
            )
            Spacer(Modifier.height(4.dp))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .clip(radius.radius_2_5x)
                .background(colors.surface2, radius.radius_2_5x)
                .border(1.dp, colors.border, radius.radius_2_5x)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) { onClick() }
                .padding(horizontal = radius.padding_1_5x),
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(radius.padding_1x),
            ) {
                Icon(
                    imageVector = MmIcon.Calendar.imageVector,
                    contentDescription = null,
                    tint = colors.text3,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = dateText,
                    style = type.caption,
                    color = colors.text,
                )
            }
        }
    }
}

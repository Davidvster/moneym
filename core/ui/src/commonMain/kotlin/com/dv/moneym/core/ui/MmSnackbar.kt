package com.dv.moneym.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dv.moneym.core.designsystem.MM

@Composable
fun MmSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val colors = MM.colors
    val type = MM.type
    val space = MM.dimen
    SnackbarHost(hostState = hostState, modifier = modifier) { data ->
        Surface(
            color = colors.surface2,
            contentColor = colors.text,
            shape = RoundedCornerShape(space.radius_2x.bottomEnd),
            border = BorderStroke(space.strokeHairline, colors.border),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            modifier = Modifier.padding(space.padding_1x),
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = space.padding_2x,
                    vertical = space.padding_1_5x,
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = data.visuals.message,
                    style = type.body,
                    color = colors.text,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                data.visuals.actionLabel?.let { label ->
                    Spacer(Modifier.width(space.padding_2x))
                    TextButton(onClick = { data.performAction() }) {
                        Text(text = label, style = type.body, color = colors.accent)
                    }
                }
            }
        }
    }
}

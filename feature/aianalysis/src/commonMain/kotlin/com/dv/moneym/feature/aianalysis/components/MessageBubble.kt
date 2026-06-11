package com.dv.moneym.feature.aianalysis.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dv.moneym.core.ai.ChatRole
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.MoneyMTheme

@Composable
fun MessageBubble(
    role: ChatRole,
    content: String,
    modifier: Modifier = Modifier,
) {
    val colors = MM.colors
    val isUser = role == ChatRole.USER
    val shape = MM.dimen.radius_2x

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(shape)
                .then(
                    if (isUser) {
                        Modifier.background(colors.accent, shape)
                    } else {
                        Modifier
                            .background(colors.surface, shape)
                            .border(1.dp, colors.border, shape)
                    }
                )
                .padding(horizontal = MM.dimen.padding_2x, vertical = MM.dimen.padding_1_5x),
        ) {
            Text(
                text = content,
                style = MM.type.body,
                color = if (isUser) colors.bg else colors.text,
            )
        }
    }
}

@Preview
@Composable
private fun MessageBubblePreview() {
    MoneyMTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(MM.dimen.padding_1x),
        ) {
            MessageBubble(
                role = ChatRole.USER,
                content = "How much did I spend on groceries this month?",
            )
            MessageBubble(
                role = ChatRole.ASSISTANT,
                content = "You spent EUR 620.00 on groceries this month, across 12 transactions.",
            )
        }
    }
}

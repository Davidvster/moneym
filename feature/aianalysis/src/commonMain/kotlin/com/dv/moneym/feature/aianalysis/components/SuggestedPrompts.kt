package com.dv.moneym.feature.aianalysis.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.ui.MmChip
import moneym.feature.aianalysis.generated.resources.Res
import moneym.feature.aianalysis.generated.resources.analyze_intro
import moneym.feature.aianalysis.generated.resources.analyze_suggested_prompts
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun SuggestedPrompts(
    onPromptClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val prompts = stringArrayResource(Res.array.analyze_suggested_prompts)

    Column(
        modifier = modifier.fillMaxWidth().padding(MM.dimen.padding_2_5x),
        verticalArrangement = Arrangement.spacedBy(MM.dimen.padding_1_5x),
    ) {
        Text(
            text = stringResource(Res.string.analyze_intro),
            style = MM.type.body,
            color = MM.colors.text2,
        )
        prompts.forEach { prompt ->
            MmChip(
                selected = false,
                onClick = { onPromptClick(prompt) },
            ) {
                Text(
                    text = prompt,
                    style = MM.type.caption,
                    color = MM.colors.text,
                )
            }
        }
    }
}

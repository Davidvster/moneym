package com.dv.moneym.feature.aianalysis.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.MoneyMTheme
import com.dv.moneym.core.model.Icon
import com.dv.moneym.core.ui.MmField
import com.dv.moneym.core.ui.MmIconButton
import com.dv.moneym.core.ui.MmIconButtonVariant
import com.dv.moneym.core.ui.imageVector
import moneym.feature.aianalysis.generated.resources.Res
import moneym.feature.aianalysis.generated.resources.analyze_input_hint
import moneym.feature.aianalysis.generated.resources.analyze_send_cd
import org.jetbrains.compose.resources.stringResource

@Composable
fun AnalyzeInputBar(
    input: String,
    enabled: Boolean,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
            .padding(horizontal = MM.dimen.padding_2_5x, vertical = MM.dimen.padding_2x),
        horizontalArrangement = Arrangement.spacedBy(MM.dimen.padding_1x),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MmField(
            value = input,
            onValueChange = onInputChange,
            placeholder = stringResource(Res.string.analyze_input_hint),
            imeAction = ImeAction.Send,
            onImeAction = onSend,
            modifier = Modifier.weight(1f),
        )
        MmIconButton(
            icon = Icon.ArrowUp.imageVector,
            onClick = { if (enabled) onSend() },
            variant = MmIconButtonVariant.Accent,
            contentDescription = stringResource(Res.string.analyze_send_cd),
        )
    }
}

@Preview
@Composable
private fun AnalyzeInputBarPreview() {
    MoneyMTheme {
        AnalyzeInputBar(
            input = "How much did I spend on groceries?",
            enabled = true,
            onInputChange = {},
            onSend = {},
        )
    }
}

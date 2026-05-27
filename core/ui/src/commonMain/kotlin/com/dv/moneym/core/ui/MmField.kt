package com.dv.moneym.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.MoneyMTheme
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun MmField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    prefix: (@Composable () -> Unit)? = null,
    suffix: (@Composable () -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    focusRequester: FocusRequester? = null,
    imeAction: ImeAction = ImeAction.Default,
    onImeAction: (() -> Unit)? = null,
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
            Spacer(modifier = Modifier.height(4.dp))
        }

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            textStyle = type.body.copy(color = colors.text),
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = imeAction,
            ),
            keyboardActions = KeyboardActions(
                onNext = { onImeAction?.invoke() },
                onDone = { onImeAction?.invoke() },
            ),
            cursorBrush = SolidColor(colors.accent),
            modifier = Modifier
                .fillMaxWidth()
                .clip(radius.radius_1_5x)
                .background(colors.surface, radius.radius_1_5x)
                .border(1.dp, colors.border, radius.radius_1_5x)
                .defaultMinSize(minHeight = if (singleLine) 52.dp else 80.dp)
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier.padding(
                        horizontal = MM.dimen.padding_2x,
                        vertical = MM.dimen.padding_2x
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (prefix != null) {
                        Box(modifier = Modifier.padding(end = 6.dp)) {
                            prefix()
                        }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        if (value.isEmpty() && placeholder != null) {
                            Text(
                                text = placeholder,
                                style = type.body,
                                color = colors.text3,
                            )
                        }
                        innerTextField()
                    }
                    if (suffix != null) {
                        Box(modifier = Modifier.padding(start = 6.dp)) {
                            suffix()
                        }
                    }
                }
            },
        )
    }
}

@Preview
@Composable
private fun MmFieldPreview() {
    MoneyMTheme {
        Column(
            Modifier.padding(MM.dimen.padding_2x),
            verticalArrangement = Arrangement.spacedBy(MM.dimen.padding_2x),
        ) {
            MmField(value = "", onValueChange = {}, label = "Name", placeholder = "Enter name")
            MmField(value = "filled value", onValueChange = {}, label = "Notes")
        }
    }
}

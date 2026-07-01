package com.dv.moneym.core.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.MoneyMTheme
import androidx.compose.ui.tooling.preview.Preview
import com.dv.moneym.core.model.Icon as ModelIcon

@Composable
fun MmSettingsRow(
    title: String,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    subtitle: String? = null,
    divider: Boolean = true,
    trailing: @Composable (() -> Unit)? = { DefaultChevronTrailing() },
) {
    val colors = MM.colors
    val type = MM.type

    MmRow(
        modifier = modifier,
        onClick = onClick,
        divider = divider,
    ) {
        Icon(
            imageVector = leadingIcon,
            contentDescription = null,
            tint = colors.text,
            modifier = Modifier.size(MM.dimen.iconMd),
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = type.body,
                color = colors.text,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = type.caption.copy(color = colors.text2),
                )
            }
        }
        if (trailing != null) {
            trailing()
        }
    }
}

@Preview
@Composable
private fun MmSettingsRowPreview() {
    MoneyMTheme {
        Column {
            MmSettingsRow(
                title = "Language",
                subtitle = "English",
                leadingIcon = ModelIcon.Globe.imageVector,
                onClick = {},
            )
            MmSettingsRow(
                title = "Categories",
                leadingIcon = ModelIcon.List.imageVector,
                onClick = {},
            )
            MmSettingsRow(
                title = "No chevron",
                leadingIcon = ModelIcon.Calendar.imageVector,
                onClick = {},
                trailing = null,
                divider = false,
            )
        }
    }
}

@Composable
private fun DefaultChevronTrailing() {
    Icon(
        imageVector = ModelIcon.ChevronRight.imageVector,
        contentDescription = null,
        tint = MM.colors.text3,
        modifier = Modifier.size(MM.dimen.padding_2x),
    )
}

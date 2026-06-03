package com.dv.moneym.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon as Material3Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.MoneyMTheme
import com.dv.moneym.core.model.Icon

val Icon.imageVector: ImageVector get() = MmIcons.forIcon(this)

@Preview
@Composable
private fun IconPreview() {
    MoneyMTheme {
        Row(
            modifier = Modifier.padding(MM.dimen.padding_2x),
            horizontalArrangement = Arrangement.spacedBy(MM.dimen.padding_1x),
        ) {
            Material3Icon(
                imageVector = Icon.Wallet.imageVector,
                contentDescription = null,
                tint = MM.colors.text,
                modifier = Modifier.size(MM.dimen.icon_1x),
            )
            Material3Icon(
                imageVector = Icon.Settings.imageVector,
                contentDescription = null,
                tint = MM.colors.text,
                modifier = Modifier.size(MM.dimen.icon_1x),
            )
            Material3Icon(
                imageVector = Icon.Check.imageVector,
                contentDescription = null,
                tint = MM.colors.accent,
                modifier = Modifier.size(MM.dimen.icon_1x),
            )
        }
    }
}

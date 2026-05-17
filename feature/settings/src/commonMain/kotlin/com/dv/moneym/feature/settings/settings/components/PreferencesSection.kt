package com.dv.moneym.feature.settings.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.ui.MmCard
import com.dv.moneym.core.ui.MmIcons
import com.dv.moneym.core.ui.MmRow
import com.dv.moneym.core.ui.wallet
import moneym.feature.settings.generated.resources.Res
import moneym.feature.settings.generated.resources.settings_categories
import moneym.feature.settings.generated.resources.settings_currency
import moneym.feature.settings.generated.resources.settings_language
import moneym.feature.settings.generated.resources.settings_wallets
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun PreferencesSection(
    currencySubtitle: String,
    languageSubtitle: String,
    onNavigateToCurrency: () -> Unit,
    onNavigateToLanguage: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToWallets: () -> Unit,
) {
    val colors = MM.colors
    val type = MM.type
    val space = MM.dimen
    MmCard(Modifier.padding(horizontal = space.padding_2x)) {
        MmRow(onClick = onNavigateToCurrency) {
            Icon(
                imageVector = MmIcons.info,
                contentDescription = null,
                tint = colors.text,
                modifier = Modifier.size(MM.dimen.icon_1x),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(Res.string.settings_currency),
                    style = type.body,
                    color = colors.text
                )
                Text(currencySubtitle, style = type.caption.copy(color = colors.text2))
            }
            Icon(
                imageVector = MmIcons.chevronRight,
                contentDescription = null,
                tint = colors.text3,
                modifier = Modifier.size(MM.dimen.padding_2x),
            )
        }
        MmRow(onClick = onNavigateToLanguage) {
            Icon(
                imageVector = MmIcons.globe,
                contentDescription = null,
                tint = colors.text,
                modifier = Modifier.size(MM.dimen.icon_1x),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(Res.string.settings_language),
                    style = type.body,
                    color = colors.text
                )
                Text(languageSubtitle, style = type.caption.copy(color = colors.text2))
            }
            Icon(
                imageVector = MmIcons.chevronRight,
                contentDescription = null,
                tint = colors.text3,
                modifier = Modifier.size(MM.dimen.padding_2x),
            )
        }
        MmRow(onClick = onNavigateToCategories) {
            Icon(
                imageVector = MmIcons.list,
                contentDescription = null,
                tint = colors.text,
                modifier = Modifier.size(MM.dimen.icon_1x),
            )
            Text(
                stringResource(Res.string.settings_categories),
                style = type.body,
                color = colors.text,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = MmIcons.chevronRight,
                contentDescription = null,
                tint = colors.text3,
                modifier = Modifier.size(MM.dimen.padding_2x),
            )
        }
        MmRow(onClick = onNavigateToWallets, divider = false) {
            Icon(
                imageVector = MmIcons.wallet,
                contentDescription = null,
                tint = colors.text,
                modifier = Modifier.size(MM.dimen.icon_1x),
            )
            Text(
                stringResource(Res.string.settings_wallets),
                style = type.body,
                color = colors.text,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = MmIcons.chevronRight,
                contentDescription = null,
                tint = colors.text3,
                modifier = Modifier.size(MM.dimen.padding_2x),
            )
        }
    }
}

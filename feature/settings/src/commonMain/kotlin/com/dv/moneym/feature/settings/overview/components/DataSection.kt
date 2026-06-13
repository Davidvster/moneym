package com.dv.moneym.feature.settings.overview.components

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.model.Icon
import com.dv.moneym.core.ui.MmCard
import com.dv.moneym.core.ui.MmSettingsRow
import com.dv.moneym.core.ui.imageVector
import moneym.feature.settings.generated.resources.Res
import moneym.feature.settings.generated.resources.settings_bank_sync
import moneym.feature.settings.generated.resources.settings_import_export
import moneym.feature.settings.generated.resources.settings_wallet_sync
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun DataSection(
    onNavigateToExport: () -> Unit,
    onNavigateToBankSync: () -> Unit,
    onNavigateToWalletSync: (() -> Unit)?,
) {
    val space = MM.dimen
    MmCard(Modifier.padding(horizontal = space.padding_2x)) {
        MmSettingsRow(
            title = stringResource(Res.string.settings_import_export),
            leadingIcon = Icon.Folder.imageVector,
            onClick = onNavigateToExport,
        )
        MmSettingsRow(
            title = stringResource(Res.string.settings_bank_sync),
            leadingIcon = Icon.Bank.imageVector,
            onClick = onNavigateToBankSync,
            divider = onNavigateToWalletSync != null,
        )
        if (onNavigateToWalletSync != null) {
            MmSettingsRow(
                title = stringResource(Res.string.settings_wallet_sync),
                leadingIcon = Icon.Wallet.imageVector,
                onClick = onNavigateToWalletSync,
                divider = false,
            )
        }
    }
}

package com.dv.moneym.feature.transactions.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.model.Account
import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.ui.MmIcons
import com.dv.moneym.core.ui.MmRow
import moneym.feature.transactions.generated.resources.Res
import moneym.feature.transactions.generated.resources.transactions_cancel
import moneym.feature.transactions.generated.resources.transactions_wallet_select
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun WalletSwitcherDialog(
    accounts: List<Account>,
    selectedAccountId: AccountId?,
    onDismiss: () -> Unit,
    onSelect: (AccountId?) -> Unit,
) {
    val colors = MM.colors
    val type = MM.type
    val space = MM.dimen

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(Res.string.transactions_wallet_select),
                style = type.title3,
                color = colors.text,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(space.padding_0_25x)) {
                accounts.forEach { account ->
                    val isSelected = account.id == selectedAccountId
                    MmRow(
                        onClick = { onSelect(account.id) },
                        divider = false,
                        padding = PaddingValues(
                            horizontal = space.padding_0_5x,
                            vertical = space.padding_0_25x,
                        ),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(text = account.name, style = type.body, color = colors.text)
                            Text(
                                text = account.currency.value,
                                style = type.caption.copy(color = colors.text2),
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = MmIcons.check,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(MM.dimen.icon_1x),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.transactions_cancel), color = colors.text2)
            }
        },
        containerColor = colors.surface,
        titleContentColor = colors.text,
    )
}

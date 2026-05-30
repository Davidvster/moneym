package com.dv.moneym.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.dv.moneym.core.model.Account
import com.dv.moneym.core.model.AccountId

@Composable
fun WalletSelector(
    accounts: List<Account>,
    selectedAccountId: AccountId?,
    onSelect: (AccountId) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (accounts.isEmpty()) return

    val selected = accounts.firstOrNull { it.id == selectedAccountId }
        ?: accounts.firstOrNull { it.isDefault }
        ?: accounts.first()

    var showDialog by rememberSaveable { mutableStateOf(false) }

    WalletChip(
        name = selected.name,
        colorHex = selected.colorHex,
        onClick = { showDialog = true },
        modifier = modifier,
    )

    if (showDialog) {
        WalletSwitcherDialog(
            accounts = accounts,
            selectedAccountId = selected.id,
            onDismiss = { showDialog = false },
            onSelect = { id ->
                onSelect(id)
                showDialog = false
            },
        )
    }
}

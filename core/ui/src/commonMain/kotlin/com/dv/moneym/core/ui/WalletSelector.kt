package com.dv.moneym.core.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.MoneyMTheme
import com.dv.moneym.core.model.Account
import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.AccountType
import com.dv.moneym.core.model.CurrencyCode
import kotlin.time.Instant

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

internal fun previewAccount(
    id: Long,
    name: String,
    colorHex: String?,
    isDefault: Boolean = false,
): Account = Account(
    id = AccountId(id),
    name = name,
    type = AccountType.CASH,
    currency = CurrencyCode("EUR"),
    isDefault = isDefault,
    archived = false,
    createdAt = Instant.fromEpochSeconds(0),
    updatedAt = Instant.fromEpochSeconds(0),
    colorHex = colorHex,
)

@Preview
@Composable
private fun WalletSelectorPreview() {
    val accounts = listOf(
        previewAccount(1L, "Cash", "#3B82F6", isDefault = true),
        previewAccount(2L, "Bank", "#22C55E"),
    )
    MoneyMTheme {
        WalletSelector(
            accounts = accounts,
            selectedAccountId = AccountId(1L),
            onSelect = {},
            modifier = Modifier.padding(MM.dimen.padding_2x),
        )
    }
}

package com.dv.moneym.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.MoneyMTheme
import com.dv.moneym.core.model.Account
import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.Icon
import moneym.core.ui.generated.resources.Res
import moneym.core.ui.generated.resources.wallet_select
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MmWalletPickerSheet(
    accounts: List<Account>,
    selectedAccountId: AccountId?,
    onSelect: (AccountId) -> Unit,
    onDismiss: () -> Unit,
    title: String? = null,
) {
    val colors = MM.colors
    val type = MM.type
    val space = MM.dimen
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(
            topStart = space.padding_2_5x,
            topEnd = space.padding_2_5x,
        ),
        containerColor = colors.bg,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = space.padding_2_5x,
                vertical = space.padding_3x,
            ),
            verticalArrangement = Arrangement.spacedBy(space.padding_2x),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 36.dp, height = space.padding_0_5x)
                        .clip(RoundedCornerShape(50))
                        .background(colors.borderStrong),
                )
            }
            Text(
                text = title ?: stringResource(Res.string.wallet_select),
                style = type.title3,
                color = colors.text,
            )
            Column(verticalArrangement = Arrangement.spacedBy(space.padding_0_25x)) {
                accounts.forEach { account ->
                    val isSelected = account.id == selectedAccountId
                    MmRow(
                        onClick = {
                            onSelect(account.id)
                            onDismiss()
                        },
                        divider = false,
                        padding = PaddingValues(
                            horizontal = space.padding_0_5x,
                            vertical = space.padding_0_25x,
                        ),
                    ) {
                        WalletColorDot(
                            colorHex = account.colorHex,
                            modifier = Modifier.padding(end = space.padding_1x),
                        )
                        Column(Modifier.weight(1f)) {
                            Text(text = account.name, style = type.body, color = colors.text)
                            Text(
                                text = account.currency.value,
                                style = type.caption.copy(color = colors.text2),
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icon.Check.imageVector,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(space.icon_1x),
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(space.padding_1x))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun MmWalletPickerSheetPreview() {
    val accounts = listOf(
        previewAccount(1L, "Cash", "#3B82F6", isDefault = true),
        previewAccount(2L, "Bank", "#22C55E"),
    )
    MoneyMTheme {
        MmWalletPickerSheet(
            accounts = accounts,
            selectedAccountId = AccountId(1L),
            onSelect = {},
            onDismiss = {},
        )
    }
}

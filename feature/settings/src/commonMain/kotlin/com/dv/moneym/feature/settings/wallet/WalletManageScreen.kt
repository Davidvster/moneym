package com.dv.moneym.feature.settings.wallet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.model.Icon
import com.dv.moneym.core.ui.MmCard
import com.dv.moneym.core.ui.MmIconButton
import com.dv.moneym.core.ui.MmRow
import com.dv.moneym.core.ui.imageVector
import com.dv.moneym.core.ui.ScreenHeader
import com.dv.moneym.core.ui.SectionLabel
import kotlinx.serialization.Serializable
import moneym.feature.settings.generated.resources.Res
import moneym.feature.settings.generated.resources.settings_wallet_manage_title
import moneym.feature.settings.generated.resources.settings_wallet_no_wallets
import moneym.feature.settings.generated.resources.settings_wallet_section_header
import moneym.feature.settings.generated.resources.settings_wallet_type_currency
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Serializable
data object WalletManageKey : NavKey

fun EntryProviderScope<NavKey>.walletManageEntry(
    onBack: () -> Unit,
    onNavigateToAddWallet: () -> Unit,
) = entry<WalletManageKey> {
    WalletManageScreen(
        onBack = onBack,
        onNavigateToAddWallet = onNavigateToAddWallet,
    )
}

@Composable
private fun WalletManageScreen(
    onBack: () -> Unit,
    onNavigateToAddWallet: () -> Unit,
    viewModel: WalletManageViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val colors = MM.colors
    val type = MM.type
    val space = MM.dimen

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        ScreenHeader(
            title = stringResource(Res.string.settings_wallet_manage_title),
            onBack = onBack,
            trailingContent = {
                MmIconButton(
                    icon = Icon.Plus.imageVector,
                    onClick = onNavigateToAddWallet,
                )
            },
        )

        if (state.pendingDeleteId != null) {
            WalletDeleteSheet(
                onConfirm = { viewModel.onIntent(WalletManageIntent.DeleteConfirmed) },
                onCancel = { viewModel.onIntent(WalletManageIntent.DeleteCancelled) },
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(
                horizontal = space.padding_2x,
                vertical = space.padding_2x,
            ),
        ) {
            val activeAccounts = state.accounts.filter { !it.archived }

            if (activeAccounts.isEmpty()) {
                item {
                    Text(
                        text = stringResource(Res.string.settings_wallet_no_wallets),
                        style = type.body,
                        color = colors.text3,
                        modifier = Modifier.padding(space.padding_2x),
                    )
                }
            } else {
                item {
                    SectionLabel(
                        text = stringResource(Res.string.settings_wallet_section_header),
                        modifier = Modifier.padding(bottom = space.padding_0_5x),
                    )
                }
                item {
                    MmCard(padded = false, shape = MM.dimen.radius_1_5x) {
                        Column {
                            activeAccounts.forEachIndexed { idx, account ->
                                val isSelected = account.id.value == state.selectedAccountId ||
                                        (state.selectedAccountId <= 0L && account.isDefault)
                                MmRow(
                                    onClick = {
                                        viewModel.onIntent(WalletManageIntent.SelectAccount(account.id.value))
                                    },
                                    divider = idx < activeAccounts.lastIndex,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = account.name,
                                            style = type.body,
                                            color = colors.text,
                                        )
                                        Text(
                                            text = stringResource(
                                                Res.string.settings_wallet_type_currency,
                                                account.type.name.lowercase()
                                                    .replaceFirstChar { it.uppercase() },
                                                account.currency.value,
                                            ),
                                            style = type.caption.copy(color = colors.text2),
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icon.Check.imageVector,
                                            contentDescription = null,
                                            tint = colors.accent,
                                            modifier = Modifier.size(MM.dimen.icon_1x),
                                        )
                                    }
                                    MmIconButton(
                                        icon = Icon.Trash.imageVector,
                                        onClick = {
                                            viewModel.onIntent(WalletManageIntent.DeleteRequested(account.id.value))
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

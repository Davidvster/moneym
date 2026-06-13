package com.dv.moneym.feature.walletsync.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.ui.MmButton
import com.dv.moneym.core.ui.MmButtonSize
import com.dv.moneym.core.ui.MmButtonVariant
import com.dv.moneym.core.ui.MmCard
import com.dv.moneym.core.ui.MmCheckbox
import com.dv.moneym.core.ui.MmField
import com.dv.moneym.core.ui.MmLoadingOverlay
import com.dv.moneym.core.ui.MmToggle
import com.dv.moneym.core.ui.ScreenHeader
import kotlinx.serialization.Serializable
import moneym.feature.walletsync.generated.resources.Res
import moneym.feature.walletsync.generated.resources.wallet_sync_access_granted
import moneym.feature.walletsync.generated.resources.wallet_sync_access_required
import moneym.feature.walletsync.generated.resources.wallet_sync_app_picker_title
import moneym.feature.walletsync.generated.resources.wallet_sync_apps_loading
import moneym.feature.walletsync.generated.resources.wallet_sync_apps_none
import moneym.feature.walletsync.generated.resources.wallet_sync_apps_selected
import moneym.feature.walletsync.generated.resources.wallet_sync_done
import moneym.feature.walletsync.generated.resources.wallet_sync_enable_subtitle
import moneym.feature.walletsync.generated.resources.wallet_sync_enable_title
import moneym.feature.walletsync.generated.resources.wallet_sync_grant_access
import moneym.feature.walletsync.generated.resources.wallet_sync_intro
import moneym.feature.walletsync.generated.resources.wallet_sync_review_pending
import moneym.feature.walletsync.generated.resources.wallet_sync_search
import moneym.feature.walletsync.generated.resources.wallet_sync_select_apps
import moneym.feature.walletsync.generated.resources.wallet_sync_select_apps_subtitle
import moneym.feature.walletsync.generated.resources.wallet_sync_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Serializable
data object WalletSyncSettingsKey : NavKey

fun EntryProviderScope<NavKey>.walletSyncSettingsEntry(
    onBack: () -> Unit,
    onOpenSuggestions: () -> Unit,
    metadata: Map<String, Any> = emptyMap(),
) = entry<WalletSyncSettingsKey>(metadata = metadata) {
    WalletSyncHomeScreen(onBack = onBack, onOpenSuggestions = onOpenSuggestions)
}

@Composable
fun WalletSyncHomeScreen(
    onBack: () -> Unit,
    onOpenSuggestions: () -> Unit,
    viewModel: WalletSyncHomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        viewModel.onIntent(WalletSyncHomeIntent.Refresh)
        onPauseOrDispose { }
    }

    WalletSyncHomeContent(
        state = state,
        onBack = onBack,
        onOpenSuggestions = onOpenSuggestions,
        onIntent = viewModel::onIntent,
    )
}

@Composable
private fun WalletSyncHomeContent(
    state: WalletSyncHomeUiState,
    onBack: () -> Unit,
    onOpenSuggestions: () -> Unit,
    onIntent: (WalletSyncHomeIntent) -> Unit,
) {
    val colors = MM.colors
    val space = MM.dimen

    Box(modifier = Modifier.fillMaxSize().background(colors.bg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(title = stringResource(Res.string.wallet_sync_title), onBack = onBack)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = space.padding_2x),
                verticalArrangement = Arrangement.spacedBy(space.padding_2x),
            ) {
                Text(
                    text = stringResource(Res.string.wallet_sync_intro),
                    style = MM.type.caption.copy(color = colors.text2),
                    modifier = Modifier.padding(top = space.padding_1x),
                )

                if (!state.accessGranted) {
                    MmCard(modifier = Modifier.fillMaxWidth(), padded = true) {
                        Text(
                            text = stringResource(Res.string.wallet_sync_access_required),
                            style = MM.type.body,
                            color = colors.text,
                        )
                        MmButton(
                            text = stringResource(Res.string.wallet_sync_grant_access),
                            onClick = { onIntent(WalletSyncHomeIntent.OpenAccessSettings) },
                            fullWidth = true,
                            modifier = Modifier.padding(top = space.padding_1x),
                        )
                    }
                } else {
                    Text(
                        text = stringResource(Res.string.wallet_sync_access_granted),
                        style = MM.type.caption.copy(color = colors.accent),
                    )
                }

                MmCard(modifier = Modifier.fillMaxWidth(), padded = true) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(Res.string.wallet_sync_enable_title),
                                style = MM.type.body,
                                color = colors.text,
                            )
                            Text(
                                text = stringResource(Res.string.wallet_sync_enable_subtitle),
                                style = MM.type.caption.copy(color = colors.text2),
                            )
                        }
                        MmToggle(
                            checked = state.enabled,
                            onCheckedChange = { onIntent(WalletSyncHomeIntent.ToggleEnabled) },
                        )
                    }
                }

                MmCard(modifier = Modifier.fillMaxWidth(), padded = true) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onIntent(WalletSyncHomeIntent.ShowAppPicker(true)) },
                    ) {
                        Text(
                            text = stringResource(Res.string.wallet_sync_select_apps),
                            style = MM.type.body,
                            color = colors.text,
                        )
                        Text(
                            text = if (state.selectedPackages.isEmpty()) {
                                stringResource(Res.string.wallet_sync_apps_none)
                            } else {
                                stringResource(Res.string.wallet_sync_apps_selected, state.selectedPackages.size)
                            },
                            style = MM.type.caption.copy(color = colors.text2),
                        )
                        Text(
                            text = stringResource(Res.string.wallet_sync_select_apps_subtitle),
                            style = MM.type.caption.copy(color = colors.text2),
                            modifier = Modifier.padding(top = space.padding_0_5x),
                        )
                    }
                }

                if (state.pendingCount > 0) {
                    MmButton(
                        text = stringResource(Res.string.wallet_sync_review_pending, state.pendingCount),
                        onClick = onOpenSuggestions,
                        fullWidth = true,
                    )
                }
            }
        }
        MmLoadingOverlay(visible = state.isLoading)
    }

    if (state.showAppPicker) {
        AppPickerSheet(state = state, onIntent = onIntent)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppPickerSheet(
    state: WalletSyncHomeUiState,
    onIntent: (WalletSyncHomeIntent) -> Unit,
) {
    val colors = MM.colors
    val space = MM.dimen
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = { onIntent(WalletSyncHomeIntent.ShowAppPicker(false)) },
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = space.padding_2_5x, topEnd = space.padding_2_5x),
        containerColor = colors.bg,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = space.padding_2_5x, vertical = space.padding_2x),
            verticalArrangement = Arrangement.spacedBy(space.padding_1x),
        ) {
            Text(
                text = stringResource(Res.string.wallet_sync_app_picker_title),
                style = MM.type.title3,
                color = colors.text,
            )
            MmField(
                value = state.appQuery,
                onValueChange = { onIntent(WalletSyncHomeIntent.SetAppQuery(it)) },
                label = stringResource(Res.string.wallet_sync_search),
            )

            if (state.appsLoading) {
                Text(
                    text = stringResource(Res.string.wallet_sync_apps_loading),
                    style = MM.type.caption.copy(color = colors.text2),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(space.padding_2x),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                    contentPadding = PaddingValues(vertical = space.padding_1x),
                ) {
                    items(state.filteredApps, key = { it.packageName }) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onIntent(WalletSyncHomeIntent.ToggleApp(app.packageName)) }
                                .padding(vertical = space.padding_1x),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            MmCheckbox(
                                checked = app.packageName in state.selectedPackages,
                                onCheckedChange = { onIntent(WalletSyncHomeIntent.ToggleApp(app.packageName)) },
                            )
                            Text(
                                text = app.label,
                                style = MM.type.body,
                                color = colors.text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f).padding(start = space.padding_1x),
                            )
                        }
                    }
                }
            }

            MmButton(
                text = stringResource(Res.string.wallet_sync_done),
                onClick = { onIntent(WalletSyncHomeIntent.ShowAppPicker(false)) },
                variant = MmButtonVariant.Primary,
                size = MmButtonSize.Md,
                fullWidth = true,
            )
        }
    }
}

package com.dv.moneym.feature.onboarding.restore

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.model.Icon
import com.dv.moneym.core.ui.MmButton
import com.dv.moneym.core.ui.MmButtonSize
import com.dv.moneym.core.ui.MmButtonVariant
import com.dv.moneym.core.ui.MmCard
import com.dv.moneym.core.ui.MmDialog
import com.dv.moneym.core.ui.MmField
import com.dv.moneym.core.ui.MmIconButton
import com.dv.moneym.core.ui.MmLoadingOverlay
import com.dv.moneym.core.ui.MmRow
import com.dv.moneym.core.ui.ScreenHeader
import com.dv.moneym.core.ui.SectionLabel
import com.dv.moneym.core.ui.imageVector
import com.dv.moneym.data.remotebackup.RemoteBackupMetadata
import com.dv.moneym.platform.rememberBinaryFilePicker
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import moneym.feature.onboarding.generated.resources.Res
import moneym.feature.onboarding.generated.resources.onboarding_restore_app_close_notice
import moneym.feature.onboarding.generated.resources.onboarding_restore_cancel
import moneym.feature.onboarding.generated.resources.onboarding_restore_confirm
import moneym.feature.onboarding.generated.resources.onboarding_restore_google_button
import moneym.feature.onboarding.generated.resources.onboarding_restore_google_connect
import moneym.feature.onboarding.generated.resources.onboarding_restore_google_section
import moneym.feature.onboarding.generated.resources.onboarding_restore_local_button
import moneym.feature.onboarding.generated.resources.onboarding_restore_passphrase_label
import moneym.feature.onboarding.generated.resources.onboarding_restore_remote_loading
import moneym.feature.onboarding.generated.resources.onboarding_restore_screen_title
import moneym.feature.onboarding.generated.resources.onboarding_restore_warning_body
import moneym.feature.onboarding.generated.resources.onboarding_restore_warning_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Serializable
data object OnboardingRestoreKey : NavKey

fun EntryProviderScope<NavKey>.onboardingRestoreEntry(
    onBack: () -> Unit,
    viewModel: OnboardingRestoreViewModel? = null,
) = entry<OnboardingRestoreKey> {
    OnboardingRestoreScreen(
        onBack = onBack,
        viewModel = viewModel ?: koinViewModel(),
    )
}

@Composable
private fun OnboardingRestoreScreen(
    onBack: () -> Unit,
    viewModel: OnboardingRestoreViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val restorePicker = rememberBinaryFilePicker { bytes ->
        if (bytes != null) viewModel.onIntent(OnboardingRestoreIntent.LocalFileSelected(bytes))
    }

    if (state.showLocalRestoreDialog) {
        LocalRestoreDialog(
            needsPassphrase = state.localNeedsPassphrase,
            errorMessage = state.localError,
            onDismiss = { viewModel.onIntent(OnboardingRestoreIntent.LocalRestoreDismissed) },
            onConfirm = { viewModel.onIntent(OnboardingRestoreIntent.LocalRestoreConfirmed(it)) },
        )
    }

    if (state.showRemoteRestoreDialog) {
        RemoteRestoreDialog(
            loading = state.remotePreviewLoading,
            preview = state.remotePreview,
            onDismiss = { viewModel.onIntent(OnboardingRestoreIntent.RemoteRestoreDismissed) },
            onConfirm = { viewModel.onIntent(OnboardingRestoreIntent.RemoteRestoreConfirmed(it)) },
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        OnboardingRestoreContent(
            state = state,
            onBack = onBack,
            onRestoreFromFile = restorePicker,
            onConnectGoogle = { viewModel.onIntent(OnboardingRestoreIntent.ConnectGoogleTapped) },
            onRemoteRestore = { viewModel.onIntent(OnboardingRestoreIntent.RemoteRestoreTapped) },
        )
        MmLoadingOverlay(visible = state.isLoading)
    }
}

@Composable
private fun OnboardingRestoreContent(
    state: OnboardingRestoreUiState,
    onBack: () -> Unit,
    onRestoreFromFile: () -> Unit,
    onConnectGoogle: () -> Unit,
    onRemoteRestore: () -> Unit,
) {
    val colors = MM.colors
    val type = MM.type
    val space = MM.dimen

    Column(modifier = Modifier.fillMaxSize().background(colors.bg)) {
        ScreenHeader(
            title = stringResource(Res.string.onboarding_restore_screen_title),
            onBack = onBack,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = space.padding_2x, vertical = space.padding_2x),
        ) {
            MmButton(
                text = stringResource(Res.string.onboarding_restore_local_button),
                onClick = onRestoreFromFile,
                variant = MmButtonVariant.Primary,
                size = MmButtonSize.Lg,
                fullWidth = true,
            )

            if (state.remoteAvailable) {
                Spacer(Modifier.height(space.padding_3x))
                SectionLabel(
                    text = stringResource(Res.string.onboarding_restore_google_section),
                    modifier = Modifier.padding(
                        horizontal = space.padding_0_5x,
                        vertical = space.padding_0_5x,
                    ),
                )
                MmCard {
                    if (!state.remoteSignedIn) {
                        MmRow(onClick = onConnectGoogle, divider = false) {
                            Text(
                                stringResource(Res.string.onboarding_restore_google_connect),
                                style = type.body,
                                color = colors.text,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    } else {
                        state.remoteAccountEmail?.let { email ->
                            MmRow(divider = true) {
                                Text(email, style = type.caption.copy(color = colors.text2))
                            }
                        }
                        MmRow(onClick = onRemoteRestore, divider = false) {
                            Text(
                                stringResource(Res.string.onboarding_restore_google_button),
                                style = type.body,
                                color = colors.text,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LocalRestoreDialog(
    needsPassphrase: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (CharArray?) -> Unit,
) {
    val colors = MM.colors
    val type = MM.type
    val space = MM.dimen
    var passphrase by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }

    MmDialog(
        title = stringResource(Res.string.onboarding_restore_warning_title),
        confirmText = stringResource(Res.string.onboarding_restore_confirm),
        confirmEnabled = !needsPassphrase || passphrase.isNotEmpty(),
        onConfirm = { onConfirm(if (needsPassphrase) passphrase.toCharArray() else null) },
        onDismiss = onDismiss,
        dismissText = stringResource(Res.string.onboarding_restore_cancel),
    ) {
        Text(
            stringResource(Res.string.onboarding_restore_warning_body),
            style = type.body,
            color = colors.text2,
        )
        Text(
            stringResource(Res.string.onboarding_restore_app_close_notice),
            style = type.caption,
            color = colors.text3,
            modifier = Modifier.padding(top = space.padding_1x),
        )
        if (needsPassphrase) {
            MmField(
                value = passphrase,
                onValueChange = { passphrase = it },
                label = stringResource(Res.string.onboarding_restore_passphrase_label),
                visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardType = KeyboardType.Password,
                suffix = {
                    MmIconButton(
                        icon = if (visible) Icon.EyeOff.imageVector else Icon.Eye.imageVector,
                        onClick = { visible = !visible },
                        contentDescription = null,
                    )
                },
            )
        }
        if (errorMessage != null) {
            Text(errorMessage, color = colors.danger, style = type.caption)
        }
    }
}

@Composable
private fun RemoteRestoreDialog(
    loading: Boolean,
    preview: RemoteBackupMetadata?,
    onDismiss: () -> Unit,
    onConfirm: (CharArray) -> Unit,
) {
    val colors = MM.colors
    val type = MM.type
    val space = MM.dimen
    var input by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }

    MmDialog(
        title = stringResource(Res.string.onboarding_restore_warning_title),
        confirmText = stringResource(Res.string.onboarding_restore_confirm),
        confirmEnabled = input.isNotEmpty() && !loading,
        onConfirm = { onConfirm(input.toCharArray()) },
        onDismiss = onDismiss,
        dismissText = stringResource(Res.string.onboarding_restore_cancel),
    ) {
        Text(
            stringResource(Res.string.onboarding_restore_app_close_notice),
            style = type.caption,
            color = colors.text3,
        )
        if (loading) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(space.padding_1x),
                modifier = Modifier.padding(top = space.padding_1x),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(space.icon_1x),
                    strokeWidth = space.padding_0_25x,
                    color = colors.accent,
                )
                Text(stringResource(Res.string.onboarding_restore_remote_loading), style = type.caption)
            }
        } else if (preview != null) {
            Column(modifier = Modifier.padding(top = space.padding_1x)) {
                formatTime(preview.createdAtMs)?.let {
                    Text(it, style = type.caption.copy(color = colors.text2))
                }
                Text(preview.appVersion, style = type.caption.copy(color = colors.text3))
                Text("${preview.sizeBytes / 1024L} KB", style = type.caption.copy(color = colors.text3))
            }
        }
        MmField(
            value = input,
            onValueChange = { if (!loading) input = it },
            label = stringResource(Res.string.onboarding_restore_passphrase_label),
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardType = KeyboardType.Password,
            suffix = {
                MmIconButton(
                    icon = if (visible) Icon.EyeOff.imageVector else Icon.Eye.imageVector,
                    onClick = { visible = !visible },
                    contentDescription = null,
                )
            },
        )
    }
}

private fun formatTime(ms: Long): String? {
    if (ms == 0L) return null
    val dt = Instant.fromEpochMilliseconds(ms).toLocalDateTime(TimeZone.currentSystemDefault())
    val h = dt.hour.toString().padStart(2, '0')
    val m = dt.minute.toString().padStart(2, '0')
    return "${dt.date} $h:$m"
}

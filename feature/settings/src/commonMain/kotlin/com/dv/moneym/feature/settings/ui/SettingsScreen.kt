package com.dv.moneym.feature.settings.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dv.moneym.core.designsystem.MoneyMTheme
import com.dv.moneym.feature.settings.presentation.SettingsEffect
import com.dv.moneym.feature.settings.presentation.SettingsIntent
import com.dv.moneym.feature.settings.presentation.SettingsViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsScreen(
    onNavigateToPinSetup: () -> Unit,
    onNavigateToCategories: () -> Unit = {},
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                SettingsEffect.NavigateToPinSetup -> onNavigateToPinSetup()
            }
        }
    }
    SettingsContent(state = state, onIntent = viewModel::onIntent, onNavigateToCategories = onNavigateToCategories)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsContent(
    state: com.dv.moneym.feature.settings.presentation.SettingsUiState,
    onIntent: (SettingsIntent) -> Unit,
    onNavigateToCategories: () -> Unit = {},
) {
    val pinEnabled = state.pinEnabled
    val biometricEnabled = state.biometricEnabled
    val biometricAvailable = state.biometricAvailable
    val backgroundLockSeconds = state.backgroundLockSeconds
    val sp = MoneyMTheme.spacing
    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = sp.lg)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(sp.md))
            Text(
                "Security",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(sp.sm))
            SettingsRow(
                label = "Enable PIN lock",
                checked = pinEnabled,
                onCheckedChange = { onIntent(SettingsIntent.PinToggled(it)) },
            )
            if (pinEnabled) {
                TextButton(
                    onClick = { onIntent(SettingsIntent.ChangePinRequested) },
                    modifier = Modifier.padding(start = sp.xs),
                ) {
                    Text("Change PIN")
                }
                if (biometricAvailable) {
                    SettingsRow(
                        label = "Use biometric unlock",
                        checked = biometricEnabled,
                        onCheckedChange = { onIntent(SettingsIntent.BiometricToggled(it)) },
                    )
                }
                val lockOptions = listOf(0 to "Always", 30 to "30 seconds", 60 to "1 minute", 300 to "5 minutes")
                Spacer(Modifier.height(sp.sm))
                Text("Lock after", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(sp.xs))
                Row {
                    lockOptions.forEach { (seconds, label) ->
                        val selected = backgroundLockSeconds == seconds
                        TextButton(
                            onClick = { onIntent(SettingsIntent.LockTimeoutChanged(seconds)) },
                        ) {
                            Text(
                                label,
                                color = if (selected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(sp.md))
            HorizontalDivider()
            Spacer(Modifier.height(sp.md))
            Text(
                "Data",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            TextButton(
                onClick = onNavigateToCategories,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Manage Categories", modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(sp.md))
            HorizontalDivider()
            Spacer(Modifier.height(sp.md))
            Text("Backup", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(sp.sm))
            Row {
                TextButton(onClick = { onIntent(SettingsIntent.ExportJsonRequested) }, enabled = !state.isExporting) {
                    Text("Export JSON")
                }
                TextButton(onClick = { onIntent(SettingsIntent.ExportCsvRequested) }, enabled = !state.isExporting) {
                    Text("Export CSV")
                }
            }
            // Exported content display
            @Suppress("DEPRECATION")
            val clipboard = LocalClipboardManager.current
            val exported = state.exportedJson
            if (exported != null) {
                Spacer(Modifier.height(sp.sm))
                OutlinedTextField(
                    value = exported.take(500) + if (exported.length > 500) "…" else "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Exported content (${exported.length} chars)") },
                    modifier = Modifier.fillMaxWidth().heightIn(max = 120.dp),
                )
                Row {
                    TextButton(onClick = { clipboard.setText(AnnotatedString(exported)) }) {
                        Text("Copy to clipboard")
                    }
                    TextButton(onClick = { onIntent(SettingsIntent.ClearExport) }) {
                        Text("Clear")
                    }
                }
            }
            Spacer(Modifier.height(sp.md))
            Text("Import from JSON", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(sp.xs))
            OutlinedTextField(
                value = state.importJson,
                onValueChange = { onIntent(SettingsIntent.ImportJsonChanged(it)) },
                label = { Text("Paste JSON here") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                isError = state.importError != null,
                supportingText = if (state.importError != null) ({ Text(state.importError) }) else null,
            )
            val preview = state.importPreview
            if (state.importJson.isNotBlank()) {
                Row {
                    TextButton(onClick = { onIntent(SettingsIntent.PreviewImportRequested) }) {
                        Text("Preview")
                    }
                    if (preview != null) {
                        TextButton(onClick = { onIntent(SettingsIntent.ApplyImportRequested) }, enabled = !state.isImporting) {
                            Text("Import")
                        }
                    }
                    TextButton(onClick = { onIntent(SettingsIntent.ClearImport) }) {
                        Text("Clear")
                    }
                }
                if (preview != null) {
                    Text(
                        "New: ${preview.transactions.new} txns, ${preview.categories.new} cats, ${preview.accounts.new} accs\n" +
                        "Duplicates: ${preview.transactions.duplicate} txns, ${preview.categories.duplicate} cats",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (state.showImportSuccess) {
                    Text("Import complete!", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.height(sp.xxl))
        }
    }
}

@Composable
private fun SettingsRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val sp = MoneyMTheme.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = sp.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

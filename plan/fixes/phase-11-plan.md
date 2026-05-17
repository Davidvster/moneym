# Phase 11 — Settings export/import data: actually export to file and import from file

## Goal
Wire up the export (JSON, CSV) and import buttons in settings to actually write/read files using platform file operations.

Currently:
- Export JSON: calls `SettingsIntent.ExportJsonRequested` -> ViewModel produces the JSON string -> stored in `state.exportedJson` -> nothing writes it to a file.
- Export CSV: same.
- Import: `MmRow(onClick = {})` is a no-op.

## Strategy: expect/actual FilePlatform

### Step 1: Create expect class
Create file: `composeApp/src/commonMain/kotlin/com/dv/moneym/platform/FilePlatform.kt`

```kotlin
package com.dv.moneym.platform

expect class FilePlatform {
    suspend fun saveFile(suggestedName: String, content: String, mimeType: String): Boolean
    suspend fun openTextFile(): String?
}
```

### Step 2: Android actual implementation
Create file: `composeApp/src/androidMain/kotlin/com/dv/moneym/platform/FilePlatform.android.kt`

```kotlin
package com.dv.moneym.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

actual class FilePlatform(private val context: Context) {
    actual suspend fun saveFile(suggestedName: String, content: String, mimeType: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val cacheFile = File(context.cacheDir, suggestedName)
                cacheFile.writeText(content)
                val uri: Uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    cacheFile,
                )
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = mimeType
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(
                    Intent.createChooser(shareIntent, "Save $suggestedName").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    actual suspend fun openTextFile(): String? = null
}
```

### Step 3: iOS actual implementation
Create file: `composeApp/src/iosMain/kotlin/com/dv/moneym/platform/FilePlatform.ios.kt`

```kotlin
package com.dv.moneym.platform

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.writeToFile

actual class FilePlatform {
    actual suspend fun saveFile(suggestedName: String, content: String, mimeType: String): Boolean {
        return withContext(Dispatchers.Main) {
            try {
                val dirs = NSSearchPathForDirectoriesInDomains(
                    NSDocumentDirectory, NSUserDomainMask, true
                )
                val documentsDir = dirs.firstOrNull() as? String ?: return@withContext false
                val filePath = "$documentsDir/$suggestedName"
                @Suppress("CAST_NEVER_SUCCEEDS")
                (content as NSString).writeToFile(
                    path = filePath,
                    atomically = true,
                    encoding = NSUTF8StringEncoding,
                    error = null,
                )
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    actual suspend fun openTextFile(): String? = null
}
```

### Step 4: Register in DI

Read `composeApp/src/androidMain/kotlin/com/dv/moneym/di/AndroidPlatformModule.kt` first, then add:
```kotlin
single { FilePlatform(get()) }
```

Read `composeApp/src/iosMain/kotlin/com/dv/moneym/di/IosPlatformModule.kt` first, then add:
```kotlin
single { FilePlatform() }
```

### Step 5: Add ExportReady + ImportRequested effects to SettingsUiState.kt

Read the file first. In `sealed interface SettingsEffect`, add:
```kotlin
data class ExportReady(val content: String, val fileName: String, val mimeType: String) : SettingsEffect
data object ImportRequested : SettingsEffect
```

Also add to `sealed interface SettingsIntent`:
```kotlin
data object ImportRequested : SettingsIntent
```

### Step 6: Update SettingsViewModel.kt

Read the file first. Change export handlers to emit effects instead of storing in state:

```kotlin
SettingsIntent.ExportJsonRequested -> {
    _state.update { it.copy(isExporting = true) }
    viewModelScope.launch {
        val json = withContext(dispatchers.io) { exporter.exportToJson() }
        _state.update { it.copy(isExporting = false) }
        _effects.send(SettingsEffect.ExportReady(json, "moneym_backup.json", "application/json"))
    }
}
SettingsIntent.ExportCsvRequested -> {
    _state.update { it.copy(isExporting = true) }
    viewModelScope.launch {
        val csv = withContext(dispatchers.io) { exporter.exportToCsv() }
        _state.update { it.copy(isExporting = false) }
        _effects.send(SettingsEffect.ExportReady(csv, "moneym_export.csv", "text/csv"))
    }
}
SettingsIntent.ImportRequested -> {
    viewModelScope.launch { _effects.send(SettingsEffect.ImportRequested) }
}
```

### Step 7: Update SettingsScreen.kt

Read the file first. Inject `FilePlatform` and `CoroutineScope`, then handle effects:

```kotlin
@Composable
fun SettingsScreen(
    ...
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val filePlatform = koinInject<FilePlatform>()
    val scope = rememberCoroutineScope()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                SettingsEffect.NavigateToPinSetup -> onNavigateToPinSetup()
                is SettingsEffect.ExportReady -> {
                    scope.launch {
                        filePlatform.saveFile(effect.fileName, effect.content, effect.mimeType)
                    }
                }
                SettingsEffect.ImportRequested -> {
                    scope.launch {
                        val content = filePlatform.openTextFile()
                        if (content != null) {
                            viewModel.onIntent(SettingsIntent.ImportJsonChanged(content))
                            viewModel.onIntent(SettingsIntent.ApplyImportRequested)
                        }
                    }
                }
            }
        }
    }
    ...
}
```

Wire import button. Find in SettingsContent the import row:
```kotlin
MmRow(onClick = {}, divider = false) {
```
Change to:
```kotlin
MmRow(onClick = { onIntent(SettingsIntent.ImportRequested) }, divider = false) {
```

### Step 8: Add FileProvider for Android

Check if `composeApp/src/androidMain/AndroidManifest.xml` has a provider. If not, add it.

Also create `composeApp/src/androidMain/res/xml/file_paths.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <cache-path name="cache" path="." />
</paths>
```

Find the AndroidManifest at `composeApp/src/androidMain/AndroidManifest.xml`. Read it first. Inside `<application>` add:
```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.provider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
</provider>
```

## Files to Create
- `composeApp/src/commonMain/kotlin/com/dv/moneym/platform/FilePlatform.kt`
- `composeApp/src/androidMain/kotlin/com/dv/moneym/platform/FilePlatform.android.kt`
- `composeApp/src/iosMain/kotlin/com/dv/moneym/platform/FilePlatform.ios.kt`
- `composeApp/src/androidMain/res/xml/file_paths.xml`

## Files to Modify (read first, then edit)
- `composeApp/src/androidMain/kotlin/com/dv/moneym/di/AndroidPlatformModule.kt`
- `composeApp/src/iosMain/kotlin/com/dv/moneym/di/IosPlatformModule.kt`
- `feature/settings/src/commonMain/kotlin/com/dv/moneym/feature/settings/presentation/SettingsUiState.kt`
- `feature/settings/src/commonMain/kotlin/com/dv/moneym/feature/settings/presentation/SettingsViewModel.kt`
- `feature/settings/src/commonMain/kotlin/com/dv/moneym/feature/settings/ui/SettingsScreen.kt`
- `composeApp/src/androidMain/AndroidManifest.xml`

## Acceptance Criteria
1. Tapping "Export as JSON" triggers a platform share/save dialog with JSON file content (Android: share sheet; iOS: file written to Documents)
2. Tapping "Export as CSV" does the same for CSV content
3. Tapping "Import data" calls `filePlatform.openTextFile()` (returns null for now, but the button is no longer a no-op)
4. No crashes on any data section button tap
5. Build compiles successfully: `./gradlew :composeApp:assembleDebug`

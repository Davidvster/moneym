# Phase 1 — Fix "Lock after X seconds" not working

## Goal
Wire the "Lock after" settings row to actually show a picker and let users change the timeout value. Currently the row has `onClick = {}` (no-op).

## Problem Analysis
In `SettingsScreen.kt` line 304:
```kotlin
MmRow(onClick = {}, divider = false) {
```
The row does nothing when clicked. The intent `SettingsIntent.LockTimeoutChanged(seconds)` already exists and is handled in `SettingsViewModel.kt` (line 92-95). `SecurityPrefs.BACKGROUND_LOCK_SECONDS` is already read and stored. The lock logic in `AppLockController.kt` correctly reads the value. The problem is purely UI: nothing calls `LockTimeoutChanged`.

## Files to Change
- `feature/settings/src/commonMain/kotlin/com/dv/moneym/feature/settings/ui/SettingsScreen.kt`

## Implementation Steps

### Step 1: Add local state for showing the lock picker dialog
Inside `SettingsContent` composable, add:
```kotlin
var showLockPicker by remember { mutableStateOf(false) }
```

### Step 2: Wire up the "Lock after" row click
Change:
```kotlin
MmRow(onClick = {}, divider = false) {
```
to:
```kotlin
MmRow(onClick = { showLockPicker = true }, divider = false) {
```

### Step 3: Add a `LockTimeoutPickerDialog` composable
Add a new private composable after `SettingsContent` in the same file:

```kotlin
@Composable
private fun LockTimeoutPickerDialog(
    currentSeconds: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val colors = MM.colors
    val type = MM.type

    val options = listOf(
        0 to "Immediately",
        30 to "30 seconds",
        60 to "1 minute",
        300 to "5 minutes",
    )
    var selectedSeconds by remember { mutableStateOf(currentSeconds) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Lock after",
                style = type.title3,
                color = colors.text,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                options.forEach { (seconds, label) ->
                    val isSelected = seconds == selectedSeconds
                    MmRow(
                        onClick = { selectedSeconds = seconds },
                        divider = false,
                        padding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = label,
                            style = type.body,
                            color = colors.text,
                            modifier = Modifier.weight(1f),
                        )
                        // Radio circle
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .border(
                                    1.5.dp,
                                    if (isSelected) colors.accent else colors.borderStrong,
                                    CircleShape,
                                )
                                .background(if (isSelected) colors.accent else Color.Transparent),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = MmIcons.check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp),
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedSeconds) }) {
                Text("OK", color = colors.accent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.text2)
            }
        },
        containerColor = colors.surface,
        titleContentColor = colors.text,
    )
}
```

### Step 4: Show the dialog when state is true
Inside `SettingsContent`, after the state variables, add:
```kotlin
if (showLockPicker) {
    LockTimeoutPickerDialog(
        currentSeconds = state.backgroundLockSeconds,
        onDismiss = { showLockPicker = false },
        onConfirm = { seconds ->
            onIntent(SettingsIntent.LockTimeoutChanged(seconds))
            showLockPicker = false
        },
    )
}
```

### Step 5: Add required imports
Add these imports to the file if not already present:
- `androidx.compose.material3.AlertDialog`
- `androidx.compose.material3.TextButton`
- `androidx.compose.foundation.layout.PaddingValues`
- `androidx.compose.foundation.layout.Arrangement`
- `androidx.compose.foundation.shape.CircleShape`
- `androidx.compose.foundation.border`
- `androidx.compose.material3.Icon` (already present)
- `androidx.compose.ui.graphics.Color`
- `androidx.compose.runtime.mutableStateOf`
- `androidx.compose.runtime.remember`
- `androidx.compose.runtime.setValue`
- `androidx.compose.runtime.getValue`
- `androidx.compose.runtime.saveable.rememberSaveable` (or use `remember`)

## Acceptance Criteria
1. Tapping "Lock after" row opens a dialog with 4 options: Immediately, 30 seconds, 1 minute, 5 minutes
2. Current selection is pre-selected in the dialog (radio circle filled)
3. Tapping an option selects it (radio fills)
4. Pressing OK calls `SettingsIntent.LockTimeoutChanged(seconds)` and closes the dialog
5. Pressing Cancel closes the dialog without changes
6. The displayed label in the settings row updates to reflect the new selection
7. The dialog uses `MM.colors` (no raw Material3 colors)
8. Build compiles: `./gradlew :composeApp:assembleDebug`

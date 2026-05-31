# Phase 1: Encryption eye icon + Restore warning dialog theme

## Item 1 — Password eye toggle

**File:** `feature/settings/src/commonMain/kotlin/com/dv/moneym/feature/settings/overview/backuprestore/BackupRestoreScreen.kt`

`PasswordDialog` (starting ~line 471) currently has:
- First `MmField` (password): `suffix = { Text("Show"/"Hide") }` that toggles `visible` state
- Second `MmField` (repeat): no suffix toggle at all

**Fix:**
- Replace text suffix on first field with `MmIconButton(icon = if (visible) Icon.EyeOff.imageVector else Icon.Eye.imageVector, onClick = { visible = !visible }, contentDescription = null)`
- Add identical `suffix` to the repeat password `MmField`

**New icons needed:**

1. `core/model/src/commonMain/kotlin/com/dv/moneym/core/model/Icon.kt`
   - Add `Eye("eye")` and `EyeOff("eye_off")` to enum before the closing `;`

2. `core/ui/src/commonMain/kotlin/com/dv/moneym/core/ui/MmIconsExtra.kt`
   - Add two extension vals on `MmIcons` using the same pattern as `calculator`:

```kotlin
internal val MmIcons.eye: ImageVector
    get() = _eye ?: buildEye().also { _eye = it }

private var _eye: ImageVector? = null

private fun buildEye(): ImageVector =
    ImageVector.Builder(name = "eye", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
        // Outer eye shape
        addPath(
            pathData = addPathNodes("M2 12 C5 6 9 3 12 3 C15 3 19 6 22 12 C19 18 15 21 12 21 C9 21 5 18 2 12 Z"),
            stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round,
            fill = null, fillAlpha = 0f,
        )
        // Pupil circle
        addPath(
            pathData = addPathNodes("M12 9 A3 3 0 1 0 12 15 A3 3 0 1 0 12 9 Z"),
            stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round,
            fill = null, fillAlpha = 0f,
        )
    }.build()

internal val MmIcons.eyeOff: ImageVector
    get() = _eyeOff ?: buildEyeOff().also { _eyeOff = it }

private var _eyeOff: ImageVector? = null

private fun buildEyeOff(): ImageVector =
    ImageVector.Builder(name = "eye_off", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
        // Outer eye shape
        addPath(
            pathData = addPathNodes("M2 12 C5 6 9 3 12 3 C15 3 19 6 22 12 C19 18 15 21 12 21 C9 21 5 18 2 12 Z"),
            stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round,
            fill = null, fillAlpha = 0f,
        )
        // Strike-through diagonal
        addPath(
            pathData = addPathNodes("M3 3 L21 21"),
            stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round,
            fill = null, fillAlpha = 0f,
        )
    }.build()
```

3. `core/ui/src/commonMain/kotlin/com/dv/moneym/core/ui/MmIcons.kt`
   - In `fun forIcon(icon: Icon): ImageVector` (the `when` expression around line 1249), add before the closing brace:
     ```kotlin
     Icon.Eye -> eye
     Icon.EyeOff -> eyeOff
     ```

---

## Item 5 — Restore warning dialog theme

**File:** `feature/settings/src/commonMain/kotlin/com/dv/moneym/feature/settings/overview/backuprestore/BackupRestoreScreen.kt`

Current (lines 165-181):
```kotlin
if (state.showRestoreWarning) {
    AlertDialog(
        onDismissRequest = { viewModel.onIntent(BackupRestoreIntent.RestoreDismissed) },
        title = { Text(stringResource(Res.string.settings_restore_warning_title)) },
        text = { Text(stringResource(Res.string.settings_restore_warning_body)) },
        confirmButton = {
            TextButton(onClick = { viewModel.onIntent(BackupRestoreIntent.RestoreConfirmed) }) {
                Text(stringResource(Res.string.settings_restore_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.onIntent(BackupRestoreIntent.RestoreDismissed) }) {
                Text("Cancel")
            }
        },
    )
}
```

Replace with `MmDialog` (already imported):
```kotlin
if (state.showRestoreWarning) {
    MmDialog(
        title = stringResource(Res.string.settings_restore_warning_title),
        confirmText = stringResource(Res.string.settings_restore_confirm),
        onConfirm = { viewModel.onIntent(BackupRestoreIntent.RestoreConfirmed) },
        onDismiss = { viewModel.onIntent(BackupRestoreIntent.RestoreDismissed) },
        dismissText = stringResource(Res.string.overview_cancel),
    ) {
        Text(
            stringResource(Res.string.settings_restore_warning_body),
            style = MM.type.body,
            color = MM.colors.text2,
        )
    }
}
```

Note: check which cancel string resource to use — may need `settings_remote_passphrase_cancel` (already imported) instead of `overview_cancel`.

Also remove the now-unused `AlertDialog` import if it's only used for the restore warning (it's also used in `MmMonthPickerDialog` and `RemoteRestoreDialog` — check if still needed in BackupRestoreScreen.kt; if RemoteRestoreDialog still uses it, keep the import).

---

## Build verification
```bash
./gradlew :composeApp:assembleDebug
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
```

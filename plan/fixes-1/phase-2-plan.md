# Phase 2: Backup UI Improvements

All changes in:
- `feature/settings/src/commonMain/kotlin/com/dv/moneym/feature/settings/overview/backuprestore/BackupRestoreScreen.kt`
- `feature/settings/src/commonMain/kotlin/com/dv/moneym/feature/settings/overview/backuprestore/BackupRestoreViewModel.kt`

---

## 2.1 — GDrive last backup: show absolute date/time (not relative)

**Problem:** `RuntimeStatusLine` calls `relativeTimeLabel()` → shows "just now" / "5 min ago".  
**Fix:** Replace `relativeLabel` with `formatTime(lastRemoteMs) ?: stringResource(never)`.

In `RuntimeStatusLine` (lines 406-450):
- Remove `relativeLabel` computation (lines 415-416)
- Change `RemoteBackupRuntimeState.Idle` branch to use `formatTime(lastRemoteMs)` for the label
- Reuse existing `settings_remote_last_backup` string resource with the absolute date string
- If `lastRemoteMs == 0L`, use `settings_remote_last_backup_never`

---

## 2.2 — Remote restore dialog: app theme + passphrase toggle

**Problem:** `RemoteRestoreDialog` uses raw Material3 `AlertDialog` + `OutlinedTextField` (no visibility toggle).  
**Fix:** Replace with `MmDialog` + `MmField` with eye icon suffix.

In `RemoteRestoreDialog` (lines 557-651):
1. Replace `AlertDialog` with `MmDialog`
2. Add `var visible by remember { mutableStateOf(false) }` state
3. Replace `OutlinedTextField` with `MmField`:
   - `visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation()`
   - `keyboardType = KeyboardType.Password`
   - `suffix = { MmIconButton(icon = if (visible) Icon.EyeOff.imageVector else Icon.Eye.imageVector, onClick = { visible = !visible }, contentDescription = null) }`
4. Confirm button: `confirmEnabled = input.isNotEmpty() && !loading && !tooNew`
5. Keep all existing preview/conflict/too-new content inside the MmDialog content lambda
6. Loading spinner row: keep the `CircularProgressIndicator` inline inside MmDialog content when `loading == true`

---

## 2.3 — GDrive busy spinner: use green (accent) color

**Problem:** `CircularProgressIndicator` in `RemoteBackupSection` (line 387) uses default Material color (purple/primary).  
**Fix:** Add `color = MM.colors.accent` parameter.

```kotlin
CircularProgressIndicator(
    modifier = Modifier.size(space.icon_1x),
    strokeWidth = space.padding_0_25x,
    color = MM.colors.accent,
)
```

---

## 2.4 — Local backup: inline spinner instead of overlay

**Problem:** `isLoading` triggers full-screen `MmLoadingOverlay` even for local backup ops.

**Analysis of `isLoading` usage in ViewModel:**
- Line 176: `BackupTapped` → local backup → sets `isLoading = true`
- Line 191: `RestoreConfirmed` → local restore → sets `isLoading = true`
- Line 246: `AutoBackupLocationSelected` → auto-backup setup → sets `isLoading = true`
- Lines 196, 230, 232: clear `isLoading` after local ops
- Line 378: `RemoteRestoreConfirmed` → remote restore → also uses `isLoading`

**Fix:**
1. Add `isLocalLoading: Boolean = false` to `BackupRestoreUiState`
2. In ViewModel: change lines 176, 191, 246 to set `isLocalLoading = true` (not `isLoading`)
3. In ViewModel: change their corresponding clear-lines (196, 230, 232) to clear `isLocalLoading`
4. Keep `isLoading` for remote restore (line 378-381) — overlay stays for remote restore
5. In `BackupRestoreContent` (local backup row, line 278-287): when `state.isLocalLoading`, replace chevron icon with `CircularProgressIndicator(modifier = Modifier.size(space.icon_1x), strokeWidth = space.padding_0_25x, color = MM.colors.accent)`
6. `MmLoadingOverlay(visible = state.isLoading)` — keep but now only triggers for remote restore

---

## 2.5 — Local backup path: URL-decode + trim

**Problem:** Stored path is a raw content URI like `content://com.android.externalstorage.documents/tree/primary%3ADownloads%2Fmoneym-backup.zip`.

**Fix:** Add a `formatBackupPath(raw: String): String` private function in the screen file (or put in a companion/top-level in commonMain):

```kotlin
private fun formatBackupPath(raw: String): String {
    val decoded = raw
        .replace("%3A", ":").replace("%3a", ":")
        .replace("%2F", "/").replace("%2f", "/")
        .replace("%20", " ")
    val treeIdx = decoded.lastIndexOf("/tree/")
    val docIdx = decoded.lastIndexOf("/document/")
    val startIdx = when {
        treeIdx >= 0 -> treeIdx + 6
        docIdx >= 0 -> docIdx + 10
        else -> -1
    }
    val docId = if (startIdx >= 0) decoded.substring(startIdx) else decoded
    return docId.replace(":", "/")
}
```

This converts `primary%3ADownloads%2Fmoneym-backup.zip` → `primary/Downloads/moneym-backup.zip`.

Apply in the path display (line 308): `text = formatBackupPath(path)`

---

## Verification
- `./gradlew :composeApp:assembleDebug`
- GDrive section shows "Last backup: 2026-05-31 17:21" (not "just now")
- GDrive restore dialog has app theme + eye toggle on passphrase field
- GDrive backup-now spinner is green
- Local backup tap shows inline spinner in row (not full-screen overlay)
- Local backup path shows `primary/Downloads/.../moneym-backup.zip` (no `%` signs, no content:// prefix)

# Phase 6 — Themed dialogs + password redesign + optional encryption + immediate cloud (tasks 7+8+9)

## Key insight
Local backups are **already plaintext zip** (`DbBackupManager.export()` bytes saved
directly). Encryption only exists on the **remote/cloud** path
(`RemoteBackupManager` + `BackupCrypto` envelope). So "don't encrypt" and the
password dialog are entirely about the **cloud** flow.

## A. core/ui — `MmDialog.kt` (themed wrapper)
Reusable themed dialog mirroring `WalletSwitcherDialog` styling
(`containerColor = MM.colors.surface`, `titleContentColor = MM.colors.text`, title uses
`MM.type.title3`). Slots: `title: String`, `content: @Composable ColumnScope.() -> Unit`,
`confirmText`, `onConfirm`, `confirmEnabled`, `dismissText`, `onDismiss`. Confirm =
accent `TextButton`, dismiss = `text2`.

## B. core/ui — `MmField` password support
Add `visualTransformation: VisualTransformation = VisualTransformation.None` param,
pass to `BasicTextField`. (Show/hide toggle handled by caller via existing `suffix` slot
+ flipping the transformation.)

## C. Password dialog redesign (`BackupRestoreScreen.kt` PassphraseDialog)
Rebuild on `MmDialog`:
- Wording passphrase → **password**.
- Password `MmField` masked (`PasswordVisualTransformation`) + show/hide eye toggle in
  `suffix`.
- **Repeat password** `MmField` + match validation (local in composable; disable confirm
  on mismatch/short).
- Min length **4** (was 8).
- **"Don't encrypt"** `MmToggle` row. When ON → hide both password fields, show a
  plaintext-on-Drive **warning** (`MM.colors.danger`).
- Submit carries the encrypt choice.

## D. VM (`BackupRestoreViewModel.kt`)
- `MIN_PASSPHRASE_LENGTH` 8 → 4.
- Rename intent `PassphraseSubmitted(value)` → `PasswordSubmitted(value: CharArray,
  encrypt: Boolean)`; update `onIntent`.
- `handlePasswordSubmitted`: if `encrypt` → validate `value.size >= MIN`, set
  passphrase, `appSettings.putBoolean(REMOTE_BACKUP_ENCRYPT, true)`, enable, dialog off,
  `flushNow()`. If `!encrypt` → `putBoolean(REMOTE_BACKUP_ENCRYPT, false)`, no passphrase,
  enable, dialog off, `flushNow()`.
- `handleRemoteAutoToggled(true)`: show dialog only when not yet configured —
  `encrypt-pref absent OR (encrypt && !sessionPassphrase.isSet)`. Otherwise enable +
  `flushNow()`. Use `flushNow()` everywhere instead of `enqueueUpload()` (task 9 immediate).
- `handleRemoteBackupNow`: gate `encrypt && !isSet` → dialog else `flushNow()`.
- Add a private helper to launch `flushNow()` in `viewModelScope`.

## E. `RemoteBackupManager` optional encryption (auto-detect on read)
- New PrefKey `REMOTE_BACKUP_ENCRYPT = "pref.remote_backup_encrypt"` in
  `core/datastore/AppSettings.kt`.
- `encryptEnabled() = appSettings.getBoolean(PrefKeys.REMOTE_BACKUP_ENCRYPT, true)`.
- `enqueueUpload()` / `flushNow()`: gate becomes
  `if (encryptEnabled() && !sessionPassphrase.isSet.value) return`.
- `runUpload()`: if `encryptEnabled()` → existing encrypt+envelope path (.bin). Else →
  upload **raw plain zip bytes** with name `moneym-backup-$createdAt.zip`, properties
  `encrypted=false`; still record `LAST_REMOTE_BACKUP_TIME_MS`.
- `peekLatestMetadata()` / `restoreLatest()`: **sniff first byte** of downloaded bytes —
  `'{'` (0x7B) ⇒ encrypted envelope (decode/decrypt as today); `'P'`/0x50 (zip `PK`) ⇒
  plaintext ⇒ restore bytes directly (ignore passphrase). For plaintext metadata build a
  `RemoteBackupMetadata` from `ref` (createdAt=modifiedAtMs, appVersion="—",
  schema=CURRENT_SCHEMA, envelopeVersion=ENVELOPE_VERSION).
- `FakeAppSettingsRepository` parity: not needed (PrefKey on `AppSettings`, not the
  repository interface) — confirm. `RemoteBackupManager` already uses `AppSettings`.

## F. Disconnect dialog → `MmDialog` (`BackupRestoreScreen.kt` lines ~184-199)
Rebuild the raw `AlertDialog` on `MmDialog`.

## G. Strings (×4 langs: values, -de, -es, -it) in `feature/settings`
New keys: `settings_remote_password_title`, `_body`, `_label`, `_repeat_label`,
`_show`, `_hide`, `_save`, `_mismatch`, `_too_short`, `_dont_encrypt`,
`_plaintext_warning`. Reuse existing `settings_remote_passphrase_cancel` for cancel.
Min-length error text takes the number.

## Conventions
- Single `onIntent`; public VM. Import classes. No hardcoded user-visible strings.
- Encrypted path (`BackupCrypto`/envelope) stays byte-identical → existing crypto tests
  pass. Plaintext path adds a branch only.

## Build / verify
- `./gradlew :core:ui:compileDebugKotlinAndroid :core:datastore:compileDebugKotlinAndroid`
- `./gradlew :data:remotebackup:compileDebugKotlinAndroid :data:backup:compileDebugKotlinAndroid`
- `./gradlew :feature:settings:compileDebugKotlinAndroid :composeApp:compileDebugKotlinAndroid`
- `./gradlew :composeApp:assembleDebug`
- Final iOS link: `:composeApp:linkDebugFrameworkIosArm64 :composeApp:linkDebugFrameworkIosSimulatorArm64`

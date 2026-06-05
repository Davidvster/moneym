# Cloud sync

MoneyM's optional **Cloud sync** keeps the same dataset in step across a user's devices by exchanging
a single JSON snapshot through the user's Google Drive `appDataFolder`.

It is presented as **one feature with one toggle** (see `BackupRestoreScreen` → "Cloud sync"). With a
single device it behaves as a continuous cloud backup; when a second device joins, the same mechanism
becomes cross-device sync — there is no separate "remote auto-backup" switch. Enabling the toggle
turns on both `PrefKeys.CROSS_DEVICE_SYNC_ENABLED` (the merge engine) and
`PrefKeys.AUTO_REMOTE_BACKUP_ENABLED` (the periodic `.db-zip` snapshot-history safety net), and both
run off the cloud flag independently of the local *file* backup toggle.

Sync is off by default and gated on `PrefKeys.CROSS_DEVICE_SYNC_ENABLED`.

## Enabling / joining (same password on every device)

The enable flow inspects the remote first (`SyncBootstrap.remoteState()`):

- **No remote yet** (first device) → "create a password" (or choose plaintext); the local snapshot
  seeds the remote.
- **Encrypted remote exists** (joining device) → prompt for the **same password set on the other
  device**, validated by decrypting the remote (`SyncBootstrap.canDecrypt`) before sync is turned on;
  a wrong password shows an inline error. There is no "pick a new password" path when joining.
- **Plaintext remote** → a confirm dialog, no password.

The passphrase is **persisted in the platform `SecureStore`** (`SyncPassphraseStore`, iOS Keychain /
Android EncryptedSharedPreferences) and hydrated into `SessionPassphrase` at boot
(`AppInitializer`) before the first pull, so sync survives app restarts without re-prompting.

## Snapshot model

Each syncable row carries a stable, app-generated **`syncId`** (a UUID) that is identity across
devices — local autoincrement PKs are never shared. Deletes are **tombstones**: the row stays with
`deleted = true` and a bumped `updatedAt` so the deletion propagates; rows are never hard-deleted
while sync is in play. Foreign keys between snapshot rows are expressed **by `syncId`**, not by local
PK (e.g. a transaction references its category/account `syncId`).

The snapshot (`SyncSnapshot`) is a flat set of typed row lists: accounts, categories, payment modes,
transactions, recurring transactions, budgets. Each row records `syncId`, content, `deleted`,
`createdAt`, `updatedAt`.

## Pull → reconcile → apply → pending-deletions

`SyncEngine.pull()` runs under a mutex and:

1. Reads the remote snapshot bytes (`SyncRemoteStore`). **Empty/null remote → bootstrap**: push the
   local snapshot and return (first device seeds the remote).
2. Decodes (decrypts if encryption is on) into a remote `SyncSnapshot`.
3. Exports the local snapshot (`SyncExporter`) and runs `SyncReconciler.reconcile(local, remote)` —
   a pure, IO-free **last-write-wins by `syncId`** pass:
   - remote-only & not deleted → **add**
   - both present, remote newer (`updatedAt >`) & not deleted → **edit**
   - remote tombstone of a still-live local row → **pending deletion** (not applied immediately)
   - tie (`updatedAt ==`) → keep local
4. `SyncApplier.apply(...)` writes adds/edits via each repository's `upsertFromSync(...)`.
5. Pending deletions are written to a durable `PendingDeletionStore`; the user confirms or declines
   them (`resolveDeletions`). **Push is gated** while any pending deletion is unresolved, so a delete
   can't silently round-trip before the user has seen it.

Push (`push()` / `enqueuePush()`) seals the local snapshot and writes it to Drive.

## Triggers

`SyncEngine` is started/stopped by a dedicated `App` effect keyed on `CROSS_DEVICE_SYNC_ENABLED`,
**independent of the local file-backup lifecycle** (a past bug tied push to local auto-backup, so an
edit on device A never uploaded unless local backup happened to be on). `SyncEngine` owns its own
debounced data-change observer (a `combine` of all six repositories) and also pulses the snapshot
history.

- **Startup** — `AppInitializer.initialize()` hydrates the passphrase then calls `syncEngine.pullNow()`.
- **Foreground** — `AppLifecycleObserver` calls `pullNow()` on `ON_RESUME`.
- **On change** — local writes feed `enqueuePush()`, which **debounces** (`DEFAULT_DEBOUNCE_MS`,
  3 s) before pushing, coalescing bursts of edits into one upload.
- **Manual** — the transaction-list sync banner is tappable → a bottom sheet with last-synced time
  and a **Sync now** button (`SyncPuller.syncNow()` = pull then push).

## Deterministic seed syncIds (no double-seeding)

Both devices seed the same defaults independently on first run. If those seeds had random `syncId`s,
the first sync would treat them as distinct rows and **double** every default. To prevent this, seeds
use **stable, language-independent** `syncId`s derived from position/fixed key, never the localized
display name:

- **Categories** — `seed-category-$index` (index in `defaultCategorySpecs`), seeded via
  `CategoryRepository.upsertFromSync(...)` (idempotent by `syncId`).
- **Default account** — `seed-account-default`, via `AccountRepository.upsertFromSync(...)`. Two
  devices' default accounts merge into one; LWW picks the surviving name.
- **Payment modes** — `seed-paymentmode-cash` / `-card` / `-transfer`, set explicitly on the seeded
  `PaymentModeEntity`.

An EN device and a DE device therefore produce identical seed `syncId`s, so reconcile sees them as
the same rows (at most an LWW name edit), never new adds.

## Device registry

`DeviceRegistryManager` maintains `moneym-devices.json` so the app can show which devices participate
in sync and when each last synced. `SyncEngine` touches the current device's entry after each
successful pull/push.

## Encryption is shared state — mismatches surface as a conflict

The remote sync-state file is **self-describing**: an encrypted envelope and a plaintext snapshot are
both JSON, so `SyncSnapshotCodec.isEncryptedEnvelope()` distinguishes them by the envelope's required
fields. The remote's mode is treated as authoritative shared state — there is no way to silently end
up with two devices configured differently.

On `pull()`, when this device cannot reconcile its configuration with the remote, `SyncEngine` raises
a durable `SyncConflict` (stored in `SyncConflictStore`, surfaced in the tx-list banner) instead of
silently skipping, and **pauses both pull and push** until resolved — mirroring how pending deletions
are surfaced:

- remote encrypted + no/wrong password → `SyncConflict(remoteEncrypted = true)`
- remote plaintext + this device configured encrypted → `SyncConflict(remoteEncrypted = false)`

`SyncConflictController` offers three resolutions:

- **Enter the other device's password** — validated by decrypting the remote; a wrong password keeps
  the conflict.
- **Override with a new password** — re-encrypts the remote; other devices then hit the conflict and
  must re-enter (same model as a deletion override).
- **Use plaintext** — drops encryption; adopts a readable plaintext remote (pull/merge) or rewrites
  an unreadable one as the new plaintext remote.

A successful decrypt **adopts** the remote's mode. Network/parse failures still surface as
`SyncRuntimeState.Error`, not a false password conflict — only a decrypt/auth failure is a conflict.

The passphrase is persisted (see "Enabling / joining" above) and shared with `.db-zip` remote backup
via `SessionPassphrase` + `PrefKeys.REMOTE_BACKUP_ENCRYPT`.

## Files in Drive `appDataFolder`

- `moneym-sync-state.json` — the live sync snapshot.
- `moneym-devices.json` — the device registry.
- `moneym-backup-<ts>.bin` / `moneym-backup-<ts>.zip` — full `.db-zip` remote backups (separate from
  sync).

`RemoteBackupManager.pruneOldBackups()` filters strictly on the `moneym-backup` name prefix, so
retention pruning **never** deletes `moneym-sync-state.json` or `moneym-devices.json`.

## Known limitations

- **Clock-skew / LWW** — `updatedAt` is device wall-clock. Large skew can let a chronologically older
  edit win; ties keep the local row. A logical/vector clock is future work (out of scope).
- **Pre-sync `.db-zip` restore** — restoring a full `.db-zip` backup is disaster recovery, distinct
  from continuous sync. A backup taken before sync existed (or from an older schema) may yield rows
  with NULL or freshly-randomized `syncId`s; the Phase-1 migration backfill only runs on a schema
  upgrade, not on an already-migrated restored file. After such a restore, expect a one-time
  reconcile/dedup pass (the startup `pullNow()` runs after the restart-driven restore) or manual
  cleanup. No automatic post-restore pull is wired into `restoreLatest` because the restore swaps the
  live DB and relies on an app restart.

## Phase plans

The UX revisit (unified toggle, persistent passphrase, password-conflict model, lifecycle
decoupling, status sheet) is detailed under
[`plan/backup-sync-revisit/`](../plan/backup-sync-revisit/).

The original design and per-phase detail live under [`plan/backup-sync/`](../plan/backup-sync/):

- [Phase 1 — schema: syncId + tombstones](../plan/backup-sync/phase-1-plan.md)
- [Phase 2 — snapshot export/import](../plan/backup-sync/phase-2-plan.md)
- [Phase 3 — reconcile (LWW)](../plan/backup-sync/phase-3-plan.md)
- [Phase 4 — encryption / session passphrase](../plan/backup-sync/phase-4-plan.md)
- [Phase 5 — pending deletions](../plan/backup-sync/phase-5-plan.md)
- [Phase 6 — device registry](../plan/backup-sync/phase-6-plan.md)
- [Phase 7 — triggers / wiring](../plan/backup-sync/phase-7-plan.md)
- [Phase 8 — hardening + coexistence + docs](../plan/backup-sync/phase-8-plan.md)

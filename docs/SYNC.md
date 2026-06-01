# Multi-device sync

MoneyM's optional cross-device sync keeps the same dataset in step across a user's devices by
exchanging a single JSON snapshot through the user's Google Drive `appDataFolder`. It is a separate
mechanism from full `.db-zip` remote backup (disaster recovery); the two share the Drive connection
and passphrase but serve different purposes.

Sync is off by default and gated on `PrefKeys.CROSS_DEVICE_SYNC_ENABLED`.

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

- **Startup** — `AppInitializer.initialize()` calls `syncEngine.pullNow()` after seeding.
- **Foreground** — `AppLifecycleObserver` calls `pullNow()` on `ON_RESUME`.
- **On change** — local writes feed `enqueuePush()`, which **debounces** (`DEFAULT_DEBOUNCE_MS`,
  3 s) before pushing, coalescing bursts of edits into one upload.

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

## Encryption (shared with remote backup)

Sync reuses the remote-backup encryption setting and **session passphrase**:

- `PrefKeys.REMOTE_BACKUP_ENCRYPT` toggles encryption (default on).
- The passphrase lives in `SessionPassphrase` (in memory, set after unlock).
- When encryption is on but the session passphrase is **unset**, `SyncEngine` **skips and logs**
  both `pull` and `push` (no plaintext is ever written). Sync resumes once the passphrase is set.

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

The design and per-phase detail live under [`plan/backup-sync/`](../plan/backup-sync/):

- [Phase 1 — schema: syncId + tombstones](../plan/backup-sync/phase-1-plan.md)
- [Phase 2 — snapshot export/import](../plan/backup-sync/phase-2-plan.md)
- [Phase 3 — reconcile (LWW)](../plan/backup-sync/phase-3-plan.md)
- [Phase 4 — encryption / session passphrase](../plan/backup-sync/phase-4-plan.md)
- [Phase 5 — pending deletions](../plan/backup-sync/phase-5-plan.md)
- [Phase 6 — device registry](../plan/backup-sync/phase-6-plan.md)
- [Phase 7 — triggers / wiring](../plan/backup-sync/phase-7-plan.md)
- [Phase 8 — hardening + coexistence + docs](../plan/backup-sync/phase-8-plan.md)

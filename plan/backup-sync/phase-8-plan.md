# Phase 8 — Hardening + coexistence + docs

**Status: DONE** — deterministic seed syncIds (categories index, account+paymentmodes fixed keys) so defaults dedup cross-device; fresh-install guard kept (no resurrect/rename-revert); bootstrap/encrypt-parity/prune-safety verified; docs/SYNC.md added. Seed + seed-merge tests green.

## Goal
Close the multi-device edge cases and document the system. The headline fix is **deterministic seed syncIds** so default categories / payment modes / the default account don't duplicate when a second device first syncs. Plus: verify/document first-sync bootstrap, `.db-zip` restore coexistence, encryption parity, prune safety; add `docs/SYNC.md`.

## 1. Deterministic seed syncIds (the important fix)
Both devices seed the same defaults independently with currently-random syncIds → after first sync they double. Fix by seeding with **stable, non-localized** syncIds so reconcile treats them as the same row.

- **SeedCategoriesUseCase** (`data/categories`): instead of `repository.insert(spec.toCategory(name))`, call `repository.upsertFromSync(CategorySyncRow(...))` with `syncId = "seed-category-$index"` (index in `defaultCategorySpecs` — stable across devices, language-independent), content from spec + localized `name`, `isUserCreated=false`, `archived=false`, `deleted=false`, timestamps = `now`. `upsertFromSync` is idempotent by syncId, so the `count()==0` guard can stay or go.
- **SeedAccountsUseCase** (`data/accounts`): seed the default account via `repository.upsertFromSync(AccountSyncRow(syncId = "seed-account-default", …, isDefault=true, …))`. Fixed syncId → both devices' default accounts merge into one (LWW picks a name; fine).
- **SeedPaymentModesUseCase** (`data/transactions`, DAO-direct): set explicit `syncId` on each seeded `PaymentModeEntity` — `"seed-paymentmode-cash"`, `"-card"`, `"-transfer"`. (No upsert needed; the `countAll()==0` guard prevents re-seed; cross-device dedup is by the shared syncId.)
- Rationale: keys derive from stable attributes (list index / fixed name), never the localized display string, so an EN device and a DE device produce identical seed syncIds.

## 2. First-sync bootstrap — verify (no code if already correct)
`SyncEngine.pull()` already: remote empty/null → push local seed. Confirm a new device with an existing remote (local seeded, remote has data) merges by syncId (seeds dedup via §1; user data adds). Add/confirm a test.

## 3. `.db-zip` restore coexistence — document (+ optional guard)
Restoring a **pre-sync** `.db-zip` backup yields rows with NULL syncId → the Phase-1 migration backfill only runs on schema upgrade, not on a restored already-migrated file, so those rows may stay NULL or (if from an older schema) get fresh random syncIds → duplicates on next sync. **Document** this in `docs/SYNC.md` as a known limitation (full `.db-zip` restore = disaster recovery, distinct from continuous sync; after restoring an old backup, expect a one-time reconcile/dedup pass or manual cleanup). No core behavior change required; if cheap, have `RemoteBackupManager.restoreLatest`/local restore trigger `syncEngine.pullNow()` afterward so a reconcile runs — optional, only if it doesn't complicate the restart flow.

## 4. Encryption parity — verify + document
Confirm `SyncEngine` skips (logs) when `REMOTE_BACKUP_ENCRYPT` is on but `SessionPassphrase` is unset (added Phase 4). Document that sync shares the remote-backup passphrase/session.

## 5. Prune safety — verify
Confirm `RemoteBackupManager.pruneOldBackups()` filters `name.startsWith("moneym-backup")` and therefore never deletes `moneym-sync-state.json` / `moneym-devices.json`. Document.

## 6. `docs/SYNC.md`
Architecture doc: snapshot model (syncId + tombstones, FK-by-syncId), the pull→reconcile(LWW)→apply→pending-deletions flow, triggers (startup / on-change debounce / foreground), device registry, encryption sharing with remote backup, file names in Drive appDataFolder, and the known limitations (clock-skew LWW, pre-sync `.db-zip` restore). Keep it concise; link the phase plans under `plan/backup-sync/`.

## 7. Clock-skew / LWW — document
`updatedAt` is device wall-clock; large skew can let an older edit win. Tie = keep local. Note a logical clock is future work (out of scope).

## Tests
- `SeedCategoriesUseCaseTest` (+ accounts/paymentmodes if they have seed tests): seeded rows have the deterministic syncIds; re-running seed is idempotent (no dup).
- A `data/sync` seed-merge test: two independently-seeded local snapshots (simulating two devices) reconcile to **no** new category/account/paymentMode adds (same syncIds) — proves no doubling.
- First-sync bootstrap test (extend `SyncEngineTest` if not already covered).

## Verification
- `./gradlew :data:categories:compileDebugKotlinAndroid :data:accounts:compileDebugKotlinAndroid :data:transactions:compileDebugKotlinAndroid :data:sync:compileDebugKotlinAndroid`
- `./gradlew :composeApp:assembleDebug :composeApp:linkDebugFrameworkIosSimulatorArm64`
- `./gradlew :data:categories:testDebugUnitTest :data:accounts:testDebugUnitTest :data:transactions:testDebugUnitTest :data:sync:testDebugUnitTest`
- Full sanity: `./gradlew testDebugUnitTest` (note the one known-unrelated `:data:budgets` FakeBudgetRepositoryTest failure remains).

## Notes
- Deterministic seed syncIds must be language-independent (index / fixed key) — NOT the localized name.
- Keep changes minimal; §3/§4/§5/§7 are mostly verify+document.

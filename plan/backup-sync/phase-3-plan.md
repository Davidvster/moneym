# Phase 3 — Snapshot export from local DB

**Status: DONE** — 6 *SyncRow types + exportForSync (dao/datasource/repo/mapper/fake), SyncExporter builds PK->syncId maps + FK rewrite + skip-on-unresolvable. SyncExporterTest green. All builds pass.

## Goal
Produce a `SyncSnapshot` (Phase-2 DTO) from current local state — **including tombstoned rows** — with all FKs expressed as syncIds. Read-only; no triggers, no upload wired automatically. A `SyncExporter` in `data/sync` reads each repo's sync rows, builds per-table `Long PK → syncId` maps, and rewrites FK Long ids to syncId strings.

## Design
- Each `data/*` repo gains a one-shot sync-read returning lightweight public **`*SyncRow`** types (NOT Room entities, to keep `data/sync` decoupled from Room). Rows carry: `id: Long` (PK, for map building), `syncId: String?`, the content fields, **FKs as Long**, `deleted: Boolean`, `createdAt`, `updatedAt`.
- `SyncExporter` (in `data/sync`, which now depends on the repo modules) consumes the rows, builds `Map<Long, String>` per table (PK→syncId), and emits the `SyncSnapshot` with FK syncIds resolved. Keeps FK-resolution logic out of the individual data modules.
- Normal app reads are **not** changed here. (Tombstone filtering on normal queries is a Phase-5 concern, when deletes first appear. The export path deliberately includes deleted rows.)

## Per-module changes (apply to accounts, categories, transactions[×3: tx, recurring, paymentMode], budgets)
For each module:
1. **`*SyncRow` public data class** (new file, e.g. `data/accounts/.../AccountSyncRow.kt`), shapes below.
2. **DAO**: add `@Query("SELECT * FROM <Table>") suspend fun selectAllForSync(): List<XxxEntity>` (no deleted filter — include tombstones).
3. **LocalDataSource** interface + impl: add `suspend fun exportForSync(): List<XxxEntity>` delegating to the DAO.
4. **Repository** interface + impl: add `suspend fun exportForSync(): List<XxxSyncRow>` mapping entity→row (entity already has `syncId`/`deleted` from Phase 1).
5. **Fake** (core/testing): override `exportForSync()`. Fakes store domain models without syncId → synthesize a deterministic syncId from the Long id (e.g. `"sync-acc-$id"`), `deleted = false`. Enough for FK-resolution tests.

### SyncRow shapes (mirror entity fields; FKs stay Long)
```
AccountSyncRow(id, syncId, name, type, currency, isDefault, archived, colorHex, deleted, createdAt, updatedAt)
CategorySyncRow(id, syncId, name, iconKey, colorHex, isUserCreated, archived, categoryType, deleted, createdAt, updatedAt)
PaymentModeSyncRow(id, syncId, name, deleted, createdAt, updatedAt)
TransactionSyncRow(id, syncId, type, amountMinor, currency, occurredOn, note?, categoryId, accountId, paymentModeId?, recurringId?, deleted, createdAt, updatedAt)
RecurringSyncRow(id, syncId, type, amountMinor, currency, note?, categoryId, accountId, paymentModeId?, startDate, freqUnit, freqInterval, dayOfWeek?, dayOfMonth?, useLastDay, endKind, endCount?, endDate?, lastMaterializedDate?, deleted, createdAt, updatedAt)
BudgetSyncRow(id, syncId, name, amountMinor, currency, categoryId?, accountId, periodType, startYearMonth, recurringMonths?, deleted, createdAt, updatedAt)
```
(Field set matches each entity exactly, minus Room annotations and minus the legacy unused `recurrenceRule` column on TransactionEntity.)

## `data/sync` module
- `build.gradle.kts` commonMain: ADD `projects.data.accounts`, `projects.data.categories`, `projects.data.transactions`, `projects.data.budgets`.
- New `data/sync/.../SyncExporter.kt`:
  - Constructor: `SyncExporter(accountRepository, categoryRepository, paymentModeRepository, transactionRepository, recurringTransactionRepository, budgetRepository, deviceIdentity)`.
  - `suspend fun export(): SyncSnapshot`:
    1. Fetch all 6 `exportForSync()` lists.
    2. Build `accMap`, `catMap`, `pmMap`, `recMap` = `id -> syncId` (skip rows whose `syncId == null` — log + drop defensively; post-backfill there should be none).
    3. Map each row to its Sync DTO, resolving FK Long → syncId via the maps. A row whose required FK syncId can't be resolved (missing parent) is **skipped** (log). Nullable FKs (paymentMode, recurring, budget.category) resolve to null when absent.
    4. Return `SyncSnapshot(formatVersion=1, generatedAtMs=now, originDeviceId=deviceIdentity.deviceId(), <lists>)`.
- Register `SyncExporter` in `composeApp/.../di/SyncModule.kt` (`single { SyncExporter(get(), get(), get(), get(), get(), get(), get()) }`).

## Repo interface reference (current methods to extend)
- `AccountRepository`, `CategoryRepository`, `BudgetRepository`, `RecurringTransactionRepository`, `PaymentModeRepository`, `TransactionRepository` — add `suspend fun exportForSync(): List<XxxSyncRow>` to each.
- Note: `PaymentModeRepository` may have **no** Fake in core/testing today. If `SyncExporterTest` needs one, create a minimal fake in the test source set (don't add a production fake unless other tests need it).

## Tests (commonTest in data/sync)
`SyncExporterTest` with the 6 repo fakes (use core/testing fakes; for PaymentMode use an in-test fake) seeded with linked data:
- A transaction's `categorySyncId`/`accountSyncId` equal the seeded category/account syncIds (FK resolution correct).
- Tombstoned rows (`deleted=true`) ARE included in the snapshot with `deleted=true`. (Fakes default deleted=false; add a way to seed a deleted row, or assert non-deleted inclusion + leave deleted-path to a row the fake marks. If fakes can't represent deleted, at minimum assert all live rows export and FKs resolve; note the gap.)
- A transaction referencing a missing category id is skipped (no crash).
- `originDeviceId` is populated from DeviceIdentity.

## Verification
- `./gradlew :data:accounts:compileDebugKotlinAndroid :data:categories:compileDebugKotlinAndroid :data:transactions:compileDebugKotlinAndroid :data:budgets:compileDebugKotlinAndroid :data:sync:compileDebugKotlinAndroid`
- `./gradlew :composeApp:assembleDebug`
- `./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64`
- `./gradlew :data:sync:testDebugUnitTest`
- Per-module unit tests for any module whose Fake changed: `./gradlew :feature:...:testDebugUnitTest` as needed (fake parity).

## Notes
- Known-unrelated pre-existing `:data:budgets` `FakeBudgetRepositoryTest` failure — ignore, don't fix.
- Keep `data/sync` → repo dependency one-directional (repos must NOT depend on data/sync).

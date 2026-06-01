# Phase 1 — `sync_id` + `deleted` on all 6 tables

**Status: DONE** — all 6 tables migrated (cat 1→2, acc 2→3, txn 2→3, budgets 2→3), syncId generated on insert, `randomblob` backfill in migration SQL. assembleDebug + iosSimulatorArm64 link + per-module tests green. Recurring update carries syncId/deleted over (its upsert rebuilds the entity). Pre-existing unrelated `FakeBudgetRepositoryTest` failure (missing accountId arg) left untouched.

## Goal
Add `sync_id TEXT` (nullable, opaque UUID) + `deleted INTEGER NOT NULL DEFAULT 0` to all 6 entity tables via Room migrations. Existing rows get a unique `sync_id` backfilled **inside the migration SQL** (`lower(hex(randomblob(16)))`). New rows get a `Uuid.random().toString()` generated in the DataSource `insert` impl. **No sync behavior, no domain-model change, no repo/fake/interface change** — app behaves identically; columns just exist and stay populated.

### Why this footprint (deviation from master plan, same end state)
Master plan proposed a `BackfillSyncIdsUseCase` run from `AppInitializer`. Replaced with **migration-embedded `randomblob` backfill** + **syncId generated inside DataSource impl** because:
- No `data/sync` module exists yet (it's Phase 2), so the use case had nowhere clean to live.
- Avoids touching repo interfaces, repo impls, all 6 fakes, AppInitializer, and domain models.
- syncId is a pure persistence concern → belongs at entity/DataSource layer.
End state identical: every row has a unique syncId; idempotency is free (migration runs once; fresh installs skip it).

## Tables / versions
| Module | DB file | table(s) | version | migration |
|---|---|---|---|---|
| categories | moneym_categories.db | Category | 1 → 2 | NEW `MIGRATION_1_2` (no migration file today) |
| accounts | moneym_accounts.db | Account | 2 → 3 | NEW `MIGRATION_ACCOUNTS_2_3` in `AccountMigrations.kt` |
| transactions | moneym_transactions.db | TransactionEntry, PaymentMode, RecurringTransactionEntry | 2 → 3 | NEW `MIGRATION_2_3` (companion style, all 3 tables) |
| budgets | moneym_budgets.db | Budget | 2 → 3 | NEW `MIGRATION_2_3` (companion style) |

## Per-table changes (apply identically to all 6 entities)
1. **Entity** — add two columns (place at end, with defaults so named-arg construction elsewhere is unaffected):
   ```kotlin
   @ColumnInfo(name = "sync_id") val syncId: String? = null,
   @ColumnInfo(name = "deleted", defaultValue = "0") val deleted: Boolean = false,
   ```
   Files: `CategoryEntity.kt`, `AccountEntity.kt`, `BudgetEntity.kt`, `TransactionEntity.kt`, `PaymentModeEntity.kt`, `RecurringTransactionEntity.kt`.

2. **Migration SQL** — for each table:
   ```sql
   ALTER TABLE <Table> ADD COLUMN sync_id TEXT;
   ALTER TABLE <Table> ADD COLUMN deleted INTEGER NOT NULL DEFAULT 0;
   UPDATE <Table> SET sync_id = lower(hex(randomblob(16))) WHERE sync_id IS NULL;
   ```
   (Separate `connection.execSQL("...")` calls, one per statement — `randomblob()` evaluates per-row in the UPDATE giving each row a distinct id.)
   - **Room schema match is critical**: entity `deleted` has `defaultValue = "0"` → column MUST be `INTEGER NOT NULL DEFAULT 0`. `sync_id` is nullable `String?` with no defaultValue → `TEXT` nullable. Mismatch = runtime `IllegalStateException` on open.

3. **DB version bump** — bump `@Database(version = N)` in the 4 `*RoomDatabase.kt`.

4. **Register migration in BOTH platform factories** (8 files) — add the new migration to the existing `.addMigrations(...)` call (keep the old ones):
   - accounts: `.addMigrations(MIGRATION_ACCOUNTS_1_2, MIGRATION_ACCOUNTS_2_3)`
   - budgets: `.addMigrations(BudgetsRoomDatabase.MIGRATION_1_2, BudgetsRoomDatabase.MIGRATION_2_3)`
   - transactions: `.addMigrations(TransactionsRoomDatabase.MIGRATION_1_2, TransactionsRoomDatabase.MIGRATION_2_3)`
   - categories: `.addMigrations(CategoriesRoomDatabase.MIGRATION_1_2)` (none today → add the call to both factories).

5. **Generate syncId for new inserts in DataSource impl** — in each `insert(...)` that constructs the entity, set `syncId = Uuid.random().toString()` (`import kotlin.uuid.Uuid`; add `@OptIn(kotlin.uuid.ExperimentalUuidApi::class)` on the function/class only if the 2.3.21 compiler flags it). `deleted` keeps its `false` default.
   - Files: `SqlDelightAccountDataSource.kt`, `SqlDelightCategoryDataSource.kt`, `SqlDelightTransactionDataSource.kt`, `SqlDelightPaymentModeDataSource.kt`, `RoomBudgetDataSource.kt`, and the recurring insert (in `SqlDelightTransactionDataSource.kt` or its own — locate the recurring `insert`).
   - **Update path:** existing `update(...)` impls do `dao.selectById(id).copy(...)` → syncId is preserved automatically (it's not in the copy's named args). Verify each update uses copy-from-existing; if any rebuilds the entity from scratch, carry `syncId`/`deleted` over.

## Style references (mirror exactly)
- Migration top-level val: `data/accounts/.../db/AccountMigrations.kt` (`MIGRATION_ACCOUNTS_1_2`).
- Migration companion style: read `TransactionsRoomDatabase.kt` / `BudgetsRoomDatabase.kt` `MIGRATION_1_2` to match the companion-object placement.
- Factory android+ios: `AccountsDatabaseFactory.kt` (both source sets).
- DataSource insert: `SqlDelightAccountDataSource.insert` (entity construction site).

## Tests
Add a Room migration test per DB (run on Android unit test source set where the bundled driver + `MigrationTestHelper` or a manual in-memory open works; if the project has no `MigrationTestHelper` setup, write a minimal test that opens a v_old DB, applies the migration via the builder, inserts/reads a row, and asserts `sync_id` non-null + `deleted = 0`). At minimum:
- `data/accounts` — open v2 with a seeded Account, run `MIGRATION_ACCOUNTS_2_3`, assert column present, old row intact, `sync_id` populated (non-null, unique), `deleted = 0`.
- Repeat for categories (1→2), transactions (2→3, check all 3 tables), budgets (2→3).
Follow the `testing` skill conventions. If migration-test infra doesn't exist and is heavy to add, at minimum add a DataSource-level test asserting a freshly inserted row has a non-null `syncId`.

## Verification
- `./gradlew :composeApp:assembleDebug`
- `./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64`
- `./gradlew testDebugUnitTest`
- App launches with pre-existing data intact; every row has a syncId.

## Files touched (summary)
- 6 entities, 4 `*RoomDatabase.kt` (version bump + migration), `AccountMigrations.kt`, 8 factory files, 6 DataSource impls, + migration tests.
- **NOT touched:** repo interfaces, repo impls (except possible update-preserve check), fakes, domain models, AppInitializer, DI.

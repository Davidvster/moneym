# Phase 3 — Repository-impl tests (in-memory fake datasource)

Test the 5 repo impls by faking their single dependency interface with an in-memory implementation, then asserting orchestration (entity↔domain mapping, soft-delete, sync export/import). All deps are interfaces → fully testable from `commonTest`. Tests + fakes go in each module's `src/commonTest/kotlin/...` SAME package as the impl (impls are `internal`).

**Determinism**: every impl calls `kotlin.time.Clock.System.now()` directly. Do NOT assert exact timestamps — assert structure/identity/relationships, and for sync-row timestamps assert "non-null / > 0" only.

## Targets

1. `AccountRepositoryImpl` (dep: `AccountLocalDataSource` interface) — `data/accounts`.
2. `BudgetRepositoryImpl` (dep: `BudgetLocalDataSource`) — `data/budgets`.
3. `CategoryRepositoryImpl` (dep: `CategoryLocalDataSource`) — `data/categories`.
4. `TransactionRepositoryImpl` (dep: `TransactionLocalDataSource`) — `data/transactions`.
5. `RecurringTransactionRepositoryImpl` (dep: `RecurringTransactionDao` — `@Dao interface` in `db/RecurringTransactionDao.kt`) — `data/transactions`.

## Fakes
For each dependency interface write an in-memory `Fake<X>` implementing ALL interface methods, backed by a `MutableStateFlow<List<Entity>>` (+ an autoincrement `Long` id counter). `observe*` returns `flow.map { filter }`; `insert` appends with next id and returns it; `update`/`softDelete`/`markDeletedBySyncId`/`reviveBySyncId` mutate by id/syncId; `exportForSync`/`selectAllForSync` returns current list; `upsertFromSync` inserts-or-updates by syncId. Keep fakes in the test source set (not production). Reuse `core:testing` Fake*Repository ONLY if it already wraps the DS — it does not, so write new DS-level fakes here.

## Coverage per impl (behavior-complete)
- `observeAll`/`observeActive`/`observeDefault`/`observeByMonth`/`observeByAccount`/`observeByCategory`/`observeByType` → returns mapped domain objects; reflects fake updates (use Turbine `app.cash.turbine.test`).
- `getById` hit + miss (null).
- `insert` → returns typed id; entity persisted with mapped fields.
- `update` → fields propagated.
- `delete`/`softDelete*` → row tombstoned (verify via export or a getById-after).
- Sync paths: `exportForSync` maps entities→sync rows; `upsertFromSync`/`markDeletedBySyncId`/`reviveBySyncId` round-trip.
- Transaction-specific: `convertCurrencyForAccount`, `getEarliest/LatestDate`, `getDistinctTransactionDates`, `countByRecurringId`, `softDeleteByAccountId`.
- Recurring-specific: `updateCursor`/`touchBySyncId` and the cursor/materialization fields.

Check each `*Repository` interface for the exact public methods to exercise; cover every one that the impl overrides with logic (skip trivial 1-line delegations only if identical to a covered path).

## Test harness
Use `kotlin.test` + `app.cash.turbine` (already a commonTest dep in these modules — verify; if a module's `commonTest` lacks turbine, add `implementation(libs.turbine)` to its `build.gradle.kts` commonTest deps). Use `runTest` from `kotlinx-coroutines-test` (via `core:testing` `runTestWithDispatchers` or directly). Build fixtures from `core:model` types; mirror `data/transactions/src/commonTest/.../TransactionRepositoryTest.kt`.

## Verify
```
./gradlew :data:accounts:testDebugUnitTest :data:budgets:testDebugUnitTest \
  :data:categories:testDebugUnitTest :data:transactions:testDebugUnitTest
```
All green. Do not modify production code (only add test sources). If a `build.gradle.kts` needs `libs.turbine` in commonTest, that's the only allowed production-tree edit.

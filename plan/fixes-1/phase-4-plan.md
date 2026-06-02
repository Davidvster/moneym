# Phase 4 — Datasource tests (real in-memory Room)

Test the 5 Room-backed datasource impls against a REAL in-memory Room DB built in `commonTest`. The project uses Room KMP with `BundledSQLiteDriver` (`libs.sqlite.bundled`, already a commonMain dep), which runs on both the Android unit-test JVM host and native iOS test targets — **no Robolectric/Android Context needed**.

## Datasource impls → DB
- `SqlDelightAccountDataSource(db)` — `AccountsRoomDatabase` (`data/accounts`)
- `RoomBudgetDataSource(db)` — `BudgetsRoomDatabase` (`data/budgets`)
- `SqlDelightCategoryDataSource(db)` — `CategoriesRoomDatabase` (`data/categories`)
- `SqlDelightTransactionDataSource(db)` + `SqlDelightPaymentModeDataSource(db)` — `TransactionsRoomDatabase` (`data/transactions`)

## In-memory DB builder (commonTest)
Each DB class is `@ConstructedBy(<X>RoomDatabaseConstructor::class)` with an expect/actual constructor object → the reified KMP builder works in common:
```kotlin
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers

fun newDb(): AccountsRoomDatabase =
    Room.inMemoryDatabaseBuilder<AccountsRoomDatabase>()
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.Default)   // not Dispatchers.IO — no IO on native
        .build()
```
Use `Dispatchers.Default` (or the test dispatcher) for the query context. Close the DB after each test (`db.close()` in `@AfterTest`). Wrap a small `newDb()` helper per module in the test source set, same package as the impl (impl is `internal`).

**Feasibility gate**: first verify `Room.inMemoryDatabaseBuilder<T>()` resolves and a trivial insert/select round-trips for ONE module (accounts). If it compiles + runs on `:data:accounts:testDebugUnitTest`, proceed to all. If the reified builder is NOT resolvable from `commonTest` (e.g. constructor actual missing for the test target), STOP, document the exact blocker in status.md, and skip the rest — the datasources stay covered indirectly by Phase 3's repo-impl tests. Do not chase it past one failed approach.

## Coverage (behavior-complete, per datasource)
Exercise every interface method through the real DAO:
- `observe*` Flows emit current rows + react to writes (Turbine).
- `insert` returns rowid; row materializes; `getById` hit/miss.
- `update` mutates; `count`.
- `softDelete`/`softDeleteByAccountId` set the tombstone column (verify the row is excluded from `observeActive`/`observeAll` per the DAO's WHERE clause, and present in `exportForSync`).
- `markDeletedBySyncId`/`reviveBySyncId`/`touchBySyncId` by syncId.
- `exportForSync` returns all (incl. tombstoned) rows; `upsertFromSync` insert-vs-update by syncId.
- `deleteAll`.
- Transaction DS specifics: `observeByMonth`/`observeByCategory`/`observeByType`/`observeByCategoryAndType` filtering, `convertCurrencyForAccount`, `getEarliest/LatestDate`, `getDistinctTransactionDates`, `countByRecurringId`.
- PaymentMode DS: its full surface.

Build fixtures from the `*Entity` constructors directly (these tests own raw entities, not domain models).

## Verify
```
./gradlew :data:accounts:testDebugUnitTest :data:budgets:testDebugUnitTest \
  :data:categories:testDebugUnitTest :data:transactions:testDebugUnitTest
```
Green. If the feasibility gate fails, leave a clear note and the partial accounts attempt removed/reverted so the build stays green. Test sources only — no production edits (if a module's commonTest lacks `libs.sqlite.bundled` on the test classpath it inherits it from commonMain `implementation`; only add a test dep if compilation proves it missing).

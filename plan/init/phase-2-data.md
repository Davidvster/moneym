# Phase 2 — Data layer

**Status**: ✅ Done

Goal: stand up the persistence layer (categories, accounts, transactions). Repositories return `Flow<T>`, write paths emit through SQLDelight, and seed UseCases populate defaults on first run.

**Exit criteria met**: all repository tests pass on Android and iOS, APK and iOS framework build, app boots and seeds 11 default categories + 1 default "Main" account.

## Steps

- [x] **2.1** `data:categories` — `Category.sq` schema, `CategoryRepository` interface, `SqlDelightCategoryDataSource`, `CategoryRepositoryImpl`, `CategoryMappers`, `CategoryRepositoryFactory`
- [x] **2.2** `data:categories` — `SeedCategoriesUseCase` (idempotent) + `DefaultCategories` (11 seed entries)
- [x] **2.3** `data:accounts` — `Account.sq` schema, `AccountRepository`, DataSource, Impl, Mappers, Factory
- [x] **2.4** `data:accounts` — `SeedAccountsUseCase` (single "Main" CASH/EUR default account)
- [x] **2.5** `data:transactions` — `TransactionEntry.sq` schema with `recurrence_rule` reservation and 3 indexes
- [x] **2.6** `data:transactions` — `TransactionRepository` + DataSource + Impl + Mappers + Factory; queries: observe-all, by-month, filtered (by category / type / both), get-by-id, upsert, delete
- [x] **2.7** Fakes in `core:testing`: `FakeCategoryRepository`, `FakeAccountRepository`, `FakeTransactionRepository`
- [x] **2.8** Tests: `SeedCategoriesUseCaseTest`, `SeedAccountsUseCaseTest`, `TransactionRepositoryTest` (all pass on Android + iOS)
- [x] **2.9** SQLDelight migration test scaffold — deferred; schema is v1 with no prior versions, migrate test scaffold added in Phase 8
- [x] **2.10** Koin modules: `dataCategoriesModule`, `dataAccountsModule`, `dataTransactionsModule` in `composeApp/di/DataModules.kt`; platform modules `androidPlatformModule` / `iosPlatformModule` wire `SqlDriverFactory`
- [x] **2.11** `App()` seeds categories and accounts via `LaunchedEffect` after Koin starts; `MainActivity` and `MainViewController` pass platform modules
- [x] **2.12** Verified: `assembleDebug` ✅ · iOS framework ✅ · `testDebugUnitTest` ✅ · `iosSimulatorArm64Test` ✅

## Issues resolved during Phase 2

| Issue | Fix |
|---|---|
| SQLDelight `.sq` files need package-matching directory structure | Moved to `sqldelight/com/dv/moneym/data/<module>/` |
| Generated row types in `com.dv.moneym.data.*`, not `.db` sub-package | Fixed all imports in DataSource/Mapper files |
| `internal` classes from data modules not accessible in `composeApp` | Added public `createXxxRepository()` factory functions per module |
| `kotlinx-datetime` not transitively exposed from `core:model` | Added explicit dep to each data module |
| `kotlin.test` not reaching data module `commonTest` via `core:testing` | Added explicit `implementation(libs.kotlin.test)` to each data module's `commonTest` |

# Phase 6 — UseCase tests

Behavior-complete unit tests for 12 use cases. Use cases are plain classes with constructor-injected deps and no Compose/Flow inside their bodies → straightforward. Tests in each module's `commonTest`, same package as the use case. Read each `*UseCase.kt` ctor + its `operator fun invoke(...)`/public method before writing; build fixtures from `core:model`. Use `core:testing` fakes for repo deps, `FixedClock` for clocks, `runTest`/`runTestWithDispatchers` for suspend funcs. Cover every branch, boundary, and empty/edge input. Existing covered use-cases (BuildFinanceSnapshot, BuildBudgetProgress, ResolvePeriodRange, ComputeCategoryBudgetRemaining, SuggestNotes) show the style — match it.

Add `libs.turbine`/`libs.kotlinx.coroutines.test`/`libs.kotlin.test` to a module's commonTest deps only if missing (test-only, allowed).

## Targets
1. `feature/aianalysis/.../usecase/BuildFinanceToolsetUseCase` — construct with its 5 fake deps; assert the toolset/tool definitions built for a (year, month); cover empty-data vs populated.
2. `feature/categories/.../domain/ArchiveCategoryUseCase` — fake `CategoryRepository`; assert archive flips flag/calls repo; cover already-archived / missing id.
3. `feature/overview/.../usecase/BuildCategoryBreakdownUseCase` — pure: assert per-category totals, percentages, sort order, empty input, zero-total guard.
4. `feature/overview/.../usecase/BuildCategoryTrendsUseCase` — assert trend series across periods, missing months filled, empty.
5. `feature/overview/.../usecase/BuildCumulativeSeriesUseCase` — assert running cumulative sums, ordering, empty.
6. `feature/overview/.../usecase/BuildOverviewPageStateUseCase` — construct with fake repos; assert composed page state per `OverviewPeriod`; empty + populated.
7. `feature/settings/.../importdata/usecase/PrepareImportPreviewUseCase` — assert parsed preview rows, category mapping, malformed/empty CSV branches, both `CsvSourceFormat` values if handled.
8. `feature/transactionEdit/.../domain/GetTransactionUseCase` — fake repo; hit + miss (null).
9. `feature/transactionEdit/.../domain/UpsertTransactionUseCase` — insert vs update path; returns id; persists.
10. `feature/transactionEdit/.../domain/DeleteTransactionUseCase` — deletes by id; verify repo call.
11. `feature/transactionEdit/.../usecase/ValidateAndBuildTransactionUseCase` — valid build; each validation failure (zero/negative amount, missing category/account, etc.) → typed error/result.
12. `data/transactions/.../SeedPaymentModesUseCase` — fake `PaymentModeRepository` (write local fake if none in scope); seeds defaults only when empty; idempotent on re-run.

## Verify
```
./gradlew :feature:aianalysis:testDebugUnitTest :feature:categories:testDebugUnitTest \
  :feature:overview:testDebugUnitTest :feature:settings:testDebugUnitTest \
  :feature:transactionEdit:testDebugUnitTest :data:transactions:testDebugUnitTest
```
Green. Test sources only (except allowed commonTest build.gradle deps). Import classes, no FQN, no comments unless non-obvious.

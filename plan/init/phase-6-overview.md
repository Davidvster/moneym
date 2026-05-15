# Phase 6 — Overview / analytics

**Status**: 📝 Sketch (expand when starting)

Goal: analytics dashboard with charts and summaries (daily/monthly/yearly), grouped-by-category breakdowns, and a trend view.

**Exit criteria**: charts render correctly with realistic test data; empty-state and short-data handling does not break charts; color usage stays restricted to category colors per `theming.md`; both themes look clean.

## Steps (will expand when starting)

- [ ] **6.1** `feature:overview` — `OverviewScreen` + ViewModel + UiState
- [ ] **6.2** Period selector (day / month / year)
- [ ] **6.3** Mixed-currency handling — group totals by currency when more than one currency is present in the range (ADR-006)
- [ ] **6.4** UseCases: `SummariseByDay`, `SummariseByMonth`, `SummariseByYear`, `SummariseByCategory`, `IncomeVsExpense`, `Trend`
- [ ] **6.5** Chart wrappers in `feature:overview` (NOT in `core:ui`):
  - [ ] **6.5.1** `SpendingDonut` — donut by category for the selected period
  - [ ] **6.5.2** `IncomeExpenseBar` — bar chart of income vs expense per day/month
  - [ ] **6.5.3** `TrendLine` — line chart over time
- [ ] **6.6** Empty state and ≤1-day-of-data handling (friendly message; no broken axes)
- [ ] **6.7** Tappable chart segments → drill into filtered transaction list
- [ ] **6.8** Strings for en/es/it/de
- [ ] **6.9** ViewModel tests with fake repositories returning fixture transaction data
- [ ] **6.10** Manual visual check on Android + iOS, light + dark

---

## Concrete implementation notes (for fresh sessions)

### What already exists

**`feature:overview`** skeleton module exists — `feature/overview/src/commonMain/.../` dirs created, empty.

**`data:transactions`** has all queries needed for aggregation:
- `TransactionRepository.observeAll()` → `Flow<List<Transaction>>`
- `TransactionRepository.observeByMonth(year, month)` → `Flow<List<Transaction>>`
- `TransactionRepository.observeFiltered(filter)` → `Flow<List<Transaction>>`

**`data:categories`** and **`data:accounts`** repos fully functional. Feature module can combine flows from all three.

### compose-charts version

`composeCharts = "0.1.4"` in `libs.versions.toml` — this version was **unresolvable** in Phase 1.
Before implementing charts:
1. Check current latest on `io.github.ehsannarmani:compose-charts` on MavenCentral
2. Update `composeCharts` version in `libs.versions.toml`
3. Add `implementation(libs.compose.charts)` to `feature:overview/build.gradle.kts` (placeholder comment already there)
4. Add maven repo if needed (may not be in mavenCentral — check github releases)

If the library is unavailable, use Canvas-based custom charts for Phase 6. Priority: bar and donut. Line chart can wait.

### Aggregation use cases pattern

Don't add use cases to `data:*` modules — they belong inside `feature:overview`. Aggregate in the ViewModel using `combine`:

```kotlin
combine(
    transactionRepo.observeByMonth(year, month),
    categoryRepo.observeAll(),
) { transactions, categories ->
    // group, sum, map
}
```

For multi-currency: group totals by `CurrencyCode`. `Money.format()` extension is in `core/model/src/commonMain/.../MoneyFormat.kt`.

### YearMonth type

`YearMonth(year, month)` data class with `.previous()` / `.next()` is in `core/model/src/commonMain/.../YearMonth.kt`. Use it for the period selector.

### Chart wrapper pattern

Per architecture docs, chart wrappers live in `feature:overview`, NOT `core:ui`. Each wrapper is a stateless Composable that takes typed data, NOT the raw library types:

```kotlin
// feature/overview/src/commonMain/.../ui/charts/SpendingDonut.kt
@Composable
fun SpendingDonut(slices: List<CategorySlice>, modifier: Modifier = Modifier) {
    // uses compose-charts internally
}
data class CategorySlice(val label: String, val amount: Money, val colorHex: String)
```

This isolates the charts library so swapping it changes only these wrapper files.

### AppScreen — add Overview

`App.kt` has Overview as `PlaceholderScreen("Overview — coming in Phase 6")`. Replace with real `OverviewScreen()`.

### Koin module wiring

Add to `composeApp/di/FeatureModules.kt`:
```kotlin
val featureOverviewModule = module {
    viewModelOf(::OverviewViewModel)
}
```
Add to `appModules` in `AppModules.kt`.

### Known version / import facts

- `kotlin.time.Instant` and `kotlin.time.Clock` (NOT `kotlinx.datetime.*`)
- `kotlinx.datetime.LocalDate`, `YearMonth` from `core:model`
- Material icons: `material-icons-core:1.7.3`
- `libs.compose.*` not `compose.*` in build files
- Add `implementation(libs.kotlin.test)` to `commonTest`
- `@BeforeTest Dispatchers.setMain(testDispatcher)` in ViewModel tests

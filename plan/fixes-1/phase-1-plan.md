# Phase 1 — Kill hardcoded values

Self-contained spec. Run all commands from `/Users/davidvalic/Developer/MoneyM/.claude/worktrees/fixes-1-quality`.

## Goal

Remove every hardcoded `2026` year, the hardcoded `"Main"` / `"USD"` / `epoch 0` in `SeedAccountsUseCase`, and the literal chart x-axis labels `"Jan/Apr/Jul/Oct/Dec"` + `"1/8/15/22/31"` in `OverviewPeriodBody`.

Builder may touch all files listed below. Keep diff scoped to this goal.

---

## 1. `SeedAccountsUseCase` — inject Clock + read currency/name from settings

**File:** `data/accounts/src/commonMain/kotlin/com/dv/moneym/data/accounts/SeedAccountsUseCase.kt`

Replace the body so it:
- Adds constructor param `private val clock: com.dv.moneym.core.common.AppClock`.
- Adds constructor param `private val defaultName: String` (caller supplies the localized "Main").
- Reads currency from `settings.getString(PrefKeys.DEFAULT_CURRENCY, "EUR")` (default `"EUR"` — matches `SeedAccountsUseCaseTest.fallsBackToEurWhenNoCurrencySet`).
- Replaces both `Instant.fromEpochMilliseconds(0)` writes with `clock.now()`.
- Keeps the existing `count() > 0L` guard.
- **Removes** the `if (!settings.getBoolean(PrefKeys.ONBOARDING_COMPLETED)) return` early-return — the existing tests call `useCase()` directly and expect seeding; gating is the caller's responsibility (see `AppInitializer.initialize()` which already runs after onboarding).

Final shape:

```kotlin
package com.dv.moneym.data.accounts

import com.dv.moneym.core.common.AppClock
import com.dv.moneym.core.datastore.AppSettings
import com.dv.moneym.core.datastore.PrefKeys
import com.dv.moneym.core.model.Account
import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.AccountType
import com.dv.moneym.core.model.CurrencyCode

class SeedAccountsUseCase(
    private val repository: AccountRepository,
    private val settings: AppSettings,
    private val clock: AppClock,
    private val defaultName: String,
) {
    suspend operator fun invoke() {
        if (repository.count() > 0L) return
        val now = clock.now()
        val currency = settings.getString(PrefKeys.DEFAULT_CURRENCY, "EUR") ?: "EUR"
        repository.insert(
            Account(
                id = AccountId(0),
                name = defaultName,
                type = AccountType.CASH,
                currency = CurrencyCode(currency),
                isDefault = true,
                archived = false,
                createdAt = now,
                updatedAt = now,
            )
        )
    }
}
```

## 2. Add `DEFAULT_CURRENCY` key

**File:** `core/datastore/src/commonMain/kotlin/com/dv/moneym/core/datastore/AppSettings.kt`

Inside `object PrefKeys`, add:

```kotlin
// User default currency for new accounts
const val DEFAULT_CURRENCY = "pref.default_currency"
```

(matches the literal `"pref.default_currency"` already used in `SeedAccountsUseCaseTest.usesUserSelectedCurrencyFromSettings`.)

## 3. Update Koin wiring

**File:** `composeApp/src/commonMain/kotlin/com/dv/moneym/di/DataModules.kt`

In `dataAccountsModule`, change:

```kotlin
single { SeedAccountsUseCase(get(), get()) }
```

to:

```kotlin
// TODO: localize via composeResources once a shared resource loader exists
single { SeedAccountsUseCase(get(), get(), get(), "Main") }
```

`get()` for `AppClock` requires that `AppClock` is registered. Run:
```
grep -rln "DefaultAppClock\|single<AppClock>\|singleOf(::DefaultAppClock)" composeApp/src
```
If no Koin registration exists for `AppClock`, ADD one in `DataModules.kt` as a new top-level module:

```kotlin
val coreCommonModule = module {
    single<com.dv.moneym.core.common.AppClock> { com.dv.moneym.core.common.DefaultAppClock() }
}
```

Then register `coreCommonModule` wherever the other data modules are aggregated (search for `dataAccountsModule` usage with `grep -rn "dataAccountsModule" composeApp/src` and add `coreCommonModule` alongside it).

## 4. Update `SeedAccountsUseCaseTest`

**File:** `data/accounts/src/commonTest/kotlin/com/dv/moneym/data/accounts/SeedAccountsUseCaseTest.kt`

Confirm `FixedClock` location and shape:
```
cat core/testing/src/commonMain/kotlin/com/dv/moneym/core/testing/FixedClock.kt
```
If it implements `AppClock`, use it. Otherwise inline at the top of the test class:
```kotlin
private val fakeClock = object : com.dv.moneym.core.common.AppClock {
    override fun now() = kotlin.time.Instant.fromEpochMilliseconds(1_700_000_000_000)
    override fun today() = kotlinx.datetime.LocalDate(2026, 1, 1)
}
```

Update `makeUseCase` + every `SeedAccountsUseCase(...)` construction to pass the clock and a default name `"Main"`:

```kotlin
private fun makeUseCase(
    repo: FakeAccountRepository = FakeAccountRepository(),
    settings: FakeAppSettings = FakeAppSettings(),
    name: String = "Main",
) = SeedAccountsUseCase(repo, settings, fakeClock, name)
```

Add a fifth test:

```kotlin
@Test
fun usesProvidedDefaultName() = runTestWithDispatchers {
    val repo = FakeAccountRepository()
    val useCase = SeedAccountsUseCase(repo, FakeAppSettings(), fakeClock, "Wallet")
    useCase()
    assertEquals("Wallet", repo.accounts.first().name)
}
```

## 5. `TransactionListUiState` — nullable initial date

**File:** `feature/transactions/src/commonMain/kotlin/com/dv/moneym/feature/transactions/list/TransactionListUiState.kt`

Change lines 27 and 42:
```kotlin
val currentMonth: YearMonth = YearMonth(2026, 1),
...
val today: LocalDate = LocalDate(2026, 1, 1),
```
to:
```kotlin
val currentMonth: YearMonth? = null,
...
val today: LocalDate? = null,
```

## 6. `TransactionListViewModel` — drop hardcoded fallback in stateIn

**File:** `feature/transactions/src/commonMain/kotlin/com/dv/moneym/feature/transactions/list/TransactionListViewModel.kt`

- Line ~121: `initialValue = TransactionListUiState(today = today)` → `initialValue = TransactionListUiState()` (drop the `today =` argument).
- The flow already populates `currentMonth = base.month` and `today = today` before first real emission; that stays.
- No other change needed.

## 7. `TransactionListScreen` — null-safe consumers

**File:** `feature/transactions/src/commonMain/kotlin/com/dv/moneym/feature/transactions/list/TransactionListScreen.kt`

- Inside `TransactionListContent` (line 126+), right after the existing `var initialScrollDone by remember {...}` block, add:
  ```kotlin
  val today = state.today ?: return  // wait for first VM emission
  val currentMonth = state.currentMonth ?: return
  ```
- Line 140: replace
  ```kotlin
  val anchor = state.earliestMonth ?: YearMonth(state.today.year, state.today.month.number)
  ```
  with
  ```kotlin
  val anchor = state.earliestMonth ?: YearMonth(today.year, today.month.number)
  ```
- In the `MonthPickerDialog` block (lines 142-156), replace `state.currentMonth.year`/`state.currentMonth.monthNumber` with `currentMonth.year`/`currentMonth.monthNumber`.
- In the pager `LaunchedEffect(pagerState.settledPage)` (lines 186-191), replace `state.currentMonth` with `currentMonth`.
- In `MonthNavRow` (lines 377+): at the top of `MonthNavRow`, add `val currentMonth = state.currentMonth ?: return`, then replace `state.currentMonth.year`/`state.currentMonth.monthNumber` reads inside the function with `currentMonth.year`/`currentMonth.monthNumber`.
- Preview at line 581-590: pass explicit non-null defaults so the preview renders:
  ```kotlin
  TransactionListContent(
      state = TransactionListUiState(
          currentMonth = YearMonth(2026, 1),
          today = LocalDate(2026, 1, 1),
      ),
      ...
  )
  ```
  (Hardcoded values inside `@Preview` are acceptable — previews aren't production data. Add `import com.dv.moneym.core.model.YearMonth` and `import kotlinx.datetime.LocalDate` if missing.)

## 8. `OverviewUiState` — nullable initial date

**File:** `feature/overview/src/commonMain/kotlin/com/dv/moneym/feature/overview/OverviewUiState.kt`

Change lines 81, 85, 89:
```kotlin
val currentPeriod: OverviewPeriod = OverviewPeriod.Month(YearMonth(2026, 1)),
...
val monthAnchor: YearMonth = YearMonth(2026, 1),
...
val yearAnchor: Int = 2026,
```
to:
```kotlin
val currentPeriod: OverviewPeriod? = null,
...
val monthAnchor: YearMonth? = null,
...
val yearAnchor: Int? = null,
```

## 9. `OverviewViewModel` — populate clock-derived state, null-safe stateIn

**File:** `feature/overview/src/commonMain/kotlin/com/dv/moneym/feature/overview/OverviewViewModel.kt`

- Search the file for any remaining `2026` literal — there should be none after step 8, but double-check.
- The `stateIn` initial value should be `OverviewUiState()` (no args, accepts all nullable defaults). Update if it currently passes a hardcoded YearMonth.
- The combine block that builds state should already use `AppClock.today()` to derive `currentPeriod` / `monthAnchor` / `yearAnchor` — confirm those still work (they assign non-null values into the nullable fields).

## 10. `OverviewPageUiState` + `OverviewPageViewModel`

**Files:**
- `feature/overview/src/commonMain/kotlin/com/dv/moneym/feature/overview/page/OverviewPageUiState.kt`
- `feature/overview/src/commonMain/kotlin/com/dv/moneym/feature/overview/page/OverviewPageViewModel.kt`

Search both for `2026` and for any `YearMonth(<int>, <int>)` literal with a hardcoded year — replace with clock-derived values. Make corresponding `UiState` fields nullable if a default cannot be computed without the clock.

## 11. `OverviewScreen` — null-safe consumers

**File:** `feature/overview/src/commonMain/kotlin/com/dv/moneym/feature/overview/OverviewScreen.kt`

Any reads of `state.currentPeriod` / `state.monthAnchor` / `state.yearAnchor` now return nullable. Guard at the top of the content composable:

```kotlin
val currentPeriod = state.currentPeriod ?: return
val monthAnchor = state.monthAnchor ?: return
val yearAnchor = state.yearAnchor ?: return
```

Then use the local non-null vals throughout.

## 12. Translate chart x-axis labels

**File:** `feature/overview/src/commonMain/composeResources/values/strings.xml`

Append (inside the root `<resources>` element):

```xml
<string-array name="overview_year_x_labels">
    <item>Jan</item>
    <item>Apr</item>
    <item>Jul</item>
    <item>Oct</item>
    <item>Dec</item>
</string-array>
<string-array name="overview_month_x_labels">
    <item>1</item>
    <item>8</item>
    <item>15</item>
    <item>22</item>
    <item>31</item>
</string-array>
```

Add the same two `<string-array>` blocks (with the matching locale's month abbreviations) to:
- `feature/overview/src/commonMain/composeResources/values-de/strings.xml` — months: `Jan, Apr, Jul, Okt, Dez`
- `feature/overview/src/commonMain/composeResources/values-es/strings.xml` — months: `Ene, Abr, Jul, Oct, Dic`
- `feature/overview/src/commonMain/composeResources/values-it/strings.xml` — months: `Gen, Apr, Lug, Ott, Dic`

The numeric day labels (1/8/15/22/31) are identical across all locales.

**File:** `feature/overview/src/commonMain/kotlin/com/dv/moneym/feature/overview/components/OverviewPeriodBody.kt`

At line 120 replace:
```kotlin
xLabels = listOf("1", "8", "15", "22", "31"),
```
with:
```kotlin
xLabels = stringArrayResource(Res.array.overview_month_x_labels),
```

At line 138 replace:
```kotlin
xLabels = listOf("Jan", "Apr", "Jul", "Oct", "Dec"),
```
with:
```kotlin
xLabels = stringArrayResource(Res.array.overview_year_x_labels),
```

Add imports:
```kotlin
import org.jetbrains.compose.resources.stringArrayResource
import moneym.feature.overview.generated.resources.overview_month_x_labels
import moneym.feature.overview.generated.resources.overview_year_x_labels
```

The Compose Multiplatform `stringArrayResource(Res.array.<name>)` returns `List<String>` directly in current CMP versions. If compile complains about type mismatch (expects List but got Array), wrap in `.toList()`.

---

## Verification

Run from `/Users/davidvalic/Developer/MoneyM/.claude/worktrees/fixes-1-quality`:

```
./gradlew :data:accounts:compileKotlinAndroid
./gradlew :feature:overview:compileKotlinAndroid
./gradlew :feature:transactions:compileKotlinAndroid
./gradlew :composeApp:assembleDebug
./gradlew :data:accounts:testDebugUnitTest
```

All must pass. Then:

```
grep -rn "2026" \
  feature/transactions/src/commonMain \
  feature/overview/src/commonMain \
  data/accounts/src/commonMain \
  composeApp/src/commonMain \
  | grep -v build/
```

Should return zero lines (except inside `@Preview` blocks, which are allowed).

Stop and report back when verification passes. List every file you changed.

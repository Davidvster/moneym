# Phase 2 — Settings entry + `:feature:budgets` list & create screens

**Goal:** User can list, create, edit, and delete budgets from Settings → Budgets. No Overview/TxEdit integration yet.

**Depends on:** Phase 1.

## Files to create

### `:feature:budgets` module

- `feature/budgets/build.gradle.kts` — clone of `feature/categories/build.gradle.kts`. Deps: `projects.data.budgets`, `projects.data.categories` (for category picker), `projects.core.designsystem`, `projects.core.ui`, `projects.core.model`, `projects.core.common`, `projects.core.navigation`, `androidx.navigation3.runtime`.

### List screen

`feature/budgets/src/commonMain/kotlin/com/dv/moneym/feature/budgets/list/`
- `BudgetListViewModel.kt`
  - Constructor: `budgetRepository: BudgetRepository`, `categoryRepository: CategoryRepository`, `dispatchers: DispatcherProvider`, `savedStateHandle: SavedStateHandle`.
  - `state: StateFlow<BudgetListUiState>` = combine of `budgetRepository.observeAll()` + `categoryRepository.observeAll()` + private `_deleteRequestId: MutableStateFlow<BudgetId?>` — joined for display (category name lookup).
  - `fun onIntent(intent: BudgetListIntent)` — handles `DeleteRequested`, `ConfirmDelete`, `DismissDelete`. `CreateClicked`/`EditClicked` emit one-shot effect or are handled by the host (push nav key).
- `BudgetListUiState.kt`
  ```kotlin
  @Serializable
  data class BudgetListUiState(
      val isLoading: Boolean = true,
      val rows: List<BudgetRowVm> = emptyList(),
      val deleteRequestId: BudgetId? = null,
      val deleteRequestName: String? = null,
  )

  data class BudgetRowVm(
      val id: BudgetId,
      val name: String,
      val amount: Money,
      val scopeLabel: String,         // category name or "All categories" string
      val recurringLabel: String?,    // null when single
  )

  sealed interface BudgetListIntent {
      data class DeleteRequested(val id: BudgetId) : BudgetListIntent
      data object ConfirmDelete : BudgetListIntent
      data object DismissDelete : BudgetListIntent
      data object CreateClicked : BudgetListIntent
      data class EditClicked(val id: BudgetId) : BudgetListIntent
  }
  ```
- `BudgetListScreen.kt` — header, `LazyColumn` of `BudgetRow` items, FAB, empty state. Take `onCreate: () -> Unit`, `onEdit: (BudgetId) -> Unit`, `onBack: () -> Unit` from host.
- `components/BudgetRow.kt` — `MmRow` with name, amount (`MmMoney`), scope label, recurring badge, delete action.

### Create screen

`feature/budgets/.../create/`
- `BudgetCreateViewModel.kt`
  - Constructor params (Koin): `budgetId: BudgetId?` (params.getOrNull), `budgetRepository`, `categoryRepository`, `appSettingsRepository`, `appClock`, `dispatchers`, `savedStateHandle`.
  - On init: if `budgetId != null`, load existing → pre-fill state. Else default `startYearMonth = appClock.today().toYearMonth()`, `currency = appSettingsRepository.observeDefaultCurrency().first()`.
  - `state: StateFlow<BudgetCreateUiState>`, `onIntent`.
- `BudgetCreateUiState.kt`
  ```kotlin
  @Serializable
  data class BudgetCreateUiState(
      val isEditMode: Boolean = false,
      val isLoading: Boolean = false,
      val isSaving: Boolean = false,
      val name: String = "",
      val amountText: String = "",
      val currency: String = "EUR",
      val availableCategories: List<Category> = emptyList(),
      val selectedCategoryId: CategoryId? = null,   // null = all categories
      val periodType: BudgetPeriodType = BudgetPeriodType.MONTHLY,
      val startYearMonth: YearMonth? = null,
      val recurringKind: RecurringKind = RecurringKind.Single,
      val recurringNMonths: Int = 3,
      val nameError: Boolean = false,
      val amountError: Boolean = false,
      val recurringCountError: Boolean = false,
      val saved: Boolean = false,
  )

  enum class RecurringKind { Single, Unlimited, NMonths }

  sealed interface BudgetCreateIntent {
      data class NameChanged(val text: String) : BudgetCreateIntent
      data class AmountChanged(val text: String) : BudgetCreateIntent
      data class CategorySelected(val id: CategoryId?) : BudgetCreateIntent
      data class StartMonthChanged(val ym: YearMonth) : BudgetCreateIntent
      data class RecurringKindChanged(val kind: RecurringKind) : BudgetCreateIntent
      data class RecurringCountChanged(val n: Int) : BudgetCreateIntent
      data object Save : BudgetCreateIntent
      data object Cancel : BudgetCreateIntent
  }
  ```
  - `Save` builds `Budget` (encoding `recurringMonths` from `recurringKind`: `Single → null`, `Unlimited → -1`, `NMonths → recurringNMonths`) → insert/update via repo → set `saved = true`. UI observes and pops nav.
- `BudgetCreateScreen.kt` — column: name `MmField`, amount `MmField`, category picker (chips with "All categories" first), month picker, recurring kind segmented control + count field (visible only for `NMonths`), Save bar.

### Strings

- `feature/budgets/src/commonMain/composeResources/values/strings.xml` + `values-de/` + `values-es/` + `values-it/` — `budgets_title`, `budgets_empty`, `budgets_new`, `budgets_name_label`, `budgets_amount_label`, `budgets_category_label`, `budgets_all_categories`, `budgets_start_month`, `budgets_recurring`, `budgets_recurring_single`, `budgets_recurring_unlimited`, `budgets_recurring_n_months`, `budgets_recurring_count_label`, `budgets_save`, `budgets_cancel`, `budgets_delete`, `budgets_delete_confirm_title`, `budgets_delete_confirm_body`, `budgets_recurring_badge_unlimited`, `budgets_recurring_badge_n_months`.

## Files to modify

- `settings.gradle.kts` — `include(":feature:budgets")`.
- `composeApp/.../di/FeatureModules.kt`
  ```kotlin
  val featureBudgetsModule = module {
      viewModelOf(::BudgetListViewModel)
      viewModel { params ->
          BudgetCreateViewModel(
              budgetId = params.getOrNull(),
              get(), get(), get(), get(), get(), get(),
          )
      }
  }
  ```
  Add to module list.
- `core/navigation/src/commonMain/kotlin/.../NavKeys.kt` (or wherever existing keys live; e.g. alongside `CategoriesKey`)
  ```kotlin
  @Serializable data object BudgetListKey : ModalKey
  @Serializable data class BudgetCreateKey(val id: Long? = null) : ModalKey
  ```
- `composeApp/src/commonMain/kotlin/com/dv/moneym/MainNav.kt`
  - Add `budgetListEntry(onBack, onCreate, onEdit)` and `budgetCreateEntry(onBack, id)` provider funcs (mirror `categoriesEntry`).
  - Register both in the nav graph.
  - Wire `onCreate = { tabBackStack.push(BudgetCreateKey()) }`, `onEdit = { id -> tabBackStack.push(BudgetCreateKey(id.value)) }`.
- Settings integration:
  - `feature/settings/.../overview/SettingsScreen.kt` — add `onNavigateToBudgets: () -> Unit` param.
  - `feature/settings/.../overview/components/PreferencesSection.kt` — add new `MmRow(onClick = onNavigateToBudgets)` between Categories and Wallets rows, using `Res.string.settings_budgets` and `Icon.Wallet` (or pick fitting existing icon).
  - `feature/settings/src/commonMain/composeResources/values{,-de,-es,-it}/strings.xml` — add `settings_budgets`.
  - `composeApp/.../MainNav.kt` — pass `onNavigateToBudgets = { tabBackStack.push(BudgetListKey) }` into `settingsEntry(...)`.

## Tests

- `feature/budgets/src/commonTest/kotlin/.../BudgetListViewModelTest.kt` (Turbine) — observes repo emissions; `DeleteRequested` then `ConfirmDelete` removes the row.
- `feature/budgets/src/commonTest/kotlin/.../BudgetCreateViewModelTest.kt` — validation (empty name → `nameError`, blank amount → `amountError`, `NMonths` with `n <= 0` → `recurringCountError`); `Save` constructs the expected `Budget` and calls `repo.insert`; edit mode pre-fills state.

## Verify

```bash
./gradlew :feature:budgets:testDebugUnitTest :feature:settings:testDebugUnitTest
./gradlew :composeApp:assembleDebug
# Manual: Android emulator, Settings → Budgets → create one → appears in list → edit → delete.
```

## Done when

- All commands green.
- Manual create/edit/delete loop works end-to-end on the emulator.
- All four locales have the new strings (placeholder translations OK for `de/es/it` if you can't translate them right now — flag in commit message).

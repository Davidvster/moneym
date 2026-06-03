# Phase 5c — Feature VM tests: wallet + overview page + transactionEdit

Modules: `feature/settings` (wallet pkg), `feature/overview`, `feature/transactionEdit`. Same harness as 5a/5b (`SyncSettingsViewModelTest.kt`). Pass `SavedStateHandle()` / required nav params (accountId, period, editingId, ruleId, currentCurrency). Read each `*ViewModel.kt` + `*UiState.kt` + `*Intent.kt` first. Cover every intent/branch/error/Flow-update; effects/SingleUiEvents asserted once.

Fakes: `core:testing` (`FakeAccountRepository`, `FakeTransactionRepository`, `FakeCategoryRepository`, `FakeRecurringTransactionRepository`, `FakeAppSettingsRepository`, `FakeAppSettings`, `FixedClock`, `TestDispatcherProvider`). Write a local `FakePaymentModeRepository` in `feature/transactionEdit` commonTest (and `feature/overview` if needed). Use-cases are real, plain classes — construct with fakes (read their ctors).

If a module's `commonTest` lacks `libs.turbine` / `libs.kotlinx.coroutines.test` / `libs.kotlin.test`, add to its `build.gradle.kts` commonTest deps (test-only, allowed). (`feature/settings` already has them.)

## VMs to test (7)

### Wallet (feature/settings/wallet)
1. **AddWalletViewModel** — `FakeAccountRepository`, `SavedStateHandle()`. Cover name/currency/type input intents, save → insert + nav effect, validation (empty name).
2. **EditWalletViewModel** — `accountId: Long`, `FakeAccountRepository`, `SavedStateHandle()`. Pre-seed account; cover load, edit fields, save, archive/delete if present.
3. **EditWalletCurrencyViewModel** — `accountId`, `currentCurrency`, `FakeAccountRepository`, `FakeTransactionRepository`, `TestDispatcherProvider`, `SavedStateHandle()`. Cover currency change, conversion-rate path, confirm.
4. **WalletManageViewModel** — `FakeAccountRepository`, `FakeTransactionRepository`, `FakeAppSettingsRepository`, `SavedStateHandle()`. Cover list, reorder/default-set, archive, balance display.

### Overview (feature/overview)
5. **OverviewPageViewModel** — read its real ctor (takes `period: OverviewPeriod` + a `BuildOverviewPageStateUseCase` and/or repos + `AppClock`). Construct `BuildOverviewPageStateUseCase` real with fake repos. Cover state build per period, empty data, and any refresh/intent.

### transactionEdit (feature/transactionEdit)
6. **TransactionEditViewModel** — `editingId: TransactionId?` (null = create, non-null = edit), real use-cases (`GetTransactionUseCase`, `UpsertTransactionUseCase`, `DeleteTransactionUseCase`, `ValidateAndBuildTransactionUseCase`, `ComputeCategoryBudgetRemainingUseCase`, `SuggestNotesUseCase` — all constructed with fakes), fake repos (incl. local `FakePaymentModeRepository`), `FakeAppSettingsRepository`, `TestDispatcherProvider`, `AppClock`/`FixedClock`, `SavedStateHandle()`. Cover: create vs edit load, amount/category/account/note/date/type intents, validation success → upsert + nav, validation failure → typed error, delete, budget-remaining + note-suggestion derived fields.
7. **RecurringEditViewModel** — `ruleId`, `FakeRecurringTransactionRepository`, `FakeCategoryRepository`, `FakeAccountRepository`, local `FakePaymentModeRepository`, `FakeAppSettingsRepository`, `TestDispatcherProvider`, `FixedClock`, `SavedStateHandle()`. Cover load rule, edit recurrence fields, save, delete.

## Notes / known unit-test pitfalls (cover the rest, note the gap)
- Paths calling Compose `getString(Res.string...)` hang in unit tests — skip that branch, note it.
- Real `Clock.System.now()`-based timers/backoff loops don't terminate under `runTest` virtual time — avoid triggering them.
- `stateIn(Lazily)` may swallow transient `isLoading=true` mid-flight — assert final state + effects.

## Verify
```
./gradlew :feature:settings:testDebugUnitTest :feature:overview:testDebugUnitTest :feature:transactionEdit:testDebugUnitTest
```
Green. Test sources only (except allowed commonTest build.gradle deps). Import classes, no FQN, no comments unless non-obvious.

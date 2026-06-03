# Phase 5b — Feature VM tests: settings (overview + paymentmodes + recurring)

Module: `feature/settings` (all in its `commonTest`). Same harness as phase 5a / `SyncSettingsViewModelTest.kt`. Pass `SavedStateHandle()` where the ctor takes one. Fakes from `core:testing`: `FakeAppSettings`, `FakeAppSettingsRepository`, `FakeAccountRepository`, `FakeCategoryRepository`, `FakeTransactionRepository`, `FakeRecurringTransactionRepository`, `FixedClock`, `TestDispatcherProvider`, `InMemorySecureStore`.

If `feature/settings` `commonTest` lacks `libs.turbine` / `libs.kotlinx.coroutines.test` / `libs.kotlin.test`, add them to its `build.gradle.kts` commonTest deps (test-only; allowed).

For each VM read `*ViewModel.kt` + `*UiState.kt` + `*Intent.kt`. Cover every intent, branch, error path, Flow-driven update; effects/SingleUiEvents asserted once.

## Test-only fakes to write (in settings commonTest)
- `FakePaymentModeRepository` implementing `data/transactions/.../PaymentModeRepository` (interface) — in-memory MutableStateFlow.
- `FakeLocaleController` implementing `core/common/.../LocaleController`.
- `FakeBiometricAuthenticator` implementing `core/security/.../BiometricAuthenticator` (configurable).

## VMs to test (9)
1. **SettingsOverviewViewModel** — `FakeAppSettingsRepository`, `FakeAccountRepository`. Cover settings rows, theme/toggle intents, account-derived state.
2. **SecuritySettingsViewModel** — `FakeAppSettings`, real `PinManager(InMemorySecureStore(), PinHasher(), FakeAppSettings)` (PinHasher is an `expect class` with a pure JCE Android actual — construct directly), `FakeBiometricAuthenticator`, `TestDispatcherProvider`. Cover enable/disable PIN, biometric toggle, change-PIN, KeyInvalidated.
3. **CurrencyPickerViewModel** — only `SavedStateHandle()`. Cover currency list/search/selection intents.
4. **ExportViewModel** — real `BackupExporter(fakes…, FakeAppSettings)` + `BackupImporter(fakes…)` (both depend only on repos + AppSettings), `TestDispatcherProvider`. Cover export trigger, import trigger, success/error state.
5. **ImportDataViewModel** — `CsvImportHolder()` (trivial), fake repos, real `PrepareImportPreviewUseCase(...)` (construct with its deps), `TestDispatcherProvider`, `FixedClock`. Cover preview build, confirm import, error/empty branches.
6. **LanguagePickerViewModel** — `FakeAppSettingsRepository`, `FakeLocaleController`. Cover language list, select→persist+apply.
7. **TxListDisplayViewModel** — `FakeAppSettingsRepository`. Cover display-pref toggles.
8. **PaymentModeListViewModel** — `FakePaymentModeRepository`, `SavedStateHandle()`. Cover list, add/rename/delete intents, dialog flags.
9. **RecurringListViewModel** — `FakeRecurringTransactionRepository`, `FakeCategoryRepository`. Cover list rendering (recurring joined with category), delete/edit intents.

## SKIP (blocked) — BackupRestoreViewModel
Depends on `DbBackupManager` + `BackupCodec` (final concrete, transitively need `DbPlatform` expect class) and `FilePlatform` (`expect class`) — not constructable from `commonTest`. Out of scope; note it in the report.

## Verify
```
./gradlew :feature:settings:testDebugUnitTest
```
Green. Test sources only (except the allowed commonTest build.gradle deps). Import classes, no FQN, no comments unless non-obvious. Keep `Fake*Repository` parity in mind if you touch any `core:testing` fake (prefer adding the new fakes locally in settings commonTest instead).

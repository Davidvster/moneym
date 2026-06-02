# Phase 5a — Feature VM tests: onboarding + security

Write behavior-complete ViewModel unit tests in each module's `commonTest`. Harness = `feature/sync/src/commonTest/.../SyncSettingsViewModelTest.kt`: `StandardTestDispatcher` + `Dispatchers.setMain/resetMain` (`@BeforeTest/@AfterTest`), `runTest(testDispatcher)`, Turbine `state.test { skipItems(1); … }`. These VMs use `savedStateHandle.saved` so pass `SavedStateHandle()` (import `androidx.lifecycle.SavedStateHandle`). Existing fakes in `core:testing`: `FakeAppSettings`, `FakeAccountRepository`, `FixedClock`, `TestDispatchers` (`TestDispatcherProvider`, `runTestWithDispatchers`), `InMemorySecureStore`.

For each VM read its `*ViewModel.kt` + `*UiState.kt` + `*Intent.kt`. Cover every intent branch, error path, and StateFlow-driven update. Effects/SingleUiEvent (navigation/snackbar) asserted via their `Flow` exactly once.

## VMs to test (4)

1. **OnboardingCurrencyViewModel** (`feature/onboarding/.../currency/`)
   Deps: `AccountRepository` → `FakeAccountRepository`; `AppSettings` → `FakeAppSettings`; `SavedStateHandle()`. Cover currency selection intent, persistence to settings, seeding/continue action.

2. **OnboardingSecurityViewModel** (`feature/onboarding/.../security/`)
   Deps: `AppSettings` → `FakeAppSettings`; `PinManager` → construct real: `PinManager(InMemorySecureStore(), <PinHasher>, FakeAppSettings)` (use the same `FakeAppSettings` instance the VM gets, or check whether a test `PinHasher` is needed — `core:security` `PinHasher`; if it's an interface write a trivial fake, if concrete use it directly); `BiometricAuthenticator` → write `FakeBiometricAuthenticator` in the test source set implementing the interface (`isAvailable`, `biometryType`, `suspend authenticate() -> BiometricResult`). Cover enable-PIN, skip, biometric-available branches.

3. **PinSetupViewModel** (`feature/security/.../setup/`)
   Deps: `PinManager` (real, as above), `DispatcherProvider` → `TestDispatcherProvider`, `BiometricAuthenticator` → fake, `AppSettings` → fake, `SavedStateHandle()`. Cover digit entry, confirm-match vs mismatch, clear, completion effect.

4. **PinUnlockViewModel** (`feature/security/.../unlock/`)
   Same dep set. Pre-seed a PIN via `PinManager.setPin(...)` in setup. Cover correct PIN → unlock effect, wrong PIN → attempts/error, biometric unlock branch, lockout if present.

Put `FakeBiometricAuthenticator` once per module that needs it (onboarding, security) in the test source set; make its result configurable (var). Keep `Fake*Repository` parity rule in mind — but these are test-only fakes, fine.

## SKIP (blocked) — OnboardingRestoreViewModel
Do NOT test it. It depends on `DbBackupManager` and `BackupCodec`, which are **final concrete classes** (not interfaces) that transitively require the `DbPlatform` `expect class` — not constructable from `commonTest` (see CLAUDE.md "Testability blocker: DbPlatform"). Faking would require a production refactor (make `DbBackupManager` an interface), which is out of scope for this phase. Leave a one-line note in your report.

## Verify
```
./gradlew :feature:onboarding:testDebugUnitTest :feature:security:testDebugUnitTest
```
Green. Test sources only — no production edits. Import classes, no FQN, no comments unless non-obvious.

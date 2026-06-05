# CLAUDE.md

Guidance for Claude Code working in this repository.

## Project Overview

MoneyM is a Kotlin Multiplatform (KMP) project targeting Android and iOS. Shared UI is built with Compose Multiplatform. The codebase is split into `core/`, `data/`, `feature/` modules consumed by the `shared` KMP library, with thin per-platform app entry points (`androidApp`, `iosApp`). Uses Room for persistence, Koin for DI, navigation3, kotlinx-serialization, kotlinx-datetime.

## Module Layout

```
shared/                        # KMP library — Koin wiring, MainNav, OnboardingNav, App(), iOS MainViewController, Android library code
androidApp/                    # com.android.application — MainActivity, MoneyMApp, AndroidManifest, res/, signing
core/
├── common/                    # AppClock, AppLogger, DispatcherProvider
├── datastore/                 # AppSettings, AppSettingsRepository, PrefKeys
├── designsystem/              # MM theme, MoneyMTheme
├── model/                     # Domain models (Account, Transaction, Category, Money, YearMonth, ...)
├── navigation/                # Nav keys / route abstractions
├── platform/                  # expect class DbPlatform + actuals
├── security/                  # PIN hashing
├── testing/                   # FakeAccountRepository, FakeAppSettings, FixedClock, TestDispatchers
└── ui/                        # Shared composables (MmButton, MmCard, MmField, ...)
data/
├── accounts/                  # AccountRepository, Room DB, SeedAccountsUseCase
├── backup/                    # DbBackupManager, BackupExporter/Importer
├── categories/                # CategoryRepository, Room DB, SeedCategoriesUseCase
├── settings/                  # (extra settings storage if any)
└── transactions/              # TransactionRepository, PaymentModeRepository, Room DB
feature/
├── categories/                # CategoryListViewModel + components
├── onboarding/                # OnboardingCurrencyViewModel, OnboardingSecurityViewModel
├── overview/                  # OverviewViewModel, OverviewPageViewModel, usecase/
├── security/                  # PinSetupViewModel, PinUnlockViewModel
├── settings/                  # SettingsOverviewViewModel + sub-screens (wallet, paymentmodes, importdata, ...)
├── transactionEdit/           # TransactionEditViewModel + usecase/
└── transactions/              # TransactionListViewModel, TransactionPageViewModel
iosApp/                        # Xcode project that consumes the `Shared` Kotlin/Native framework
```

Each module's source is under `src/commonMain/kotlin/...`, with `commonTest/`, `androidMain/`, `iosMain/` where applicable.

## Build Commands

Gradle task names of the form `compileKotlinAndroid` are **ambiguous** — always use the variant-qualified form (`compileDebugKotlinAndroid` / `compileReleaseKotlinAndroid`).

```bash
# Android
./gradlew :androidApp:assembleDebug

# iOS — no XCFramework task is configured. Link the framework binaries directly,
# then let the Xcode project consume them.
./gradlew :shared:linkDebugFrameworkIosArm64 \
          :shared:linkDebugFrameworkIosSimulatorArm64

# Open in Xcode: iosApp/iosApp.xcodeproj, scheme iosApp
# Or build from CLI for the simulator (no signing):
xcodebuild -project iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -configuration Debug \
  -destination "generic/platform=iOS Simulator" \
  build CODE_SIGNING_ALLOWED=NO

# Tests
./gradlew testDebugUnitTest        # all module's Android unit tests
./gradlew iosSimulatorArm64Test    # Kotlin/Native iOS sim tests
./gradlew allTests                 # everything (slow)
./gradlew :feature:overview:testDebugUnitTest   # single module
```

## Architecture Conventions

### ViewModel API surface
Every ViewModel exposes exactly one public entry point: `fun onIntent(intent: <VmName>Intent)`. All other methods are `private`. State is a single `internal val state: StateFlow<<Vm>UiState>`. The Intent sealed interface lives in the same file as the UiState (or a sibling `<Vm>Intent.kt` if absent). Folding new behavior into Intent variants is preferred over adding public methods.

### Dumb UI
Composables should not hold domain state in `remember { mutableStateOf(...) }`. Dialog visibility, search-active flags, picker selections, form values: those go in the ViewModel's UiState and are driven via `onIntent`. Pure UI transients (LazyListState, scroll-sync flags like `initialScrollDone`, `rememberSaveable` for transient pickers) stay in the composable.

### Business logic location
Composable → ViewModel → UseCase. If a composable computes anything beyond presentation, push it to the VM. If a VM's body grows past ~250 lines or any function past ~60 lines, extract pure logic into a use case under `feature/<module>/.../usecase/`. Use cases are plain classes with constructor-injected dependencies and no Compose/Flow/Coroutine deps inside their bodies.

### ViewModel visibility for Koin
Any ViewModel registered in `shared/src/commonMain/kotlin/com/dv/moneym/di/FeatureModules.kt` must be **`public`** (the default — do not mark with `internal`). `shared` cannot reference an `internal` class across a module boundary. Same for UseCases registered there.

### Koin module locations
- `shared/.../di/DataModules.kt` — repositories, seeders, `AppClock` (`single<AppClock> { DefaultAppClock() }` in `coreCommonModule`).
- `shared/.../di/FeatureModules.kt` — every ViewModel + every use case.
- The platform `actual` constructors (e.g. `DbPlatform`) are wired in platform-specific Koin modules under `shared/androidMain/` and `shared/iosMain/`.

### Repository ↔ fake parity
When adding a method to a `*Repository` interface in `data/`, also add the matching override in the corresponding `core/testing/Fake*Repository`. Otherwise every `:*:compileDebugUnitTestKotlinAndroid` task starts failing. Same for `AppSettingsRepository` → `FakeAppSettingsRepository`.

### Testability blocker: `DbPlatform`
`core/platform/.../DbPlatform.kt` is an `expect class`, not an interface, so `DbBackupManager` (which depends on it) cannot be constructed from `commonTest`. Anything that takes `DbBackupManager` is not unit-testable from common code today. Convert `DbPlatform` to an interface (or add a `FakeDbPlatform` in a JVM-only test source set) before writing those tests.

### State defaults — no hardcoded dates
UiState fields like `currentMonth: YearMonth?` and `today: LocalDate?` are nullable and default `null`. The ViewModel populates them from `AppClock.today()` on first emit. Compose screens guard with `?: return` until the first real emission. **Do not** put a literal `YearMonth(2026, 1)` or `LocalDate(...)` as a UiState default — preview-only defaults are fine inside `@Preview` composables.

## Kotlin Conventions

- Import the class — never use a fully qualified name unless absolutely necessary (e.g. name collision).
- No comments unless the *why* is non-obvious.
- No `// TODO: caller adds X later` placeholder code.

## Platform Abstraction

Platform-specific behavior uses Kotlin's `expect/actual`:

- `commonMain/Platform.kt` — `expect` interface/class
- `androidMain/Platform.android.kt`, `iosMain/Platform.ios.kt` — `actual` implementations

If a platform abstraction needs to be fake-able from `commonTest`, declare it as a plain `interface` in `commonMain` and have the platform module provide the implementation — `expect class` cannot be constructed from common tests.

## Dependency Catalog

All dependency versions in `gradle/libs.versions.toml`. Add new deps there, not as hardcoded versions in build scripts.

## i18n / Strings

Compose-multiplatform string resources live per module under `src/commonMain/composeResources/values{,-de,-es,-it}/strings.xml`. Access via `stringResource(Res.string.<key>)`. For arrays: `stringArrayResource(Res.array.<key>)`. Never put a user-visible literal (chart axis label, default name, etc.) in Kotlin source — add a string resource and translate to de/es/it.

## Localized seed values

`SeedAccountsUseCase` takes `defaultName: String` and reads currency from `PrefKeys.DEFAULT_CURRENCY` (fallback `"EUR"`). Currently `shared` passes the literal `"Main"` (see TODO in `DataModules.kt`); once a shared resource loader exists, this should be localized.

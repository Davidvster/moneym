# Phase 1 — Skeleton modules + DI bootstrap

**Status**: ⏳ Ready to start

Goal: stand up the full multi-module skeleton, wire DI, and render a "Hello MoneyM" screen on Android and iOS using `MoneyMTheme`. No feature code yet — only the foundation.

**Exit criteria**: `./gradlew :composeApp:assembleDebug` succeeds, the iOS framework builds, `./gradlew allTests` passes, and a freshly installed app shows the "Hello MoneyM" screen in both light and dark mode on both platforms.

## Step status

- [x] **1.1** Library catalog versions and entries pinned in `libs.versions.toml`
- [x] **1.2** Update `settings.gradle.kts` to include every module
- [x] **1.3** Scaffold module skeletons (build files + empty source dirs)
  - [x] **1.3.1** `core:model`
  - [x] **1.3.2** `core:common`
  - [x] **1.3.3** `core:designsystem`
  - [x] **1.3.4** `core:ui`
  - [x] **1.3.5** `core:database`
  - [x] **1.3.6** `core:datastore`
  - [x] **1.3.7** `core:security`
  - [x] **1.3.8** `core:navigation`
  - [x] **1.3.9** `core:testing`
  - [x] **1.3.10** `data:transactions`
  - [x] **1.3.11** `data:categories`
  - [x] **1.3.12** `data:accounts`
  - [x] **1.3.13** `data:settings`
  - [x] **1.3.14** `data:backup`
  - [x] **1.3.15** `feature:transactions`
  - [x] **1.3.16** `feature:transactionEdit`
  - [x] **1.3.17** `feature:overview`
  - [x] **1.3.18** `feature:categories`
  - [x] **1.3.19** `feature:settings`
  - [x] **1.3.20** `feature:security`
  - [x] **1.3.21** `feature:onboarding`
- [x] **1.4** Verify all skeletons compile (`compileKotlinMetadata` green on all 21 modules)
- [x] **1.5** Implement `core:model` minimum types (Money, CurrencyCode, TransactionId/CategoryId/AccountId, TransactionType)
- [x] **1.6** Implement `core:common` (DispatcherProvider + Android/iOS actuals, AppClock, AppLogger/Kermit)
- [x] **1.7** Implement `core:designsystem` (MoneyMTheme, colors, typography, spacing, CategoryColor; MoneyMIcons stubbed — icons dep added Phase 3)
- [x] **1.8** Implement `core:database` (SqlDriverFactory interface, AndroidSqlDriverFactory, IosSqlDriverFactory)
- [x] **1.9** Implement `core:datastore` (AppSettings interface + PrefKeys, DefaultAppSettings)
- [x] **1.10** Implement `core:security` (SecureStore interface, PinHasher expect/actual + Android PBKDF2 + iOS Phase 1 stub, PinHasherTest)
- [x] **1.11** Implement `core:navigation` (NavRoute marker interface)
- [x] **1.12** Implement `core:testing` (TestDispatcherProvider, runTestWithDispatchers, FixedClock)
- [x] **1.13** Wire `composeApp` deps + Koin bootstrap (coreCommonModule, coreDatastoreModule)
- [x] **1.14** Create `App()` composable renders "Hello MoneyM" under `MoneyMTheme` via KoinApplication
- [x] **1.15** Android entry already calls `App()` ✓
- [x] **1.16** iOS entry already calls `App()` ✓
- [x] **1.17** Final verification: `assembleDebug` ✓ · Android unit tests ✓ · iOS simulator tests ✓

---

## 1.2 Update `settings.gradle.kts` to include every module

**Goal**: register every Phase 1 module so Gradle can resolve typed project accessors.

**Files**: `settings.gradle.kts`

**Actions**:

Replace the existing `include(":composeApp")` line with a list that registers each module path:

```kotlin
include(":composeApp")
include(":core:model")
include(":core:common")
include(":core:designsystem")
include(":core:ui")
include(":core:database")
include(":core:datastore")
include(":core:security")
include(":core:navigation")
include(":core:testing")
include(":data:transactions")
include(":data:categories")
include(":data:accounts")
include(":data:settings")
include(":data:backup")
include(":feature:transactions")
include(":feature:transactionEdit")
include(":feature:overview")
include(":feature:categories")
include(":feature:settings")
include(":feature:security")
include(":feature:onboarding")
```

**Acceptance**: `./gradlew projects` lists all 21 module paths. (`projects.core.model` etc. become valid in build scripts once the module directories also exist — that's step 1.3.)

---

## 1.3 Scaffold module skeletons

Each sub-step creates a single module: directory tree + `build.gradle.kts` + empty source dirs. No production code yet.

**Module shape A — pure Kotlin core** (no Compose, no platform deps):
Used by: `core:model`, `core:common`

```
<path>/
  build.gradle.kts
  src/
    commonMain/kotlin/com/dv/moneym/<package>/.gitkeep
    commonTest/kotlin/com/dv/moneym/<package>/.gitkeep
    androidMain/kotlin/com/dv/moneym/<package>/.gitkeep      (only if expect/actual needed)
    iosMain/kotlin/com/dv/moneym/<package>/.gitkeep          (only if expect/actual needed)
```

`build.gradle.kts` (Shape A):
```kotlin
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
}

kotlin {
    androidTarget { compilerOptions { jvmTarget.set(JvmTarget.JVM_11) } }
    listOf(iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework { baseName = "<name>" ; isStatic = true }
    }
    sourceSets {
        commonMain.dependencies {
            // module-specific
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "com.dv.moneym.<package>"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig { minSdk = libs.versions.android.minSdk.get().toInt() }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_11 ; targetCompatibility = JavaVersion.VERSION_11 }
}
```

**Module shape B — Compose core / feature** (with Compose plugins):
Used by: `core:designsystem`, `core:ui`, `core:navigation`, all `feature:*`

Adds:
```kotlin
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}
```

and Compose deps in `commonMain`.

**Module shape C — data module** (no Compose, has SQLDelight):
Used by: `data:transactions`, `data:categories`, `data:accounts`

Adds:
```kotlin
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.sqldelight)
}

sqldelight {
    databases {
        create("<Name>Database") {
            packageName.set("com.dv.moneym.data.<name>.db")
        }
    }
}
```

Plus SQLDelight runtime dependencies in `commonMain`, and platform drivers in `androidMain` / `iosMain`.

**Module shape D — pure data without DB** (just utilities):
Used by: `data:settings`, `data:backup`

Same as shape A; the `data:` prefix is conceptual, not tooling.

---

### 1.3.1 `core:model`

**Goal**: shape A. Pure domain types. No dependencies.
**Files**: `core/model/build.gradle.kts`, `core/model/src/{commonMain,commonTest}/kotlin/com/dv/moneym/core/model/.gitkeep`
**Dependencies (commonMain)**: `kotlinx-serialization-json` (for `@Serializable` value classes), `kotlinx-datetime` (for `LocalDate`/`Instant` in domain types).
**Plugins**: `kotlinSerialization` in addition to shape A.
**Acceptance**: `./gradlew :core:model:compileKotlinMetadata` succeeds.

### 1.3.2 `core:common`

**Goal**: shape A. Concurrency + clock + logging utilities.
**Files**: `core/common/build.gradle.kts`, `core/common/src/{commonMain,commonTest,androidMain,iosMain}/kotlin/com/dv/moneym/core/common/.gitkeep`
**Dependencies (commonMain)**: `kotlinx-coroutines-core`, `kotlinx-datetime`, `kermit`.
**Acceptance**: `./gradlew :core:common:compileKotlinMetadata` succeeds.

### 1.3.3 `core:designsystem`

**Goal**: shape B. Theme tokens.
**Files**: `core/designsystem/build.gradle.kts`, source dirs.
**Dependencies (commonMain)**: Compose runtime/foundation/material3/ui, `projects.core.model`, `projects.core.common`.
**Acceptance**: `./gradlew :core:designsystem:compileKotlinMetadata` succeeds.

### 1.3.4 `core:ui`

**Goal**: shape B. Reusable Composables.
**Files**: same shape as 1.3.3.
**Dependencies**: same as `core:designsystem` plus `projects.core.designsystem`, `projects.core.model`.
**Acceptance**: compile succeeds.

### 1.3.5 `core:database`

**Goal**: shape C with no database declared yet (this module exposes the driver factory only).
**Files**: `core/database/build.gradle.kts`, source dirs incl. `androidMain` and `iosMain`.
**Dependencies (commonMain)**: `sqldelight-runtime`, `sqldelight-coroutines-extensions`, `projects.core.common`. (`androidMain`: `sqldelight-android-driver`. `iosMain`: `sqldelight-native-driver`.)
**Plugins**: drop `sqldelight` plugin here (no `.sq` files in this module — each data module declares its own database). Use shape A + driver deps.
**Acceptance**: compile succeeds.

### 1.3.6 `core:datastore`

**Goal**: shape A. Settings wrapper.
**Dependencies (commonMain)**: `multiplatform-settings`, `multiplatform-settings-coroutines`, `multiplatform-settings-no-arg`, `projects.core.common`.
**Acceptance**: compile succeeds.

### 1.3.7 `core:security`

**Goal**: shape A with platform actuals.
**Dependencies (commonMain)**: `projects.core.common`, `projects.core.datastore`, `kotlinx-coroutines-core`. (`androidMain`: `androidx-biometric`, `androidx-security-crypto`, `androidx-activity-compose` for prompt host.)
**Acceptance**: compile succeeds.

### 1.3.8 `core:navigation`

**Goal**: shape B.
**Dependencies (commonMain)**: `androidx-navigation-compose`, `kotlinx-serialization-json`, `projects.core.model`.
**Plugins**: add `kotlinSerialization`.
**Acceptance**: compile succeeds.

### 1.3.9 `core:testing`

**Goal**: shape A.
**Dependencies (commonMain)**: `projects.core.common`, `projects.core.model`, `kotlin-test`, `kotlinx-coroutines-test`, `turbine`, `kotest-assertions-core`.
**Acceptance**: compile succeeds.

### 1.3.10 `data:transactions`

**Goal**: shape C with `TransactionsDatabase`.
**Dependencies (commonMain)**: `projects.core.model`, `projects.core.common`, `projects.core.database`, `sqldelight-runtime`, `sqldelight-coroutines-extensions`, `kotlinx-coroutines-core`.
**Files**: build script + empty `src/commonMain/sqldelight/com/dv/moneym/data/transactions/` directory (no `.sq` yet).
**Acceptance**: compile succeeds (empty SQLDelight database is OK).

### 1.3.11 `data:categories`

Same shape as 1.3.10 with `CategoriesDatabase` package.

### 1.3.12 `data:accounts`

Same shape as 1.3.10 with `AccountsDatabase` package.

### 1.3.13 `data:settings`

**Goal**: shape D (no SQLDelight).
**Dependencies (commonMain)**: `projects.core.model`, `projects.core.common`, `projects.core.datastore`.
**Acceptance**: compile succeeds.

### 1.3.14 `data:backup`

**Goal**: shape D.
**Dependencies (commonMain)**: `projects.core.model`, `projects.core.common`, `kotlinx-serialization-json`.
**Plugins**: add `kotlinSerialization`.
**Acceptance**: compile succeeds.

### 1.3.15 — 1.3.21 `feature:*`

All seven feature modules follow shape B.

**Common dependencies (commonMain)** for each:
- `projects.core.designsystem`
- `projects.core.ui`
- `projects.core.model`
- `projects.core.common`
- `projects.core.navigation`
- `androidx-lifecycle-viewmodelCompose`
- `androidx-lifecycle-runtimeCompose`
- `koin-compose`
- `koin-compose-viewmodel`

Feature-specific data deps (added per feature):
- `feature:transactions`, `feature:transactionEdit`, `feature:overview`: `projects.data.transactions`, `projects.data.categories`, `projects.data.accounts`
- `feature:categories`: `projects.data.categories`
- `feature:settings`: `projects.data.settings`, `projects.data.backup`
- `feature:security`: `projects.core.security`, `projects.data.settings`
- `feature:onboarding`: `projects.data.settings`, `projects.core.security`

**Test dependencies** for each: `projects.core.testing`.

**Acceptance**: each module compiles individually.

---

## 1.4 Verify all skeletons compile

**Goal**: confirm the module graph is internally consistent before adding code.

**Command**: `./gradlew assemble`

**Acceptance**: green build. If it fails, the typical culprit is a missing `include(...)` in `settings.gradle.kts` or a typo in `projects.<...>` references. Fix before continuing.

---

## 1.5 Implement `core:model`

**Goal**: the minimum domain types other modules will need to compile against during Phase 1.

**Files**: `core/model/src/commonMain/kotlin/com/dv/moneym/core/model/`
- `Money.kt` — `CurrencyCode`, `Money(minorUnits: Long, currency: CurrencyCode)`
- `Ids.kt` — `TransactionId`, `CategoryId`, `AccountId` (all `@JvmInline value class`)
- `TransactionType.kt` — `enum class TransactionType { INCOME, EXPENSE }`

No `Transaction`, `Category`, `Account` data classes yet — those land in Phase 2 when their data modules exist.

**Acceptance**: `./gradlew :core:model:compileKotlinMetadata` succeeds. One smoke test in `commonTest` that asserts `Money(100, CurrencyCode("EUR"))` round-trips through `kotlinx-serialization`.

---

## 1.6 Implement `core:common`

**Goal**: foundational utilities used everywhere.

**Files**: `core/common/src/commonMain/kotlin/com/dv/moneym/core/common/`
- `DispatcherProvider.kt`:
  ```kotlin
  interface DispatcherProvider {
      val main: CoroutineDispatcher
      val default: CoroutineDispatcher
      val io: CoroutineDispatcher
  }
  ```
- `AppClock.kt`:
  ```kotlin
  interface AppClock { fun now(): Instant ; fun today(): LocalDate }
  ```
- `AppLogger.kt` — thin Kermit wrapper:
  ```kotlin
  object AppLogger { fun tag(tag: String): Logger = Logger.withTag(tag) }
  ```
- `DefaultDispatcherProvider.kt` — `expect class`
- `androidMain/.../DefaultDispatcherProvider.android.kt` — `actual` using `Dispatchers.IO`
- `iosMain/.../DefaultDispatcherProvider.ios.kt` — `actual` using `Dispatchers.Default.limitedParallelism(64)` for `io`

**Acceptance**: compile succeeds; one test confirming the Android `actual` returns `Dispatchers.IO`.

---

## 1.7 Implement `core:designsystem`

**Goal**: theme tokens and `MoneyMTheme` so every Composable can render correctly.

**Files**: `core/designsystem/src/commonMain/kotlin/com/dv/moneym/core/designsystem/`
- `MoneyMTheme.kt` — top-level composable that wraps `MaterialTheme` with our `ColorScheme` (light + dark), `Typography`, and `LocalSpacing` / `LocalMoneyMColors` CompositionLocals.
- `MoneyMColors.kt` — `lightMoneyMColors()` and `darkMoneyMColors()` returning Material 3 `ColorScheme` using the palette from `theming.md`.
- `MoneyMTypography.kt` — `moneyMTypography()` returning `Typography` with tabular-figure setup for amount styles.
- `MoneyMSpacing.kt` — `data class MoneyMSpacing(val xxs, xs, sm, md, lg, xl, xxl: Dp)` plus default values.
- `MoneyMIcons.kt` — `object MoneyMIcons { val add, edit, delete, ... = Icons.Filled.* }` (Material Symbols passthrough for now).
- `CategoryColor.kt` — `fun categoryColor(hex: String): Color` with contrast fallback.

**Acceptance**: compile succeeds; one Compose preview test (Android only) confirms `MoneyMTheme { Text("hi") }` renders without crashing.

---

## 1.8 Implement `core:database`

**Goal**: a `SqlDriverFactory` that data modules use to instantiate their database.

**Files**:
- `commonMain/.../SqlDriverFactory.kt`:
  ```kotlin
  expect class SqlDriverFactory { fun create(schema: SqlSchema<...>, name: String): SqlDriver }
  ```
- `androidMain/.../SqlDriverFactory.android.kt` — backed by `AndroidSqliteDriver`, requires a `Context`.
- `iosMain/.../SqlDriverFactory.ios.kt` — backed by `NativeSqliteDriver`.

**Acceptance**: compile succeeds.

---

## 1.9 Implement `core:datastore`

**Goal**: a single `AppSettings` interface backed by `multiplatform-settings`.

**Files**:
- `commonMain/.../AppSettings.kt`:
  ```kotlin
  interface AppSettings {
      fun getString(key: String): String?
      fun putString(key: String, value: String)
      fun getBoolean(key: String, default: Boolean = false): Boolean
      fun putBoolean(key: String, value: Boolean)
      fun observeString(key: String): Flow<String?>
      // ... matched Long/Int variants
  }
  ```
- `commonMain/.../DefaultAppSettings.kt` — implementation taking a `Settings` from multiplatform-settings.
- Use `SettingsFactory` from `multiplatform-settings-no-arg` to construct platform-default instances.

**Acceptance**: compile succeeds.

---

## 1.10 Implement `core:security`

**Goal**: enough of the security abstraction to compile (real PIN flow lands in Phase 4, but the interfaces and the PBKDF2 hasher must exist now so other modules can take type-safe deps).

**Files**:
- `commonMain/.../SecureStore.kt` — interface from `security.md`.
- `commonMain/.../PinHasher.kt`:
  ```kotlin
  class PinHasher {
      fun hash(pin: String): HashedPin           // PBKDF2-HMAC-SHA256, 600k iterations, 16-byte salt
      fun verify(pin: String, hashed: HashedPin): Boolean   // constant-time
  }
  data class HashedPin(val algorithm: String, val iterations: Int, val salt: ByteArray, val digest: ByteArray)
  ```
- `androidMain/.../SecureStore.android.kt` — stub returning `TODO()` is fine for Phase 1; real impl in Phase 4. Document the stub.
- `iosMain/.../SecureStore.ios.kt` — same stub pattern.
- `commonTest/.../PinHasherTest.kt` — round-trip test, constant-time verify test.

**Acceptance**: compile succeeds; `PinHasher` round-trip test passes on JVM.

---

## 1.11 Implement `core:navigation`

**Goal**: base scaffolding for type-safe routes.

**Files**:
- `commonMain/.../NavRoute.kt`:
  ```kotlin
  interface NavRoute   // marker; subclasses use @Serializable
  ```
- `commonMain/.../NavExtensions.kt` — helpers like `inline fun <reified T : NavRoute> NavGraphBuilder.route(content: ...)`.

**Acceptance**: compile succeeds.

---

## 1.12 Implement `core:testing`

**Goal**: shared test infrastructure.

**Files**:
- `commonMain/.../runTestWithDispatchers.kt`:
  ```kotlin
  fun runTestWithDispatchers(
      dispatcher: TestDispatcher = StandardTestDispatcher(),
      block: suspend TestScope.(TestDispatcherProvider) -> Unit,
  ) = runTest(dispatcher) { block(this, TestDispatcherProvider(dispatcher)) }

  class TestDispatcherProvider(d: TestDispatcher) : DispatcherProvider {
      override val main = d ; override val default = d ; override val io = d
  }
  ```
- `commonMain/.../FixedClock.kt` — `AppClock` test double with settable now.
- `commonMain/.../Fakes.kt` — empty file with package + convention comment (real fakes land per data module).

**Acceptance**: compile succeeds; one self-test verifying `runTestWithDispatchers` runs a coroutine.

---

## 1.13 Wire `composeApp` deps + Koin bootstrap + Kermit

**Goal**: bring the app shell up with DI initialized, even before any real screens exist.

**Files**:
- `composeApp/build.gradle.kts` — add `implementation` deps on `projects.core.designsystem`, `projects.core.common`, `projects.core.navigation`, `projects.core.datastore`, `projects.core.database`, `projects.core.security`, and `koin-compose`, `koin-compose-viewmodel`, `kermit`.
- `composeApp/src/commonMain/kotlin/com/dv/moneym/di/AppModules.kt`:
  ```kotlin
  val appModules: List<Module> = listOf(
      coreCommonModule,
      coreDatastoreModule,
      // …filled in as later phases land each module's bindings
  )
  ```
- Empty Koin module files in `composeApp/src/commonMain/kotlin/com/dv/moneym/di/` for each foundation: `CoreCommonModule.kt`, `CoreDatastoreModule.kt`, etc. — they declare `val coreCommonModule = module { single<DispatcherProvider> { DefaultDispatcherProvider() } }`.
- `composeApp/src/commonMain/kotlin/com/dv/moneym/App.kt`:
  ```kotlin
  @Composable
  fun App() {
      KoinApplication(application = { modules(appModules) }) {
          MoneyMTheme { HelloMoneyM() }
      }
  }

  @Composable private fun HelloMoneyM() { /* centered text */ }
  ```

**Acceptance**: `./gradlew :composeApp:assembleDebug` succeeds; Koin starts without missing-binding errors.

---

## 1.14 Create `App()` composable that renders Hello MoneyM under `MoneyMTheme`

Folded into 1.13 above (it's the same Compose unit). Separate step in the checklist so it's visible. Mark together when both are done.

---

## 1.15 Wire Android entry

**Goal**: `MainActivity` hosts `App()`.

**Files**: `composeApp/src/androidMain/kotlin/com/dv/moneym/MainActivity.kt`
```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { App() }
    }
}
```

Update `composeApp/src/androidMain/AndroidManifest.xml` if not already pointing at this `MainActivity`.

**Acceptance**: `./gradlew :composeApp:assembleDebug` installs and the app shows the Hello-MoneyM screen.

---

## 1.16 Wire iOS entry

**Goal**: `MainViewController()` hosts `App()`.

**Files**: `composeApp/src/iosMain/kotlin/com/dv/moneym/MainViewController.kt`
```kotlin
fun MainViewController() = ComposeUIViewController { App() }
```

Open the Xcode project (`iosApp/`), confirm it calls `MainViewControllerKt.MainViewController()` from its SwiftUI/UIKit entry point.

**Acceptance**: iOS framework builds; running from Xcode shows the Hello-MoneyM screen.

---

## 1.17 Final verification

**Goal**: confirm Phase 1 exit criteria.

**Steps**:
1. `./gradlew clean`
2. `./gradlew :composeApp:assembleDebug` → green
3. `./gradlew allTests` → green
4. iOS framework build (`./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64`) → green
5. Run on Android emulator: Hello-MoneyM in light mode. Toggle system to dark mode → app re-renders correctly.
6. Run on iOS simulator: Hello-MoneyM in light + dark mode.

**Acceptance**: all six pass. Update Phase 1 status in this file to ✅ and proceed to Phase 2.

---

## If something goes sideways

Common Phase 1 failure modes and where to look:

- **Missing project accessor (`projects.core.<x>`)** → step 1.2: module not added to `settings.gradle.kts`.
- **Compose plugin errors on a non-Compose module** → wrong shape (B vs A) in 1.3.x.
- **iOS framework link failure mentioning duplicate symbols** → two modules export a framework with the same `baseName`. Each module sets its own unique `baseName`.
- **SQLDelight asks for a `.sq` file** → no `.sq` files in `commonMain/sqldelight/...` is OK *if* the `sqldelight { databases { create(...) } }` block is omitted; for empty data modules in Phase 1 we don't declare a database yet.
- **Koin "no definition found"** → an `appModules` list is missing a module reference, or a dependency hasn't been registered yet. Phase 1 should only require the foundation modules.

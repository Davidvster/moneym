# Phase 8 — Polish + release prep

**Status**: 📝 Sketch (expand when starting)

Goal: final pass before release. Localization quality, accessibility audit, performance, app icons, and release-build verification.

**Exit criteria**: release-candidate build installed on real Android + iOS devices, dogfooded for a week, no major regressions.

## Steps (will expand when starting)

- [ ] **8.1** Localization quality pass — review en/es/it/de strings with a native speaker or translator. Verify plurals, currency formatting per locale.
- [ ] **8.2** Empty / error / loading states audit — sweep every screen, ensure each has all three.
- [ ] **8.3** Accessibility audit:
  - [ ] **8.3.1** TalkBack walkthrough on Android
  - [ ] **8.3.2** VoiceOver walkthrough on iOS
  - [ ] **8.3.3** Dynamic font sizing — every screen at largest accessibility size
  - [ ] **8.3.4** Color contrast verification on theme tokens
- [ ] **8.4** Performance:
  - [ ] **8.4.1** Transaction list with 5,000+ rows — scroll smoothness, query speed
  - [ ] **8.4.2** Overview charts with 12 months × ~30 transactions/day
  - [ ] **8.4.3** Cold launch time (Android baseline profile if needed)
- [ ] **8.5** App icons (adaptive icon for Android, `Assets.xcassets` for iOS) — minimalist black-and-white per design system
- [ ] **8.6** Launch screens (Android `windowSplashScreen…` + iOS storyboard)
- [ ] **8.7** Release build verification:
  - [ ] **8.7.1** Android: signed release APK / AAB, R8/proguard rules verified
  - [ ] **8.7.2** iOS: archive build, code-signed
- [ ] **8.8** Store listing assets (screenshots, descriptions in en/es/it/de) — out of scope for this plan unless requested
- [ ] **8.9** Re-evaluate crash reporting (ADR-014) — still skipping unless user feedback suggests otherwise
- [ ] **8.10** Final manual regression pass against the original v1 feature list

---

## Concrete implementation notes (for fresh sessions)

### Library versions confirmed working (as of Phase 3)

| Library | Version | Catalog key |
|---|---|---|
| Kotlin | 2.3.21 | `kotlin` |
| Compose Multiplatform | 1.10.3 | `composeMultiplatform` |
| Material3 | 1.10.0-alpha05 | `material3` |
| material-icons-core | **1.7.3** | `composeIconsCore` |
| material-icons-extended | **1.7.3** | `composeIconsCore` |
| SQLDelight | 2.0.2 | `sqldelight` |
| kotlinx-datetime | **0.7.0** | `kotlinxDatetime` |
| Koin | 4.0.0 | `koin` |
| androidx-lifecycle | 2.10.0 | `androidx-lifecycle` |
| multiplatform-settings | 1.2.0 | `multiplatformSettings` |
| Kermit | 2.0.5 | `kermit` |
| Turbine | 1.2.0 | `turbine` |
| kotest-assertions-core | 5.9.1 | `kotest` |
| Mockative | 3.0.1 | `mockative` |

### Datetime migration (completed in Phase 3)

All code uses:
- `import kotlin.time.Instant` (NOT `kotlinx.datetime.Instant`)
- `import kotlin.time.Clock` (NOT `kotlinx.datetime.Clock`)
- `kotlinx.datetime.LocalDate`, `LocalDateTime`, `TimeZone`, `todayIn`, `toLocalDateTime` still from `kotlinx.datetime`

### `expect`/`actual` classes warning

Build emits: `'expect'/'actual' classes are in Beta. Consider using '-Xexpect-actual-classes'`. This is harmless — it's from `DefaultDispatcherProvider`. Suppress per-project by adding to `gradle.properties`:
```
kotlin.native.suppressExpectActualClassesWarning=true
```
Or add `-Xexpect-actual-classes` to Kotlin Native compiler options.

### R8/Proguard rules needed for release (step 8.7.1)

Add `proguard-rules.pro` to `composeApp/`:
```
# Koin
-keep class org.koin.** { *; }
-keepclassmembers class * { @org.koin.core.annotation.* <methods>; }
# SQLDelight
-keep class app.cash.sqldelight.** { *; }
# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keep @kotlinx.serialization.Serializable class * { *; }
# Kermit
-keep class co.touchlab.kermit.** { *; }
```
Wire in `composeApp/build.gradle.kts` release build type.

### Navigation library (unresolved)

`org.jetbrains.androidx.navigation:navigation-compose` — version `2.10.0` was unresolvable. Current navigation uses simple `AppScreen` sealed class state in `App.kt`. Phase 8 is a good time to retry with the correct version. Check JetBrains GitHub releases for compatible version with CMP 1.10.3 / lifecycle 2.10.0.

### Module structure summary (all 21 modules)

```
:composeApp          App entry, DI bootstrap, root navigation, App.kt (AppScreen nav)
:core:model          Domain types: Money, Transaction, Category, Account, YearMonth, TransactionFilter, MoneyFormat
:core:common         DispatcherProvider (expect/actual), AppClock, AppLogger (Kermit wrapper)
:core:designsystem   MoneyMTheme, colors, typography, spacing, MoneyMIcons, MoneyMCategoryIcons (Phase 5+), CategoryColor
:core:ui             Reusable composables (empty in Phase 3, built up each phase)
:core:database       SqlDriverFactory interface, AndroidSqlDriverFactory, IosSqlDriverFactory
:core:datastore      AppSettings interface, DefaultAppSettings, PrefKeys
:core:security       SecureStore interface, PinHasher (real Android/iOS in Phase 4), BiometricAuthenticator (Phase 4)
:core:navigation     NavRoute marker interface (Jetpack Nav wiring deferred)
:core:testing        TestDispatcherProvider, runTestWithDispatchers, FixedClock, FakeCategoryRepository, FakeAccountRepository, FakeTransactionRepository
:data:transactions   TransactionRepository + SQLDelight (TransactionEntry table)
:data:categories     CategoryRepository + SQLDelight (Category table) + SeedCategoriesUseCase + DefaultCategories
:data:accounts       AccountRepository + SQLDelight (Account table) + SeedAccountsUseCase
:data:settings       (empty skeleton — SettingsRepository goes here in Phase 5+)
:data:backup         (empty skeleton — JSON/CSV export/import in Phase 7)
:feature:transactions  TransactionListScreen, TransactionListViewModel, filter chips, month nav
:feature:transactionEdit  TransactionEditScreen, TransactionEditViewModel, UpsertTransaction/Delete/GetTransaction use cases
:feature:overview    (empty skeleton — charts in Phase 6)
:feature:categories  (empty skeleton — Phase 5)
:feature:settings    (empty skeleton — Phase 5+)
:feature:security    (empty skeleton — Phase 4)
:feature:onboarding  (empty skeleton — Phase 5)
```

### Koin wiring overview (after Phase 3)

`composeApp/di/`:
- `AppModules.kt` — `appModules` list, includes all module lists
- `DataModules.kt` — `dataCategoriesModule`, `dataAccountsModule`, `dataTransactionsModule`
- `FeatureModules.kt` — `featureTransactionsModule`, `featureTransactionEditModule`
- `androidMain/di/AndroidPlatformModule.kt` — `androidPlatformModule(context)`: provides `SqlDriverFactory`
- `iosMain/di/IosPlatformModule.kt` — `iosPlatformModule()`: provides `SqlDriverFactory`

Each phase adds to `FeatureModules.kt` and updates `appModules` list.

### Compose UI test (instrumented Android)

Phase 3 plan mentioned Compose UI test for transaction list — not yet written. For Phase 8 polish, add at minimum one end-to-end UI test using `androidx.compose.ui:ui-test-junit4` in `composeApp/src/androidInstrumentedTest/`. This requires `androidTest.dependencies` block in `composeApp/build.gradle.kts`.

### `data class` IDs (not value class)

`TransactionId`, `CategoryId`, `AccountId`, `CurrencyCode` are `data class` with a single `val value: Long/String` field. `@JvmInline value class` was attempted but `kotlin.jvm.JvmInline` is not available on Kotlin/Native in this build setup. Keep as `data class`.

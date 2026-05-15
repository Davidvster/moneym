# Phase 5 — Categories + onboarding

**Status**: ✅ Done

Goal: full category management (CRUD with icon and color pickers) and the first-launch onboarding flow.

**Exit criteria**: fresh install routes to onboarding (currency → optional PIN → optional biometric → done); second launch goes straight to transactions; categories CRUD round-trips and is reflected in the transaction picker.

## Steps (will expand when starting)

- [ ] **5.1** `feature:categories` — `CategoryListScreen` (active + archived sections) + ViewModel
- [ ] **5.2** `feature:categories` — `CategoryEditScreen` (create + edit shared) — name, icon picker, color picker
- [ ] **5.3** Icon picker — grid of `MoneyMIcons` keys (extend `core:designsystem` registry)
- [ ] **5.4** Color picker — 12 preset hues + custom hex input with contrast validation
- [ ] **5.5** UseCases: `CreateCategory`, `UpdateCategory`, `ArchiveCategory`, `UnarchiveCategory`
- [ ] **5.6** Archive (not delete) — guarded by "any transactions reference this category" check
- [ ] **5.7** `feature:onboarding` — `OnboardingScreen` (skippable 1–2 step flow)
- [ ] **5.8** `feature:onboarding` — currency selection step (default = device locale's currency)
- [ ] **5.9** `feature:onboarding` — optional PIN setup step (reuses `feature:security` screens)
- [ ] **5.10** `feature:onboarding` — optional biometric step
- [ ] **5.11** `feature:onboarding` — sets `pref.onboarding_completed = true` and `pref.default_currency` on completion
- [ ] **5.12** `composeApp` root — observe `pref.onboarding_completed`; route to onboarding when false
- [ ] **5.13** Strings for en/es/it/de
- [ ] **5.14** ViewModel tests + at least one Compose UI test
- [ ] **5.15** Verify: install → onboarding → home; restart → home directly; categories CRUD works end-to-end

---

## Concrete implementation notes (for fresh sessions)

### What already exists

**`data:categories`** fully implemented:
- `CategoryRepository` interface + `createCategoryRepository(db, dispatchers)` factory
- `SeedCategoriesUseCase` — idempotent, seeds 11 defaults from `DefaultCategories.kt`
- `FakeCategoryRepository` in `core:testing`
- Koin wiring in `composeApp/di/DataModules.kt` (`dataCategoriesModule`)

Default categories (in `data/categories/src/commonMain/.../DefaultCategories.kt`): Groceries, Eating out, Rent, Transport, Utilities, Health, Entertainment, Shopping, Salary, Other (expense), Other (income).

**Skeleton modules** `feature:categories` and `feature:onboarding` exist with empty source dirs — add code without Gradle changes.

**`AppSettings` / `PrefKeys`** in `core/datastore/src/commonMain/.../AppSettings.kt`:
`PrefKeys.DEFAULT_CURRENCY`, `PrefKeys.ONBOARDING_COMPLETED`, etc.

### AppScreen navigation — add Onboarding

`App.kt` has `sealed interface AppScreen` with `Transactions`, `TransactionEdit(id)`, `Overview`, `Settings`. Add `object Onboarding` and gate in `AppContent`:
```kotlin
val onboardingDone by appSettings.observeBoolean(PrefKeys.ONBOARDING_COMPLETED).collectAsState(false)
if (!onboardingDone) {
    OnboardingScreen(onComplete = { /* write pref */ })
} else { /* existing nav shell */ }
```
Inject `AppSettings` via `koinInject<AppSettings>()`.

### Archive vs delete for categories

`CategoryRepository` has `update(category)` and `delete(id)`. Policy: archive when any transaction references the category. `ArchiveCategoryUseCase` should check `TransactionRepository.observeFiltered(ByCategory(id))` for non-empty, then call `update(category.copy(archived = true))` instead of delete.

### Icon registry for category icons

`MoneyMIcons` in `core/designsystem` has navigation icons only. Add `object MoneyMCategoryIcons` mapping icon key strings → `ImageVector`. Most category icons (restaurant, car, bolt, etc.) are in `material-icons-extended`. Add dep to `core:designsystem`:
```
compose-material-icons-extended = { module = "org.jetbrains.compose.material:material-icons-extended", version.ref = "composeIconsCore" }
```
`composeIconsCore = "1.7.3"` already in catalog.

### Color picker

`defaultCategoryColors` list and `categoryColor(hex): Color` already in `core/designsystem/src/commonMain/.../CategoryColor.kt`. Use these directly.

### Koin module wiring

Add to `composeApp/di/FeatureModules.kt` (file exists after Phase 3):
```kotlin
val featureCategoriesModule = module {
    viewModelOf(::CategoryListViewModel)
    viewModelOf(::CategoryEditViewModel)
    single { ArchiveCategoryUseCase(get(), get()) }
}
val featureOnboardingModule = module {
    viewModelOf(::OnboardingViewModel)
}
```
Add both to `appModules` list in `AppModules.kt`.

### ViewModel test pattern

Always add to every ViewModel test class:
```kotlin
private val testDispatcher = StandardTestDispatcher()
@BeforeTest fun setUp() { Dispatchers.setMain(testDispatcher) }
@AfterTest fun tearDown() { Dispatchers.resetMain() }
```

### Known version / import facts

- `kotlin.time.Instant` and `kotlin.time.Clock` (NOT `kotlinx.datetime.*`)
- `kotlinx.datetime.LocalDate`, `TimeZone`, `todayIn` still from `kotlinx.datetime`
- Material icons: `material-icons-core:1.7.3`, `material-icons-extended:1.7.3`
- `Icons.AutoMirrored.Filled.ArrowBack/List` (not deprecated `Icons.Filled.*` variants)
- `libs.compose.*` in build files, NOT `compose.*`
- Add `implementation(libs.kotlin.test)` explicitly to `commonTest` of every new module
- `data class` for ID types (not `value class` — `@JvmInline` not available on K/N)

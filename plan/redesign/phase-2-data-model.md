# Phase 2 — Data Model Extension

Add the new domain types and persistence keys required by the redesign. No UI changes.

---

## Goal
The app gains `TxDisplayPrefs`, `ThemeMode`, `IndicatorStyle`, `Density` — all persisted via `AppSettings`. ViewModels in later phases read these and pass them down to components.

---

## Files to modify / create

### `core/model/src/commonMain/…/model/`

**`TxDisplayPrefs.kt`** — **New**
```kotlin
enum class IndicatorStyle { IconTile, SoftIcon, Bar, Dot, Minimal }
enum class Density { Compact, Comfortable }

data class TxDisplayPrefs(
    val indicatorStyle: IndicatorStyle = IndicatorStyle.IconTile,
    val showCategoryName: Boolean = true,
    val showNote: Boolean = true,
    val density: Density = Density.Comfortable,
)
```

**`ThemeMode.kt`** — **New**
```kotlin
enum class ThemeMode { Light, Dark, Auto }
```

### `core/datastore/src/commonMain/…/datastore/AppSettings.kt`

Add new keys to `PrefKeys`:
```kotlin
const val TX_INDICATOR_STYLE = "pref.tx_indicator_style"   // IndicatorStyle.name
const val TX_SHOW_CATEGORY   = "pref.tx_show_category"     // Boolean
const val TX_SHOW_NOTE       = "pref.tx_show_note"         // Boolean
const val TX_DENSITY         = "pref.tx_density"           // Density.name
```
(THEME_MODE key already exists.)

### `core/datastore/src/commonMain/…/datastore/AppSettingsRepository.kt` — **New**

Interface in `core/datastore`:
```kotlin
interface AppSettingsRepository {
    fun observeThemeMode(): Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
    fun observeTxDisplayPrefs(): Flow<TxDisplayPrefs>
    suspend fun setTxDisplayPrefs(prefs: TxDisplayPrefs)
    fun observeDefaultCurrency(): Flow<String>
    suspend fun setDefaultCurrency(currency: String)
    fun observeLanguage(): Flow<String>
    suspend fun setLanguage(language: String)
}
```

**`DefaultAppSettingsRepository.kt`** — implementation wrapping `AppSettings` key/value store. Serialize enums as `.name`, deserialize with `enumValueOf`.

### Koin wiring

In the `core/datastore` Koin module: `single<AppSettingsRepository> { DefaultAppSettingsRepository(get()) }`.

---

## Key implementation notes

- `ThemeMode` replaces the raw string approach currently used. Existing stored value `"light"/"dark"/"auto"` maps to enum via `enumValueOf` with a fallback to `Auto`.
- `TxDisplayPrefs` is stored as 4 separate keys (not serialized JSON) so each pref can be observed independently.
- The Settings ViewModel in Phase 6 reads `observeThemeMode()` and emits it to the root `App.kt` which calls `MoneyMTheme(isDark = resolvedDark)`. This keeps the theme reactive.
- `TxDisplayPrefs` is observed by `TransactionListViewModel` (Phase 3) and passed down as part of `TransactionListUiState`.

---

## Verification
1. Unit test: `DefaultAppSettingsRepository` round-trips each enum through `putString`/`getString`.
2. `observeTxDisplayPrefs()` emits a new value after `setTxDisplayPrefs(…)` — test with Turbine.
3. Build passes — no import errors in features that already use `AppSettings` directly.

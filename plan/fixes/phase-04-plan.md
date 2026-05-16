# Phase 4: Theme Picker Applies Theme

## Problem
The theme picker segmented control in Settings calls `onIntent(SettingsIntent.ThemeModeChanged(...))` which calls `appSettingsRepository.setThemeMode(mode)` which calls `appSettings.putString(PrefKeys.THEME_MODE, ...)`. The problem is that `DefaultAppSettings.observeString()` returns `flowOf(getString(...))` — a cold, non-reactive flow that never emits again after the first collection. So `App.kt`'s `appSettingsRepo.observeThemeMode().collectAsState(ThemeMode.Auto)` only reads the value at startup and never re-emits when the theme changes.

## Files to modify
- `core/datastore/src/commonMain/kotlin/com/dv/moneym/core/datastore/DefaultAppSettings.kt` — make `observeBoolean`, `observeString`, `observeInt` reactive by using a `MutableSharedFlow` or in-memory `MutableStateFlow` per key, emitting whenever `put*` is called

## Implementation steps

1. Add an internal `MutableSharedFlow<String>` (or `MutableStateFlow<Map<String, Any?>>`) that emits the key whenever any `put*` method is called.

2. Replace the non-reactive `observeString`, `observeBoolean`, `observeInt` methods:

   **Approach**: Use a single `MutableSharedFlow<Unit>` that broadcasts whenever anything changes. Each `observe*` method returns a `flow { emit(get*(key)); ...collect(changesFlow) { emit(get*(key)) } }`.

   Better: use a replay `SharedFlow<String>` keyed by key name:
   ```kotlin
   private val changesFlow = MutableSharedFlow<String>(extraBufferCapacity = 64)
   
   override fun putString(key: String, value: String) {
       settings.putString(key, value)
       changesFlow.tryEmit(key)
   }
   // same for putBoolean, putInt, putLong
   
   override fun observeString(key: String, defaultValue: String?): Flow<String?> =
       changesFlow
           .filter { it == key }
           .onStart { emit(key) }
           .map { getString(key, defaultValue) }
   
   override fun observeBoolean(key: String, defaultValue: Boolean): Flow<Boolean> =
       changesFlow
           .filter { it == key }
           .onStart { emit(key) }
           .map { getBoolean(key, defaultValue) }
   
   override fun observeInt(key: String, defaultValue: Int): Flow<Int> =
       changesFlow
           .filter { it == key }
           .onStart { emit(key) }
           .map { getInt(key, defaultValue) }
   ```

3. Add the necessary imports: `kotlinx.coroutines.flow.MutableSharedFlow`, `filter`, `onStart`, `map`.

4. No changes to `DefaultAppSettingsRepository` or `App.kt` needed — the fix is purely in the `AppSettings` implementation.

## Acceptance criteria
- [ ] Tapping "Light" in Settings → Theme immediately changes the app to light mode without restarting
- [ ] Tapping "Dark" immediately switches to dark mode
- [ ] Tapping "Auto" follows system dark/light preference
- [ ] App restores the last selected theme on restart
- [ ] `TxDisplayPrefs` changes in Settings also apply live (observable prefs are reactive)

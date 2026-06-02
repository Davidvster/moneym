# Phase 1 — VM-state refactor: AnalyzeViewModel + SyncSettingsViewModel

Bring both VMs to the `viewmodel-state` skill convention. Reference impl: `feature/transactions/src/commonMain/.../list/TransactionListViewModel.kt`.

## Task A — Make nested types serializable (core:ai)
Add `@Serializable` (import `kotlinx.serialization.Serializable`) to, in `core/ai/src/commonMain/kotlin/com/dv/moneym/core/ai/`:
- `ChatMessage.kt`: annotate both `enum class ChatRole` and `data class ChatMessage`.
- `AiGroundingMode.kt`: annotate `enum class AiGroundingMode`.

core:ai already depends on `libs.kotlinx.serialization.json` — no Gradle change. Verify the serialization Gradle plugin is applied to core:ai (`build.gradle.kts` plugins block); if `kotlin("plugin.serialization")` / `alias(libs.plugins.kotlinSerialization)` is missing, add it (match how `feature/transactions` or another module applies it).

## Task B — AnalyzeViewModel
File: `feature/aianalysis/src/commonMain/kotlin/com/dv/moneym/feature/aianalysis/`.

`AnalyzeUiState.kt`:
- Add `@Serializable` to `AnalyzeUiState`.
- Replace `val errorKey: String? = null` with `val error: AnalyzeError? = null`.
- Add a typed error in the same file:
  ```kotlin
  @Serializable
  sealed interface AnalyzeError { @Serializable data object GenerationFailed : AnalyzeError }
  ```

`AnalyzeViewModel.kt`:
- Add `savedStateHandle: SavedStateHandle` as the last constructor param.
- Replace the raw flow with:
  ```kotlin
  private val _state by savedStateHandle.saved { MutableStateFlow(AnalyzeUiState(groundingMode = loadGroundingMode())) }
  internal val state = _state.onStart { init() }.stateIn(viewModelScope, SharingStarted.Lazily, _state.value)
  ```
  (imports: `androidx.lifecycle.SavedStateHandle`, `androidx.lifecycle.serialization.saved`, `kotlinx.coroutines.flow.onStart`, `SharingStarted`, `stateIn`.)
- Move the constructor `init {}` body into `private suspend fun init() { … }` (the availability check). Keep `withContext(dispatchers.io)`.
- `sendMessage` error path: `errorKey = ERROR_KEY` → `error = AnalyzeError.GenerationFailed`; `ClearError` intent sets `error = null`. Remove the `ERROR_KEY` companion const if now unused.

`AnalyzeScreen.kt`:
- `if (state.errorKey != null)` → `if (state.error != null)`. Keep `stringResource(Res.string.analyze_error)`.

Koin (`composeApp/src/commonMain/kotlin/com/dv/moneym/di/FeatureModules.kt`, `featureAianalysisModule`): add `savedStateHandle = get(),` to the `AnalyzeViewModel(...)` call.

## Task C — SyncSettingsViewModel
File: `feature/sync/src/commonMain/kotlin/com/dv/moneym/feature/sync/`.

`SyncSettingsUiState.kt`: add `@Serializable` to both `DeviceRow` and `SyncSettingsUiState`.

`SyncSettingsViewModel.kt`:
- Add `savedStateHandle: SavedStateHandle` as last constructor param.
- Replace `_state`/`state`:
  ```kotlin
  private val _state by savedStateHandle.saved { MutableStateFlow(SyncSettingsUiState()) }
  internal val state = _state.onStart { init() }.stateIn(viewModelScope, SharingStarted.Lazily, _state.value)
  ```
- Rename `private fun refresh()` → `private suspend fun init()`? No — `refresh()` launches its own coroutine and is also called from several places (`toggleSync`, `submitRename`, `removeDevice`, `Refresh` intent). Keep `private fun refresh()` as-is and make `init()`:
  ```kotlin
  private suspend fun init() { refreshNow() }
  ```
  Simplest: extract the body currently inside `refresh()`'s `viewModelScope.launch { … }` into `private suspend fun refreshNow()`, have `refresh()` = `viewModelScope.launch { refreshNow() }`, and `init()` = `refreshNow()`. Remove the constructor `init { refresh() }` block.
- `SyncSettingsIntent.Refresh` keeps calling `refresh()`.

Koin (`featureSyncModule`): add `savedStateHandle = get(),` to `SyncSettingsViewModel(...)`.

## Task D — Update tests
- `feature/sync/src/commonTest/.../SyncSettingsViewModelTest.kt`: the `vm(...)` helper now needs a `SavedStateHandle` — pass `SavedStateHandle()` (import `androidx.lifecycle.SavedStateHandle`). Emission order: with `stateIn(Lazily, _state.value)` the first emitted item is the default `_state.value` (still `isLoading=true`), then `init()` runs on first collection → existing `skipItems(1)` + `awaitSettled()` pattern should still hold; adjust if an extra emission appears.
- `feature/aianalysis/src/commonTest/.../AnalyzeViewModelTest.kt`: pass `SavedStateHandle()` to the VM constructor; replace any `errorKey` assertions with `error`/`AnalyzeError.GenerationFailed`. Account for the new `onStart { init() }` emission ordering (availability flips after first collect).

## Verify
```
./gradlew :core:ai:compileDebugKotlinAndroid \
  :feature:aianalysis:testDebugUnitTest \
  :feature:sync:testDebugUnitTest
```
All green. Do not touch other modules.

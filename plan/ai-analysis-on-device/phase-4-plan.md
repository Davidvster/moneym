# Phase 4 — `feature/aimodels`: model manager screen

**Status:** ✅ Done — module builds android+ios, 5/5 VM tests pass. 16 strings ×4 langs. Token field rendered always-visible (not collapsible — no extra UiState). VM not yet Koin-registered (Phase 6). `ai_models_verifying` string unused until repo exposes a verifying state.
**Depends on:** Phase 3 (`LlmModelRepository`)

## Goal
Screen to browse / download / cancel / delete / activate models, show progress, and enter an
optional HF token.

## Module setup
- Register `:feature:aimodels` in `settings.gradle.kts`.
- `feature/aimodels/build.gradle.kts` modeled on `feature/about/build.gradle.kts`. Deps:
  `data/llmmodels`, `core/model`, `core/common`, `core/designsystem`, `core/ui`, `core/navigation`,
  compose, lifecycle-viewmodel, koin-compose-viewmodel, navigation3, kotlinx-serialization.
  commonTest: kotlin-test, `core/testing`, coroutines-test, turbine.

## Types (`commonMain`)
- `AiModelsUiState`: `models: List<ModelRowUi>`, `hfToken: String`, `tokenSaved: Boolean`, `error: AiModelsError?`.
  - `ModelRowUi`: `id, displayNameKey, sizeLabel, status: ModelStatus` where
    `ModelStatus = NotDownloaded | Downloading(progress) | Downloaded | Active`.
- `AiModelsIntent`: `Download(id)`, `Cancel(id)`, `Delete(id)`, `SetActive(id)`,
  `HfTokenChanged(text)`, `SaveToken`, `ClearError`.
- `AiModelsViewModel` (intent-only, single `StateFlow`): observes `repository.observeModels()`,
  maps to `ModelRowUi`. `onIntent` dispatches to repository (download/cancel/delete/setActive) in
  `viewModelScope`; token via `repository.setHfToken`. Errors → `error` state. **Public** (Koin).

## UI (`commonMain`)
- `AiModelsScreen` + `@Serializable data object AiModelsKey : ModalKey` +
  `fun EntryProviderScope<NavKey>.aiModelsEntry(onBack, metadata)`.
- Reuse `ScreenHeader`, `MmCard`, `MmButton`, `MmField`, a progress indicator. Display name resolved
  from a `stringResource` chosen by `displayNameKey` (map id→Res.string in the screen).
- Rows: name + size + trailing control by status (Download button / progress + Cancel / Active radio
  + Delete). Collapsible HF-token `MmField` + Save.
- `@Preview` with fake state (preview-only literals allowed).

## Strings (`composeResources/values{,-de,-es,-it}/strings.xml`)
Title, model display names (per id), Download/Cancel/Delete/Active/Save labels, token field
label/hint, error + verifying messages. No literals in Kotlin.

## Tests (`commonTest`)
- `AiModelsViewModelTest` (FakeLlmModelRepository + Turbine): models map to rows; Download intent
  triggers repo; SetActive marks active row; token save reflected; error surfaces + clears.

## Skills
`feature-module` (gradle/DI/nav/source layout), `viewmodel-state`, `ui-patterns`, `testing`.

## Verify
`./gradlew :feature:aimodels:compileDebugKotlinAndroid :feature:aimodels:compileKotlinIosSimulatorArm64 :feature:aimodels:testDebugUnitTest`

## Commit
`feat(feature-aimodels): model manager screen (download/activate/delete + HF token)`

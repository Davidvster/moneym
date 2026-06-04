# Phase 5 — `feature/aianalysis`: engine switcher

**Status:** Not started
**Depends on:** Phase 1 (registry), Phase 3 (repo for active-model check)

## Goal
Let the user pick the AI engine in the analysis screen; gracefully handle the local engine having no
downloaded/active model.

## Tasks
1. `AnalyzeUiState.kt`:
   - add `engines: List<AiEngineOption> = emptyList()` where
     `data class AiEngineOption(val id: AiEngineId, val available: Boolean, val needsDownload: Boolean)`.
   - add `selectedEngine: AiEngineId? = null` and `needsModelDownload: Boolean = false`.
   - `AnalyzeIntent`: add `data class EngineChanged(val id: AiEngineId)` and `data object OpenModelManager`
     (the screen turns the latter into the `onManageModels` nav callback).
2. `AnalyzeViewModel`:
   - Replace `engine: AiEngine` ctor param with `registry: AiEngineRegistry`. Keep `appSettings`.
   - `init`: compute `registry.availabilities()` (on IO), build `engines` list (only ids present in
     registry), load persisted selection from `PrefKeys.AI_ENGINE_ID` (default = first AVAILABLE, else
     first). For `LOCAL_LLM`, `needsDownload = availability == DOWNLOADABLE`.
   - `EngineChanged`: persist to `PrefKeys.AI_ENGINE_ID`, update state, recompute `needsModelDownload`.
   - `sendMessage`: resolve `registry.byId(selectedEngine)`. If null or (LOCAL_LLM &&
     availability != AVAILABLE) → set `needsModelDownload = true` and return (no generation). Else
     stream as today via that engine.
   - `supportsTools` now read from the *selected* engine.
3. `AnalyzeScreen`:
   - Add an engine selector row above the grounding row (reuse `MmSegmented` if ≤3 options, else a
     dropdown). Labels from new string resources keyed by `AiEngineId`.
   - When `needsModelDownload` → show a notice + `MmButton` "Download a model" calling
     `onIntent(OpenModelManager)`.
   - `analyzeEntry` + `AnalyzeScreen` gain `onManageModels: () -> Unit`; map `OpenModelManager`
     intent (collected via a one-shot or handled in screen) to `onManageModels`. Simrlest: handle
     `OpenModelManager` by exposing a callback the screen invokes when that intent is sent — keep VM
     pure by having the screen call `onManageModels` directly from the button instead of routing the
     nav intent through the VM (nav is a UI concern). Prefer: button calls `onManageModels()` directly;
     drop `OpenModelManager` intent if not needed.
4. Strings (`feature/aianalysis/composeResources/values{,-de,-es,-it}`): engine labels (Gemini Nano /
   Apple Intelligence / On-device model), "engine unavailable" notice, "download a model" button.

## Tests (`commonTest`)
- Update `AnalyzeViewModelTest`: construct with a fake registry (fake engines incl. a LOCAL_LLM whose
  availability is DOWNLOADABLE). Assert: engine list built; EngineChanged persists + switches;
  selecting unavailable LOCAL_LLM + SendMessage → `needsModelDownload=true`, no message appended;
  switching to an AVAILABLE engine streams.

## Skills
`viewmodel-state`, `ui-patterns`, `testing`.

## Verify
`./gradlew :feature:aianalysis:compileDebugKotlinAndroid :feature:aianalysis:compileKotlinIosSimulatorArm64 :feature:aianalysis:testDebugUnitTest`

## Commit
`feat(feature-aianalysis): engine switcher + needs-download state`

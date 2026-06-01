# Phase 5b — Overview AI button + Sparkles icon + availability gating + final cross-platform build

**Status:** DONE (commit 8e5d6a8). All 5 verify commands green: feature:overview compile, composeApp:assembleDebug, iOS link, xcodebuild iosApp, core:ai + feature:aianalysis tests.

## Goal
Add the AI button to the Overview header (right of the month/year picker), shown only when the on-device engine is available, that opens the Analyze screen for the currently selected period. Then verify the whole app builds on Android AND iOS.

## Context (verified)
- `AnalyzeKey(year: Int, month: Int) : ModalKey` + `analyzeEntry(onBack, metadata)` exist (Phase 5a) and are registered in `MainNav.kt`.
- Overview header picker Row is in `feature/overview/.../components/OverviewHeader.kt` (the bottom Row with ChevronLeft / period Box / ChevronRight). Add the AI `MmIconButton` after the ChevronRight branch, inside that Row.
- `OverviewScreen.kt`: `overviewEntry(onTabSelected, metadata)` → `OverviewScreen` → `OverviewContent` → `OverviewHeader`. `OverviewContent` already computes `currentPeriod`.
- `OverviewViewModel` is registered via `viewModelOf(::OverviewViewModel)` — adding a constructor dep that exists in the Koin graph needs no DI change.
- Icon system: enum `Icon` in `core/model/.../Icon.kt`; vectors in `core/ui/.../MmIcons.kt` (each a `by lazy ImageVector.Builder(...).apply { addPath(addPathNodes("..."), ...) }`), dispatched by `MmIcons.forIcon(icon)` `when`; `Icon.imageVector` extension in `core/ui/.../Icon.kt`.

## Tasks

1. **Add a Sparkles icon**:
   - `core/model/.../Icon.kt`: add `Sparkles("sparkles")` to the enum.
   - `core/ui/.../MmIcons.kt`: add a private `sparkles` `ImageVector` (mirror the existing `by lazy ImageVector.Builder` style; a 24×24 four-point sparkle, e.g. main star path `M12 3 L13.6 9.2 L19.5 11 L13.6 12.8 L12 19 L10.4 12.8 L4.5 11 L10.4 9.2 Z` plus a small accent star like `M18 4 L18.7 6 L20.7 6.7 L18.7 7.4 L18 9.4 L17.3 7.4 L15.3 6.7 L17.3 6 Z`, stroked round like the others). Add `Icon.Sparkles -> sparkles` to `forIcon`.

2. **`OverviewUiState.kt`**: add `val aiAvailable: Boolean = false` to `OverviewUiState`.

3. **`OverviewViewModel.kt`**:
   - Constructor: add `private val aiEngine: AiEngine` (import `com.dv.moneym.core.ai.AiEngine`, `AiAvailability`).
   - Add `private val _aiAvailable = MutableStateFlow(false)`; in `init()` launch `_aiAvailable.value = runCatching { aiEngine.availability() == AiAvailability.AVAILABLE }.getOrDefault(false)`.
   - Fold into emitted state: add `.combine(_aiAvailable) { s, avail -> s.copy(aiAvailable = avail) }` to the existing state chain (after the `_uiBooleans` combine).

4. **`feature/overview/build.gradle.kts`**: add `implementation(projects.core.ai)` (commonMain deps).

5. **`OverviewHeader.kt`**: add params `aiAvailable: Boolean = false` and `onAnalyzeClick: () -> Unit = {}`. After the `ChevronRight` if/else branch, inside the bottom Row, add:
   ```kotlin
   if (aiAvailable) {
       MmIconButton(
           icon = Icon.Sparkles.imageVector,
           size = MM.dimen.padding_4x,
           onClick = onAnalyzeClick,
           contentDescription = stringResource(Res.string.overview_analyze_cd),
       )
   }
   ```
   (Match the actual `MmIconButton` param names — if it has no `contentDescription` param, drop it. Place the button so it sits to the right of the picker; a `Spacer(Modifier.weight(1f))` before it is acceptable if the Row needs to push it to the trailing edge — check the existing Row layout and keep it visually balanced.)

6. **`OverviewScreen.kt`**:
   - `overviewEntry`: add param `onAnalyze: (year: Int, month: Int) -> Unit = { _, _ -> }`; pass to `OverviewScreen` → `OverviewContent`.
   - In `OverviewContent`, compute a representative `(year, month)` from `currentPeriod`:
     - `OverviewPeriod.Month` → `yearMonth.year, yearMonth.monthNumber`
     - `OverviewPeriod.Year` → `year, 1`
     - `OverviewPeriod.DateRange` → `startYear, startMonth`
   - Pass to `OverviewHeader(aiAvailable = state.aiAvailable, onAnalyzeClick = { onAnalyze(year, month) }, ...)`.

7. **`feature/overview` strings**: add `overview_analyze_cd` (content description, e.g. "Analyze with AI") to `values{,-de,-es,-it}/strings.xml`.

8. **`MainNav.kt`**: in the existing `overviewEntry(...)` call, add `onAnalyze = { year, month -> tabBackStack.push(AnalyzeKey(year, month)) }`. Import `com.dv.moneym.feature.aianalysis.AnalyzeKey` (already imported analyzeEntry in 5a; add the Key import if missing).

## Constraints
- Keep Overview's intent-only VM contract; navigation is a callback (like `onTabSelected`), not a VM intent — the VM only supplies `aiAvailable`.
- No user-visible literal in Kotlin — `overview_analyze_cd` via resource.
- Kotlin conventions; no TODO placeholders.

## Verify (final cross-platform)
```bash
cd /Users/davidvalic/Developer/MoneyM/.claude/worktrees/ai-analysis
./gradlew :feature:overview:compileDebugKotlinAndroid
./gradlew :composeApp:assembleDebug
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug \
  -destination "generic/platform=iOS Simulator" build CODE_SIGNING_ALLOWED=NO
./gradlew :core:ai:testDebugUnitTest :feature:aianalysis:testDebugUnitTest
```
All must pass. Report files + full build/test results.

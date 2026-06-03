# Phase 7b — Previews for feature screens

Add `@Preview` to feature screens missing one. Rule: preview the **stateless content composable** (the inner `*Content` / `*Body` / `ManageCategoriesScreen` / `CurrencyStep`-style fn that takes `state` + callbacks), NOT the top-level `koinViewModel()`-bound `*Screen`. Construct a literal `<Screen>UiState(...)` (preview-only hardcoded dates/values OK). Co-located private `@Composable fun <Name>Preview()`, `@Preview` import `androidx.compose.ui.tooling.preview.Preview`, wrap in `MoneyMTheme { … }`, callbacks `{}`, `onIntent = {}`.

**Do NOT refactor production to extract a content composable.** If a screen has no stateless inner composable (e.g. it inlines everything in the VM-bound `*Screen`, or composes multiple VMs like `SettingsScreen`), either preview an existing stateless sub-composable in the file, or SKIP it and note why. Some screens may already have a `*ContentPreview` — skip those.

Read each screen file first to find the stateless composable + its `UiState` shape + any `@OptIn` needed.

## This builder (Group 7b-1): 11 screens
- `feature/about/.../AboutScreen.kt`
- `feature/aianalysis/.../AnalyzeScreen.kt` (preview the message-list/content area with a sample `AnalyzeUiState` incl. a couple `ChatMessage`s)
- `feature/infopage/.../InfoPageScreen.kt`
- `feature/onboarding/.../currency/OnboardingCurrencyStep.kt` (preview `CurrencyStep`)
- `feature/onboarding/.../restore/OnboardingRestoreScreen.kt`
- `feature/onboarding/.../security/OnboardingSecurityStep.kt`
- `feature/security/.../setup/PinSetupScreen.kt` (preview `PinSetupContent`)
- `feature/security/.../unlock/PinUnlockScreen.kt` (preview its content composable)
- `feature/sync/.../PendingDeletionsScreen.kt`
- `feature/sync/.../SyncSettingsScreen.kt` (preview `SyncSettingsContent`)
- `feature/categories/.../list/CategoryListScreen.kt` (preview `ManageCategoriesScreen` with sample categories)

Build sample domain objects (`Category`, `Account`, `Money`, etc.) from `core:model`. Import classes, no FQN. No comments unless non-obvious. Only add preview functions.

## Verify (compile the touched modules)
```
./gradlew :feature:about:compileDebugKotlinAndroid :feature:aianalysis:compileDebugKotlinAndroid \
  :feature:infopage:compileDebugKotlinAndroid :feature:onboarding:compileDebugKotlinAndroid \
  :feature:security:compileDebugKotlinAndroid :feature:sync:compileDebugKotlinAndroid \
  :feature:categories:compileDebugKotlinAndroid
```
All compile. Report which screens got previews and which were skipped (+reason).

# Phase 8 Plan: AI Widget Builder + Constrained A2UI Renderer

## Goal

Let users create custom overview widgets by writing a prompt, generating constrained A2UI JSON with the existing AI engine stack, previewing the rendered widget, saving it to `OverviewRepository`, and rendering enabled saved widgets as overview blocks.

Important safety decision: generate and store app-rendered JSON only. Do not generate Kotlin, Compose code, scripts, expressions, network calls, or arbitrary executable content.

Commit after verification.

## Expected Files/Modules

- `feature/overview/src/commonMain/kotlin/com/dv/moneym/feature/overview/OverviewAiWidgetBuilderScreen.kt`
- `feature/overview/src/commonMain/kotlin/com/dv/moneym/feature/overview/OverviewAiWidgetBuilderViewModel.kt`
- `feature/overview/src/commonMain/kotlin/com/dv/moneym/feature/overview/OverviewAiWidgetBuilderUiState.kt`
- `feature/overview/src/commonMain/kotlin/com/dv/moneym/feature/overview/a2ui/...`
- `feature/overview/src/commonMain/kotlin/com/dv/moneym/feature/overview/components/OverviewPeriodBody.kt`
- `feature/overview/src/commonMain/kotlin/com/dv/moneym/feature/overview/page/OverviewPageViewModel.kt`
- `feature/overview/src/commonMain/kotlin/com/dv/moneym/feature/overview/page/OverviewPageUiState.kt`
- `feature/settings/src/commonMain/kotlin/com/dv/moneym/feature/settings/overview/OverviewSettingsScreen.kt` only if create/edit navigation labels need adjustment.
- `shared/src/commonMain/kotlin/com/dv/moneym/MainNav.kt`
- `shared/src/commonMain/kotlin/com/dv/moneym/di/FeatureModules.kt`
- `feature/overview/src/commonMain/composeResources/values*/strings.xml`
- `feature/overview/src/commonTest/kotlin/com/dv/moneym/feature/overview/...`
- `plan/overview-ai/status.md`

## Navigation

- Add `OverviewAiWidgetBuilderKey(widgetId: Long? = null)` in `feature/overview`.
- Wire `overviewSettingsEntry(onOpenAiWidgetBuilder = { id -> tabBackStack.push(OverviewAiWidgetBuilderKey(id)) })`.
- Add builder entry with `metadata = modalTransitionMeta`.
- Create mode uses `widgetId = null`; edit mode loads an existing saved widget by id.

## Builder Screen

- Composable should be a real tool screen, not a marketing page:
  - screen header with back
  - prompt field
  - title field or generated title field
  - generate button
  - preview area
  - save button
  - validation/generation error states
- ViewModel public API must remain `fun onIntent(intent: OverviewAiWidgetBuilderIntent)`.
- State should own prompt/title/json/preview/error/loading/saving flags; composables stay dumb.
- Save should upsert an `OverviewAiWidget` with:
  - title
  - prompt
  - generated A2UI JSON
  - enabled = true for new widgets
  - existing enabled/sortOrder retained on edit
  - timestamps from `AppClock`
  - `lastGeneratedAt` and `lastGenerationEngineId` when generated

## AI Generation

- Reuse existing AI infrastructure:
  - `AiEngineRegistry`
  - selected engine preference from `PrefKeys.AI_ENGINE_ID`
  - response language pattern from AI analysis if useful
- For Phase 8, keep the generation prompt grounded by an app-owned overview context and data catalog. Do not use tool tags for this screen.
- The prompt sent to the model must demand JSON only and include:
  - allowed component types
  - allowed binding keys
  - depth/child/count limits
  - instruction not to include markdown/code fences
- If the selected engine is unavailable, show a localized error and do not save.
- If the model wraps JSON in markdown fences, strip fences before validation; do not attempt to evaluate code.

## Overview Widget Context

Build a dedicated use case in `feature/overview` for the data available to widgets:

- period label/type
- selected account/wallet label and currency when selected
- transaction filter type and selected categories
- precomputed metrics from `OverviewPageUiState`:
  - income
  - expenses
  - net
  - daily/monthly averages
  - category expense breakdown
  - category income breakdown
  - budget progress
  - monthly spend/income/net series
  - cumulative spend series
- The renderer should bind only to this bounded context; it must not query repositories directly.

## A2UI Model, Validation, and Rendering

Add a small app-owned A2UI subset in `feature/overview/a2ui`.

Allowed component types:

- `card`
- `column`
- `row`
- `text`
- `moneyMetric`
- `percentage`
- `progress`
- `barList`
- `categoryList`
- `spacer`
- `divider`

Validation requirements:

- JSON parses.
- Root is an allowed component.
- Component types are allowlisted.
- Bindings reference allowlisted overview data keys only.
- Reject unknown fields that look like code/execution (`code`, `script`, `expression`, `eval`, `url`, `network`, `onClick`, etc.).
- Enforce depth limit, child count limit, and total node limit.
- Text length and title length have reasonable caps.
- Invalid saved widgets render a localized invalid-widget card, never raw JSON.

Renderer requirements:

- Render with existing MoneyM design system (`MmCard`, typography, colors).
- Do not render nested cards inside cards if avoidable; root `card` may contain unframed rows/columns.
- `moneyMetric` formats from bound numeric values with the current overview currency.
- `barList` and `categoryList` support bounded top-N lists from context.
- Add light/dark previews with valid sample JSON and invalid sample JSON.

## Overview Rendering

- `OverviewPeriodBody` should render `OverviewResolvedBlock.AiWidget` using the A2UI renderer.
- Pass enough widget context from `OverviewPageUiState` to render bindings without recomputing data in Compose.
- Keep built-in cards visually unchanged.

## Tests

Add focused tests for:

- Prompt/context builder includes allowed component catalog and key overview data.
- Validator accepts a valid sample.
- Validator rejects:
  - unknown component type
  - unknown binding
  - too-deep tree
  - too many children / too many total nodes
  - raw code/expression/network fields
- Builder ViewModel:
  - generate success with fake AI engine
  - generate invalid JSON failure
  - save new widget upserts repository
  - edit existing widget loads and saves
- Overview block rendering:
  - saved enabled AI widget appears as an overview block
  - disabled widget does not render

## Verification

Run:

```bash
./gradlew --no-configuration-cache :feature:overview:testDebugUnitTest :feature:settings:testDebugUnitTest
```

Then run:

```bash
git diff --check
```

If implementation touches shared navigation/DI, also run:

```bash
./gradlew --no-configuration-cache :shared:compileDebugKotlinAndroid
```

## Status Update Required

After implementation and verification, update `plan/overview-ai/status.md` with:

- Phase 8 status and verification command results.
- The commit hash after committing.

Commit only Phase 8 changes.

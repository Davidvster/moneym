# Phase 1 — small UI fixes (tasks 3, 4, 5)

Three independent edits in `feature/categories`, `feature/settings`, `core/designsystem`. No behavior changes beyond described. Do NOT touch anything else.

## Task 3 — import data checkboxes black/white (currently purple)

File: `feature/settings/src/commonMain/kotlin/com/dv/moneym/feature/settings/overview/importdata/ImportDataScreen.kt`

Two `Checkbox(...)` calls use Material3 default colors (primary = purple): the "select all" row (~L160) and per-transaction row (~L450).

Add a `colors = CheckboxDefaults.colors(...)` argument to BOTH, driven by `MM.colors` (import `com.dv.moneym.core.designsystem.MM` if not already, and `androidx.compose.material3.CheckboxDefaults`):
- `checkedColor = MM.colors.text`
- `checkmarkColor = MM.colors.bg`
- `uncheckedColor = MM.colors.border`

Goal: monochrome checkbox that adapts to light/dark (text near-black in light, near-white in dark). No purple/accent tint.

## Task 4 — category edit save button stops shrinking on full sheet expand

Root cause: in `NewCategorySheet`, the `Column` holds Header + `NewCategorySheetBody` (which has `Modifier.verticalScroll(...)` but NO `weight`) + `NewCategorySaveButton`. When the ModalBottomSheet expands fully, the scroll child measures against full available height and squeezes the save button.

Files:
1. `feature/categories/src/commonMain/kotlin/com/dv/moneym/feature/categories/list/components/NewCategorySheetBody.kt`
   - Add `modifier: Modifier = Modifier` parameter to `NewCategorySheetBody`.
   - Apply it to the root `Column`: `Column(modifier = modifier.verticalScroll(rememberScrollState()).padding(...))` (keep existing verticalScroll + padding, just prepend `modifier`).
2. `feature/categories/src/commonMain/kotlin/com/dv/moneym/feature/categories/list/CategoryListScreen.kt`
   - In `NewCategorySheet` (~L195), pass `modifier = Modifier.weight(1f)` to the `NewCategorySheetBody(...)` call. (The `Column` in `NewCategorySheet` is the parent — body gets the remaining space, button keeps fixed `Lg` height.)

## Task 5 — add 12 palette colors, reorder by color wheel (27 total)

Replace the color lists in BOTH files with the identical 27-entry, hue-ordered list below (red→orange→yellow→green→teal→cyan→blue→indigo→violet→magenta, gray last). Drop the old per-line `// Health`/`// Entertainment` comments — order no longer maps to them.

1. `feature/categories/src/commonMain/kotlin/com/dv/moneym/feature/categories/list/CategoryListScreen.kt` — `private val CATEGORY_PALETTE` (~L245). This is what the picker renders.
2. `core/designsystem/src/commonMain/kotlin/com/dv/moneym/core/designsystem/CategoryColor.kt` — `val defaultCategoryColors` (L7). Safe to reorder: only used as `.first()` fallback in wallet screens; not indexed by seeding.

New list (use exactly):
```kotlin
    Color(0xFFF4743B),
    Color(0xFFC97A4F),
    Color(0xFFD88B33),
    Color(0xFFFF9F1C),
    Color(0xFFB89148),
    Color(0xFFF4B400),
    Color(0xFFA8C63A),
    Color(0xFF7A9572),
    Color(0xFF2EA84F),
    Color(0xFF4A8E5C),
    Color(0xFF3F9E70),
    Color(0xFF12B5A5),
    Color(0xFF1CA7C9),
    Color(0xFF4F8694),
    Color(0xFF3A82A5),
    Color(0xFF5A7BA8),
    Color(0xFF2D6CDF),
    Color(0xFF6B5BC4),
    Color(0xFF5A3FC0),
    Color(0xFF8B6FB0),
    Color(0xFF9B51E0),
    Color(0xFFB07089),
    Color(0xFFE84B8A),
    Color(0xFFD14C7A),
    Color(0xFFC2566B),
    Color(0xFFE63946),
    Color(0xFF8A8A8A),
```

## Verify
`./gradlew :feature:categories:compileDebugKotlinAndroid :feature:settings:compileDebugKotlinAndroid :core:designsystem:compileDebugKotlinAndroid` should compile. Final full Android build done by orchestrator.

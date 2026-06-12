# Phase 1 — budgets bottom padding + AI analysis history "+" to bottom button

## Task 1: Budget list bottom button missing navigationBarsPadding

File: `feature/budgets/src/commonMain/kotlin/com/dv/moneym/feature/budgets/list/BudgetListScreen.kt` (lines ~147-151)

The bottom "New budget" `Box` has:
```kotlin
Box(
    modifier = Modifier.padding(
        horizontal = MM.dimen.padding_2_5x,
        vertical = MM.dimen.padding_2x,
    ),
)
```
Add `.navigationBarsPadding()` after the padding, exactly matching `BudgetCreateScreen.kt:286-292`:
```kotlin
Box(
    modifier = Modifier
        .padding(
            horizontal = MM.dimen.padding_2_5x,
            vertical = MM.dimen.padding_2x,
        )
        .navigationBarsPadding(),
)
```
Add the `androidx.compose.foundation.layout.navigationBarsPadding` import if missing.

## Task 2: AI analysis history — move "+" from top-right to bottom button

File: `feature/aianalysis/src/commonMain/kotlin/com/dv/moneym/feature/aianalysis/history/AnalyzeHistoryScreen.kt`

Current (lines ~82-95): `ScreenHeader(trailingContent = { MmIconButton(Icon.Plus, onClick = { onIntent(AnalyzeHistoryIntent.NewChat); onBack() }) })`.

Changes:
1. Remove `trailingContent` block from `ScreenHeader` (and now-unused imports, e.g. `MmIconButton` if unused elsewhere in file).
2. At the bottom of the screen's root Column (after the list content), add — copy the BudgetListScreen pattern exactly:
```kotlin
Box(
    modifier = Modifier
        .padding(
            horizontal = MM.dimen.padding_2_5x,
            vertical = MM.dimen.padding_2x,
        )
        .navigationBarsPadding(),
) {
    MmButton(
        text = stringResource(Res.string.analyze_history_new_chat),
        onClick = {
            onIntent(AnalyzeHistoryIntent.NewChat)
            onBack()
        },
        variant = MmButtonVariant.Primary,
        fullWidth = true,
        leadingIcon = Icon.Plus.imageVector,
    )
}
```
Check the screen's root layout: list content must take `Modifier.weight(1f)` so the button anchors at the bottom (see BudgetListScreen for reference). If the list/empty state doesn't have weight(1f), add it.

Imports: `MmButton`, `MmButtonVariant`, `navigationBarsPadding`, and the new string key import (compose resources need per-key imports — `import moneym.feature.aianalysis.generated.resources.analyze_history_new_chat` style; check existing imports in the file for the exact package).

3. New string key `analyze_history_new_chat` in `feature/aianalysis/src/commonMain/composeResources/`:
   - `values/strings.xml` (English): `<string name="analyze_history_new_chat">New analysis</string>`
   - AND all 27 locale dirs: `values-ar values-cs values-da values-de values-es values-et values-fi values-fr values-hi values-hr values-hu values-is values-it values-ja values-lt values-lv values-mk values-nb values-nl values-pl values-pt values-ru values-sk values-sl values-sv values-tr values-vi values-zh` — translate the value appropriately per locale (de/es/it carefully; rest best-effort). Look at the existing `analyze_history_new_chat_cd` translations in each locale for wording consistency.

## Verification

```bash
./gradlew :feature:budgets:compileDebugKotlinAndroid :feature:aianalysis:compileDebugKotlinAndroid --no-configuration-cache
```
Both must pass. Do NOT commit — the orchestrator commits.

## Conventions
- No comments in code.
- Match surrounding code style.

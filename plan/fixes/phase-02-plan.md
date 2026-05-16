# Phase 2: Reorder Categories + i18n

## Problem
1. Drag-and-drop only moves one step at a time instead of continuously following the pointer to the hovered position. The current implementation uses a fixed `itemHeight = 56f` and accumulates `dragOffsetY`, but the logic resets `dragOffsetY = 0f` on each step — this causes single-step movement only.
2. Hardcoded strings in `CategoryListScreen.kt` need to use the resource system for i18n.

## Files to modify
- `feature/categories/src/commonMain/kotlin/com/dv/moneym/feature/categories/ui/CategoryListScreen.kt` — fix drag logic and replace hardcoded strings with `stringResource`
- `feature/categories/src/commonMain/composeResources/values/strings.xml` — add missing string keys
- `feature/categories/src/commonMain/composeResources/values-de/strings.xml` — add German translations
- `feature/categories/src/commonMain/composeResources/values-es/strings.xml` — add Spanish translations
- `feature/categories/src/commonMain/composeResources/values-it/strings.xml` — add Italian translations

## Implementation steps

### Fix drag-and-drop reordering

The current logic:
```kotlin
dragOffsetY += dragAmount.y
val targetIndex = (index + (dragOffsetY / itemHeight).toInt()).coerceIn(0, categories.size - 1)
if (targetIndex != draggingIndex) {
    onReorder(draggingIndex, targetIndex)
    draggingIndex = targetIndex
    dragOffsetY = 0f  // ← BUG: resets accumulated offset, so only 1 step
}
```

Fix: Track `dragOffsetY` relative to where the **dragging item's current position** is, not the original position. Use `LazyListState` to get actual item heights. Simpler approach: instead of resetting to 0, keep cumulative offset relative to dragging item start:

Replace the drag logic with position-tracking using `LazyListState`:
```kotlin
onDrag = { change, dragAmount ->
    change.consume()
    dragOffsetY += dragAmount.y
    val itemHeight = listState.layoutInfo.visibleItemsInfo
        .firstOrNull { it.index == draggingIndex }?.size?.toFloat() ?: 64f
    val stepsFloat = dragOffsetY / itemHeight
    val steps = stepsFloat.toInt()
    if (steps != 0) {
        val targetIndex = (draggingIndex + steps).coerceIn(0, categories.size - 1)
        if (targetIndex != draggingIndex) {
            onReorder(draggingIndex, targetIndex)
            draggingIndex = targetIndex
            dragOffsetY -= steps * itemHeight
        }
    }
}
```

This accumulates offset and only moves when a full item height has been crossed, then subtracts the moved amount — so continuous dragging moves continuously.

### Internationalize strings

Hardcoded strings to replace:
- `"Categories"` (ScreenHeader title) → `stringResource(Res.string.categories_title)`
- `"New"` (button) → `stringResource(Res.string.categories_new_button)`
- `"Expense"`, `"Income"` (tab labels) → `stringResource(Res.string.categories_tab_expense)`, `stringResource(Res.string.categories_tab_income)`
- `"${categories.size} ${...} categories · long-press to reorder"` → use format string
- `"Edit category"` / `"New category"` (bottom sheet header) → string resources
- `"Name"`, `"Color"`, `"Icon"` (labels) → string resources
- `"Category name"` (placeholder) → string resource
- `"Delete category"`, `"Save changes"`, `"Create category"` (button labels) → string resources
- `"Delete \"$categoryName\"?"` (dialog title) → string resource with placeholder
- `"This will remove..."` (dialog body) → string resource
- `"Cancel"`, `"Delete"` (dialog buttons) → string resources
- `"New ${...} category"` (bottom button) → use format strings

Add all new string keys to `values/strings.xml` with English defaults, and to all locale variants.

## Acceptance criteria
- [ ] Long-press and drag on a category item in the list continuously moves it as the user's finger/cursor moves — not just one position at a time
- [ ] Releasing the drag keeps the item in its final position
- [ ] Category list screen title, tab labels, button labels, sheet headers, and dialog texts are all string resources (no hardcoded English)
- [ ] German, Spanish, and Italian translations exist for the new strings

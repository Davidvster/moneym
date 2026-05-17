# Phase 6 — Split Large Composables in TransactionEdit & CategoryListScreen

## Goal
Split `TransactionEditContent` (~290 lines) and `ManageCategoriesScreen` (~180 lines) and `NewCategorySheet` (~260 lines) into semantically named sub-composables, each under 100 lines. Also extract `MonthPickerContent` since it's near-identical in both `TransactionListScreen.kt` and `OverviewScreen.kt`.

## Files to Edit

- `/Users/davidvalic/Developer/MoneyM2/feature/transactionEdit/src/commonMain/kotlin/com/dv/moneym/feature/transactionedit/ui/TransactionEditScreen.kt`
- `/Users/davidvalic/Developer/MoneyM2/feature/categories/src/commonMain/kotlin/com/dv/moneym/feature/categories/ui/CategoryListScreen.kt`

## Detailed Changes

### TransactionEditScreen.kt

`TransactionEditContent` (~290 lines) — extract these private sub-composables:

**`TransactionEditModalHeader`** — the top bar with close button, title, and optional delete button:
```kotlin
@Composable
private fun TransactionEditModalHeader(
    isEditMode: Boolean,
    onDismiss: () -> Unit,
    onDeleteRequested: () -> Unit,
)
```

**`AmountDisplay`** — the big amount display with hidden BasicTextField:
```kotlin
@Composable
private fun AmountDisplay(
    amountText: String,
    formattedAmount: String,
    currencyCode: String,
    amountValue: Double,
    focusRequester: FocusRequester,
    onAmountChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
)
```

**`CategoryPicker`** — the "CATEGORY" label + FlowRow of category chips:
```kotlin
@Composable
private fun CategoryPicker(
    categories: List<Category>,
    selectedCategoryId: CategoryId?,
    onCategorySelected: (CategoryId) -> Unit,
)
```

**`TransactionEditSaveBar`** — the pinned save button bar with divider line:
```kotlin
@Composable
private fun TransactionEditSaveBar(
    isEditMode: Boolean,
    isSaving: Boolean,
    onSave: () -> Unit,
)
```

`TransactionEditContent` becomes a coordinator calling `TransactionEditModalHeader`, `TypeToggleBar`, `AmountDisplay`, date field, note field, `CategoryPicker`, and `TransactionEditSaveBar`. It should stay under 100 lines.

### CategoryListScreen.kt

**`ManageCategoriesScreen`** (~180 lines) — extract:

**`CategoryListHeader`** — ScreenHeader + segment tabs + hint text:
```kotlin
@Composable
private fun CategoryListHeader(
    activeTab: CategoryTab,
    categoriesCount: Int,
    onBack: () -> Unit,
    onSetTab: (CategoryTab) -> Unit,
    onNewCategoryClick: () -> Unit,
)
```

**`DraggableCategoryList`** — the LazyColumn with drag-reorder logic:
```kotlin
@Composable
private fun DraggableCategoryList(
    categories: List<Category>,
    onReorder: (Int, Int) -> Unit,
    onCategoryClick: (Category) -> Unit,
    modifier: Modifier = Modifier,
)
```

**`CategoryRow`** — single category row in the list:
```kotlin
@Composable
private fun CategoryRow(
    cat: Category,
    isDragging: Boolean,
    onPointerInput: Modifier,
    onClick: () -> Unit,
    isLast: Boolean,
)
```

**`NewCategorySheet`** (~260 lines) — already exists as a function, but its body is too large. Extract:

**`CategoryPreviewChip`** — the live preview chip at top of sheet:
```kotlin
@Composable
private fun CategoryPreviewChip(
    name: String,
    selectedColor: Color,
    selectedIconKey: String,
)
```

**`ColorPickerSection`** — the color label + FlowRow of palette swatches + custom swatch:
```kotlin
@Composable
private fun ColorPickerSection(
    palette: List<Color>,
    selectedColor: Color,
    customColor: Color?,
    onColorSelected: (Color) -> Unit,
    onCustomColorClick: () -> Unit,
)
```

**`IconPickerSection`** — the icon label + FlowRow of icon options:
```kotlin
@Composable
private fun IconPickerSection(
    iconOptions: List<String>,
    selectedIconKey: String,
    selectedColor: Color,
    onIconSelected: (String) -> Unit,
)
```

`NewCategorySheet` body then calls `CategoryPreviewChip`, `MmField` for name, `ColorPickerSection`, `IconPickerSection`, optional delete button, and the pinned save button — staying under 100 lines.

## Important Constraints
- All extracted composables must be `private` (same file, unless otherwise noted).
- Preserve all state hoisting — do NOT change how state flows.
- `categoryToEdit`, `draggingIndex`, `dragOffsetY`, `showNewCategorySheet`, etc. remain in their current scope — only the rendering logic is extracted.
- After extraction, verify all functions are under 100 lines.

## Acceptance Criteria
1. `TransactionEditContent` is under 100 lines.
2. `ManageCategoriesScreen` is under 100 lines.
3. `NewCategorySheet` is under 100 lines.
4. All extracted sub-composables are under 100 lines.
5. `./gradlew :composeApp:assembleDebug` passes.
6. Visual behavior is identical.

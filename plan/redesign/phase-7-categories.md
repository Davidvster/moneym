# Phase 7 — Categories Redesign

Migrate `CategoryListScreen` and `CategoryEditScreen` to match the new design. Adds drag-to-reorder and the new-category sheet with color + icon picker.

---

## Files to modify / create

| File | Action |
|---|---|
| `feature/categories/src/commonMain/…/ui/CategoryListScreen.kt` | Full rewrite → becomes ManageCategoriesScreen |
| `feature/categories/src/commonMain/…/ui/CategoryEditScreen.kt` | Full rewrite → becomes NewCategorySheet |
| `feature/categories/src/commonMain/…/presentation/CategoryListUiState.kt` | Add `expenseCategories`, `incomeCategories`, `activeTab` |
| `feature/categories/src/commonMain/…/presentation/CategoryListViewModel.kt` | Add `reorder(from, to)`, `setTab(tab)` |
| `feature/categories/src/commonMain/…/presentation/CategoryEditUiState.kt` | Add `selectedColor`, `selectedIcon`, `previewChip` |

---

## CategoryListScreen (ManageCategoriesScreen)

```
Column {
  ScreenHeader(
    title = "Categories",
    trailing = Button(variant=ghost, size=sm, leading=Icon(plus)) { "New" }
    // → opens NewCategorySheet
  )

  Row(16dp padding) {
    MmSegmented(["Expense","Income"], activeTab)
  }

  Text(
    "${list.size} ${tab.lowercase()} categories · drag to reorder",
    caption, text3, 4 20 12dp padding
  )

  // Reorderable list
  LazyColumn(reorderState) {
    items(list, key = { it.id }) { cat ->
      Row(
        modifier = Modifier.detectReorder(reorderState),
        12 20dp padding,
        borderBottom
      ) {
        // Drag handle — 6-dot SVG
        Icon(MmIcons.dragHandle, 14dp, text3)
        CategoryIconTile(cat, 36dp, IndicatorStyle.IconTile)
        Text(cat.name, body, weight=1)
        Icon(MmIcons.chevronRight, text3)
      }
    }
  }

  Box(20dp padding) {
    MmButton("New ${tab.lowercase()} category", variant=secondary, fullWidth, leading=Icon(plus))
  }
}
```

**Drag-to-reorder:** Use `sh.calvin.reorderable:reorderable` library (KMP-compatible). On drop: `viewModel.reorder(fromIndex, toIndex)` → repository updates `order` field for all affected categories.

---

## NewCategorySheet

Rendered as `ModalBottomSheet(shape = RoundedCornerShape(topStart=20.dp, topEnd=20.dp))`.

```
Column {
  // Grabber
  Box(36×4dp, pill, borderStrong, center, 10dp padding)

  // Header
  Row(8 12 4dp padding) {
    MmIconButton(close)
    Text("New category", title3, weight=1, center)
    Spacer(40dp)
  }

  Column(scroll, 16 20 0dp padding) {
    // Live preview chip
    Row(center) {
      Row(10 18 10 12dp padding, pill, surface bg, 1dp border) {
        Box(28dp, 8dp radius, selectedColor) { Icon(selectedIcon, 16dp, white, strokeWidth=1.8) }
        Text(nameValue, body)
      }
    }

    Spacer(24dp)
    MmField(label="Name", value=nameValue, onValueChange=…)

    Spacer(20dp)
    Text("Type", caption, text2)
    Spacer(8dp)
    MmSegmented(["Expense","Income"], typeTab)

    Spacer(24dp)
    Text("Color", caption, text2)
    Spacer(12dp)
    // 15-color swatch grid (5 per row)
    FlowRow(gap=10dp) {
      palette.forEach { color ->
        Box(
          36dp, 10dp radius, color bg,
          border = if selected: "0 0 0 2dp bg, 0 0 0 4dp text" (ring effect via nested Boxes)
        ) {
          if selected: Icon(check, 16dp, white, strokeWidth=2.5)
        }
      }
    }

    Spacer(24dp)
    Text("Icon", caption, text2)
    Spacer(12dp)
    // 15-icon grid
    FlowRow(gap=10dp) {
      iconOptions.forEach { ic ->
        Box(
          44dp, 10dp radius,
          bg = if selected: selectedColor else surface,
          border = 1dp if selected: selectedColor else border
        ) {
          Icon(ic, 20dp, stroke = if selected: white else text, strokeWidth=1.6)
        }
      }
    }
  }

  // Pinned create button
  Box(1dp divider top, 16 20 0dp padding) {
    MmButton("Create category", variant=accent, size=lg, fullWidth, leading=Icon(check))
  }
}
```

**Color ring selection effect:**
```kotlin
Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(color)) {
    if (isSelected) {
        Box(Modifier.fillMaxSize().padding(2.dp).clip(RoundedCornerShape(8.dp)).background(MM.colors.bg)) {
            Box(Modifier.fillMaxSize().clip(RoundedCornerShape(6.dp)).background(color)) {
                Icon(check, …)
            }
        }
    }
}
```

**Palette (15 colors):**
`#C2566B, #8B6FB0, #4A8E5C, #4F8694, #B89148, #7A9572, #C97A4F, #5A7BA8, #B07089, #8A8A8A, #D14C7A, #6B5BC4, #3F9E70, #3A82A5, #D88B33`

**Icon options (15):**
`heart, film, car, bolt, basket, utensils, home, bag, tag, banknote, gift, sun, moon, globe, folder`

---

## CategoryEditScreen (existing → edit mode)

When tapping a category row (chevron right), open the same sheet but pre-filled. Add a "Delete category" danger button at the bottom above the save button. ViewModel determines whether it's a new or edit flow.

---

## Key implementation notes

- `reorderable` library: `sh.calvin.reorderable:reorderable:2.x`. Add to `feature/categories/build.gradle.kts`.
- `reorder(from, to)` writes new `order` ints to the DB — use a single transaction. Categories are displayed in `order` ascending.
- The `ModalBottomSheet` scrim dismisses the sheet on tap-outside.
- `MmField` for name: `onValueChange` updates ViewModel state; preview chip reflects changes live.
- Icon set for categories maps to `MmIcons` entries — same icons used in the icon picker are the same `ImageVector`s used in `CategoryIconTile`.

---

## Verification
1. Drag handle: long-press and drag reorders categories; order persists after app restart.
2. New category sheet: name field updates preview chip live.
3. Color ring selection shows the double-ring effect (bg gap + text ring).
4. Icon picker: selected icon gets category-color bg and white icon.
5. "Create category" saves and the new category appears in the list.
6. Expense/Income tab correctly filters the list.

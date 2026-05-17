# Phase 2 — Localize Hardcoded Strings in CategoryListScreen

## Goal
Replace all hardcoded display strings in `CategoryListScreen.kt` with `stringResource()` calls backed by entries in `categories/strings.xml`. All required keys already exist in the strings file.

## Files to Edit

### Kotlin UI
- `/Users/davidvalic/Developer/MoneyM2/feature/categories/src/commonMain/kotlin/com/dv/moneym/feature/categories/ui/CategoryListScreen.kt`

### String resources (add missing keys if any)
- `/Users/davidvalic/Developer/MoneyM2/feature/categories/src/commonMain/composeResources/values/strings.xml`
- `/Users/davidvalic/Developer/MoneyM2/feature/categories/src/commonMain/composeResources/values-de/strings.xml`
- `/Users/davidvalic/Developer/MoneyM2/feature/categories/src/commonMain/composeResources/values-es/strings.xml`
- `/Users/davidvalic/Developer/MoneyM2/feature/categories/src/commonMain/composeResources/values-it/strings.xml`

## Hardcoded Strings to Replace

In `CategoryListScreen.kt`:

1. `"New"` (button label in ScreenHeader trailing content)
   → `stringResource(Res.string.categories_new_button)`

2. `"Expense"` and `"Income"` in the MmSegmented options list
   → `stringResource(Res.string.categories_tab_expense)` and `stringResource(Res.string.categories_tab_income)`

3. `"${categories.size} ${if (activeTab == CategoryTab.Expense) "expense" else "income"} categories · long-press to reorder"`
   → Use: `"${categories.size} ${if (activeTab == CategoryTab.Expense) stringResource(Res.string.categories_tab_expense).lowercase() else stringResource(Res.string.categories_tab_income).lowercase()} ${stringResource(Res.string.categories_hint)}"`
   Note: `categories_hint` = `"long-press to reorder"` — this already exists in strings.xml. But the full composited sentence needs the count. Use: `"${categories.size} ${tabLabel} ${stringResource(Res.string.categories_hint)}"`

4. `"New ${if (activeTab == CategoryTab.Expense) "expense" else "income"} category"` (bottom button)
   → Use: `if (activeTab == CategoryTab.Expense) stringResource(Res.string.categories_new_expense) else stringResource(Res.string.categories_new_income)`

5. In `NewCategorySheet`:
   - `"Edit category"` → `stringResource(Res.string.categories_edit_sheet_title)`
   - `"New category"` → `stringResource(Res.string.categories_new_sheet_title)`
   - `"e.g. Groceries"` (placeholder) → `stringResource(Res.string.categories_name_placeholder)`
   - `"Delete category"` → `stringResource(Res.string.categories_delete)`
   - `"Save changes"` → `stringResource(Res.string.categories_save_changes)`
   - `"Create category"` → `stringResource(Res.string.categories_create)`
   - `"Category name"` (fallback preview text) → `stringResource(Res.string.categories_name_label)`

6. In `HsvColorPickerSheet`:
   - `"Pick color"` → add `categories_color_picker_title` = `"Pick color"` to strings.xml
   - `"Hue"` → add `categories_color_hue` = `"Hue"` to strings.xml
   - `"Saturation"` → add `categories_color_saturation` = `"Saturation"` to strings.xml
   - `"Brightness"` → add `categories_color_brightness` = `"Brightness"` to strings.xml
   - `"Hex color"` (MmField label) → add `categories_color_hex` = `"Hex color"` to strings.xml
   - `"Cancel"` in buttons → `stringResource(Res.string.categories_cancel)`
   - `"Select color"` → add `categories_color_select` = `"Select color"` to strings.xml

7. In `DeleteConfirmSheet`:
   - `"Delete \"$categoryName\"?"` → `stringResource(Res.string.categories_delete_confirm_title, categoryName)`
   - `"This will remove the category. Transactions using it may be affected."` → `stringResource(Res.string.categories_delete_confirm_body)`
   - `"Delete"` (confirm button) → `stringResource(Res.string.categories_delete_button)`

## New String Keys to Add (to all strings.xml files)

Add to `values/strings.xml`:
```xml
<string name="categories_color_picker_title">Pick color</string>
<string name="categories_color_hue">Hue</string>
<string name="categories_color_saturation">Saturation</string>
<string name="categories_color_brightness">Brightness</string>
<string name="categories_color_hex">Hex color</string>
<string name="categories_color_select">Select color</string>
```

Add corresponding translations to `values-de/strings.xml`:
```xml
<string name="categories_color_picker_title">Farbe wählen</string>
<string name="categories_color_hue">Farbton</string>
<string name="categories_color_saturation">Sättigung</string>
<string name="categories_color_brightness">Helligkeit</string>
<string name="categories_color_hex">Hex-Farbe</string>
<string name="categories_color_select">Farbe auswählen</string>
```

Add to `values-es/strings.xml`:
```xml
<string name="categories_color_picker_title">Elegir color</string>
<string name="categories_color_hue">Tono</string>
<string name="categories_color_saturation">Saturación</string>
<string name="categories_color_brightness">Brillo</string>
<string name="categories_color_hex">Color hexadecimal</string>
<string name="categories_color_select">Seleccionar color</string>
```

Add to `values-it/strings.xml`:
```xml
<string name="categories_color_picker_title">Scegli colore</string>
<string name="categories_color_hue">Tonalità</string>
<string name="categories_color_saturation">Saturazione</string>
<string name="categories_color_brightness">Luminosità</string>
<string name="categories_color_hex">Colore esadecimale</string>
<string name="categories_color_select">Seleziona colore</string>
```

## Import Changes in CategoryListScreen.kt
Add to the existing `moneym.feature.categories.generated.resources.*` imports:
```kotlin
import moneym.feature.categories.generated.resources.categories_new_button
import moneym.feature.categories.generated.resources.categories_tab_expense
import moneym.feature.categories.generated.resources.categories_tab_income
import moneym.feature.categories.generated.resources.categories_hint
import moneym.feature.categories.generated.resources.categories_new_expense
import moneym.feature.categories.generated.resources.categories_new_income
import moneym.feature.categories.generated.resources.categories_edit_sheet_title
import moneym.feature.categories.generated.resources.categories_new_sheet_title
import moneym.feature.categories.generated.resources.categories_name_placeholder
import moneym.feature.categories.generated.resources.categories_delete
import moneym.feature.categories.generated.resources.categories_save_changes
import moneym.feature.categories.generated.resources.categories_create
import moneym.feature.categories.generated.resources.categories_delete_confirm_title
import moneym.feature.categories.generated.resources.categories_delete_confirm_body
import moneym.feature.categories.generated.resources.categories_delete_button
import moneym.feature.categories.generated.resources.categories_color_picker_title
import moneym.feature.categories.generated.resources.categories_color_hue
import moneym.feature.categories.generated.resources.categories_color_saturation
import moneym.feature.categories.generated.resources.categories_color_brightness
import moneym.feature.categories.generated.resources.categories_color_hex
import moneym.feature.categories.generated.resources.categories_color_select
```

## Acceptance Criteria
1. Zero hardcoded user-visible strings remain in `CategoryListScreen.kt`.
2. All new string keys are added to all 4 locale files (`values/`, `values-de/`, `values-es/`, `values-it/`).
3. `./gradlew :composeApp:assembleDebug` passes.
4. The screen visually behaves identically (strings are the same in English).

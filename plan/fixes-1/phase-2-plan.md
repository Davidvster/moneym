# Phase 2 — Category picker: title + OK button per mode (task 2)

## Goal
`MmCategoryPickerSheet` is used two ways:
- **Multi-select** (transactions filter): keep title "Filter by category" + the OK button (commits multi-select).
- **Single-select** (bank sync category assignment): the title should be **"Select category"** and there should be **no OK button** (picking already dismisses).

## File 1 — Kotlin
`core/ui/src/commonMain/kotlin/com/dv/moneym/core/ui/MmCategoryPickerSheet.kt`

Add two params to the private `CategoryPickerSheetImpl`:
- `titleRes: StringResource`
- `showConfirmButton: Boolean`

Changes:
1. Import `org.jetbrains.compose.resources.StringResource` and the new generated key `moneym.core.ui.generated.resources.category_picker_select_title` (add strings FIRST so codegen resolves it — see File 2).
2. Multi-select overload (lines 38-53) → call `CategoryPickerSheetImpl(..., titleRes = Res.string.category_picker_title, showConfirmButton = true)`.
3. Single-select overload (lines 55-72) → `CategoryPickerSheetImpl(..., titleRes = Res.string.category_picker_select_title, showConfirmButton = false)`.
4. In the impl: title `Text(text = stringResource(titleRes), ...)` (was hardcoded `category_picker_title` at line 121).
5. Wrap the OK `MmButton` (lines 168-174) in `if (showConfirmButton) { ... }`. Keep the trailing `Spacer`.

Do NOT change the multi-select call site behavior in transactions.

## File 2 — strings
Add key `category_picker_select_title` to **core/ui** `values/strings.xml` and **all 27 locale** files, inserted right after the existing `category_picker_title` line (line ~28) in each `core/ui/src/commonMain/composeResources/values{,-<locale>}/strings.xml`.

Translation table (value text):

| locale | value |
|--------|-------|
| values (en) | Select category |
| ar | اختر الفئة |
| cs | Vyberte kategorii |
| da | Vælg kategori |
| de | Kategorie auswählen |
| es | Seleccionar categoría |
| et | Vali kategooria |
| fi | Valitse kategoria |
| fr | Sélectionner une catégorie |
| hi | श्रेणी चुनें |
| hr | Odaberite kategoriju |
| hu | Kategória kiválasztása |
| is | Veldu flokk |
| it | Seleziona categoria |
| ja | カテゴリを選択 |
| lt | Pasirinkite kategoriją |
| lv | Izvēlieties kategoriju |
| mk | Изберете категорија |
| nb | Velg kategori |
| nl | Categorie selecteren |
| pl | Wybierz kategorię |
| pt | Selecionar categoria |
| ru | Выберите категорию |
| sk | Vyberte kategóriu |
| sl | Izberite kategorijo |
| sv | Välj kategori |
| tr | Kategori seç |
| vi | Chọn danh mục |
| zh | 选择类别 |

XML line format must match siblings exactly:
`    <string name="category_picker_select_title">VALUE</string>`

## Verify
- `./gradlew :core:ui:compileDebugKotlinAndroid :feature:banksync:compileDebugKotlinAndroid :feature:transactions:compileDebugKotlinAndroid`
- Confirm all 28 files contain `category_picker_select_title`:
  `grep -rL category_picker_select_title core/ui/src/commonMain/composeResources/*/strings.xml` should print nothing.

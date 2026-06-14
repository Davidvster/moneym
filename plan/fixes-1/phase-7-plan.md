# Phase 7 — Export/Import screen redesign

Repo: /Users/davidvalic/Developer/MoneyM. Largest phase. Restructure the
export/import screen into clearly separated Export and Import sections, each with
one primary button opening a bottom sheet. Export sheet gains format (JSON/CSV) +
an optional day-level date-range filter. Move the date-range dialog into `core/ui`
for reuse.

Files:
- `data/backup/.../BackupExporter.kt` — date filtering
- `feature/settings/.../overview/export/ExportUiState.kt` — state + intents
- `feature/settings/.../overview/export/ExportViewModel.kt` — handle new intents
- `feature/settings/.../overview/export/ExportScreen.kt` — redesign + new ExportOptionsSheet
- `core/ui/.../MmDateRangePickerDialog.kt` — NEW (moved from overview, label params)
- `feature/overview/.../OverviewScreen.kt` + delete `feature/overview/.../components/DateRangePickerDialog.kt`
- `feature/settings` strings (29 files)

---

## A. BackupExporter date filtering (`data/backup/BackupExporter.kt`)

Add optional inclusive day-level range to both methods; `null` = no bound
(current behavior). `Transaction.occurredOn` is `kotlinx.datetime.LocalDate`.
```kotlin
import kotlinx.datetime.LocalDate

suspend fun exportToJson(startDate: LocalDate? = null, endDate: LocalDate? = null): String {
    // ...
    val transactions = transactionRepository.observeAll().first()
        .filter { (startDate == null || it.occurredOn >= startDate) &&
                  (endDate == null || it.occurredOn <= endDate) }
    // ... use the filtered list for transactions = ...map { it.toDto() }
}

suspend fun exportToCsv(startDate: LocalDate? = null, endDate: LocalDate? = null): String {
    val transactions = transactionRepository.observeAll().first()
        .filter { (startDate == null || it.occurredOn >= startDate) &&
                  (endDate == null || it.occurredOn <= endDate) }
    // ... rest unchanged
}
```
(JSON stays a full backup except the transaction list is range-filtered — that's
the intended optional filter. Defaults keep existing callers/tests compiling.)

---

## B. core/ui — MmDateRangePickerDialog (moved + parametrized)

Create `core/ui/src/commonMain/kotlin/com/dv/moneym/core/ui/MmDateRangePickerDialog.kt`
by copying `feature/overview/.../components/DateRangePickerDialog.kt` verbatim,
then:
- Rename to `MmDateRangePickerDialog`, make it `public` (drop `internal`).
- Replace the 5 hardcoded `overview_*` string resources with **String params**:
  `title: String, fromLabel: String, toLabel: String, okLabel: String,
  cancelLabel: String`. Remove the `moneym.feature.overview.generated.resources.*`
  imports and `stringResource` usages — use the params instead.
- Keep everything else (millis conversion, SelectableDates, themed colors,
  onConfirm signature `(sy,sm,sd,ey,em,ed)`).

Then migrate the existing caller and delete the old file:
- `feature/overview/.../OverviewScreen.kt:274` — call `MmDateRangePickerDialog`
  (import from `com.dv.moneym.core.ui`), passing the overview strings:
  `title = stringResource(Res.string.overview_date_range_title),
   fromLabel = ...overview_date_range_from, toLabel = ...overview_date_range_to,
   okLabel = ...overview_ok, cancelLabel = ...overview_cancel`. Keep all other
  args identical.
- Remove the `import ...components.DateRangePickerDialog`.
- **Delete** `feature/overview/.../components/DateRangePickerDialog.kt`.
- The `overview_date_range_*`/`overview_ok`/`overview_cancel` keys stay (still
  passed in).

Verify `core/ui` has Material3 DatePicker available (it uses `ModalBottomSheet`
already, so material3 is on the classpath — `DateRangePicker`/`DatePickerDialog`
are in the same artifact).

---

## C. ExportUiState + intents (`ExportUiState.kt`)

`ExportUiState` is `@Serializable` (saved in SavedStateHandle). `LocalDate` is
`@Serializable` in kotlinx-datetime — fine. Add:
```kotlin
import kotlinx.datetime.LocalDate
// in ExportUiState:
val exportFormatCsv: Boolean = false,     // false = JSON
val exportStartDate: LocalDate? = null,
val exportEndDate: LocalDate? = null,
val showExportSheet: Boolean = false,
val showExportDateDialog: Boolean = false,
```
Add intents:
```kotlin
data class SetExportFormat(val csv: Boolean) : ExportIntent
data class ShowExportSheet(val visible: Boolean) : ExportIntent
data class ShowExportDateDialog(val visible: Boolean) : ExportIntent
data class SetExportDateRange(val start: LocalDate?, val end: LocalDate?) : ExportIntent
data object ClearExportDateRange : ExportIntent
data object ExportRequested : ExportIntent
```
Keep the existing import intents. The old `ExportJsonRequested`/`ExportCsvRequested`
may be removed (replaced by `ExportRequested` + `SetExportFormat`); update the
VM accordingly.

---

## D. ExportViewModel (`ExportViewModel.kt`)

Replace the two export branches with state-driven handling:
```kotlin
is ExportIntent.SetExportFormat -> _state.update { it.copy(exportFormatCsv = intent.csv) }
is ExportIntent.ShowExportSheet -> _state.update { it.copy(showExportSheet = intent.visible) }
is ExportIntent.ShowExportDateDialog -> _state.update { it.copy(showExportDateDialog = intent.visible) }
is ExportIntent.SetExportDateRange -> _state.update {
    it.copy(exportStartDate = intent.start, exportEndDate = intent.end, showExportDateDialog = false)
}
ExportIntent.ClearExportDateRange -> _state.update { it.copy(exportStartDate = null, exportEndDate = null) }
ExportIntent.ExportRequested -> {
    val s = _state.value
    _state.update { it.copy(isExporting = true, showExportSheet = false) }
    viewModelScope.launch {
        val result = withContext(dispatchers.io) {
            if (s.exportFormatCsv)
                Triple(exporter.exportToCsv(s.exportStartDate, s.exportEndDate), "moneym_export.csv", "text/csv")
            else
                Triple(exporter.exportToJson(s.exportStartDate, s.exportEndDate), "moneym_backup.json", "application/json")
        }
        _state.update { it.copy(isExporting = false) }
        _effects.send(ExportEffect.ExportReady(result.first, result.second, result.third))
    }
}
```
Keep all import-related branches unchanged.

---

## E. ExportScreen redesign (`ExportScreen.kt`)

Drive everything from VM state/intents (remove the local `formatIndex` and
`showImportSheet` `remember`s — fold into state). The `ExportContent` signature
becomes `(state: ExportUiState, onIntent: (ExportIntent) -> Unit, onBack)`.

**Main screen layout** — a `LazyColumn` with two clearly separated sections:
1. `SectionLabel(stringResource(settings_export_start))` ("Export") + an `MmCard`
   with a short description `Text(stringResource(settings_export_desc))` + an
   **Export** `MmButton` (Lg, Primary, fullWidth, enabled = !isExporting) →
   `onIntent(ShowExportSheet(true))`.
2. `SectionLabel(stringResource(settings_import))` ("Import") + an `MmCard` with
   `Text(stringResource(settings_import_desc))` + an **Import** `MmButton` (Lg,
   Secondary, fullWidth) → `onIntent(ShowExportSheet... )` no: opens the import
   sheet. Keep using a state flag or reuse `ShowExportSheet`? Use a dedicated
   approach: keep the import sheet shown via a separate state. Simplest: add a
   `showImportSheet` to state too, OR keep the import flow as-is via the existing
   `ImportSourceSheet` toggled by a new state field. **Add
   `showImportSheet: Boolean = false` to ExportUiState + `ShowImportSheet(visible)`
   intent** for symmetry (dumb-UI). Import button → `onIntent(ShowImportSheet(true))`.

**ExportOptionsSheet** (new private composable, model on `ImportSourceSheet.kt`):
- drag-handle box + `MmSheetHeader(title = stringResource(settings_export_start))`.
- Format: `MmSegmented(options = listOf("JSON","CSV"), selectedIndex = if (state.exportFormatCsv) 1 else 0, onOptionSelected = { onIntent(SetExportFormat(it == 1)) }, size = Sm)` with a label (reuse `settings_export_format`).
- Date-range row: a full-width clickable `Row` (clip + clickable + ripple, like
  Phase 5) showing `stringResource(settings_export_date_range)` + the current
  value (`stringResource(settings_export_date_all)` when both null, else
  `"$start – $end"` formatted `d.M.yyyy`) + a trailing calendar/edit icon
  (`Icon.Calendar.imageVector`). Tap → `onIntent(ShowExportDateDialog(true))`.
  When a range is set, also show a small "Clear" text button →
  `onIntent(ClearExportDateRange)` (`settings_export_date_clear`).
- Export `MmButton` (Lg, Primary, fullWidth) → `onIntent(ExportRequested)`.

**Date dialog**: when `state.showExportDateDialog`, render
`MmDateRangePickerDialog` (from core/ui). For init values, default to the current
range if set, else compute today via `kotlin.time.Clock.System` →
`kotlinx.datetime` (e.g. `Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date`)
for both start and end. Pass labels from settings strings:
`title = settings_export_date_range, fromLabel = settings_export_date_from,
 toLabel = settings_export_date_to, okLabel = settings_export_date_ok,
 cancelLabel = settings_payment_mode_cancel` (reuse existing Cancel).
`onConfirm = { sy,sm,sd, ey,em,ed -> onIntent(SetExportDateRange(LocalDate(sy,sm,sd), LocalDate(ey,em,ed))) }`,
`onDismiss = { onIntent(ShowExportDateDialog(false)) }`.

**ImportSourceSheet**: render when `state.showImportSheet`, wiring its callbacks
to set `ShowImportSheet(false)` then `onImportSourceSelected(...)` (keep the
existing `onImportSourceSelected` screen param + `CsvSourceFormat`). The
`ExportReady` effect handling and `exportDataEntry` signature stay unchanged.

Update the `@Preview` to pass an `ExportUiState()` + `{}`.

---

## F. New strings (settings, base + all 28 locales)

Add to `values/strings.xml` and every `values-<locale>/strings.xml`
(de/es/it careful; rest machine-assisted, never English):
```xml
<string name="settings_export_desc">Save your data as a JSON backup or a CSV file.</string>
<string name="settings_import_desc">Restore a MoneyM backup or import a CSV file.</string>
<string name="settings_export_date_range">Date range</string>
<string name="settings_export_date_all">All dates</string>
<string name="settings_export_date_from">From</string>
<string name="settings_export_date_to">To</string>
<string name="settings_export_date_ok">OK</string>
<string name="settings_export_date_clear">Clear</string>
```
Reuse existing: `settings_export_start` ("Export"), `settings_import`
("Import"), `settings_export_format`, `settings_payment_mode_cancel`.

---

## Verify
```bash
./gradlew :data:backup:compileDebugKotlinAndroid \
          :core:ui:compileDebugKotlinAndroid \
          :feature:overview:compileDebugKotlinAndroid \
          :feature:settings:compileDebugKotlinAndroid
./gradlew :data:backup:testDebugUnitTest :feature:settings:testDebugUnitTest --no-configuration-cache
./gradlew :core:ui:compileKotlinIosSimulatorArm64
```
All must pass. Confirm every locale has the 8 new keys. Confirm the old
`feature/overview/.../components/DateRangePickerDialog.kt` is deleted and overview
still compiles. Report files changed, results, deviations. Do NOT commit.

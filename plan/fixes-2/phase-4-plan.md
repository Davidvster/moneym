# Phase 4 — Localize Hardcoded Strings in OverviewScreen and TransactionListScreen

## Goal
Replace all hardcoded display strings in `OverviewScreen.kt` and `TransactionListScreen.kt` with localized `stringResource()` calls.

## Files to Edit

### Kotlin
- `/Users/davidvalic/Developer/MoneyM2/feature/overview/src/commonMain/kotlin/com/dv/moneym/feature/overview/ui/OverviewScreen.kt`
- `/Users/davidvalic/Developer/MoneyM2/feature/transactions/src/commonMain/kotlin/com/dv/moneym/feature/transactions/ui/TransactionListScreen.kt`

### String resources
- `/Users/davidvalic/Developer/MoneyM2/feature/overview/src/commonMain/composeResources/values/strings.xml`
- `/Users/davidvalic/Developer/MoneyM2/feature/overview/src/commonMain/composeResources/values-de/strings.xml`
- `/Users/davidvalic/Developer/MoneyM2/feature/overview/src/commonMain/composeResources/values-es/strings.xml`
- `/Users/davidvalic/Developer/MoneyM2/feature/overview/src/commonMain/composeResources/values-it/strings.xml`
- `/Users/davidvalic/Developer/MoneyM2/feature/transactions/src/commonMain/composeResources/values/strings.xml`
- `/Users/davidvalic/Developer/MoneyM2/feature/transactions/src/commonMain/composeResources/values-de/strings.xml`
- `/Users/davidvalic/Developer/MoneyM2/feature/transactions/src/commonMain/composeResources/values-es/strings.xml`
- `/Users/davidvalic/Developer/MoneyM2/feature/transactions/src/commonMain/composeResources/values-it/strings.xml`

## Hardcoded Strings in OverviewScreen.kt

1. `"AVG / DAY"` (in month mode avg card) → add `overview_avg_day` = `"AVG / DAY"` to overview strings.xml
2. `"AVG / MONTH"` (in year mode avg card) → add `overview_avg_month` = `"AVG / MONTH"`
3. `"AVG / DAY"` same key used again in year mode card
4. Short month names hardcoded in `monthNames2`: `listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")` — these are X-axis labels for the bar chart. They are already localized via `localizedMonthNames()` for full names but NOT for these short bar-chart labels. Convert these to use the existing `overview_month_*` keys (truncated to 3 chars each).

   In the year bar chart section replace:
   ```kotlin
   val monthNames2 = listOf(
       "Jan", "Feb", "Mar", "Apr", "May", "Jun",
       "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
   )
   ```
   With: `val monthNames2 = localizedMonthNames().map { it.take(3) }`
   (This reuses the existing localized full month names, trimmed to 3 chars.)

5. `"Select Month"` (dialog title in `OverviewMonthPickerDialog`) → add `overview_dialog_select_month` = `"Select Month"`
6. `"Previous year"` (content description) → add `overview_prev_year_cd` = `"Previous year"`
7. `"Next year"` (content description) → add `overview_next_year_cd` = `"Next year"`
8. Short month names in `OverviewMonthPickerDialog`: `listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")` → Use `localizedMonthNames().map { it.take(3) }`
9. `"Now"` (TextButton in dialogs) → add `overview_now` = `"Now"`
10. `"OK"` (confirm button text) → add `overview_ok` = `"OK"`
11. `"Cancel"` (dismiss button text) → add `overview_cancel` = `"Cancel"`
12. `"Select Year"` (dialog title in `OverviewYearPickerDialog`) → add `overview_dialog_select_year` = `"Select Year"`

In `CategoryTrendsCard`:
13. `"${trend.txCount} transaction${if (trend.txCount == 1) "" else "s"}"` — plural string. Add:
    - `overview_tx_count_singular` = `"%1$d transaction"` 
    - `overview_tx_count_plural` = `"%1$d transactions"`
    Replace with: `if (trend.txCount == 1) stringResource(Res.string.overview_tx_count_singular, trend.txCount) else stringResource(Res.string.overview_tx_count_plural, trend.txCount)`

## Hardcoded Strings in TransactionListScreen.kt

1. `"Search transactions…"` (search field placeholder) → add `transactions_search_placeholder` = `"Search transactions…"`
2. `"Search"` (search icon button content description) → add `transactions_search_cd` = `"Search"`
3. `"Close search"` (close icon button content description) → add `transactions_close_search_cd` = `"Close search"`
4. `"NET"` label → add `transactions_net_label` = `"NET"`
5. `"Select Month"` (dialog title in MonthPickerDialog) → add `transactions_dialog_select_month` = `"Select Month"`
6. `"Previous year"` content description → add `transactions_prev_year_cd` = `"Previous year"`
7. `"Next year"` content description → add `transactions_next_year_cd` = `"Next year"`
8. Short month names in MonthPickerDialog:
   ```kotlin
   val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
   ```
   Replace with a helper composable that maps localized month names to 3-char codes. The `monthLabel()` function at bottom already uses the full resource keys; create a `localizedMonthAbbreviations()` function similar to `monthLabel()`.
9. `"Now"` button → add `transactions_now` = `"Now"`
10. `"OK"` button → add `transactions_ok` = `"OK"`
11. `"Cancel"` button → add `transactions_cancel` = `"Cancel"`

## New String Keys to Add

### feature/overview/strings.xml (new keys to append)
```xml
<string name="overview_avg_day">AVG / DAY</string>
<string name="overview_avg_month">AVG / MONTH</string>
<string name="overview_dialog_select_month">Select Month</string>
<string name="overview_dialog_select_year">Select Year</string>
<string name="overview_prev_year_cd">Previous year</string>
<string name="overview_next_year_cd">Next year</string>
<string name="overview_now">Now</string>
<string name="overview_ok">OK</string>
<string name="overview_cancel">Cancel</string>
<string name="overview_tx_count_singular">%1$d transaction</string>
<string name="overview_tx_count_plural">%1$d transactions</string>
```

### feature/overview/strings-de.xml
```xml
<string name="overview_avg_day">ø / TAG</string>
<string name="overview_avg_month">ø / MONAT</string>
<string name="overview_dialog_select_month">Monat auswählen</string>
<string name="overview_dialog_select_year">Jahr auswählen</string>
<string name="overview_prev_year_cd">Vorheriges Jahr</string>
<string name="overview_next_year_cd">Nächstes Jahr</string>
<string name="overview_now">Jetzt</string>
<string name="overview_ok">OK</string>
<string name="overview_cancel">Abbrechen</string>
<string name="overview_tx_count_singular">%1$d Transaktion</string>
<string name="overview_tx_count_plural">%1$d Transaktionen</string>
```

### feature/overview/strings-es.xml
```xml
<string name="overview_avg_day">PROM / DÍA</string>
<string name="overview_avg_month">PROM / MES</string>
<string name="overview_dialog_select_month">Seleccionar mes</string>
<string name="overview_dialog_select_year">Seleccionar año</string>
<string name="overview_prev_year_cd">Año anterior</string>
<string name="overview_next_year_cd">Año siguiente</string>
<string name="overview_now">Ahora</string>
<string name="overview_ok">OK</string>
<string name="overview_cancel">Cancelar</string>
<string name="overview_tx_count_singular">%1$d transacción</string>
<string name="overview_tx_count_plural">%1$d transacciones</string>
```

### feature/overview/strings-it.xml
```xml
<string name="overview_avg_day">MEDIA / GIORNO</string>
<string name="overview_avg_month">MEDIA / MESE</string>
<string name="overview_dialog_select_month">Seleziona mese</string>
<string name="overview_dialog_select_year">Seleziona anno</string>
<string name="overview_prev_year_cd">Anno precedente</string>
<string name="overview_next_year_cd">Anno successivo</string>
<string name="overview_now">Ora</string>
<string name="overview_ok">OK</string>
<string name="overview_cancel">Annulla</string>
<string name="overview_tx_count_singular">%1$d transazione</string>
<string name="overview_tx_count_plural">%1$d transazioni</string>
```

### feature/transactions/strings.xml (new keys to append)
Read the existing file first to avoid duplicates, then append:
```xml
<string name="transactions_search_placeholder">Search transactions…</string>
<string name="transactions_search_cd">Search</string>
<string name="transactions_close_search_cd">Close search</string>
<string name="transactions_net_label">NET</string>
<string name="transactions_dialog_select_month">Select Month</string>
<string name="transactions_prev_year_cd">Previous year</string>
<string name="transactions_next_year_cd">Next year</string>
<string name="transactions_now">Now</string>
<string name="transactions_ok">OK</string>
<string name="transactions_cancel">Cancel</string>
```

### feature/transactions/strings-de.xml
```xml
<string name="transactions_search_placeholder">Transaktionen suchen…</string>
<string name="transactions_search_cd">Suchen</string>
<string name="transactions_close_search_cd">Suche schließen</string>
<string name="transactions_net_label">NETTO</string>
<string name="transactions_dialog_select_month">Monat auswählen</string>
<string name="transactions_prev_year_cd">Vorheriges Jahr</string>
<string name="transactions_next_year_cd">Nächstes Jahr</string>
<string name="transactions_now">Jetzt</string>
<string name="transactions_ok">OK</string>
<string name="transactions_cancel">Abbrechen</string>
```

### feature/transactions/strings-es.xml
```xml
<string name="transactions_search_placeholder">Buscar transacciones…</string>
<string name="transactions_search_cd">Buscar</string>
<string name="transactions_close_search_cd">Cerrar búsqueda</string>
<string name="transactions_net_label">NETO</string>
<string name="transactions_dialog_select_month">Seleccionar mes</string>
<string name="transactions_prev_year_cd">Año anterior</string>
<string name="transactions_next_year_cd">Año siguiente</string>
<string name="transactions_now">Ahora</string>
<string name="transactions_ok">OK</string>
<string name="transactions_cancel">Cancelar</string>
```

### feature/transactions/strings-it.xml
```xml
<string name="transactions_search_placeholder">Cerca transazioni…</string>
<string name="transactions_search_cd">Cerca</string>
<string name="transactions_close_search_cd">Chiudi ricerca</string>
<string name="transactions_net_label">NETTO</string>
<string name="transactions_dialog_select_month">Seleziona mese</string>
<string name="transactions_prev_year_cd">Anno precedente</string>
<string name="transactions_next_year_cd">Anno successivo</string>
<string name="transactions_now">Ora</string>
<string name="transactions_ok">OK</string>
<string name="transactions_cancel">Annulla</string>
```

## Important
- ALWAYS read each existing strings.xml before editing — do not add duplicate keys.
- In `TransactionListScreen.kt`, add a `@Composable private fun localizedMonthAbbreviations()` function that calls the existing 12 `transactions_month_*` string resources and `.take(3)` each.
- Use that in `MonthPickerDialog` instead of the hardcoded list.

## Acceptance Criteria
1. Zero hardcoded user-visible strings remain in `OverviewScreen.kt` and `TransactionListScreen.kt`.
2. All new string keys added to all 4 locale files per feature.
3. `./gradlew :composeApp:assembleDebug` passes.

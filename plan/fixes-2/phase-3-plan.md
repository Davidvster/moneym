# Phase 3 — Localize Hardcoded Strings in Security, Onboarding, Settings screens

## Goal
Replace all hardcoded display strings in PinSetupScreen, PinUnlockScreen, OnboardingScreen, SettingsScreen, CurrencyPickerScreen, LanguagePickerScreen, and TxListDisplayScreen.

## Files to Edit

### Kotlin UI files
- `/Users/davidvalic/Developer/MoneyM2/feature/security/src/commonMain/kotlin/com/dv/moneym/feature/security/ui/PinSetupScreen.kt`
- `/Users/davidvalic/Developer/MoneyM2/feature/security/src/commonMain/kotlin/com/dv/moneym/feature/security/ui/PinUnlockScreen.kt`
- `/Users/davidvalic/Developer/MoneyM2/feature/onboarding/src/commonMain/kotlin/com/dv/moneym/feature/onboarding/ui/OnboardingScreen.kt`
- `/Users/davidvalic/Developer/MoneyM2/feature/settings/src/commonMain/kotlin/com/dv/moneym/feature/settings/ui/SettingsScreen.kt`
- `/Users/davidvalic/Developer/MoneyM2/feature/settings/src/commonMain/kotlin/com/dv/moneym/feature/settings/ui/CurrencyPickerScreen.kt`
- `/Users/davidvalic/Developer/MoneyM2/feature/settings/src/commonMain/kotlin/com/dv/moneym/feature/settings/ui/LanguagePickerScreen.kt`
- `/Users/davidvalic/Developer/MoneyM2/feature/settings/src/commonMain/kotlin/com/dv/moneym/feature/settings/ui/TxListDisplayScreen.kt`

### String resource files
- `/Users/davidvalic/Developer/MoneyM2/feature/security/src/commonMain/composeResources/values/strings.xml`
- `/Users/davidvalic/Developer/MoneyM2/feature/security/src/commonMain/composeResources/values-de/strings.xml`
- `/Users/davidvalic/Developer/MoneyM2/feature/security/src/commonMain/composeResources/values-es/strings.xml`
- `/Users/davidvalic/Developer/MoneyM2/feature/security/src/commonMain/composeResources/values-it/strings.xml`
- `/Users/davidvalic/Developer/MoneyM2/feature/onboarding/src/commonMain/composeResources/values/strings.xml`
- `/Users/davidvalic/Developer/MoneyM2/feature/settings/src/commonMain/composeResources/values/strings.xml`
- `/Users/davidvalic/Developer/MoneyM2/feature/settings/src/commonMain/composeResources/values-de/strings.xml`
- `/Users/davidvalic/Developer/MoneyM2/feature/settings/src/commonMain/composeResources/values-es/strings.xml`
- `/Users/davidvalic/Developer/MoneyM2/feature/settings/src/commonMain/composeResources/values-it/strings.xml`

## Strings to Localize

### PinSetupScreen.kt
The security module already has many keys in strings.xml. These are hardcoded:
- `"M"` (app icon letter) — do NOT localize (brand mark)
- `"MoneyM"` — do NOT localize (brand name)
- `"Create a PIN"` → use existing `security_pin_setup_title`
- `"Confirm your PIN"` → use existing `security_pin_confirm_title`
- `"Cancel"` (close button content description) → use existing `security_cancel`

In `PinSetupScreen.kt`, the text composables use `state.step == PinSetupStep.ENTER_FIRST` to pick title.
Replace:
```kotlin
text = if (state.step == PinSetupStep.ENTER_FIRST) {
    "Create a PIN"
} else {
    "Confirm your PIN"
},
```
With:
```kotlin
text = if (state.step == PinSetupStep.ENTER_FIRST) {
    stringResource(Res.string.security_pin_setup_title)
} else {
    stringResource(Res.string.security_pin_confirm_title)
},
```

Add import: `import org.jetbrains.compose.resources.stringResource`
Add import: `import moneym.feature.security.generated.resources.Res`
Add import: `import moneym.feature.security.generated.resources.security_pin_setup_title`
Add import: `import moneym.feature.security.generated.resources.security_pin_confirm_title`
Add import: `import moneym.feature.security.generated.resources.security_cancel`

### PinUnlockScreen.kt
Hardcoded:
- `"M"` — do NOT localize (brand mark)
- `"MoneyM"` — do NOT localize (brand name)
- `"Enter your PIN"` → use existing `security_pin_enter_title`
- `"Try again in $seconds seconds"` → use existing `security_backoff_retry` (format string with `%1$d`)

Replace:
```kotlin
text = if (state.backoffRemainingMs > 0) {
    val seconds = (state.backoffRemainingMs / 1000) + 1
    "Try again in $seconds seconds"
} else {
    "Enter your PIN"
},
```
With:
```kotlin
text = if (state.backoffRemainingMs > 0) {
    val seconds = (state.backoffRemainingMs / 1000) + 1
    stringResource(Res.string.security_backoff_retry, seconds)
} else {
    stringResource(Res.string.security_pin_enter_title)
},
```

Add imports for `Res`, `security_pin_enter_title`, `security_backoff_retry`, `stringResource`.

### OnboardingScreen.kt
- `"Search currency…"` placeholder → add `onboarding_search_currency` = `"Search currency…"` to strings.xml
- `"CURRENCIES"` section label → add `onboarding_currencies_header` = `"CURRENCIES"` to strings.xml

Replace:
```kotlin
placeholder = "Search currency…",
```
With:
```kotlin
placeholder = stringResource(Res.string.onboarding_search_currency),
```

Replace:
```kotlin
SectionLabel(
    text = "CURRENCIES",
    ...
)
```
With:
```kotlin
SectionLabel(
    text = stringResource(Res.string.onboarding_currencies_header),
    ...
)
```

### SettingsScreen.kt (LockTimeoutPickerDialog)
Hardcoded strings in the dialog:
- `"Lock after"` (dialog title) → add `settings_lock_after_title` = `"Lock after"` to strings.xml
- `"Immediately"` → use existing `settings_lock_immediately`
- `"30 seconds"` → use existing `settings_lock_30s`
- `"1 minute"` → use existing `settings_lock_1m`
- `"5 minutes"` → use existing `settings_lock_5m`
- `"OK"` (confirm button) → add `settings_ok` = `"OK"` to strings.xml
- `"Cancel"` (dismiss button) → add `settings_cancel_dialog` = `"Cancel"` (or reuse `settings_cancel` which already exists)
- `"MoneyM v2.0 · build 2026.05.15"` version string — leave as-is (not a UI string for translation)

In `LockTimeoutPickerDialog`, replace the options list from hardcoded Pair<Int,String>:
```kotlin
val options = listOf(
    0 to stringResource(Res.string.settings_lock_immediately),
    30 to stringResource(Res.string.settings_lock_30s),
    60 to stringResource(Res.string.settings_lock_1m),
    300 to stringResource(Res.string.settings_lock_5m),
)
```

Replace title: `Text("Lock after", ...)` → `Text(stringResource(Res.string.settings_lock_after_title), ...)`
Replace OK button: `Text("OK", ...)` → `Text(stringResource(Res.string.settings_ok), ...)`
Replace Cancel button: `Text("Cancel", ...)` → `Text(stringResource(Res.string.settings_cancel), ...)`

Also: `"Previous year"` and `"Next year"` content descriptions in OverviewMonthPickerDialog (already in OverviewScreen.kt — that's a separate screen, covered in Phase 4).

In `SettingsScreen.kt`, the languageSubtitle switch uses hardcoded language names:
```kotlin
val languageSubtitle = when (state.language) {
    "en" -> "English"
    "de" -> "Deutsch"
    "es" -> "Español"
    "it" -> "Italiano"
    "fr" -> "Français"
    "pt" -> "Português"
    else -> "System default"
}
```
Add string keys for these or leave as-is since they're language native names (localization of language names is debatable). For now, add:
- `settings_lang_system_default` = `"System default"` to settings strings.xml (en only; leave native language names in code since they're universal).

Actually the language names (Deutsch, Español, etc.) should stay in code since they're the native name of the language — not translated.

### CurrencyPickerScreen.kt
Hardcoded:
- `"Currency"` (ScreenHeader title) → add `settings_currency_picker_title` = `"Currency"` to settings strings.xml
- `"Search currency…"` → add `settings_search_currency` = `"Search currency…"` to settings strings.xml
- `"POPULAR"` section label → add `settings_currency_popular` = `"POPULAR"` to settings strings.xml
- `"ALL CURRENCIES"` section label → add `settings_currency_all` = `"ALL CURRENCIES"` to settings strings.xml

### LanguagePickerScreen.kt
- `"Language"` (ScreenHeader title) → add `settings_language_picker_title` = `"Language"` to settings strings.xml

### TxListDisplayScreen.kt
- `"Transaction list"` (ScreenHeader title) → add `settings_tx_list_display_title` = `"Transaction list"` to settings strings.xml

## New String Keys to Add

### feature/settings/strings.xml
```xml
<string name="settings_lock_after_title">Lock after</string>
<string name="settings_ok">OK</string>
<string name="settings_currency_picker_title">Currency</string>
<string name="settings_search_currency">Search currency…</string>
<string name="settings_currency_popular">POPULAR</string>
<string name="settings_currency_all">ALL CURRENCIES</string>
<string name="settings_language_picker_title">Language</string>
<string name="settings_tx_list_display_title">Transaction list</string>
<string name="settings_lang_system_default">System default</string>
```

### feature/settings/strings-de.xml (add same keys with German translations)
```xml
<string name="settings_lock_after_title">Sperren nach</string>
<string name="settings_ok">OK</string>
<string name="settings_currency_picker_title">Währung</string>
<string name="settings_search_currency">Währung suchen…</string>
<string name="settings_currency_popular">BELIEBT</string>
<string name="settings_currency_all">ALLE WÄHRUNGEN</string>
<string name="settings_language_picker_title">Sprache</string>
<string name="settings_tx_list_display_title">Transaktionsliste</string>
<string name="settings_lang_system_default">Systemstandard</string>
```

### feature/settings/strings-es.xml
```xml
<string name="settings_lock_after_title">Bloquear después de</string>
<string name="settings_ok">OK</string>
<string name="settings_currency_picker_title">Moneda</string>
<string name="settings_search_currency">Buscar moneda…</string>
<string name="settings_currency_popular">POPULARES</string>
<string name="settings_currency_all">TODAS LAS MONEDAS</string>
<string name="settings_language_picker_title">Idioma</string>
<string name="settings_tx_list_display_title">Lista de transacciones</string>
<string name="settings_lang_system_default">Idioma del sistema</string>
```

### feature/settings/strings-it.xml
```xml
<string name="settings_lock_after_title">Blocca dopo</string>
<string name="settings_ok">OK</string>
<string name="settings_currency_picker_title">Valuta</string>
<string name="settings_search_currency">Cerca valuta…</string>
<string name="settings_currency_popular">POPOLARI</string>
<string name="settings_currency_all">TUTTE LE VALUTE</string>
<string name="settings_language_picker_title">Lingua</string>
<string name="settings_tx_list_display_title">Lista transazioni</string>
<string name="settings_lang_system_default">Predefinito di sistema</string>
```

### feature/onboarding/strings.xml (add new keys)
```xml
<string name="onboarding_search_currency">Search currency…</string>
<string name="onboarding_currencies_header">CURRENCIES</string>
```

### feature/onboarding/strings-de.xml
```xml
<string name="onboarding_search_currency">Währung suchen…</string>
<string name="onboarding_currencies_header">WÄHRUNGEN</string>
```

### feature/onboarding/strings-es.xml
```xml
<string name="onboarding_search_currency">Buscar moneda…</string>
<string name="onboarding_currencies_header">MONEDAS</string>
```

### feature/onboarding/strings-it.xml
```xml
<string name="onboarding_search_currency">Cerca valuta…</string>
<string name="onboarding_currencies_header">VALUTE</string>
```

## Important
- Always read the existing strings.xml files before appending new keys to avoid duplicates.
- All new string keys must be added to ALL locale variants (values/, values-de/, values-es/, values-it/).

## Acceptance Criteria
1. Zero hardcoded user-visible strings remain in PinSetupScreen, PinUnlockScreen, OnboardingScreen, SettingsScreen, CurrencyPickerScreen, LanguagePickerScreen, TxListDisplayScreen.
2. All new string keys exist in all 4 locale files for settings and onboarding, and 4 locale files for security.
3. `./gradlew :composeApp:assembleDebug` passes.

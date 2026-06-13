# Phase 2 — Banksync multi-screen setup flow

## Goal
Split the single `BankSyncSettingsScreen` mega-screen into three screens, each in its own directory with isolated ViewModel/UiState/Intent: home, credentials, bank picker. Improve country selection with a localized supported-country list.

## Target layout (feature/banksync/src/commonMain/kotlin/com/dv/moneym/feature/banksync/)
```
home/         BankSyncHomeScreen.kt, BankSyncHomeViewModel.kt, BankSyncHomeUiState.kt (Intent in UiState file or sibling)
credentials/  BankSyncCredentialsScreen.kt, BankSyncCredentialsViewModel.kt, BankSyncCredentialsUiState.kt
bankpicker/   BankPickerScreen.kt, BankPickerViewModel.kt, BankPickerUiState.kt
suggestions/  BankSuggestionsScreen.kt, BankSuggestionsViewModel.kt, BankSuggestionsUiState.kt (MOVE existing files, package update only — no redesign yet)
countries/    EnableBankingCountries.kt
usecase/      (unchanged)
```
Old `BankSyncSettingsScreen.kt`, `BankSyncSettingsViewModel.kt`, `BankSyncSettingsUiState.kt`, `BankSyncSettingsIntent.kt` are replaced by the three new screens — delete after split.

## Screen specs

### home/ (route key: keep `BankSyncSettingsKey` name, move declaration next to home screen)
Shown states:
- Credentials NOT configured → intro/empty state with explanation + primary button "Set up bank sync" → navigate to credentials screen.
- Configured, no active bank connection → status card (not connected) + primary "Connect a bank" → navigate to bank picker screen. Secondary access to credentials screen (e.g. "Edit credentials" row/button).
- Connected → StatusCard (status, session expiry, pending-suggestions button), LINKED ACCOUNTS list (toggle + wallet mapping — keep current inline mapping UI for now, phase 3 replaces with sheet), ControlsCard (auto-sync toggle, Sync Now, Disconnect, "Connect a bank" for adding/reconnecting).
VM (BankSyncHomeViewModel): credentials-configured flag, session/status/expiry, accounts list + SetAccountEnabled + SetLocalAccountMapping, auto-sync toggle, Sync Now (engine), Disconnect, pending count. Reload state on screen resume (it must reflect changes made by credentials/bankpicker screens — observe repos/flows where possible; current VM logic shows how).
- `MmLoadingOverlay` while initial load or syncing.

### credentials/ (new key `BankSyncCredentialsKey`)
- App ID field + PEM private key multiline field (reuse current CredentialsCard contents/strings).
- Bottom-anchored PRIMARY MmButton "Save & continue" (new string key) — validates via client like today (`isValidatingCredentials`), on success: if first-time setup → navigate forward to bank picker; in all cases also allow back. Use a one-shot nav effect from VM (check how other VMs in repo do nav effects; if none, expose a `navigateNext: Boolean`/event in state consumed by screen — follow existing app pattern, e.g. search for how onboarding navigates).
- `MmLoadingOverlay` while validating. Error via state (existing `bank_sync_error_generic`).

### bankpicker/ (new key `BankPickerKey`)
Two-step within screen:
1. Country selection: searchable list of Enable Banking supported countries from `countries/EnableBankingCountries.kt`. Each row: localized country name + 2-char code (MmRow). Search field filters by localized name OR code (case-insensitive). No raw-code-only flow anymore — but typing a 2-char code in search finds the country.
2. After country picked → load banks (`LoadBanks`), bank list as MmRow rows (bank name; keep search field for bank filter — current `bankSearch` behavior, no 25-row cap needed with LazyColumn).
- Tapping bank → `ConnectBank` → auth URL opens (current behavior via state/engine), screen shows awaiting-auth state with redirect paste field + "Complete connection" (current ConnectCard auth-flow mode). `BankAuthCallbackBus` collection moves here. On successful completion → pop back to home (nav effect).
- `MmLoadingOverlay` while loading banks / completing connection. Cancel returns to bank list.

### countries/EnableBankingCountries.kt
```kotlin
object EnableBankingCountries { val codes: List<String> = listOf("AT","BE","BG","HR","CY","CZ","DK","EE","FI","FR","DE","GR","HU","IS","IE","IT","LV","LI","LT","LU","MT","NL","NO","PL","PT","RO","SK","SI","ES","SE","GB") }
```

### Localized country names — core/common
Add next to existing `DateFormatter.kt` pattern in `core/common`:
- commonMain: `expect fun countryDisplayName(countryCode: String): String`
- androidMain: `java.util.Locale("", countryCode).getDisplayCountry(Locale.getDefault())` (fall back to code if blank)
- iosMain: `NSLocale.currentLocale.localizedStringForCountryCode(countryCode) ?: countryCode` (use `displayNameForKey(NSLocaleCountryCode, value=...)` if simpler)
Check existing DateFormatter.android.kt / DateFormatter.ios.kt for file naming + package.

## Navigation wiring (shared/src/commonMain/kotlin/com/dv/moneym/MainNav.kt lines ~234-243)
- Keep `bankSyncSettingsEntry` → home screen (update import/package), callbacks: onBack, onOpenSuggestions, onNavigateToInfo, NEW onNavigateToCredentials, onNavigateToBankPicker.
- Add `bankSyncCredentialsEntry(onBack, onContinueToBankPicker)` and `bankPickerEntry(onBack, onConnected)`; `onConnected` pops back to BankSyncSettingsKey (removeLast until home — check how other flows pop; simplest: `tabBackStack.removeLast()` from bankpicker if credentials was replaced, otherwise pop twice — implement by popping back to the home key: inspect tabBackStack API in MainNav for a popUpTo-style helper; if none, remove entries while last != BankSyncSettingsKey).
- All with `metadata = modalTransitionMeta` like current entries.

## Koin (shared/.../di/FeatureModules.kt ~331-375)
Replace `BankSyncSettingsViewModel` registration with three public VMs: `BankSyncHomeViewModel`, `BankSyncCredentialsViewModel`, `BankPickerViewModel` (constructor deps split per responsibility; keep existing use-case singles). All classes public.

## Strings (feature/banksync composeResources, base + ALL 27 locales: ar cs da de es et fi fr hi hr hu is it ja lt lv mk nb nl pl pt ru sk sl sv tr vi zh)
New keys (suggest, adjust as needed):
- `bank_sync_setup_cta` "Set up bank sync"
- `bank_sync_save_continue` "Save & continue"
- `bank_sync_edit_credentials` "Edit credentials"
- `bank_sync_choose_country` "Choose your country"
- `bank_sync_search_country` "Search country or code"
- `bank_sync_supported_countries_hint` "Bank sync works with PSD2 open-banking APIs in these countries."
- `bank_sync_change_country` "Change country"
Reuse existing keys where they fit (`bank_sync_connect_bank`, `bank_sync_search_banks`, `bank_sync_load_banks` may become obsolete — remove unused keys from ALL locale files if no longer referenced).
Machine-assisted translation quality fine for non-de/es/it; keep de/es/it careful.

## Tests
Existing `BankSyncSettingsViewModelTest.kt` must be split so module still compiles: move credential tests → `BankSyncCredentialsViewModelTest`, bank-list/auth tests → `BankPickerViewModelTest`, rest → `BankSyncHomeViewModelTest`. They must COMPILE and ideally pass (`./gradlew :feature:banksync:testDebugUnitTest --no-configuration-cache`); deeper coverage added in phase 6.

## Conventions
- One public entry point per VM: `onIntent(...)`; state `internal val state: StateFlow<...>` — copy existing VM structure.
- VMs public (Koin cross-module).
- No hardcoded dates in UiState defaults.
- Import classes, no FQNs; compose resource keys need per-key imports.

## Verify
```
./gradlew :core:common:compileDebugKotlinAndroid :feature:banksync:compileDebugKotlinAndroid :shared:compileDebugKotlinAndroid
./gradlew :feature:banksync:testDebugUnitTest --no-configuration-cache
./gradlew :shared:compileKotlinIosSimulatorArm64
```
(iOS compile needed because of the new iosMain actual in core/common.)

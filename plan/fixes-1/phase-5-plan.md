# Phase 5 — Transactions: combined sync (bank + cross-device) (task 7)

## Goal
When bank sync is configured, the Transactions screen's existing top-right sync control must also surface bank sync. The sync sheet becomes **two sections** (cross-device "Cloud sync" + "Bank sync"), each shown only if that source is enabled. The button appears if **either** source is enabled.

## Architecture (mirror the existing `SyncStatusProvider` pattern)
The transactions VM already depends on `SyncStatusProvider` (implemented by `SyncEngine`, passed as `get<SyncEngine>()` — no explicit Koin binding). Do the same for bank sync.

### 1. New interface — `data/banksync/src/commonMain/kotlin/com/dv/moneym/data/banksync/BankSyncStatusProvider.kt`
```kotlin
package com.dv.moneym.data.banksync

import kotlinx.coroutines.flow.Flow

interface BankSyncStatusProvider {
    val isEnabled: Flow<Boolean>
    val isSyncing: Flow<Boolean>
    val pendingCount: Flow<Int>
    val lastSyncedMs: Flow<Long>
    suspend fun requestSync()
}
```

### 2. Implement on `BankSyncEngine` (`data/banksync/.../BankSyncEngine.kt`)
Add `: BankSyncStatusProvider` to the class and these members (it already has `appSettings`, `bankSyncRepository`, `runtime`, `syncNow()`):
```kotlin
override val isEnabled: Flow<Boolean>
    get() = appSettings.observeBoolean(PrefKeys.BANK_SYNC_CONFIGURED, defaultValue = false)
override val isSyncing: Flow<Boolean>
    get() = runtime.map { it is BankSyncRuntimeState.Running }
override val pendingCount: Flow<Int>
    get() = bankSyncRepository.observePendingCount()
override val lastSyncedMs: Flow<Long>
    get() = runtime.map { appSettings.getLong(PrefKeys.BANK_SYNC_LAST_SYNC_MS, defaultValue = 0L) }
override suspend fun requestSync() { syncNow() }
```
Imports: `kotlinx.coroutines.flow.Flow`, `kotlinx.coroutines.flow.map`. (`AppSettings.observeBoolean`, `getLong` exist; `BankSyncRuntimeState.Running` exists.)
No Koin change needed — `get<BankSyncEngine>()` already returns a `BankSyncStatusProvider`.

### 3. transactions build dep — `feature/transactions/build.gradle.kts`
Add to `commonMain.dependencies`: `implementation(projects.data.banksync)` (next to `projects.data.sync`).

## ViewModel — `TransactionListViewModel.kt`
- Import `com.dv.moneym.data.banksync.BankSyncStatusProvider` and `kotlinx.coroutines.flow.flowOf`.
- Add constructor param (nullable, default null — like `syncPuller`): `private val bankSyncStatus: BankSyncStatusProvider? = null,` (place after `syncPuller`).
- Extend the `combine` chain (after the existing `syncStatus.lastSyncedMs` combine, ~line 149), null-guarding the provider:
```kotlin
.combine(bankSyncStatus?.isEnabled ?: flowOf(false)) { s, e -> s.copy(isBankSyncEnabled = e) }
.combine(bankSyncStatus?.isSyncing ?: flowOf(false)) { s, b -> s.copy(isBankSyncing = b) }
.combine(bankSyncStatus?.pendingCount ?: flowOf(0)) { s, c -> s.copy(bankPendingCount = c) }
.combine(bankSyncStatus?.lastSyncedMs ?: flowOf(0L)) { s, ms -> s.copy(bankLastSyncedMs = ms) }
```
- `onIntent`: add `TransactionListIntent.BankSyncNow -> viewModelScope.launch { bankSyncStatus?.requestSync() }`.

## UiState — `TransactionListUiState.kt`
Add fields:
```kotlin
val isBankSyncEnabled: Boolean = false,
val isBankSyncing: Boolean = false,
val bankPendingCount: Int = 0,
val bankLastSyncedMs: Long = 0L,
```

## Intent — `TransactionListIntent.kt`
Add `data object BankSyncNow : TransactionListIntent`.

## Screen — `TransactionListScreen.kt`
### Header gating (~line 678)
`if (state.isSyncEnabled || state.isBankSyncEnabled) { SyncActionButton(...) }`.
Pass `isSyncInProgress = state.isSyncInProgress || state.isBankSyncing` so the spinner reflects either source. `hasSyncConflict` stays `state.hasSyncConflict` (cross-device only).

### SyncStatusSheet → two sections
Add params: `crossDeviceEnabled: Boolean`, `bankEnabled: Boolean`, `bankIsSyncing: Boolean`, `bankLastSyncedMs: Long`, `bankPendingCount: Int`, `onBankSyncNow: () -> Unit`, `onReviewSuggestions: () -> Unit` (keep existing cross-device params).
Body:
- **Cloud section** (only if `crossDeviceEnabled`): header `Text(stringResource(Res.string.transactions_sync_sheet_title), style = type.title3)` ("Cloud sync"), then the existing status text / conflict body / pending-deletions row / "Sync now" button (`onSyncNow`).
- If both enabled, a spacer/divider (`MM.colors.divider`) between sections.
- **Bank section** (only if `bankEnabled`): header `Text(stringResource(Res.string.transactions_bank_sync_section), style = type.title3)` ("Bank sync"); status text reusing existing keys:
  `when { bankIsSyncing -> transactions_syncing; bankLastSyncedMs > 0 -> stringResource(transactions_sync_last, formatSyncTime(bankLastSyncedMs)); else -> transactions_sync_never }`;
  if `bankPendingCount > 0` a `MmButton(text = stringResource(transactions_bank_review, bankPendingCount), onClick = onReviewSuggestions, variant = Outline, fullWidth = true)`;
  `MmButton(text = stringResource(transactions_sync_now), onClick = onBankSyncNow, enabled = !bankIsSyncing, fullWidth = true)`.

Keep the existing top-level sheet title removed/replaced by the per-section headers (when only cloud is enabled the result reads the same — a "Cloud sync" header + content).

### Call site (~line 240)
Thread a new screen-level callback `onNavigateToBankSuggestions: () -> Unit` through `transactionsEntry` → `TransactionListScreen` → `TransactionListContent`, and wire `SyncStatusSheet(... crossDeviceEnabled = state.isSyncEnabled, bankEnabled = state.isBankSyncEnabled, bankIsSyncing = state.isBankSyncing, bankLastSyncedMs = state.bankLastSyncedMs, bankPendingCount = state.bankPendingCount, onBankSyncNow = { onIntent(TransactionListIntent.BankSyncNow) }, onReviewSuggestions = { onIntent(TransactionListIntent.ShowSyncSheet(false)); onNavigateToBankSuggestions() }, ...)`.

## Nav — `transactionsEntry` + `MainNav.kt`
- Add param `onNavigateToBankSuggestions: () -> Unit = {}` to `transactionsEntry` (TransactionListScreen.kt ~line 139) and pass it down.
- In `MainNav.kt` (transactionsEntry call ~line 145): add `onNavigateToBankSuggestions = { tabBackStack.push(BankSuggestionsKey) }` (`BankSuggestionsKey` already imported).

## Strings — transactions module, all 28 locales (`feature/transactions/src/commonMain/composeResources/values{,-<loc>}/strings.xml`)
Add near the other `transactions_sync_*` keys.

### `transactions_bank_sync_section` = "Bank sync"
| loc|val|loc|val |
|---|---|---|---|
|values|Bank sync|nb|Banksynkronisering|
|ar|مزامنة البنك|nl|Banksynchronisatie|
|cs|Synchronizace banky|pl|Synchronizacja banku|
|da|Banksynkronisering|pt|Sincronização bancária|
|de|Banksynchronisierung|ru|Синхронизация с банком|
|es|Sincronización bancaria|sk|Synchronizácia banky|
|et|Panga sünkroonimine|sl|Sinhronizacija banke|
|fi|Pankkisynkronointi|sv|Banksynkronisering|
|fr|Synchronisation bancaire|tr|Banka senkronizasyonu|
|hi|बैंक सिंक|vi|Đồng bộ ngân hàng|
|hr|Sinkronizacija banke|zh|银行同步|
|hu|Banki szinkronizálás|is|Bankasamstilling|
|it|Sincronizzazione bancaria|ja|銀行同期|
|lt|Banko sinchronizavimas|lv|Bankas sinhronizācija|
|mk|Синхронизација со банка|||

### `transactions_bank_review` = "Review %1$d suggestions"
| loc|val|loc|val |
|---|---|---|---|
|values|Review %1$d suggestions|nb|Gå gjennom %1$d forslag|
|ar|مراجعة %1$d اقتراحات|nl|%1$d suggesties bekijken|
|cs|Zkontrolovat %1$d návrhů|pl|Przejrzyj %1$d sugestii|
|da|Gennemgå %1$d forslag|pt|Rever %1$d sugestões|
|de|%1$d Vorschläge prüfen|ru|Просмотреть %1$d предложений|
|es|Revisar %1$d sugerencias|sk|Skontrolovať %1$d návrhov|
|et|Vaata üle %1$d soovitust|sl|Preglej %1$d predlogov|
|fi|Tarkista %1$d ehdotusta|sv|Granska %1$d förslag|
|fr|Examiner %1$d suggestions|tr|%1$d öneriyi incele|
|hi|%1$d सुझावों की समीक्षा करें|vi|Xem %1$d gợi ý|
|hr|Pregledaj %1$d prijedloga|zh|查看 %1$d 条建议|
|hu|%1$d javaslat áttekintése|is|Skoða %1$d tillögur|
|it|Rivedi %1$d suggerimenti|ja|%1$d件の候補を確認|
|lt|Peržiūrėti %1$d pasiūlymų|lv|Pārskatīt %1$d ieteikumus|
|mk|Прегледај %1$d предлози|||

## Tests / parity
The transactions VM test's `makeVm` uses named args and does not pass `syncPuller`; the new `bankSyncStatus` param defaults to `null`, so the test still compiles unchanged. No fake required (optional).

## Verify
- `grep -rL transactions_bank_sync_section feature/transactions/src/commonMain/composeResources/*/strings.xml` → nothing
- `grep -rL transactions_bank_review feature/transactions/src/commonMain/composeResources/*/strings.xml` → nothing
- `./gradlew :data:banksync:compileDebugKotlinAndroid :feature:transactions:compileDebugKotlinAndroid :shared:compileDebugKotlinAndroid`
- `./gradlew :feature:transactions:compileDebugUnitTestKotlinAndroid --no-configuration-cache`

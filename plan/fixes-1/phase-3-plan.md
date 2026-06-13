# Phase 3 — Bank Sync home layout (tasks 1 + 3)

## Goal
On `BankSyncHomeScreen`:
1. "Connect another bank account" becomes a **full-width button pinned at the very bottom** with a leading `+`, like the "add new budget" button. Same bottom-button layout applies in the not-connected state (body message centered, add button at bottom).
2. In `ControlsCard`, **Sync Now** and **Disconnect** each get their **own full-width stacked row** (no longer a side-by-side Row), and the old in-card connect button is removed (it moved to the bottom bar).

## File (single)
`feature/banksync/src/commonMain/kotlin/com/dv/moneym/feature/banksync/home/BankSyncHomeScreen.kt`

No new strings — reuse `bank_sync_connect_bank` (the "+" button text), `bank_sync_edit_credentials`, etc.

## Reference pattern (budgets bottom button)
`feature/budgets/.../list/BudgetListScreen.kt` ~lines 156-162:
```kotlin
MmButton(
    text = stringResource(Res.string.budgets_new),
    onClick = onCreate,
    variant = MmButtonVariant.Primary,
    fullWidth = true,
    leadingIcon = Icon.Plus.imageVector,
)
```
placed in a bottom `Box` with `navigationBarsPadding()`. `Icon` is `com.dv.moneym.core.model.Icon`; `imageVector` is `com.dv.moneym.core.ui.imageVector` (both already imported in this file).

## Restructure `BankSyncHomeContent`
Keep: `Box(fillMaxSize, bg)` outer, `MmLoadingOverlay`, and the disconnect `MmDeleteSheet`.

Inside the inner `Column(fillMaxSize)`:
- `ScreenHeader(...)` unchanged.
- `when`:
  - `state.isLoading` → `Unit` (nothing; loading overlay covers it).
  - `!state.configured` → `IntroState(...)` with `Modifier.weight(1f)` so it fills, **unchanged otherwise** (this is first-run before credentials exist; no bottom connect bar here).
  - `else` (configured) →
    - **Body** in a `Box(Modifier.weight(1f))`:
      - if `!state.connected`: a centered `Column` (verticalArrangement Center, horizontalAlignment CenterHorizontally, padding) containing `StatusCard(...)` and below it an **outline** `MmButton(text = bank_sync_edit_credentials, onClick = onNavigateToCredentials, variant = Outline)`. (The old `NotConnectedActions` Connect button is dropped — it's now the bottom bar.)
      - else (connected): the existing `LazyColumn` with StatusCard, accounts header + items (or `MmEmptyState`), and `ControlsCard`.
    - **Bottom bar**: a `Box(Modifier.fillMaxWidth().padding(horizontal = space.padding_2x, vertical = space.padding_1x).navigationBarsPadding())` containing
      ```kotlin
      MmButton(
          text = stringResource(Res.string.bank_sync_connect_bank),
          onClick = onNavigateToBankPicker,
          fullWidth = true,
          leadingIcon = Icon.Plus.imageVector,
      )
      ```

Add import `androidx.compose.foundation.layout.navigationBarsPadding`.

You may delete the now-unused `NotConnectedActions` composable (lines 248-267) since its Connect button moved to the bottom bar and Edit-credentials is inlined in the not-connected body. (If you prefer, keep a trimmed version — but no dead/unused composable should remain.)

## Restructure `ControlsCard`
Replace the side-by-side `Row` (lines 406-426) and the trailing connect `MmButton` (lines 427-433) with two stacked full-width buttons inside the existing `MmCard` (its content lambda is a ColumnScope — verify):
```kotlin
MmButton(
    text = if (state.isSyncing) stringResource(Res.string.bank_sync_syncing)
           else stringResource(Res.string.bank_sync_sync_now),
    onClick = { onIntent(BankSyncHomeIntent.SyncNow) },
    fullWidth = true,
    enabled = !state.isSyncing && state.connected,
    modifier = Modifier.padding(top = space.padding_1x),
)
MmButton(
    text = stringResource(Res.string.bank_sync_disconnect),
    onClick = { onIntent(BankSyncHomeIntent.RequestDisconnect) },
    variant = MmButtonVariant.Danger,
    fullWidth = true,
    modifier = Modifier.padding(top = space.padding_1x),
)
```
`ControlsCard` no longer needs the `onNavigateToBankPicker` param — remove it and update the call site (line 191-197).

Remove the now-unused `MmButtonSize` import only if nothing else uses it (StatusCard still uses `MmButtonSize.Sm` at line 304 — so keep the import).

## Verify
`./gradlew :feature:banksync:compileDebugKotlinAndroid`
No unused-import / unused-param warnings introduced; no leftover dead composables.

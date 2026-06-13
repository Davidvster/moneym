# Phase 5 — Sync button visibility + editable category/wallet rows

Repo: /Users/davidvalic/Developer/MoneyM. Three edits. No new strings (so no
locale work). Reuse `Icon.Edit` (a pencil; enum value exists in
`core/model/.../Icon.kt`) as the trailing "editable" affordance.

---

## Fix 1 — Sync button shows for ANY enabled sync (trivial)

File: `feature/transactions/.../list/TransactionListScreen.kt`, line 786.

Change:
```kotlin
if (state.isSyncEnabled || state.isBankSyncEnabled) {
```
to:
```kotlin
if (state.isSyncEnabled || state.isBankSyncEnabled || state.isWalletSyncEnabled) {
```
`state.isWalletSyncEnabled` already exists in `TransactionListUiState` and is
wired in the VM — no other change needed.

---

## Fix 2 — Suggestions category/wallet rows: clear, tappable, ripple

File: `feature/banksync/.../suggestions/SuggestionsScreen.kt`, inside
`SuggestionCard`, the `if (isPendingTab)` block (lines ~549-568). Today each is a
bare `Text` with `.clickable().padding()` — tiny target, weak feedback, and no
visual hint that it's editable.

Replace the **category** `Text` (lines ~550-557) with an editable row:
```kotlin
Row(
    modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(MM.dimen.padding_1x))
        .clickable { onIntent(SuggestionsIntent.ShowCategoryPicker(row.id)) }
        .padding(vertical = space.padding_1_5x, horizontal = space.padding_0_5x),
    verticalAlignment = Alignment.CenterVertically,
) {
    Text(
        text = stringResource(Res.string.suggestions_category_label) + ": " + (row.categoryName ?: "—"),
        style = MM.type.caption.copy(color = colors.text2),
        modifier = Modifier.weight(1f),
    )
    Icon(
        imageVector = Icon.Edit.imageVector,
        contentDescription = null,
        tint = colors.text3,
        modifier = Modifier.size(MM.dimen.padding_2x),
    )
}
```

Replace the **wallet/account** `Text` (lines ~558-568) with the same pattern,
preserving its danger-when-unset color:
```kotlin
Row(
    modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(MM.dimen.padding_1x))
        .clickable { onIntent(SuggestionsIntent.ShowAccountPicker(row.id)) }
        .padding(vertical = space.padding_1_5x, horizontal = space.padding_0_5x),
    verticalAlignment = Alignment.CenterVertically,
) {
    Text(
        text = stringResource(Res.string.bank_sync_account_target_label) + ": " +
            (row.targetAccountName ?: stringResource(Res.string.bank_sync_account_target_none)),
        style = MM.type.caption.copy(
            color = if (row.targetAccountId == null) colors.danger else colors.text2,
        ),
        modifier = Modifier.weight(1f),
    )
    Icon(
        imageVector = Icon.Edit.imageVector,
        contentDescription = null,
        tint = if (row.targetAccountId == null) colors.danger else colors.text3,
        modifier = Modifier.size(MM.dimen.padding_2x),
    )
}
```

Notes:
- `.clip(...)` before `.clickable()` bounds the Material ripple to the rounded
  rect. Material3 `clickable` provides the ripple via the theme's default
  indication — no extra setup needed.
- Use the Compose `Icon` already imported in the file. The icon vector comes from
  `Icon.Edit.imageVector` (`com.dv.moneym.core.model.Icon` + `imageVector`
  extension — likely already imported; add if missing).
- Add imports if absent: `androidx.compose.foundation.clickable` (already used),
  `androidx.compose.ui.draw.clip`, `androidx.compose.foundation.shape.RoundedCornerShape`,
  `androidx.compose.foundation.layout.size`, `androidx.compose.ui.Alignment`.

---

## Fix 3 — Bank-sync home wallet row: same treatment

File: `feature/banksync/.../home/BankSyncHomeScreen.kt`, inside `AccountCard`,
the target-account `Text` (lines ~352-360). Apply the identical editable-row
pattern as Fix 2's wallet row, wiring
`onIntent(BankSyncHomeIntent.ShowAccountPicker(account.uid))` and using
`targetName == null` for the danger color:
```kotlin
Row(
    modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(MM.dimen.padding_1x))
        .clickable { onIntent(BankSyncHomeIntent.ShowAccountPicker(account.uid)) }
        .padding(top = space.padding_1x, bottom = space.padding_0_5x, start = space.padding_0_5x, end = space.padding_0_5x),
    verticalAlignment = Alignment.CenterVertically,
) {
    Text(
        text = stringResource(Res.string.bank_sync_account_target_label) + ": " +
            (targetName ?: stringResource(Res.string.bank_sync_account_target_none)),
        style = MM.type.caption.copy(color = if (targetName == null) colors.danger else colors.text2),
        modifier = Modifier.weight(1f),
    )
    Icon(
        imageVector = Icon.Edit.imageVector,
        contentDescription = null,
        tint = if (targetName == null) colors.danger else colors.text3,
        modifier = Modifier.size(MM.dimen.padding_2x),
    )
}
```
Add the same imports if absent.

---

## Verify
```bash
./gradlew :feature:transactions:compileDebugKotlinAndroid \
          :feature:banksync:compileDebugKotlinAndroid
```
Both must compile. Report files changed, compile result, deviations. Do NOT
commit — the main thread commits.

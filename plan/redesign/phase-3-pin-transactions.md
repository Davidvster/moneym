# Phase 3 — PIN Screen + Transactions Screen

Migrate the two most-used screens. Depends on Phase 0 (tokens) and Phase 1 (components).

---

## Screens

### 01 · PIN Unlock / PIN Setup

**Files:**
- `feature/security/src/commonMain/…/ui/PinUnlockScreen.kt` — full rewrite
- `feature/security/src/commonMain/…/ui/PinSetupScreen.kt` — full rewrite
- `feature/security/src/commonMain/…/ui/PinKeypad.kt` — extract shared keypad composable

**Layout (from `screens.jsx` → `PinScreen`):**
```
Column(center, verticalCenter, 0 32dp padding) {
  // App lockup
  Box(56×56dp, RoundedCornerShape(16dp), bg=MM.colors.text) {
    Text("M", style=MM.type.title2.copy(28sp, Bold, color=MM.colors.bg))
  }
  Spacer(12dp)
  Text("MoneyM", style=MM.type.title2)
  Text("Enter your PIN", style=MM.type.caption.copy(color=MM.colors.text2))

  Spacer(48dp)

  // 4 dots
  Row(gap=18dp) {
    repeat(4) { i ->
      Box(12dp circle) {
        bg = if (i < filledCount) MM.colors.text else Color.Transparent
        border = 1.5dp if (i < filledCount) MM.colors.text else MM.colors.borderStrong
      }
    }
  }

  Spacer(48dp)

  // 3×4 keypad
  Grid(columns=3, gap=14dp) {
    "1".."9" -> KeyButton(80×72dp, 16dp radius, surface bg, 1dp border, 28sp text)
    FaceId   -> transparent 80×72dp, no border, Icon(faceId)
    "0"      -> KeyButton(same as digit)
    Delete   -> transparent, Icon(backspace)
  }
}
```

**Shake animation on wrong PIN:**
- `val shakeOffset = remember { Animatable(0f) }`
- On wrong PIN: launch { shakeOffset.animateTo(0f, keyframes { for 4 cycles: ±8dp, 300ms total }) }
- Apply via `Modifier.offset(x = shakeOffset.value.dp)` on the dots row.

**PinKeypad shared composable:**
```kotlin
@Composable fun PinKeypad(onKey: (Char) -> Unit, onDelete: () -> Unit, onBiometric: (() -> Unit)? = null)
```
Used by both PinUnlockScreen and PinSetupScreen.

---

### 02 · Transaction List

**Files:**
- `feature/transactions/src/commonMain/…/ui/TransactionListScreen.kt` — full rewrite
- `feature/transactions/src/commonMain/…/presentation/TransactionListUiState.kt` — add `txDisplayPrefs: TxDisplayPrefs`
- `feature/transactions/src/commonMain/…/presentation/TransactionListViewModel.kt` — observe `txDisplayPrefs` from `AppSettingsRepository`

**Layout:**
```
Column {
  // Header
  Row(16dp padding) {
    Text("Transactions", style=MM.type.title1)
    MmIconButton(MmIcons.search)
  }
  Row(gap=4dp, margin=12dp top, 16dp bottom) {
    MmIconButton(MmIcons.chevronLeft, 32dp)
    Text("May 2026", style=MM.type.body, minWidth=96dp, center)
    MmIconButton(MmIcons.chevronRight, 32dp)
    Spacer(weight=1f)
    Column(align=end) {
      Text("NET", style=MM.type.micro, color=text3)
      MmMoney(netAmount, "+", 17sp, 600, color=if net>0 accent else text)
    }
  }
  MmSegmented(["All","Expenses","Income"], selectedFilter)

  // Grouped list
  LazyColumn {
    for each date group:
      stickyHeader { SectionLabel(formattedDate, 20dp padding) }
      items(txList) { tx ->
        TxRow(tx, txDisplayPrefs, divider = tx != lastInGroup)
      }
  }

  // Pinned bottom button
  Box(borderTop=1dp divider, bg=MM.colors.bg, padding=12 16 16) {
    MmButton("New transaction", variant=primary, size=lg, fullWidth, leading=MmIcons.plus)
  }

  MmTabBar(active=TabRoute.Transactions, onTabSelected=…)
}
```

**`TxRow` composable** (shared, also used in Phase 6 preview):
```kotlin
@Composable fun TxRow(tx: Transaction, prefs: TxDisplayPrefs, divider: Boolean = true)
```
- Leading: `CategoryIconTile(tx.category, 38dp, prefs.indicatorStyle)`
- Primary text: tx.note if (hasNote && prefs.showNote) else tx.category.name
- Secondary text: tx.category.name if (hasNote && prefs.showNote && prefs.showCategoryName)
- Trailing: `MmMoney` — expenses in text color with "−", income in accent green with "+"
- Padding: 14×20dp (comfortable) or 10×20dp (compact)

**ViewModel change:**
```kotlin
val uiState: StateFlow<TransactionListUiState> = combine(
    transactionRepo.observeTransactions(…),
    appSettingsRepo.observeTxDisplayPrefs(),
) { txs, prefs -> TransactionListUiState(transactions = txs, txDisplayPrefs = prefs) }
    .stateIn(…)
```

---

## Key implementation notes

- `TxRow` lives in `core/ui` (not inside the feature) so it can be reused in the TxListDisplay preview (Phase 6).
- Month switcher in the header: click left/right updates a `YearMonth` in ViewModel state; list auto-filters.
- Net amount: sum all transactions in the selected month, positive = green accent.
- "New transaction" button calls the existing nav action to `TransactionEditScreen`.
- PinKeypad: key buttons use `Modifier.clickable(indication=null, interactionSource=…)` — no ripple.

---

## Verification
1. Transactions list shows correct grouped rows with date headers.
2. Segmented filter (All/Expenses/Income) filters correctly.
3. TxRow renders all 5 indicator styles from `TxDisplayPrefs` without crash.
4. PIN screen shake animation plays on wrong PIN entry.
5. Face ID button triggers biometric prompt (Android: BiometricPrompt, iOS: LAContext).

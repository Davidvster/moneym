# Phase 2: Month/year picker bounds + Recurring UI

## Item 2 — Month/year selector min bound when no transactions

### Root cause
When no transactions exist:
- `TransactionListUiState.earliestMonth = null` → picker `minYear/minMonth = null` → no restriction → can navigate to any past year
- `OverviewViewModel` `minIso = null` → `minSelectableDateIso = null` → same issue

### Fix A: TransactionListScreen.kt (lines 155-156)
File: `feature/transactions/src/commonMain/kotlin/com/dv/moneym/feature/transactions/list/TransactionListScreen.kt`

Current:
```kotlin
val minYear = state.earliestMonth?.year
val minMonth = state.earliestMonth?.monthNumber
```

Replace with:
```kotlin
val minYear = state.earliestMonth?.year ?: state.today?.year
val minMonth = state.earliestMonth?.monthNumber ?: state.today?.monthNumber
```

`state.today: LocalDate?` is already in `TransactionListUiState` (line 44 of UiState file).

### Fix B: OverviewViewModel.kt (line 150)
File: `feature/overview/src/commonMain/kotlin/com/dv/moneym/feature/overview/OverviewViewModel.kt`

Current:
```kotlin
minSelectableDateIso = minIso,
```

Replace with:
```kotlin
minSelectableDateIso = minIso ?: today.toString(),
```

`today` is a `LocalDate` property already available in scope of the ViewModel (injected via `AppClock`).

---

## Item 3 — Recurring transactions UI

### Fix: RecurringListScreen.kt
File: `feature/settings/src/commonMain/kotlin/com/dv/moneym/feature/settings/recurring/RecurringListScreen.kt`

**3a. Add string resource `settings_recurring_new` to all locales:**

- `feature/settings/src/commonMain/composeResources/values/strings.xml` → add: `<string name="settings_recurring_new">New recurring transaction</string>`
- `feature/settings/src/commonMain/composeResources/values-de/strings.xml` → add: `<string name="settings_recurring_new">Neue Dauertransaktion</string>`
- `feature/settings/src/commonMain/composeResources/values-es/strings.xml` → add: `<string name="settings_recurring_new">Nueva transacción recurrente</string>`
- `feature/settings/src/commonMain/composeResources/values-it/strings.xml` → add: `<string name="settings_recurring_new">Nuova transazione ricorrente</string>`

**3b. Replace FAB with bottom MmButton and fix empty state:**

Current structure of `RecurringListContent`:
```kotlin
Box(Modifier.fillMaxSize().background(MM.colors.bg)) {
    Column(Modifier.fillMaxSize()) {
        ScreenHeader(...)
        when { ... EmptyView() ... LazyColumn ... }
    }
    FloatingActionButton(onClick = onCreateNew, modifier = Modifier.align(Alignment.BottomEnd).padding(MM.dimen.padding_4x), ...) { ... }
}
```

Replace with:
```kotlin
Column(Modifier.fillMaxSize().background(MM.colors.bg)) {
    ScreenHeader(stringResource(Res.string.settings_recurring_title), onBack = onBack)
    Box(Modifier.weight(1f)) {
        when {
            state.isLoading -> Unit
            state.rules.isEmpty() -> EmptyView()
            else -> LazyColumn(modifier = Modifier.padding(MM.dimen.padding_2x)) {
                items(state.rules, key = { it.id.value }) { rule ->
                    MmCard(modifier = Modifier.padding(bottom = MM.dimen.padding_1x)) {
                        MmRow(onClick = { onEdit(rule.id) }) {
                            RuleSummary(rule = rule, categoryName = state.categories[rule.categoryId]?.name ?: "—")
                        }
                    }
                }
            }
        }
    }
    Box(
        modifier = Modifier.padding(
            horizontal = MM.dimen.padding_2_5x,
            vertical = MM.dimen.padding_2x,
        ),
    ) {
        MmButton(
            text = stringResource(Res.string.settings_recurring_new),
            onClick = onCreateNew,
            variant = MmButtonVariant.Secondary,
            fullWidth = true,
            leadingIcon = Icon.Plus.imageVector,
        )
    }
}
```

**3c. Fix EmptyView: add horizontal padding to text:**
```kotlin
@Composable
private fun EmptyView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(Res.string.settings_recurring_empty),
            style = MM.type.body,
            color = MM.colors.text3,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = MM.dimen.padding_4x),
        )
    }
}
```

**3d. Add imports to RecurringListScreen.kt:**
- `import com.dv.moneym.core.ui.MmButton`
- `import com.dv.moneym.core.ui.MmButtonVariant`
- `import androidx.compose.ui.text.style.TextAlign`
- `import moneym.feature.settings.generated.resources.settings_recurring_new`
- Remove: `import androidx.compose.material3.FloatingActionButton` (no longer used)
- Remove: `import androidx.compose.ui.graphics.Color` (no longer used for FAB contentColor)
- Remove: `import androidx.compose.foundation.layout.size` (no longer used)

---

## Build verification
```bash
./gradlew :composeApp:assembleDebug
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
```

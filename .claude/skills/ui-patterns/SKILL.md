---
name: ui-patterns
description: Conventions for building Compose Multiplatform screens and components in MoneyM — composable structure, state hoisting, theming, previews, accessibility, and i18n. Use when writing or reviewing any code under feature/*/ui/ or core/ui/ or core/designsystem/.
---

# UI implementation patterns

These rules apply to every Compose UI file in the project. Follow them mechanically — exceptions
need to be argued in the PR.

## File and package layout

```
feature/<featureName>/
  src/commonMain/kotlin/com/dv/moneym/feature/<featureName>/
    subfeature-name/
      <Screen>Screen.kt            // top-level composable (stateful wrapper + stateless content)
      components/                  // composables only used in this feature
      <Screen>ViewModel.kt
      <Screen>UiState.kt
      <Screen>Intent.kt
```

Reusable composables live in `core:ui`; theme tokens live in `core:designsystem`. Never duplicate
either.

## Stateful wrapper + stateless content

Every screen is split in two:

```kotlin
@Composable
fun TransactionListScreen(
    viewModel: TransactionListViewModel = koinViewModel(),
    onAddTransaction: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    TransactionListContent(
        state = state,
        onIntent = viewModel::onIntent,
        onAddTransaction = onAddTransaction,
    )
}

@Composable
private fun TransactionListContent(
    state: TransactionListUiState,
    onIntent: (TransactionListIntent) -> Unit,
    onAddTransaction: () -> Unit,
) { /* pure UI, no VM, no navigation calls */
}
```

The stateless `*Content` is the only thing tested and previewed. The wrapper exists to bind the
ViewModel and route callbacks outward.

## State hoisting

- Children receive immutable state + lambdas. Never pass a ViewModel down.
- Compute derived UI fields in the ViewModel — keep `*Content` dumb.
- No `remember { mutableStateOf(...) }` for screen state. Local-only UI state (e.g. a dropdown
  expanded flag, a dialog visibility) may stay in the composable.

## The UI is a dumb rendering layer — this is non-negotiable

**Composables only do two things: render state and emit intents.** They never contain business logic.

### What the UI MUST NOT do

```kotlin
// ❌ Filtering a list based on domain rules
val categories = state.availableCategories.filter { it.type == state.type }

// ❌ Computing derived domain values (percentages, totals, averages)
val percent = (cat.amount / total * 100).toInt()
val balance = state.income - state.expenses

// ❌ Comparing domain objects to make a conditional decision
val isToday = state.date == todayDate   // todayDate computed locally

// ❌ Selecting between data sets based on a local UI filter
val categories = when (localFilter) {
    Filter.All -> expenses + income
    Filter.Expenses -> expenses
    Filter.Income -> income
}

// ❌ Re-mapping or re-shaping domain data
val slices = categories.map { it.copy(percent = recalculate(it.amount, total)) }
```

All of the above belong in the ViewModel's `combine` block or `onIntent` handler, and the result goes directly into `UiState` as a ready-to-render field.

### What the UI IS allowed to do

```kotlin
// ✅ Direct read from state
Text(state.categoryName)

// ✅ Null/empty check for conditional rendering
if (state.categories.isEmpty()) EmptyState() else CategoryList(state.categories)

// ✅ Mapping domain data to a rendering primitive (color, size) — no business logic
val color = Color(cat.categoryColor)  // just a cast, not a decision

// ✅ String resource lookup from a typed state field
Text(stringResource(when (state.error) { is NoContent -> Res.string.error_no_content }))

// ✅ Local-only ephemeral UI state
var showDropdown by remember { mutableStateOf(false) }

// ✅ Sending an intent and letting the VM decide
MmButton(onClick = { onIntent(DateQuickTapped) })  // VM resolves yesterday vs today
```

### How to enforce the boundary

Before writing logic in a composable, ask: **can I write a pure Kotlin unit test for this?**
- Yes → it belongs in the ViewModel.
- No (needs Compose, context, resources) → it may stay in the composable.

## Theming and design system

- Always use `MaterialTheme.colorScheme.*` or the `MoneyMTheme` tokens. **Never hard-code hex.**
- Spacing comes from `MoneyMTheme.spacing` (4/8/12/16/24/32). No raw `MM.dimen.padding_1x` outside
  `core:designsystem`.
- Typography comes from `MaterialTheme.typography`. No raw `TextStyle` constructions in features.
- Icons via `core:designsystem`'s `MoneyMIcons` object — even if it forwards to Material icons,
  indirection lets us swap later.
- Category colors are the only color that varies per data. They come from `Category.colorHex` and
  are surfaced via a `categoryColor(...)` helper in `core:designsystem`.

## Light + dark mode

- All screens must render in both. Add a side-by-side preview using the multiplatform `@Preview`
  annotation, one light and one dark.
- Test minimum-contrast on tonal surfaces — `MoneyMTheme` is responsible, but eyeball your screen in
  dark mode before merging.

## Previews (NON-NEGOTIABLE)

**Every composable function — screens, components, and sub-components — MUST have at least two
previews: one light, one dark. Missing previews mean the task is incomplete.**

Each `*Content` composable gets at least two previews:

```kotlin
@Preview
@Composable
private fun TransactionListContentPreview_Light() {
    MoneyMTheme(darkTheme = false) {
        TransactionListContent(state = sampleState, onIntent = {}, onAddTransaction = {})
    }
}

@Preview
@Composable
private fun TransactionListContentPreview_Dark() {
    MoneyMTheme(darkTheme = true) { /* same */ }
}
```

Preview-only sample data lives in a sibling `*Samples.kt` file marked `internal`.

## Internationalization (NON-NEGOTIABLE)

**Hard rule: every user-visible string MUST use `stringResource(Res.string.x)`. No hardcoded
string literals in composables — not even "OK", "Cancel", or button labels.**

Strings live in the owning feature module under `commonMain/composeResources/values/strings.xml`.

**Every new string key MUST be added to all four locale files in the same change:**

| File | Locale |
|------|--------|
| `composeResources/values/strings.xml` | English (default) |
| `composeResources/values-de/strings.xml` | German |
| `composeResources/values-es/strings.xml` | Spanish |
| `composeResources/values-it/strings.xml` | Italian |

If you add a key to `values/` but not the others, the task is incomplete. Missing translations
fall back to English at runtime, but all four files must be updated before a task is considered
done.

Format arguments use `%1$s` / `%1$d` placeholders:

```xml
<string name="settings_import_transactions_selected">Transactions (%1$d selected)</string>
```

```kotlin
stringResource(Res.string.settings_import_transactions_selected, selectedCount)
```

Plural forms (1 item vs N items) are handled with two separate keys (`_one` / `_other`) since
Compose Multiplatform `stringResource` does not support Android-style `<plurals>` in commonMain.

## Accessibility

- Every actionable element (icon buttons, swipe affordances, chart segments) has a
  `contentDescription` or `Modifier.semantics { contentDescription = ... }`.
- Tap targets ≥ MM.dimen.padding_6x.
- Don't gate critical information on color alone — use icon + text/number alongside category color.

## Lists

- Use `LazyColumn` with stable keys (`key = { it.id }`) for transaction lists. Never index-based
  keys.
- Group headers (e.g. "Today", "Yesterday", date strings) are emitted via `stickyHeader`.

## What NOT to do

- Don't do `LaunchedEffect(Unit) { viewModel.load() }` from the screen — ViewModel loads in its
  `init`.
- Don't pass `NavController` into a composable. Navigation is one lambda callback per destination.
- Don't read `LocalContext.current` in `commonMain`. Use platform abstractions in `core:common`.
- Don't import `androidx.compose.material.*` (Material 2). Material 3 only.
- **Don't filter, map, group, sort, or compute inside a composable.** If you're about to write any
  collection operation on domain data inside a `@Composable` function, stop — that logic belongs
  in the ViewModel. Expose the already-prepared list in `UiState`.
- **Don't derive booleans from domain comparisons in the composable.** `val isToday = state.date == today`
  computed locally in a composable is wrong — the VM exposes `val isDateToday: Boolean` in state.
- **Don't maintain a local copy of domain state.** `var localFilter by remember { mutableStateOf(...) }`
  that shadows or overrides domain data is wrong — filter state belongs in the ViewModel with a
  corresponding intent (`FilterChanged`), so it survives navigation and is testable.
- Don't write a composable longer than 100 lines. Extract sub-composables instead.

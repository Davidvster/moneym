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
    ui/
      <Screen>Screen.kt            // top-level composable (stateful wrapper + stateless content)
      components/                  // composables only used in this feature
    presentation/
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
  expanded flag) may stay in the composable.

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

## Previews

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

## Internationalization

- Every user-visible string goes through Compose resources (`stringResource(Res.string.x)`).
- Strings live in `core:designsystem` or the owning feature module under
  `commonMain/composeResources/values/`.
- Supported locales for v1: `en` (default), `es`, `it`, `de`. Always update all four when adding a
  string. Missing translations fall back to English silently — that's fine for development, but
  `en`, `es`, `it`, `de` must all be present before release.

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

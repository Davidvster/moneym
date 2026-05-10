---
name: viewmodel-state
description: Conventions for ViewModels and UI state management in MoneyM — StateFlow-backed immutable state, intent-driven updates, one-shot effects, scope/dispatcher rules. Use when writing or reviewing any *ViewModel.kt or *UiState.kt.
---

# ViewModel and state management

Every screen's presentation logic lives in a ViewModel. We use the multiplatform `androidx.lifecycle.ViewModel` (via `lifecycle-viewmodel-compose`) so the same class works on Android and iOS.

## State shape

UI state is **one immutable data class per screen**, exposed as `StateFlow<UiState>`:

```kotlin
data class TransactionListUiState(
    val isLoading: Boolean = true,
    val days: List<DayGroup> = emptyList(),
    val filter: TransactionFilter = TransactionFilter.None,
    val error: TransactionListError? = null,
)
```

- One state class per screen, named `<Screen>UiState`.
- Default values represent the initial loading state.
- Fields are domain-shaped, **not** widget-shaped (no `Color`, no `Dp`, no `String` for amounts — pass `Money`/`Category`/etc. and let the UI format).
- Errors are typed (sealed class or enum). No raw `Throwable` in state.

## Intents

Inputs to the ViewModel arrive as a sealed `Intent`:

```kotlin
sealed interface TransactionListIntent {
    data class FilterChanged(val filter: TransactionFilter) : TransactionListIntent
    data class DeleteRequested(val id: TransactionId) : TransactionListIntent
    object Retry : TransactionListIntent
}

class TransactionListViewModel(...) : ViewModel() {
    fun onIntent(intent: TransactionListIntent) { /* when (intent) ... */ }
}
```

- One entry point: `onIntent(intent)`. No fan-out of public methods.
- Intents are nouns of user action ("FilterChanged"), not commands ("changeFilter").
- `init` block triggers initial loads; no `load()` lifecycle method called from UI.

## One-shot effects

Things like "navigate", "show snackbar", "dismiss sheet" don't belong in state — they fire once. Use a `Channel` (consumed as a Flow) or a `SharedFlow` with replay 0:

```kotlin
private val _effects = Channel<TransactionListEffect>(Channel.BUFFERED)
val effects: Flow<TransactionListEffect> = _effects.receiveAsFlow()
```

The screen collects effects in a `LaunchedEffect` and dispatches them to the navigation/snackbar host. **Navigation events never travel through `UiState`.**

## Reactive data

- Repositories expose `Flow<T>`. ViewModels `combine` them into `UiState`, then call `.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialUiState)`.
- Never call `.first()` to seed state — that breaks reactivity. Always derive from the live Flow.
- For derived/computed fields, compute inside `combine` or `map` — do not duplicate state.

## Coroutines

- Use `viewModelScope` for all launches.
- Inject a `DispatcherProvider` from `core:common`. Never reference `Dispatchers.IO` directly — there is no `Dispatchers.IO` on native, and tests need to swap dispatchers.
- For UseCase calls that block (DB writes, file IO), wrap with `withContext(dispatchers.io)` **at the UseCase or Repository layer**, not in the ViewModel.

## Lifecycle and Koin

- ViewModels are provided by Koin: `viewModel { TransactionListViewModel(get(), get()) }`.
- In the screen: `koinViewModel<TransactionListViewModel>()` (from `koin-compose-viewmodel`).
- Do not put ViewModels in the DI graph manually — always through the Koin `viewModel` DSL so lifecycle is handled.

## What goes in a ViewModel — and what doesn't

In the ViewModel:
- Combining and mapping repository Flows into UI state
- Calling UseCases in response to intents
- Emitting effects
- Lightweight formatting decisions that depend on domain logic ("show empty state when X")

NOT in the ViewModel:
- Strings: pass IDs / typed errors, let the UI resolve `stringResource(...)`
- Currency/date formatting: that's a Compose-side concern using locale
- Navigation: the ViewModel emits an effect; the screen does the navigating
- Platform APIs (Context, NSURL, etc.): platform glue belongs in `core:*` modules behind expect/actual

## Testing

ViewModel tests live in the feature module's `commonTest`. See the `testing` skill for setup. The contract a ViewModel test verifies:

1. Given an initial intent/state, the StateFlow emits the expected sequence.
2. Given a repository Flow change, state updates accordingly.
3. Effects are emitted exactly once and in order.

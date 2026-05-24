---
name: viewmodel-state
description: Conventions for ViewModels and UI state management in MoneyM — StateFlow-backed immutable state, intent-driven updates, one-shot effects, scope/dispatcher rules. Use when writing or reviewing any *ViewModel.kt or *UiState.kt.
---

# ViewModel and state management

Every screen's presentation logic lives in a ViewModel. We use the multiplatform `androidx.lifecycle.ViewModel` (via `lifecycle-viewmodel-compose`) so the same class works on Android and iOS.

## State shape

UI state is **one immutable data class per screen**, exposed as `StateFlow<UiState>`.

**No hardcoded date or year defaults.** Fields like `currentMonth: YearMonth?` and `today: LocalDate?` must be nullable with `null` defaults, populated by the VM from an injected `AppClock` on first emit. Compose screens guard with `?: return` until the first real emission. A literal `YearMonth(2026, 1)` as a state default is a bug — it freezes the UI to that month if the flow stalls. Preview-only defaults inside `@Preview` composables are fine.

```kotlin
@Serializable
internal data class TransactionListUiState(
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
internal sealed interface TransactionListIntent {
    data class FilterChanged(val filter: TransactionFilter) : TransactionListIntent
    data class DeleteRequested(val id: TransactionId) : TransactionListIntent
    object Retry : TransactionListIntent
}

class TransactionListViewModel(
    ...,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state by savedStateHandle.saved {
        MutableStateFlow(TransactionListUiState())
    }
    internal val state: StateFlow<TransactionListUiState> = _state
        .onStart { init() }
        .stateIn(viewModelScope, SharingStarted.Lazily, _state.value)

    private val _effects = Channel<TransactionListEffect>(Channel.BUFFERED)
    internal val effects = _effects.receiveAsFlow()

    private suspend fun init() {
        // initial action
    }

    internal fun onIntent(intent: TransactionListIntent) { /* when (intent) ... */ }
}
```

- One entry point: `internal fun onIntent(intent)`. No fan-out of public methods.
- Intents are nouns of user action ("FilterChanged"), not commands ("changeFilter").
- `suspend fun init()` block triggers initial loads; no `load()` lifecycle method called from UI.
- No other internal/public function apart from onIntent(intent), the UI can only communicate to the ViewModel via intent.

## One-shot effects

Things like "navigate", "show snackbar", "dismiss sheet" don't belong in state — they fire once. Use a `Channel` (consumed as a Flow) or a `SharedFlow` with replay 0:

```kotlin
private val _effects = Channel<TransactionListEffect>(Channel.BUFFERED)
internal val effects: Flow<TransactionListEffect> = _effects.receiveAsFlow()
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
- **The VM class must be `public`** (the Kotlin default — do not mark with `internal`) if it is registered in `composeApp/src/commonMain/.../di/FeatureModules.kt`. `composeApp` cannot see an `internal` class declared in a sibling module. Same applies to UseCases registered there.

## Business logic belongs in the ViewModel — never in the UI

**Hard rule: the UI is a dumb rendering layer.** It reads `UiState`, renders it, and sends `Intent` back. It never derives domain values, filters lists, computes percentages, compares domain objects, or decides what to show beyond simple null checks.

If you find yourself writing any of the following inside a composable, stop and move it to the ViewModel:

| Pattern in composable | Where it belongs |
|---|---|
| `list.filter { it.type == state.type }` | VM: expose pre-filtered list in state |
| `items.map { it.copy(percent = (it.amount / total * 100).toInt()) }` | VM: compute derived fields inside `combine` |
| `val isToday = state.date == todayDate` | VM: expose `val isDateToday: Boolean` in `UiState` |
| `if (state.type == EXPENSE) expenseCategories else incomeCategories` | VM: expose one `val categories` already resolved |
| `val total = if (filter == All) income + expenses else expenses` | VM: expose `val displayedTotal: Double` |
| `items.groupBy { it.category }` | VM or UseCase |
| business `when` branches on domain enums | VM |

### Correct pattern

```kotlin
// ❌ Wrong — composable deciding which list to show
@Composable
fun CategoryPicker(state: EditUiState) {
    val categories = state.availableCategories.filter { it.type == state.type }
    // ...
}

// ✅ Correct — VM resolves, composable just renders
// In ViewModel:
is EditIntent.TypeChanged -> _state.update {
    it.copy(
        type = intent.type,
        selectedCategoryId = null,
        visibleCategories = allCategories.filter { c -> c.type == intent.type },
    )
}
// In composable:
CategoryPicker(categories = state.visibleCategories)
```

```kotlin
// ❌ Wrong — composable computing percentages
val slices = categories.map {
    it.copy(percent = ((it.amount / total) * 100).toInt())
}

// ✅ Correct — VM computes, composable reads
// In ViewModel combine block:
categoryBreakdown = rawCategories.map { cs ->
    cs.copy(percent = if (totalMinor > 0) ((cs.amountMinor * 100) / totalMinor).toInt() else 0)
}
// In composable:
val slices = state.categoryBreakdown.map { DonutSlice(Color(it.categoryColor), it.percent / 100f) }
```

### The test tells you who owns it

If the logic is worth a unit test, it belongs in the ViewModel. Composables are not unit-tested in this project. If you can't test it without rendering Compose, it's allowed in the composable; if you can reason about it with pure Kotlin, put it in the ViewModel.

## What goes in a ViewModel — and what doesn't

In the ViewModel:
- Combining and mapping repository Flows into UI state
- Calling UseCases in response to intents
- Emitting effects
- **All filtering, mapping, computing, and deriving of data before it reaches the UI**
- Boolean flags the UI reads directly: `val isDateToday: Boolean`, `val hasSelection: Boolean`, `val showEmptyState: Boolean`

NOT in the ViewModel:
- **Strings**: never put user-visible text in ViewModel state as raw `String`. Use a typed error
  (sealed class / enum) and let the UI call `stringResource(...)` to translate it. A raw string
  in state bypasses localisation and is untestable. Example of the correct pattern:
  ```kotlin
  // ❌ Wrong — hardcoded English leaks into state
  _state.update { it.copy(error = "No file content") }

  // ✅ Correct — typed error, UI translates
  sealed interface ImportError { data object NoContent : ImportError; data object ParseFailed : ImportError }
  // UiState holds: val error: ImportError? = null
  // Screen: Text(stringResource(when (state.error) { ImportError.NoContent -> Res.string.import_error_no_content ... }))
  ```
- Currency/date formatting: that's a Compose-side concern using locale
- Navigation: the ViewModel emits an effect; the screen does the navigating
- Platform APIs (Context, NSURL, etc.): platform glue belongs in `core:*` modules behind expect/actual
- **Color, Dp, TextStyle**: state fields are domain types only. Never `Color`, `Dp`, or typography in `UiState`.

## Testing (NON-NEGOTIABLE)

**Every ViewModel MUST have unit tests. Writing a ViewModel without tests means the task is
incomplete.** ViewModel tests live in the feature module's `commonTest`. See the `testing` skill
for setup. The contract a ViewModel test verifies:

1. Given an initial intent/state, the StateFlow emits the expected sequence.
2. Given a repository Flow change, state updates accordingly.
3. Effects are emitted exactly once and in order.

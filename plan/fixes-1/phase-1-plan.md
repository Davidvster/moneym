# Phase 1 — Recency-weighted note suggestions

Rank transaction note suggestions by exponential time decay instead of all-time count, so recently-used notes win.

## Algorithm
Per candidate note: `score = Σ over each use  0.5.pow(ageDays / 30.0)`, where `ageDays = max(0, occurredOn.daysUntil(today))`. Half-life 30 days.
Ranking: prefix-matches (note lowercased starts with query lowercased) first, then contains-matches, each group sorted by score desc. Exclude notes exactly equal to the raw query. Take top `limit` (5).

## File 1 (NEW): `feature/transactionEdit/src/commonMain/kotlin/com/dv/moneym/feature/transactionedit/usecase/SuggestNotesUseCase.kt`

Plain class, no Compose/Flow/coroutine in body. Package `com.dv.moneym.feature.transactionedit.usecase`.

```kotlin
package com.dv.moneym.feature.transactionedit.usecase

import com.dv.moneym.core.model.Transaction
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlin.math.pow

class SuggestNotesUseCase {
    operator fun invoke(
        transactions: List<Transaction>,
        query: String,
        today: LocalDate,
        limit: Int = 5,
    ): List<String> {
        if (query.isBlank()) return emptyList()

        val scores = HashMap<String, Double>()
        for (txn in transactions) {
            val note = txn.note?.takeIf { it.isNotBlank() } ?: continue
            val ageDays = txn.occurredOn.daysUntil(today).coerceAtLeast(0)
            val weight = 0.5.pow(ageDays / 30.0)
            scores[note] = (scores[note] ?: 0.0) + weight
        }

        val q = query.lowercase()
        val prefix = scores.entries
            .filter { it.key.lowercase().startsWith(q) && it.key != query }
            .sortedByDescending { it.value }
            .map { it.key }
        val contains = scores.entries
            .filter { it.key.lowercase().contains(q) && !it.key.lowercase().startsWith(q) && it.key != query }
            .sortedByDescending { it.value }
            .map { it.key }

        return (prefix + contains).take(limit)
    }
}
```
Verify `Transaction.occurredOn` is `LocalDate` and `Transaction.note` is `String?` (it is). `daysUntil` is on `kotlinx.datetime.LocalDate`.

## File 2 (EDIT): `feature/transactionEdit/src/commonMain/kotlin/com/dv/moneym/feature/transactionedit/TransactionEditViewModel.kt`

- Add constructor param `private val suggestNotes: SuggestNotesUseCase,` (place it just before `dispatchers`). Add import `com.dv.moneym.feature.transactionedit.usecase.SuggestNotesUseCase`.
- Note: the injected clock is named `clock` (field `clock: AppClock`), and there is already `private val today = clock.today()` at top of the class — reuse `today`.
- Replace the body of `updateNoteSuggestions(query)` (currently lines ~339–369). Keep the blank-query early return and the IO fetch; swap the inline `eachCount`/sort block for the use case:

```kotlin
private fun updateNoteSuggestions(query: String) {
    viewModelScope.launch {
        if (query.isBlank()) {
            _state.update { it.copy(noteSuggestions = emptyList()) }
            return@launch
        }
        val allTxns = withContext(dispatchers.io) {
            transactionRepository.observeAll().first()
        }
        _state.update { it.copy(noteSuggestions = suggestNotes(allTxns, query, today)) }
    }
}
```

## File 3 (EDIT): `composeApp/src/commonMain/kotlin/com/dv/moneym/di/FeatureModules.kt`

- Add `single { SuggestNotesUseCase() }` near the other transactionEdit use-case singles (around line 107, after `ValidateAndBuildTransactionUseCase`).
- Add `suggestNotes = get(),` to the `TransactionEditViewModel(...)` constructor block (after `computeBudgetRemaining = get(),`, before `dispatchers = get(),`).
- Add import `com.dv.moneym.feature.transactionedit.usecase.SuggestNotesUseCase`.

## Test (NEW): `feature/transactionEdit/src/commonTest/kotlin/com/dv/moneym/feature/transactionedit/usecase/SuggestNotesUseCaseTest.kt`
Build `Transaction` instances (check the model's required fields) with differing `occurredOn`. Assert a note used yesterday with 1 occurrence outranks a note used ~1 year ago with 5 occurrences, for a query matching both. Use a fixed `today = LocalDate(2026, 6, 1)`. If constructing `Transaction` is heavy, keep the test minimal but compiling; if it proves too fiddly, skip the test rather than block the build.

## Constraints
- Import classes, no FQNs (CLAUDE.md). Use case stays `public` (default) for cross-module Koin.
- Do not touch other files.

## Verify
`./gradlew :feature:transactionEdit:compileDebugKotlinAndroid` and, if test added, `:feature:transactionEdit:testDebugUnitTest`.

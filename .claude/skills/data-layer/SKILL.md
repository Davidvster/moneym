---
name: data-layer
description: Conventions for the data layer in MoneyM — Repository and DataSource interfaces, SQLDelight schema/query conventions, Flow boundaries, mapping rules, migrations, and the line between domain models and DB row types. Use when writing or reviewing any data/*/ module.
---

# Data layer

## Layers inside a data module

```
data/<name>/
  src/commonMain/
    kotlin/com/dv/moneym/data/<name>/
      <Name>Repository.kt          # interface — public API
      internal/
        <Name>RepositoryImpl.kt    # implementation, package-private
        <Name>LocalDataSource.kt   # interface
        SqlDelight<Name>DataSource.kt  # impl backed by SQLDelight
        <Name>Mappers.kt           # row <-> domain
    sqldelight/com/dv/moneym/data/<name>/
      <Name>.sq                    # schema + queries
```

The repository is the only thing other modules import. Implementations live under `internal/` and are not exposed publicly.

## Repository contract

```kotlin
interface TransactionRepository {
    fun observeAll(): Flow<List<Transaction>>
    fun observeByDay(date: LocalDate): Flow<List<Transaction>>
    fun observeFiltered(filter: TransactionFilter): Flow<List<Transaction>>

    suspend fun get(id: TransactionId): Transaction?
    suspend fun upsert(transaction: Transaction): TransactionId
    suspend fun delete(id: TransactionId)
}
```

- **Observable reads return `Flow<T>`.** No `suspend fun getAll(): List<T>` — callers can't react to changes from that.
- **Writes are `suspend fun`** and return either `Unit`, the new id, or the affected count.
- The repository **does not throw for missing rows** — return null. Reserve exceptions for truly exceptional cases (DB corruption); model expected failures with sealed result types or nullable returns.

## DataSource

DataSources are thin wrappers around the SQLDelight queries. They:
- Expose the same observable / suspend shape as the repository, but in terms of generated SQLDelight row types.
- Use `.asFlow().mapToList(dispatchers.io)` from SQLDelight's coroutines extension.
- **Never** map to domain types — that's the repository's job.

The split exists so we can swap data sources (e.g. add a `RemoteTransactionDataSource` later for sync) without rewriting repositories.

## Mapping

Mapping between DB rows and domain types lives in `<Name>Mappers.kt` as top-level `internal` extension functions:

```kotlin
internal fun TransactionRow.toDomain(): Transaction = Transaction(
    id = TransactionId(id),
    amount = Money(amountMinor, CurrencyCode(currency)),
    ...
)

internal fun Transaction.toRow(): TransactionRow = ...
```

Map at the repository boundary — DataSources speak SQL row types, the rest of the app speaks domain types.

## SQLDelight conventions

- One `.sq` file per table, named after the table.
- Schema comes first, then named queries.
- Query names are verbs: `selectAll`, `selectById`, `selectByDay`, `insert`, `update`, `delete`. No `findX`.
- Use `INTEGER PRIMARY KEY` IDs as `Long`, wrapped in a value class (`TransactionId(value: Long)`) at the domain layer.
- Monetary amounts are stored as `INTEGER NOT NULL` minor units (cents). Currency is a separate `TEXT NOT NULL` column holding the ISO-4217 code.
- Dates are stored as `TEXT NOT NULL` in `yyyy-MM-dd` for `LocalDate`, or epoch milliseconds (`INTEGER`) for instants. Pick one and document it in the schema.

## Migrations

- Schema version is tracked by SQLDelight automatically based on `.sq` and `.sqm` files.
- **Never edit an existing `.sq` schema once shipped.** Add a `<n>.sqm` migration file at the same path.
- Every migration is one-way and idempotent. We don't ship downgrades.
- Test every migration in `androidUnitTest` with a real driver against the previous schema dump.

## Flow boundaries

- Repositories return cold Flows directly from SQLDelight. Don't `.share()` or `.stateIn` inside the repository — let the ViewModel decide hot/cold.
- Don't expose `MutableStateFlow` from a repository. If you need an in-memory cache, hide it behind the `Flow<T>` return type.
- All Flow emissions happen on the IO dispatcher (handled by `.mapToList(dispatchers.io)`).

## Transactions

When a write needs to touch multiple tables, use SQLDelight's `transaction { ... }` block, called from the DataSource. Repositories should not know about SQL transactions.

## Initial seed data

Default categories (groceries, eating out, rent, transport, etc.) are seeded by `data:categories` on first run. The pattern:
- Schema includes an `is_user_created INTEGER NOT NULL` column.
- A `CategorySeedUseCase` runs once on app startup, inserts defaults if the table is empty, never overwrites user data.
- Seed list lives as a constant in `data:categories` — not in `feature:*`.

## Error handling

- DB-level failures bubble up as `SqlException` — let them propagate; we catch and log at the app shell, then show a generic error state.
- Validation belongs to UseCases / domain (e.g. "amount must be > 0"), not to the repository.

## What NOT to put in a data module

- Compose dependencies (UI is upstream)
- ViewModels or UseCases (those live in features)
- Cross-data-module imports — if `data:transactions` needs categories, depend on `data:categories`, don't reach in
- Networking code yet — Phase 6 will add a sync module

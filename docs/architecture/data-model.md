# Data model

Local-first SQLDelight schema. Domain types in `core:model`. Mapping between rows and domain happens at the repository boundary.

## Entities

### Money

A value object — not a table.

```kotlin
@JvmInline value class CurrencyCode(val value: String)  // ISO-4217, e.g. "EUR"

data class Money(
    val minorUnits: Long,         // e.g. 1234 = 12.34 EUR
    val currency: CurrencyCode,
)
```

We store amounts as **minor units** (cents) in a `Long`. No floats anywhere — they introduce rounding errors that compound across totals. The UI formats according to the device locale via `core:common` formatters.

### Transaction

```sql
CREATE TABLE TransactionRow (
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    type              TEXT    NOT NULL,    -- 'INCOME' | 'EXPENSE'
    amount_minor      INTEGER NOT NULL,
    currency          TEXT    NOT NULL,    -- ISO-4217
    occurred_on       TEXT    NOT NULL,    -- 'yyyy-MM-dd'
    note              TEXT,
    category_id       INTEGER NOT NULL REFERENCES CategoryRow(id),
    account_id        INTEGER NOT NULL REFERENCES AccountRow(id),
    recurrence_rule   TEXT,                -- reserved for v2; nullable, ignored in v1
    created_at        INTEGER NOT NULL,    -- epoch millis
    updated_at        INTEGER NOT NULL
);

CREATE INDEX idx_transaction_date     ON TransactionRow(occurred_on);
CREATE INDEX idx_transaction_category ON TransactionRow(category_id);
CREATE INDEX idx_transaction_account  ON TransactionRow(account_id);
```

```kotlin
@JvmInline value class TransactionId(val value: Long)

enum class TransactionType { INCOME, EXPENSE }

data class Transaction(
    val id: TransactionId,
    val type: TransactionType,
    val amount: Money,
    val occurredOn: LocalDate,
    val note: String?,
    val categoryId: CategoryId,
    val accountId: AccountId,
    val createdAt: Instant,
    val updatedAt: Instant,
)
```

### Category

```sql
CREATE TABLE CategoryRow (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    name            TEXT    NOT NULL,
    icon_key        TEXT    NOT NULL,         -- maps to MoneyMIcons table in designsystem
    color_hex       TEXT    NOT NULL,         -- '#RRGGBB'
    is_user_created INTEGER NOT NULL,         -- 0 for seeds, 1 for user-created
    archived        INTEGER NOT NULL DEFAULT 0,
    created_at      INTEGER NOT NULL,
    updated_at      INTEGER NOT NULL
);
```

Default seeds (inserted on first run, `is_user_created = 0`):

| Name | Icon key | Color |
|---|---|---|
| Groceries | `cart` | `#7E9C8C` |
| Eating out | `restaurant` | `#C97B57` |
| Rent | `home` | `#5F6F8A` |
| Transport | `car` | `#3B7080` |
| Utilities | `bolt` | `#B89A4B` |
| Health | `heart_pulse` | `#9B5C7D` |
| Entertainment | `play_circle` | `#7C5C9B` |
| Shopping | `bag` | `#6D6D6D` |
| Salary (income) | `wallet` | `#4A7A56` |
| Other (expense) | `dots` | `#8A8A8A` |
| Other (income) | `dots` | `#8A8A8A` |

Categories cannot be deleted if any transaction references them; they can be **archived** (hidden from new-transaction picker but still rendered on historical data). The user can edit any category, including seeds.

### Account

```sql
CREATE TABLE AccountRow (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    name            TEXT    NOT NULL,
    type            TEXT    NOT NULL,    -- 'CASH' | 'BANK' | 'CARD' | 'OTHER'
    currency        TEXT    NOT NULL,
    is_default      INTEGER NOT NULL,    -- 0 / 1; exactly one row has 1
    archived        INTEGER NOT NULL DEFAULT 0,
    created_at      INTEGER NOT NULL,
    updated_at      INTEGER NOT NULL
);
```

On first run we insert a single default account ("Main", currency = device locale's currency). The UI hides the account picker when there's only one — adding a second one unlocks the picker.

### AppSettings (preferences, not SQL)

Stored via `multiplatform-settings`. Keys:

- `pref.default_currency` — `String` (ISO-4217)
- `pref.theme_mode` — `"system" | "light" | "dark"`
- `pref.pin_enabled` — `Boolean`
- `pref.biometric_enabled` — `Boolean`
- `pref.background_lock_seconds` — `Int` (default 30)
- `pref.last_export_at` — `Long?` (epoch millis)
- `pref.onboarding_completed` — `Boolean`

Secret material (the PIN hash + salt) goes through `core:security`, NOT plain preferences. See `security.md`.

## Migrations

SQLDelight tracks the schema version. The release schema is `1.sq`. Subsequent changes ship as `.sqm` files. Rules:

- Never edit a shipped `.sq`. Add a migration.
- One migration per schema version bump. Atomic, idempotent, one-way.
- Each migration has a test in `androidUnitTest` that loads the prior schema dump, applies the migration, and asserts the new shape and data preservation.

## Read patterns and their indexes

The transaction list screen drives the schema. The dominant queries:

1. "All transactions for a given month, grouped by day, newest first" — `idx_transaction_date`.
2. "Filtered by category" — `idx_transaction_category` + date.
3. "Filtered by type (income/expense)" — sequential scan with date predicate; type column not indexed (cardinality is 2).
4. "Aggregates for overview" — sum/group queries, mostly bounded by date — `idx_transaction_date` suffices.

If profiling shows otherwise after we have real data, we add covering indexes in a migration.

## Import / export

Both JSON and CSV serialize the same logical shape: transactions, categories, accounts, settings (excluding secrets). See `import-export.md` for formats.

## Future schema reservations

- `recurrence_rule TEXT` on `TransactionRow` — placeholder for v2 recurring transactions (likely RRULE format).
- Sync metadata: each table will gain `remote_id TEXT`, `last_synced_at INTEGER`, `is_dirty INTEGER` columns when we add cloud backup. We do **not** add these in v1 — they go in a migration so the change is auditable.
- Attachments: explicitly out of scope; if we add them, they're a new table referencing transactions, not a column on transactions.

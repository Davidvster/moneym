# Import / export

Import/export is the **foundation of backup/restore** and the seam where future cloud sync will plug in. v1 ships file-based JSON and CSV; cloud destinations come later without breaking the format.

## What's exported

- All transactions
- All categories (including user-created)
- All accounts
- Non-secret app settings (theme, default currency, background-lock seconds)

**Not exported**: PIN hash, biometric binding state, attempt counters, anything in `core:security`'s `SecureStore`.

## Formats

### JSON

Canonical, lossless. Used for round-trip backup/restore.

```json
{
  "moneym": {
    "version": 1,
    "exportedAt": "2026-05-11T10:15:00Z",
    "currency": "EUR"
  },
  "accounts": [
    { "id": 1, "name": "Main", "type": "CASH", "currency": "EUR", "isDefault": true, "archived": false, "createdAt": 1700000000000, "updatedAt": 1700000000000 }
  ],
  "categories": [
    { "id": 1, "name": "Groceries", "iconKey": "cart", "colorHex": "#7E9C8C", "isUserCreated": false, "archived": false, "createdAt": 1700000000000, "updatedAt": 1700000000000 }
  ],
  "transactions": [
    { "id": 1, "type": "EXPENSE", "amountMinor": 1234, "currency": "EUR", "occurredOn": "2026-05-10", "note": "lunch", "categoryId": 1, "accountId": 1, "createdAt": 1715000000000, "updatedAt": 1715000000000 }
  ],
  "settings": {
    "themeMode": "system",
    "backgroundLockSeconds": 30
  }
}
```

Rules:
- `version` is bumped whenever the structure changes. Importers read older versions; we never break older exports.
- IDs in the export are the local IDs at export time — on import they are remapped (see "Import semantics" below).
- Timestamps are epoch milliseconds. `occurredOn` is `yyyy-MM-dd` (no timezone).
- All currency fields are ISO-4217 codes.

### CSV

Human-friendly, lossy at the structure level (one file per entity). Designed for spreadsheets and quick edits.

**`transactions.csv`** — one row per transaction:

```
date,type,amount,currency,category,account,note
2026-05-10,EXPENSE,12.34,EUR,Groceries,Main,lunch
```

- `amount` uses major units with a `.` decimal separator (locale-independent — we control the format).
- `category` and `account` are looked up by name on import. If the name is missing, the import either creates a stub (default) or fails (strict mode), depending on user choice.

**`categories.csv`** — optional export, useful for users who want to bulk-edit names/colors:

```
name,icon_key,color_hex,is_user_created,archived
Groceries,cart,#7E9C8C,false,false
```

**`accounts.csv`** — same pattern.

CSV exports are emitted as a zip when more than one entity is included (`moneym-export-2026-05-11.zip`).

## Import semantics

Import is **always additive by default**. We never silently delete or overwrite local data. The user gets a preview before any write occurs:

```
You're about to import:
  • 312 transactions   (new: 312, duplicates: 0)
  • 14 categories      (new: 4, matches: 10)
  • 2 accounts         (new: 1, matches: 1)
```

Modes:

| Mode | Behaviour |
|---|---|
| **Merge** (default) | Add new rows. Match existing rows by stable keys (see below). Skip duplicates. |
| **Replace** | Wipe all transactions/categories/accounts first, then import. User must explicitly confirm with a typed confirmation string. PIN material is preserved. |

### Match keys

For JSON imports (which carry full records):
- `Category` matched by `name + iconKey + colorHex`.
- `Account` matched by `name + type + currency`.
- `Transaction` matched by `occurredOn + amountMinor + currency + categoryName + accountName + note`. Conservative — false negatives create duplicates, which is recoverable; false positives lose data, which is not.

For CSV imports:
- Categories/accounts referenced by name. Unknown names create new rows.
- Transactions matched on the same composite key as above.

### Validation

Every import runs through a validator before writing anything:

- Schema: all required fields present, valid types, ISO-4217 currency codes, valid hex colors.
- Referential integrity: every transaction's `categoryId` / category name and `accountId` / account name resolves.
- Date sanity: `occurredOn` is a valid date, within a reasonable range (1970–2100).
- Money sanity: `amountMinor >= 0` (sign is encoded by `type`).

A single invalid row in a CSV is reported with a line number and either skipped (lenient mode, default) or aborts the import (strict mode). For JSON, any validation failure aborts — JSON is supposed to be lossless and machine-generated.

## Architecture

Lives in `data:backup`:

```
data/backup/
  src/commonMain/kotlin/com/dv/moneym/data/backup/
    BackupExporter.kt           // public: writes a stream
    BackupImporter.kt           // public: reads a stream, returns Preview, then Apply
    json/
      JsonExporter.kt
      JsonImporter.kt
      ExportDto.kt              // serialization-only DTOs
    csv/
      CsvExporter.kt
      CsvImporter.kt
    internal/
      ImportPreview.kt
      ImportMode.kt
      Validation.kt
```

The exporter takes a `Sink` (Okio); the importer takes a `Source`. Platform-specific file-picking is handled in `feature:settings` via expect/actual — `core:security` is not involved.

## Forward-compatibility for cloud sync

When we add cloud backup (e.g. Google Drive), the contract stays the same:
- `BackupExporter.exportToJson(sink)` produces the same bytes.
- A new `data:sync` module pumps that stream to the cloud provider.
- Conflict resolution happens at the import layer using the same merge logic, with an added "remote vs local timestamp" rule.

What this means for v1: keep the exporter/importer **pure** (no file system, no UI). Anything platform- or destination-specific lives outside `data:backup`.

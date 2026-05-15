# Phase 7 — Import / export

**Status**: 📝 Sketch (expand when starting)

Goal: JSON and CSV export/import per `import-export.md`. Treated as the foundation for future cloud sync — keep exporter/importer pure (no file system, no UI).

**Exit criteria**: export JSON → wipe DB → import → state matches. Export CSV → opens correctly in spreadsheet. Import preview shows accurate new/duplicate/update counts.

## Steps (will expand when starting)

- [ ] **7.1** `data:backup` — `ExportDto` (DTOs, serialization-only)
- [ ] **7.2** `data:backup` — `JsonExporter.exportToJson(sink: Sink)` — pure, takes an Okio sink
- [ ] **7.3** `data:backup` — `JsonImporter.preview(source: Source): ImportPreview` and `apply(preview, mode: ImportMode)`
- [ ] **7.4** `data:backup` — `CsvExporter` (writes a zip when multiple entities included)
- [ ] **7.5** `data:backup` — `CsvImporter` with lenient (default) and strict modes
- [ ] **7.6** `data:backup` — validation layer (schema, referential integrity, date/money sanity)
- [ ] **7.7** Match keys: composite stable keys per `import-export.md` (categories, accounts, transactions)
- [ ] **7.8** `feature:settings` — Export entry (pick JSON or CSV, choose location via platform picker)
- [ ] **7.9** `feature:settings` — Import flow (file picker → preview screen → confirm with mode selection)
- [ ] **7.10** Platform file picker via expect/actual in `feature:settings`:
  - [ ] **7.10.1** Android: Storage Access Framework (`ACTION_OPEN_DOCUMENT` / `ACTION_CREATE_DOCUMENT`)
  - [ ] **7.10.2** iOS: `UIDocumentPicker`
- [ ] **7.11** Replace mode: typed confirmation string required (per `import-export.md`)
- [ ] **7.12** Round-trip test: export → clear DB → import → state equality
- [ ] **7.13** Edge-case tests: invalid CSV row (line number reporting), unknown category in CSV (creates stub vs aborts in strict)
- [ ] **7.14** Strings for en/es/it/de

---

## Concrete implementation notes (for fresh sessions)

### What already exists

**`data:backup`** skeleton module exists — `data/backup/src/commonMain/.../` dirs created, empty.
- `build.gradle.kts` has `kotlinx-serialization-json` and `kotlinSerialization` plugin
- No Okio dep yet — add if needed, or use `kotlinx.serialization` directly with `String`/`ByteArray` sinks

### data:backup build.gradle.kts — Okio decision

The plan references `Sink` / `Source` from Okio but Okio is not in `libs.versions.toml`. For Phase 7 either:
1. Use `String` / `ByteArray` as IO types (simpler — no extra dep)
2. Add Okio: `okio = "3.9.0"` to catalog, `io.github.kotlin-multiplatform:okio:3.9.0` to backup deps

Recommendation: use `String` output / `String` input for JSON. Caller (feature:settings) handles file I/O. CSV zips may need ByteArray or Okio.

### `data:backup` has no `internal` exposure issue

Unlike data repos, `BackupExporter` and `BackupImporter` are public classes called directly from `feature:settings`. No factory function needed.

### Repositories available for reading (all in `composeApp` Koin graph)

- `CategoryRepository` → `get<CategoryRepository>()` in Koin
- `AccountRepository` → `get<AccountRepository>()`
- `TransactionRepository` → `get<TransactionRepository>()`
- `AppSettings` → `get<AppSettings>()` (for settings export)

Wire `BackupExporter` / `BackupImporter` in `data:backup` with constructor injection — they take repositories as constructor params.

### JSON export DTO pattern

DTOs are `@Serializable` data classes separate from domain models. Map domain → DTO in exporter, DTO → domain in importer. Keep DTOs in `data:backup/internal/`. Use `kotlinx.serialization.json.Json` configured with `prettyPrint = false`.

### Platform file picker (step 7.10)

Lives in `feature:settings` with expect/actual. Create:
- `feature/settings/src/commonMain/.../FilePicker.kt` — `interface FilePicker { suspend fun pickReadFile(): ByteArray? ; suspend fun pickWriteFile(name: String, bytes: ByteArray) }`
- `feature/settings/src/androidMain/.../FilePicker.android.kt` — uses `ActivityResultContracts.OpenDocument` + `createDocument`
- `feature/settings/src/iosMain/.../FilePicker.ios.kt` — uses `UIDocumentPickerViewController` via cinterop

Expose via factory in `feature:settings/FilePicker.kt`:
```kotlin
expect fun createFilePicker(/* platform context */): FilePicker
```

### AppScreen — Settings

`App.kt` has Settings as `PlaceholderScreen("Settings — coming in Phase 5")`. Phase 5 adds Security section; Phase 7 adds Import/Export section. Settings is a single screen with multiple sections.

### Koin wiring

Add to `composeApp/di/FeatureModules.kt`:
```kotlin
val dataBackupModule = module {
    single { BackupExporter(get(), get(), get(), get()) }  // categoryRepo, accountRepo, txnRepo, settings
    single { BackupImporter(get(), get(), get()) }
}
```

### Known version / import facts

- `kotlin.time.Instant` (NOT `kotlinx.datetime.Instant`)
- `kotlin.time.Clock` (NOT `kotlinx.datetime.Clock`)
- `@Serializable` from `kotlinx.serialization` — annotation processor plugin already in `data:backup/build.gradle.kts`
- `kotlin.test` explicit in `commonTest`
- `@BeforeTest Dispatchers.setMain` in ViewModel tests

# External Bank Sync (Enable Banking, BYO credentials)

Each user registers their own free Enable Banking account + application (restricted
production mode — own linked accounts, free for personal use) and pastes the
Application ID + RSA private key (PEM) into MoneyM. The app signs RS256 JWTs with
that key, drives the bank consent flow, and fetches transactions, surfaced as
suggestions the user accepts (creates a local transaction with an external id) or
rejects (never asked again, still viewable in a Rejected tab). Auto-sync runs on
startup after the Drive sync pull; users can opt out and sync manually.

Branch: `feature/external-sync`. One phase per commit; update the Status line of a
phase in this file in the same commit that completes it.

## Architecture

- `data/banksync` — Enable Banking REST client (Ktor, reuses catalog deps),
  RS256 JWT signer (cryptography-kotlin, PKCS#8 PEM), credentials in SecureStore,
  own Room DB `moneym_banksync.db` (`BankAccount`, `BankSuggestion`), `BankSyncEngine`.
- `feature/banksync` — settings sub-screen (credentials, connect bank, accounts,
  auto-sync toggle, sync now, disconnect) + suggestions review screen
  (Pending/Rejected tabs, duplicate hints, batch accept/reject).
- Transactions DB v3→v4: `TransactionEntry.external_id` (+ index), rides
  `TransactionSyncRow` so accepts propagate across devices via Drive sync.
  Rejections are device-local (known limitation).
- External id: EB entry_reference/transaction id, else
  `sha256(accountUid|bookingDate|amountMinor|currency|reference)` + collision suffix.
- Startup: `AppInitializer` calls `bankSyncEngine.autoSyncIfDue()` right after
  `syncEngine.pullNow()`; throttled (6h) and never blocks bootstrap. No ON_RESUME
  hook (PSD2 unattended-call limits).
- Consent redirect: paste-redirect-URL fallback first; `moneym://bank-callback`
  deep link (Android intent-filter + iOS CFBundleURLTypes → BankAuthCallbackBus)
  in Phase 6.

## Phases

### Phase 0 — Branch + plan file
Status: done
Branch `feature/external-sync`, this file.

### Phase 1 — Transactions DB v4: external_id
Status: done
`TransactionEntity.external_id` + index; `MIGRATION_3_4`; repository methods
`existsByExternalId` / `setExternalId` / `findByDateAndAmount` through DAO +
LocalDataSource + impl; `TransactionSyncRow.externalId` (default null) mapped in
export/upsertFromSync; `FakeTransactionRepository` parity; migration + repo +
sync round-trip tests.

### Phase 2 — data/banksync: EB client + JWT signer
Status: done
New KMP module. `EnableBankingCredentialsStore` (SecureStore; new SecurityKeys
`banksync.app_id|private_key_pem|session_id`); `internal/EbJwtSigner` (RS256,
PKCS#8 PEM, instructive error on PKCS#1 headers); `EnableBankingClient` interface
+ `internal/DefaultEnableBankingClient` (Ktor, ignoreUnknownKeys DTOs);
`ExternalIdResolver`; amount parsing (decimal + credit_debit_indicator → Money,
currency-exponent aware); Koin `BankSyncModule` beside remoteBackupCommonModule.
Tests incl. signer on iosSimulatorArm64Test (PEM/Apple-provider risk probe).

### Phase 3 — data/banksync persistence + BankSyncEngine
Status: pending
`BankSyncRoomDatabase` v1 (`BankAccount` with per-account cursor + local account
mapping; `BankSuggestion` with UNIQUE external_id and PENDING/ACCEPTED/REJECTED
status); `BankSyncRepository` + impl; `BankSyncEngine` (`runtime` StateFlow,
`syncNow`, `autoSyncIfDue`; 5-day overlap window, booked only); 4 prefs through
PrefKeys → AppSettingsRepository → Default + Fake; `FakeBankSyncRepository` in
core/testing. Tests: dedupe, overlap idempotency, cursor, pagination,
reject-never-reinserted.

### Phase 4 — feature/banksync settings + connect flow
Status: pending
`BankSyncSettingsKey`/entry/Screen/ViewModel/UiState/Intent modeled on
feature/sync. Not-configured state (app id + PEM paste, validate via
GET /application); configured state (session status, ASPSP picker, browser auth,
paste-redirect fallback, accounts with local-account mapping + enabled toggle,
auto-sync toggle, Sync now, Disconnect). Use cases `ConnectBankUseCase`,
`ParseRedirectCodeUseCase`. Nav + Koin + settings DATA-section row. i18n: all
keys in English + 27 locales. VM tests.

### Phase 5 — Suggestion review screen
Status: pending
`BankSuggestionsKey` + screen with Pending/Rejected tabs; MmCard per suggestion
(checkbox, description, counterparty, date, signed amount, category chip, target
account); inline "Possible duplicate" sub-card via `FindDuplicateUseCase`
(occurred_on + amount_minor + currency); `AcceptSuggestionUseCase` (upsert →
setExternalId → accept); batch bar; Restore-to-pending on Rejected tab;
pending-count badge row in bank sync settings. i18n all locales; tests.

### Phase 6 — Startup hook + deep link
Status: pending
`AppInitializer`: `runCatching { bankSyncEngine.autoSyncIfDue() }` after
`syncEngine.pullNow()`. Deep link `moneym://bank-callback`: BankAuthCallbackBus,
Android intent-filter + onNewIntent, iOS CFBundleURLTypes + handleDeepLink; VM
collects bus while awaiting auth. Tests.

### Phase 7 — README + polish
Status: pending
README "Bank sync (Enable Banking)" setup instructions (account → application →
redirect URL `moneym://bank-callback` → activate restricted production by linking
own accounts → download PEM, copy app id → paste into Settings → Bank sync;
180-day renewal + rate limits). i18n key-set sweep; full test run.

## Risks

1. PEM/RS256 on iOS via cryptography-kotlin — probed in Phase 2 before UI;
   fallback expect/actual signer (java.security / SecKeyCreateSignature).
2. EB/PSD2 rate limits — startup-only + 6h throttle; 429 → friendly retry-later.
3. Unstable EB transaction ids — fallback hash; booked-only; idempotent overlap.
4. Session expiry ≤180 days — valid_until stored, status surfaced, auto-sync
   degrades to "reconnect required".
5. Multi-device — accepts propagate via sync external id; rejections local.

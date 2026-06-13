# Phase 5 — Onboarding PSD2 mention + last-sync timestamp

## Tasks

### 1. Onboarding bank-sync feature description → mention PSD2/open banking
Key `onboarding_welcome_feature_bank_desc` in `feature/onboarding/src/commonMain/composeResources/values/strings.xml` and ALL 27 locale dirs (`values-<locale>`).
Current EN: "Import transactions from your EU bank as suggestions you review — using your own free key."
New EN: "Import transactions from your EU bank via PSD2 open banking as suggestions you review — using your own free key."
Update the same key in every `values-<locale>/strings.xml` to add the "PSD2 open banking" idea, translated naturally into that locale (keep the rest of each existing translation, just weave in PSD2 open banking). Locales: ar cs da de es et fi fr hi hr hu is it ja lt lv mk nb nl pl pt ru sk sl sv tr vi zh. de/es/it must read naturally.

### 2. Home screen: show last-sync date
Add string key `bank_sync_last_synced` with EN value "Last synced %1$s" to `feature/banksync/src/commonMain/composeResources/values/strings.xml` AND all 27 locale dirs (translate per locale; %1$s is the date placeholder, keep it).
Then in `feature/banksync/src/commonMain/kotlin/com/dv/moneym/feature/banksync/home/BankSyncHomeScreen.kt`, in `ControlsCard`, the block currently only renders `bank_sync_last_sync_never` when `state.lastSyncMs == 0L`. Add an `else` branch: when `lastSyncMs > 0` render a Text with `stringResource(Res.string.bank_sync_last_synced, formatDate(state.lastSyncMs))` using the existing private `formatDate(epochMs: Long)` helper in that file (already locale-aware via core/common). Same caption style as the "never" text. Add the per-key import for `bank_sync_last_synced`.

## Conventions
- Compose resource keys need per-key imports (`import moneym.feature.banksync.generated.resources.bank_sync_last_synced`). Missing import = "Unresolved reference".
- Every new key in base + ALL 27 locales, same change.

## Verify
```
./gradlew :feature:onboarding:compileDebugKotlinAndroid :feature:banksync:compileDebugKotlinAndroid
```
Both must pass. Confirm `bank_sync_last_synced` present in all 28 banksync strings.xml files and `onboarding_welcome_feature_bank_desc` updated in all 28 onboarding files.

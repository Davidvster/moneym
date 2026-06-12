# MoneyM

**A private, offline-first personal finance manager for Android & iOS.**

MoneyM tracks your spending, budgets, and accounts entirely on your device. There is no account to
create, no analytics, no server that ever sees your data. Optional features (encrypted cloud sync,
AI analysis) run on infrastructure *you* control — your own Google Drive, an on-device LLM — never
ours.

🌐 **Website:** https://davidvster.github.io/moneym.github.io/

Open source under the [MIT License](LICENSE). Issues and pull requests welcome.

---

## Features

- **Transactions** — quick add/edit with categories, payment modes (cash / card / transfer), and
  multiple account wallets.
- **Overview** — month and year views with a spending-by-category donut, daily bars, and cumulative
  / trend charts.
- **Categories** — fully custom categories with color and icon pickers.
- **Budgets** — set per-category budgets and track them against actual spend.
- **Security** — optional PIN lock; the PIN is hashed, never stored in plaintext.
- **On-device AI analysis** — chat about your own finances using a **local** LLM. On Android you
  download a small GGUF model that runs on-device; on iOS it uses Apple's built-in **Foundation
  Models**. Your transactions never leave the phone.
- **Encrypted cloud sync** — *optional* cross-device sync and backup through **your own** Google
  Drive (the hidden `appDataFolder`). Data is encrypted on-device with AES-256-GCM from a passphrase
  only you know. Off by default — no Firebase, no `google-services.json`.
- **Local backup / restore** — export and import a backup file independently of any cloud feature.
- **Bank sync (optional)** — import transactions from your EU bank as reviewable suggestions via
  Enable Banking's PSD2 API, using your own free personal credentials. See
  [Bank sync setup](#optional-bank-sync-enable-banking).
- **29 languages** — fully localized UI (English, German, Spanish, French, Japanese, Chinese, and
  many more).
- **Custom design system** — a bespoke neutral-token theme with Geist fonts and hand-built
  components (no stock Material chrome).

## Tech stack

Kotlin Multiplatform · Compose Multiplatform · Room · Koin · navigation3 ·
kotlinx-serialization · kotlinx-datetime · Ktor (Google Drive client) ·
[dev.whyoleg.cryptography](https://github.com/whyoleg/cryptography-kotlin) (AES / PBKDF2).

## Project structure

```
shared/        KMP library — Koin wiring, navigation, App(), iOS entry point
androidApp/    Android application (com.dv.moneym) — MainActivity, manifest, signing
iosApp/        Xcode project consuming the Kotlin/Native framework
core/          common, model, designsystem, ui, ui-graphs, datastore, security,
               navigation, platform, ai, oauth, utils, testing
data/          transactions, categories, accounts, budgets, settings, backup,
               remotebackup, sync, llmmodels
feature/       transactions, transactionEdit, overview, categories, budgets,
               settings, security, onboarding, sync, aianalysis, aimodels,
               about, infopage
```

Architecture conventions (ViewModel/Intent/UseCase, DI, testing) are documented in
[CLAUDE.md](CLAUDE.md).

## Build & run

```bash
# Android
./gradlew :androidApp:assembleDebug

# iOS — link the framework, then build via Xcode or xcodebuild
./gradlew :shared:linkDebugFrameworkIosArm64 \
          :shared:linkDebugFrameworkIosSimulatorArm64
# open iosApp/iosApp.xcodeproj (scheme: iosApp), or build for the simulator:
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp \
  -configuration Debug -destination "generic/platform=iOS Simulator" \
  build CODE_SIGNING_ALLOWED=NO

# Tests
./gradlew testDebugUnitTest        # Android unit tests
./gradlew iosSimulatorArm64Test    # Kotlin/Native iOS sim tests
```

> Gradle tasks like `compileKotlinAndroid` are ambiguous — use the variant-qualified form
> (`compileDebugKotlinAndroid` / `compileReleaseKotlinAndroid`).

## Screenshot tests (Paparazzi)

Every `@Preview` composable in `core/` and `feature/` modules is rendered and compared against a
committed golden PNG using [Paparazzi](https://github.com/cashapp/paparazzi). Tests live in each
module's `src/androidUnitTest/` source set and run on the JVM — no emulator or device needed.

Requires **JDK 21+** (Paparazzi 2.x requirement).

```bash
# Verify all modules against their committed goldens
./gradlew verifyPaparazziDebug --no-configuration-cache

# Verify a single module
./gradlew :core:ui:verifyPaparazziDebug --no-configuration-cache

# Re-record goldens after a UI change (review the diff before committing!)
./gradlew :feature:overview:recordPaparazziDebug --no-configuration-cache
```

`--no-configuration-cache` is required — the Paparazzi plugin's task graph isn't compatible with
Gradle's configuration cache (`org.gradle.configuration-cache=true` in `gradle.properties`).

A new `@Preview` is picked up automatically (via
[ComposablePreviewScanner](https://github.com/sergio-sastre/ComposablePreviewScanner)) — just
record and commit its golden PNG alongside the preview.

On failure, rendered-vs-golden diff images are written to `<module>/build/paparazzi/failures/`.
Goldens are recorded on macOS; re-record locally if you're on a different OS and see widespread
diffs from font-rendering differences.

## Optional: Google Drive cloud sync

Cloud sync is **completely optional**. The app builds, runs, and ships unchanged with no setup — if
no OAuth client ID is configured, the cloud section simply stays hidden in Settings and everything
else works normally. There is no Firebase dependency and no `google-services.json`.

To enable it for a local build you provision OAuth client IDs in the Google Cloud Console (Drive API
+ the `drive.appdata` scope) and wire them in. The Android quick-start: put your **Web** OAuth client
ID in `local.properties` (git-ignored):

```properties
googleOAuthServerClientId=YOUR_WEB_CLIENT_ID.apps.googleusercontent.com
```

Full step-by-step guides:

- [docs/google-drive-backup-setup.md](docs/google-drive-backup-setup.md) — creating the OAuth
  clients (Android / Web / iOS) and adding the iOS GoogleSignIn SDK.
- [docs/SYNC.md](docs/SYNC.md) — the cross-device sync model (snapshot, reconcile, passphrase).
- [docs/REMOTE_BACKUP.md](docs/REMOTE_BACKUP.md) — encrypted backup internals.

## Optional: Bank sync (Enable Banking)

MoneyM can import transactions from your EU bank accounts as **suggestions** you accept or reject.
It uses [Enable Banking](https://enablebanking.com)'s PSD2 open-banking API in a bring-your-own-key
model: **you** register a free personal Enable Banking application and paste its credentials into
the app. MoneyM ships with no API keys, talks to the API directly from your device, and stores your
key in the platform keystore (Android Keystore / iOS Keychain). Free for personal use on your own
accounts; no PSD2 license needed because data access runs under Enable Banking's license.

How to obtain the key:

1. **Create an account** at [enablebanking.com](https://enablebanking.com) (free) and open the
   [Control Panel](https://enablebanking.com/cp).
2. **Register an application** in the **production** environment (a sandbox application cannot
   reach real banks): choose a name, and set the redirect URL to exactly
   `https://davidvster.github.io/moneym.github.io/bank-callback.html` (Enable Banking only accepts
   `https` redirect URLs; this page forwards your bank's consent result back into the app via the
   `moneym://bank-callback` deep link, and offers a copy button if the deep link doesn't fire).
3. **Download the private key** (a PEM file) generated for the application. It is shown **once** —
   store it safely. The key must be PKCS#8 (`-----BEGIN PRIVATE KEY-----`); if you have a
   `BEGIN RSA PRIVATE KEY` file, convert it:
   `openssl pkcs8 -topk8 -nocrypt -in key.pem -out key-pkcs8.pem`.
4. **Copy the Application ID** (a UUID shown on the application page).
5. **Activate the application by linking your own bank account** in the Control Panel
   ("Activate by linking accounts"): choose the country, your bank and the account type
   (personal or business), then sign in at your bank to confirm the link. Only the accounts you
   link are accessible — that is what keeps personal use free, no contract needed. The
   application switches to active restricted production mode.
6. In MoneyM open **Settings → Bank sync**, paste the Application ID and the PEM contents, and tap
   **Save & validate**. Then **Connect a bank**: pick the same country and bank, authorize in the
   browser, and you're back in the app — accept or reject the imported suggestions. (The full
   guide is also available in the app behind the **ⓘ** button on the Bank sync screen.)

Notes:

- Bank consent is valid for up to **180 days** (a PSD2 rule); after that the settings screen shows
  a reconnect prompt and you re-authorize the bank.
- Automatic sync runs at most once every 6 hours on app start (banks rate-limit unattended PSD2
  access to ~4 calls/day); **Sync now** in settings is always available.
- Fetched transactions never enter your books silently — each one appears as a suggestion with a
  possible-duplicate hint, and rejected suggestions are never asked about again.

## Contributing

MoneyM is open source. Bug reports, feature ideas, and pull requests are welcome — please read
[CLAUDE.md](CLAUDE.md) first for the module layout and architecture conventions the codebase follows.

## License

[MIT](LICENSE) © 2026 Davidvster

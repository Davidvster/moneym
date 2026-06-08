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

## Contributing

MoneyM is open source. Bug reports, feature ideas, and pull requests are welcome — please read
[CLAUDE.md](CLAUDE.md) first for the module layout and architecture conventions the codebase follows.

## License

[MIT](LICENSE) © 2026 Davidvster

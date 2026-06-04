# Phase 3 — `data/llmmodels`: catalog + downloader + repository

**Status:** ✅ Done — module builds android+ios, 9/9 tests pass. `ModelFileStore` seam keeps repo/downloader testable without DbPlatform. sha256 empty for all 3 catalog entries → verification skipped (hashes unconfirmed offline; downloader verifies once real hashes filled). Catalog `.litertlm` filenames are best-guess — confirm against HF before shipping.
**Depends on:** Phase 2 (appFilesDirectory)

## Goal
New data module that knows the model catalog, downloads `.litertlm` files with progress + sha256
verification, stores them, and tracks the active model. Optional HF token in `SecureStore`.

## Module setup
- Register `:data:llmmodels` in `settings.gradle.kts`.
- `data/llmmodels/build.gradle.kts` modeled on `data/accounts/build.gradle.kts`. Deps:
  `core/model`, `core/common`, `core/platform`, `core/datastore`, `core/security`,
  kotlinx-coroutines, kotlinx-serialization, Ktor (`ktor-client-core`, `ktor-client-content-negotiation`),
  and platform engines: androidMain `ktor-client-okhttp`, iosMain `ktor-client-darwin`.
  commonTest: `ktor-client-mock`, kotlin-test, coroutines-test, turbine, `core/testing`.

## Types (`commonMain`)
- `LlmModel`: `id: String, displayNameKey: String, fileName: String, url: String, sizeBytes: Long, sha256: String, format: String, requiresToken: Boolean`.
- `LlmModelCatalog`: static `val models: List<LlmModel>` — v1 (LiteRT-LM `.litertlm`, HF `litert-community`):
  - `gemma3-1b-it` — `https://huggingface.co/litert-community/Gemma3-1B-IT/resolve/main/<file>.litertlm`
  - `gemma4-e2b-it` — `https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm`
  - `gemma3n-e2b-it` — `https://huggingface.co/litert-community/<gemma-3n-E2B>/resolve/main/<file>.litertlm`
  - `sizeBytes`/`sha256`: pin from HF (`GET` `…/resolve/main/<file>?...` headers or the repo's file
    listing). If a value can't be confirmed at build time, set `sha256 = ""` and **skip** verification
    when empty (document this); never guess a wrong hash.
- `DownloadProgress`: `data class(bytesRead: Long, totalBytes: Long)` with `fraction: Float`.
- `LlmModelState`: `model: LlmModel, downloaded: Boolean, active: Boolean, progress: Float?`.

## Storage / state
- Path: `${dbPlatform.appFilesDirectory}/models/<fileName>`; download to `<fileName>.part` then rename.
- `downloaded` derived from final file existence (+ size match when known).
- Active model id persisted: add `PrefKeys.AI_ACTIVE_MODEL_ID` (Phase 6 also references it; add the
  key here in `core/datastore`).
- HF token: `SecureStore` (key e.g. `"hf_token"`).

## `LlmModelDownloader`
- Ktor streaming `GET` via a platform client: `expect fun llmHttpClient(): HttpClient`
  (androidMain → `HttpClient(OkHttp)`, iosMain → `HttpClient(Darwin)`).
- Stream `response.bodyAsChannel()` to the `.part` file, emit `Flow<DownloadProgress>` from
  bytes/contentLength. Optional `Range` resume if `.part` exists.
- Add `Authorization: Bearer <token>` header when `model.requiresToken` and a token is present.
- After completion: if `sha256` non-empty, verify (streaming SHA-256 over the file) and fail on
  mismatch (delete `.part`); else accept. Rename `.part` → final.
- Cancellation via coroutine cancellation; delete `.part` on cancel.

## `LlmModelRepository` (interface + impl `DefaultLlmModelRepository`)
- `fun observeModels(): Flow<List<LlmModelState>>` — combine catalog + disk + active id + in-memory
  progress `MutableStateFlow`.
- `suspend fun download(id: String)` / `fun cancel(id: String)` / `suspend fun delete(id: String)`
  / `suspend fun setActive(id: String)` / `suspend fun activeModelPath(): String?`.
- `suspend fun setHfToken(token: String)` / `fun observeHasToken(): Flow<Boolean>` (or get).
- `core/testing/FakeLlmModelRepository`: in-memory, deterministic — required by repo↔fake parity rule.

## Tests (`commonTest`)
- `LlmModelDownloaderTest` (ktor-client-mock): emits increasing progress; verifies sha256 success +
  mismatch-fails; adds auth header when token set + requiresToken.
- `DefaultLlmModelRepositoryTest`: setActive persists; activeModelPath returns stored path; delete
  flips downloaded false. Use a fake file layer / temp dir abstraction (inject a small file-IO
  seam so it's testable without `DbPlatform`).

## Skills
`data-layer` (repository/datasource boundaries, Flow rules, mapping), `testing` (fake-first, Turbine).

## Verify
`./gradlew :data:llmmodels:compileDebugKotlinAndroid :data:llmmodels:compileKotlinIosSimulatorArm64 :data:llmmodels:testDebugUnitTest`

## Commit
`feat(data-llmmodels): model catalog, ktor downloader (progress+sha256), repository + fake`

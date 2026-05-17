# Architecture overview

MoneyM is a Kotlin Compose Multiplatform personal-finance app, **local-first**, targeting **Android and iOS**. This document is the architectural source of truth and is expected to evolve. If code disagrees with this document, one of them is wrong — discuss in PR before merging.

## Principles

1. **Local-first.** All data lives in a local SQLDelight database. Sync is a future capability layered on top, not a foundation.
2. **Feature-modular.** The unit of organization is the feature, not the layer. Each feature is a self-contained vertical (UI → ViewModel → UseCase → Repository wiring).
3. **Layer-disciplined inside modules.** Each feature internally respects the layer order: UI → ViewModel → UseCase → Repository → DataSource. The layers are package-level; the module boundary is the feature.
4. **Reactive everywhere.** Repositories emit `Flow<T>`. ViewModels `combine` and `stateIn` into UI state. UI re-renders on state change. There is no manual refresh.
5. **No cross-feature imports.** If two features share code, the shared code becomes a `core:*` module. If they share data, they both depend on the same `data:*` module.
6. **Small, sharp dependencies.** A module declares exactly the dependencies it needs — no transitive convenience.
7. **Multiplatform first.** Anything in `commonMain` is the default. Platform code lives behind `expect/actual` and is justified.

## Logical layers

```
┌────────────────────────────────────────────────┐
│              feature/<name>/                   │  Composables (stateful wrapper + stateless content)
├────────────────────────────────────────────────┤
│         feature/<name>/                        │  ViewModel + UiState + Intent + Effect
├────────────────────────────────────────────────┤
│            feature/<name>/domain               │  UseCases (feature-local; reusable ones promoted to core)
├────────────────────────────────────────────────┤
│                 data/<name>                    │  Repository (public) + DataSource (internal) + SQL
├────────────────────────────────────────────────┤
│                core/database                   │  SQLDelight driver factory (expect/actual)
└────────────────────────────────────────────────┘
```

## Module structure

```
MoneyM/
├── composeApp/                     App entry point, DI bootstrap, root navigation
├── core/
│   ├── model/                      Domain types: Transaction, Category, Account, Money, ...
│   ├── common/                     DispatcherProvider, Clock, formatting, Result helpers
│   ├── designsystem/               Theme tokens, colors, typography, spacing, icons
│   ├── ui/                         Reusable Composables (Buttons, EmptyState, Loader, ...)
│   ├── database/                   SQLDelight driver factory, schema setup
│   ├── datastore/                  multiplatform-settings wrapper
│   ├── security/                   PIN hashing, biometric prompts, secure storage
│   ├── navigation/                 Route definitions, navigation helpers
│   └── testing/                    Test utilities, fakes, fixtures
├── data/
│   ├── transactions/               Transaction persistence
│   ├── categories/                 Category persistence + seed defaults
│   ├── accounts/                   Account persistence + seed defaults
│   ├── settings/                   App settings persistence
│   └── backup/                     JSON/CSV import/export
├── feature/
│   ├── transactions/               Daily & monthly list + filtering
│   ├── transactionEdit/            Add / edit / delete transaction
│   ├── overview/                   Analytics dashboard, charts
│   ├── categories/                 Manage categories
│   ├── settings/                   Settings screens (incl. import/export entry)
│   ├── security/                   PIN setup, PIN unlock, biometric prompt
│   └── onboarding/                 First-launch flow
└── iosApp/                         iOS Xcode project (entry only)
```

### Module naming

I chose `core:model` for the shared domain types module (singular, terse, conventional — matches the Now in Android template and is what most Kotlin engineers reach for first). Other candidates considered: `core:domain` (rejected — implies use cases live there too, but use cases are feature-local), `core:entity` (rejected — DB-flavored connotation), `shared` (rejected — too vague at the top level).

### Allowed dependencies

Strict rules, enforced by review (and by Gradle module configuration — no transitive `api` exports):

| From | May depend on |
|---|---|
| `composeApp` | Any `feature:*`, bootstrap `core:*` (`navigation`, `designsystem`, `common`, `database`, `datastore`, `security`) |
| `feature:*` | Any `core:*`, any `data:*` — **never another feature** |
| `data:*` | `core:model`, `core:common`, `core:database`, `core:datastore`, `core:security` |
| `core:ui` | `core:designsystem`, `core:model`, `core:common` |
| `core:designsystem` | `core:model` (for `Category` color lookup), `core:common` |
| `core:navigation` | `core:model` |
| `core:database` | `core:common` |
| `core:datastore` | `core:common` |
| `core:security` | `core:common`, `core:datastore` |
| `core:common` | (nothing) |
| `core:model` | (nothing) |
| `core:testing` | `core:common`, `core:model`, plus test-time deps |

If a violation is needed, it's a signal that the architecture is wrong — open a discussion before working around it.

## Threading and dispatchers

- `core:common` defines `DispatcherProvider { main, default, io }`. There is no `Dispatchers.IO` on native — we provide a multiplatform `io` (`Dispatchers.Default.limitedParallelism(64)` on iOS, `Dispatchers.IO` on Android).
- ViewModels never reference dispatchers directly — they `launch` in `viewModelScope` and let UseCases/Repositories switch context.
- All DB and file IO happens on `dispatchers.io`. UI never touches IO threads.

## State flow

```
SQLDelight ──Flow──▶ DataSource ──Flow──▶ Repository ──Flow──▶ UseCase ──Flow──▶ ViewModel.state ──▶ UI
                                                                                       ▲
                                                                                       │ intents (write paths)
                                                                                       │
                                                                                       └ User
```

Writes go in the reverse direction: `UI → ViewModel.onIntent → UseCase → Repository → DataSource → SQLDelight`. SQLDelight then re-emits on the read Flows automatically.

## Why this shape

- **Feature modules instead of "ui/data/domain" mega-modules**: faster builds (only the touched feature recompiles), clearer ownership, prevents the slow drift toward a monolith.
- **Data modules per concern, not per feature**: a category lives on its own, used by `transactions` and `overview` features. One feature owning a data module would force the next feature to depend on the wrong thing.
- **No `:domain` module**: use cases are usually feature-specific. We start with them inside features; we promote a use case to `core:*` only when a second feature needs it. ([YAGNI](https://en.wikipedia.org/wiki/You_aren%27t_gonna_need_it) wins over upfront abstraction.)
- **`core:navigation` exists even though Jetpack Nav is centralized**: shared `NavRoute` types and `NavGraphBuilder` helpers live there so features don't import each other to navigate.

## Open questions

These are decisions we deliberately deferred — they live in `docs/architecture/decisions.md`. The big ones:

- Whether to promote `domain` to its own module after we've built 3–4 features.
- Whether sync gets its own `data:sync` module or extends each `data:*` repository.
- iOS distribution mechanism (Cocoapods vs SPM vs binary framework).

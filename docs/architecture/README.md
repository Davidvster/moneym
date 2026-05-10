# Architecture reference

Living documents that describe how MoneyM is built. If something in this directory disagrees with the code, treat it as a bug in one or the other — surface it in PR.

| Doc | What it covers |
|---|---|
| [overview.md](overview.md) | Principles, module structure, dependency rules, threading model |
| [data-model.md](data-model.md) | SQLDelight schema, entities, indexes, migration policy |
| [security.md](security.md) | PIN flow, biometrics, secure storage, lock lifecycle, threat model |
| [theming.md](theming.md) | Design system: palette (light + dark), typography, spacing, category colors |
| [import-export.md](import-export.md) | JSON / CSV formats, import semantics, forward-compat for cloud sync |
| [libraries.md](libraries.md) | Every library used, with the rationale and the alternatives rejected |
| [roadmap.md](roadmap.md) | Phased implementation plan, each phase ending in a runnable app |
| [decisions.md](decisions.md) | ADR log — decided and open |

## Editing rules

- One PR per doc when the change is non-trivial. Small clarifications can ride along with code.
- ADRs are append-only. Reversals get a new entry that references the old one.
- If a doc is wrong, fix it in the same PR as the code change that made it wrong.

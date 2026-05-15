# Implementation plan

This folder is the **execution plan** for building MoneyM. Each phase is its own file with numbered steps so any single step can be picked up and resumed independently.

If a session runs out of context mid-phase, the next session reads:
1. The current phase file (e.g. `phase-1-skeleton.md`)
2. The status block at the top of that file
3. Continues from the first unchecked step

Architecture, conventions, and rationale live in [`/docs/architecture/`](../docs/architecture/) — this folder describes **what to do**, not why.

## Phase status

| Phase | File | Status |
|---|---|---|
| Phase 0 — Approval gate | [phase-0-approval.md](phase-0-approval.md) | ✅ Done |
| Phase 1 — Skeleton modules + DI bootstrap | [phase-1-skeleton.md](phase-1-skeleton.md) | ✅ Done |
| Phase 2 — Data layer | [phase-2-data.md](phase-2-data.md) | ✅ Done |
| Phase 3 — Transactions feature | [phase-3-transactions.md](phase-3-transactions.md) | ✅ Done |
| Phase 4 — Security (PIN + biometrics) | [phase-4-security.md](phase-4-security.md) | ✅ Done |
| Phase 5 — Categories + onboarding | [phase-5-categories-onboarding.md](phase-5-categories-onboarding.md) | ✅ Done |
| Phase 6 — Overview / analytics | [phase-6-overview.md](phase-6-overview.md) | ✅ Done |
| Phase 7 — Import / export | [phase-7-import-export.md](phase-7-import-export.md) | ✅ Done |
| Phase 8 — Polish + release prep | [phase-8-polish.md](phase-8-polish.md) | ✅ Done (code deliverables) |

Legend: ✅ done · 🔄 in progress · ⏳ ready (waiting on user) · 📝 sketched (will expand when started)

## How to resume

When picking up after an interruption:

1. Open the phase file marked 🔄 (or ⏳ if none is in progress).
2. Read the **Status** block at top — it lists which numbered steps are done.
3. Find the first unchecked step. Read its **Goal**, **Files**, and **Acceptance** sections.
4. Execute it. Mark it ✅ in the Status block when complete.
5. If the step turns out to be wrong, **don't silently change it** — add a note under the step and surface in chat.

## Conventions for this folder

- Step numbers are stable. Adding a step inserts a sub-step (`3a`, `3b`) rather than renumbering.
- Each phase file has the same shape: Status block, then numbered steps with Goal / Files / Acceptance.
- Each step is small enough that one execution turn can finish it. If a step balloons, split it.
- Architecture decisions go in `/docs/architecture/decisions.md`, not here. This folder is "do", not "decide".

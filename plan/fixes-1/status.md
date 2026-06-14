# fixes-1 — status

Master plan: `~/.claude/plans/1-missing-transaltions-tingly-aurora.md`

| Phase | Title | Status | Commit |
|-------|-------|--------|--------|
| 1 | i18n quick wins (currency subtitle + indicator labels) | ✅ done | `3ad3a166` |
| 2 | Payment types + budgets delete/add UX | ✅ done | `34c2338a` |
| 3 | avg/mo & avg/day deep investigation | ⚠️ done (no-op) | `<docs>` |
| 4 | Notification (wallet) sync app picker | ✅ done | `a4b0638f` |
| 5 | Sync button visibility + sync-picker row clickability | ✅ done | `059c26b7` |
| 6 | Theme picker → bottom sheet | ✅ done | `17d62517` |
| 7 | Export/Import screen redesign | ✅ done | `2489b242` |

Legend: ⬜ todo · ⏳ in progress · ✅ done · ⚠️ done-with-notes

## Final cross-platform build — ✅ all green
- `:androidApp:assembleDebug` — BUILD SUCCESSFUL
- `:shared:linkDebugFrameworkIosArm64` + `IosSimulatorArm64` — BUILD SUCCESSFUL
- iOS app `xcodebuild` (sim, no signing) — ** BUILD SUCCEEDED **

All 7 phases complete on branch `fixes-1-ux-i18n`.

## Notes
- Locale dirs in feature/settings: 28 locales + base (`values/`).
- Branch name is `fixes-1-ux-i18n` (the `fixes-1` branch already existed with
  unrelated work).

### Phase 3 finding (no-op)
avg/mo & avg/day in yearly overview graphs are **already fully localized**:
- Code: `OverviewPeriodBody.kt` legend header (`inYearMode`, lines ~515/523) and
  per-category trend card (line ~765) all use
  `stringResource(Res.string.overview_cat_avg_month / overview_cat_avg_day)`.
  `AvgStatsCard.kt` uses `overview_avg_*`. No `.name`/concat/hardcoded literal.
- Resources: all 28 locales have translated values for `overview_cat_avg_*` and
  `overview_avg_*` (e.g. de `ø/Tag`, es `prom/día`, ja `日平均`) — verified by
  dumping every locale; zero English leftovers.
- Conclusion: no code change. User's report likely from an older build. No commit
  beyond this status/plan docs commit.

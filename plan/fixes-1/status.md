# fixes-1 Bank Sync polish — status

Branch: `fixes-1-banksync-polish`

| Phase | Scope | Status |
|-------|-------|--------|
| 1 | Reusable themed `MmSnackbar` + bank reject undo | ✅ done |
| 2 | Category picker: title + OK button per mode | ✅ done |
| 3 | Bank Sync home layout (bottom connect btn, stacked sync/disconnect) | ✅ done |
| 4 | Suggestions: single-accept confirm + filter sheet fixes | ✅ done |
| 5 | Transactions combined sync (bank + cross-device) | ✅ done |
| 6 | X-close on all 15 bottom sheets | ✅ done |

## Final verification
- ✅ Android `:androidApp:assembleDebug` — BUILD SUCCESSFUL
- ✅ iOS framework link (arm64 + simulatorArm64) — BUILD SUCCESSFUL
- ✅ touched-module unit tests (`:feature:transactions`, `:feature:banksync`) — passing
- ✅ i18n: new keys present in all locales (per-phase grep coverage clean)

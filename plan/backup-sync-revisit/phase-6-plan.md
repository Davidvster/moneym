# Phase 6 — Verification, edge cases, docs

## Automated
Full unit sweep green:
```
./gradlew :data:sync:testDebugUnitTest :data:remotebackup:testDebugUnitTest \
          :feature:settings:testDebugUnitTest :feature:sync:testDebugUnitTest \
          :feature:transactions:testDebugUnitTest
./gradlew :composeApp:assembleDebug :composeApp:linkDebugFrameworkIosSimulatorArm64
```

## Prune safety (verified)
`RemoteBackupManager.pruneOldBackups()` filters `name.startsWith("moneym-backup")`
(`BACKUP_NAME_PREFIX`). Sync files are `moneym-sync-state.json` / `moneym-devices.json`, so
retention pruning never deletes them.

## Docs
- `docs/SYNC.md` rewritten for the unified model: one toggle, same-password join, persistent
  passphrase, password/encryption conflict resolution, lifecycle decoupling, manual Sync now.
- `docs/REMOTE_BACKUP.md` notes the snapshot history now rides the Cloud sync flag.

## Manual two-device matrix (run on Android + iOS sim before release)
1. **Restart survival** — device A: enable Cloud sync, set password, add a txn, kill+reopen →
   no password re-prompt; sync runs.
2. **Add propagation** — A adds txn → B (foreground / Sync now) shows it.
3. **Join same-password** — fresh B enables → prompted for A's password → enters it → A's data
   appears. Wrong password → inline error, sync stays off.
4. **Delete confirm** — A deletes a txn → B shows "items removed — review" → confirm removes on B,
   decline keeps it (and revives on A next push).
5. **Password change = conflict** — A changes password (override) → B surfaces "Sync paused —
   action needed" → entering the new password resolves; "use plaintext" / override also work.
6. **Plaintext symmetry** — A on plaintext, B encrypted → B sees the plaintext-downgrade conflict.
7. **Status UI** — banner shows Syncing → Synced; tapping opens the sheet; Sync now works.

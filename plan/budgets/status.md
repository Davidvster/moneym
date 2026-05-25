# Budgets Feature — Status

| Phase | Title | Status | Commit |
|-------|-------|--------|--------|
| 1 | Data layer (`:data:budgets` + `core/model` + fakes) | ✅ done | `ef82022` |
| 2 | Settings entry + `:feature:budgets` list & create | ✅ done | `04dfc76` |
| 3 | Overview budget breakdown card | ✅ done | `dda7c47` |
| 4 | Tx-edit budget remaining chip | ✅ done | pending commit |

All four phases shipped on `worktree-budgets-feature` branch. Verify:
- `./gradlew :composeApp:assembleDebug`
- `./gradlew :data:budgets:testDebugUnitTest :core:model:testDebugUnitTest`
- `./gradlew :feature:budgets:testDebugUnitTest`
- `./gradlew :feature:overview:testDebugUnitTest`
- `./gradlew :feature:transactionEdit:testDebugUnitTest`

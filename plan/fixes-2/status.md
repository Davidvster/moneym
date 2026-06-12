# fixes-2 status

| Phase | Scope | Status |
|-------|-------|--------|
| 1 | Budgets bottom padding + AI history button to bottom | done |
| 2 | Android status bar icons follow app theme | done |
| 3 | Overview filter-aware avg/categories/charts | done |
| 4 | Dev/prod app separation (.dev suffix) | done — Android debug = com.dv.moneym.dev "MoneyM Dev"; iOS Debug = com.dv.moneym.MoneyM.dev "MoneyM Dev". USER ACTION: register dev package/bundle ids in Google Cloud OAuth |
| 5 | iOS Google Drive restore hang | done — HttpTimeout added (Darwin resource timeout was 7d), metadata peek no longer downloads full backup; verified on iPhone 16e sim. Android per-account misses: appDataFolder is scoped per OAuth client+account | 
| 6 | iPad amount keyboard | done — not reproducible on iPad Pro 11 sim; Decimal renders full-width as natively expected. Reported tiny pad = iPadOS floating-keyboard state (pinch-out to re-dock). No code change |
| Final | Android + iOS builds, tests | in progress |

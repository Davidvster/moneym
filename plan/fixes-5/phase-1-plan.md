# Phase 1 Plan: Notification Parser Accuracy

## Goal
Tighten wallet notification parsing so promotional, market, challenge, and reward notifications are ignored, while real payment notifications continue producing useful wallet suggestions.

## Implementation
- Add non-transaction rejection in `NotificationParser` before amount extraction creates a suggestion.
- Add hint lists in `NotificationParserHints` for promotions, markets/investments, challenges/rewards, deadlines/T&Cs, and worth/benefits/points language.
- Prefer ignoring suspicious notifications over creating low-confidence suggestions.
- Extract bunq direct debit merchants from `€7.61 to FBTO from Main. Paid automatically with Auto Accept.` as `FBTO`.

## Tests
- Add parser tests for challenge, stock/market, and Revolut promotion notifications returning `null`.
- Add a bunq direct debit parser test expecting amount `761`, currency `EUR`, direction `DEBIT`, description/counterparty `FBTO`.
- Keep existing valid wallet and NuBank parsing tests passing.

## Verification
- `./gradlew :data:walletsync:testDebugUnitTest --console=plain`
- `git diff --check`

## Commit
- `fixes-5 phase 1: tighten notification parsing`

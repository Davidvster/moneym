# Phase 2: Small UI Fixes

## Goal

Fix small user-facing polish issues in transaction edit, info pages, and settings.

## Changes

- Transaction edit note preselection:
  - Ensure selecting a note suggestion moves the cursor to the end of the note input.
- Info page links:
  - Extend `HtmlText` to support clickable `<a href="...">...</a>` links with theme accent styling.
  - Open links through `LocalUriHandler`.
  - Add anchors to the bank sync instructions in every locale.
- Settings version footer:
  - Add a shared app-info abstraction.
  - Provide Android and iOS implementations from platform metadata.
  - Render app name and version dynamically instead of hardcoded `MoneyM v1.0.0`.

## Verification

- `./gradlew :feature:transactionEdit:testDebugUnitTest :feature:infopage:compileDebugKotlinAndroid :feature:settings:testDebugUnitTest`

## Commit

- `Fix small UI polish issues`

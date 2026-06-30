# Phase 7 Plan: Overview Filter + Header Polish

## Goal

Fix user items 4 and 5:

- The overview category filter bottom sheet should look like the transaction screen category filter, including separate Expense and Income sections.
- The overview top-right area is too cluttered. Move "Analyze with AI" out of the top header action cluster, preferably near the date/period controls, while keeping the customize/edit icon discoverable in the overview header.

Commit after verification.

## Expected Files/Modules

- `feature/overview/src/commonMain/kotlin/com/dv/moneym/feature/overview/OverviewScreen.kt`
- `feature/overview/src/commonMain/kotlin/com/dv/moneym/feature/overview/components/OverviewHeader.kt`
- `feature/overview/src/commonMain/composeResources/values*/strings.xml` only if new strings are unavoidable.
- `feature/overview/src/commonTest/kotlin/com/dv/moneym/feature/overview/OverviewViewModelTest.kt` only if state behavior changes.
- `feature/overview/src/androidUnitTest/kotlin/com/dv/moneym/feature/overview/PreviewScreenshotTest.kt` or previews only if existing snapshot expectations need refresh.
- `plan/overview-ai/status.md`

## Implementation Notes

### Category Filter Sheet

- Use the existing shared `MmCategoryPickerSheet` grouped mode, matching the transaction screen call:
  - `groupedByType = true`
  - `groupLabelForType = { TransactionType.EXPENSE -> ..., TransactionType.INCOME -> ... }`
- Reuse existing overview string resources if suitable:
  - `overview_expenses`
  - `overview_income`
- Do not introduce new user-visible strings unless necessary. If new strings are introduced, add them to base + all 27 locale files.

### Header / Analyze Placement

- Remove the AI button from the top title/action row.
- Keep these in the top row:
  - title
  - wallet selector when needed
  - category filter icon when categories exist
  - customize/edit icon
- Add the AI action lower in the header, near the period/date controls. Suggested layout:
  - Keep the Month/Year/Custom segmented control on its own full-width row.
  - Keep the All/Expenses/Income segmented control full-width.
  - In the period navigation row, keep previous/date/next controls and place a compact accent `Analyze with AI` button to the right when available. If width is tight, use icon+short text or place it on a small row below the period controls.
- Ensure mobile widths do not overflow; use weights/`widthIn`/wrapping where needed.
- Preserve the existing `overview_analyze_cd` resource for the button label/content description if it is acceptable.
- Keep `onAnalyzeClick` callback behavior unchanged.

## Tests / Verification

- Update previews for `OverviewHeader` to show AI available and selected categories if useful.
- Run:

```bash
./gradlew --no-configuration-cache :feature:overview:testDebugUnitTest
```

- Run:

```bash
git diff --check
```

- If no unit behavior changes, record structural verification in `status.md`.

## Status Update Required

After implementation and verification, update `plan/overview-ai/status.md` with:

- Phase 7 status and verification command results.
- The commit hash after committing.

Commit only Phase 7 changes.

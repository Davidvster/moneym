# Phase 5: Month Switching with HorizontalPager Animation

## Problem
In the Transactions screen, clicking the left/right chevron changes the month but there is no swipe gesture and no horizontal pager animation. In the Overview screen, month switching uses `AnimatedContent` with a slide, but no swipe gesture. The user wants:
- Swipe left/right to navigate months (in addition to the chevron buttons)
- When navigating, the content should slide in/out horizontally like a `HorizontalPager`

## Files to modify
- `feature/transactions/src/commonMain/kotlin/com/dv/moneym/feature/transactions/ui/TransactionListScreen.kt` — wrap the transaction list body in a `HorizontalPager`-style animation; add swipe detection
- `feature/overview/src/commonMain/kotlin/com/dv/moneym/feature/overview/ui/OverviewScreen.kt` — the period-animated section already has `AnimatedContent` for period changes; add swipe on the header month/period row too; improve animation to match pager feel
- `feature/transactions/src/commonMain/kotlin/com/dv/moneym/feature/transactions/presentation/TransactionListIntent.kt` — already has `PreviousMonth` / `NextMonth` (no change needed)
- `feature/overview/src/commonMain/kotlin/com/dv/moneym/feature/overview/presentation/OverviewIntent.kt` — already has `PreviousPeriod` / `NextPeriod`

## Implementation steps

### Transactions Screen

1. Wrap the list body (the `when { ... }` block) in an `AnimatedContent` keyed on `state.currentMonth`:
   ```kotlin
   AnimatedContent(
       targetState = state.currentMonth,
       transitionSpec = {
           val dir = if (targetState > initialState) 1 else -1
           slideInHorizontally(tween(280)) { it * dir } togetherWith
               slideOutHorizontally(tween(280)) { -it * dir }
       },
       label = "month_pager",
       modifier = Modifier.weight(1f),
   ) { month ->
       // existing list content keyed on month
   }
   ```
   Note: `YearMonth` needs a `compareTo` — check if it implements `Comparable`. If not, compare year*12+month.

2. Add a horizontal swipe gesture to the list body using `Modifier.pointerInput`:
   ```kotlin
   .pointerInput(Unit) {
       detectHorizontalDragGestures(
           onHorizontalDrag = { _, dragAmount -> ... accumulate },
           onDragEnd = { if (accumulated > threshold) onIntent(PreviousMonth) else if < -threshold onIntent(NextMonth) }
       )
   }
   ```
   Or use `detectDragGestures` with horizontal threshold detection. Use 80dp as swipe threshold.

3. `YearMonth` comparison: add a helper `fun YearMonth.compareTo(other: YearMonth): Int` inline in the screen file if not already available in the model.

### Overview Screen

1. The `AnimatedContent` in Overview already slides the content body. Enhance it to also accept swipe: add a `Modifier.pointerInput` on the scrollable column to detect horizontal swipes and call `onIntent(PreviousPeriod/NextPeriod)`.

2. The `periodOffset` state already drives the slide direction in the existing `AnimatedContent` — keep that logic.

## Acceptance criteria
- [ ] Swiping left on the Transactions screen navigates to the next month with a slide-left animation
- [ ] Swiping right on the Transactions screen navigates to the previous month with a slide-right animation
- [ ] The month chevron buttons also trigger the same slide animation
- [ ] Swiping left on the Overview screen navigates to the next period
- [ ] Swiping right navigates to the previous period
- [ ] Animations are smooth (no jank) on both platforms

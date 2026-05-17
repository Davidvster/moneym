# Phase 3 — New transaction type bar (income/expense) more noticeable

## Goal
Make the Income/Expense segmented control at the top of the transaction edit screen more prominent by using green background for income and red background for expense. Instead of the subtle `MmSegmented` pill control, build a custom full-width two-button toggle bar.

## File to Change
`feature/transactionEdit/src/commonMain/kotlin/com/dv/moneym/feature/transactionedit/ui/TransactionEditScreen.kt`

## Current Code (lines ~300-312)
```kotlin
MmSegmented(
    options = listOf(stringResource(Res.string.edit_type_expense), stringResource(Res.string.edit_type_income)),
    selectedIndex = if (state.type == TransactionType.EXPENSE) 0 else 1,
    onOptionSelected = { index ->
        onIntent(
            TransactionEditIntent.TypeChanged(
                if (index == 0) TransactionType.EXPENSE else TransactionType.INCOME,
            ),
        )
    },
    fillWidth = true,
)
```

## Implementation Steps

### Step 1: Replace MmSegmented with a custom TypeToggleBar composable
Replace the existing `MmSegmented` block with:

```kotlin
TypeToggleBar(
    isExpense = state.type == TransactionType.EXPENSE,
    expenseLabel = stringResource(Res.string.edit_type_expense),
    incomeLabel = stringResource(Res.string.edit_type_income),
    onExpenseSelected = { onIntent(TransactionEditIntent.TypeChanged(TransactionType.EXPENSE)) },
    onIncomeSelected = { onIntent(TransactionEditIntent.TypeChanged(TransactionType.INCOME)) },
    modifier = Modifier.fillMaxWidth(),
)
```

### Step 2: Add the TypeToggleBar private composable
Add this at the bottom of the file (before the `@Preview` if any, or at the end):

```kotlin
@Composable
private fun TypeToggleBar(
    isExpense: Boolean,
    expenseLabel: String,
    incomeLabel: String,
    onExpenseSelected: () -> Unit,
    onIncomeSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MM.colors
    val type = MM.type
    val radius = MM.radius

    // Colors: green for income, red for expense
    val expenseActiveColor = colors.danger      // red
    val incomeActiveColor = colors.accent       // green
    val inactiveBg = colors.surface2
    val inactiveFg = colors.text2

    Row(
        modifier = modifier
            .height(44.dp)
            .clip(radius.xl)
            .background(inactiveBg),
    ) {
        // Expense tab
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(radius.xl)
                .background(if (isExpense) expenseActiveColor else Color.Transparent)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { onExpenseSelected() },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = expenseLabel,
                style = type.caption.copy(
                    fontWeight = if (isExpense) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (isExpense) Color.White else inactiveFg,
                ),
            )
        }
        // Income tab
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(radius.xl)
                .background(if (!isExpense) incomeActiveColor else Color.Transparent)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { onIncomeSelected() },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = incomeLabel,
                style = type.caption.copy(
                    fontWeight = if (!isExpense) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (!isExpense) Color.White else inactiveFg,
                ),
            )
        }
    }
}
```

### Step 3: Add required imports if not already present
- `androidx.compose.foundation.clickable`
- `androidx.compose.foundation.interaction.MutableInteractionSource`
- `androidx.compose.foundation.layout.Row`
- `androidx.compose.foundation.layout.Box`
- `androidx.compose.foundation.layout.fillMaxHeight`
- `androidx.compose.foundation.layout.height`
- `androidx.compose.ui.text.font.FontWeight`
- `androidx.compose.ui.graphics.Color`
- `androidx.compose.ui.Alignment`

Most of these are already imported in the file. Verify and add only missing ones.

## Acceptance Criteria
1. The type bar is visually prominent — full-width bar that's 44dp tall
2. When "Expense" is selected, the expense side shows a red background with white text
3. When "Income" is selected, the income side shows a green background with white text
4. The inactive side shows a neutral gray background with muted text
5. Clicking either side switches the transaction type correctly
6. Build compiles: `./gradlew :composeApp:assembleDebug`

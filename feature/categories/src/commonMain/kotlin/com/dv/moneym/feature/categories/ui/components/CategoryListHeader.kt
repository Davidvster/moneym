package com.dv.moneym.feature.categories.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.ui.MmButton
import com.dv.moneym.core.ui.MmButtonSize
import com.dv.moneym.core.ui.MmButtonVariant
import com.dv.moneym.core.ui.MmIcons
import com.dv.moneym.core.ui.MmSegmented
import com.dv.moneym.core.ui.ScreenHeader
import com.dv.moneym.feature.categories.presentation.CategoryTab
import moneym.feature.categories.generated.resources.Res
import moneym.feature.categories.generated.resources.categories_hint
import moneym.feature.categories.generated.resources.categories_new_button
import moneym.feature.categories.generated.resources.categories_tab_expense
import moneym.feature.categories.generated.resources.categories_tab_income
import moneym.feature.categories.generated.resources.categories_title
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun CategoryListHeader(
    activeTab: CategoryTab,
    categoryCount: Int,
    onBack: () -> Unit,
    onSetTab: (CategoryTab) -> Unit,
    onAddClick: () -> Unit,
) {
    val colors = MM.colors

    ScreenHeader(
        title = stringResource(Res.string.categories_title),
        onBack = onBack,
        trailingContent = {
            MmButton(
                text = stringResource(Res.string.categories_new_button),
                onClick = onAddClick,
                variant = MmButtonVariant.Ghost,
                size = MmButtonSize.Sm,
                leadingIcon = MmIcons.plus,
            )
        },
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MM.space.padding_2x, vertical = MM.space.padding_1_5x),
    ) {
        MmSegmented(
            options = listOf(
                stringResource(Res.string.categories_tab_expense),
                stringResource(Res.string.categories_tab_income),
            ),
            selectedIndex = if (activeTab == CategoryTab.Expense) 0 else 1,
            onOptionSelected = { onSetTab(if (it == 0) CategoryTab.Expense else CategoryTab.Income) },
            fillWidth = true,
        )
    }

    val tabLabel = if (activeTab == CategoryTab.Expense)
        stringResource(Res.string.categories_tab_expense).lowercase()
    else
        stringResource(Res.string.categories_tab_income).lowercase()

    Text(
        text = "$categoryCount $tabLabel ${stringResource(Res.string.categories_hint)}",
        style = MM.type.caption.copy(color = colors.text3),
        modifier = Modifier.padding(start = MM.space.padding_2_5x, end = MM.space.padding_2_5x, top = 4.dp, bottom = MM.space.padding_1_5x),
    )
}

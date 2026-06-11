package com.dv.moneym.feature.categories.list.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.MoneyMTheme
import com.dv.moneym.core.ui.MmSegmented
import com.dv.moneym.core.ui.ScreenHeader
import com.dv.moneym.feature.categories.list.CategoryTab
import moneym.feature.categories.generated.resources.Res
import moneym.feature.categories.generated.resources.categories_hide_archived
import moneym.feature.categories.generated.resources.categories_hint
import moneym.feature.categories.generated.resources.categories_show_archived_count
import moneym.feature.categories.generated.resources.categories_tab_expense
import moneym.feature.categories.generated.resources.categories_tab_income
import moneym.feature.categories.generated.resources.categories_title
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun CategoryListHeader(
    activeTab: CategoryTab,
    categoryCount: Int,
    archivedCount: Int,
    showArchived: Boolean,
    onBack: () -> Unit,
    onSetTab: (CategoryTab) -> Unit,
    onToggleArchived: () -> Unit,
) {
    val colors = MM.colors

    ScreenHeader(
        title = stringResource(Res.string.categories_title),
        onBack = onBack,
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MM.dimen.padding_2x, vertical = MM.dimen.padding_1_5x),
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

    val hint = if (showArchived) "" else " ${stringResource(Res.string.categories_hint)}"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = MM.dimen.padding_2_5x,
                end = MM.dimen.padding_2_5x,
                top = MM.dimen.padding_0_5x,
                bottom = MM.dimen.padding_1_5x,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "$categoryCount $tabLabel$hint",
            style = MM.type.caption.copy(color = colors.text3),
            modifier = Modifier.weight(1f),
        )
        val toggleLabel = if (showArchived)
            stringResource(Res.string.categories_hide_archived)
        else
            stringResource(Res.string.categories_show_archived_count, archivedCount)
        Text(
            text = toggleLabel,
            style = MM.type.caption.copy(color = colors.accent),
            modifier = Modifier.clickable(onClick = onToggleArchived),
        )
    }
}

@Preview
@Composable
private fun CategoryListHeaderPreview() {
    MoneyMTheme {
        Column {
            CategoryListHeader(
                activeTab = CategoryTab.Expense,
                categoryCount = 8,
                archivedCount = 2,
                showArchived = false,
                onBack = {},
                onSetTab = {},
                onToggleArchived = {},
            )
        }
    }
}

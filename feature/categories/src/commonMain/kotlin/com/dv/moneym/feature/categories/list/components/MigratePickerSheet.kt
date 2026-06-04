package com.dv.moneym.feature.categories.list.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.categoryColor
import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.Icon
import com.dv.moneym.core.model.IndicatorStyle
import com.dv.moneym.core.ui.CategoryIconTile
import com.dv.moneym.core.ui.MmRow
import com.dv.moneym.core.ui.imageVector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MigratePickerSheet(
    title: String,
    targets: List<Category>,
    onSelect: (CategoryId) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MM.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(
            topStart = MM.dimen.padding_2_5x,
            topEnd = MM.dimen.padding_2_5x,
        ),
        containerColor = colors.bg,
        dragHandle = null,
    ) {
        Column(modifier = Modifier.navigationBarsPadding()) {
            Text(
                text = title,
                style = MM.type.title3,
                color = colors.text,
                modifier = Modifier.padding(
                    horizontal = MM.dimen.padding_2_5x,
                    vertical = MM.dimen.padding_3x,
                ),
            )
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(targets, key = { it.id.value }) { cat ->
                    MmRow(
                        onClick = { onSelect(cat.id) },
                        divider = cat.id != targets.lastOrNull()?.id,
                    ) {
                        CategoryIconTile(
                            categoryName = cat.name,
                            categoryColor = categoryColor(cat.colorHex),
                            categoryIcon = Icon.fromKeyOrDefault(cat.iconKey).imageVector,
                            size = 36.dp,
                            variant = IndicatorStyle.IconTile,
                        )
                        Text(
                            text = cat.name,
                            style = MM.type.body,
                            color = colors.text,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

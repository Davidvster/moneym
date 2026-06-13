package com.dv.moneym.feature.categories.list.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.Icon as MmIcon
import com.dv.moneym.core.ui.MmRow
import com.dv.moneym.core.ui.MmSheetHeader
import com.dv.moneym.core.ui.imageVector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DeleteCategoryOptionsSheet(
    category: Category,
    transactionCount: Int,
    title: String,
    subtitle: String,
    migrateLabel: String,
    archiveLabel: String,
    unarchiveLabel: String,
    deleteAllLabel: String,
    onMigrate: () -> Unit,
    onArchive: () -> Unit,
    onUnarchive: () -> Unit,
    onDeleteAll: () -> Unit,
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
            Column(
                modifier = Modifier.padding(
                    start = MM.dimen.padding_2_5x,
                    end = MM.dimen.padding_2_5x,
                    top = MM.dimen.padding_3x,
                    bottom = MM.dimen.padding_2x,
                ),
            ) {
                MmSheetHeader(title = title, onClose = onDismiss)
                if (transactionCount > 0) {
                    Text(
                        text = subtitle,
                        style = MM.type.caption,
                        color = colors.text2,
                        modifier = Modifier.padding(top = MM.dimen.padding_0_5x),
                    )
                }
            }

            if (category.archived) {
                OptionRow(MmIcon.ArrowUp, unarchiveLabel, colors.text, onUnarchive)
            } else {
                OptionRow(MmIcon.Folder, archiveLabel, colors.text, onArchive)
            }
            OptionRow(MmIcon.Tag, migrateLabel, colors.text, onMigrate)
            OptionRow(MmIcon.Trash, deleteAllLabel, colors.danger, onDeleteAll, last = true)
        }
    }
}

@Composable
private fun OptionRow(
    icon: MmIcon,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    last: Boolean = false,
) {
    MmRow(onClick = onClick, divider = !last) {
        Icon(
            imageVector = icon.imageVector,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(MM.dimen.icon_1x),
        )
        Text(
            text = label,
            style = MM.type.body,
            color = tint,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

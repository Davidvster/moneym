package com.dv.moneym.feature.transactionedit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.ui.MmSheetHeader
import com.dv.moneym.core.ui.mmClickable
import moneym.feature.transactionedit.generated.resources.Res
import moneym.feature.transactionedit.generated.resources.edit_calculator_title
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CalculatorBottomSheet(
    initialAmountText: String,
    onDismiss: () -> Unit,
    onAmountSaved: (String) -> Unit,
) {
    val colors = MM.colors
    val type = MM.type
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Calculator state
    var display by remember {
        mutableStateOf(
            if (initialAmountText.isNotBlank() && initialAmountText.toDoubleOrNull() != null &&
                initialAmountText.toDouble() > 0
            ) {
                initialAmountText
            } else {
                ""
            }
        )
    }
    var currentValue by remember { mutableStateOf(0.0) }
    var pendingOp by remember { mutableStateOf<Char?>(null) }
    var justAppliedOp by remember { mutableStateOf(false) }

    // Initialize from current amount
    LaunchedEffect(Unit) {
        val initial = initialAmountText.toDoubleOrNull() ?: 0.0
        if (initial > 0) {
            currentValue = initial
        }
    }

    fun calcResult(): Double {
        val inputVal = display.toDoubleOrNull() ?: 0.0
        return when (pendingOp) {
            '+' -> currentValue + inputVal
            '-' -> currentValue - inputVal
            '*' -> currentValue * inputVal
            '/' -> if (inputVal != 0.0) currentValue / inputVal else currentValue
            else -> inputVal
        }
    }

    fun formatResult(v: Double): String {
        val major = v.toLong()
        val frac = ((v - major) * 100).toLong().coerceAtLeast(0L)
        return "$major.${frac.toString().padStart(2, '0')}"
    }

    fun onCalcKey(key: String) {
        when (key) {
            "C" -> {
                display = ""
                currentValue = 0.0
                pendingOp = null
                justAppliedOp = false
            }

            "⌫" -> {
                if (display.isNotEmpty()) display = display.dropLast(1)
            }

            "+", "-", "*", "/" -> {
                val result = calcResult()
                currentValue = result
                pendingOp = key[0]
                display = ""
                justAppliedOp = true
            }

            "=" -> {
                val result = calcResult()
                display = formatResult(result)
                currentValue = result
                pendingOp = null
                justAppliedOp = false
            }

            "." -> {
                if (!display.contains('.')) display += "."
            }

            else -> {
                if (justAppliedOp) {
                    display = key
                    justAppliedOp = false
                } else {
                    if (display.length < 12) display += key
                }
            }
        }
    }

    val resultPreview = if (pendingOp != null && display.isNotEmpty()) {
        val result = calcResult()
        formatResult(result)
    } else {
        ""
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(
            topStart = MM.dimen.padding_2_5x,
            topEnd = MM.dimen.padding_2_5x
        ),
        containerColor = colors.bg,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = MM.dimen.padding_2x,
                vertical = MM.dimen.padding_2x
            ),
            verticalArrangement = Arrangement.spacedBy(MM.dimen.padding_1_5x),
        ) {
            // Grab handle
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(width = 36.dp, height = MM.dimen.padding_0_5x)
                        .clip(RoundedCornerShape(50))
                        .background(colors.borderStrong),
                )
            }

            MmSheetHeader(
                title = stringResource(Res.string.edit_calculator_title),
                onClose = onDismiss,
            )

            // Display
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(MM.dimen.padding_1_5x))
                    .background(colors.surface)
                    .padding(horizontal = MM.dimen.padding_2x, vertical = MM.dimen.padding_1_5x),
                horizontalAlignment = Alignment.End,
            ) {
                if (pendingOp != null) {
                    Text(
                        text = "${formatResult(currentValue)} $pendingOp${if (display.isNotEmpty()) " $display" else ""}",
                        style = type.caption.copy(color = colors.text2),
                    )
                }
                Text(
                    text = display.ifEmpty { "0" },
                    style = type.display.copy(fontSize = 36.sp, color = colors.text),
                )
                if (resultPreview.isNotEmpty()) {
                    Text(
                        text = "= $resultPreview",
                        style = type.caption.copy(color = colors.accent),
                    )
                }
            }

            // Calculator buttons
            val rows = listOf(
                listOf("7", "8", "9", "/"),
                listOf("4", "5", "6", "*"),
                listOf("1", "2", "3", "-"),
                listOf("C", "0", ".", "+"),
            )

            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MM.dimen.padding_1x),
                ) {
                    row.forEach { key ->
                        val isOp = key in listOf("/", "*", "-", "+")
                        val isClear = key == "C"
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(MM.dimen.padding_7x)
                                .clip(RoundedCornerShape(MM.dimen.padding_1_5x))
                                .background(
                                    when {
                                        isClear -> colors.danger.copy(alpha = 0.15f)
                                        isOp -> colors.accent.copy(alpha = 0.15f)
                                        else -> colors.surface
                                    }
                                )
                                .border(
                                    1.dp,
                                    when {
                                        isClear -> colors.danger.copy(alpha = 0.3f)
                                        isOp -> colors.accent.copy(alpha = 0.3f)
                                        else -> colors.border
                                    },
                                    RoundedCornerShape(MM.dimen.padding_1_5x),
                                )
                                .mmClickable { onCalcKey(key) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = key,
                                style = type.title3.copy(
                                    color = when {
                                        isClear -> colors.danger
                                        isOp -> colors.accent
                                        else -> colors.text
                                    }
                                ),
                            )
                        }
                    }
                }
            }

            // Bottom row: backspace + save
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MM.dimen.padding_1x),
            ) {
                // Backspace
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(MM.dimen.padding_7x)
                        .clip(RoundedCornerShape(MM.dimen.padding_1_5x))
                        .background(colors.surface)
                        .border(1.dp, colors.border, RoundedCornerShape(MM.dimen.padding_1_5x))
                        .mmClickable { onCalcKey("⌫") },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("⌫", style = type.title3.copy(color = colors.text))
                }
                // Equals / result preview
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(MM.dimen.padding_7x)
                        .clip(RoundedCornerShape(MM.dimen.padding_1_5x))
                        .background(colors.accent.copy(alpha = 0.15f))
                        .border(
                            1.dp,
                            colors.accent.copy(alpha = 0.3f),
                            RoundedCornerShape(MM.dimen.padding_1_5x)
                        )
                        .mmClickable { onCalcKey("=") },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("=", style = type.title3.copy(color = colors.accent))
                }
                // Save / apply
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(MM.dimen.padding_7x)
                        .clip(RoundedCornerShape(MM.dimen.padding_1_5x))
                        .background(colors.accent)
                        .mmClickable(rippleColor = Color.White) {
                            val result = calcResult()
                            val finalDisplay = if (display.isEmpty()) {
                                formatResult(currentValue)
                            } else {
                                formatResult(result)
                            }
                            onAmountSaved(finalDisplay)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("✓", style = type.title3.copy(color = Color.White))
                }
            }

            Spacer(Modifier.height(MM.dimen.padding_2x))
        }
    }
}
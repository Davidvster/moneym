package com.dv.moneym.feature.security.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.ui.MmIcons

// Each keypad cell: 80x72dp, 16dp radius, surface bg, 1dp border
@Composable
private fun KeyCell(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val colors = MM.colors
    val shape = RoundedCornerShape(16.dp)
    var pressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .size(width = 80.dp, height = 72.dp)
            .clip(shape)
            .background(colors.surface, shape)
            .border(1.dp, colors.border, shape)
            .alpha(if (pressed) 0.65f else 1f)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                    },
                    onTap = { onClick() },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun TransparentKeyCell(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    var pressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .size(width = 80.dp, height = 72.dp)
            .alpha(if (pressed) 0.5f else 1f)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                    },
                    onTap = { onClick() },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
fun PinKeypad(
    onKey: (Char) -> Unit,
    onDelete: () -> Unit,
    onBiometric: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val colors = MM.colors
    val type = MM.type

    // Row 1: 1 2 3
    // Row 2: 4 5 6
    // Row 3: 7 8 9
    // Row 4: [biometric/empty] [0] [backspace]

    val digitStyle = type.title2.copy(
        fontSize = 28.sp,
        fontWeight = FontWeight.Normal,
        color = colors.text,
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Rows 1-3
        listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9)).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { digit ->
                    KeyCell(onClick = { onKey(digit.digitToChar()) }) {
                        Text(text = "$digit", style = digitStyle)
                    }
                }
            }
        }

        // Row 4: biometric | 0 | backspace
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Biometric or spacer
            if (onBiometric != null) {
                TransparentKeyCell(onClick = onBiometric) {
                    val painter = rememberVectorPainter(MmIcons.faceId)
                    Image(
                        painter = painter,
                        contentDescription = "Use biometric",
                        modifier = Modifier.size(26.dp),
                        colorFilter = ColorFilter.tint(colors.text),
                    )
                }
            } else {
                Box(modifier = Modifier.size(width = 80.dp, height = 72.dp))
            }

            // 0 key
            KeyCell(onClick = { onKey('0') }) {
                Text(text = "0", style = digitStyle)
            }

            // Backspace
            TransparentKeyCell(onClick = onDelete) {
                val painter = rememberVectorPainter(MmIcons.backspace)
                Image(
                    painter = painter,
                    contentDescription = "Delete",
                    modifier = Modifier.size(26.dp),
                    colorFilter = ColorFilter.tint(colors.text),
                )
            }
        }
    }
}

// Legacy adapter — keeps existing call sites with onDigit: (Int) -> Unit compiling
@Composable
fun PinKeypad(
    onDigit: (Int) -> Unit,
    onDelete: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    PinKeypad(
        onKey = { char -> onDigit(char.digitToInt()) },
        onDelete = onDelete,
        onBiometric = null,
        modifier = modifier,
    )
}

package com.dv.moneym.feature.security.shared

import com.dv.moneym.core.ui.imageVector
import com.dv.moneym.core.model.Icon
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.security.BiometryType

// Each keypad cell: 80x72dp, 16dp radius, surface bg, 1dp border
@Composable
private fun KeyCell(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val colors = MM.colors
    val shape = RoundedCornerShape(MM.dimen.padding_2x)
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
    biometryType: BiometryType = BiometryType.Fingerprint,
    modifier: Modifier = Modifier,
) {
    val colors = MM.colors
    val type = MM.type

    val digitStyle = type.title2.copy(
        fontSize = 28.sp,
        fontWeight = FontWeight.Normal,
        color = colors.text,
    )

    // Select biometric icon based on type
    val biometricIcon = when (biometryType) {
        BiometryType.FaceId -> Icon.FaceId.imageVector
        BiometryType.Fingerprint -> Icon.Fingerprint.imageVector
        BiometryType.None -> Icon.Fingerprint.imageVector
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MM.dimen.padding_1_5x),
    ) {
        // Rows 1-3
        listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9)).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(MM.dimen.padding_1_5x)) {
                row.forEach { digit ->
                    KeyCell(onClick = { onKey(digit.digitToChar()) }) {
                        Text(text = "$digit", style = digitStyle)
                    }
                }
            }
        }

        // Row 4: biometric | 0 | backspace
        Row(horizontalArrangement = Arrangement.spacedBy(MM.dimen.padding_1_5x)) {
            // Biometric or spacer
            if (onBiometric != null) {
                TransparentKeyCell(onClick = onBiometric) {
                    val painter = rememberVectorPainter(biometricIcon)
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
                val painter = rememberVectorPainter(Icon.Backspace.imageVector)
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
package com.dv.moneym.core.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dv.moneym.core.designsystem.MM

enum class TabRoute { Transactions, Overview, Settings }

@Composable
fun MmTabBar(
    activeTab: TabRoute,
    onTabSelected: (TabRoute) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MM.colors
    val dividerColor = colors.border

    val tabs = listOf(
        Triple(TabRoute.Transactions, MmIcons.list, "Transactions"),
        Triple(TabRoute.Overview, MmIcons.chart, "Overview"),
        Triple(TabRoute.Settings, MmIcons.settings, "Settings"),
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.bg)
            .drawBehind {
                val strokeWidth = 1.dp.toPx()
                drawLine(
                    color = dividerColor,
                    start = Offset(0f, strokeWidth / 2),
                    end = Offset(size.width, strokeWidth / 2),
                    strokeWidth = strokeWidth,
                )
            },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .windowInsetsPadding(WindowInsets.navigationBars),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            tabs.forEach { (route, icon, label) ->
                val isActive = activeTab == route
                TabItem(
                    icon = icon,
                    label = label,
                    isActive = isActive,
                    activeColor = colors.text,
                    inactiveColor = colors.text3,
                    onClick = { onTabSelected(route) },
                )
            }
        }
    }
}

@Composable
private fun TabItem(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    activeColor: Color,
    inactiveColor: Color,
    onClick: () -> Unit,
) {
    val animatedColor by animateColorAsState(
        targetValue = if (isActive) activeColor else inactiveColor,
        animationSpec = spring(),
        label = "tab_color",
    )
    val animatedIconSize by animateFloatAsState(
        targetValue = if (isActive) 22f else 20f,
        animationSpec = spring(stiffness = 400f),
        label = "tab_icon_size",
    )

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onClick() })
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(modifier = Modifier.size(22.dp), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = animatedColor,
                modifier = Modifier.size(animatedIconSize.dp),
            )
        }
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isActive) FontWeight.W600 else FontWeight.W500,
            color = animatedColor,
        )
    }
}

package com.dv.moneym.core.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

// Extension icons — added separately to keep MmIcons.kt manageable

val MmIcons.calculator: ImageVector
    get() = _calculator ?: buildCalculator().also { _calculator = it }

private var _calculator: ImageVector? = null

private fun buildCalculator(): ImageVector =
    ImageVector.Builder(
        name = "calculator",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        // Calculator body
        addPath(
            pathData = addPathNodes("M5 3 A2 2 0 0 0 3 5 L3 19 A2 2 0 0 0 5 21 L19 21 A2 2 0 0 0 21 19 L21 5 A2 2 0 0 0 19 3 Z"),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            fill = null,
            fillAlpha = 0f,
        )
        // Display area
        addPath(
            pathData = addPathNodes("M7 7 L17 7 L17 10 L7 10 Z"),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            fill = null,
            fillAlpha = 0f,
        )
        // Button dots (simplified representation)
        addPath(
            pathData = addPathNodes("M7 13 L9 13 M11 13 L13 13 M15 13 L17 13 M7 17 L9 17 M11 17 L13 17 M15 17 L17 17"),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            fill = null,
            fillAlpha = 0f,
        )
    }.build()

val MmIcons.wallet: ImageVector
    get() = _wallet ?: buildWallet().also { _wallet = it }

private var _wallet: ImageVector? = null

private fun buildWallet(): ImageVector =
    ImageVector.Builder(
        name = "wallet",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        // Wallet body
        addPath(
            pathData = addPathNodes("M2 7 A2 2 0 0 1 4 5 L20 5 A2 2 0 0 1 22 7 L22 17 A2 2 0 0 1 20 19 L4 19 A2 2 0 0 1 2 17 Z"),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            fill = null,
            fillAlpha = 0f,
        )
        // Coin/circle at right side
        addPath(
            pathData = addPathNodes("M16 12 A2 2 0 1 0 16 14 A2 2 0 1 0 16 12 Z"),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            fill = null,
            fillAlpha = 0f,
        )
        // Top flap line
        addPath(
            pathData = addPathNodes("M2 9 L22 9"),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            fill = null,
            fillAlpha = 0f,
        )
    }.build()

package com.dv.moneym.core.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

// Extension icons — added separately to keep MmIcons.kt manageable

internal val MmIcons.calculator: ImageVector
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

internal val MmIcons.edit: ImageVector
    get() = _edit ?: buildEdit().also { _edit = it }

private var _edit: ImageVector? = null

private fun buildEdit(): ImageVector =
    ImageVector.Builder(
        name = "edit",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        addPath(
            pathData = addPathNodes("M4 20 L8 20 L20 8 L16 4 L4 16 Z"),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            fill = null,
            fillAlpha = 0f,
        )
        addPath(
            pathData = addPathNodes("M16 4 L20 8"),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            fill = null,
            fillAlpha = 0f,
        )
    }.build()

internal val MmIcons.wallet: ImageVector
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

internal val MmIcons.eye: ImageVector
    get() = _eye ?: buildEye().also { _eye = it }

private var _eye: ImageVector? = null

private fun buildEye(): ImageVector =
    ImageVector.Builder(
        name = "eye",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        addPath(
            pathData = addPathNodes("M2 12 C5 6 9 3 12 3 C15 3 19 6 22 12 C19 18 15 21 12 21 C9 21 5 18 2 12 Z"),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            fill = null,
            fillAlpha = 0f,
        )
        addPath(
            pathData = addPathNodes("M12 9 A3 3 0 1 0 12.001 9 Z"),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            fill = null,
            fillAlpha = 0f,
        )
    }.build()

internal val MmIcons.eyeOff: ImageVector
    get() = _eyeOff ?: buildEyeOff().also { _eyeOff = it }

private var _eyeOff: ImageVector? = null

private fun buildEyeOff(): ImageVector =
    ImageVector.Builder(
        name = "eye_off",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        addPath(
            pathData = addPathNodes("M2 12 C5 6 9 3 12 3 C15 3 19 6 22 12 C19 18 15 21 12 21 C9 21 5 18 2 12 Z"),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            fill = null,
            fillAlpha = 0f,
        )
        addPath(
            pathData = addPathNodes("M3 3 L21 21"),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            fill = null,
            fillAlpha = 0f,
        )
    }.build()

private fun strokePath(builder: ImageVector.Builder, data: String) {
    builder.addPath(
        pathData = addPathNodes(data),
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 1.8f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        fill = null,
        fillAlpha = 0f,
    )
}

private fun buildIcon(iconName: String, vararg paths: String): ImageVector =
    ImageVector.Builder(
        name = iconName,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply { paths.forEach { strokePath(this, it) } }.build()

internal val MmIcons.coffee: ImageVector
    get() = _coffee ?: buildIcon(
        "coffee",
        "M5 8 L17 8 L16 18 A3 3 0 0 1 13 21 L9 21 A3 3 0 0 1 6 18 Z",
        "M17 9 A3 3 0 0 1 17 15",
        "M9 2 C9 4 11 4 11 6 M13 2 C13 4 15 4 15 6",
    ).also { _coffee = it }

private var _coffee: ImageVector? = null

internal val MmIcons.plane: ImageVector
    get() = _plane ?: buildIcon(
        "plane",
        "M22 2 L11 13 M22 2 L15 21 L11 13 L3 9 Z",
    ).also { _plane = it }

private var _plane: ImageVector? = null

internal val MmIcons.book: ImageVector
    get() = _book ?: buildIcon(
        "book",
        "M6 3 L18 3 A1 1 0 0 1 19 4 L19 20 A1 1 0 0 1 18 21 L7 21 A2 2 0 0 1 5 19 L5 4 A1 1 0 0 1 6 3 Z",
        "M5 18 L18 18",
    ).also { _book = it }

private var _book: ImageVector? = null

internal val MmIcons.paw: ImageVector
    get() = _paw ?: buildIcon(
        "paw",
        "M7 6.4 A1.6 1.6 0 1 0 7 9.6 A1.6 1.6 0 1 0 7 6.4 Z",
        "M10 3.9 A1.6 1.6 0 1 0 10 7.1 A1.6 1.6 0 1 0 10 3.9 Z",
        "M14 3.9 A1.6 1.6 0 1 0 14 7.1 A1.6 1.6 0 1 0 14 3.9 Z",
        "M17 6.4 A1.6 1.6 0 1 0 17 9.6 A1.6 1.6 0 1 0 17 6.4 Z",
        "M12 12.5 A3 2.5 0 1 0 12 17.5 A3 2.5 0 1 0 12 12.5 Z",
    ).also { _paw = it }

private var _paw: ImageVector? = null

internal val MmIcons.medicalCross: ImageVector
    get() = _medicalCross ?: buildIcon(
        "medical_cross",
        "M10 3 L14 3 L14 10 L21 10 L21 14 L14 14 L14 21 L10 21 L10 14 L3 14 L3 10 L10 10 Z",
    ).also { _medicalCross = it }

private var _medicalCross: ImageVector? = null

internal val MmIcons.fuel: ImageVector
    get() = _fuel ?: buildIcon(
        "fuel",
        "M4 21 L14 21 L14 5 A2 2 0 0 0 12 3 L6 3 A2 2 0 0 0 4 5 Z",
        "M6 6 L12 6 L12 10 L6 10 Z",
        "M14 8 L18 8 A2 2 0 0 1 20 10 L20 15 A1.5 1.5 0 0 1 17 15 L17 12 L14 12",
    ).also { _fuel = it }

private var _fuel: ImageVector? = null

internal val MmIcons.phone: ImageVector
    get() = _phone ?: buildIcon(
        "phone",
        "M7 2 L17 2 A1 1 0 0 1 18 3 L18 21 A1 1 0 0 1 17 22 L7 22 A1 1 0 0 1 6 21 L6 3 A1 1 0 0 1 7 2 Z",
        "M10 19 L14 19",
    ).also { _phone = it }

private var _phone: ImageVector? = null

internal val MmIcons.dumbbell: ImageVector
    get() = _dumbbell ?: buildIcon(
        "dumbbell",
        "M3 9 L3 15 M6 7 L6 17 M6 12 L18 12 M18 7 L18 17 M21 9 L21 15",
    ).also { _dumbbell = it }

private var _dumbbell: ImageVector? = null

internal val MmIcons.shirt: ImageVector
    get() = _shirt ?: buildIcon(
        "shirt",
        "M8 3 L4 6 L6 9 L8 8 L8 21 L16 21 L16 8 L18 9 L20 6 L16 3 C16 5 8 5 8 3 Z",
    ).also { _shirt = it }

private var _shirt: ImageVector? = null

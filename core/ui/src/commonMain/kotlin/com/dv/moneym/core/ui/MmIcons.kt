package com.dv.moneym.core.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp
import com.dv.moneym.core.model.Icon

internal object MmIcons {

    private val plus: ImageVector by lazy {
        ImageVector.Builder(
            name = "plus",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                pathData = addPathNodes("M12 5 L12 19"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
            addPath(
                pathData = addPathNodes("M5 12 L19 12"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
        }.build()
    }

    private val close: ImageVector by lazy {
        ImageVector.Builder(
            name = "close",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                pathData = addPathNodes("M6 6 L18 18"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
            addPath(
                pathData = addPathNodes("M18 6 L6 18"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
        }.build()
    }

    private val search: ImageVector by lazy {
        ImageVector.Builder(
            name = "search",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                pathData = addPathNodes("M11 4 A7 7 0 1 0 11 18 A7 7 0 1 0 11 4 Z"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
            addPath(
                pathData = addPathNodes("M16.5 16.5 L21 21"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
        }.build()
    }

    private val chevronLeft: ImageVector by lazy {
        ImageVector.Builder(
            name = "chevronLeft",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                pathData = addPathNodes("M15 6 L9 12 L15 18"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
        }.build()
    }

    private val chevronRight: ImageVector by lazy {
        ImageVector.Builder(
            name = "chevronRight",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                pathData = addPathNodes("M9 6 L15 12 L9 18"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
        }.build()
    }

    private val chevronDown: ImageVector by lazy {
        ImageVector.Builder(
            name = "chevronDown",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                pathData = addPathNodes("M6 9 L12 15 L18 9"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
        }.build()
    }

    private val check: ImageVector by lazy {
        ImageVector.Builder(
            name = "check",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                pathData = addPathNodes("M5 12 L10 17 L19 7"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
        }.build()
    }

    private val trash: ImageVector by lazy {
        ImageVector.Builder(
            name = "trash",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                pathData = addPathNodes("M4 7 L20 7"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
            addPath(
                pathData = addPathNodes("M9 7 L9 5 A2 2 0 0 1 11 3 L13 3 A2 2 0 0 1 15 5 L15 7"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
            addPath(
                pathData = addPathNodes("M6 7 L7 20 A2 2 0 0 0 9 22 L15 22 A2 2 0 0 0 17 20 L18 7"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
        }.build()
    }

    private val backspace: ImageVector by lazy {
        ImageVector.Builder(
            name = "backspace",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                pathData = addPathNodes("M21 5 L9 5 L3 12 L9 19 L21 19 A1 1 0 0 0 22 18 L22 6 A1 1 0 0 0 21 5 Z"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
            addPath(
                pathData = addPathNodes("M14 9 L18 13"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
            addPath(
                pathData = addPathNodes("M18 9 L14 13"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
        }.build()
    }

    private val faceId: ImageVector by lazy {
        ImageVector.Builder(
            name = "faceId",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                pathData = addPathNodes("M5 9 L5 7 A2 2 0 0 1 7 5 L9 5"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
            addPath(
                pathData = addPathNodes("M19 9 L19 7 A2 2 0 0 0 17 5 L15 5"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
            addPath(
                pathData = addPathNodes("M5 15 L5 17 A2 2 0 0 0 7 19 L9 19"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
            addPath(
                pathData = addPathNodes("M19 15 L19 17 A2 2 0 0 1 17 19 L15 19"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
            addPath(
                pathData = addPathNodes("M9 10 L9 11"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
            addPath(
                pathData = addPathNodes("M15 10 L15 11"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
            addPath(
                pathData = addPathNodes("M12 9 L12 13"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
            addPath(
                pathData = addPathNodes("M9 15 C9.6 15.6 10.6 16 12 16 C13.4 16 14.4 15.6 15 15"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
        }.build()
    }

    private val fingerprint: ImageVector by lazy {
        ImageVector.Builder(
            name = "fingerprint",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                pathData = addPathNodes("M12 11 L12 14 A8 8 0 0 1 11 18"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
            addPath(
                pathData = addPathNodes("M8 11 A4 4 0 0 1 16 11 L16 14"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
            addPath(
                pathData = addPathNodes("M5 11 A7 7 0 0 1 19 11 L19 12"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
            addPath(
                pathData = addPathNodes("M19 14 A14 14 0 0 1 18.5 18"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
            addPath(
                pathData = addPathNodes("M14 17 A3 3 0 0 1 13.5 19"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
            addPath(
                pathData = addPathNodes("M12 7 L12 7"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
        }.build()
    }

    private val lock: ImageVector by lazy {
        ImageVector.Builder(
            name = "lock",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                pathData = addPathNodes("M5 11 L5 21 L19 21 L19 11 Z"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
            addPath(
                pathData = addPathNodes("M8 11 L8 8 A4 4 0 0 1 16 8 L16 11"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
        }.build()
    }

    private val sun: ImageVector by lazy {
        ImageVector.Builder(
            name = "sun",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                pathData = addPathNodes("M12 8 A4 4 0 1 0 12 16 A4 4 0 1 0 12 8 Z"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
            addPath(
                pathData = addPathNodes("M12 2 L12 4 M12 20 L12 22 M2 12 L4 12 M20 12 L22 12 M4.9 4.9 L6.3 6.3 M17.7 17.7 L19.1 19.1 M4.9 19.1 L6.3 17.7 M17.7 6.3 L19.1 4.9"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
        }.build()
    }

    private val moon: ImageVector by lazy {
        ImageVector.Builder(
            name = "moon",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                pathData = addPathNodes("M21 12.8 A9 9 0 1 1 11.2 3 A7 7 0 0 0 21 12.8 Z"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
        }.build()
    }

    private val sliders: ImageVector by lazy {
        ImageVector.Builder(
            name = "sliders",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                pathData = addPathNodes("M4 6 L20 6 M4 12 L20 12 M4 18 L20 18"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
            addPath(
                pathData = addPathNodes("M9 4 A2 2 0 1 0 9 8 A2 2 0 1 0 9 4 Z"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
            addPath(
                pathData = addPathNodes("M15 10 A2 2 0 1 0 15 14 A2 2 0 1 0 15 10 Z"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
            addPath(
                pathData = addPathNodes("M11 16 A2 2 0 1 0 11 20 A2 2 0 1 0 11 16 Z"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
        }.build()
    }

    private val globe: ImageVector by lazy {
        ImageVector.Builder(
            name = "globe",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                pathData = addPathNodes("M12 3 A9 9 0 1 0 12 21 A9 9 0 1 0 12 3 Z"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
            addPath(
                pathData = addPathNodes("M3 12 L21 12"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
            addPath(
                pathData = addPathNodes("M12 3 A14 14 0 0 1 12 21 A14 14 0 0 1 12 3"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
        }.build()
    }

    private val list: ImageVector by lazy {
        ImageVector.Builder(
            name = "list",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                pathData = addPathNodes("M8 6 L20 6 M8 12 L20 12 M8 18 L20 18"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
            addPath(
                pathData = addPathNodes("M4 6 A1 1 0 1 0 4 8 A1 1 0 1 0 4 6 Z"),
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                stroke = null,
            )
            addPath(
                pathData = addPathNodes("M4 11 A1 1 0 1 0 4 13 A1 1 0 1 0 4 11 Z"),
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                stroke = null,
            )
            addPath(
                pathData = addPathNodes("M4 17 A1 1 0 1 0 4 19 A1 1 0 1 0 4 17 Z"),
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                stroke = null,
            )
        }.build()
    }

    private val download: ImageVector by lazy {
        ImageVector.Builder(
            name = "download",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                pathData = addPathNodes("M12 3 L12 16"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
            addPath(
                pathData = addPathNodes("M7 11 L12 16 L17 11"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
            addPath(
                pathData = addPathNodes("M5 20 L19 20"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
        }.build()
    }

    private val folder: ImageVector by lazy {
        ImageVector.Builder(
            name = "folder",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                pathData = addPathNodes("M4 7 A2 2 0 0 1 6 5 L9 5 L11 7 L18 7 A2 2 0 0 1 20 9 L20 18 A2 2 0 0 1 18 20 L6 20 A2 2 0 0 1 4 18 Z"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
        }.build()
    }

    private val arrowUp: ImageVector by lazy {
        ImageVector.Builder(
            name = "arrowUp",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                pathData = addPathNodes("M12 19 L12 5"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
            addPath(
                pathData = addPathNodes("M6 11 L12 5 L18 11"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
        }.build()
    }

    private val arrowDown: ImageVector by lazy {
        ImageVector.Builder(
            name = "arrowDown",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                pathData = addPathNodes("M12 5 L12 19"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
            addPath(
                pathData = addPathNodes("M6 13 L12 19 L18 13"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
        }.build()
    }

    private val info: ImageVector by lazy {
        ImageVector.Builder(
            name = "info",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                pathData = addPathNodes("M12 3 A9 9 0 1 0 12 21 A9 9 0 1 0 12 3 Z"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
            addPath(
                pathData = addPathNodes("M12 11 L12 16"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
            addPath(
                pathData = addPathNodes("M12 7.5 A0.5 0.5 0 1 0 12 8.5 A0.5 0.5 0 1 0 12 7.5 Z"),
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                stroke = null,
            )
        }.build()
    }

    private val heart: ImageVector by lazy {
        ImageVector.Builder(
            name = "heart",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                pathData = addPathNodes("M20.84 4.61 A5.5 5.5 0 0 0 13.06 4.61 L12 5.67 L10.94 4.61 A5.5 5.5 0 0 0 3.16 12.39 L12 21.23 L20.84 12.39 A5.5 5.5 0 0 0 20.84 4.61 Z"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
        }.build()
    }

    private val film: ImageVector by lazy {
        ImageVector.Builder(
            name = "film",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                pathData = addPathNodes("M3 6 L21 6 L21 20 A2 2 0 0 1 19 22 L5 22 A2 2 0 0 1 3 20 Z"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
            addPath(
                pathData = addPathNodes("M7 4 L7 20 M17 4 L17 20 M3 9 L7 9 M17 9 L21 9 M3 15 L7 15 M17 15 L21 15"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
        }.build()
    }

    private val car: ImageVector by lazy {
        ImageVector.Builder(
            name = "car",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                pathData = addPathNodes("M5 17 L5 12 L7 7 L17 7 L19 12 L19 17"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
            addPath(
                pathData = addPathNodes("M3 17 L21 17"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
            addPath(
                pathData = addPathNodes("M7.5 15.9 A1.6 1.6 0 1 0 7.5 19.1 A1.6 1.6 0 1 0 7.5 15.9 Z"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
            addPath(
                pathData = addPathNodes("M16.5 15.9 A1.6 1.6 0 1 0 16.5 19.1 A1.6 1.6 0 1 0 16.5 15.9 Z"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
        }.build()
    }

    private val bolt: ImageVector by lazy {
        ImageVector.Builder(
            name = "bolt",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                pathData = addPathNodes("M13 2 L4 14 L11 14 L10 22 L19 10 L12 10 Z"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
        }.build()
    }

    private val basket: ImageVector by lazy {
        ImageVector.Builder(
            name = "basket",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                pathData = addPathNodes("M4 10 L3 20 A2 2 0 0 0 5 22 L19 22 A2 2 0 0 0 21 20 L20 10"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
            addPath(
                pathData = addPathNodes("M7 10 L12 4 L17 10"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
            addPath(
                pathData = addPathNodes("M2 10 L22 10"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
        }.build()
    }

    private val utensils: ImageVector by lazy {
        ImageVector.Builder(
            name = "utensils",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                pathData = addPathNodes("M6 3 L6 11 A2 2 0 0 0 8 13 L8 22"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
            addPath(
                pathData = addPathNodes("M10 3 L10 11 A2 2 0 0 1 8 13"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
            addPath(
                pathData = addPathNodes("M17 3 C15 3 14 4 14 6 L14 12 C14 13 15 14 16 14 L17 14 L17 22"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
        }.build()
    }

    private val home: ImageVector by lazy {
        ImageVector.Builder(
            name = "home",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                pathData = addPathNodes("M3 11 L12 3 L21 11"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
            addPath(
                pathData = addPathNodes("M5 10 L5 20 A1 1 0 0 0 6 21 L10 21 L10 14 L14 14 L14 21 L18 21 A1 1 0 0 0 19 20 L19 10"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
        }.build()
    }

    private val bag: ImageVector by lazy {
        ImageVector.Builder(
            name = "bag",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                pathData = addPathNodes("M6 7 L18 7 L17 20 A2 2 0 0 1 15 22 L9 22 A2 2 0 0 1 7 20 Z"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
            addPath(
                pathData = addPathNodes("M9 7 L9 5 A3 3 0 0 1 15 5 L15 7"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
        }.build()
    }

    private val tag: ImageVector by lazy {
        ImageVector.Builder(
            name = "tag",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                pathData = addPathNodes("M20.59 13.41 L13.41 20.59 A2 2 0 0 1 10.58 20.59 L3 13 L3 3 L13 3 L20.59 10.59 A2 2 0 0 1 20.59 13.41 Z"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
            addPath(
                pathData = addPathNodes("M7.5 6 A1.5 1.5 0 1 0 7.5 9 A1.5 1.5 0 1 0 7.5 6 Z"),
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                stroke = null,
            )
        }.build()
    }

    private val banknote: ImageVector by lazy {
        ImageVector.Builder(
            name = "banknote",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                pathData = addPathNodes("M2 8 A2 2 0 0 1 4 6 L20 6 A2 2 0 0 1 22 8 L22 16 A2 2 0 0 1 20 18 L4 18 A2 2 0 0 1 2 16 Z"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
            addPath(
                pathData = addPathNodes("M12 9.5 A2.5 2.5 0 1 0 12 14.5 A2.5 2.5 0 1 0 12 9.5 Z"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
            addPath(
                pathData = addPathNodes("M5.5 9 L5.5 9"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
            addPath(
                pathData = addPathNodes("M18.5 15 L18.5 15"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
        }.build()
    }

    private val gift: ImageVector by lazy {
        ImageVector.Builder(
            name = "gift",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                pathData = addPathNodes("M3 9 A1 1 0 0 1 4 8 L20 8 A1 1 0 0 1 21 9 L21 21 L3 21 Z"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
            addPath(
                pathData = addPathNodes("M12 8 L12 21 M3 12 L21 12"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
            addPath(
                pathData = addPathNodes("M12 8 C11 5 9 3 7 3 A2 2 0 0 0 7 8 L12 8 Z"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
            addPath(
                pathData = addPathNodes("M12 8 C13 5 15 3 17 3 A2 2 0 0 1 17 8 L12 8 Z"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
        }.build()
    }

    private val calendar: ImageVector by lazy {
        ImageVector.Builder(
            name = "calendar",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                pathData = addPathNodes("M3 7 A2 2 0 0 1 5 5 L19 5 A2 2 0 0 1 21 7 L21 19 A2 2 0 0 1 19 21 L5 21 A2 2 0 0 1 3 19 Z"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
            addPath(
                pathData = addPathNodes("M3 10 L21 10 M8 3 L8 7 M16 3 L16 7"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
        }.build()
    }

    private val dragHandle: ImageVector by lazy {
        ImageVector.Builder(
            name = "dragHandle",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            // 2 columns x 3 rows, each dot ~2dp, spaced 4dp apart, centered in 24x24
            // columns at x=9, x=15; rows at y=8, y=12, y=16
            val xs = listOf(9f, 15f)
            val ys = listOf(8f, 12f, 16f)
            for (y in ys) {
                for (x in xs) {
                    addPath(
                        pathData = addPathNodes("M${x - 1} $y A1 1 0 1 0 ${x + 1} $y A1 1 0 1 0 ${x - 1} $y Z"),
                        fill = SolidColor(Color.Black),
                        pathFillType = PathFillType.NonZero,
                        stroke = null,
                    )
                }
            }
        }.build()
    }

    private val settings: ImageVector by lazy {
        ImageVector.Builder(
            name = "settings",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                pathData = addPathNodes("M12 9 A3 3 0 1 0 12 15 A3 3 0 1 0 12 9 Z"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
            addPath(
                pathData = addPathNodes("M19.4 15 A1.65 1.65 0 0 0 19.73 16.82 L19.79 16.88 A2 2 0 1 1 16.96 19.71 L16.9 19.65 A1.65 1.65 0 0 0 15.08 19.32 A1.65 1.65 0 0 0 14.08 20.83 L14.08 21 A2 2 0 1 1 10.08 21 L10.08 20.91 A1.65 1.65 0 0 0 9.08 19.4 A1.65 1.65 0 0 0 7.26 19.73 L7.2 19.79 A2 2 0 1 1 4.37 16.96 L4.43 16.9 A1.65 1.65 0 0 0 4.76 15.08 A1.65 1.65 0 0 0 3.25 14.08 L3.08 14.08 A2 2 0 1 1 3.08 10.08 L3.17 10.08 A1.65 1.65 0 0 0 4.68 9.08 A1.65 1.65 0 0 0 4.35 7.26 L4.29 7.2 A2 2 0 1 1 7.12 4.37 L7.18 4.43 A1.65 1.65 0 0 0 9 4.76 L9 4.76 A1.65 1.65 0 0 0 10 3.25 L10 3.08 A2 2 0 1 1 14 3.08 L14 3.17 A1.65 1.65 0 0 0 15 4.68 L15 4.68 A1.65 1.65 0 0 0 16.82 4.35 L16.88 4.29 A2 2 0 1 1 19.71 7.12 L19.65 7.18 A1.65 1.65 0 0 0 19.32 9 L19.32 9 A1.65 1.65 0 0 0 20.83 10 L21 10 A2 2 0 1 1 21 14 L20.91 14 A1.65 1.65 0 0 0 19.4 15 Z"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
        }.build()
    }

    private val chart: ImageVector by lazy {
        ImageVector.Builder(
            name = "chart",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                pathData = addPathNodes("M4 19 L4 9 M10 19 L10 5 M16 19 L16 12 M3 21 L21 21"),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
                fillAlpha = 0f,
            )
        }.build()
    }

    internal fun forIcon(icon: Icon): ImageVector = when (icon) {
        Icon.Plus -> plus
        Icon.Close -> close
        Icon.Search -> search
        Icon.ChevronLeft -> chevronLeft
        Icon.ChevronRight -> chevronRight
        Icon.ChevronDown -> chevronDown
        Icon.Check -> check
        Icon.Trash -> trash
        Icon.Backspace -> backspace
        Icon.FaceId -> faceId
        Icon.Fingerprint -> fingerprint
        Icon.Lock -> lock
        Icon.Sun -> sun
        Icon.Moon -> moon
        Icon.Sliders -> sliders
        Icon.Globe -> globe
        Icon.List -> list
        Icon.Download -> download
        Icon.Folder -> folder
        Icon.ArrowUp -> arrowUp
        Icon.ArrowDown -> arrowDown
        Icon.Info -> info
        Icon.Heart -> heart
        Icon.Film -> film
        Icon.Car -> car
        Icon.Bolt -> bolt
        Icon.Basket -> basket
        Icon.Utensils -> utensils
        Icon.Home -> home
        Icon.Bag -> bag
        Icon.Tag -> tag
        Icon.Banknote -> banknote
        Icon.Gift -> gift
        Icon.Calendar -> calendar
        Icon.DragHandle -> dragHandle
        Icon.Settings -> settings
        Icon.Chart -> chart
        Icon.Calculator -> calculator
        Icon.Wallet -> wallet
        Icon.Cart -> basket
        Icon.Restaurant -> utensils
        Icon.HeartPulse -> heart
        Icon.Health -> heart
        Icon.PlayCircle -> film
        Icon.Bank -> banknote
        Icon.Dots -> tag
    }
}

package com.dv.moneym.feature.infopage

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import sergio.sastre.composable.preview.scanner.android.AndroidComposablePreviewScanner
import sergio.sastre.composable.preview.scanner.android.AndroidPreviewInfo
import sergio.sastre.composable.preview.scanner.core.preview.ComposablePreview

@RunWith(Parameterized::class)
class PreviewScreenshotTest(
    private val preview: ComposablePreview<AndroidPreviewInfo>,
) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun previews(): List<ComposablePreview<AndroidPreviewInfo>> =
            AndroidComposablePreviewScanner()
                .scanPackageTrees("com.dv.moneym.feature.infopage")
                .includePrivatePreviews()
                .getPreviews()
    }

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_6,
        renderingMode = SessionParams.RenderingMode.SHRINK,
        maxPercentDifference = 0.1,
    )

    @Test
    fun snapshot() {
        paparazzi.snapshot {
            preview()
        }
    }
}

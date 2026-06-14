package com.dv.moneym.feature.transactions

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.android.resources.ScreenOrientation
import com.dv.moneym.feature.transactions.list.StoreTransactionListPreview
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

// Renders the full-screen store preview at phone / 7" / 10" form factors.
// recordPaparazziDebug writes store_<device>_transactions.png for the framing script.
@RunWith(Parameterized::class)
class StoreScreenshotTest(
    private val deviceName: String,
    private val device: DeviceConfig,
) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun devices(): List<Array<Any>> = listOf(
            arrayOf("phone", DeviceConfig.PIXEL_6),
            arrayOf("tab7", DeviceConfig.NEXUS_7),
            arrayOf(
                "tab10",
                DeviceConfig.NEXUS_10.copy(
                    screenWidth = 1600,
                    screenHeight = 2560,
                    orientation = ScreenOrientation.PORTRAIT,
                ),
            ),
        )
    }

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = device,
        renderingMode = SessionParams.RenderingMode.NORMAL,
        maxPercentDifference = 0.1,
        useDeviceResolution = true,
    )

    @Test
    fun store() {
        paparazzi.snapshot(name = "store_${deviceName}_transactions") {
            StoreTransactionListPreview()
        }
    }
}

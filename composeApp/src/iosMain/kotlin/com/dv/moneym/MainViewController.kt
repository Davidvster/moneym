package com.dv.moneym

import androidx.compose.ui.window.ComposeUIViewController
import com.dv.moneym.di.iosPlatformModule

fun MainViewController() = ComposeUIViewController {
    App(platformModules = listOf(iosPlatformModule()))
}

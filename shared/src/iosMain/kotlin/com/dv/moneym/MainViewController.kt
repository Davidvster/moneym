package com.dv.moneym

import androidx.compose.ui.window.ComposeUIViewController
import com.dv.moneym.data.banksync.BankAuthCallbackBus
import com.dv.moneym.di.iosPlatformModule
import org.koin.mp.KoinPlatform

fun MainViewController() = ComposeUIViewController {
    initKoin(listOf(iosPlatformModule()))
    App()
}

fun handleDeepLink(url: String) {
    if (!url.startsWith("moneym://bank-callback")) return
    runCatching { KoinPlatform.getKoin().get<BankAuthCallbackBus>().emit(url) }
}

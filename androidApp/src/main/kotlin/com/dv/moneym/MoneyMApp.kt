package com.dv.moneym

import android.app.Application
import com.dv.moneym.di.androidPlatformModule

class MoneyMApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin(listOf(androidPlatformModule(this)))
    }
}

package com.dv.moneym

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
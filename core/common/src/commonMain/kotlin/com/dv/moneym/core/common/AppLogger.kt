package com.dv.moneym.core.common

import co.touchlab.kermit.Logger

object AppLogger {
    fun tag(tag: String): Logger = Logger.withTag(tag)
}

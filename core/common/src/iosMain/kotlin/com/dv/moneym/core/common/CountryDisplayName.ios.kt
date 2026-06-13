package com.dv.moneym.core.common

import platform.Foundation.NSLocale
import platform.Foundation.NSLocaleCountryCode
import platform.Foundation.currentLocale

actual fun countryDisplayName(countryCode: String): String =
    NSLocale.currentLocale.displayNameForKey(NSLocaleCountryCode, countryCode)
        ?.takeIf { it.isNotBlank() }
        ?: countryCode

package com.dv.moneym.core.common

import java.util.Locale

actual fun countryDisplayName(countryCode: String): String =
    Locale("", countryCode).getDisplayCountry(Locale.getDefault()).ifBlank { countryCode }

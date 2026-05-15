package com.dv.moneym.core.common

interface LocaleController {
    fun applyLocale(languageTag: String)
    fun getCurrentLanguageTag(): String
}

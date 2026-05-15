package com.dv.moneym.locale

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.dv.moneym.core.common.LocaleController
import com.dv.moneym.core.datastore.AppSettings
import com.dv.moneym.core.datastore.PrefKeys

class AndroidLocaleController(private val settings: AppSettings) : LocaleController {
    override fun applyLocale(languageTag: String) {
        val list = if (languageTag.isEmpty()) LocaleListCompat.getEmptyLocaleList()
                   else LocaleListCompat.forLanguageTags(languageTag)
        AppCompatDelegate.setApplicationLocales(list)
        settings.putString(PrefKeys.LANGUAGE, languageTag)
    }

    override fun getCurrentLanguageTag(): String =
        settings.getString(PrefKeys.LANGUAGE) ?: ""
}

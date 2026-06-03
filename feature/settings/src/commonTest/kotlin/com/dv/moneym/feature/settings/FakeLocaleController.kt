package com.dv.moneym.feature.settings

import com.dv.moneym.core.common.LocaleController

class FakeLocaleController(
    initialTag: String = "",
) : LocaleController {
    var appliedTag: String? = null
        private set
    private var currentTag: String = initialTag

    override fun applyLocale(languageTag: String) {
        appliedTag = languageTag
        currentTag = languageTag
    }

    override fun getCurrentLanguageTag(): String = currentTag
}

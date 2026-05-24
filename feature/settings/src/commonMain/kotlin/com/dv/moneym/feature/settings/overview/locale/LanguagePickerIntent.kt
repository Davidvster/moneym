package com.dv.moneym.feature.settings.overview.locale

sealed interface LanguagePickerIntent {
    data class SetLanguage(val tag: String) : LanguagePickerIntent
}

package com.dv.moneym.core.common

enum class SupportedLanguage(
    val code: String,
    val nativeName: String,
    val englishName: String,
) {
    ENGLISH("en", "English", "English"),
    GERMAN("de", "Deutsch", "German"),
    SPANISH("es", "Español", "Spanish"),
    ITALIAN("it", "Italiano", "Italian"),
    FRENCH("fr", "Français", "French"),
    PORTUGUESE("pt", "Português", "Portuguese"),
    LITHUANIAN("lt", "Lietuvių", "Lithuanian"),
    ESTONIAN("et", "Eesti", "Estonian"),
    MACEDONIAN("mk", "Македонски", "Macedonian"),
    SWEDISH("sv", "Svenska", "Swedish"),
    NORWEGIAN("nb", "Norsk bokmål", "Norwegian"),
    ICELANDIC("is", "Íslenska", "Icelandic"),
    LATVIAN("lv", "Latviešu", "Latvian"),
    POLISH("pl", "Polski", "Polish"),
    DUTCH("nl", "Nederlands", "Dutch"),
    DANISH("da", "Dansk", "Danish"),
    FINNISH("fi", "Suomi", "Finnish"),
    CROATIAN("hr", "Hrvatski", "Croatian"),
    SLOVAK("sk", "Slovenčina", "Slovak"),
    CZECH("cs", "Čeština", "Czech"),
    HUNGARIAN("hu", "Magyar", "Hungarian"),
    JAPANESE("ja", "日本語", "Japanese"),
    VIETNAMESE("vi", "Tiếng Việt", "Vietnamese"),
    TURKISH("tr", "Türkçe", "Turkish"),
    SLOVENIAN("sl", "Slovenščina", "Slovenian"),
    RUSSIAN("ru", "Русский", "Russian"),
    ARABIC("ar", "العربية", "Arabic"),
    HINDI("hi", "हिन्दी", "Hindi"),
    CHINESE("zh", "中文", "Chinese");

    companion object {
        fun fromCode(code: String): SupportedLanguage? =
            entries.firstOrNull { it.code == code.trim().lowercase() }

        fun fromLanguageTag(tag: String): SupportedLanguage? {
            val primary = tag.substringBefore('-').trim().lowercase()
            return when (primary) {
                "no", "nn" -> NORWEGIAN
                else -> fromCode(primary)
            }
        }

        fun responseLanguageNameForTag(tag: String): String? =
            fromLanguageTag(tag)
                ?.takeUnless { it == ENGLISH }
                ?.englishName
    }
}

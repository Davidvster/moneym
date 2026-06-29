package com.dv.moneym.core.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SupportedLanguageTest {

    @Test
    fun resolvesStoredLanguageCode() {
        assertEquals(SupportedLanguage.GERMAN, SupportedLanguage.fromCode("de"))
    }

    @Test
    fun resolvesPrimaryLanguageFromTag() {
        assertEquals(SupportedLanguage.PORTUGUESE, SupportedLanguage.fromLanguageTag("pt-BR"))
    }

    @Test
    fun resolvesNorwegianAliases() {
        assertEquals(SupportedLanguage.NORWEGIAN, SupportedLanguage.fromLanguageTag("no"))
        assertEquals(SupportedLanguage.NORWEGIAN, SupportedLanguage.fromLanguageTag("nn-NO"))
    }

    @Test
    fun responseLanguageSkipsEnglishAndUnknownTags() {
        assertNull(SupportedLanguage.responseLanguageNameForTag("en-US"))
        assertNull(SupportedLanguage.responseLanguageNameForTag("xx"))
    }

    @Test
    fun responseLanguageUsesEnglishLanguageName() {
        assertEquals("German", SupportedLanguage.responseLanguageNameForTag("de-DE"))
    }
}

package com.dv.moneym.feature.banksync.usecase

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ParseRedirectCodeUseCaseTest {

    private val parse = ParseRedirectCodeUseCase()

    @Test
    fun parsesCodeFromFullRedirectUrl() {
        assertEquals(
            "abc123",
            parse("moneym://bank-callback?code=abc123&state=xyz"),
        )
        assertEquals(
            "abc123",
            parse("https://example.com/cb?state=xyz&code=abc123#frag"),
        )
    }

    @Test
    fun acceptsBareCode() {
        assertEquals("abc123", parse("  abc123 "))
    }

    @Test
    fun rejectsGarbage() {
        assertNull(parse(""))
        assertNull(parse("https://example.com/cb?state=xyz"))
        assertNull(parse("some random sentence"))
    }
}

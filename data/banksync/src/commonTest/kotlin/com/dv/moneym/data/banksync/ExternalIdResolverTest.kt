package com.dv.moneym.data.banksync

import com.dv.moneym.data.banksync.internal.platformCryptographyProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate

class ExternalIdResolverTest {

    private val resolver = ExternalIdResolver(platformCryptographyProvider())
    private val date = LocalDate(2026, 6, 1)

    @Test
    fun prefersEntryReference() = runTest {
        val id = resolver.resolve("acc-1", "ref-42", date, 1250, "EUR", "coffee")
        assertEquals("eb:acc-1:ref-42", id)
    }

    @Test
    fun fallbackHashIsStable() = runTest {
        val a = resolver.resolve("acc-1", null, date, 1250, "EUR", "coffee")
        val b = resolver.resolve("acc-1", "", date, 1250, "EUR", "coffee")
        assertEquals(a, b)
        assertTrue(a.startsWith("ebh:"))
    }

    @Test
    fun fallbackHashDiffersOnAnyField() = runTest {
        val base = resolver.resolve("acc-1", null, date, 1250, "EUR", "coffee")
        assertNotEquals(base, resolver.resolve("acc-2", null, date, 1250, "EUR", "coffee"))
        assertNotEquals(base, resolver.resolve("acc-1", null, date, 1251, "EUR", "coffee"))
        assertNotEquals(base, resolver.resolve("acc-1", null, date, 1250, "USD", "coffee"))
        assertNotEquals(base, resolver.resolve("acc-1", null, date, 1250, "EUR", "tea"))
    }

    @Test
    fun disambiguateSuffixesDuplicatesInBatch() {
        val out = resolver.disambiguate(listOf("x", "y", "x", "x"))
        assertEquals(listOf("x", "y", "x-2", "x-3"), out)
    }
}

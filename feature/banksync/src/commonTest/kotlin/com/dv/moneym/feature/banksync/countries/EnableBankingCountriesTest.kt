package com.dv.moneym.feature.banksync.countries

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EnableBankingCountriesTest {

    @Test
    fun codesAreTwoLetterUppercaseAndUnique() {
        val codes = EnableBankingCountries.codes
        assertTrue(codes.isNotEmpty())
        assertTrue(codes.all { it.length == 2 }, "all codes must be 2 letters")
        assertTrue(codes.all { it == it.uppercase() }, "all codes must be uppercase")
        assertEquals(codes.size, codes.toSet().size, "codes must be unique")
    }
}

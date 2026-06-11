package com.dv.moneym.data.banksync.internal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class EbAmountParserTest {

    @Test
    fun parsesTwoDecimalCurrencies() {
        assertEquals(1250L, parseAmountToMinorUnits("12.50", "EUR"))
        assertEquals(1250L, parseAmountToMinorUnits("12.5", "EUR"))
        assertEquals(1200L, parseAmountToMinorUnits("12", "EUR"))
        assertEquals(10L, parseAmountToMinorUnits("0.1", "EUR"))
        assertEquals(-1250L, parseAmountToMinorUnits("-12.50", "EUR"))
    }

    @Test
    fun parsesZeroDecimalCurrencies() {
        assertEquals(1000L, parseAmountToMinorUnits("1000", "JPY"))
        assertEquals(-5L, parseAmountToMinorUnits("-5", "ISK"))
    }

    @Test
    fun parsesThreeDecimalCurrencies() {
        assertEquals(1234L, parseAmountToMinorUnits("1.234", "KWD"))
        assertEquals(1200L, parseAmountToMinorUnits("1.2", "KWD"))
    }

    @Test
    fun rejectsTooManyDecimalsAndGarbage() {
        assertFailsWith<IllegalArgumentException> { parseAmountToMinorUnits("1.234", "EUR") }
        assertFailsWith<IllegalArgumentException> { parseAmountToMinorUnits("12,50", "EUR") }
        assertFailsWith<IllegalArgumentException> { parseAmountToMinorUnits("", "EUR") }
        assertFailsWith<IllegalArgumentException> { parseAmountToMinorUnits("abc", "EUR") }
    }
}

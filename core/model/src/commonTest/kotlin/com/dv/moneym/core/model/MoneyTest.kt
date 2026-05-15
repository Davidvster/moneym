package com.dv.moneym.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MoneyTest {
    private val eur = CurrencyCode("EUR")

    @Test
    fun additionProducesCorrectMinorUnits() {
        val result = Money(100, eur) + Money(250, eur)
        assertEquals(350, result.minorUnits)
    }

    @Test
    fun unaryMinusNegatesMinorUnits() {
        assertEquals(-100, (-Money(100, eur)).minorUnits)
    }

    @Test
    fun zeroIsNeitherPositiveNorNegative() {
        val zero = Money.zero(eur)
        assertTrue(zero.isZero)
        assertTrue(!zero.isPositive)
        assertTrue(!zero.isNegative)
    }
}

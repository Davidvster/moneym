package com.dv.moneym.data.walletsync

import com.dv.moneym.core.model.SyncDirection
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NotificationParserTest {

    private val parser = NotificationParser()
    private val today = LocalDate(2026, 6, 13)

    private fun parse(
        title: String? = null,
        text: String? = null,
        pkg: String = "com.google.android.apps.walletnfcrel",
        appLabel: String? = "Google Wallet",
        defaultCurrency: String = "EUR",
    ) = parser.parse(pkg, appLabel, title, text, postTimeMs = 1000L, today = today, defaultCurrency = defaultCurrency)

    @Test
    fun parsesGooglePayExpenseWithSymbolAndMerchant() {
        val s = parse(title = "Google Pay", text = "You paid €12.34 to Starbucks")
        assertTrue(s != null)
        assertEquals(1234L, s.amountMinor)
        assertEquals("EUR", s.currency)
        assertEquals(SyncDirection.DEBIT, s.direction)
        assertEquals("Starbucks", s.counterparty)
        assertEquals(today, s.date)
    }

    @Test
    fun parsesGooglePayExpenseWithSymbolAndMerchantActual() {
        val s = parse(title = "1040 Starbucks", text = "€32.34 with the card bank **1234")
        assertTrue(s != null)
            assertEquals(3234L, s.amountMinor)
        assertEquals("EUR", s.currency)
        assertEquals(SyncDirection.DEBIT, s.direction)
        assertEquals("1040 Starbucks", s.description)
        assertEquals(today, s.date)
    }

    @Test
    fun parsesDollarAmount() {
        val s = parse(text = "Paid $5.00 at Target")
        assertEquals(500L, s?.amountMinor)
        assertEquals("USD", s?.currency)
        assertEquals("Target", s?.counterparty)
    }

    @Test
    fun parsesEuropeanDecimalComma() {
        val s = parse(text = "Du hast 12,34 € an Bäckerei gezahlt")
        assertEquals(1234L, s?.amountMinor)
        assertEquals("EUR", s?.currency)
    }

    @Test
    fun parsesThousandsGrouping() {
        val s = parse(text = "Payment of ₹1,234.56 to Amazon")
        assertEquals(123456L, s?.amountMinor)
        assertEquals("INR", s?.currency)
    }

    @Test
    fun refundIsCredit() {
        val s = parse(text = "Refund of €20.00 received from Shop")
        assertEquals(SyncDirection.CREDIT, s?.direction)
        assertEquals(2000L, s?.amountMinor)
    }

    @Test
    fun fallsBackToDefaultCurrencyWhenNoSymbol() {
        val s = parse(text = "Paid 9.99 at Kiosk", defaultCurrency = "SEK")
        assertEquals(999L, s?.amountMinor)
        assertEquals("SEK", s?.currency)
    }

    @Test
    fun returnsNullWhenNoAmount() {
        assertNull(parse(title = "Google Wallet", text = "Your pass was added"))
    }

    @Test
    fun returnsNullForBlankNotification() {
        assertNull(parse(title = null, text = null))
    }

    @Test
    fun stableExternalIdDedupesSamePurchaseSameDay() {
        val a = parse(text = "You paid €12.34 to Starbucks")
        val b = parse(text = "You paid €12.34 to Starbucks")
        assertEquals(a?.externalId, b?.externalId)
    }

    @Test
    fun isoCodeAfterAmountIsDetected() {
        val s = parse(text = "Paid 15.00 USD at Shop")
        assertEquals("USD", s?.currency)
        assertEquals(1500L, s?.amountMinor)
    }
}

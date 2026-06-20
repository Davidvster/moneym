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
    ) = parser.parse(
        pkg,
        appLabel,
        title,
        text,
        postTimeMs = 1000L,
        today = today,
        defaultCurrency = defaultCurrency
    )

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

    @Test
    fun testNuBankOne() {
        val s = parse(
            pkg = "com.nu.production",
            title = "Transferência recebida",
            text = "Você recebeu uma transferência de R\$ 410,00 de PREC ASD S.A.."
        )
        assertTrue(s != null)
        assertEquals(41000L, s.amountMinor)
        assertEquals("BRL", s.currency)
        assertEquals(SyncDirection.CREDIT, s.direction)
        assertEquals("PREC ASD S.A.", s.description)
        assertEquals(today, s.date)
    }

    @Test
    fun testNuBankTwo() {
        val s = parse(
            pkg = "com.nu.production",
            title = "Compra no crédito aprovada",
            text = "Compra de R\$ 16,00 APROVADA em BELAEXPRESS para o cartão com final 1111."
        )
        assertTrue(s != null)
        assertEquals(1600L, s.amountMinor)
        assertEquals("BRL", s.currency)
        assertEquals(SyncDirection.DEBIT, s.direction)
        assertEquals("BELAEXPRESS", s.description)
        assertEquals(today, s.date)
    }

    @Test
    fun testNuBankThree() {
        val s = parse(
            pkg = "com.nu.production",
            title = "Compra no crédito aprovada",
            text = "Compra de R\$ 124,93 APROVADA em BIG MAIS SUPERMERCADOS para o cartão com final 1111."
        )
        assertTrue(s != null)
        assertEquals(12493L, s.amountMinor)
        assertEquals("BRL", s.currency)
        assertEquals(SyncDirection.DEBIT, s.direction)
        assertEquals("BIG MAIS SUPERMERCADOS", s.description)
        assertEquals(today, s.date)
    }

    @Test
    fun testNuBankEmpty() {
        val s = parse(
            pkg = "com.nu.production",
            title = "Ative o modo torcida no celular",
            text = "Não perca nenhum lance dos jogos, com dados extras para YouTube e redes sociais no NuCel, o chip de celular do Nubank."
        )
        assertEquals(s, null)
    }

    @Test
    fun testRevolutEmpty() {
        val s = parse(
            pkg = "com.revolut.revolut",
            title = "Move your crypto to Revolut",
            text = "EU customers should move to a licensed platform by 1 July. Switch to Revolut, we're MiCA-licensed and here to stay."
        )
        assertEquals(s, null)
    }

    @Test
    fun testRevolutPromotion() {
        val s = parse(
            pkg = "com.revolut.revolut",
            title = "Your Metal free trial",
            text = "Enjoy benefits worth €2,400, on us for 2 months if you join by 23/06/2026. T&Cs apply"
        )
        assertTrue(s != null)
        assertEquals(240000, s.amountMinor)
        assertEquals("EUR", s.currency)
        assertEquals(SyncDirection.DEBIT, s.direction)
        assertEquals(today, s.date)
    }
}

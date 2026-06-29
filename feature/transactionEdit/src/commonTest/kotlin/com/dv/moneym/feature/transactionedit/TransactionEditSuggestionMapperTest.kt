package com.dv.moneym.feature.transactionedit

import com.dv.moneym.core.model.TransactionType
import kotlin.test.Test
import kotlin.test.assertEquals

class TransactionEditSuggestionMapperTest {

    @Test
    fun mapsSuggestionFieldsToDraft() {
        val draft = transactionEditDraftFromSuggestion(
            amountMinor = 1234,
            currency = "EUR",
            isExpense = true,
            dateIso = "2026-05-10",
            description = "Coffee",
            counterparty = "Cafe",
            targetAccountId = 7,
            categoryId = 3,
            sourceLabel = "Wallet",
            suggestionSourceType = "WALLET",
            suggestionId = 42,
            externalId = "wallet:1",
        )

        assertEquals(1234, draft.amountMinor)
        assertEquals("EUR", draft.currency)
        assertEquals(TransactionType.EXPENSE, draft.type)
        assertEquals("2026-05-10", draft.dateIso)
        assertEquals("Coffee", draft.note)
        assertEquals(7, draft.accountId)
        assertEquals(3, draft.categoryId)
        assertEquals("Wallet", draft.suggestionSourceName)
        assertEquals("WALLET", draft.suggestionSourceType)
        assertEquals(42, draft.suggestionId)
        assertEquals("wallet:1", draft.externalId)
    }
}

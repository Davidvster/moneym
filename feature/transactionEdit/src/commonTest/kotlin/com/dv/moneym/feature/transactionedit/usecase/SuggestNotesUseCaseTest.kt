package com.dv.moneym.feature.transactionedit.usecase

import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.Money
import com.dv.moneym.core.model.Transaction
import com.dv.moneym.core.model.TransactionId
import com.dv.moneym.core.model.TransactionType
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class SuggestNotesUseCaseTest {

    private val epoch = Instant.fromEpochMilliseconds(0)
    private val today = LocalDate(2026, 6, 1)
    private val useCase = SuggestNotesUseCase()

    private fun txn(id: Long, note: String, date: LocalDate) = Transaction(
        id = TransactionId(id),
        type = TransactionType.EXPENSE,
        amount = Money(100, CurrencyCode("EUR")),
        occurredOn = date,
        note = note,
        categoryId = CategoryId(1),
        accountId = AccountId(1),
        createdAt = epoch,
        updatedAt = epoch,
    )

    @Test
    fun recent_single_use_outranks_old_frequent_use() {
        val recent = listOf(txn(1, "Coffee shop", LocalDate(2026, 5, 31)))
        val oldFrequent = (2L..6L).map { txn(it, "Coffee bar", LocalDate(2025, 6, 1)) }

        val result = useCase(recent + oldFrequent, "cof", today)

        assertEquals(listOf("Coffee shop", "Coffee bar"), result)
    }

    @Test
    fun blank_query_returns_empty() {
        val result = useCase(listOf(txn(1, "Coffee", today)), "  ", today)
        assertEquals(emptyList(), result)
    }
}

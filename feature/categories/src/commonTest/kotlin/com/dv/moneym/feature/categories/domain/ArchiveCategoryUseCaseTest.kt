package com.dv.moneym.feature.categories.domain

import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.Icon
import com.dv.moneym.core.model.Money
import com.dv.moneym.core.model.Transaction
import com.dv.moneym.core.model.TransactionId
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.testing.FakeCategoryRepository
import com.dv.moneym.core.testing.FakeTransactionRepository
import com.dv.moneym.core.testing.runTestWithDispatchers
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class ArchiveCategoryUseCaseTest {

    private val epoch = Instant.fromEpochMilliseconds(0)
    private val catRepo = FakeCategoryRepository()
    private val txnRepo = FakeTransactionRepository()
    private val useCase = ArchiveCategoryUseCase(catRepo, txnRepo)

    private fun category(id: Long, archived: Boolean = false) = Category(
        id = CategoryId(id),
        name = "Food",
        iconKey = Icon.Basket.key,
        colorHex = "#112233",
        isUserCreated = true,
        archived = archived,
        createdAt = epoch,
        updatedAt = epoch,
    )

    private fun expense(id: Long, categoryId: Long) = Transaction(
        id = TransactionId(id),
        type = TransactionType.EXPENSE,
        amount = Money(1000, CurrencyCode("EUR")),
        occurredOn = LocalDate(2026, 5, 1),
        note = null,
        categoryId = CategoryId(categoryId),
        accountId = AccountId(1),
        createdAt = epoch,
        updatedAt = epoch,
    )

    @Test
    fun missing_category_returns_not_found() = runTestWithDispatchers {
        val result = useCase(CategoryId(99))
        assertEquals(ArchiveResult.NotFound, result)
    }

    @Test
    fun category_with_transactions_is_archived_not_deleted() = runTestWithDispatchers {
        catRepo.addAll(listOf(category(1)))
        txnRepo.addAll(listOf(expense(10, 1)))

        val result = useCase(CategoryId(1))
        assertEquals(ArchiveResult.Archived, result)
        val stored = catRepo.categories.first { it.id == CategoryId(1) }
        assertTrue(stored.archived)
    }

    @Test
    fun category_without_transactions_is_deleted() = runTestWithDispatchers {
        catRepo.addAll(listOf(category(1)))

        val result = useCase(CategoryId(1))
        assertEquals(ArchiveResult.Deleted, result)
        assertTrue(catRepo.categories.none { it.id == CategoryId(1) })
    }

    @Test
    fun transactions_of_other_categories_do_not_keep_category_alive() = runTestWithDispatchers {
        catRepo.addAll(listOf(category(1)))
        txnRepo.addAll(listOf(expense(10, 2)))

        val result = useCase(CategoryId(1))
        assertEquals(ArchiveResult.Deleted, result)
    }
}

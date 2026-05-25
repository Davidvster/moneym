package com.dv.moneym.data.transactions

import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.EndCondition
import com.dv.moneym.core.model.Money
import com.dv.moneym.core.model.MonthlyDayKind
import com.dv.moneym.core.model.RecurrenceRule
import com.dv.moneym.core.model.RecurringTransaction
import com.dv.moneym.core.model.RecurringTransactionId
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.testing.FakeRecurringTransactionRepository
import com.dv.moneym.core.testing.FakeTransactionRepository
import com.dv.moneym.core.testing.FixedClock
import com.dv.moneym.core.testing.runTestWithDispatchers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

class MaterializeRecurringTransactionsUseCaseTest {

    private val eur = CurrencyCode("EUR")

    private fun rule(
        id: Long = 1,
        amount: Long = 100,
        startDate: LocalDate,
        rule: RecurrenceRule,
        end: EndCondition = EndCondition.Unlimited,
        cursor: LocalDate? = null,
    ): RecurringTransaction {
        val epoch = Instant.fromEpochMilliseconds(0)
        return RecurringTransaction(
            id = RecurringTransactionId(id),
            type = TransactionType.EXPENSE,
            amount = Money(amount, eur),
            note = null,
            categoryId = CategoryId(1),
            accountId = AccountId(1),
            paymentModeId = null,
            startDate = startDate,
            rule = rule,
            endCondition = end,
            lastMaterializedDate = cursor,
            createdAt = epoch,
            updatedAt = epoch,
        )
    }

    private fun clockAt(year: Int, month: Int, day: Int): FixedClock {
        val instant = LocalDateTime(year, month, day, 12, 0)
            .toInstant(TimeZone.currentSystemDefault())
        return FixedClock(instant)
    }

    @Test
    fun freshRuleMaterializesFirstOccurrence() = runTestWithDispatchers {
        val txRepo = FakeTransactionRepository()
        val recRepo = FakeRecurringTransactionRepository()
        val clock = clockAt(2026, 5, 20)
        recRepo.addAll(
            listOf(
                rule(
                    startDate = LocalDate(2026, 5, 15),
                    rule = RecurrenceRule.Monthly(1, MonthlyDayKind.OnDay(15)),
                )
            )
        )

        MaterializeRecurringTransactionsUseCase(recRepo, txRepo, clock).invoke()

        val txs = txRepo.transactions
        assertEquals(1, txs.size)
        assertEquals(LocalDate(2026, 5, 15), txs[0].occurredOn)
        assertEquals(RecurringTransactionId(1), txs[0].recurringId)
        assertEquals(LocalDate(2026, 5, 15), recRepo.rules.first().lastMaterializedDate)
    }

    @Test
    fun catchUpInsertsAllMissedOccurrences() = runTestWithDispatchers {
        val txRepo = FakeTransactionRepository()
        val recRepo = FakeRecurringTransactionRepository()
        val clock = clockAt(2026, 5, 20)
        recRepo.addAll(
            listOf(
                rule(
                    startDate = LocalDate(2026, 2, 15),
                    rule = RecurrenceRule.Monthly(1, MonthlyDayKind.OnDay(15)),
                    cursor = LocalDate(2026, 2, 15),
                )
            )
        )
        // simulate prior materialization
        txRepo.upsert(
            com.dv.moneym.core.model.Transaction(
                id = com.dv.moneym.core.model.UNSAVED_TRANSACTION_ID,
                type = TransactionType.EXPENSE,
                amount = Money(100, eur),
                occurredOn = LocalDate(2026, 2, 15),
                note = null,
                categoryId = CategoryId(1),
                accountId = AccountId(1),
                createdAt = Instant.fromEpochMilliseconds(0),
                updatedAt = Instant.fromEpochMilliseconds(0),
                recurringId = RecurringTransactionId(1),
            )
        )

        MaterializeRecurringTransactionsUseCase(recRepo, txRepo, clock).invoke()

        val dates = txRepo.transactions.map { it.occurredOn }.sorted()
        assertEquals(
            listOf(
                LocalDate(2026, 2, 15),
                LocalDate(2026, 3, 15),
                LocalDate(2026, 4, 15),
                LocalDate(2026, 5, 15),
            ),
            dates,
        )
        assertEquals(LocalDate(2026, 5, 15), recRepo.rules.first().lastMaterializedDate)
    }

    @Test
    fun countConditionExhaustsThenNoOps() = runTestWithDispatchers {
        val txRepo = FakeTransactionRepository()
        val recRepo = FakeRecurringTransactionRepository()
        val clock = clockAt(2026, 5, 10)
        recRepo.addAll(
            listOf(
                rule(
                    startDate = LocalDate(2026, 5, 1),
                    rule = RecurrenceRule.Daily(1),
                    end = EndCondition.Count(3),
                )
            )
        )

        val useCase = MaterializeRecurringTransactionsUseCase(recRepo, txRepo, clock)
        useCase.invoke()
        assertEquals(3, txRepo.transactions.size)

        useCase.invoke()
        assertEquals(3, txRepo.transactions.size) // no duplicates
    }

    @Test
    fun secondInvocationDoesNotDuplicate() = runTestWithDispatchers {
        val txRepo = FakeTransactionRepository()
        val recRepo = FakeRecurringTransactionRepository()
        val clock = clockAt(2026, 5, 20)
        recRepo.addAll(
            listOf(
                rule(
                    startDate = LocalDate(2026, 5, 15),
                    rule = RecurrenceRule.Monthly(1, MonthlyDayKind.OnDay(15)),
                )
            )
        )

        val useCase = MaterializeRecurringTransactionsUseCase(recRepo, txRepo, clock)
        useCase.invoke()
        useCase.invoke()

        assertEquals(1, txRepo.transactions.size)
    }

    @Test
    fun futureStartDateMaterializesNothing() = runTestWithDispatchers {
        val txRepo = FakeTransactionRepository()
        val recRepo = FakeRecurringTransactionRepository()
        val clock = clockAt(2026, 5, 10)
        recRepo.addAll(
            listOf(
                rule(
                    startDate = LocalDate(2026, 6, 1),
                    rule = RecurrenceRule.Monthly(1, MonthlyDayKind.OnDay(1)),
                )
            )
        )

        MaterializeRecurringTransactionsUseCase(recRepo, txRepo, clock).invoke()

        assertTrue(txRepo.transactions.isEmpty())
        assertEquals(null, recRepo.rules.first().lastMaterializedDate)
    }
}

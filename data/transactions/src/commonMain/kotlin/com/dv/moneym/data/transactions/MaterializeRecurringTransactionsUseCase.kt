package com.dv.moneym.data.transactions

import com.dv.moneym.core.common.AppClock
import com.dv.moneym.core.model.Transaction
import com.dv.moneym.core.model.UNSAVED_TRANSACTION_ID
import com.dv.moneym.data.transactions.recurrence.RecurrenceMath
import kotlinx.coroutines.flow.first

class MaterializeRecurringTransactionsUseCase(
    private val recurringRepo: RecurringTransactionRepository,
    private val transactionRepo: TransactionRepository,
    private val clock: AppClock,
) {
    suspend operator fun invoke() {
        val rules = recurringRepo.observeAll().first()
        if (rules.isEmpty()) return
        val today = clock.today()
        val now = clock.now()
        for (rule in rules) {
            val already = transactionRepo.countByRecurringId(rule.id)
            val due = RecurrenceMath.materializeDue(
                rule = rule.rule,
                endCondition = rule.endCondition,
                startDate = rule.startDate,
                cursor = rule.lastMaterializedDate,
                today = today,
                alreadyMaterializedCount = already,
            )
            if (due.isEmpty()) continue
            for (date in due) {
                transactionRepo.upsert(
                    Transaction(
                        id = UNSAVED_TRANSACTION_ID,
                        type = rule.type,
                        amount = rule.amount,
                        occurredOn = date,
                        note = rule.note,
                        categoryId = rule.categoryId,
                        accountId = rule.accountId,
                        createdAt = now,
                        updatedAt = now,
                        paymentModeId = rule.paymentModeId,
                        recurringId = rule.id,
                    )
                )
            }
            recurringRepo.updateCursor(rule.id, due.last())
        }
    }
}

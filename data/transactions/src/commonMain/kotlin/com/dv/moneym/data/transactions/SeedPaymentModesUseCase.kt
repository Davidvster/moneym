package com.dv.moneym.data.transactions

import com.dv.moneym.core.common.DispatcherProvider
import com.dv.moneym.data.transactions.db.TransactionsDatabase
import kotlinx.coroutines.withContext

class SeedPaymentModesUseCase(
    private val db: TransactionsDatabase,
    private val dispatchers: DispatcherProvider,
) {
    suspend operator fun invoke() = withContext(dispatchers.io) {
        val count = db.paymentModeQueries.countAll().executeAsOne()
        if (count == 0L) {
            val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
            db.paymentModeQueries.insert(name = "Cash", created_at = now, updated_at = now)
            db.paymentModeQueries.insert(name = "Card", created_at = now, updated_at = now)
            db.paymentModeQueries.insert(name = "Transfer", created_at = now, updated_at = now)
        }
    }
}

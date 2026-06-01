package com.dv.moneym.data.transactions

import com.dv.moneym.data.transactions.db.PaymentModeEntity
import com.dv.moneym.data.transactions.db.TransactionsRoomDatabase
import kotlin.time.Clock

class SeedPaymentModesUseCase(
    private val db: TransactionsRoomDatabase,
) {
    suspend operator fun invoke() {
        val dao = db.paymentModeDao()
        if (dao.countAll() == 0L) {
            val now = Clock.System.now().toEpochMilliseconds()
            dao.insert(PaymentModeEntity(name = "Cash",     createdAt = now, updatedAt = now, syncId = "seed-paymentmode-cash"))
            dao.insert(PaymentModeEntity(name = "Card",     createdAt = now, updatedAt = now, syncId = "seed-paymentmode-card"))
            dao.insert(PaymentModeEntity(name = "Transfer", createdAt = now, updatedAt = now, syncId = "seed-paymentmode-transfer"))
        }
    }
}

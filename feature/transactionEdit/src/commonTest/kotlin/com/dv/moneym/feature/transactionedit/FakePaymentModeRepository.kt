package com.dv.moneym.feature.transactionedit

import com.dv.moneym.core.model.PaymentMode
import com.dv.moneym.core.model.PaymentModeId
import com.dv.moneym.data.transactions.PaymentModeRepository
import com.dv.moneym.data.transactions.PaymentModeSyncRow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.time.Instant

class FakePaymentModeRepository(
    initial: List<PaymentMode> = emptyList(),
) : PaymentModeRepository {

    private val _modes = MutableStateFlow(initial)
    private var nextId = (initial.maxOfOrNull { it.id.value } ?: 0L) + 1

    val modes: List<PaymentMode> get() = _modes.value

    override fun observeAll(): Flow<List<PaymentMode>> = _modes.asStateFlow()

    override suspend fun getById(id: PaymentModeId): PaymentMode? =
        _modes.value.firstOrNull { it.id == id }

    override suspend fun create(name: String) {
        val id = PaymentModeId(nextId++)
        _modes.update { it + PaymentMode(id, name, Instant.DISTANT_PAST, Instant.DISTANT_PAST) }
    }

    override suspend fun rename(id: PaymentModeId, name: String) {
        _modes.update { list -> list.map { if (it.id == id) it.copy(name = name) else it } }
    }

    override suspend fun delete(id: PaymentModeId) {
        _modes.update { list -> list.filterNot { it.id == id } }
    }

    override suspend fun markDeletedBySyncId(syncId: String, now: Long) {}
    override suspend fun reviveBySyncId(syncId: String, now: Long) {}
    override suspend fun exportForSync(): List<PaymentModeSyncRow> = emptyList()
    override suspend fun upsertFromSync(row: PaymentModeSyncRow): Long = 0L
}

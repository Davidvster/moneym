package com.dv.moneym.data.sync

import com.dv.moneym.core.model.PaymentMode
import com.dv.moneym.core.model.PaymentModeId
import com.dv.moneym.data.transactions.PaymentModeRepository
import com.dv.moneym.data.transactions.PaymentModeSyncRow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakePaymentModeRepository : PaymentModeRepository {
    private val _modes = MutableStateFlow<List<PaymentMode>>(emptyList())
    private var nextId = 1L

    fun addAll(modes: List<PaymentMode>) = _modes.update { it + modes }

    override fun observeAll(): Flow<List<PaymentMode>> = _modes

    override suspend fun getById(id: PaymentModeId): PaymentMode? =
        _modes.value.find { it.id == id }

    override suspend fun create(name: String) {
        // not needed for sync export tests
    }

    override suspend fun rename(id: PaymentModeId, name: String) {
        _modes.update { list -> list.map { if (it.id == id) it.copy(name = name) else it } }
    }

    override suspend fun delete(id: PaymentModeId) {
        _modes.update { list -> list.filter { it.id != id } }
    }

    override suspend fun exportForSync(): List<PaymentModeSyncRow> =
        _modes.value.map { m ->
            PaymentModeSyncRow(
                id = m.id.value,
                syncId = "sync-pm-${m.id.value}",
                name = m.name,
                deleted = false,
                createdAt = m.createdAt.toEpochMilliseconds(),
                updatedAt = m.updatedAt.toEpochMilliseconds(),
            )
        }
}

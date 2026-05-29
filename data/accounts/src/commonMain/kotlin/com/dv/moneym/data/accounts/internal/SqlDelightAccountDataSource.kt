package com.dv.moneym.data.accounts.internal

import com.dv.moneym.data.accounts.db.AccountEntity
import com.dv.moneym.data.accounts.db.AccountsRoomDatabase
import kotlinx.coroutines.flow.Flow

internal class SqlDelightAccountDataSource(
    private val db: AccountsRoomDatabase,
) : AccountLocalDataSource {

    private val dao get() = db.accountDao()

    override fun observeAll(): Flow<List<AccountEntity>> = dao.selectAll()

    override fun observeDefault(): Flow<AccountEntity?> = dao.selectDefault()

    override suspend fun getById(id: Long): AccountEntity? = dao.selectById(id)

    override suspend fun count(): Long = dao.countAll()

    override suspend fun insert(
        name: String, type: String, currency: String,
        isDefault: Boolean, createdAt: Long, updatedAt: Long,
        colorHex: String?,
    ): Long = dao.insert(
        AccountEntity(
            name = name,
            type = type,
            currency = currency,
            isDefault = isDefault,
            createdAt = createdAt,
            updatedAt = updatedAt,
            colorHex = colorHex,
        )
    )

    override suspend fun update(
        id: Long, name: String, type: String, currency: String,
        isDefault: Boolean, archived: Boolean, updatedAt: Long,
        colorHex: String?,
    ) {
        val existing = dao.selectById(id) ?: return
        dao.update(
            existing.copy(
                name = name,
                type = type,
                currency = currency,
                isDefault = isDefault,
                archived = archived,
                updatedAt = updatedAt,
                colorHex = colorHex
            )
        )
    }

    override suspend fun delete(id: Long) = dao.deleteById(id)

    override suspend fun deleteAll() = dao.deleteAll()
}

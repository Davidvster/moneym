package com.dv.moneym.data.accounts.internal

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.dv.moneym.core.common.DispatcherProvider
import com.dv.moneym.data.accounts.Account
import com.dv.moneym.data.accounts.db.AccountsDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

internal class SqlDelightAccountDataSource(
    private val db: AccountsDatabase,
    private val dispatchers: DispatcherProvider,
) : AccountLocalDataSource {

    private val q get() = db.accountQueries

    override fun observeAll(): Flow<List<Account>> =
        q.selectAll().asFlow().mapToList(dispatchers.io)

    override fun observeDefault(): Flow<Account?> =
        q.selectDefault().asFlow().mapToOneOrNull(dispatchers.io)

    override suspend fun getById(id: Long): Account? = withContext(dispatchers.io) {
        q.selectById(id).executeAsOneOrNull()
    }

    override suspend fun count(): Long = withContext(dispatchers.io) {
        q.countAll().executeAsOne()
    }

    override suspend fun insert(
        name: String, type: String, currency: String,
        isDefault: Boolean, createdAt: Long, updatedAt: Long,
    ): Long = withContext(dispatchers.io) {
        var id = 0L
        db.transaction {
            q.insert(name, type, currency, if (isDefault) 1L else 0L, createdAt, updatedAt)
            id = q.lastInsertId().executeAsOne()
        }
        id
    }

    override suspend fun update(
        id: Long, name: String, type: String, currency: String,
        isDefault: Boolean, archived: Boolean, updatedAt: Long,
    ) = withContext(dispatchers.io) {
        q.updateById(name, type, currency, if (isDefault) 1L else 0L, if (archived) 1L else 0L, updatedAt, id)
    }

    override suspend fun delete(id: Long) = withContext(dispatchers.io) {
        q.deleteById(id)
    }
}

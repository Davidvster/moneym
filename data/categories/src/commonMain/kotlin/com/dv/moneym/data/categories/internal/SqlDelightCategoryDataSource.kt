package com.dv.moneym.data.categories.internal

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.dv.moneym.core.common.DispatcherProvider
import com.dv.moneym.data.categories.Category
import com.dv.moneym.data.categories.db.CategoriesDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

internal class SqlDelightCategoryDataSource(
    private val db: CategoriesDatabase,
    private val dispatchers: DispatcherProvider,
) : CategoryLocalDataSource {

    private val q get() = db.categoryQueries

    override fun observeAll(): Flow<List<Category>> =
        q.selectAll().asFlow().mapToList(dispatchers.io)

    override fun observeActive(): Flow<List<Category>> =
        q.selectActive().asFlow().mapToList(dispatchers.io)

    override suspend fun getById(id: Long): Category? = withContext(dispatchers.io) {
        q.selectById(id).executeAsOneOrNull()
    }

    override suspend fun count(): Long = withContext(dispatchers.io) {
        q.countAll().executeAsOne()
    }

    override suspend fun insert(
        name: String, iconKey: String, colorHex: String,
        isUserCreated: Boolean, createdAt: Long, updatedAt: Long,
    ): Long = withContext(dispatchers.io) {
        var id = 0L
        db.transaction {
            q.insert(name, iconKey, colorHex, if (isUserCreated) 1L else 0L, createdAt, updatedAt)
            id = q.lastInsertId().executeAsOne()
        }
        id
    }

    override suspend fun update(
        id: Long, name: String, iconKey: String, colorHex: String,
        archived: Boolean, updatedAt: Long,
    ) = withContext(dispatchers.io) {
        q.updateById(name, iconKey, colorHex, if (archived) 1L else 0L, updatedAt, id)
    }

    override suspend fun delete(id: Long) = withContext(dispatchers.io) {
        q.deleteById(id)
    }
}

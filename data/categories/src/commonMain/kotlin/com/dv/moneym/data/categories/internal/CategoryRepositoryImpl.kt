package com.dv.moneym.data.categories.internal

import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.data.categories.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

internal class CategoryRepositoryImpl(
    private val dataSource: CategoryLocalDataSource,
) : CategoryRepository {

    override fun observeAll(): Flow<List<Category>> =
        dataSource.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observeActive(): Flow<List<Category>> =
        dataSource.observeActive().map { rows -> rows.map { it.toDomain() } }

    override suspend fun getById(id: CategoryId): Category? =
        dataSource.getById(id.value)?.toDomain()

    override suspend fun count(): Long = dataSource.count()

    override suspend fun insert(category: Category): CategoryId {
        val now = Clock.System.now().toEpochMilliseconds()
        val id = dataSource.insert(
            name = category.name,
            iconKey = category.iconKey,
            colorHex = category.colorHex,
            isUserCreated = category.isUserCreated,
            createdAt = now,
            updatedAt = now,
        )
        return CategoryId(id)
    }

    override suspend fun update(category: Category) {
        val now = Clock.System.now().toEpochMilliseconds()
        dataSource.update(
            id = category.id.value,
            name = category.name,
            iconKey = category.iconKey,
            colorHex = category.colorHex,
            archived = category.archived,
            updatedAt = now,
        )
    }

    override suspend fun delete(id: CategoryId) = dataSource.delete(id.value)
}

package com.dv.moneym.data.categories.internal

import com.dv.moneym.data.categories.db.CategoriesRoomDatabase
import com.dv.moneym.data.categories.db.CategoryEntity
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow

internal class SqlDelightCategoryDataSource(
    private val db: CategoriesRoomDatabase,
) : CategoryLocalDataSource {

    private val dao get() = db.categoryDao()

    override fun observeAll(): Flow<List<CategoryEntity>> = dao.selectAll()

    override fun observeActive(): Flow<List<CategoryEntity>> = dao.selectActive()

    override suspend fun getById(id: Long): CategoryEntity? = dao.selectById(id)

    override suspend fun count(): Long = dao.countAll()

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun insert(
        name: String, iconKey: String, colorHex: String,
        isUserCreated: Boolean, createdAt: Long, updatedAt: Long,
        categoryType: String,
    ): Long = dao.insert(
        CategoryEntity(
            name = name,
            iconKey = iconKey,
            colorHex = colorHex,
            isUserCreated = isUserCreated,
            createdAt = createdAt,
            updatedAt = updatedAt,
            categoryType = categoryType,
            syncId = Uuid.random().toString(),
        )
    )

    override suspend fun update(
        id: Long, name: String, iconKey: String, colorHex: String,
        archived: Boolean, updatedAt: Long,
    ) {
        val existing = dao.selectById(id) ?: return
        dao.update(existing.copy(name = name, iconKey = iconKey, colorHex = colorHex, archived = archived, updatedAt = updatedAt))
    }

    override suspend fun delete(id: Long) = dao.deleteById(id)

    override suspend fun deleteAll() = dao.deleteAll()
}

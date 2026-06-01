package com.dv.moneym.data.categories.internal

import com.dv.moneym.data.categories.CategorySyncRow
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

    override suspend fun exportForSync(): List<CategoryEntity> = dao.selectAllForSync()

    override suspend fun upsertFromSync(row: CategorySyncRow): Long {
        val syncId = requireNotNull(row.syncId) { "upsertFromSync requires a non-null syncId" }
        val existing = dao.selectBySyncId(syncId)
        return if (existing == null) {
            dao.insert(
                CategoryEntity(
                    id = 0,
                    name = row.name,
                    iconKey = row.iconKey,
                    colorHex = row.colorHex,
                    isUserCreated = row.isUserCreated,
                    archived = row.archived,
                    createdAt = row.createdAt,
                    updatedAt = row.updatedAt,
                    categoryType = row.categoryType,
                    syncId = syncId,
                    deleted = row.deleted,
                )
            )
        } else {
            dao.update(
                existing.copy(
                    name = row.name,
                    iconKey = row.iconKey,
                    colorHex = row.colorHex,
                    isUserCreated = row.isUserCreated,
                    archived = row.archived,
                    categoryType = row.categoryType,
                    updatedAt = row.updatedAt,
                    deleted = row.deleted,
                )
            )
            existing.id
        }
    }
}

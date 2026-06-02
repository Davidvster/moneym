package com.dv.moneym.data.categories.internal

import com.dv.moneym.data.categories.CategorySyncRow
import com.dv.moneym.data.categories.db.CategoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
internal class FakeCategoryLocalDataSource : CategoryLocalDataSource {

    val rows = MutableStateFlow<List<CategoryEntity>>(emptyList())
    private var nextId = 1L

    private fun mutate(block: (List<CategoryEntity>) -> List<CategoryEntity>) {
        rows.value = block(rows.value)
    }

    override fun observeAll(): Flow<List<CategoryEntity>> =
        rows.map { list -> list.filter { !it.deleted }.sortedBy { it.name } }

    override fun observeActive(): Flow<List<CategoryEntity>> =
        rows.map { list -> list.filter { !it.deleted && !it.archived }.sortedBy { it.name } }

    override suspend fun getById(id: Long): CategoryEntity? = rows.value.firstOrNull { it.id == id }

    override suspend fun count(): Long = rows.value.count { !it.deleted }.toLong()

    override suspend fun insert(
        name: String, iconKey: String, colorHex: String,
        isUserCreated: Boolean, createdAt: Long, updatedAt: Long,
        categoryType: String,
    ): Long {
        val id = nextId++
        mutate {
            it + CategoryEntity(
                id = id,
                name = name,
                iconKey = iconKey,
                colorHex = colorHex,
                isUserCreated = isUserCreated,
                createdAt = createdAt,
                updatedAt = updatedAt,
                categoryType = categoryType,
                syncId = Uuid.random().toString(),
            )
        }
        return id
    }

    override suspend fun update(
        id: Long, name: String, iconKey: String, colorHex: String,
        archived: Boolean, updatedAt: Long,
    ) {
        mutate { list ->
            list.map {
                if (it.id == id) it.copy(
                    name = name,
                    iconKey = iconKey,
                    colorHex = colorHex,
                    archived = archived,
                    updatedAt = updatedAt,
                ) else it
            }
        }
    }

    override suspend fun softDelete(id: Long, now: Long) {
        mutate { list -> list.map { if (it.id == id) it.copy(deleted = true, updatedAt = now) else it } }
    }

    override suspend fun markDeletedBySyncId(syncId: String, now: Long) {
        mutate { list -> list.map { if (it.syncId == syncId) it.copy(deleted = true, updatedAt = now) else it } }
    }

    override suspend fun reviveBySyncId(syncId: String, now: Long) {
        mutate { list -> list.map { if (it.syncId == syncId) it.copy(updatedAt = now) else it } }
    }

    override suspend fun deleteAll() {
        rows.value = emptyList()
    }

    override suspend fun exportForSync(): List<CategoryEntity> = rows.value

    override suspend fun upsertFromSync(row: CategorySyncRow): Long {
        val syncId = requireNotNull(row.syncId) { "upsertFromSync requires a non-null syncId" }
        val existing = rows.value.firstOrNull { it.syncId == syncId }
        return if (existing == null) {
            val id = nextId++
            mutate {
                it + CategoryEntity(
                    id = id,
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
            }
            id
        } else {
            mutate { list ->
                list.map {
                    if (it.syncId == syncId) it.copy(
                        name = row.name,
                        iconKey = row.iconKey,
                        colorHex = row.colorHex,
                        isUserCreated = row.isUserCreated,
                        archived = row.archived,
                        categoryType = row.categoryType,
                        updatedAt = row.updatedAt,
                        deleted = row.deleted,
                    ) else it
                }
            }
            existing.id
        }
    }
}

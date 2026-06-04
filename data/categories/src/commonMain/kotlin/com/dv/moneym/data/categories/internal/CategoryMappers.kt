package com.dv.moneym.data.categories.internal

import com.dv.moneym.core.model.Category as DomainCategory
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.data.categories.CategorySyncRow
import com.dv.moneym.data.categories.db.CategoryEntity
import kotlin.time.Instant

internal fun CategoryEntity.toDomain() = DomainCategory(
    id = CategoryId(id),
    name = name,
    iconKey = iconKey,
    colorHex = colorHex,
    isUserCreated = isUserCreated,
    archived = archived,
    createdAt = Instant.fromEpochMilliseconds(createdAt),
    updatedAt = Instant.fromEpochMilliseconds(updatedAt),
    type = if (categoryType == "INCOME") TransactionType.INCOME else TransactionType.EXPENSE,
    sortOrder = sortOrder,
)

internal fun CategoryEntity.toSyncRow() = CategorySyncRow(
    id = id,
    syncId = syncId,
    name = name,
    iconKey = iconKey,
    colorHex = colorHex,
    isUserCreated = isUserCreated,
    archived = archived,
    categoryType = categoryType,
    deleted = deleted,
    createdAt = createdAt,
    updatedAt = updatedAt,
    sortOrder = sortOrder,
)

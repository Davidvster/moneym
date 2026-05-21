package com.dv.moneym.data.categories.internal

import com.dv.moneym.core.model.Category as DomainCategory
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.TransactionType
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
)

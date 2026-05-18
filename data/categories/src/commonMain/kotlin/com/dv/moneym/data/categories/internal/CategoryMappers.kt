package com.dv.moneym.data.categories.internal

import com.dv.moneym.core.model.Category as DomainCategory
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.data.categories.Category as CategoryRow
import kotlin.time.Instant

internal fun CategoryRow.toDomain() = DomainCategory(
    id = CategoryId(id),
    name = name,
    iconKey = icon_key,
    colorHex = color_hex,
    isUserCreated = is_user_created != 0L,
    archived = archived != 0L,
    createdAt = Instant.fromEpochMilliseconds(created_at),
    updatedAt = Instant.fromEpochMilliseconds(updated_at),
    type = if (category_type == "INCOME") TransactionType.INCOME else TransactionType.EXPENSE,
)

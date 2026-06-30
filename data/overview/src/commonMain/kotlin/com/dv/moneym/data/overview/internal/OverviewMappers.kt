package com.dv.moneym.data.overview.internal

import com.dv.moneym.data.overview.OverviewAiWidget
import com.dv.moneym.data.overview.OverviewBlockId
import com.dv.moneym.data.overview.OverviewLayoutBlock
import com.dv.moneym.data.overview.db.OverviewAiWidgetEntity
import com.dv.moneym.data.overview.db.OverviewLayoutBlockEntity
import kotlin.time.Instant

internal fun OverviewLayoutBlockEntity.toDomain(): OverviewLayoutBlock = OverviewLayoutBlock(
    blockId = OverviewBlockId(blockId),
    sortOrder = sortOrder,
    visible = visible,
)

internal fun OverviewLayoutBlock.toEntity(): OverviewLayoutBlockEntity = OverviewLayoutBlockEntity(
    blockId = blockId.value,
    sortOrder = sortOrder,
    visible = visible,
)

internal fun OverviewAiWidgetEntity.toDomain(): OverviewAiWidget = OverviewAiWidget(
    id = id,
    title = title,
    prompt = prompt,
    a2uiJson = a2uiJson,
    enabled = enabled,
    sortOrder = sortOrder,
    createdAt = Instant.fromEpochMilliseconds(createdAt),
    updatedAt = Instant.fromEpochMilliseconds(updatedAt),
    lastGeneratedAt = lastGeneratedAt?.let(Instant::fromEpochMilliseconds),
    lastGenerationEngineId = lastGenerationEngineId,
)

internal fun OverviewAiWidget.toEntity(): OverviewAiWidgetEntity = OverviewAiWidgetEntity(
    id = id,
    title = title,
    prompt = prompt,
    a2uiJson = a2uiJson,
    enabled = enabled,
    sortOrder = sortOrder,
    createdAt = createdAt.toEpochMilliseconds(),
    updatedAt = updatedAt.toEpochMilliseconds(),
    lastGeneratedAt = lastGeneratedAt?.toEpochMilliseconds(),
    lastGenerationEngineId = lastGenerationEngineId,
)

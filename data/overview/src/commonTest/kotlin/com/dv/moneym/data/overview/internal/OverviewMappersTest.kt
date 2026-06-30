package com.dv.moneym.data.overview.internal

import com.dv.moneym.data.overview.OverviewAiWidget
import com.dv.moneym.data.overview.OverviewBlockId
import com.dv.moneym.data.overview.OverviewLayoutBlock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class OverviewMappersTest {
    private val createdAt = Instant.fromEpochMilliseconds(1000)
    private val updatedAt = Instant.fromEpochMilliseconds(2000)
    private val generatedAt = Instant.fromEpochMilliseconds(3000)

    @Test
    fun layoutBlockRoundTrip() {
        val block = OverviewLayoutBlock(
            blockId = OverviewBlockId("totals"),
            sortOrder = 1,
            visible = true,
        )

        assertEquals(block, block.toEntity().toDomain())
    }

    @Test
    fun aiWidgetRoundTrip() {
        val widget = OverviewAiWidget(
            id = 5,
            title = "Cash flow",
            prompt = "Show cash flow",
            a2uiJson = """{"type":"metric"}""",
            enabled = true,
            sortOrder = 2,
            createdAt = createdAt,
            updatedAt = updatedAt,
            lastGeneratedAt = generatedAt,
            lastGenerationEngineId = "local",
        )

        assertEquals(widget, widget.toEntity().toDomain())
    }

    @Test
    fun aiWidgetNullableGenerationFieldsRoundTrip() {
        val widget = OverviewAiWidget(
            id = 7,
            title = "Forecast",
            prompt = "Forecast spending",
            a2uiJson = "{}",
            enabled = false,
            sortOrder = 3,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

        val result = widget.toEntity().toDomain()

        assertNull(result.lastGeneratedAt)
        assertNull(result.lastGenerationEngineId)
        assertEquals(widget, result)
    }
}

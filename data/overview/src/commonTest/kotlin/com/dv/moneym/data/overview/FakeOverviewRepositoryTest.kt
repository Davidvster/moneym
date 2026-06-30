package com.dv.moneym.data.overview

import app.cash.turbine.test
import com.dv.moneym.core.testing.FakeOverviewRepository
import com.dv.moneym.core.testing.runTestWithDispatchers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.time.Instant

class FakeOverviewRepositoryTest {
    private val epoch = Instant.fromEpochMilliseconds(0)

    @Test
    fun mirrorsRepositoryContract() = runTestWithDispatchers {
        val repository = FakeOverviewRepository()
        repository.replaceLayout(
            listOf(
                OverviewLayoutBlock(OverviewBlockId("totals"), sortOrder = 0, visible = true),
            )
        )

        val id = repository.upsertAiWidget(
            OverviewAiWidget(
                title = "Widget",
                prompt = "Prompt",
                a2uiJson = "{}",
                enabled = true,
                sortOrder = 0,
                createdAt = epoch,
                updatedAt = epoch,
            )
        )
        repository.setAiWidgetEnabled(id, false)

        repository.observeLayoutPrefs().test {
            assertEquals("totals", awaitItem().blocks.first().blockId.value)
            cancelAndIgnoreRemainingEvents()
        }
        repository.observeAiWidgets().test {
            val widget = awaitItem().first()
            assertEquals(id, widget.id)
            assertFalse(widget.enabled)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

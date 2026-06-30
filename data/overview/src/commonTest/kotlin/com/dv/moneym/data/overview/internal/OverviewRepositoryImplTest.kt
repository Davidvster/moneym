package com.dv.moneym.data.overview.internal

import app.cash.turbine.test
import com.dv.moneym.core.testing.runTestWithDispatchers
import com.dv.moneym.data.overview.OverviewAiWidget
import com.dv.moneym.data.overview.OverviewBlockId
import com.dv.moneym.data.overview.OverviewLayoutBlock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

class OverviewRepositoryImplTest {
    private val dataSource = FakeOverviewLocalDataSource()
    private val repository = OverviewRepositoryImpl(dataSource)
    private val epoch = Instant.fromEpochMilliseconds(0)

    private fun widget(
        id: Long = 0,
        title: String = "Cash flow",
        sortOrder: Int = 0,
        enabled: Boolean = true,
    ) = OverviewAiWidget(
        id = id,
        title = title,
        prompt = "Prompt for $title",
        a2uiJson = "{}",
        enabled = enabled,
        sortOrder = sortOrder,
        createdAt = epoch,
        updatedAt = epoch,
    )

    @Test
    fun replaceLayoutEmitsSortedPrefs() = runTestWithDispatchers {
        repository.observeLayoutPrefs().test {
            assertTrue(awaitItem().blocks.isEmpty())

            repository.replaceLayout(
                listOf(
                    OverviewLayoutBlock(OverviewBlockId("monthly_spend"), sortOrder = 2, visible = false),
                    OverviewLayoutBlock(OverviewBlockId("totals"), sortOrder = 1, visible = true),
                )
            )

            val blocks = awaitItem().blocks
            assertEquals(listOf("totals", "monthly_spend"), blocks.map { it.blockId.value })
            assertFalse(blocks[1].visible)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun upsertDeleteAndToggleWidget() = runTestWithDispatchers {
        repository.observeAiWidgets().test {
            assertTrue(awaitItem().isEmpty())

            val id = repository.upsertAiWidget(widget(title = "One"))
            assertEquals(listOf("One"), awaitItem().map { it.title })

            repository.upsertAiWidget(widget(id = id, title = "Renamed", sortOrder = 3))
            assertEquals(listOf("Renamed"), awaitItem().map { it.title })

            repository.setAiWidgetEnabled(id, false)
            assertFalse(awaitItem().first().enabled)

            repository.deleteAiWidget(id)
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun replaceAiWidgetsDropsExistingRows() = runTestWithDispatchers {
        repository.upsertAiWidget(widget(title = "Old"))

        repository.observeAiWidgets().test {
            assertEquals(listOf("Old"), awaitItem().map { it.title })

            repository.replaceAiWidgets(listOf(widget(title = "New", sortOrder = 1)))

            val widgets = awaitItem()
            assertEquals(listOf("New"), widgets.map { it.title })
            assertEquals(1L, widgets.first().id)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

package com.dv.moneym.feature.settings.overview

import kotlin.test.Test
import kotlin.test.assertEquals

class OverviewSettingsReorderTest {

    @Test
    fun reordersUsingLazyListIndicesAfterHeader() {
        val blocks = (1..9).toList()

        val reordered = reorderOverviewBuiltInBlocks(
            blocks = blocks,
            fromLazyListIndex = 9,
            toLazyListIndex = 8,
        )

        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7, 9, 8), reordered)
    }

    @Test
    fun ignoresMovesOutsideBuiltInRows() {
        val blocks = (1..9).toList()

        assertEquals(
            blocks,
            reorderOverviewBuiltInBlocks(
                blocks = blocks,
                fromLazyListIndex = 0,
                toLazyListIndex = 1,
            ),
        )
        assertEquals(
            blocks,
            reorderOverviewBuiltInBlocks(
                blocks = blocks,
                fromLazyListIndex = 1,
                toLazyListIndex = 10,
            ),
        )
    }
}

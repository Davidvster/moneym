package com.dv.moneym.core.datastore

import app.cash.turbine.test
import com.dv.moneym.core.model.Density
import com.dv.moneym.core.model.IndicatorStyle
import com.dv.moneym.core.model.ThemeMode
import com.dv.moneym.core.model.TxDisplayPrefs
import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultAppSettingsRepositoryTest {

    private fun createRepo(): DefaultAppSettingsRepository {
        val settings = DefaultAppSettings(MapSettings())
        return DefaultAppSettingsRepository(settings)
    }

    @Test
    fun `observeThemeMode emits updated value after setThemeMode`() = runTest {
        val repo = createRepo()
        repo.observeThemeMode().test {
            assertEquals(ThemeMode.Auto, awaitItem()) // initial
            repo.setThemeMode(ThemeMode.Dark)
            assertEquals(ThemeMode.Dark, awaitItem()) // updated
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeDefaultCurrency emits updated value after setDefaultCurrency`() = runTest {
        val repo = createRepo()
        repo.observeDefaultCurrency().test {
            assertEquals("USD", awaitItem()) // initial default
            repo.setDefaultCurrency("EUR")
            assertEquals("EUR", awaitItem()) // updated
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeTxDisplayPrefs emits updated value after setTxDisplayPrefs`() = runTest {
        val repo = createRepo()
        val newPrefs = TxDisplayPrefs(
            indicatorStyle = IndicatorStyle.Bar,
            showCategoryName = false,
            showNote = false,
            density = Density.Compact,
        )
        repo.observeTxDisplayPrefs().test {
            awaitItem() // initial
            repo.setTxDisplayPrefs(newPrefs)
            // setTxDisplayPrefs writes 4 keys; combine may emit intermediates — skip to the final value
            var updated = awaitItem()
            while (updated != newPrefs) {
                updated = awaitItem()
            }
            assertEquals(newPrefs, updated)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeSelectedAccountId emits updated value after setSelectedAccountId`() = runTest {
        val repo = createRepo()
        repo.observeSelectedAccountId().test {
            assertEquals(-1L, awaitItem()) // initial default
            repo.setSelectedAccountId(42L)
            assertEquals(42L, awaitItem()) // updated
            cancelAndIgnoreRemainingEvents()
        }
    }
}

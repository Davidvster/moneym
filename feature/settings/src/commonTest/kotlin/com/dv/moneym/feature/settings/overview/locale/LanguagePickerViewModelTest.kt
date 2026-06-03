package com.dv.moneym.feature.settings.overview.locale

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.dv.moneym.core.testing.FakeAppSettingsRepository
import com.dv.moneym.feature.settings.FakeLocaleController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class LanguagePickerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() { Dispatchers.setMain(testDispatcher) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun selectedLanguageReflectsRepository() = runTest(testDispatcher) {
        val settings = FakeAppSettingsRepository()
        val vm = LanguagePickerViewModel(settings, FakeLocaleController(), SavedStateHandle())

        vm.selectedLanguage.test {
            assertEquals("", awaitItem())
        }
    }

    @Test
    fun setLanguagePersistsAndAppliesLocale() = runTest(testDispatcher) {
        val settings = FakeAppSettingsRepository()
        val locale = FakeLocaleController()
        val vm = LanguagePickerViewModel(settings, locale, SavedStateHandle())

        vm.selectedLanguage.test {
            assertEquals("", awaitItem())
            vm.onIntent(LanguagePickerIntent.SetLanguage("de"))
            assertEquals("de", awaitItem())
        }
        assertEquals("de", locale.appliedTag)
    }
}

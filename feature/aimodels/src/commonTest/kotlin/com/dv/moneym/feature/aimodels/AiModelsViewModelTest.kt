package com.dv.moneym.feature.aimodels

import app.cash.turbine.test
import com.dv.moneym.core.testing.FakeLlmModelRepository
import com.dv.moneym.data.llmmodels.LlmModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AiModelsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() { Dispatchers.setMain(testDispatcher) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    private fun model(id: String) = LlmModel(
        id = id,
        displayNameKey = "key_$id",
        fileName = "$id.litertlm",
        url = "https://example/$id",
        sizeBytes = 2_000_000_000L,
        sha256 = "",
        format = "litertlm",
        requiresToken = false,
    )

    private val catalog = listOf(model("a"), model("b"))

    private fun vm(repo: FakeLlmModelRepository = FakeLlmModelRepository(catalog)) =
        AiModelsViewModel(repo)

    @Test
    fun modelsMapToRows() = runTest(testDispatcher) {
        vm().state.test {
            var s = awaitItem()
            while (s.models.size != 2) s = awaitItem()
            assertEquals(listOf("a", "b"), s.models.map { it.id })
            assertEquals("key_a", s.models.first().displayNameKey)
            assertEquals("2.0 GB", s.models.first().sizeLabel)
            assertTrue(s.models.all { it.status == ModelStatus.NotDownloaded })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun downloadIntentMarksDownloaded() = runTest(testDispatcher) {
        val repo = FakeLlmModelRepository(catalog)
        val vm = vm(repo)
        vm.state.test {
            var s = awaitItem()
            while (s.models.size != 2) s = awaitItem()
            vm.onIntent(AiModelsIntent.Download("a"))
            var after = awaitItem()
            while (after.models.first { it.id == "a" }.status !is ModelStatus.Downloaded) {
                after = awaitItem()
            }
            assertEquals(ModelStatus.Downloaded, after.models.first { it.id == "a" }.status)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun setActiveMarksActiveRow() = runTest(testDispatcher) {
        val repo = FakeLlmModelRepository(catalog)
        val vm = vm(repo)
        vm.state.test {
            var s = awaitItem()
            while (s.models.size != 2) s = awaitItem()
            vm.onIntent(AiModelsIntent.Download("a"))
            vm.onIntent(AiModelsIntent.SetActive("a"))
            var after = awaitItem()
            while (after.models.first { it.id == "a" }.status != ModelStatus.Active) {
                after = awaitItem()
            }
            assertEquals(ModelStatus.Active, after.models.first { it.id == "a" }.status)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun saveTokenReflectedInState() = runTest(testDispatcher) {
        val vm = vm()
        vm.state.test {
            var s = awaitItem()
            while (s.models.size != 2) s = awaitItem()
            vm.onIntent(AiModelsIntent.HfTokenChanged("hf_secret"))
            var typed = awaitItem()
            while (typed.hfToken != "hf_secret") typed = awaitItem()
            assertTrue(!typed.tokenSaved)
            vm.onIntent(AiModelsIntent.SaveToken)
            var saved = awaitItem()
            while (!saved.tokenSaved) saved = awaitItem()
            assertTrue(saved.tokenSaved)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun errorSurfacesAndClears() = runTest(testDispatcher) {
        val repo = ThrowingDeleteRepository(catalog)
        val vm = AiModelsViewModel(repo)
        vm.state.test {
            var s = awaitItem()
            while (s.models.size != 2) s = awaitItem()
            vm.onIntent(AiModelsIntent.Delete("a"))
            var err = awaitItem()
            while (err.error == null) err = awaitItem()
            assertEquals(AiModelsError.Delete, err.error)
            vm.onIntent(AiModelsIntent.ClearError)
            var cleared = awaitItem()
            while (cleared.error != null) cleared = awaitItem()
            assertNull(cleared.error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private class ThrowingDeleteRepository(catalog: List<LlmModel>) :
        com.dv.moneym.data.llmmodels.LlmModelRepository {
        private val delegate = FakeLlmModelRepository(catalog)
        override fun observeModels() = delegate.observeModels()
        override suspend fun download(id: String) = delegate.download(id)
        override fun cancel(id: String) = delegate.cancel(id)
        override suspend fun delete(id: String) { throw IllegalStateException("boom") }
        override suspend fun setActive(id: String) = delegate.setActive(id)
        override suspend fun activeModelPath() = delegate.activeModelPath()
        override suspend fun setHfToken(token: String) = delegate.setHfToken(token)
        override fun observeHasToken() = delegate.observeHasToken()
    }
}

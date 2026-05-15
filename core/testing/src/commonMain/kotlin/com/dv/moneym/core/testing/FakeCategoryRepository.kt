package com.dv.moneym.core.testing

import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.data.categories.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeCategoryRepository : CategoryRepository {
    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    private var nextId = 1L

    val categories: List<Category> get() = _categories.value

    fun addAll(categories: List<Category>) = _categories.update { it + categories }

    override fun observeAll(): Flow<List<Category>> = _categories
    override fun observeActive(): Flow<List<Category>> = _categories.map { it.filter { c -> !c.archived } }
    override suspend fun getById(id: CategoryId): Category? = _categories.value.find { it.id == id }
    override suspend fun count(): Long = _categories.value.size.toLong()
    override suspend fun insert(category: Category): CategoryId {
        val id = CategoryId(nextId++)
        _categories.update { it + category.copy(id = id) }
        return id
    }
    override suspend fun update(category: Category) {
        _categories.update { list -> list.map { if (it.id == category.id) category else it } }
    }
    override suspend fun delete(id: CategoryId) {
        _categories.update { list -> list.filter { it.id != id } }
    }
}

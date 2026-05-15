package com.dv.moneym.core.testing

import com.dv.moneym.core.model.Account
import com.dv.moneym.core.model.AccountId
import com.dv.moneym.data.accounts.AccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeAccountRepository : AccountRepository {
    private val _accounts = MutableStateFlow<List<Account>>(emptyList())
    private var nextId = 1L

    val accounts: List<Account> get() = _accounts.value

    fun addAll(accounts: List<Account>) = _accounts.update { it + accounts }

    override fun observeAll(): Flow<List<Account>> = _accounts
    override fun observeDefault(): Flow<Account?> = _accounts.map { it.firstOrNull { a -> a.isDefault } }
    override suspend fun getById(id: AccountId): Account? = _accounts.value.find { it.id == id }
    override suspend fun count(): Long = _accounts.value.size.toLong()
    override suspend fun insert(account: Account): AccountId {
        val id = AccountId(nextId++)
        _accounts.update { it + account.copy(id = id) }
        return id
    }
    override suspend fun update(account: Account) {
        _accounts.update { list -> list.map { if (it.id == account.id) account else it } }
    }
}

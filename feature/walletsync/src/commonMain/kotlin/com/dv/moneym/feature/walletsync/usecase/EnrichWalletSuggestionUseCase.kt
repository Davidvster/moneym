package com.dv.moneym.feature.walletsync.usecase

import com.dv.moneym.core.model.Account
import com.dv.moneym.core.model.SyncDirection
import com.dv.moneym.core.model.Transaction
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.data.walletsync.WalletSuggestion

class EnrichWalletSuggestionUseCase {

    operator fun invoke(
        suggestion: WalletSuggestion,
        accounts: List<Account>,
        recentTransactions: List<Transaction>,
    ): WalletSuggestion {
        val suggestedAccountId = findMatchingAccount(suggestion.currency, accounts)
        val suggestedCategoryId = findMatchingCategory(suggestion, recentTransactions)

        return suggestion.copy(
            suggestedAccountId = suggestedAccountId,
            suggestedCategoryId = suggestedCategoryId,
        )
    }

    private fun findMatchingAccount(currency: String, accounts: List<Account>): Long? {
        val normalizedCurrency = currency.uppercase()

        return accounts.firstOrNull { it.isDefault && it.currency.value.uppercase() == normalizedCurrency }?.id?.value
            ?: accounts.firstOrNull { it.currency.value.uppercase() == normalizedCurrency }?.id?.value
    }

    private fun findMatchingCategory(
        suggestion: WalletSuggestion,
        transactions: List<Transaction>,
    ): Long? {
        val query = (suggestion.counterparty ?: suggestion.description ?: "").lowercase()
        if (query.isBlank()) return null

        val targetType = when (suggestion.direction) {
            SyncDirection.DEBIT -> TransactionType.EXPENSE
            SyncDirection.CREDIT -> TransactionType.INCOME
        }

        return transactions
            .filter { it.type == targetType }
            .filter { it.note?.lowercase()?.contains(query) == true }
            .groupBy { it.categoryId }
            .maxByOrNull { it.value.size }?.key?.value
    }
}
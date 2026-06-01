package com.dv.moneym.feature.transactionedit.usecase

import com.dv.moneym.core.model.Transaction
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlin.math.pow

class SuggestNotesUseCase {
    operator fun invoke(
        transactions: List<Transaction>,
        query: String,
        today: LocalDate,
        limit: Int = 5,
    ): List<String> {
        if (query.isBlank()) return emptyList()

        val scores = HashMap<String, Double>()
        for (txn in transactions) {
            val note = txn.note?.takeIf { it.isNotBlank() } ?: continue
            val ageDays = txn.occurredOn.daysUntil(today).coerceAtLeast(0)
            val weight = 0.5.pow(ageDays / 30.0)
            scores[note] = (scores[note] ?: 0.0) + weight
        }

        val q = query.lowercase()
        val prefix = scores.entries
            .filter { it.key.lowercase().startsWith(q) && it.key != query }
            .sortedByDescending { it.value }
            .map { it.key }
        val contains = scores.entries
            .filter { it.key.lowercase().contains(q) && !it.key.lowercase().startsWith(q) && it.key != query }
            .sortedByDescending { it.value }
            .map { it.key }

        return (prefix + contains).take(limit)
    }
}

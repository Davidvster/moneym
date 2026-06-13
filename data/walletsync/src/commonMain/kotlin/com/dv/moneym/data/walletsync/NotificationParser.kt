package com.dv.moneym.data.walletsync

import com.dv.moneym.core.model.SyncDirection
import kotlinx.datetime.LocalDate
import kotlin.math.roundToLong

/**
 * Best-effort parser turning a payment-app notification into a [WalletSuggestion].
 *
 * Notification text is localized and varies by app and version, so this is heuristic: it extracts
 * an amount + currency from the title/text, guesses the merchant, and defaults to an expense
 * unless the wording signals a refund/credit. Returns null when no amount can be found.
 *
 * Pure and dependency-free so it can be unit-tested exhaustively; the caller supplies [today] (from
 * the app clock) and the user's [defaultCurrency] for notifications that omit a currency.
 */
class NotificationParser {

    fun parse(
        packageName: String,
        appLabel: String?,
        title: String?,
        text: String?,
        postTimeMs: Long,
        today: LocalDate,
        defaultCurrency: String,
    ): WalletSuggestion? {
        val haystack = listOfNotNull(title, text).joinToString(" ").trim()
        if (haystack.isBlank()) return null

        val amountMinor = extractAmountMinor(haystack) ?: return null
        if (amountMinor <= 0L) return null

        val currency = extractCurrency(haystack) ?: defaultCurrency
        val direction = if (CREDIT_HINTS.any { haystack.contains(it, ignoreCase = true) }) {
            SyncDirection.CREDIT
        } else {
            SyncDirection.DEBIT
        }
        val merchant = extractMerchant(haystack)

        val merchantSlug = merchant?.lowercase()?.replace(WHITESPACE, "-") ?: "unknown"
        val externalId = "wallet:$packageName:$today:$amountMinor:$merchantSlug"

        return WalletSuggestion(
            id = 0,
            externalId = externalId,
            amountMinor = amountMinor,
            currency = currency,
            direction = direction,
            date = today,
            description = merchant ?: title,
            counterparty = merchant,
            sourcePackage = packageName,
            sourceAppLabel = appLabel,
            capturedAt = postTimeMs,
        )
    }

    private fun extractCurrency(haystack: String): String? {
        SYMBOL_TO_CODE.forEach { (symbol, code) ->
            if (haystack.contains(symbol)) return code
        }
        return ISO_CODES.firstOrNull { code ->
            Regex("\\b$code\\b", RegexOption.IGNORE_CASE).containsMatchIn(haystack)
        }
    }

    private fun extractAmountMinor(haystack: String): Long? {
        val match = AMOUNT_REGEX.find(haystack) ?: return null
        return parseNumberToMinor(match.groupValues[1])
    }

    private fun parseNumberToMinor(raw: String): Long? {
        var s = raw.trim()
        val hasDot = s.contains('.')
        val hasComma = s.contains(',')
        when {
            hasDot && hasComma -> {
                // The separator that appears last is the decimal one; strip the other (grouping).
                val decimalSep = if (s.lastIndexOf('.') > s.lastIndexOf(',')) '.' else ','
                val groupSep = if (decimalSep == '.') ',' else '.'
                s = s.replace(groupSep.toString(), "").replace(decimalSep, '.')
            }
            hasComma -> {
                // A single comma followed by exactly 2 digits is a decimal; otherwise grouping.
                val after = s.substringAfterLast(',')
                s = if (after.length == 2) s.replace(',', '.') else s.replace(",", "")
            }
            hasDot -> {
                // A single dot followed by 3 digits with a leading group is grouping, e.g. 1.234.
                val after = s.substringAfterLast('.')
                if (after.length == 3 && s.substringBeforeLast('.').isNotEmpty()) {
                    s = s.replace(".", "")
                }
            }
        }
        val value = s.toDoubleOrNull() ?: return null
        return (value * 100).roundToLong()
    }

    private fun extractMerchant(haystack: String): String? {
        val match = MERCHANT_REGEX.find(haystack) ?: return null
        return match.groupValues[1].trim().trim('.', ',', '!', ':').takeIf { it.isNotBlank() }
    }

    private companion object {
        val SYMBOL_TO_CODE = linkedMapOf(
            "€" to "EUR",
            "£" to "GBP",
            "₹" to "INR",
            "¥" to "JPY",
            "$" to "USD",
        )
        val ISO_CODES = listOf(
            "EUR", "USD", "GBP", "INR", "JPY", "CHF", "CAD", "AUD", "SEK", "NOK", "DKK", "PLN", "CZK",
        )
        val CREDIT_HINTS = listOf("refund", "received", "credited", "credit", "cashback", "deposit")
        val WHITESPACE = Regex("\\s+")

        // Amount with optional currency symbol, e.g. "$12.34", "12,34 €", "1.234,56", "1234".
        val AMOUNT_REGEX = Regex("[\\$€£₹¥]?\\s?(\\d{1,3}(?:[.,]\\d{3})*(?:[.,]\\d{1,2})?|\\d+(?:[.,]\\d{1,2})?)")

        // Merchant follows "to"/"at"/"from", e.g. "You paid €5 to Starbucks".
        val MERCHANT_REGEX = Regex(
            "(?:to|at|from)\\s+([A-Za-z0-9&.'\\- ]{2,40})",
            RegexOption.IGNORE_CASE,
        )
    }
}

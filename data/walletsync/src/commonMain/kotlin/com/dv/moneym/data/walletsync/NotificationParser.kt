package com.dv.moneym.data.walletsync

import com.dv.moneym.core.model.CommonCurrencies
import com.dv.moneym.core.model.CurrencyInfo
import com.dv.moneym.core.model.SyncDirection
import kotlinx.datetime.LocalDate
import kotlin.math.absoluteValue

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
        val rawTitle = title?.trim().orEmpty()
        val rawText = text?.trim().orEmpty()
        if (rawTitle.isBlank() && rawText.isBlank()) return null

        val normTitle = normalize(rawTitle)
        val normText = normalize(rawText)
        val combined = listOf(normTitle, normText).filter { it.isNotBlank() }.joinToString(" ")
        if (isLikelyNonTransactionNotification(combined)) return null

        val amountCandidate = pickBestAmountCandidate(
            title = normTitle,
            text = normText,
            defaultCurrency = defaultCurrency,
        ) ?: return null

        val amountMinor = amountCandidate.amountMinor.absoluteValue
        if (amountMinor <= 0L) return null

        val currency = amountCandidate.currency
            ?: extractCurrency(normText, defaultCurrency)
            ?: extractCurrency(normTitle, defaultCurrency)
            ?: defaultCurrency.uppercase()

        val titleCandidate = cleanupTitleCandidate(rawTitle)
        val textMerchant = extractMerchantFromText(rawText)

        val merchant = textMerchant
            ?: titleCandidate?.takeIf { isUsefulTitle(it, appLabel, packageName) }

        val description = when {
            textMerchant != null -> textMerchant
            titleCandidate != null && isUsefulTitle(titleCandidate, appLabel, packageName) -> titleCandidate
            merchant != null -> merchant
            rawTitle.isNotBlank() -> rawTitle
            rawText.isNotBlank() -> rawText
            else -> "Payment"
        }

        val direction = inferDirection(combined, amountCandidate.amountMinor)

        val merchantSlug = slugify(merchant ?: description)
        val externalId = "wallet:$packageName:$today:$amountMinor:$currency:$merchantSlug"

        return WalletSuggestion(
            id = 0,
            externalId = externalId,
            amountMinor = amountMinor,
            currency = currency,
            direction = direction,
            date = today,
            description = description,
            counterparty = merchant,
            sourcePackage = packageName,
            sourceAppLabel = appLabel,
            capturedAt = postTimeMs,
        )
    }

    private fun pickBestAmountCandidate(
        title: String,
        text: String,
        defaultCurrency: String,
    ): AmountCandidate? {
        val candidates = mutableListOf<AmountCandidate>()
        candidates += findAmountCandidates(title, Source.TITLE, defaultCurrency)
        candidates += findAmountCandidates(text, Source.TEXT, defaultCurrency)

        return candidates.maxWithOrNull(
            compareBy<AmountCandidate> { it.score }
                .thenBy { if (it.currency != null) 1 else 0 }
                .thenBy { if (it.source == Source.TEXT) 1 else 0 }
        )
    }

    private fun findAmountCandidates(
        input: String,
        source: Source,
        defaultCurrency: String,
    ): List<AmountCandidate> {
        if (input.isBlank()) return emptyList()

        val out = mutableListOf<AmountCandidate>()

        for (match in AMOUNT_REGEX.findAll(input)) {
            val beforeCurrency = match.groupValues.getOrElse(1) { "" }
            val rawNumber = match.groupValues.getOrElse(2) { "" }
            val afterCurrency = match.groupValues.getOrElse(3) { "" }

            if (rawNumber.isBlank()) continue

            val signedMinor = parseNumberToMinor(rawNumber) ?: continue
            val absMinor = signedMinor.absoluteValue
            if (absMinor == 0L) continue

            val context = surroundingContext(input, match.range.first, match.range.last + 1).lowercase()
            val currency = normalizeCurrency(beforeCurrency, defaultCurrency)
                ?: normalizeCurrency(afterCurrency, defaultCurrency)

            var score = 0
            score += if (source == Source.TEXT) 20 else 5
            if (currency != null) score += 40
            if (looksDecimal(rawNumber)) score += 10
            if (containsAny(context, AMOUNT_HINTS)) score += 10
            if (containsAny(context, CARD_HINTS) || MASKED_CARD_REGEX.containsMatchIn(context)) score -= 40
            if (looksLikeYear(rawNumber) && currency == null) score -= 25
            if (looksLikeTime(rawNumber)) score -= 25
            if (containsAny(context, DATE_HINTS) && currency == null) score -= 30

            if (score > 0) {
                out += AmountCandidate(
                    amountMinor = signedMinor,
                    currency = currency,
                    source = source,
                    score = score,
                )
            }
        }

        return out
    }

    private fun extractMerchantFromText(rawText: String): String? {
        val text = normalize(rawText)
        if (text.isBlank()) return null

        for (pattern in MERCHANT_PATTERNS) {
            val match = pattern.find(text) ?: continue
            val cleaned = cleanupMerchant(match.groupValues[1])
            if (!cleaned.isNullOrBlank()) return cleaned
        }

        return null
    }

    private fun cleanupMerchant(raw: String): String? {
        var s = normalize(raw)
        s = AMOUNT_REGEX.replace(s, " ")
        s = MASKED_CARD_REGEX.replace(s, " ")
        s = trimLeadingTokens(s, LEADING_CONNECTORS)
        s = trimAfterFirstMarker(s, MERCHANT_STOP_MARKERS)
        s = trimTrailingTokens(s, TRAILING_MERCHANT_NOISE)
        s = trimCardTail(s)
        s = normalize(s)
        // Trim leading punctuation but preserve trailing dots for abbreviations (S.A., Ltd., Inc.)
        s = s.trimStart(' ', '.', ',', ':', ';', '-', '–', '—', '|', '•', '·')
        s = s.trimEnd(' ', ',', ':', ';', '-', '–', '—', '|', '•', '·')
        // Collapse consecutive trailing dots to one: "S.A.." → "S.A."
        while (s.endsWith("..")) s = s.dropLast(1)

        return s.takeIf { it.isNotBlank() }
    }

    private fun isLikelyNonTransactionNotification(input: String): Boolean {
        val lower = normalize(input).lowercase()
        if (lower.isBlank()) return false

        if (containsAny(lower, MARKET_HINTS) && PERCENT_REGEX.containsMatchIn(lower)) return true
        if (containsAny(lower, MARKET_HINTS) && containsAny(lower, NEWS_HINTS)) return true
        if (containsAny(lower, CHALLENGE_REWARD_HINTS) && containsAny(lower, DEADLINE_TERMS_HINTS)) return true
        if (containsAny(lower, PROMOTIONAL_HINTS) && containsAny(lower, WORTH_BENEFITS_POINTS_HINTS)) return true
        if (containsAny(lower, WORTH_BENEFITS_POINTS_HINTS) &&
            containsAny(lower, DEADLINE_TERMS_HINTS) &&
            !containsAny(lower, PAYMENT_CONFIRMATION_HINTS)
        ) {
            return true
        }

        return false
    }

    private fun cleanupTitleCandidate(rawTitle: String): String? {
        val s = normalize(rawTitle).trim(' ', '.', ',', ':', ';', '-', '–', '—')
        return s.takeIf { it.isNotBlank() }
    }

    private fun isUsefulTitle(
        value: String,
        appLabel: String?,
        packageName: String,
    ): Boolean {
        val norm = normalize(value).lowercase()
        if (norm.isBlank()) return false

        val app = normalize(appLabel.orEmpty()).lowercase()
        val pkgLeaf = packageName.substringAfterLast('.')
            .replace('.', ' ')
            .replace('_', ' ')
            .replace('-', ' ')
            .lowercase()

        if (norm == app || norm == pkgLeaf) return false
        if (norm == "google pay") return false
        if (norm == "google wallet") return false
        if (norm.contains("wallet")) return false
        if (norm.contains("mobile banking")) return false
        if (norm.endsWith(" pay")) return false

        return true
    }

    private fun inferDirection(
        combined: String,
        signedAmountMinor: Long,
    ): SyncDirection {
        val lower = combined.lowercase()
        if (containsAny(lower, CREDIT_HINTS)) return SyncDirection.CREDIT
        if (containsAny(lower, DEBIT_HINTS)) return SyncDirection.DEBIT
        return if (signedAmountMinor < 0L) SyncDirection.DEBIT else SyncDirection.DEBIT
    }

    private fun extractCurrency(
        input: String,
        defaultCurrency: String,
    ): String? {
        if (input.isBlank()) return null

        findExplicitCurrencyCode(input)?.let { return it }

        val normalized = normalize(input)
        for (symbol in SORTED_CURRENCY_SYMBOLS) {
            if (containsSymbol(normalized, symbol)) {
                val codes = SYMBOL_TO_CODES[symbol].orEmpty()
                if (codes.isEmpty()) return null
                if (codes.size == 1) return codes.first()
                return codes.firstOrNull { it == defaultCurrency.uppercase() } ?: codes.first()
            }
        }

        return null
    }

    private fun normalizeCurrency(
        raw: String?,
        defaultCurrency: String,
    ): String? {
        if (raw.isNullOrBlank()) return null

        findExplicitCurrencyCode(raw)?.let { return it }

        val symbol = normalizeCurrencySymbol(raw)
        val codes = SYMBOL_TO_CODES[symbol].orEmpty()
        if (codes.isEmpty()) return null
        if (codes.size == 1) return codes.first()
        return codes.firstOrNull { it == defaultCurrency.uppercase() } ?: codes.first()
    }

    private fun findExplicitCurrencyCode(input: String): String? {
        val upper = input.uppercase()
        return SUPPORTED_CURRENCY_CODES.firstOrNull { code ->
            containsToken(upper, code)
        }
    }

    private fun containsSymbol(input: String, symbol: String): Boolean {
        val haystack = input.uppercase()
        val needle = symbol.uppercase()

        var index = haystack.indexOf(needle)
        while (index >= 0) {
            val before = haystack.getOrNull(index - 1)
            val after = haystack.getOrNull(index + needle.length)
            val tokenLike = needle.any { it.isLetterOrDigit() }

            val ok = if (!tokenLike) {
                true
            } else {
                (before == null || !before.isLetterOrDigit()) &&
                        (after == null || !after.isLetterOrDigit())
            }

            if (ok) return true
            index = haystack.indexOf(needle, index + 1)
        }

        return false
    }

    private fun containsToken(
        haystackUpper: String,
        tokenUpper: String,
    ): Boolean {
        var index = haystackUpper.indexOf(tokenUpper)
        while (index >= 0) {
            val before = haystackUpper.getOrNull(index - 1)
            val after = haystackUpper.getOrNull(index + tokenUpper.length)
            val ok = (before == null || !before.isLetterOrDigit()) &&
                    (after == null || !after.isLetterOrDigit())

            if (ok) return true
            index = haystackUpper.indexOf(tokenUpper, index + 1)
        }
        return false
    }

    private fun parseNumberToMinor(raw: String): Long? {
        var s = normalize(raw)

        val negative = s.startsWith("-") || s.startsWith("−")
        s = s.removePrefix("+").removePrefix("-").removePrefix("−").trim()
        s = s.replace(" ", "").replace("'", "").replace("'", "")
        if (s.isBlank()) return null

        val lastComma = s.lastIndexOf(',')
        val lastDot = s.lastIndexOf('.')

        val decimalSep = when {
            lastComma >= 0 && lastDot >= 0 -> if (lastComma > lastDot) ',' else '.'
            lastComma >= 0 -> {
                val frac = s.substring(lastComma + 1).filter(Char::isDigit)
                if (frac.length in 1..2) ',' else null
            }
            lastDot >= 0 -> {
                val frac = s.substring(lastDot + 1).filter(Char::isDigit)
                if (frac.length in 1..2) '.' else null
            }
            else -> null
        }

        val intPart: String
        val fracPart: String

        if (decimalSep != null) {
            intPart = s.substringBeforeLast(decimalSep).filter(Char::isDigit)
            fracPart = s.substringAfterLast(decimalSep).filter(Char::isDigit).padEnd(2, '0').take(2)
        } else {
            intPart = s.filter(Char::isDigit)
            fracPart = "00"
        }

        if (intPart.isBlank()) return null

        val major = intPart.toLongOrNull() ?: return null
        val minor = fracPart.toLongOrNull() ?: return null
        val result = major * 100L + minor

        return if (negative) -result else result
    }

    private fun looksDecimal(raw: String): Boolean {
        val index = maxOf(raw.lastIndexOf(','), raw.lastIndexOf('.'))
        if (index < 0 || index >= raw.lastIndex) return false
        val frac = raw.substring(index + 1).filter(Char::isDigit)
        return frac.length in 1..2
    }

    private fun looksLikeYear(raw: String): Boolean {
        val digits = raw.filter(Char::isDigit)
        val year = digits.toIntOrNull() ?: return false
        return digits.length == 4 && year in 1900..2100
    }

    private fun looksLikeTime(raw: String): Boolean {
        val s = raw.trim()
        val index = when {
            ':' in s -> s.indexOf(':')
            '.' in s -> s.indexOf('.')
            else -> -1
        }
        if (index <= 0 || index >= s.lastIndex) return false

        val left = s.substring(0, index).filter(Char::isDigit)
        val right = s.substring(index + 1).filter(Char::isDigit)
        return left.length in 1..2 && right.length == 2
    }

    private fun trimCardTail(input: String): String {
        val lower = input.lowercase()
        var cut = input.length

        for (marker in CARD_TAIL_MARKERS) {
            val index = lower.indexOf(marker)
            if (index >= 0 && index < cut) cut = index
        }

        return input.substring(0, cut)
    }

    private fun trimLeadingTokens(input: String, tokens: List<String>): String {
        var s = input.trim()
        var changed: Boolean

        do {
            changed = false
            for (token in tokens.sortedByDescending { it.length }) {
                if (s.startsWith(token, ignoreCase = true)) {
                    s = s.drop(token.length).trimStart()
                    changed = true
                }
            }
        } while (changed)

        return s
    }

    private fun trimTrailingTokens(input: String, tokens: List<String>): String {
        var s = input.trim()
        var changed: Boolean

        do {
            changed = false
            for (token in tokens.sortedByDescending { it.length }) {
                if (s.endsWith(token, ignoreCase = true)) {
                    s = s.dropLast(token.length).trimEnd()
                    changed = true
                }
            }
        } while (changed)

        return s
    }

    private fun trimAfterFirstMarker(input: String, markers: List<String>): String {
        val lower = input.lowercase()
        val cut = markers
            .mapNotNull { marker ->
                lower.indexOf(marker).takeIf { it >= 0 }
            }
            .minOrNull()

        return if (cut == null) input else input.substring(0, cut).trimEnd()
    }

    private fun surroundingContext(
        input: String,
        start: Int,
        endExclusive: Int,
        radius: Int = 18,
    ): String {
        val from = (start - radius).coerceAtLeast(0)
        val to = (endExclusive + radius).coerceAtMost(input.length)
        return input.substring(from, to)
    }

    private fun slugify(input: String): String {
        val lower = normalize(input).lowercase()
        val out = StringBuilder()
        var lastWasDash = false

        for (c in lower) {
            when {
                c.isLetterOrDigit() -> {
                    out.append(c)
                    lastWasDash = false
                }
                !lastWasDash -> {
                    out.append('-')
                    lastWasDash = true
                }
            }
        }

        return out.toString().trim('-').ifBlank { "unknown" }
    }

    private fun normalize(input: String): String {
        return input
            .replace('\u00A0', ' ')
            .replace('\u202F', ' ')
            .replace('\u2007', ' ')
            .replace('\n', ' ')
            .replace('\t', ' ')
            .replace(MULTISPACE, " ")
            .trim()
    }

    private fun containsAny(
        haystack: String,
        needles: List<String>,
    ): Boolean {
        return needles.any { haystack.contains(it, ignoreCase = true) }
    }

    private enum class Source { TITLE, TEXT }

    private data class AmountCandidate(
        val amountMinor: Long,
        val currency: String?,
        val source: Source,
        val score: Int,
    )

    private companion object {
        private val MULTISPACE = Regex("\\s+")

        private fun normalizeCurrencySymbol(symbol: String): String {
            val out = StringBuilder(symbol.length)
            for (c in symbol) {
                if (!c.isWhitespace()) out.append(c)
            }
            return out.toString().uppercase()
        }

        private val SUPPORTED_CURRENCY_CODES: Set<String> = CommonCurrencies
            .map(CurrencyInfo::code)
            .map(String::uppercase)
            .toSet()

        private val SYMBOL_TO_CODES: Map<String, List<String>> = CommonCurrencies
            .groupBy { normalizeCurrencySymbol(it.symbol) }
            .mapValues { (_, infos) -> infos.map { it.code.uppercase() } }

        private val SORTED_CURRENCY_SYMBOLS: List<String> = SYMBOL_TO_CODES.keys
            .sortedByDescending { it.length }

        private val currencyCodePattern: String =
            SUPPORTED_CURRENCY_CODES.joinToString("|") { Regex.escape(it) }

        private val currencySymbolPattern: String =
            SORTED_CURRENCY_SYMBOLS.joinToString("|") { Regex.escape(it) }

        private val currencyTokenPattern: String =
            "(?:$currencySymbolPattern|$currencyCodePattern)"

        private const val NUMBER_PATTERN: String =
            "[+\\-−]?\\d{1,3}(?:[ ,.'']\\d{3})+(?:[.,]\\d{1,2}(?!\\d))?|[+\\-−]?\\d+(?:[.,]\\d{1,2}(?!\\d))?"

        private val AMOUNT_REGEX = Regex(
            "($currencyTokenPattern)?\\s*($NUMBER_PATTERN)(?:\\s*($currencyTokenPattern))?",
            RegexOption.IGNORE_CASE,
        )

        private val MASKED_CARD_REGEX = Regex(
            "(?:\\*{2,}|x{2,}|X{2,}|•{2,}|·{2,})\\s*\\d{0,4}",
            RegexOption.IGNORE_CASE,
        )

        private val MERCHANT_PATTERNS        get() = NotificationParserHints.MERCHANT_PATTERNS
        private val LEADING_CONNECTORS       get() = NotificationParserHints.LEADING_CONNECTORS
        private val MERCHANT_STOP_MARKERS    get() = NotificationParserHints.MERCHANT_STOP_MARKERS
        private val TRAILING_MERCHANT_NOISE get() = NotificationParserHints.TRAILING_MERCHANT_NOISE
        private val CARD_TAIL_MARKERS       get() = NotificationParserHints.CARD_TAIL_MARKERS
        private val CARD_HINTS              get() = NotificationParserHints.CARD_HINTS
        private val AMOUNT_HINTS            get() = NotificationParserHints.AMOUNT_HINTS
        private val DATE_HINTS              get() = NotificationParserHints.DATE_HINTS
        private val CREDIT_HINTS            get() = NotificationParserHints.CREDIT_HINTS
        private val DEBIT_HINTS             get() = NotificationParserHints.DEBIT_HINTS
        private val MARKET_HINTS            get() = NotificationParserHints.MARKET_HINTS
        private val NEWS_HINTS              get() = NotificationParserHints.NEWS_HINTS
        private val CHALLENGE_REWARD_HINTS  get() = NotificationParserHints.CHALLENGE_REWARD_HINTS
        private val DEADLINE_TERMS_HINTS    get() = NotificationParserHints.DEADLINE_TERMS_HINTS
        private val PROMOTIONAL_HINTS       get() = NotificationParserHints.PROMOTIONAL_HINTS
        private val WORTH_BENEFITS_POINTS_HINTS get() = NotificationParserHints.WORTH_BENEFITS_POINTS_HINTS
        private val PAYMENT_CONFIRMATION_HINTS  get() = NotificationParserHints.PAYMENT_CONFIRMATION_HINTS

        private val PERCENT_REGEX = Regex("""\d+(?:[.,]\d+)?\s*%""")
    }
}

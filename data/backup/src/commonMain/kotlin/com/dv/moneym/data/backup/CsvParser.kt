package com.dv.moneym.data.backup

import kotlin.math.abs
import kotlinx.datetime.LocalDate

data class ParsedCsvTransaction(
    val date: LocalDate,
    val type: String,            // "INCOME" or "EXPENSE"
    val amountMinorUnits: Long,  // always positive
    val currencyCode: String,    // empty for EHF (filled later from selected account)
    val categoryName: String,
    val note: String?,
)

data class ParsedImport(
    val transactions: List<ParsedCsvTransaction>,
    val uniqueCategoryNames: List<String>,
    val parseError: String? = null,
)

object CsvParser {

    fun parseMoneyM(csv: String): ParsedImport {
        val lines = csv.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return error("Empty file")

        val header = lines.first().lowercase().trim()
        if (header != "date,type,amount,currency,category,account,note") {
            return error("Unrecognised MoneyM CSV header: ${lines.first().take(80)}")
        }

        val transactions = mutableListOf<ParsedCsvTransaction>()
        val seenCategories = linkedSetOf<String>()

        for (i in 1 until lines.size) {
            val fields = splitCsvLine(lines[i])
            if (fields.size < 7) continue
            val dateStr = fields[0].trim()
            val typeStr = fields[1].trim().uppercase()
            val amountStr = fields[2].trim()
            val currency = fields[3].trim()
            val category = fields[4].trim().ifEmpty { "Uncategorised" }
            val note = fields[6].trim().ifEmpty { null }

            val date = runCatching { LocalDate.parse(dateStr) }.getOrNull() ?: continue
            val amountMinor = parseDecimalToMinorUnits(amountStr) ?: continue
            val type = if (typeStr == "INCOME") "INCOME" else "EXPENSE"

            seenCategories.add(category)
            transactions.add(
                ParsedCsvTransaction(
                    date = date,
                    type = type,
                    amountMinorUnits = amountMinor,
                    currencyCode = currency,
                    categoryName = category,
                    note = note,
                )
            )
        }

        return ParsedImport(transactions, seenCategories.toList())
    }

    fun parseEasyHomeFinance(csv: String): ParsedImport {
        val lines = csv.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return error("Empty file")

        val header = lines.first().lowercase().trim().replace(" ", "")
        if (!header.startsWith("id;date;wallet;category")) {
            return error("Unrecognised Easy Home Finance CSV header: ${lines.first().take(80)}")
        }

        val transactions = mutableListOf<ParsedCsvTransaction>()
        val seenCategories = linkedSetOf<String>()

        for (i in 1 until lines.size) {
            val fields = lines[i].split(";")
            if (fields.size < 7) continue
            val dateStr = fields[1].trim()
            val category = fields[3].trim().ifEmpty { "Uncategorised" }
            val note = fields[5].trim().ifEmpty { null }
            val amountStr = fields[6].trim()

            val date = runCatching { LocalDate.parse(dateStr) }.getOrNull() ?: continue
            val amountDouble = amountStr.toDoubleOrNull() ?: continue
            val amountMinor = (abs(amountDouble) * 100).toLong()
            val type = if (amountDouble >= 0.0) "INCOME" else "EXPENSE"

            seenCategories.add(category)
            transactions.add(
                ParsedCsvTransaction(
                    date = date,
                    type = type,
                    amountMinorUnits = amountMinor,
                    currencyCode = "",
                    categoryName = category,
                    note = note,
                )
            )
        }

        return ParsedImport(transactions, seenCategories.toList())
    }

    private fun error(msg: String) = ParsedImport(emptyList(), emptyList(), msg)

    private fun parseDecimalToMinorUnits(value: String): Long? {
        val parts = value.split(".")
        return when (parts.size) {
            1 -> parts[0].toLongOrNull()?.let { it * 100 }
            2 -> {
                val major = parts[0].toLongOrNull() ?: return null
                val centsStr = parts[1].padEnd(2, '0').take(2)
                val cents = centsStr.toLongOrNull() ?: return null
                major * 100 + cents
            }
            else -> null
        }
    }

    // Minimal RFC-4180 CSV splitter — handles double-quoted fields
    private fun splitCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val buf = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' && !inQuotes -> inQuotes = true
                c == '"' && inQuotes -> {
                    if (i + 1 < line.length && line[i + 1] == '"') {
                        buf.append('"'); i++
                    } else {
                        inQuotes = false
                    }
                }
                c == ',' && !inQuotes -> {
                    result.add(buf.toString())
                    buf.clear()
                }
                else -> buf.append(c)
            }
            i++
        }
        result.add(buf.toString())
        return result
    }
}

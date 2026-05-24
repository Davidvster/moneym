package com.dv.moneym.feature.settings.overview.importdata.usecase

import com.dv.moneym.core.model.Account
import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.data.backup.CsvParser
import com.dv.moneym.feature.settings.overview.importdata.CategoryMappingUiItem
import com.dv.moneym.feature.settings.overview.importdata.CsvSourceFormat
import com.dv.moneym.feature.settings.overview.importdata.ImportTransactionUiItem

data class ImportPreview(
    val parseError: String? = null,
    val selectedAccountId: AccountId? = null,
    val categoryMappings: List<CategoryMappingUiItem> = emptyList(),
    val transactions: List<ImportTransactionUiItem> = emptyList(),
)

class PrepareImportPreviewUseCase {

    operator fun invoke(
        content: String,
        format: CsvSourceFormat,
        accounts: List<Account>,
        existingCategories: List<Category>,
    ): ImportPreview {
        if (content.isBlank()) {
            return ImportPreview(parseError = "No file content")
        }
        val parsed = when (format) {
            CsvSourceFormat.MONEYM -> CsvParser.parseMoneyM(content)
            CsvSourceFormat.EASY_HOME_FINANCE -> CsvParser.parseEasyHomeFinance(content)
        }
        if (parsed.parseError != null) {
            return ImportPreview(parseError = parsed.parseError)
        }
        val defaultAccount = accounts.firstOrNull { it.isDefault } ?: accounts.firstOrNull()
        val mappings = buildCategoryMappings(parsed.uniqueCategoryNames, existingCategories)
        val txItems = parsed.transactions.mapIndexed { i, t ->
            val currency = t.currencyCode.ifEmpty { defaultAccount?.currency?.value ?: "EUR" }
            ImportTransactionUiItem(
                id = "$i",
                date = t.date,
                type = if (t.type == "INCOME") TransactionType.INCOME else TransactionType.EXPENSE,
                amountMinorUnits = t.amountMinorUnits,
                currencyCode = currency,
                categoryName = t.categoryName,
                note = t.note,
            )
        }
        return ImportPreview(
            selectedAccountId = defaultAccount?.id,
            categoryMappings = mappings,
            transactions = txItems,
        )
    }

    private fun buildCategoryMappings(
        csvNames: List<String>,
        existing: List<Category>,
    ): List<CategoryMappingUiItem> {
        val byNameLower = existing.associateBy { it.name.lowercase() }
        return csvNames.map { csvName ->
            val match = byNameLower[csvName.lowercase()]
            CategoryMappingUiItem(
                csvName = csvName,
                mappedToCategoryId = match?.id,
                mappedToCategoryName = match?.name,
            )
        }
    }
}

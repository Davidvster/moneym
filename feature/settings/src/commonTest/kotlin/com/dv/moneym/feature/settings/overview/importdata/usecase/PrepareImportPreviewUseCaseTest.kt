package com.dv.moneym.feature.settings.overview.importdata.usecase

import com.dv.moneym.core.model.Account
import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.AccountType
import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.feature.settings.overview.importdata.CsvSourceFormat
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class PrepareImportPreviewUseCaseTest {

    private val epoch = Instant.fromEpochMilliseconds(0)
    private val useCase = PrepareImportPreviewUseCase()

    private fun account(id: Long, isDefault: Boolean, currency: String = "EUR") = Account(
        id = AccountId(id),
        name = "Acc$id",
        type = AccountType.CASH,
        currency = CurrencyCode(currency),
        isDefault = isDefault,
        archived = false,
        createdAt = epoch,
        updatedAt = epoch,
    )

    private fun category(id: Long, name: String) = Category(
        id = CategoryId(id),
        name = name,
        iconKey = "icon",
        colorHex = "#000000",
        isUserCreated = true,
        archived = false,
        createdAt = epoch,
        updatedAt = epoch,
    )

    private val moneyMHeader = "date,type,amount,currency,category,account,note"

    @Test
    fun blank_content_returns_parse_error() {
        val preview = useCase("   ", CsvSourceFormat.MONEYM, emptyList(), emptyList())
        assertEquals("No file content", preview.parseError)
        assertTrue(preview.transactions.isEmpty())
    }

    @Test
    fun malformed_header_surfaces_parser_error() {
        val preview = useCase("a,b,c\n1,2,3", CsvSourceFormat.MONEYM, emptyList(), emptyList())
        assertTrue(preview.parseError != null)
        assertTrue(preview.transactions.isEmpty())
    }

    @Test
    fun moneym_rows_parsed_with_type_and_amount() {
        val csv = buildString {
            appendLine(moneyMHeader)
            appendLine("2026-05-01,EXPENSE,12.50,EUR,Food,Main,lunch")
            appendLine("2026-05-02,INCOME,100.00,EUR,Salary,Main,")
        }
        val preview = useCase(csv, CsvSourceFormat.MONEYM, listOf(account(1, true)), emptyList())
        assertNull(preview.parseError)
        assertEquals(2, preview.transactions.size)
        val first = preview.transactions.first()
        assertEquals("0", first.id)
        assertEquals(LocalDate(2026, 5, 1), first.date)
        assertEquals(TransactionType.EXPENSE, first.type)
        assertEquals(1250L, first.amountMinorUnits)
        assertEquals("lunch", first.note)
        assertEquals(TransactionType.INCOME, preview.transactions[1].type)
        assertNull(preview.transactions[1].note)
    }

    @Test
    fun selected_account_is_default_then_first() {
        val csv = "$moneyMHeader\n2026-05-01,EXPENSE,1.00,EUR,Food,Main,x"
        val withDefault = useCase(
            csv, CsvSourceFormat.MONEYM,
            listOf(account(1, false), account(2, true)), emptyList(),
        )
        assertEquals(AccountId(2), withDefault.selectedAccountId)

        val noDefault = useCase(
            csv, CsvSourceFormat.MONEYM,
            listOf(account(5, false), account(6, false)), emptyList(),
        )
        assertEquals(AccountId(5), noDefault.selectedAccountId)
    }

    @Test
    fun category_mapping_matches_case_insensitively() {
        val csv = buildString {
            appendLine(moneyMHeader)
            appendLine("2026-05-01,EXPENSE,1.00,EUR,FOOD,Main,x")
            appendLine("2026-05-02,EXPENSE,1.00,EUR,Unknown,Main,y")
        }
        val preview = useCase(
            csv, CsvSourceFormat.MONEYM,
            listOf(account(1, true)),
            listOf(category(1, "Food")),
        )
        val foodMapping = preview.categoryMappings.first { it.csvName == "FOOD" }
        assertEquals(CategoryId(1), foodMapping.mappedToCategoryId)
        assertEquals("Food", foodMapping.mappedToCategoryName)

        val unknownMapping = preview.categoryMappings.first { it.csvName == "Unknown" }
        assertNull(unknownMapping.mappedToCategoryId)
    }

    @Test
    fun empty_currency_falls_back_to_default_account_then_eur() {
        val csv = "$moneyMHeader\n2026-05-01,EXPENSE,1.00,,Food,Main,x"
        val withAccount = useCase(
            csv, CsvSourceFormat.MONEYM,
            listOf(account(1, true, currency = "USD")), emptyList(),
        )
        assertEquals("USD", withAccount.transactions.first().currencyCode)

        val noAccount = useCase(csv, CsvSourceFormat.MONEYM, emptyList(), emptyList())
        assertEquals("EUR", noAccount.transactions.first().currencyCode)
    }

    @Test
    fun easy_home_finance_format_parsed() {
        val csv = buildString {
            appendLine("id;date;wallet;category;subcategory;note;amount")
            appendLine("1;2026-05-01;Main;Food;;lunch;-12.50")
            appendLine("2;2026-05-02;Main;Salary;;;100.00")
        }
        val preview = useCase(csv, CsvSourceFormat.EASY_HOME_FINANCE, listOf(account(1, true)), emptyList())
        assertNull(preview.parseError)
        assertEquals(2, preview.transactions.size)
        assertEquals(TransactionType.EXPENSE, preview.transactions[0].type)
        assertEquals(1250L, preview.transactions[0].amountMinorUnits)
        assertEquals(TransactionType.INCOME, preview.transactions[1].type)
        assertEquals("EUR", preview.transactions[0].currencyCode)
    }
}

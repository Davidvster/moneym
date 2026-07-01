package com.dv.moneym.di

import com.dv.moneym.data.accounts.AccountRepository
import com.dv.moneym.data.accounts.SeedAccountsUseCase
import com.dv.moneym.data.accounts.createAccountRepository
import com.dv.moneym.data.accounts.db.AccountsRoomDatabase
import com.dv.moneym.data.aichat.AiChatRepository
import com.dv.moneym.data.aichat.createAiChatRepository
import com.dv.moneym.data.aichat.db.AiChatRoomDatabase
import com.dv.moneym.data.aiproviders.AiProviderRepository
import com.dv.moneym.data.aiproviders.aiProvidersHttpClient
import com.dv.moneym.data.aiproviders.internal.DefaultAiProviderRepository
import com.dv.moneym.data.aiproviders.internal.RemoteAiClient
import com.dv.moneym.data.budgets.BudgetRepository
import com.dv.moneym.data.budgets.createBudgetRepository
import com.dv.moneym.data.budgets.db.BudgetsRoomDatabase
import com.dv.moneym.data.categories.CategoryRepository
import com.dv.moneym.data.categories.SeedCategoriesUseCase
import com.dv.moneym.data.categories.createCategoryRepository
import com.dv.moneym.data.categories.db.CategoriesRoomDatabase
import com.dv.moneym.data.llmmodels.DefaultLlmModelRepository
import com.dv.moneym.data.llmmodels.LlmModelDownloader
import com.dv.moneym.data.llmmodels.LlmModelRepository
import com.dv.moneym.data.llmmodels.createModelFileStore
import com.dv.moneym.data.llmmodels.llmHttpClient
import com.dv.moneym.data.overview.OverviewRepository
import com.dv.moneym.data.overview.createOverviewRepository
import com.dv.moneym.data.overview.db.OverviewRoomDatabase
import com.dv.moneym.data.transactions.MaterializeRecurringTransactionsUseCase
import com.dv.moneym.data.transactions.PaymentModeRepository
import com.dv.moneym.data.transactions.RecurringTransactionRepository
import com.dv.moneym.data.transactions.SeedPaymentModesUseCase
import com.dv.moneym.data.transactions.TransactionRepository
import com.dv.moneym.data.transactions.createPaymentModeRepository
import com.dv.moneym.data.transactions.createRecurringTransactionRepository
import com.dv.moneym.data.transactions.createTransactionRepository
import com.dv.moneym.data.transactions.db.TransactionsRoomDatabase
import moneym.composeapp.generated.resources.Res
import moneym.composeapp.generated.resources.category_seed_eating_out
import moneym.composeapp.generated.resources.default_account_name
import moneym.composeapp.generated.resources.category_seed_entertainment
import moneym.composeapp.generated.resources.category_seed_gift
import moneym.composeapp.generated.resources.category_seed_groceries
import moneym.composeapp.generated.resources.category_seed_health
import moneym.composeapp.generated.resources.category_seed_other_expense
import moneym.composeapp.generated.resources.category_seed_other_income
import moneym.composeapp.generated.resources.category_seed_payment
import moneym.composeapp.generated.resources.category_seed_rent
import moneym.composeapp.generated.resources.category_seed_salary
import moneym.composeapp.generated.resources.category_seed_shopping
import moneym.composeapp.generated.resources.category_seed_transport
import moneym.composeapp.generated.resources.category_seed_utilities
import com.dv.moneym.platform.DbPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.jetbrains.compose.resources.getString
import org.koin.dsl.module

val dataCategoriesModule = module {
    single<CategoryRepository> { createCategoryRepository(get<CategoriesRoomDatabase>()) }
    single {
        SeedCategoriesUseCase(get(), nameProvider = {
            listOf(
                getString(Res.string.category_seed_groceries),
                getString(Res.string.category_seed_eating_out),
                getString(Res.string.category_seed_rent),
                getString(Res.string.category_seed_transport),
                getString(Res.string.category_seed_utilities),
                getString(Res.string.category_seed_health),
                getString(Res.string.category_seed_entertainment),
                getString(Res.string.category_seed_shopping),
                getString(Res.string.category_seed_other_expense),
                getString(Res.string.category_seed_salary),
                getString(Res.string.category_seed_payment),
                getString(Res.string.category_seed_gift),
                getString(Res.string.category_seed_other_income),
            )
        })
    }
}

val dataAccountsModule = module {
    single<AccountRepository> { createAccountRepository(get<AccountsRoomDatabase>()) }
    single { SeedAccountsUseCase(get(), get(), get(), { getString(Res.string.default_account_name) }) }
}

val dataTransactionsModule = module {
    single<TransactionRepository> { createTransactionRepository(get<TransactionsRoomDatabase>()) }
    single<PaymentModeRepository> { createPaymentModeRepository(get<TransactionsRoomDatabase>()) }
    single<RecurringTransactionRepository> { createRecurringTransactionRepository(get<TransactionsRoomDatabase>()) }
    single { SeedPaymentModesUseCase(get()) }
    single { MaterializeRecurringTransactionsUseCase(get(), get(), get()) }
}

val dataBudgetsModule = module {
    single<BudgetRepository> { createBudgetRepository(get<BudgetsRoomDatabase>()) }
}

val dataOverviewModule = module {
    single<OverviewRepository> { createOverviewRepository(get<OverviewRoomDatabase>()) }
}

val dataAichatModule = module {
    single<AiChatRepository> { createAiChatRepository(get<AiChatRoomDatabase>()) }
}

val dataAiProvidersModule = module {
    single { RemoteAiClient(aiProvidersHttpClient()) }
    single<AiProviderRepository> {
        DefaultAiProviderRepository(
            appSettings = get(),
            secureStore = get(),
            client = get(),
        )
    }
}

val dataLlmModelsModule = module {
    single { createModelFileStore(get<DbPlatform>().appFilesDirectory) }
    single {
        LlmModelDownloader(
            client = llmHttpClient(),
            fileStore = get(),
        )
    }
    single<LlmModelRepository> {
        DefaultLlmModelRepository(
            appSettings = get(),
            fileStore = get(),
            downloader = get(),
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
            runtime = get(),
        )
    }
}

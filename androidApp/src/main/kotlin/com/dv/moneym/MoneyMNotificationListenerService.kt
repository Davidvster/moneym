package com.dv.moneym

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.dv.moneym.core.common.AppClock
import com.dv.moneym.core.datastore.AppSettings
import com.dv.moneym.core.datastore.PrefKeys
import com.dv.moneym.data.accounts.AccountRepository
import com.dv.moneym.data.transactions.TransactionRepository
import com.dv.moneym.data.walletsync.NotificationParser
import com.dv.moneym.data.walletsync.WalletSyncRepository
import com.dv.moneym.feature.walletsync.usecase.EnrichWalletSuggestionUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext
import java.time.LocalDate as JavaLocalDate

/**
 * Captures payment notifications from user-selected apps and turns them into pending wallet
 * suggestions. Bound by the system only while the user has granted notification access.
 */
class MoneyMNotificationListenerService : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val appSettings: AppSettings by lazy { GlobalContext.get().get() }
    private val parser: NotificationParser by lazy { GlobalContext.get().get() }
    private val repository: WalletSyncRepository by lazy { GlobalContext.get().get() }
    private val clock: AppClock by lazy { GlobalContext.get().get() }
    private val accountRepository: AccountRepository by lazy { GlobalContext.get().get() }
    private val transactionRepository: TransactionRepository by lazy { GlobalContext.get().get() }
    private val enrichUseCase: EnrichWalletSuggestionUseCase by lazy { GlobalContext.get().get() }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!appSettings.getBoolean(PrefKeys.WALLET_SYNC_ENABLED, defaultValue = false)) return

        val selected = appSettings.getString(PrefKeys.WALLET_SYNC_PACKAGES, defaultValue = null)
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?.toSet()
            .orEmpty()
        val packageName = sbn.packageName ?: return
        if (packageName !in selected) return

        val extras = sbn.notification?.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = (extras.getCharSequence(Notification.EXTRA_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT))?.toString()

        val defaultCurrency = appSettings.getString(PrefKeys.DEFAULT_CURRENCY, defaultValue = "EUR")
            ?: "EUR"
        val suggestion = parser.parse(
            packageName = packageName,
            appLabel = appLabel(packageName),
            title = title,
            text = text,
            postTimeMs = sbn.postTime,
            today = clock.today(),
            defaultCurrency = defaultCurrency,
        ) ?: return

        scope.launch {
            val accounts = accountRepository.observeAll().first()
            val allTxns = transactionRepository.observeAll().first()
            val today = clock.today()
            val sixMonthsAgo = JavaLocalDate.of(today.year, today.monthNumber, today.dayOfMonth)
                .minusMonths(6)
            val recentTxns = allTxns.filter { txn ->
                JavaLocalDate.of(
                    txn.occurredOn.year,
                    txn.occurredOn.monthNumber,
                    txn.occurredOn.dayOfMonth
                ) >= sixMonthsAgo
            }
            val enriched = enrichUseCase(suggestion, accounts, recentTxns)
            val inserted = repository.insertSuggestionsIfNew(listOf(enriched))
            if (inserted > 0) {
                appSettings.putLong(
                    PrefKeys.WALLET_SYNC_LAST_CAPTURE_MS,
                    clock.now().toEpochMilliseconds()
                )
            }
        }
    }

    private fun appLabel(packageName: String): String? = runCatching {
        val pm = packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    }.getOrNull()

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
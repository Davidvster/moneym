package com.dv.moneym.feature.banksync.usecase

import com.dv.moneym.core.datastore.AppSettings
import com.dv.moneym.core.datastore.PrefKeys
import com.dv.moneym.data.banksync.BankAccountLink
import com.dv.moneym.data.banksync.BankSyncRepository
import com.dv.moneym.data.banksync.EbSessionInfo
import com.dv.moneym.data.banksync.EnableBankingClient
import com.dv.moneym.data.banksync.EnableBankingCredentialsStore

class CompleteConnectionUseCase(
    private val client: EnableBankingClient,
    private val credentialsStore: EnableBankingCredentialsStore,
    private val bankSyncRepository: BankSyncRepository,
    private val appSettings: AppSettings,
) {
    suspend operator fun invoke(code: String, bankName: String): Result<EbSessionInfo> =
        client.createSession(code).onSuccess { session ->
            credentialsStore.saveSessionId(session.sessionId)
            session.validUntil?.let {
                appSettings.putLong(PrefKeys.BANK_SYNC_SESSION_VALID_UNTIL_MS, it.toEpochMilliseconds())
            }
            bankSyncRepository.upsertAccounts(
                session.accounts.map { account ->
                    BankAccountLink(
                        uid = account.uid,
                        bankName = bankName,
                        displayName = account.name,
                        iban = account.iban,
                        currency = account.currency.orEmpty(),
                    )
                }
            )
        }
}

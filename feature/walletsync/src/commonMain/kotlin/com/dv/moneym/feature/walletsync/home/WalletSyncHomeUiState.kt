package com.dv.moneym.feature.walletsync.home

import com.dv.moneym.platform.InstalledApp

val WALLET_SYNC_SUGGESTED_PACKAGES: Set<String> = setOf(
    // Wallets/global
    "com.google.android.apps.walletnfcrel",
    "com.google.android.apps.nbu.paisa.user",
    "com.paypal.android.p2pmobile",
    "com.transferwise.android",
    "com.revolut.revolut",

    // Europe/UK
    "de.number26.android",
    "co.uk.getmondo",
    "com.chase.intl",
    "com.bunq.android",
    "com.starlingbank.android",
    "com.grppl.android.shell.CMBlloydsTSB73",
    "uk.co.hsbc.hsbcukmobilebanking",
    "com.barclays.android.barclaysmobilebanking",
    "com.rbs.mobile.android.natwest",
    "com.ing.mobile",
    "es.bancosantander.apps",
    "com.bbva.bbvacontigo",
    "es.lacaixa.mobile.android.newwapicon",
    "net.bnpparibas.mescomptes",
    "fr.creditagricole.androidapp",
    "com.db.pbc.mibanking",
    "de.ingdiba.bankingapp",
    "de.traderepublic.app",

    // US/Canada
    "com.squareup.cash",
    "com.venmo",
    "com.chase.sig.android",
    "com.infonow.bofa",
    "com.wf.wellsfargomobile",
    "com.citi.citimobile",
    "com.konylabs.capitalone",
    "com.usbank.mobilebanking",
    "com.ally.MobileBanking",
    "com.discoverfinancial.mobile",
    "com.usaa.mobile.android.usaa",
    "com.sofi.mobile",
    "com.onedebit.chime",
    "com.td",
    "com.rbc.mobile.android",
    "com.scotiabank.banking",
    "com.cibc.android.mobi",

    // LATAM
    "com.nu.production",
    "com.mercadopago.wallet",
    "com.picpay",
    "br.com.bb.android",
    "br.com.itau",
    "br.com.bradesco",
    "com.santander.app",
    "mx.bancosantander.supermovil",
    "com.bancomer.mbanking",
    "com.bancoazteca.bazdigitalmovil",
    "com.todo1.mobile",
    "com.nequi.MobileApp",

    // Asia
    "com.eg.android.AlipayGphone",
    "com.tencent.mm",
    "com.phonepe.app",
    "net.one97.paytm",
    "com.globe.gcash.android",
    "com.paymaya",
    "com.dbs.sg.dbsmbanking",
    "com.ocbc.mobile",
    "com.uob.mighty.app",
    "my.com.maybank2u.m2umobile",
    "com.kasikorn.retail.mbanking.wap",
    "jp.co.rakuten_bank.rakutenbank",
    "jp.co.smbc.direct",

    // Africa
    "com.safaricom.mpesa.lifestyle",
    "com.sbg.mobile.phone",
    "za.co.fnb.connect.itt",
    "za.co.absa.absaafrica",
    "capitec.acuity.mobile.prod",
    "com.gtbank.gtworldv1",
    "com.kuda.android",
    "team.opay.pay",
    "com.chippercash",

    // Middle East/Turkey
    "com.EmiratesNBD.android",
    "com.adcb.cbgdigi",
    "com.bankmuscat.mbanking",
    "com.stcpay",
    "com.alrajhiretailapp",
    "com.pozitron.iscep",
    "com.garanti.cepsubesi",
    "com.ykb.android",
    "com.akbank.android.apps.akbank_direkt",

    // Russia/CIS
    "ru.sberbankmobile",
    "com.idamob.tinkoff.android",
    "ru.vtb24.mobilebanking.android",
    "ru.alfabank.mobile.android",

    // Australia/New Zealand
    "com.commbank.netbank",
    "org.westpac.bank",
    "com.anz.android.gomoney",
    "au.com.nab.mobile",
    "au.com.ingdirect.android",
    "au.com.up.money",
    "nz.co.kiwibank.mobile",
    "nz.co.asb.asbmobile",
    "nz.co.anz.android.mobilebanking",
    "nz.co.westpac",
)

data class WalletSyncHomeUiState(
    val isLoading: Boolean = true,
    val accessGranted: Boolean = false,
    val enabled: Boolean = false,
    val pendingCount: Int = 0,
    val selectedPackages: Set<String> = emptySet(),
    val showAppPicker: Boolean = false,
    val appsLoading: Boolean = false,
    val installedApps: List<InstalledApp> = emptyList(),
    val appQuery: String = "",
    val pendingRemovePackage: String? = null,
) {
    val filteredApps: List<InstalledApp>
        get() = if (appQuery.isBlank()) installedApps
        else installedApps.filter { it.label.contains(appQuery, ignoreCase = true) }

    val suggestedApps: List<InstalledApp>
        get() = filteredApps.filter { it.packageName in WALLET_SYNC_SUGGESTED_PACKAGES }

    val otherApps: List<InstalledApp>
        get() = filteredApps.filterNot { it.packageName in WALLET_SYNC_SUGGESTED_PACKAGES }

    val selectedApps: List<InstalledApp>
        get() = selectedPackages
            .map { pkg -> installedApps.firstOrNull { it.packageName == pkg } ?: InstalledApp(pkg, pkg) }
            .sortedBy { it.label.lowercase() }
}

sealed interface WalletSyncHomeIntent {
    data object Refresh : WalletSyncHomeIntent
    data object ToggleEnabled : WalletSyncHomeIntent
    data object OpenAccessSettings : WalletSyncHomeIntent
    data class ShowAppPicker(val show: Boolean) : WalletSyncHomeIntent
    data class ToggleApp(val packageName: String) : WalletSyncHomeIntent
    data class SetAppQuery(val text: String) : WalletSyncHomeIntent
    data class RemoveAppRequested(val packageName: String) : WalletSyncHomeIntent
    data object ConfirmRemoveApp : WalletSyncHomeIntent
    data object DismissRemoveDialog : WalletSyncHomeIntent
}

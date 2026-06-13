package com.dv.moneym.platform

/** Wallet notification-listener sync is Android-only; gates its Settings entry off on iOS. */
expect val walletSyncSupported: Boolean

package com.dv.moneym.data.banksync.internal

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.providers.openssl3.Openssl3

internal actual fun platformCryptographyProvider(): CryptographyProvider = CryptographyProvider.Openssl3

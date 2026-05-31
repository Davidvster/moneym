package com.dv.moneym.core.security

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.providers.openssl3.Openssl3

internal actual fun platformCryptographyProvider(): CryptographyProvider = CryptographyProvider.Openssl3

package com.dv.moneym.core.security

import dev.whyoleg.cryptography.CryptographyProvider

internal expect fun platformCryptographyProvider(): CryptographyProvider

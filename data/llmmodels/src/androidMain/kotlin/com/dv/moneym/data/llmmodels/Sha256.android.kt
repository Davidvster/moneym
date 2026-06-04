package com.dv.moneym.data.llmmodels

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.providers.jdk.JDK

internal actual fun platformCryptographyProvider(): CryptographyProvider = CryptographyProvider.JDK

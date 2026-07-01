package com.dv.moneym.core.security

object SecurityKeys {
    const val PIN_HASH = "security.pin_hash"
    const val BIOMETRIC_BOUND = "security.biometric_bound"
    const val BANK_SYNC_APP_ID = "banksync.app_id"
    const val BANK_SYNC_PRIVATE_KEY_PEM = "banksync.private_key_pem"
    const val BANK_SYNC_SESSION_ID = "banksync.session_id"
    const val AI_PROVIDER_OPENAI_API_KEY = "ai.provider.openai.api_key"
    const val AI_PROVIDER_ANTHROPIC_API_KEY = "ai.provider.anthropic.api_key"
    const val AI_PROVIDER_GEMINI_API_KEY = "ai.provider.gemini.api_key"
    const val AI_PROVIDER_OPENROUTER_API_KEY = "ai.provider.openrouter.api_key"
}

object SecurityPrefs {
    const val PIN_ENABLED = "pref.pin_enabled"
    const val BIOMETRIC_ENABLED = "pref.biometric_enabled"
    const val BACKGROUND_LOCK_SECONDS = "pref.background_lock_seconds"
    const val LAST_BACKGROUND_AT = "pref.last_background_at"
    const val FAILED_PIN_ATTEMPTS = "pref.failed_pin_attempts"
    const val LAST_FAILED_ATTEMPT_AT = "pref.last_failed_attempt_at"

    // Allow screenshots / screen recording (default false = blocked via FLAG_SECURE)
    const val ALLOW_SCREENSHOTS = "pref.allow_screenshots"

    const val DEFAULT_LOCK_SECONDS = 30
}

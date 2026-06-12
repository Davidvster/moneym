package com.dv.moneym.feature.banksync.usecase

class ParseRedirectCodeUseCase {
    operator fun invoke(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null
        val queryStart = trimmed.indexOf('?')
        if (queryStart < 0) {
            return trimmed.takeIf { candidate ->
                candidate.none { it.isWhitespace() || it == '/' || it == ':' || it == '&' || it == '=' }
            }
        }
        val query = trimmed.substring(queryStart + 1).substringBefore('#')
        return query.split('&')
            .map { it.substringBefore('=') to it.substringAfter('=', "") }
            .firstOrNull { (key, value) -> key == "code" && value.isNotBlank() }
            ?.second
    }
}

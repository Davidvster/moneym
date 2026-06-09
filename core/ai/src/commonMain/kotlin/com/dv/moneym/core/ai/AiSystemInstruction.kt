package com.dv.moneym.core.ai

private const val BASE_SYSTEM_INSTRUCTION =
    "You are a personal finance assistant inside a budgeting app. " +
        "Answer questions about the user's spending, income, accounts, and budgets " +
        "using only the financial data provided. Be concise and never invent numbers."

// The data and tool results are always English, but the reply must match the app's language.
// [responseLanguage] is the English name of that language (e.g. "German"); null/blank leaves the
// default English behaviour untouched.
fun aiSystemInstruction(responseLanguage: String? = null): String =
    if (responseLanguage.isNullOrBlank()) {
        BASE_SYSTEM_INSTRUCTION
    } else {
        "$BASE_SYSTEM_INSTRUCTION Always write your reply in $responseLanguage, " +
            "even though the financial data and tool results are in English."
    }

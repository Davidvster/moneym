package com.dv.moneym.data.llmmodels

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

actual fun llmHttpClient(): HttpClient = HttpClient(Darwin)

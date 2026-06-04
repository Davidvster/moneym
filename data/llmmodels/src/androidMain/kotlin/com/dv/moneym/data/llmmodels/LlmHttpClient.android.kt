package com.dv.moneym.data.llmmodels

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

actual fun llmHttpClient(): HttpClient = HttpClient(OkHttp)

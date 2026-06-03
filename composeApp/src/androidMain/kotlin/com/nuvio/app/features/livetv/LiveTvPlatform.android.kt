package com.nuvio.app.features.livetv

import java.net.HttpURLConnection
import java.net.URL

actual suspend fun fetchText(url: String): String? = runCatching {
    val conn = URL(url).openConnection() as HttpURLConnection
    conn.requestMethod = "GET"
    conn.connectTimeout = 15_000
    conn.readTimeout = 15_000
    conn.setRequestProperty("User-Agent", "Mozilla/5.0")
    conn.inputStream.bufferedReader().use { it.readText() }
}.getOrNull()

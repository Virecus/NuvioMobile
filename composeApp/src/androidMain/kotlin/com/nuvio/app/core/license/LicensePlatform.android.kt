package com.nuvio.app.core.license

import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

actual suspend fun platformFetchLicenseRegistry(url: String, json: Json): List<LicenseRecord>? =
    withContext(Dispatchers.IO) {
        runCatching {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("Accept", "application/json")
            }
            connection.inputStream.bufferedReader().use { reader ->
                json.decodeFromString<List<LicenseRecord>>(reader.readText())
            }
        }.getOrNull()
    }

actual fun currentIsoTimestamp(): String = Instant.now().toString()

actual fun daysBetweenIso(fromIso: String, toIso: String): Long = runCatching {
    val from = Instant.parse(fromIso)
    val to = Instant.parse(toIso)
    ChronoUnit.DAYS.between(from, to).coerceAtLeast(0)
}.getOrDefault(0L)

actual fun formatIsoDate(isoTimestamp: String): String = runCatching {
    val instant = Instant.parse(isoTimestamp)
    DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(ZoneId.systemDefault()).format(instant)
}.getOrDefault(isoTimestamp)

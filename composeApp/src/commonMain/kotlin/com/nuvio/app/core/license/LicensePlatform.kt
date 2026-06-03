package com.nuvio.app.core.license

import kotlinx.serialization.json.Json

expect suspend fun platformFetchLicenseRegistry(url: String, json: Json): List<LicenseRecord>?

expect fun currentIsoTimestamp(): String

expect fun daysBetweenIso(fromIso: String, toIso: String): Long

expect fun formatIsoDate(isoTimestamp: String): String

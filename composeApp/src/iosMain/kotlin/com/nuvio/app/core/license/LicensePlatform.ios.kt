package com.nuvio.app.core.license

import kotlinx.serialization.json.Json
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSTimeZone
import platform.Foundation.NSURL
import platform.Foundation.NSURLConnection
import platform.Foundation.NSURLRequest
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.localTimeZone
import platform.Foundation.timeIntervalSince1970
import platform.Foundation.timeIntervalSinceReferenceDate

actual suspend fun platformFetchLicenseRegistry(url: String, json: Json): List<LicenseRecord>? =
    runCatching {
        val nsUrl = NSURL(string = url)
        val request = NSURLRequest.requestWithURL(nsUrl)
        val data = NSURLConnection.sendSynchronousRequest(request, null, null) ?: return null
        val jsonString = NSString.create(data, NSUTF8StringEncoding)?.toString() ?: return null
        json.decodeFromString<List<LicenseRecord>>(jsonString)
    }.getOrNull()

actual fun currentIsoTimestamp(): String {
    val formatter = NSDateFormatter().apply {
        dateFormat = "yyyy-MM-dd'T'HH:mm:ss'Z'"
        timeZone = NSTimeZone.timeZoneWithName("UTC") ?: NSTimeZone.localTimeZone
    }
    return formatter.stringFromDate(NSDate())
}

actual fun daysBetweenIso(fromIso: String, toIso: String): Long = runCatching {
    val formatter = NSDateFormatter().apply {
        dateFormat = "yyyy-MM-dd'T'HH:mm:ssX"
        timeZone = NSTimeZone.timeZoneWithName("UTC") ?: NSTimeZone.localTimeZone
    }
    val from = formatter.dateFromString(fromIso) ?: return 0L
    val to = formatter.dateFromString(toIso) ?: return 0L
    val seconds = to.timeIntervalSince1970 - from.timeIntervalSince1970
    (seconds / 86400).toLong().coerceAtLeast(0)
}.getOrDefault(0L)

actual fun formatIsoDate(isoTimestamp: String): String = runCatching {
    val parser = NSDateFormatter().apply {
        dateFormat = "yyyy-MM-dd'T'HH:mm:ssX"
        timeZone = NSTimeZone.timeZoneWithName("UTC") ?: NSTimeZone.localTimeZone
    }
    val date = parser.dateFromString(isoTimestamp) ?: return isoTimestamp
    val display = NSDateFormatter().apply {
        dateFormat = "dd.MM.yyyy"
        timeZone = NSTimeZone.localTimeZone
    }
    display.stringFromDate(date)
}.getOrDefault(isoTimestamp)

package com.nuvio.app.features.livetv

import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSURLSession
import platform.Foundation.NSURL
import platform.Foundation.setHTTPMethod
import platform.Foundation.setValue
import kotlin.coroutines.resume

actual suspend fun fetchText(url: String): String? = suspendCancellableCoroutine { cont ->
    val nsUrl = NSURL.URLWithString(url) ?: run { cont.resume(null); return@suspendCancellableCoroutine }
    val request = NSMutableURLRequest.requestWithURL(nsUrl).apply {
        setHTTPMethod("GET")
        setValue("Mozilla/5.0", forHTTPHeaderField = "User-Agent")
    }
    val task = NSURLSession.sharedSession.dataTaskWithRequest(request) { data, _, _ ->
        val text = data?.let { kotlin.text.Charsets.UTF_8.decode(it as kotlin.ByteArray).toString() }
        cont.resume(text)
    }
    task.resume()
    cont.invokeOnCancellation { task.cancel() }
}

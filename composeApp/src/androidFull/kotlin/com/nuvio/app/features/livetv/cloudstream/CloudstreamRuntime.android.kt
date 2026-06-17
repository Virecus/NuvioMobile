package com.nuvio.app.features.livetv.cloudstream

import android.content.Context
import android.os.Build
import android.util.Log
import com.lagradost.cloudstream3.AcraApplication
import com.lagradost.cloudstream3.app
import com.lagradost.nicehttp.ignoreAllSSLErrors
import okhttp3.Cache
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "CloudstreamRuntime"

/**
 * Mobile port of NuvioTV's PluginRuntimeHooks. Owns the lazy initialisation of
 * the CloudStream library runtime (`app.baseClient` + `AcraApplication.context`)
 * needed before any DEX extension code runs. Conscrypt is installed separately
 * in [initLiveTvRuntime]. Hilt removed; manual `initialize(context)` lifecycle.
 */
object CloudstreamRuntime {
    @Volatile private var appContext: Context? = null
    @Volatile private var initialized = false

    fun initialize(context: Context) {
        appContext = context.applicationContext
        AcraApplication.context = context.applicationContext
    }

    /**
     * Lazily build the CloudStream NiceHttp base client. Safe to call repeatedly;
     * only the first call performs work. Must run before loadExtension /
     * downloadExtension / extension code.
     */
    fun ensureInitialized() {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            val ctx = appContext ?: return
            try {
                app.baseClient = OkHttpClient.Builder()
                    .cookieJar(InMemoryCookieJar)
                    .followRedirects(true)
                    .followSslRedirects(true)
                    .ignoreAllSSLErrors()
                    .cache(
                        Cache(
                            directory = File(ctx.cacheDir, "cs_http_cache"),
                            maxSize = 50L * 1024L * 1024L,
                        )
                    )
                    .build()
            } catch (e: Throwable) {
                Log.w(TAG, "Failed to init NiceHttp baseClient (API ${Build.VERSION.SDK_INT}): ${e.message}")
            }
            initialized = true
        }
    }
}

/** Simple in-memory cookie jar — replaces NuvioTV's app-scoped extensionCookieJar. */
private object InMemoryCookieJar : CookieJar {
    private val store = ConcurrentHashMap<String, List<Cookie>>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        store[url.host] = cookies
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> =
        store[url.host].orEmpty()
}

package com.nuvio.app.features.livetv.cloudstream

import android.content.Context
import android.util.Log
import com.nuvio.app.features.livetv.LiveChannel
import com.nuvio.app.features.livetv.PluginLiveStream
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val TAG = "InatBoxPluginBridge"

// InatBox is distributed as a CloudStream .cs3 DEX extension via Kraptor's repo.
// The download URL + scraper id are pinned here (single built-in live source);
// the plugin itself owns domain resolution + AES decryption, so when upstream
// updates the plugin, broken domains fix themselves with no app change.
private const val INATBOX_SCRAPER_ID = "InatBox"
private const val INATBOX_CS3_URL =
    "https://raw.githubusercontent.com/Kraptor123/cs-kraptor/builds/InatBox.cs3"

// Series-like categories are skipped in phase 1 (no episode picker UI on mobile).
private fun isSeriesCategory(category: String): Boolean {
    val c = category.lowercase()
    return c.contains("dizi") || c == "tv show" || c == "anime"
}

/**
 * Owns the CloudStream DEX loader + runner for the built-in InatBox live source.
 * Hilt-free singleton initialised from MainActivity (full variant only).
 */
object InatBoxPluginBridge {
    @Volatile private var appContext: Context? = null
    @Volatile private var downloaded = false
    private val downloadMutex = Mutex()

    private val registry by lazy { ExternalExtractorRegistry() }
    private val loader by lazy { ExternalExtensionLoader(appContext!!, registry) }
    private val runner by lazy { ExternalExtensionRunner(loader) }

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    private suspend fun ensureDownloaded(): Boolean {
        if (downloaded) return true
        downloadMutex.withLock {
            if (downloaded) return true
            val file = loader.downloadExtension(INATBOX_SCRAPER_ID, INATBOX_CS3_URL)
            downloaded = file != null
            if (!downloaded) Log.e(TAG, "Failed to download InatBox .cs3")
            return downloaded
        }
    }

    /**
     * Returns live channels for [sourceId] (only "inatbox" supported). Series
     * categories are filtered out for phase 1. Maps CloudStream channels to the
     * commonMain LiveChannel shape (id=url, streamPageUrl=url).
     */
    suspend fun fetchLiveChannels(sourceId: String): List<LiveChannel>? {
        if (sourceId != "inatbox") return null
        if (appContext == null) return null
        if (!ensureDownloaded()) return null
        return try {
            val channels = runner.getLiveChannels(INATBOX_SCRAPER_ID)
                .filterNot { isSeriesCategory(it.category) }
            if (channels.isEmpty()) {
                null
            } else {
                channels.map { cs ->
                    LiveChannel(
                        id = cs.id,
                        name = cs.name,
                        poster = cs.poster.orEmpty(),
                        streamPageUrl = cs.id,
                        category = cs.category,
                    )
                }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "fetchLiveChannels failed: ${e.message}", e)
            null
        }
    }

    /**
     * Resolves a single playable stream for [channelId]. Series channels return
     * null (no episode picker yet). Carries stream headers (Referer/User-Agent).
     */
    suspend fun resolveLiveStream(sourceId: String, channelId: String): PluginLiveStream? {
        if (sourceId != "inatbox") return null
        if (appContext == null) return null
        if (!ensureDownloaded()) return null
        return try {
            when (val r = runner.resolveLiveChannel(INATBOX_SCRAPER_ID, channelId)) {
                is CsLiveChannelResult.Streams -> {
                    val stream = r.streams.firstOrNull() ?: return null
                    PluginLiveStream(url = stream.url, headers = stream.headers.orEmpty())
                }
                is CsLiveChannelResult.Series -> {
                    Log.d(TAG, "resolveLiveStream: channel is a series, skipping (no picker)")
                    null
                }
                CsLiveChannelResult.Empty -> null
            }
        } catch (e: Throwable) {
            Log.e(TAG, "resolveLiveStream failed: ${e.message}", e)
            null
        }
    }
}

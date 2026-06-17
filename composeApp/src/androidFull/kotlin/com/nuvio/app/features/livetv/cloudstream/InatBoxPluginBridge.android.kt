package com.nuvio.app.features.livetv.cloudstream

import android.content.Context
import android.util.Log
import com.nuvio.app.features.livetv.LiveChannel
import com.nuvio.app.features.livetv.LiveEpisode
import com.nuvio.app.features.livetv.LiveResolveResult
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
     * Returns live channels for [sourceId] (only "inatbox" supported). Maps
     * CloudStream channels to the commonMain LiveChannel shape (id=url,
     * streamPageUrl=url). Plugin category order is preserved.
     */
    suspend fun fetchLiveChannels(sourceId: String): List<LiveChannel>? {
        if (sourceId != "inatbox") return null
        if (appContext == null) return null
        if (!ensureDownloaded()) return null
        return try {
            val channels = runner.getLiveChannels(INATBOX_SCRAPER_ID)
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
     * Resolves a channel into a playable stream or a series with episodes.
     * Carries stream headers (Referer/User-Agent).
     */
    suspend fun resolveChannel(sourceId: String, channelId: String): LiveResolveResult {
        if (sourceId != "inatbox") return LiveResolveResult.Empty
        if (appContext == null) return LiveResolveResult.Empty
        if (!ensureDownloaded()) return LiveResolveResult.Empty
        return try {
            when (val r = runner.resolveLiveChannel(INATBOX_SCRAPER_ID, channelId)) {
                is CsLiveChannelResult.Streams -> {
                    val stream = r.streams.firstOrNull()
                        ?: return LiveResolveResult.Empty
                    LiveResolveResult.Stream(
                        PluginLiveStream(url = stream.url, headers = stream.headers.orEmpty())
                    )
                }
                is CsLiveChannelResult.Series -> LiveResolveResult.Series(
                    title = r.title,
                    episodes = r.episodes.map { ep ->
                        LiveEpisode(ep.season, ep.episode, ep.name, ep.data, ep.label)
                    },
                )
                CsLiveChannelResult.Empty -> LiveResolveResult.Empty
            }
        } catch (e: Throwable) {
            Log.e(TAG, "resolveChannel failed: ${e.message}", e)
            LiveResolveResult.Empty
        }
    }

    /** Resolves a playable stream for a series episode (from LiveEpisode.data). */
    suspend fun resolveEpisodeStream(sourceId: String, episodeData: String): PluginLiveStream? {
        if (sourceId != "inatbox") return null
        if (appContext == null) return null
        if (!ensureDownloaded()) return null
        return try {
            val stream = runner.getLiveEpisodeStreams(INATBOX_SCRAPER_ID, episodeData).firstOrNull()
                ?: return null
            PluginLiveStream(url = stream.url, headers = stream.headers.orEmpty())
        } catch (e: Throwable) {
            Log.e(TAG, "resolveEpisodeStream failed: ${e.message}", e)
            null
        }
    }
}

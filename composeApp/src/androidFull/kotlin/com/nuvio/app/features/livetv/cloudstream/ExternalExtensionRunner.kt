package com.nuvio.app.features.livetv.cloudstream

import android.util.Log
import com.lagradost.cloudstream3.AnimeLoadResponse
import com.lagradost.cloudstream3.APIHolder
import com.lagradost.cloudstream3.LiveStreamLoadResponse
import com.lagradost.cloudstream3.MainPageData
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.MovieLoadResponse
import com.lagradost.cloudstream3.TvSeriesLoadResponse
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.Qualities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "ExtExtensionRunner"

/**
 * Executes loaded CloudStream extensions for live TV. Trimmed port of NuvioTV's
 * ExternalExtensionRunner — only the live-channel paths are kept (TMDB
 * search/match logic removed). Hilt removed.
 */
class ExternalExtensionRunner(
    private val extensionLoader: ExternalExtensionLoader,
) {
    /**
     * Fetches live channels from a plugin that supports TvType.Live.
     */
    suspend fun getLiveChannels(scraperId: String): List<CsLiveChannel> = withContext(Dispatchers.IO) {
        extensionLoader.ensureExtractorsLoaded(listOf(scraperId))
        val api = extensionLoader.getApi(scraperId)
            ?: synchronized(APIHolder.allProviders) {
                APIHolder.allProviders.firstOrNull { it.name.equals(scraperId, ignoreCase = true) }
            }
        if (api == null) {
            val known = synchronized(APIHolder.allProviders) { APIHolder.allProviders.map { it.name } }
            Log.e(TAG, "getLiveChannels: no API for $scraperId (allProviders=$known)")
            return@withContext emptyList()
        }
        Log.d(TAG, "getLiveChannels: using API ${api.name}")
        if (!api.supportedTypes.contains(TvType.Live)) {
            Log.w(TAG, "getLiveChannels: $scraperId does not declare TvType.Live")
        }
        try {
            val pageDatas: List<MainPageData> = api.mainPage.ifEmpty {
                listOf(MainPageData("", "", false))
            }
            Log.d(TAG, "getLiveChannels: ${pageDatas.size} mainPage categories: ${pageDatas.map { it.name }}")
            val channels = mutableListOf<CsLiveChannel>()
            for (pageData in pageDatas) {
                val request = MainPageRequest(pageData.name, pageData.data, pageData.horizontalImages)
                // Try page=1 first; some providers (e.g. InatBox) return 0 results on page=1
                // but work correctly on page=0.
                var response = runCatching { api.getMainPage(1, request) }
                    .onFailure { Log.w(TAG, "getLiveChannels: [${pageData.name}] page=1 error: ${it.javaClass.simpleName}: ${it.message}") }
                    .getOrNull()
                if (response != null && response.items.all { it.list.isEmpty() }) {
                    Log.d(TAG, "getLiveChannels: [${pageData.name}] page=1 returned empty lists, retrying page=0")
                    response = runCatching { api.getMainPage(0, request) }
                        .onFailure { Log.w(TAG, "getLiveChannels: [${pageData.name}] page=0 error: ${it.javaClass.simpleName}: ${it.message}") }
                        .getOrNull() ?: response
                }
                if (response == null) {
                    Log.d(TAG, "getLiveChannels: [${pageData.name}] → null (both pages failed)")
                    continue
                }
                val listNames = response.items.map { "${it.name}(${it.list.size})" }
                Log.d(TAG, "getLiveChannels: [${pageData.name}] → ${response.items.size} lists: $listNames")
                for (list in response.items) {
                    for (item in list.list) {
                        channels += CsLiveChannel(
                            id = item.url,
                            name = item.name,
                            poster = item.posterUrl,
                            category = list.name.ifBlank { pageData.name },
                            scraperId = scraperId,
                        )
                    }
                }
            }
            Log.d(TAG, "getLiveChannels: total ${channels.size} channels")
            channels
        } catch (e: Exception) {
            Log.e(TAG, "getLiveChannels $scraperId failed: ${e.javaClass.simpleName}: ${e.message}", e)
            emptyList()
        } catch (e: Error) {
            Log.e(TAG, "getLiveChannels $scraperId ERROR: ${e.javaClass.simpleName}: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Resolves a live channel:
     * - TvSeriesLoadResponse/AnimeLoadResponse → CsLiveChannelResult.Series (episode list)
     * - Otherwise → CsLiveChannelResult.Streams(list) with the playable links
     */
    suspend fun resolveLiveChannel(scraperId: String, channelId: String): CsLiveChannelResult =
        withContext(Dispatchers.IO) {
            extensionLoader.ensureExtractorsLoaded(listOf(scraperId))
            val api = extensionLoader.getApi(scraperId)
                ?: synchronized(APIHolder.allProviders) {
                    APIHolder.allProviders.firstOrNull { it.name.equals(scraperId, ignoreCase = true) }
                }
                ?: return@withContext CsLiveChannelResult.Empty
            try {
                val loadResponse = runCatching { api.load(channelId) }
                    .onFailure { Log.e(TAG, "resolveLiveChannel api.load failed: ${it.message}", it) }
                    .getOrNull()
                    ?: return@withContext CsLiveChannelResult.Empty
                Log.d(TAG, "resolveLiveChannel: channelId=${channelId.take(80)} responseType=${loadResponse::class.simpleName}")
                if (loadResponse is TvSeriesLoadResponse || loadResponse is AnimeLoadResponse) {
                    val episodes = when (loadResponse) {
                        is TvSeriesLoadResponse -> loadResponse.episodes.mapNotNull { ep ->
                            val data = ep.data ?: return@mapNotNull null
                            CsLiveEpisode(ep.season ?: 1, ep.episode ?: 1, ep.name, data)
                        }
                        is AnimeLoadResponse -> {
                            val nameSeasonRegex = Regex("""^[Ss](\d{1,2})\s*[-–]""")
                            data class RawEp(val realSeason: Int, val epNum: Int, val ep: com.lagradost.cloudstream3.Episode, val data: String)
                            val rawList = loadResponse.episodes.values.flatten().mapNotNull { ep ->
                                val data = ep.data ?: return@mapNotNull null
                                val s = ep.name?.let { nameSeasonRegex.find(it)?.groupValues?.get(1)?.toIntOrNull() } ?: (ep.season ?: 1)
                                RawEp(s, ep.episode ?: 1, ep, data)
                            }
                            val versionGroups = mutableListOf<MutableList<RawEp>>(mutableListOf())
                            var prevS = 0
                            for (raw in rawList) {
                                if (raw.realSeason < prevS) versionGroups.add(mutableListOf())
                                versionGroups.last().add(raw)
                                prevS = raw.realSeason
                            }
                            fun inferLabel(group: List<RawEp>): String {
                                val sampleUrl = group.firstOrNull()?.data?.uppercase() ?: return "Versiyon"
                                return when {
                                    "_TR_DUB" in sampleUrl || "_TRDUB" in sampleUrl -> "TR Dublaj"
                                    "_TR" in sampleUrl -> "TR Dublaj"
                                    "_ENG" in sampleUrl || "_EN_" in sampleUrl -> "TR Altyazı"
                                    "_DUB" in sampleUrl -> "TR Dublaj"
                                    else -> "Versiyon ${versionGroups.indexOf(group) + 1}"
                                }
                            }
                            val result = mutableListOf<CsLiveEpisode>()
                            versionGroups.forEach { group ->
                                val label = inferLabel(group)
                                val seen = mutableSetOf<Pair<Int, Int>>()
                                group.forEach { raw ->
                                    if (seen.add(raw.realSeason to raw.epNum)) {
                                        result.add(CsLiveEpisode(raw.realSeason, raw.epNum, raw.ep.name, raw.data, label))
                                    }
                                }
                            }
                            result
                        }
                        else -> emptyList()
                    }
                    Log.d(TAG, "resolveLiveChannel: → Series (${loadResponse::class.simpleName}) episodes=${episodes.size}")
                    return@withContext CsLiveChannelResult.Series(channelId, loadResponse.name, episodes)
                }
                val data = when (loadResponse) {
                    is LiveStreamLoadResponse -> loadResponse.dataUrl ?: loadResponse.url
                    is MovieLoadResponse -> loadResponse.dataUrl
                    else -> loadResponse.url
                }
                val links = mutableListOf<ExtractorLink>()
                runCatching {
                    api.loadLinks(
                        data = data,
                        isCasting = false,
                        subtitleCallback = {},
                        callback = { links.add(it) }
                    )
                }
                val results = links.filterValid().map { it.toCsScraperResult(api.name) }
                if (results.isEmpty()) CsLiveChannelResult.Empty else CsLiveChannelResult.Streams(results)
            } catch (e: Exception) {
                Log.e(TAG, "resolveLiveChannel $scraperId/$channelId failed: ${e.message}", e)
                CsLiveChannelResult.Empty
            }
        }

    suspend fun getLiveEpisodeStreams(scraperId: String, episodeData: String): List<CsScraperResult> =
        withContext(Dispatchers.IO) {
            extensionLoader.ensureExtractorsLoaded(listOf(scraperId))
            val api = extensionLoader.getApi(scraperId)
                ?: synchronized(APIHolder.allProviders) {
                    APIHolder.allProviders.firstOrNull { it.name.equals(scraperId, ignoreCase = true) }
                }
                ?: return@withContext emptyList()
            val links = mutableListOf<ExtractorLink>()
            runCatching {
                api.loadLinks(
                    data = episodeData,
                    isCasting = false,
                    subtitleCallback = {},
                    callback = { links.add(it) }
                )
            }.onFailure { Log.e(TAG, "getLiveEpisodeStreams loadLinks failed: ${it.message}", it) }
            links.filterValid().map { it.toCsScraperResult(api.name) }
        }

    /** Filter out broken ExtractorLinks (invalid URLs, error strings, etc.) */
    private fun List<ExtractorLink>.filterValid(): List<ExtractorLink> {
        return filter { link ->
            val url = link.url
            when {
                url.isBlank() -> false
                url == "error" || url == "null" -> false
                !url.startsWith("http://") && !url.startsWith("https://") -> false
                else -> true
            }.also { valid ->
                if (!valid) Log.w(TAG, "Filtered invalid link: source=${link.source}, url=${url.take(60)}")
            }
        }
    }

    private fun ExtractorLink.toCsScraperResult(providerName: String): CsScraperResult {
        val qualityStr = Qualities.getStringByInt(quality).ifEmpty { null }
        val streamType = when (type) {
            ExtractorLinkType.M3U8 -> "hls"
            ExtractorLinkType.DASH -> "dash"
            else -> null
        }
        val allHeaders = buildMap {
            putAll(headers)
            if (referer.isNotBlank()) put("Referer", referer)
        }

        return CsScraperResult(
            title = name,
            name = source,
            url = url,
            quality = qualityStr,
            type = streamType,
            headers = allHeaders.ifEmpty { null },
            provider = providerName,
        )
    }
}

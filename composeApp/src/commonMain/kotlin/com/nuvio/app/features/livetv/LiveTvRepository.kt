package com.nuvio.app.features.livetv

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

private const val GINIKO_JSON_URL =
    "https://raw.githubusercontent.com/Kraptor123/cs-kraptor/master/giniko.json"

private val json = Json { ignoreUnknownKeys = true }

val GINIKO_SOURCE = LiveSource(
    id = "giniko",
    name = "Giniko",
    description = "Dünyadan ücretsiz canlı kanallar",
    logo = "https://www.giniko.com/images/favicon.ico",
)

val INATBOX_SOURCE = LiveSource(
    id = "inatbox",
    name = "InatBox",
    description = "Türk ve yabancı kanallar",
    logo = "https://blogger.googleusercontent.com/img/b/R29vZ2xl/AVvXsEh3vCp6N1K4bECoYRQD-cisJF2_6V_Hk01ZhDmoPR2JuM8O5qr4MqrPO1munM9cRlleBBSK6odYhLtDBWv4E3vhPhynlmS5hVVtJZShHoGA5REQ8_3v8SIlccTEqzVQu2UJyNYQdJNrKIfWy66RQeT0D-CcmFCbHPz5023H6p2v5fv4NVloZ5Rqo_yGrIY/s320/iNat-Box-App.png",
)

object LiveTvRepository {

    suspend fun fetchChannelsForSource(sourceId: String): List<LiveChannel>? = when (sourceId) {
        "giniko" -> fetchGinikoChannels()
        "inatbox" -> fetchPluginLiveChannels(sourceId)
        else -> null
    }

    // Giniko — JSON tabanlı, m3u8 scrape
    private suspend fun fetchGinikoChannels(): List<LiveChannel>? = withContext(Dispatchers.IO) {
        runCatching {
            val text = fetchText(GINIKO_JSON_URL) ?: return@runCatching null
            val list = json.decodeFromString<List<GinikoChannel>>(text)
            list.map { ch ->
                LiveChannel(
                    id = ch.link,
                    name = ch.name,
                    poster = ch.poster,
                    streamPageUrl = ch.link,
                    category = alphabetCategory(ch.name),
                )
            }.sortedWith(compareBy({ it.category }, { it.name }))
        }.getOrNull()
    }

    /**
     * Resolves a channel into a playable stream, or a series (with episodes) for
     * channels that expand into multiple episodes (InatBox dizi/anime).
     */
    suspend fun resolveChannel(channel: LiveChannel): LiveResolveResult = withContext(Dispatchers.IO) {
        return@withContext when {
            channel.streamPageUrl.contains("giniko.com") -> {
                // Giniko — m3u8 scrape
                val stream = runCatching {
                    val html = fetchText(channel.streamPageUrl) ?: return@runCatching null
                    val regex = Regex("""https?://[^\s"'<>]+\.m3u8[^\s"'<>]*""")
                    regex.find(html)?.value?.let { PluginLiveStream(it) }
                }.getOrNull()
                if (stream != null) LiveResolveResult.Stream(stream) else LiveResolveResult.Empty
            }
            else -> {
                // InatBox (plugin) — resolve via CloudStream extension; may be a series.
                resolvePluginChannel("inatbox", channel.id)
            }
        }
    }

    /** Resolves a playable stream for a specific series episode. */
    suspend fun resolveEpisodeStream(episode: LiveEpisode): PluginLiveStream? =
        resolvePluginEpisodeStream("inatbox", episode.data)

    private fun alphabetCategory(name: String): String {
        val first = name.uppercase().trim().firstOrNull() ?: return "#"
        return when {
            first.isDigit() -> "0-9"
            first in 'A'..'Z' -> first.toString()
            else -> "#"
        }
    }
}

expect suspend fun fetchText(url: String): String?

// Platform-specific live TV plugin (CloudStream DEX) integration.
// androidFull → real bridge; androidPlaystore + iOS → empty/null.
expect suspend fun fetchPluginLiveChannels(sourceId: String): List<LiveChannel>?
expect suspend fun resolvePluginChannel(sourceId: String, channelId: String): LiveResolveResult
expect suspend fun resolvePluginEpisodeStream(sourceId: String, episodeData: String): PluginLiveStream?

package com.nuvio.app.features.livetv

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

private const val GINIKO_JSON_URL =
    "https://raw.githubusercontent.com/Kraptor123/cs-kraptor/master/giniko.json"

private const val INATBOX_M3U_URL =
    "https://raw.githubusercontent.com/feroxx/test/refs/heads/main/Kanallar/canlitv.m3u"

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
        "inatbox" -> fetchInatBoxChannels()
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

    // InatBox — platform API tabanlı, kategori bazlı
    private suspend fun fetchInatBoxChannels(): List<LiveChannel>? {
        return fetchInatBoxAllChannels()
    }

    private fun parseM3U(content: String): List<LiveChannel> {
        val channels = mutableListOf<LiveChannel>()
        val lines = content.lines()
        var i = 0
        while (i < lines.size) {
            val line = lines[i].trim()
            if (line.startsWith("#EXTINF:")) {
                val nameMatch = Regex(""",(.+)$""").find(line)
                val name = nameMatch?.groupValues?.get(1)?.trim() ?: ""
                val logo = Regex("""tvg-logo="([^"]+)"""").find(line)?.groupValues?.get(1) ?: ""
                val group = Regex("""group-title="([^"]+)"""").find(line)?.groupValues?.get(1) ?: "Diğer"
                // Next non-empty, non-comment line is the URL
                var j = i + 1
                while (j < lines.size && (lines[j].isBlank() || lines[j].startsWith("#"))) j++
                val url = if (j < lines.size) lines[j].trim() else ""
                if (url.isNotBlank() && name.isNotBlank()) {
                    channels.add(
                        LiveChannel(
                            id = url,
                            name = name,
                            poster = logo,
                            streamPageUrl = url,
                            category = group,
                        )
                    )
                }
                i = j + 1
            } else {
                i++
            }
        }
        return channels
    }

    suspend fun resolveStreamUrl(channel: LiveChannel): String? = withContext(Dispatchers.IO) {
        return@withContext when {
            channel.id.startsWith("{") -> {
                // InatBox JSON item — platform API ile çöz
                resolveInatBoxStreamUrl(channel.id)
            }
            channel.streamPageUrl.contains("giniko.com") -> {
                // Giniko — m3u8 scrape
                runCatching {
                    val html = fetchText(channel.streamPageUrl) ?: return@runCatching null
                    val regex = Regex("""https?://[^\s"'<>]+\.m3u8[^\s"'<>]*""")
                    regex.find(html)?.value
                }.getOrNull()
            }
            else -> channel.streamPageUrl
        }
    }

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

// Platform-specific InatBox implementation
expect suspend fun fetchInatBoxAllChannels(): List<LiveChannel>?
expect suspend fun resolveInatBoxStreamUrl(itemJson: String): String?

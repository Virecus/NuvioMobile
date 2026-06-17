package com.nuvio.app.features.livetv.cloudstream

/**
 * CloudStream-side live TV models. Kept in the `cloudstream` subpackage and
 * prefixed `Cs` to avoid clashing with the commonMain `LiveChannel` (which has a
 * different shape). The bridge maps these to the commonMain models.
 */
data class CsLiveChannel(
    val id: String,
    val name: String,
    val poster: String?,
    val category: String,
    val scraperId: String = "",
    val isDisabled: Boolean = false,
)

data class CsLiveEpisode(
    val season: Int,
    val episode: Int,
    val name: String?,
    val data: String,
    val label: String? = null,
)

/** Mirror of NuvioTV's LocalScraperResult, trimmed for live playback. */
data class CsScraperResult(
    val title: String,
    val name: String? = null,
    val url: String,
    val quality: String? = null,
    val language: String? = null,
    val provider: String? = null,
    val type: String? = null,
    val headers: Map<String, String>? = null,
)

sealed class CsLiveChannelResult {
    data class Streams(val streams: List<CsScraperResult>) : CsLiveChannelResult()
    data class Series(val videoId: String, val title: String, val episodes: List<CsLiveEpisode>) : CsLiveChannelResult()
    object Empty : CsLiveChannelResult()
}

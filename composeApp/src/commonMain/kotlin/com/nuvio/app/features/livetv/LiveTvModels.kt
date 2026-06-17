package com.nuvio.app.features.livetv

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GinikoChannel(
    @SerialName("icerik_ismi") val name: String,
    @SerialName("link") val link: String,
    @SerialName("poster") val poster: String,
)

data class LiveChannel(
    val id: String,
    val name: String,
    val poster: String,
    val streamPageUrl: String,
    val category: String,
)

data class LiveSource(
    val id: String,
    val name: String,
    val description: String,
    val logo: String? = null,
)

/** A resolved playable live stream plus any HTTP headers the player must send. */
data class PluginLiveStream(
    val url: String,
    val headers: Map<String, String> = emptyMap(),
)

/** A single episode of a series-type live channel (e.g. InatBox dizi/anime). */
data class LiveEpisode(
    val season: Int,
    val episode: Int,
    val name: String?,
    val data: String,
    val label: String? = null, // e.g. "TR Dublaj", "TR Altyazı"
)

/**
 * Result of resolving a live channel: either a directly playable stream, a
 * series with an episode list (caller opens the picker), or empty.
 */
sealed interface LiveResolveResult {
    data class Stream(val stream: PluginLiveStream) : LiveResolveResult
    data class Series(val title: String, val episodes: List<LiveEpisode>) : LiveResolveResult
    data object Empty : LiveResolveResult
}

sealed interface LiveTvState {
    data object Idle : LiveTvState
    data object Loading : LiveTvState
    data object NetworkError : LiveTvState
    data class Ready(
        val sources: List<LiveSource>,
        val channelsBySource: Map<String, List<LiveChannel>>,
    ) : LiveTvState
}

@kotlinx.serialization.Serializable
sealed interface LiveNavState {
    @kotlinx.serialization.Serializable
    data object SourceList : LiveNavState
    @kotlinx.serialization.Serializable
    data class CategoryList(val sourceId: String, val sourceName: String, val sourceDescription: String, val sourceLogo: String?) : LiveNavState
    @kotlinx.serialization.Serializable
    data class ChannelList(val sourceId: String, val sourceName: String, val sourceDescription: String, val sourceLogo: String?, val category: String) : LiveNavState
}

fun LiveSource.toNavCategoryList() = LiveNavState.CategoryList(id, name, description, logo)
fun LiveSource.toNavChannelList(category: String) = LiveNavState.ChannelList(id, name, description, logo, category)
fun LiveNavState.CategoryList.toSource() = LiveSource(sourceId, sourceName, sourceDescription, sourceLogo)
fun LiveNavState.ChannelList.toSource() = LiveSource(sourceId, sourceName, sourceDescription, sourceLogo)

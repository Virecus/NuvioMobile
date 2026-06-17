package com.nuvio.app.features.livetv

import com.nuvio.app.features.livetv.cloudstream.InatBoxPluginBridge

actual suspend fun fetchPluginLiveChannels(sourceId: String): List<LiveChannel>? =
    InatBoxPluginBridge.fetchLiveChannels(sourceId)

actual suspend fun resolvePluginChannel(sourceId: String, channelId: String): LiveResolveResult =
    InatBoxPluginBridge.resolveChannel(sourceId, channelId)

actual suspend fun resolvePluginEpisodeStream(sourceId: String, episodeData: String): PluginLiveStream? =
    InatBoxPluginBridge.resolveEpisodeStream(sourceId, episodeData)

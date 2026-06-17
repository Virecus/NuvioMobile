package com.nuvio.app.features.livetv

import com.nuvio.app.features.livetv.cloudstream.InatBoxPluginBridge

actual suspend fun fetchPluginLiveChannels(sourceId: String): List<LiveChannel>? =
    InatBoxPluginBridge.fetchLiveChannels(sourceId)

actual suspend fun resolvePluginLiveStream(sourceId: String, channelId: String): PluginLiveStream? =
    InatBoxPluginBridge.resolveLiveStream(sourceId, channelId)

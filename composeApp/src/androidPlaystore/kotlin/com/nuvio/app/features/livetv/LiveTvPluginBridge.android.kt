package com.nuvio.app.features.livetv

// PlayStore variant: no CloudStream DEX plugin runtime. InatBox is absent.

actual suspend fun fetchPluginLiveChannels(sourceId: String): List<LiveChannel>? = null

actual suspend fun resolvePluginLiveStream(sourceId: String, channelId: String): PluginLiveStream? = null

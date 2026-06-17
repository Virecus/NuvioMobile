package com.nuvio.app.features.livetv

// PlayStore variant: no CloudStream DEX plugin runtime. InatBox is absent.

actual suspend fun fetchPluginLiveChannels(sourceId: String): List<LiveChannel>? = null

actual suspend fun resolvePluginChannel(sourceId: String, channelId: String): LiveResolveResult =
    LiveResolveResult.Empty

actual suspend fun resolvePluginEpisodeStream(sourceId: String, episodeData: String): PluginLiveStream? = null
